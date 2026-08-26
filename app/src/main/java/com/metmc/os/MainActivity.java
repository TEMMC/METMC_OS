package com.metmc.os;

import com.metmc.os.linux.LinuxGuiEnvironment;
import com.metmc.os.linux.LinuxGuiLauncher;

import com.metmc.os.desktop.AndroidWindowLauncher;
import com.metmc.os.desktop.MetmcDesktop;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.zip.GZIPInputStream;

public class MainActivity extends Activity {

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

        MetmcDesktop desktopView = new MetmcDesktop(this);
        setContentView(desktopView);
        tick();
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

        // TOP BAR
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(16),0,dp(16),0);
        top.setBackgroundColor(PANEL);

        TextView logo = tv("◈  METMC OS",18);
        logo.setTypeface(Typeface.DEFAULT,Typeface.BOLD);

        top.addView(logo,new LinearLayout.LayoutParams(0,-1,1));

        clock = tv("",14);
        top.addView(clock,new LinearLayout.LayoutParams(-2,-1));

        root.addView(top,new LinearLayout.LayoutParams(-1,dp(52)));

        // DESKTOP
        FrameLayout area = new FrameLayout(this);
        area.setBackgroundColor(BG);

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);

        TextView title = tv("METMC OS",38);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);

        TextView sub = tv("Android Desktop Environment",16);
        sub.setGravity(Gravity.CENTER);
        sub.setTextColor(GRAY);

        TextView ready = tv("● System Ready",14);
        ready.setGravity(Gravity.CENTER);
        ready.setTextColor(Color.rgb(100,220,140));

        center.addView(title,new LinearLayout.LayoutParams(-1,dp(55)));
        center.addView(sub,new LinearLayout.LayoutParams(-1,dp(35)));
        center.addView(ready,new LinearLayout.LayoutParams(-1,dp(35)));

        area.addView(center,new FrameLayout.LayoutParams(-1,-1));

        // DOCK
        dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(8),dp(5),dp(8),dp(5));
        dock.setBackgroundColor(PANEL);

        addDock("☰\nApps", v -> showApps());
        addDock("▣\nFiles", v -> openFiles());
        addDock("🐧\nLinux", v -> linuxPanel());
        addDock("⚙\nSettings", v -> settings());
        addDock("⏻\nPower", v -> power());

        FrameLayout.LayoutParams dp =
            new FrameLayout.LayoutParams(-1,dp(76));
        dp.gravity = Gravity.BOTTOM;
        dp.setMargins(dp(55),0,dp(55),dp(12));

        area.addView(dock,dp);

        root.addView(area,new LinearLayout.LayoutParams(-1,0,1));

        // STATUS
        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(dp(14),0,dp(14),0);
        bottom.setBackgroundColor(PANEL);

        status = tv("● ONLINE",13);
        status.setTextColor(Color.rgb(100,220,140));

        TextView device = tv("  Android Desktop",13);
        device.setTextColor(GRAY);

        bottom.addView(status,new LinearLayout.LayoutParams(0,-1,1));
        bottom.addView(device,new LinearLayout.LayoutParams(-2,-1));

        root.addView(bottom,new LinearLayout.LayoutParams(-1,dp(36)));

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
        final Dialog d=new Dialog(this);
        d.getWindow();

        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20),dp(20),dp(20),dp(20));
        box.setBackgroundColor(PANEL);

        TextView h=tv("METMC OS • Applications",22);
        h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        box.addView(h,new LinearLayout.LayoutParams(-1,dp(55)));

        EditText search=new EditText(this);
        search.setHint("Search applications");
        search.setHintTextColor(GRAY);
        search.setTextColor(WHITE);
        box.addView(search,new LinearLayout.LayoutParams(-1,dp(55)));

        ScrollView scroll=new ScrollView(this);
        LinearLayout list=new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        PackageManager pm=getPackageManager();
        Intent query=new Intent(Intent.ACTION_MAIN,null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> apps=pm.queryIntentActivities(query,0);

        for(ResolveInfo info:apps) {
            String name=info.loadLabel(pm).toString();

            Button b=btn("▣  "+name);
            b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
            b.setOnClickListener(v -> {
                try {
                    Intent launch=pm.getLaunchIntentForPackage(
                        info.activityInfo.packageName);
                    if(launch!=null) {
                        AndroidWindowLauncher.launch(
                            MainActivity.this,
                            info.activityInfo.packageName
                        );
                    }
                } catch(Exception e) {
                    panel("Launch error",e.toString());
                }
            });

            list.addView(b,new LinearLayout.LayoutParams(-1,dp(58)));
        }

        scroll.addView(list);
        box.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        d.setContentView(box);
        Window w=d.getWindow();
        if(w!=null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(dp(700),dp(500));
        }
        d.show();
        if(d.getWindow()!=null)
            d.getWindow().setLayout(dp(700),dp(500));
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
        "https://github.com/debuerreotype/docker-debian-artifacts/raw/refs/heads/dist-arm64v8/bookworm/rootfs.tar.xz";

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
                "Debian Linux environment was not found.\\n\\n" +
                "METMC OS can install the ARM64 Debian environment " +
                "into:\\n" + METMC_ROOTFS +
                "\\n\\nRoot access is required.")
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
                "METMC OS will create the Linux environment and " +
                "download the Debian ARM64 root filesystem.\\n\\n" +
                "This can require several hundred MB of download " +
                "and additional storage after extraction.")
            .setPositiveButton("Continue", (d,w) -> startDebianInstall())
            .setNegativeButton("Cancel",null)
            .show();
    }

    boolean hasRoot() {
        try {
            java.lang.Process p = rootProcess("id");
            return p.waitFor() == 0;
        } catch(Exception e) {
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
                File archive = new File(base,"debian-bookworm-arm64.tar.xz");

                runRoot(
                    "mkdir -p " + shellQuote(METMC_LINUX) +
                    " " + shellQuote(METMC_ROOTFS)
                );

                runOnUiThread(() -> {
                    progress.setIndeterminate(false);
                    progress.setProgress(0);
                    progress.setMessage("Downloading Debian...");
                });

                downloadFile(DEBIAN_URL, archive, progress);

                runOnUiThread(() ->
                    progress.setMessage("Extracting Debian..."));

                runRoot(
                    "rm -rf " + shellQuote(METMC_ROOTFS) +
                    "/* " +
                    "&& tar -xJf " + shellQuote(archive.getAbsolutePath()) +
                    " -C " + shellQuote(METMC_ROOTFS)
                );

                runOnUiThread(() ->
                    progress.setMessage("Configuring Debian..."));

                runRoot(
                    "mkdir -p " + shellQuote(METMC_ROOTFS + "/proc") +
                    " " + shellQuote(METMC_ROOTFS + "/sys") +
                    " " + shellQuote(METMC_ROOTFS + "/dev") +
                    " " + shellQuote(METMC_ROOTFS + "/tmp") +
                    " " + shellQuote(METMC_ROOTFS + "/run") +
                    " && chmod 1777 " + shellQuote(METMC_ROOTFS + "/tmp") +
                    " && printf '%s\\n' " +
                    "'nameserver 1.1.1.1' " +
                    "'nameserver 8.8.8.8' " +
                    "> " + shellQuote(METMC_ROOTFS + "/etc/resolv.conf")
                );

                runRoot(
                    "rm -f " + shellQuote(archive.getAbsolutePath())
                );

                runOnUiThread(() ->
                    progress.setMessage("Verifying Debian..."));

                final String check = runRoot(
                    "test -x " + shellQuote(METMC_ROOTFS + "/bin/bash") +
                    " && chroot " + shellQuote(METMC_ROOTFS) +
                    " /bin/bash -lc " +
                    shellQuote(
                        "echo 'METMC Linux ready'; " +
                        "cat /etc/os-release | grep PRETTY_NAME; " +
                        "uname -m"
                    )
                );

                result = check;

            } catch(Exception e) {
                result = "Installation failed:\\n" + e;
            }

            final String finalResult = result;

            runOnUiThread(() -> {
                progress.dismiss();

                if (new File(METMC_ROOTFS + "/bin/bash").exists()) {
                    new AlertDialog.Builder(this)
                        .setTitle("Debian Ready")
                        .setMessage(
                            "METMC Linux has been installed.\\n\\n" +
                            finalResult)
                        .setPositiveButton("Open Linux", (d,w) ->
                            showLinuxControl())
                        .show();
                } else {
                    panel("Debian Installation Failed", finalResult);
                }
            });

        }).start();
    }

    void downloadFile(
        String urlString,
        File target,
        ProgressDialog progress) throws Exception {

        URL url = new URL(urlString);
        HttpURLConnection c =
            (HttpURLConnection)url.openConnection();

        c.setConnectTimeout(30000);
        c.setReadTimeout(60000);
        c.setInstanceFollowRedirects(true);
        c.connect();

        int size = c.getContentLength();

        if(size > 0) {
            progress.setIndeterminate(false);
            progress.setMax(100);
        }

        try(
            InputStream in = new BufferedInputStream(c.getInputStream());
            FileOutputStream out = new FileOutputStream(target)
        ) {
            byte[] buffer = new byte[1024 * 128];
            long done = 0;
            int n;

            while((n=in.read(buffer))!=-1) {
                out.write(buffer,0,n);
                done += n;

                if(size > 0) {
                    int percent=(int)((done*100)/size);
                    runOnUiThread(() ->
                        progress.setProgress(percent));
                }
            }

            out.flush();
        } finally {
            c.disconnect();
        }
    }



    String shellQuote(String s) {
        return "'" + s.replace("'","'\\\\''") + "'";
    }

    void showLinuxControl() {
        final Dialog d = new Dialog(this);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20),dp(20),dp(20),dp(20));
        box.setBackgroundColor(PANEL);

        TextView title = tv("🐧 METMC Linux • Debian",22);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        box.addView(title,new LinearLayout.LayoutParams(-1,dp(55)));

        Button shell = btn("▣ Debian Terminal");
        box.addView(shell,new LinearLayout.LayoutParams(-1,dp(58)));

        Button apps = btn("▦ Linux Applications");
        box.addView(apps,new LinearLayout.LayoutParams(-1,dp(58)));

        Button update = btn("↻ Update Debian");
        box.addView(update,new LinearLayout.LayoutParams(-1,dp(58)));

        Button info = btn("● Linux Status");
        box.addView(info,new LinearLayout.LayoutParams(-1,dp(58)));

        Button close = btn("Close");
        box.addView(close,new LinearLayout.LayoutParams(-1,dp(58)));

        shell.setOnClickListener(v -> showLinuxShell());

        apps.setOnClickListener(v -> showLinuxApps());

        update.setOnClickListener(v ->
            runLinuxCommand(
                "apt-get update",
                r -> panel("Debian",r)));

        info.setOnClickListener(v ->
            runLinuxCommand(
                "cat /etc/os-release; echo; uname -m",
                r -> panel("Linux Status",r)));

        Button desktop = btn("Open Linux Desktop");
        desktop.setOnClickListener(v ->
            startActivity(
                new Intent(
                    MainActivity.this,
                    com.metmc.os.linux.LinuxDesktopActivity.class
                )
            )
        );
        box.addView(
            desktop,
            new LinearLayout.LayoutParams(-1,dp(55))
        );

        close.setOnClickListener(v -> d.dismiss());

        d.setContentView(box);
        d.show();

        if(d.getWindow()!=null)
            d.getWindow().setLayout(dp(650),dp(500));
    }

    void showLinuxShell() {
        final Dialog d = new Dialog(this);

        LinearLayout terminal = new LinearLayout(this);
        terminal.setOrientation(LinearLayout.VERTICAL);
        terminal.setBackgroundColor(Color.rgb(12, 13, 16));

        // Terminal title bar
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), 0, dp(6), 0);
        bar.setBackgroundColor(Color.rgb(30, 32, 38));

        TextView icon = tv("●", 13);
        icon.setTextColor(Color.rgb(80, 210, 120));

        TextView title = tv("  Debian Terminal", 15);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        bar.addView(icon,
            new LinearLayout.LayoutParams(dp(25), dp(48)));

        bar.addView(title,
            new LinearLayout.LayoutParams(0, dp(48), 1));

        Button clear = btn("Clear");
        clear.setTextSize(12);
        bar.addView(clear,
            new LinearLayout.LayoutParams(dp(70), dp(42)));

        Button close = btn("×");
        close.setTextSize(20);
        bar.addView(close,
            new LinearLayout.LayoutParams(dp(48), dp(42)));

        terminal.addView(
            bar,
            new LinearLayout.LayoutParams(-1, dp(50))
        );

        // Terminal output
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(8, 9, 11));

        TextView output = new TextView(this);
        output.setText(
            "METMC OS Linux Terminal\n" +
            "Debian ARM64 environment\n" +
            "──────────────────────────────\n\n" +
            "root@metmc:~# "
        );

        output.setTextColor(Color.rgb(225, 230, 235));
        output.setTextSize(14);
        output.setTypeface(Typeface.MONOSPACE);
        output.setPadding(
            dp(14), dp(14), dp(14), dp(20)
        );

        scroll.addView(output);

        terminal.addView(
            scroll,
            new LinearLayout.LayoutParams(-1, 0, 1)
        );

        // Command input row
        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setPadding(
            dp(10), dp(6), dp(10), dp(6)
        );
        inputRow.setBackgroundColor(Color.rgb(25, 27, 32));

        TextView prompt = tv("root@metmc:~$ ", 13);
        prompt.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        prompt.setTextColor(Color.rgb(90, 220, 130));

        inputRow.addView(
            prompt,
            new LinearLayout.LayoutParams(
                -2, dp(52)
            )
        );

        EditText command = new EditText(this);
        command.setSingleLine(true);
        command.setTextColor(Color.WHITE);
        command.setHintTextColor(Color.rgb(110, 115, 125));
        command.setHint("command");
        command.setTextSize(14);
        command.setTypeface(Typeface.MONOSPACE);
        command.setPadding(0, 0, 0, 0);
        command.setBackgroundColor(Color.TRANSPARENT);

        inputRow.addView(
            command,
            new LinearLayout.LayoutParams(
                0, dp(52), 1
            )
        );

        Button run = btn("▶");
        run.setTextSize(15);
        run.setContentDescription("Run command");

        inputRow.addView(
            run,
            new LinearLayout.LayoutParams(
                dp(55), dp(48)
            )
        );

        terminal.addView(
            inputRow,
            new LinearLayout.LayoutParams(-1, dp(64))
        );

        Runnable execute = () -> {
            String cmd = command.getText().toString().trim();

            if (cmd.length() == 0)
                return;

            output.append(
                cmd + "\n"
            );

            command.setText("");

            runLinuxCommand(cmd, result -> {
                output.append(
                    result + "\n\nroot@metmc:~# "
                );

                scroll.post(() ->
                    scroll.fullScroll(View.FOCUS_DOWN)
                );

                command.requestFocus();
            });
        };

        run.setOnClickListener(v -> execute.run());

        command.setOnEditorActionListener((v, actionId, event) -> {
            if (event != null &&
                event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {

                execute.run();
                return true;
            }

            return false;
        });

        clear.setOnClickListener(v -> {
            output.setText("root@metmc:~# ");
            command.requestFocus();
        });

        close.setOnClickListener(v -> d.dismiss());

        d.setContentView(terminal);
        d.setTitle("Debian Terminal");

        d.setOnShowListener(v -> {
            Window w = d.getWindow();

            if (w != null) {
                w.setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
                );

                w.setLayout(
                    dp(760),
                    dp(520)
                );
            }

            command.requestFocus();
        });

        d.show();

        if (d.getWindow() != null) {
            d.getWindow().setLayout(
                dp(760),
                dp(520)
            );
        }
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
