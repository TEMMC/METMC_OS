package com.metmc.os.linux;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

public final class LinuxGuiLauncher {

    private LinuxGuiLauncher() {}

    public static void launch(
            Activity activity,
            String rootfs,
            String command
    ) {
        if (activity == null || command == null || command.trim().isEmpty())
            return;

        new Thread(() -> {
            try {
                String shell =
                        "export HOME=/root; " +
                        "export USER=root; " +
                        "export LANG=C.UTF-8; " +
                        "export DISPLAY=${DISPLAY:-:0}; " +
                        "export XDG_RUNTIME_DIR=/tmp/metmc-runtime; " +
                        "mkdir -p \"$XDG_RUNTIME_DIR\"; " +
                        "chmod 700 \"$XDG_RUNTIME_DIR\"; " +
                        "cd /root; " +
                        command;

                Process process = new ProcessBuilder(
                        "/system/bin/su",
                        "-c",
                        "chroot " + quote(rootfs) +
                        " /bin/bash -lc " + quote(shell)
                ).redirectErrorStream(true).start();

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
        return " + value.replace(", "\) + ";
    }
}
