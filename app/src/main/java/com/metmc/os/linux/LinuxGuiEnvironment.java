package com.metmc.os.linux;

import java.io.File;

public final class LinuxGuiEnvironment {

    private LinuxGuiEnvironment() {}

    public static boolean rootfsExists(String rootfs) {
        return new File(rootfs, "bin/bash").exists();
    }

    public static boolean x11SocketExists() {
        String[] sockets = {
            "/data/data/com.termux/files/usr/tmp/.X11-unix/X0",
            "/data/data/com.termux/files/usr/tmp/.X11-unix/X1",
            "/tmp/.X11-unix/X0"
        };

        for (String socket : sockets) {
            if (new File(socket).exists())
                return true;
        }

        return false;
    }

    public static String detectDisplay() {
        if (new File("/data/data/com.termux/files/usr/tmp/.X11-unix/X0").exists())
            return ":0";

        if (new File("/data/data/com.termux/files/usr/tmp/.X11-unix/X1").exists())
            return ":1";

        if (new File("/tmp/.X11-unix/X0").exists())
            return ":0";

        return null;
    }
}
