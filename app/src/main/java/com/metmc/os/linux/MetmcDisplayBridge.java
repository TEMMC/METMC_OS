package com.metmc.os.linux;

import android.content.Context;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public final class MetmcDisplayBridge extends SurfaceView
        implements SurfaceHolder.Callback {

    private static boolean loaded = false;

    public MetmcDisplayBridge(Context context) {
        super(context);

        if (!loaded) {
            System.loadLibrary("metmc_display");
            loaded = true;
        }

        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startBridge(holder.getSurface());
    }

    @Override
    public void surfaceChanged(
            SurfaceHolder holder,
            int format,
            int width,
            int height) {

        if (width > 0 && height > 0) {
            startBridge(holder.getSurface());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        nativeStop();
    }

    private void startBridge(Surface surface) {
        if (surface == null)
            return;

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0)
            return;

        nativeStart(
                surface,
                width,
                height
        );
    }

    private native boolean nativeStart(
            Surface surface,
            int width,
            int height
    );

    private native void nativeStop();
}
