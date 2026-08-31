package com.metmc.os.linux;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.BufferedInputStream;
import java.io.InputStream;

public class LinuxDisplayView extends SurfaceView
        implements SurfaceHolder.Callback {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int BPP = 4;
    private static final int FRAME_SIZE = WIDTH * HEIGHT * BPP;

    private volatile boolean running;
    private Thread captureThread;
    private Process ffmpeg;

    private Bitmap bitmap;
    private int[] pixels;
    private final Object bitmapLock = new Object();

    private long lastMove;

    public LinuxDisplayView(Context context) {
        super(context);

        getHolder().addCallback(this);

        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        start();
    }

    @Override
    public void surfaceChanged(
            SurfaceHolder holder,
            int format,
            int width,
            int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stop();
    }

    public synchronized void start() {
        if (running) {
            return;
        }

        running = true;

        captureThread = new Thread(
                this::captureLoop,
                "METMC-Linux-X11-Capture"
        );

        captureThread.start();
    }

    public synchronized void stop() {
        running = false;

        stopFFmpeg();

        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }
    }

    private void captureLoop() {
        while (running) {
            try {
                startFFmpeg();

                if (ffmpeg == null) {
                    throw new Exception("FFmpeg failed to start");
                }

                InputStream input =
                        new BufferedInputStream(
                                ffmpeg.getInputStream(),
                                FRAME_SIZE
                        );

                byte[] frame = new byte[FRAME_SIZE];

                while (running && readFrame(input, frame)) {
                    updateBitmap(frame);
                    postInvalidateOnAnimation();
                }

            } catch (Exception ignored) {
                if (running) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            } finally {
                stopFFmpeg();
            }
        }
    }

    private void startFFmpeg() throws Exception {

        String rootfs = "/data/local/linux/rootfs";

        String command =
                "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                "export HOME=/root; " +
                "export USER=root; " +
                "export DISPLAY=:100; " +
                "export XDG_RUNTIME_DIR=/tmp/metmc-runtime; " +
                "mkdir -p /tmp/metmc-runtime; " +
                "chmod 700 /tmp/metmc-runtime; " +

                "if ! pgrep -x Xvfb >/dev/null 2>&1; then " +
                "Xvfb :100 -screen 0 1280x720x24 -ac " +
                ">/tmp/metmc-xvfb.log 2>&1 & " +
                "sleep 2; " +
                "fi; " +

                "if ! pgrep -x openbox >/dev/null 2>&1; then " +
                "DISPLAY=:100 openbox " +
                ">/tmp/metmc-openbox.log 2>&1 & " +
                "sleep 2; " +
                "fi; " +

                "exec ffmpeg " +
                "-loglevel error " +
                "-f x11grab " +
                "-draw_mouse 1 " +
                "-video_size 1280x720 " +
                "-framerate 30 " +
                "-i :100 " +
                "-pix_fmt rgba " +
                "-f rawvideo -";

        ffmpeg = new ProcessBuilder(
                "su",
                "-c",
                "chroot " + quote(rootfs) +
                        " /bin/bash -lc " + quote(command)
        )
                .redirectErrorStream(false)
                .start();
    }

    private boolean readFrame(InputStream input, byte[] frame)
            throws Exception {

        int offset = 0;

        while (offset < frame.length && running) {
            int count =
                    input.read(
                            frame,
                            offset,
                            frame.length - offset
                    );

            if (count < 0) {
                return false;
            }

            offset += count;
        }

        return offset == frame.length;
    }

    private void updateBitmap(byte[] frame) {

        synchronized (bitmapLock) {

            if (bitmap == null ||
                    bitmap.getWidth() != WIDTH ||
                    bitmap.getHeight() != HEIGHT) {

                bitmap = Bitmap.createBitmap(
                        WIDTH,
                        HEIGHT,
                        Bitmap.Config.ARGB_8888
                );

                pixels = new int[WIDTH * HEIGHT];
            }

            int j = 0;

            for (int i = 0; i < pixels.length; i++) {

                int r = frame[j++] & 0xff;
                int g = frame[j++] & 0xff;
                int b = frame[j++] & 0xff;
                int a = frame[j++] & 0xff;

                pixels[i] =
                        (a << 24) |
                        (r << 16) |
                        (g << 8) |
                        b;
            }

            bitmap.setPixels(
                    pixels,
                    0,
                    WIDTH,
                    0,
                    0,
                    WIDTH,
                    HEIGHT
            );
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        canvas.drawColor(Color.BLACK);

        synchronized (bitmapLock) {

            if (bitmap != null) {

                canvas.drawBitmap(
                        bitmap,
                        null,
                        new android.graphics.Rect(
                                0,
                                0,
                                getWidth(),
                                getHeight()
                        ),
                        null
                );
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (event.isCtrlPressed()) {

            if (keyCode == KeyEvent.KEYCODE_C) {
                sendKey("ctrl+c");
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_V) {
                sendKey("ctrl+v");
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_A) {
                sendKey("ctrl+a");
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_X) {
                sendKey("ctrl+x");
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_Z) {
                sendKey("ctrl+z");
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_Y) {
                sendKey("ctrl+y");
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_S) {
                sendKey("ctrl+s");
                return true;
            }
        }

        String key =
                KeyEvent.keyCodeToString(keyCode)
                        .replace("KEYCODE_", "")
                        .toLowerCase();

        sendKey(key);

        return true;
    }

    private void sendKey(String key) {

        new Thread(() -> {

            try {

                String command =
                        "export DISPLAY=:100; " +
                        "xdotool key " + quote(key);

                new ProcessBuilder(
                        "su",
                        "-c",
                        "chroot /data/local/linux/rootfs " +
                                "/bin/bash -lc " +
                                quote(command)
                ).start();

            } catch (Exception ignored) {
            }

        }, "METMC-X11-Key").start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        requestFocus();

        float scaleX = WIDTH / (float) Math.max(1, getWidth());
        float scaleY = HEIGHT / (float) Math.max(1, getHeight());

        int x = Math.max(
                0,
                Math.min(
                        WIDTH - 1,
                        (int) (event.getX() * scaleX)
                )
        );

        int y = Math.max(
                0,
                Math.min(
                        HEIGHT - 1,
                        (int) (event.getY() * scaleY)
                )
        );

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:

                sendX11(
                        "mousemove --sync " +
                        x + " " + y +
                        " && xdotool mousedown 1"
                );

                return true;

            case MotionEvent.ACTION_MOVE:

                long now = System.currentTimeMillis();

                if (now - lastMove >= 16) {

                    lastMove = now;

                    sendX11(
                            "mousemove " +
                            x + " " + y
                    );
                }

                return true;

            case MotionEvent.ACTION_UP:

                sendX11(
                        "mousemove --sync " +
                        x + " " + y +
                        " && xdotool mouseup 1"
                );

                performClick();

                return true;

            case MotionEvent.ACTION_CANCEL:

                sendX11("mouseup 1");

                return true;
        }

        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void sendX11(String action) {

        new Thread(() -> {

            try {

                String command =
                        "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                        "export DISPLAY=:100; " +
                        "export HOME=/root; " +
                        "export XDG_RUNTIME_DIR=/tmp/metmc-runtime; " +
                        "xdotool " + action;

                new ProcessBuilder(
                        "su",
                        "-c",
                        "chroot /data/local/linux/rootfs " +
                        "/bin/bash -c " +
                        quote(command)
                )
                .redirectErrorStream(true)
                .start()
                .waitFor();

            } catch (Exception ignored) {
            }

        }, "METMC-X11-Input").start();
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {

        if ((event.getSource() &
                InputDevice.SOURCE_CLASS_POINTER) != 0 &&
                event.getAction() == MotionEvent.ACTION_SCROLL) {

            float scroll =
                    event.getAxisValue(
                            MotionEvent.AXIS_VSCROLL
                    );

            sendMouse(
                    scroll > 0
                            ? "click 4"
                            : "click 5"
            );

            return true;
        }

        return super.onGenericMotionEvent(event);
    }


    private synchronized void stopFFmpeg() {

        if (ffmpeg != null) {

            try {
                ffmpeg.destroy();
            } catch (Exception ignored) {
            }

            try {
                ffmpeg.destroyForcibly();
            } catch (Exception ignored) {
            }

            ffmpeg = null;
        }
    }

    private static String quote(String value) {

        return "'" +
                value.replace(
                        "'",
                        "'\\''"
                ) +
                "'";
    }
}
