package com.metmc.os.linux;

import android.app.Activity;
import android.widget.Toast;

public final class LinuxGuiLauncher {

    private LinuxGuiLauncher() {}

    public static void launch(
            Activity activity,
            String rootfs,
            String command
    ) {
        if (activity == null ||
                rootfs == null ||
                command == null ||
                command.trim().isEmpty())
            return;

        new Thread(() -> {
            try {
                String shell =
                        "export DISPLAY=:100; " +
                        "export HOME=/root; " +
                        "export USER=root; " +
                        "export LANG=C.UTF-8; " +
                        "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                        "export XDG_RUNTIME_DIR=/tmp/metmc-runtime; " +
                        "mkdir -p \"$XDG_RUNTIME_DIR\"; " +
                        "chmod 700 \"$XDG_RUNTIME_DIR\"; " +

                        // Start Openbox automatically.
                        "if ! pgrep -x openbox >/dev/null 2>&1; then " +
                        "DISPLAY=:100 openbox " +
                        ">/tmp/metmc-openbox.log 2>&1 & " +
                        "sleep 2; " +
                        "fi; " +

                        // Ensure a non-root user exists for running GUI apps.
                        "id metmc >/dev/null 2>&1 || useradd -m -s /bin/bash metmc; " +
                        "chown -R metmc:metmc /home/metmc 2>/dev/null; " +

                        // Launch the requested Linux application as 'metmc', not root.
                        "su -s /bin/bash metmc -c " +
                        quote("export DISPLAY=:100; export HOME=/home/metmc; export USER=metmc; cd /home/metmc; " + command) + ";";

                String chrootCommand =
                        "chroot " + quote(rootfs) +
                        " /bin/bash -lc " +
                        quote(shell);

                Process process = new ProcessBuilder(
                        "su",
                        "-c",
                        chrootCommand
                )
                .redirectErrorStream(true)
                .start();

                int code = process.waitFor();

                final String result =
                        "Linux process exited: " + code;

                activity.runOnUiThread(() ->
                        Toast.makeText(
                                activity,
                                result,
                                Toast.LENGTH_SHORT
                        ).show()
                );

            } catch (Exception e) {

                activity.runOnUiThread(() ->
                        Toast.makeText(
                                activity,
                                "Linux GUI error: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }
        }).start();
    }

    private static String quote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
