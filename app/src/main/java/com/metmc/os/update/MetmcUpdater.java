package com.metmc.os.update;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

public final class MetmcUpdater {

    private static final String PACKAGE_NAME = "com.metmc.os";

    private static final String UPDATE_JSON =
            "https://github.com/TEMMC/METMC_OS/releases/download/metmc-os-latest/metmc-update.json";

    private static final String APK_NAME = "metmc-os.apk";

    private MetmcUpdater() {}

    public interface Callback {
        void onResult(boolean updateAvailable, String message);
    }

    public static void checkForUpdate(
            Context context,
            boolean showNoUpdateMessage
    ) {
        checkForUpdate(context, showNoUpdateMessage, null);
    }

    public static void checkForUpdate(
            Context context,
            boolean showNoUpdateMessage,
            Callback callback
    ) {
        new Thread(() -> {
            try {
                UpdateInfo update = fetchUpdate(context);

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (update == null) {
                        if (showNoUpdateMessage) {
                            Toast.makeText(
                                    context,
                                    "METMC OS is up to date.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                        if (callback != null) {
                            callback.onResult(false, "Already up to date.");
                        }

                        return;
                    }

                    if (callback != null) {
                        callback.onResult(
                                true,
                                "Update " + update.versionName + " available."
                        );
                    }

                    showUpdateDialog(context, update);
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (showNoUpdateMessage) {
                        Toast.makeText(
                                context,
                                "Update check failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    if (callback != null) {
                        callback.onResult(false, e.getMessage());
                    }
                });
            }
        }).start();
    }

