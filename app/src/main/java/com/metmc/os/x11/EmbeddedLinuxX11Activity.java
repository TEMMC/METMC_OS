package com.metmc.os.x11;

import android.app.Activity;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.view.ViewGroup;

import com.termux.x11.CmdEntryPoint;
import com.termux.x11.LorieView;
import com.termux.x11.MainActivity;

import java.io.File;

/**
 * Hosts the Linux desktop directly in METMC OS.
 * The X server is the embedded Xlorie renderer; no Termux:X11, VNC or
 * screenshot/FFmpeg transport is used.
 */
public final class EmbeddedLinuxX11Activity extends Activity {
    private static final String TAG = "METMC-X11";
    private LorieView display;
    private Process linuxSession;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setNavigationBarColor(0xFF101010);
        getWindow().setStatusBarColor(0xFF101010);
        File tmp = new File(getFilesDir(), "x11tmp");
        tmp.mkdirs();
        new File(tmp, ".X11-unix").mkdirs();

        try {
            Os.setenv("TMPDIR", tmp.getAbsolutePath(), true);
            Os.setenv("XDG_RUNTIME_DIR", tmp.getAbsolutePath(), true);
        } catch (ErrnoException e) {
            Log.e(TAG, "Unable to configure runtime environment", e);
            finish();
            return;
        }

        try {
            CmdEntryPoint server = new CmdEntryPoint();
            if (!server.start(new String[]{":0", "-nolock", "-legacy-drawing", "-nocursor"})) {
                throw new IllegalStateException("Xlorie server did not start");
            }

            MainActivity shim = MainActivity.getInstance();
            shim.initLorieView(this);
            display = shim.getLorieView();
            if (display == null) throw new IllegalStateException("LorieView unavailable");
            display.setFocusable(true);
            display.setFocusableInTouchMode(true);
            display.requestFocus();

            setContentView(display, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            startLinuxDesktop(tmp);
        } catch (Throwable t) {
            Log.e(TAG, "Embedded Linux desktop failed to start", t);
            finish();
        }
    }

    private void startLinuxDesktop(File tmp) {
        final String root = "/data/local/linux/rootfs";
        final String tmpPath = tmp.getAbsolutePath();
        final String command =
                "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                "export HOME=/root USER=root DISPLAY=:0 TMPDIR=" + quote(tmpPath) +
                " XDG_RUNTIME_DIR=" + quote(tmpPath) + "; " +
                "mkdir -p \"$XDG_RUNTIME_DIR\" \"$XDG_RUNTIME_DIR/.X11-unix\"; " +
                "chmod 700 \"$XDG_RUNTIME_DIR\"; " +
                "if command -v dbus-daemon >/dev/null 2>&1; then " +
                "dbus-daemon --session --fork --nopidfile >/tmp/metmc-dbus.log 2>&1 || true; fi; " +
                "if command -v openbox >/dev/null 2>&1 && ! pgrep -x openbox >/dev/null 2>&1; then " +
                "openbox >/tmp/metmc-openbox.log 2>&1 & fi; " +
                "if [ -x /usr/bin/gnome-shell ]; then gnome-shell --wayland >/tmp/metmc-gnome.log 2>&1 & fi; " +
                "wait";
        try {
            linuxSession = new ProcessBuilder("su", "-c",
                    "chroot " + quote(root) + " /bin/bash -lc " + quote(command))
                    .redirectErrorStream(true)
                    .start();
        } catch (Exception e) {
            Log.e(TAG, "Unable to start Linux session", e);
        }
    }

    @Override protected void onDestroy() {
        if (linuxSession != null) {
            try { linuxSession.destroy(); } catch (Throwable ignored) {}
            try { linuxSession.destroyForcibly(); } catch (Throwable ignored) {}
            linuxSession = null;
        }
        super.onDestroy();
    }

    private static String quote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
