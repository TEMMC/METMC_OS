package com.metmc.os.linux;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.*;
import java.util.*;

public class LinuxDesktopActivity extends Activity {

    private FrameLayout desktop;
    private LinearLayout taskbar;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(10,12,16));

        desktop = new FrameLayout(this);
        desktop.setBackgroundColor(Color.rgb(18,22,28));

        root.addView(desktop,
                new FrameLayout.LayoutParams(
                        -1,-1));

        taskbar = new LinearLayout(this);
        taskbar.setOrientation(LinearLayout.HORIZONTAL);
        taskbar.setGravity(Gravity.CENTER_VERTICAL);
        taskbar.setPadding(6,4,6,4);
        taskbar.setBackgroundColor(Color.rgb(28,30,36));

        FrameLayout.LayoutParams taskParams =
                new FrameLayout.LayoutParams(
                        -1,58,
                        Gravity.BOTTOM);

        root.addView(taskbar,taskParams);

        setContentView(root);

        addTaskButton(
                "Terminal",
                v -> openTerminal());

        addTaskButton(
                "Debian",
                v -> openLinuxDisplay());

        loadLinuxApps();
    }

    private void addTaskButton(
            String name,
            View.OnClickListener listener) {

        Button b = new Button(this);
        b.setText(name);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setSingleLine(true);

        taskbar.addView(
                b,
                new LinearLayout.LayoutParams(
                        130,52));

        b.setOnClickListener(listener);
    }

    private void loadLinuxApps() {

        new Thread(() -> {

            try {
                Process p = new ProcessBuilder(
                        "su", "-c",
                        "chroot /data/local/linux/rootfs " +
                        "/bin/bash -lc " +
                        quote(
                            "find /usr/share/applications " +
                            "-type f -name '*.desktop' " +
                            "2>/dev/null | sort"
                        )
                ).redirectErrorStream(true).start();

                BufferedReader r =
                        new BufferedReader(
                                new InputStreamReader(
                                        p.getInputStream()));

                ArrayList<String> apps =
                        new ArrayList<>();

                String line;

                while ((line = r.readLine()) != null) {
                    if (!line.trim().isEmpty())
                        apps.add(line.trim());
                }

                p.waitFor();

                runOnUiThread(() -> {

                    for (String desktopFile : apps) {

                        String name =
                                desktopName(desktopFile);

                        if (name == null ||
                                name.trim().isEmpty())
                            continue;

                        addTaskButton(
                                name,
                                v -> launchDesktopFile(
                                        desktopFile,
                                        name));
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        addTaskButton(
                                "Linux error",
                                v -> openErrorWindow(
                                        e.toString())));
            }

        }, "METMC-AppScanner").start();
    }

    private String desktopName(String file) {

        try {

            Process p = new ProcessBuilder(
                    "su","-c",
                    "chroot /data/local/linux/rootfs " +
                    "/bin/bash -lc " +
                    quote(
                        "grep -m1 '^Name=' " +
                        quote(file) +
                        " | cut -d= -f2-"
                    )
            ).redirectErrorStream(true).start();

            BufferedReader r =
                    new BufferedReader(
                            new InputStreamReader(
                                    p.getInputStream()));

            String name = r.readLine();

            p.waitFor();

            return name;

        } catch (Exception e) {
            return null;
        }
    }

    private void launchDesktopFile(
            String desktopFile,
            String name) {

        new Thread(() -> {

            try {

                Process p = new ProcessBuilder(
                        "su","-c",
                        "chroot /data/local/linux/rootfs " +
                        "/bin/bash -lc " +
                        quote(
                            "export HOME=/root; " +
                            "export USER=root; " +
                            "export DISPLAY=:100; " +
                            "export XDG_RUNTIME_DIR=/tmp/metmc-runtime; " +
                            "mkdir -p \"$XDG_RUNTIME_DIR\"; " +
                            "chmod 700 \"$XDG_RUNTIME_DIR\"; " +
                            "exec gtk-launch " +
                            quote(
                                new File(desktopFile)
                                    .getName()
                                    .replace(".desktop","")
                            )
                        )
                ).redirectErrorStream(true).start();

                runOnUiThread(() -> {

                    TextView status =
                            new TextView(this);

                    status.setText(
                            name +
                            "\\nLinux application started on DISPLAY=:100");

                    status.setTextColor(Color.WHITE);
                    status.setPadding(20,20,20,20);
                    status.setBackgroundColor(Color.BLACK);

                    DesktopWindow window =
                            new DesktopWindow(
                                    this,
                                    desktop,
                                    name,
                                    status);

                    desktop.addView(window);
                    window.bringToFront();
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        openErrorWindow(
                                "Failed to launch " +
                                name + "\\n\\n" +
                                e));
            }

        }, "METMC-LinuxLauncher").start();
    }

    private void openErrorWindow(String message) {

        TextView text = new TextView(this);
        text.setText(message);
        text.setTextColor(Color.WHITE);
        text.setPadding(20,20,20,20);
        text.setBackgroundColor(Color.BLACK);

        DesktopWindow window =
                new DesktopWindow(
                        this,
                        desktop,
                        "METMC Linux Error",
                        text);

        desktop.addView(window);
        window.bringToFront();
    }

    private static String quote(String value) {
        return "'" +
                value.replace(
                        "'",
                        "'\\''") +
                "'";
    }

    private void openTerminal() {

        TextView content = new TextView(this);
        content.setText(
                "METMC Linux Terminal\n\n" +
                "Use the Terminal button to open the shell."
        );
        content.setTextColor(Color.WHITE);
        content.setTextSize(14);
        content.setPadding(15,15,15,15);
        content.setBackgroundColor(Color.BLACK);

        DesktopWindow window =
                new DesktopWindow(
                        this,
                        desktop,
                        "METMC Terminal",
                        content);

        desktop.addView(window);
        window.bringToFront();
    }

    private void openLinuxDisplay() {

        LinuxDisplayView display =
                new LinuxDisplayView(this);

        DesktopWindow window =
                new DesktopWindow(
                        this,
                        desktop,
                        "Debian Linux",
                        display);

        desktop.addView(window);
        window.bringToFront();

        display.start();
    }

    public void openWindow(
            String title,
            View content) {

        DesktopWindow window =
                new DesktopWindow(
                        this,
                        desktop,
                        title,
                        content);

        desktop.addView(window);
        window.bringToFront();
    }
}
