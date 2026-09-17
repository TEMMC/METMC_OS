package com.metmc.os.boot;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public final class MetmcStorageBridge {

    private static final String ROOTFS = "/data/local/linux/rootfs";

    private MetmcStorageBridge() {}

    /**
     * Exposes Android's complete /storage tree inside Debian.
     *
     * Linux therefore sees:
     *   /storage/emulated/0       -> Android internal shared storage
     *   /storage/<volume-id>     -> SD/USB/removable volumes
     *
     * The Android storage itself is never copied. Both sides use
     * the same underlying files.
     */
    public static boolean mountSharedStorage() {
        try {
            String script =
                    "set -e; " +
                    "R='" + ROOTFS + "'; " +
                    "mkdir -p \"$R/storage\"; " +
                    "if ! mountpoint -q \"$R/storage\"; then " +
                    "mount --rbind /storage \"$R/storage\"; " +
                    "mount --make-rslave \"$R/storage\"; " +
                    "fi; " +
                    "mkdir -p \"$R/mnt/media_rw\"; " +
                    "if [ -d /mnt/media_rw ]; then " +
                    "mountpoint -q \"$R/mnt/media_rw\" || " +
                    "mount --rbind /mnt/media_rw \"$R/mnt/media_rw\" 2>/dev/null || true; " +
                    "fi; " +
                    "exit 0";

            Process p = new ProcessBuilder(
                    "su", "-c", script
            ).redirectErrorStream(true).start();

            BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream())
            );

            while (r.readLine() != null) {}

            return p.waitFor() == 0;

        } catch (Exception e) {
            return false;
        }
    }
}
