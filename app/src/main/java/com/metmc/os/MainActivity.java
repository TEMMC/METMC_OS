package com.metmc.os;

import com.metmc.os.linux.LinuxGuiEnvironment;
import com.metmc.os.linux.LinuxGuiLauncher;
import com.metmc.os.linux.DesktopWindow;

import com.metmc.os.desktop.AndroidWindowLauncher;
import com.metmc.os.desktop.MetmcDesktop;


import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.view.KeyEvent;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.zip.GZIPInputStream;

public class MainActivity extends Activity {

    private FrameLayout desktopArea;

    private FrameLayout area;
    private MetmcDesktop desktopView;

    LinearLayout root, desktop, dock;
    TextView clock, status;
    Handler handler = new Handler();

    int BG = Color.rgb(12,13,18);
    int PANEL = Color.rgb(24,25,32);
    int PANEL2 = Color.rgb(32,33,42);
    int WHITE = Color.WHITE;
    int GRAY = Color.rgb(175,178,190);

    int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + .5f);
    }

    TextView tv(String s, float size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(WHITE);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    Button btn(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setBackgroundColor(PANEL2);
        return b;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setStatusBarColor(Color.rgb(8,8,10));
        getWindow().setNavigationBarColor(Color.rgb(8,8,10));

        desktopView = new MetmcDesktop(this);
        setContentView(desktopView);
        tick();

        checkLinuxEnvironmentOnStartup();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 9001 && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            getContentResolver().takePersistableUriPermission(
                data.getData(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
            desktopView.applyWallpaper(data.getData());
        }
    }

    void checkLinuxEnvironmentOnStartup() {
        File rootfs = new File(METMC_ROOTFS);

        if (!new File(rootfs, "bin/bash").exists()) {
            handler.postDelayed(this::showLinuxInstaller, 600);
        }
    }

    // Central root executor for ALL METMC OS Debian/Linux features.






    // Central Android root executor.
    // All privileged METMC operations should use this layer.






    java.lang.Process rootProcess(String command) throws Exception {
        String[] suPaths = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/debug_ramdisk/su"
        };

        Exception last = null;

        for (String su : suPaths) {
            try {
                java.lang.Process p = new ProcessBuilder(
                    su, "-c", command
                ).redirectErrorStream(true).start();

                return p;
            } catch (Exception e) {
                last = e;
            }
        }

        throw new java.io.IOException(
            "Unable to start Android root shell: " +
            (last == null ? "su not found" : last.getMessage())
        );
    }

    boolean hasRootAccess() {
        try {
            java.lang.Process p = rootProcess("id");

            java.io.BufferedReader reader =
                new java.io.BufferedReader(
                    new java.io.InputStreamReader(
                        p.getInputStream()
                    )
                );

            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }

            int code = p.waitFor();

            return code == 0 &&
                   output.toString().contains("uid=0");
        } catch (Exception e) {
            return false;
        }
    }

    String runRoot(String command) throws Exception {
        java.lang.Process p = rootProcess(command);

        java.io.BufferedReader reader =
            new java.io.BufferedReader(
                new java.io.InputStreamReader(
                    p.getInputStream()
                )
            );

        StringBuilder output = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            output.append(line).append('\n');
        }

        int code = p.waitFor();

        if (code != 0) {
            throw new java.io.IOException(
                "Root command failed (" + code + "):\n" +
                output.toString()
            );
        }

        return output.toString();
    }

    void build() {

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        /*
         * METMC OS UNIFIED DESKTOP
         *
         * Android and Linux are treated as applications,
         * not separate desktop environments.
         */

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(16),0,dp(16),0);
        top.setBackgroundColor(PANEL);

        TextView logo = tv("◈  METMC OS",18);
        logo.setTypeface(
            Typeface.DEFAULT,
            Typeface.BOLD
        );

        top.addView(
            logo,
            new LinearLayout.LayoutParams(
                0,-1,1
            )
        );

        clock = tv("",14);

        top.addView(
            clock,
            new LinearLayout.LayoutParams(
                -2,-1
            )
        );

        root.addView(
            top,
            new LinearLayout.LayoutParams(
                -1,dp(52)
            )
        );

        /*
         * Desktop workspace.
         */
        area = new FrameLayout(this);

        desktopArea.setBackgroundColor(BG);

        LinearLayout center =
            new LinearLayout(this);

        center.setOrientation(
            LinearLayout.VERTICAL
        );

        center.setGravity(Gravity.CENTER);

        TextView title =
            tv("METMC OS",38);

        title.setGravity(Gravity.CENTER);

        title.setTypeface(
            Typeface.DEFAULT,
            Typeface.BOLD
        );

        TextView sub =
            tv(
                "Android + Linux Desktop",
                16
            );

        sub.setGravity(Gravity.CENTER);
        sub.setTextColor(GRAY);

        TextView ready =
            tv("● System Ready",14);

        ready.setGravity(Gravity.CENTER);

        ready.setTextColor(
            Color.rgb(100,220,140)
        );

        center.addView(
            title,
            new LinearLayout.LayoutParams(
                -1,dp(55)
            )
        );

        center.addView(
            sub,
            new LinearLayout.LayoutParams(
                -1,dp(35)
            )
        );

        center.addView(
            ready,
            new LinearLayout.LayoutParams(
                -1,dp(35)
            )
        );

        desktopArea.addView(
            center,
            new FrameLayout.LayoutParams(
                -1,-1
            )
        );

        /*
         * SINGLE APPLICATION TASKBAR
         *
         * No Linux button.
         * No Android button.
         * No wallpaper button.
         * No settings button.
         * No decorative icon buttons.
         *
         * Only currently opened applications appear here.
         */
        dock = new LinearLayout(this);

        dock.setOrientation(
            LinearLayout.HORIZONTAL
        );

        dock.setGravity(
            Gravity.CENTER_VERTICAL
        );

        dock.setPadding(
            dp(8),
            dp(5),
            dp(8),
            dp(5)
        );

        dock.setBackgroundColor(PANEL);

        FrameLayout.LayoutParams dockParams =
            new FrameLayout.LayoutParams(
                -1,
                dp(58)
            );

        dockParams.gravity =
            Gravity.BOTTOM;

        dockParams.setMargins(
            dp(8),
            0,
            dp(8),
            dp(8)
        );

        desktopArea.addView(
            dock,
            dockParams
        );

        /*
         * START / APPLICATIONS BUTTON
         *
         * This is the only permanent taskbar
         * control. Running applications are added
         * beside it.
         */
        Button apps = btn("Apps");

        apps.setTextSize(13);
        apps.setAllCaps(false);

        apps.setOnClickListener(
            v -> showApps()
        );

        dock.addView(
            apps,
            new LinearLayout.LayoutParams(
                dp(82),
                dp(48)
            )
        );

        /*
         * Workspace.
         */
        root.addView(
            desktopArea,
            new LinearLayout.LayoutParams(
                -1,
                0,
                1
            )
        );

        /*
         * Small system status bar.
         */
        LinearLayout bottom =
            new LinearLayout(this);

        bottom.setGravity(
            Gravity.CENTER_VERTICAL
        );

        bottom.setPadding(
            dp(14),
            0,
            dp(14),
            0
        );

        bottom.setBackgroundColor(
            PANEL
        );

        TextView status =
            tv("● METMC OS",13);

        status.setTextColor(
            Color.rgb(100,220,140)
        );

        TextView device =
            tv("Android Desktop",13);

        device.setTextColor(GRAY);

        bottom.addView(
            status,
            new LinearLayout.LayoutParams(
                0,-1,1
            )
        );

        bottom.addView(
            device,
            new LinearLayout.LayoutParams(
                -2,-1
            )
        );

        root.addView(
            bottom,
            new LinearLayout.LayoutParams(
                -1,dp(32)
            )
        );

        setContentView(root);
    }

    void addDock(String label, View.OnClickListener click) {
        Button b=btn(label);
        b.setGravity(Gravity.CENTER);
        b.setOnClickListener(click);
        dock.addView(b,new LinearLayout.LayoutParams(dp(105),dp(65)));
    }

    void panel(String title,String message) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Close",null)
            .show();
    }

    void showApps() {

        final Dialog d = new Dialog(this);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(
            dp(18), dp(16),
            dp(18), dp(16)
        );
        box.setBackgroundColor(PANEL);

        TextView header =
            tv("METMC OS • Applications",22);

        header.setTypeface(
            Typeface.DEFAULT,
            Typeface.BOLD
        );

        box.addView(
            header,
            new LinearLayout.LayoutParams(
                -1,dp(52)
            )
        );

        EditText search = new EditText(this);
        search.setHint("Search applications");
        search.setHintTextColor(GRAY);
        search.setTextColor(WHITE);
        search.setSingleLine(true);

        box.addView(
            search,
            new LinearLayout.LayoutParams(
                -1,dp(52)
            )
        );

        ScrollView scroll = new ScrollView(this);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        /*
         * =========================================================
         * ANDROID
         * =========================================================
         *
         * Scan installed packages directly.
         *
         * This is deliberately NOT limited to
         * ACTION_MAIN + CATEGORY_LAUNCHER.
         */
        TextView androidHeader =
            tv("ANDROID APPLICATIONS",12);

        androidHeader.setTextColor(GRAY);
        androidHeader.setPadding(
            dp(8),dp(12),
            dp(8),dp(4)
        );

        list.addView(androidHeader);

        PackageManager pm = getPackageManager();

        java.util.List<android.content.pm.ApplicationInfo> packages =
            pm.getInstalledApplications(
                PackageManager.GET_META_DATA
            );

        java.util.Collections.sort(
            packages,
            (a,b) -> {

                String an =
                    String.valueOf(
                        pm.getApplicationLabel(a)
                    );

                String bn =
                    String.valueOf(
                        pm.getApplicationLabel(b)
                    );

                return an.compareToIgnoreCase(bn);
            }
        );

        int androidCount = 0;

        for(android.content.pm.ApplicationInfo info : packages) {

            final String packageName =
                info.packageName;

            /*
             * Find an activity that Android can actually launch.
             */
            final Intent launchIntent =
                pm.getLaunchIntentForPackage(
                    packageName
                );

            if(launchIntent == null)
                continue;

            final String appName =
                String.valueOf(
                    pm.getApplicationLabel(info)
                ).trim();

            if(appName.isEmpty())
                continue;

            Button app = btn(appName);

            app.setAllCaps(false);
            app.setGravity(
                Gravity.LEFT |
                Gravity.CENTER_VERTICAL
            );

            app.setTag(
                "android:" + packageName
            );

            app.setOnClickListener(v -> {

                try {

                    Intent launch =
                        pm.getLaunchIntentForPackage(
                            packageName
                        );

                    if(launch == null) {
                        panel(
                            "Application unavailable",
                            appName +
                            " does not have a launchable activity."
                        );
                        return;
                    }

                    launch.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    );

                    startActivity(launch);

                    /*
                     * Add the application to the
                     * unified METMC taskbar.
                     */
                    addRunningTask(
                        appName,
                        () -> {

                            Intent again =
                                pm.getLaunchIntentForPackage(
                                    packageName
                                );

                            if(again != null) {

                                again.addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                                );

                                startActivity(again);
                            }
                        }
                    );

                    d.dismiss();

                } catch(Exception e) {

                    panel(
                        "Launch error",
                        appName + "\n\n" + e
                    );
                }
            });

            list.addView(
                app,
                new LinearLayout.LayoutParams(
                    -1,dp(52)
                )
            );

            androidCount++;
        }

        TextView androidTotal =
            tv(
                androidCount +
                " launchable Android applications",
                12
            );

        androidTotal.setTextColor(GRAY);
        androidTotal.setPadding(
            dp(8),dp(8),
            dp(8),dp(12)
        );

        list.addView(androidTotal);

        /*
         * =========================================================
         * LINUX
         * =========================================================
         */
        TextView linuxHeader =
            tv("LINUX APPLICATIONS",12);

        linuxHeader.setTextColor(GRAY);
        linuxHeader.setPadding(
            dp(8),dp(18),
            dp(8),dp(4)
        );

        list.addView(linuxHeader);

        TextView linuxLoading =
            tv(
                "Loading Linux applications...",
                14
            );

        linuxLoading.setTextColor(GRAY);

        list.addView(
            linuxLoading,
            new LinearLayout.LayoutParams(
                -1,dp(48)
            )
        );

        scroll.addView(list);

        box.addView(
            scroll,
            new LinearLayout.LayoutParams(
                -1,0,1
            )
        );

        Button close = btn("Close");

        close.setOnClickListener(
            v -> d.dismiss()
        );

        box.addView(
            close,
            new LinearLayout.LayoutParams(
                -1,dp(50)
            )
        );

        d.setContentView(box);
        d.show();

        if(d.getWindow() != null) {

            d.getWindow().setLayout(
                dp(700),
                dp(620)
            );
        }

        /*
         * Linux application discovery runs in the
         * background so the Android UI remains responsive.
         */
        new Thread(() -> {

            try {

                String result =
                    runLinuxCommandSync(
                        "find /usr/share/applications " +
                        "-name '*.desktop' -type f " +
                        "2>/dev/null | sort"
                    );

                String[] files =
                    result.split("\\n");

                runOnUiThread(() -> {

                    list.removeView(
                        linuxLoading
                    );

                    int linuxCount = 0;

                    for(String file : files) {

                        if(file.trim().isEmpty())
                            continue;

                        try {

                            String data =
                                runLinuxCommandSync(
                                    "cat " +
                                    shellQuote(file)
                                );

                            String name =
                                desktopValue(
                                    data,
                                    "Name"
                                );

                            String exec =
                                desktopValue(
                                    data,
                                    "Exec"
                                );

                            if(name == null ||
                               name.trim().isEmpty())
                                continue;

                            if(exec == null ||
                               exec.trim().isEmpty())
                                continue;

                            final String linuxName =
                                name.trim();

                            final String linuxExec =
                                exec.trim();

                            Button linuxApp =
                                btn(linuxName);

                            linuxApp.setAllCaps(false);
                            linuxApp.setGravity(
                                Gravity.LEFT |
                                Gravity.CENTER_VERTICAL
                            );

                            linuxApp.setTag(
                                "linux:" + linuxName
                            );

                            linuxApp.setOnClickListener(v -> {

                                LinuxGuiLauncher.launch(
                                    MainActivity.this,
                                    METMC_ROOTFS,
                                    "export DISPLAY=:100; " +
                                    "export XDG_RUNTIME_DIR=/tmp/metmc-runtime; " +
                                    "mkdir -p /tmp/metmc-runtime; " +
                                    "chmod 700 /tmp/metmc-runtime; " +
                                    linuxExec +
                                    " >/tmp/metmc-linux-app.log " +
                                    "2>&1 &"
                                );

                                addRunningTask(
                                    linuxName,
                                    () ->
                                        LinuxGuiLauncher.launch(
                                            MainActivity.this,
                                            METMC_ROOTFS,
                                            "export DISPLAY=:100; " +
                                            "export XDG_RUNTIME_DIR=/tmp/metmc-runtime; " +
                                            "mkdir -p /tmp/metmc-runtime; " +
                                            "chmod 700 /tmp/metmc-runtime; " +
                                            linuxExec +
                                            " >/tmp/metmc-linux-app.log " +
                                            "2>&1 &"
                                        )
                                );

                                d.dismiss();
                            });

                            list.addView(
                                linuxApp,
                                new LinearLayout.LayoutParams(
                                    -1,dp(52)
                                )
                            );

                            linuxCount++;

                        } catch(Exception ignored) {
                        }
                    }

                    TextView linuxTotal =
                        tv(
                            linuxCount +
                            " Linux applications",
                            12
                        );

                    linuxTotal.setTextColor(GRAY);
                    linuxTotal.setPadding(
                        dp(8),dp(8),
                        dp(8),dp(12)
                    );

                    list.addView(linuxTotal);
                });

            } catch(Exception e) {

                runOnUiThread(() -> {

                    list.removeView(
                        linuxLoading
                    );

                    TextView error =
                        tv(
                            "Linux application service unavailable.",
                            14
                        );

                    error.setTextColor(GRAY);

                    list.addView(error);
                });
            }

        }).start();
    }

    void addRunningTask(
        String title,
        Runnable launch
    ) {

        if(dock == null)
            return;

        for(int i = 0; i < dock.getChildCount(); i++) {

            View child =
                dock.getChildAt(i);

            if(title.equals(
                child.getTag()
            )) {

                child.setVisibility(
                    View.VISIBLE
                );

                child.bringToFront();
                return;
            }
        }

        Button task =
            btn(title);

        task.setTag(title);
        task.setTextSize(12);
        task.setAllCaps(false);
        task.setMaxLines(1);
        task.setEllipsize(
            android.text.TextUtils.TruncateAt.END
        );

        task.setOnClickListener(
            v -> {
                try {
                    launch.run();
                } catch(Exception e) {
                    panel(
                        "Application error",
                        e.toString()
                    );
                }
            }
        );

        dock.addView(
            task,
            new LinearLayout.LayoutParams(
                dp(145),
                dp(48)
            )
        );

        task.bringToFront();
    }

    void openFiles() {
        try {
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("*/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i,100);
        } catch(Exception e) {
            panel("Files","Android file picker unavailable.");
        }
    }

    static final String METMC_ROOTFS = "/data/local/linux/rootfs";
    static final String METMC_LINUX = "/data/local/linux";
    static final String DEBIAN_URL =
            "https://cloudfront.debian.net/cdimage/cloud/bookworm/latest/debian-12-generic-arm64.tar.xz";

    void linuxPanel() {
        File rootfs = new File(METMC_ROOTFS);
        if (!new File(rootfs, "bin/bash").exists()) {
            showLinuxInstaller();
            return;
        }
        showLinuxControl();
    }

    void showLinuxInstaller() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("METMC Linux")
                .setMessage(
                        "Debian Linux environment was not found.\n\n" +
                        "METMC OS can install the official Debian 12 " +
                        "Bookworm ARM64 environment into:\n" +
                        METMC_ROOTFS +
                        "\n\nRoot access is required."
                )
                .setPositiveButton("Install Debian", null)
                .setNegativeButton("Later", null)
                .create();

        dialog.setOnShowListener(v ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(x -> {
                            dialog.dismiss();
                            installDebian();
                        })
        );

        dialog.show();
    }

    void installDebian() {
        if (!hasRoot()) {
            panel("METMC Linux",
                    "Root access is required to install Debian into " +
                            METMC_LINUX + ".");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Install Debian")
                .setMessage(
                        "METMC OS will download the official Debian 12 " +
                        "Bookworm ARM64 root filesystem.\n\n" +
                        "Several hundred MB of storage may be required."
                )
                .setPositiveButton("Continue",
                        (d, w) -> startDebianInstall())
                .setNegativeButton("Cancel", null)
                .show();
    }

    boolean hasRoot() {
        try {
            java.lang.Process p = rootProcess("id");
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    void startDebianInstall() {
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("METMC Linux");
        progress.setMessage("Preparing Debian...");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            String result;

            try {
                File base = new File(METMC_LINUX);
                File archive = new File(
                        base,
                        "debian-bookworm-arm64.tar.xz"
                );

                String tempRoot =
                        METMC_LINUX + "/debian-rootfs-new";
                String backupRoot =
                        METMC_LINUX + "/debian-rootfs-backup";

                runRoot(
                        "mkdir -p " +
                        shellQuote(METMC_LINUX)
                );

                runOnUiThread(() -> {
                    progress.setIndeterminate(false);
                    progress.setProgress(0);
                    progress.setMessage("Downloading Debian 12...");
                });

                downloadFile(DEBIAN_URL, archive, progress);

                runOnUiThread(() ->
                        progress.setMessage("Extracting Debian..."));

                /*
                 * Never extract directly into the active rootfs.
                 * Build and verify the new rootfs first.
                 */
                runRoot(
                        "set -e; " +
                        "rm -rf " + shellQuote(tempRoot) + "; " +
                        "mkdir -p " + shellQuote(tempRoot) + "; " +
                        "tar -xJf " + shellQuote(archive.getAbsolutePath()) +
                        " -C " + shellQuote(tempRoot) + "; " +

                        /*
                         * Debian cloud archives normally contain the
                         * filesystem directly. Handle a single
                         * rootfs/ wrapper defensively.
                         */
                        "if [ -d " + shellQuote(tempRoot + "/rootfs") +
                        " ] && [ ! -x " +
                        shellQuote(tempRoot + "/bin/bash") +
                        " ]; then " +
                        "mv " + shellQuote(tempRoot + "/rootfs") +
                        "/* " + shellQuote(tempRoot) + "/; " +
                        "mv " + shellQuote(tempRoot + "/rootfs") +
                        "/.[!.]* " + shellQuote(tempRoot) +
                        "/ 2>/dev/null || true; " +
                        "rm -rf " + shellQuote(tempRoot + "/rootfs") +
                        "; " +
                        "fi; " +

                        /*
                         * Verify before replacing the active installation.
                         */
                        "test -x " +
                        shellQuote(tempRoot + "/bin/bash") + "; " +
                        "test -x " +
                        shellQuote(tempRoot +
                                "/usr/lib/ld-linux-aarch64.so.1") + "; " +
                        "test -d " +
                        shellQuote(tempRoot + "/etc") + "; " +
                        "test -d " +
                        shellQuote(tempRoot + "/usr");"
                );

                runOnUiThread(() ->
                        progress.setMessage("Activating Debian..."));

                runRoot(
                        "set -e; " +

                        "rm -rf " + shellQuote(backupRoot) + "; " +

                        "if [ -d " + shellQuote(METMC_ROOTFS) +
                        " ]; then " +
                        "mv " + shellQuote(METMC_ROOTFS) +
                        " " + shellQuote(backupRoot) + "; " +
                        "fi; " +

                        "mv " + shellQuote(tempRoot) +
                        " " + shellQuote(METMC_ROOTFS) + "; " +

                        "mkdir -p " +
                        shellQuote(METMC_ROOTFS + "/proc") + " " +
                        shellQuote(METMC_ROOTFS + "/sys") + " " +
                        shellQuote(METMC_ROOTFS + "/dev") + " " +
                        shellQuote(METMC_ROOTFS + "/tmp") + " " +
                        shellQuote(METMC_ROOTFS + "/run") + "; " +

                        "chmod 1777 " +
                        shellQuote(METMC_ROOTFS + "/tmp") + "; " +

                        "printf '%s\\n' " +
                        "'nameserver 1.1.1.1' " +
                        "'nameserver 8.8.8.8' > " +
                        shellQuote(METMC_ROOTFS + "/etc/resolv.conf") + "; " +

                        /*
                         * Verify the active installation once more.
                         */
                        "test -x " +
                        shellQuote(METMC_ROOTFS + "/bin/bash") + "; " +

                        /*
                         * Only remove the old installation after the
                         * new one has successfully become active.
                         */
                        "rm -rf " + shellQuote(backupRoot) + "; " +
                        "rm -f " + shellQuote(archive.getAbsolutePath())
                );

                runOnUiThread(() ->
                        progress.setMessage("Verifying Debian..."));

                result = runRoot(
                        "test -x " +
                        shellQuote(METMC_ROOTFS + "/bin/bash") +
                        " && test -x " +
                        shellQuote(
                                METMC_ROOTFS +
                                "/usr/lib/ld-linux-aarch64.so.1"
                        ) +
                        " && chroot " +
                        shellQuote(METMC_ROOTFS) +
                        " /bin/bash -lc " +
                        shellQuote(
                                "echo 'METMC Linux ready'; " +
                                "cat /etc/os-release | " +
                                "grep PRETTY_NAME; " +
                                "uname -m"
                        )
                );

            } catch (Exception e) {
                result = "Installation failed:\n" + e;
            }

            final String finalResult = result;

            runOnUiThread(() -> {
                progress.dismiss();

                if (new File(
                        METMC_ROOTFS + "/bin/bash"
                ).exists()) {
                    new AlertDialog.Builder(this)
                            .setTitle("Debian Ready")
                            .setMessage(
                                    "METMC Linux has been installed.\n\n" +
                                    finalResult
                            )
                            .setPositiveButton(
                                    "Open Linux",
                                    (d, w) -> showLinuxControl()
                            )
                            .show();
                } else {
                    panel(
                            "Debian Installation Failed",
                            finalResult
                    );
                }
            });
        }, "METMC-Debian-Install").start();
    }

    void runLinuxCommand(
        String command,
        LinuxCallback callback) {

        new Thread(() -> {
            String result;

            try {
                String full =
                    "export HOME=/root; " +
                    "export USER=root; " +
                    "export LANG=C.UTF-8; " +
                    "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "export DISPLAY=:100; " +
                    "test -x " + shellQuote(METMC_ROOTFS + "/bin/bash") +
                    " && chroot " + shellQuote(METMC_ROOTFS) +
                    " /bin/bash -lc " +
                    shellQuote(
                        "export HOME=/root; " +
                        "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                        "export DISPLAY=:100; " +
                        command
                    );

                result=runRoot(full);

            } catch(Exception e) {
                result="ERROR: "+e;
            }

            final String r=result;
            runOnUiThread(() -> callback.done(r));
        }).start();
    }

    interface LinuxCallback {
        void done(String result);
    }

    void showLinuxApps() {
        new Thread(() -> {
            try {
                String result = runLinuxCommandSync(
                    "find /usr/share/applications -name '*.desktop' -type f 2>/dev/null | sort"
                );

                String[] files = result.split("\\n");

                runOnUiThread(() -> {
                    final Dialog d = new Dialog(this);

                    LinearLayout box = new LinearLayout(this);
                    box.setOrientation(LinearLayout.VERTICAL);
                    box.setPadding(dp(16),dp(16),dp(16),dp(16));
                    box.setBackgroundColor(PANEL);

                    TextView title = tv("🐧 Debian Applications",22);
                    title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                    box.addView(title,
                        new LinearLayout.LayoutParams(-1,dp(55)));

                    ScrollView scroll = new ScrollView(this);
                    LinearLayout list = new LinearLayout(this);
                    list.setOrientation(LinearLayout.VERTICAL);

                    int count = 0;

                    for(String file : files) {
                        if(file.trim().isEmpty())
                            continue;

                        try {
                            String data = runLinuxCommandSync(
                                "cat " + shellQuote(file)
                            );

                            String name = desktopValue(data,"Name");
                            String exec = desktopValue(data,"Exec");

                            if(name == null || name.trim().isEmpty())
                                continue;

                            if(exec == null || exec.trim().isEmpty())
                                continue;

                            final String appName = name.trim();
                            final String appExec = exec.trim();

                            Button app = btn("▣  " + appName);
                            app.setGravity(
                                Gravity.LEFT | Gravity.CENTER_VERTICAL);

                            app.setOnClickListener(v -> {
                                LinuxGuiLauncher.launch(
                                    MainActivity.this,
                                    METMC_ROOTFS,
                                    "export DISPLAY=:100; " +
                                    "export XDG_RUNTIME_DIR=/tmp/metmc-runtime; " +
                                    "mkdir -p /tmp/metmc-runtime; " +
                                    "chmod 700 /tmp/metmc-runtime; " +
                                    appExec +
                                    " >/tmp/metmc-" +
                                    shellQuote(appName)
                                    + ".log 2>&1 &"
                                );

                                Toast.makeText(
                                    MainActivity.this,
                                    "Opening " + appName,
                                    Toast.LENGTH_SHORT
                                ).show();
                            });

                            list.addView(
                                app,
                                new LinearLayout.LayoutParams(
                                    -1,dp(58)));

                            count++;

                        } catch(Exception ignored) {
                        }
                    }

                    if(count == 0) {
                        TextView empty = tv(
                            "No Debian applications found.\\n\\n" +
                            "Check that /usr/share/applications " +
                            "contains .desktop files.",
                            15);
                        empty.setPadding(
                            dp(10),dp(20),dp(10),dp(20));
                        list.addView(empty);
                    }

                    scroll.addView(list);

                    box.addView(
                        scroll,
                        new LinearLayout.LayoutParams(
                            -1,0,1));

                    Button close = btn("Close");
                    close.setOnClickListener(v -> d.dismiss());

                    box.addView(
                        close,
                        new LinearLayout.LayoutParams(
                            -1,dp(55)));

                    d.setContentView(box);
                    d.show();

                    if(d.getWindow()!=null)
                        d.getWindow().setLayout(
                            dp(700),dp(600));
                });

            } catch(Exception e) {
                runOnUiThread(() ->
                    panel("Debian Applications",
                        "ERROR: " + e));
            }
        }).start();
    }

    String runLinuxCommandSync(String command) throws Exception {
        String full =
            "export HOME=/root; " +
            "export USER=root; " +
            "export LANG=C.UTF-8; " +
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
            "export DISPLAY=:100; " +
            "test -x " + shellQuote(METMC_ROOTFS + "/bin/bash") +
            " && chroot " + shellQuote(METMC_ROOTFS) +
            " /bin/bash -lc " +
            shellQuote(command);

        return runRoot(full);
    }

    String desktopValue(String data,String key) {
        for(String line : data.split("\\n")) {
            if(line.startsWith(key + "="))
                return line.substring(key.length()+1);
        }
        return null;
    }

    void settings() {
        new AlertDialog.Builder(this)
            .setTitle("METMC OS Settings")
            .setItems(new String[]{
                "Display",
                "Desktop",
                "Applications",
                "Linux Subsystem",
                "About METMC OS"
            },(d,which) -> {
                if(which==4)
                    panel("About METMC OS",
                        "METMC OS v6\nAndroid Desktop Environment\n\n"+
                        "Native Java runtime\n64-bit ARM");
                else
                    panel("Settings","Settings module selected.");
            })
            .show();
    }

    void power() {
        new AlertDialog.Builder(this)
            .setTitle("Power")
            .setItems(new String[]{
                "Lock Screen",
                "Restart METMC OS",
                "Exit"
            },(d,w) -> {
                if(w==2) finish();
                if(w==1) {
                    recreate();
                }
            })
            .show();
    }

    com.metmc.os.linux.LinuxDisplayView linuxDisplayView;
    DesktopWindow linuxDisplayWindow;

    void showLinuxDisplayWindow() {
        if (linuxDisplayWindow != null && linuxDisplayWindow.getParent() != null) {
            linuxDisplayWindow.setVisibility(View.VISIBLE);
            linuxDisplayWindow.bringToFront();
            return;
        }

        linuxDisplayView = new com.metmc.os.linux.LinuxDisplayView(this);

        linuxDisplayWindow = new DesktopWindow(
            this,
            desktopArea,
            "Linux Display",
            linuxDisplayView
        );

        desktopArea.addView(linuxDisplayWindow);
        linuxDisplayWindow.bringToFront();

        addRunningTask("Linux Display", this::showLinuxDisplayWindow);
    }

    void tick() {
        if(clock!=null)
            clock.setText(new SimpleDateFormat(
                "EEE  dd MMM  HH:mm:ss",
                Locale.getDefault()).format(new Date()));

        handler.postDelayed(this::tick,1000);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
