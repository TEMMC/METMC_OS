package com.metmc.os.linux;

import android.view.Surface;

public final class LinuxDisplayBridge {

    static {
        System.loadLibrary("metmc_display");
    }

    private Surface surface;

    public void attach(Surface surface) {
        this.surface = surface;

        if (surface != null) {
            nativeAttach(surface);
        }
    }

    public void detach() {
        nativeDetach();
        surface = null;
    }

    public void resize(int width, int height) {
        nativeResize(width, height);
    }

    public void start() {
        nativeStart();
    }

    public void stop() {
        nativeStop();
    }

    public static native void nativeAttach(Surface surface);
    public static native void nativeDetach();
    public static native void nativeResize(int width, int height);
    public static native void nativeStart();
    public static native void nativeStop();

    private LinuxDisplayBridge() {
    }
}