    private static UpdateInfo fetchUpdate(Context context)
            throws Exception {

        HttpURLConnection connection =
                (HttpURLConnection) new URL(UPDATE_JSON).openConnection();

        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty(
                "User-Agent",
                "METMC-OS-Updater"
        );

        int response = connection.getResponseCode();

        if (response != HttpURLConnection.HTTP_OK) {
            throw new Exception(
                    "Update server returned HTTP " + response
            );
        }

        StringBuilder json = new StringBuilder();

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     connection.getInputStream()
                             )
                     )) {

            String line;

            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        } finally {
            connection.disconnect();
        }

        JSONObject object = new JSONObject(json.toString());

        String packageName =
                object.getString("packageName");

        if (!PACKAGE_NAME.equals(packageName)) {
            throw new SecurityException(
                    "Update package mismatch."
            );
        }

        long remoteVersionCode =
                object.getLong("versionCode");

        String remoteVersionName =
                object.getString("versionName");

        long localVersionCode =
                getLocalVersionCode(context);

        if (remoteVersionCode <= localVersionCode) {
            return null;
        }

        String apkUrl =
                object.getString("apkUrl");

        String sha256 =
                object.getString("sha256")
                        .trim()
                        .toLowerCase(Locale.US);

        if (!apkUrl.startsWith(
                "https://github.com/TEMMC/METMC_OS/releases/"
        )) {
            throw new SecurityException(
                    "APK source is not an official METMC release."
            );
        }

        return new UpdateInfo(
                remoteVersionCode,
                remoteVersionName,
                apkUrl,
                sha256
        );
    }

    private static long getLocalVersionCode(Context context) {
        try {
            PackageInfo info;

            if (android.os.Build.VERSION.SDK_INT >= 33) {
                info = context.getPackageManager()
                        .getPackageInfo(
                                context.getPackageName(),
                                PackageManager.PackageInfoFlags.of(0)
                        );
            } else {
                info = context.getPackageManager()
                        .getPackageInfo(
                                context.getPackageName(),
                                0
                        );
            }

            return info.getLongVersionCode();

        } catch (Exception e) {
            return 0;
        }
    }

    private static void showUpdateDialog(
            Context context,
            UpdateInfo update
    ) {
        if (!(context instanceof Activity)) {
            return;
        }

        Activity activity = (Activity) context;

        if (activity.isFinishing()) {
            return;
        }

        new AlertDialog.Builder(activity)
                .setTitle("METMC OS Update")
                .setMessage(
                        "A new METMC OS version is available.\n\n" +
                        "Installed: " +
                        getLocalVersionCode(context) +
                        "\nNew: " +
                        update.versionCode +
                        " (" +
                        update.versionName +
                        ")\n\n" +
                        "The official release APK will be downloaded, " +
                        "verified and installed."
                )
                .setNegativeButton("Later", null)
                .setPositiveButton(
                        "Update Now",
                        (dialog, which) ->
                                downloadAndInstall(
                                        context,
                                        update
                                )
                )
                .show();
    }

    private static void downloadAndInstall(
            Context context,
            UpdateInfo update
    ) {
        Toast.makeText(
                context,
                "Downloading METMC OS " +
                        update.versionName + "...",
                Toast.LENGTH_LONG
        ).show();

        new Thread(() -> {
            File apk = null;

            try {
                apk = downloadApk(context, update);

                verifySha256(apk, update.sha256);

                verifyApkPackageAndVersion(
                        context,
                        apk,
                        update
                );

                File finalApk = apk;

                new Handler(Looper.getMainLooper()).post(() -> {
                    installAsRoot(context, finalApk);
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        new AlertDialog.Builder(context)
                                .setTitle("Update failed")
                                .setMessage(e.getMessage())
                                .setPositiveButton("Close", null)
                                .show()
                );
            }
        }).start();
    }

    private static File downloadApk(
            Context context,
            UpdateInfo update
    ) throws Exception {

        File dir = context.getExternalFilesDir(null);

        if (dir == null) {
            dir = context.getCacheDir();
        }

        File partial =
                new File(dir, APK_NAME + ".part");

        File apk =
                new File(dir, APK_NAME);

        if (partial.exists()) {
            partial.delete();
        }

        HttpURLConnection connection =
                (HttpURLConnection)
                        new URL(update.apkUrl).openConnection();

        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty(
                "User-Agent",
                "METMC-OS-Updater"
        );

        int response = connection.getResponseCode();

        if (response != HttpURLConnection.HTTP_OK) {
            throw new Exception(
                    "APK download failed: HTTP " + response
            );
        }

        try (
                InputStream input =
                        new BufferedInputStream(
                                connection.getInputStream()
                        );

                OutputStream output =
                        new FileOutputStream(partial)
        ) {

            byte[] buffer = new byte[64 * 1024];

            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        } finally {
            connection.disconnect();
        }

        if (apk.exists()) {
            apk.delete();
        }

        if (!partial.renameTo(apk)) {
            throw new Exception(
                    "Unable to finalize downloaded APK."
            );
        }

        return apk;
    }

    private static void verifySha256(
            File file,
            String expected
    ) throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

        try (InputStream input =
                     new FileInputStream(file)) {

            byte[] buffer = new byte[64 * 1024];

            int count;

            while ((count = input.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
        }

        StringBuilder actual =
                new StringBuilder();

        for (byte b : digest.digest()) {
            actual.append(
                    String.format(
                            Locale.US,
                            "%02x",
                            b
                    )
            );
        }

        if (!actual.toString().equalsIgnoreCase(expected)) {
            throw new SecurityException(
                    "APK SHA-256 verification failed."
            );
        }
    }

    @SuppressWarnings("deprecation")
    private static void verifyApkPackageAndVersion(
            Context context,
            File apk,
            UpdateInfo update
    ) throws Exception {

        PackageManager pm =
                context.getPackageManager();

        PackageInfo archive =
                pm.getPackageArchiveInfo(
                        apk.getAbsolutePath(),
                        0
                );

        if (archive == null) {
            throw new SecurityException(
                    "Downloaded file is not a valid Android APK."
            );
        }

        if (!PACKAGE_NAME.equals(
                archive.packageName
        )) {
            throw new SecurityException(
                    "Downloaded APK package mismatch."
            );
        }

        long apkVersion =
                archive.getLongVersionCode();

        if (apkVersion != update.versionCode) {
            throw new SecurityException(
                    "Downloaded APK version mismatch."
            );
        }
    }

    private static void installAsRoot(
            Context context,
            File apk
    ) {
        try {
            String path =
                    apk.getAbsolutePath()
                            .replace(
                                    "\\",
                                    "\\\\"
                            )
                            .replace(
                                    "'",
                                    "'\\''"
                            );

            String command =
                    "pm install -r '" +
                    path +
                    "'";

            Process process =
                    new ProcessBuilder(
                            "su",
                            "-c",
                            command
                    )
                    .redirectErrorStream(true)
                    .start();

            StringBuilder output =
                    new StringBuilder();

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         process.getInputStream()
                                 )
                         )) {

                String line;

                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }

            int result =
                    process.waitFor();

            if (result != 0 ||
                    !output.toString()
                            .toLowerCase(Locale.US)
                            .contains("success")) {

                throw new Exception(
                        "Root installation failed:\n" +
                        output
                );
            }

            Toast.makeText(
                    context,
                    "METMC OS updated. Restarting...",
                    Toast.LENGTH_LONG
            ).show();

            new Handler(Looper.getMainLooper())
                    .postDelayed(() -> {

                        try {
                            context.startActivity(
                                    new android.content.Intent()
                                            .setClassName(
                                                    PACKAGE_NAME,
                                                    PACKAGE_NAME +
                                                            ".MainActivity"
                                            )
                                            .addFlags(
                                                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK |
                                                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                                    android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                                            )
                            );
                        } catch (Exception ignored) {
                        }

                        try {
                            Runtime.getRuntime().exec(
                                    new String[]{
                                            "su",
                                            "-c",
                                            "am force-stop " +
                                                    PACKAGE_NAME
                                    }
                            );

                            Thread.sleep(500);

                            Runtime.getRuntime().exec(
                                    new String[]{
                                            "su",
                                            "-c",
                                            "am start -n " +
                                                    PACKAGE_NAME +
                                                    "/.MainActivity"
                                    }
                            );
                        } catch (Exception ignored) {
                        }

                    }, 1500);

        } catch (Exception e) {

            new AlertDialog.Builder(context)
                    .setTitle("Installation failed")
                    .setMessage(
                            "METMC OS could not install the update.\n\n" +
                            e.getMessage()
                    )
                    .setPositiveButton(
                            "Close",
                            null
                    )
                    .show();
        }
    }

    private static final class UpdateInfo {

        final long versionCode;
        final String versionName;
        final String apkUrl;
        final String sha256;

        UpdateInfo(
                long versionCode,
                String versionName,
                String apkUrl,
                String sha256
        ) {
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.apkUrl = apkUrl;
            this.sha256 = sha256;
        }
    }
}
