package com.metmc.os.x11;

import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class NativeDisplayView extends SurfaceView
        implements SurfaceHolder.Callback {

    static {
        System.loadLibrary("metmc_display");
    }

    private long nativeHandle;

    public NativeDisplayView(Context context) {
        super(context);

        getHolder().addCallback(this);

        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        nativeHandle = nativeCreate();
        nativeSetSurface(nativeHandle, getHolder().getSurface());
        nativeStart(nativeHandle);
    }

    @Override
    public void surfaceChanged(
            SurfaceHolder holder,
            int format,
            int width,
            int height
    ) {
        if (nativeHandle != 0) {
            nativeResize(nativeHandle, width, height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (nativeHandle != 0) {
            nativeStop(nativeHandle);
            nativeDestroy(nativeHandle);
            nativeHandle = 0;
        }
    }

    public void sendPointer(
            float x,
            float y,
            int action
    ) {
        if (nativeHandle != 0) {
            nativePointer(
                    nativeHandle,
                    x,
                    y,
                    action
            );
        }
    }

    public void sendKey(
            int keyCode,
            boolean pressed
    ) {
        if (nativeHandle != 0) {
            nativeKey(
                    nativeHandle,
                    keyCode,
                    pressed
            );
        }
    }

    private static native long nativeCreate();

    private static native void nativeDestroy(
            long handle
    );

    private static native void nativeSetSurface(
            long handle,
            android.view.Surface surface
    );

    private static native void nativeResize(
            long handle,
            int width,
            int height
    );

    private static native void nativeStart(
            long handle
    );

    private static native void nativeStop(
            long handle
    );

    private static native void nativePointer(
            long handle,
            float x,
            float y,
            int action
    );

    private static native void nativeKey(
            long handle,
            int keyCode,
            boolean pressed
    );
}
