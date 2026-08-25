package com.metmc.os.linux;

import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class LinuxDisplayView extends SurfaceView
        implements SurfaceHolder.Callback {

    private final LinuxDisplayBridge bridge;

    public LinuxDisplayView(Context context) {
        super(context);

        bridge = new LinuxDisplayBridge();
        getHolder().addCallback(this);

        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        bridge.attach(holder.getSurface());
    }

    @Override
    public void surfaceChanged(
            SurfaceHolder holder,
            int format,
            int width,
            int height) {

        bridge.resize(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        bridge.detach();
    }

    public void start() {
        bridge.start();
    }

    public void stop() {
        bridge.stop();
    }
}
