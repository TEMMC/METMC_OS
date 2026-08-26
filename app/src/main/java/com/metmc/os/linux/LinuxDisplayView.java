package com.metmc.os.linux;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.MotionEvent;
import android.view.KeyEvent;

import java.io.BufferedInputStream;
import java.io.InputStream;

public class LinuxDisplayView extends SurfaceView
        implements SurfaceHolder.Callback {

    private static final int X11_WIDTH = 1280;
    private static final int X11_HEIGHT = 720;
    private static final int BYTES_PER_PIXEL = 4;
    private static final int FRAME_SIZE =
            X11_WIDTH * X11_HEIGHT * BYTES_PER_PIXEL;

    private volatile boolean running;
    private Thread captureThread;
    private Process ffmpeg;
    private Bitmap bitmap;
    private final Object bitmapLock = new Object();

    public LinuxDisplayView(Context context) {
        super(context);

        getHolder().addCallback(this);

        setFocusable(true);
        setFocusableInTouchMode(true);
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
        if (running)
            return;

        running = true;

        captureThread = new Thread(
                this::captureLoop,
                "METMC-X11-Stream"
        );

        captureThread.start();
    }

    public synchronized void stop() {
        running = false;

        stopFFmpeg();

        if (captureThread != null) {
            captureThread.interrupt();

            if (Thread.currentThread() != captureThread) {
                try {
                    captureThread.join(1500);
                } catch (InterruptedException ignored) {
                }
            }

            captureThread = null;
        }
    }

    private void captureLoop() {

        while (running) {

            try {
                startFFmpeg();

                if (ffmpeg == null)
                    throw new Exception("FFmpeg did not start");

                InputStream input =
                        new BufferedInputStream(
                                ffmpeg.getInputStream(),
                                FRAME_SIZE
                        );

                byte[] frame = new byte[FRAME_SIZE];

                while (running) {

                    if (!readFrame(input, frame))
                        break;

                    updateBitmap(frame);
                    postInvalidateOnAnimation();
                }

            } catch (Exception e) {

                if (running) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                        break;
                    }
                }

            } finally {
                stopFFmpeg();
            }
        }
    }

    private void startFFmpeg() throws Exception {

        String command =
                "export DISPLAY=:100; " +
                "export HOME=/root; " +
                "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                "exec ffmpeg " +
                "-loglevel error " +
                "-f x11grab " +
                "-draw_mouse 1 " +
                "-video_size 1280x720 " +
                "-framerate 30 " +
                "-i :100 " +
                "-pix_fmt rgba " +
                "-f rawvideo " +
                "-";

        ffmpeg = new ProcessBuilder(
                "su",
                "-c",
                "chroot /data/local/linux/rootfs " +
                "/bin/bash -lc " +
                quote(command)
        )
                .redirectErrorStream(false)
                .start();

        // Give FFmpeg a moment to initialize.
        Thread.sleep(150);
    }

    private boolean readFrame(
            InputStream input,
            byte[] buffer) throws Exception {

        int offset = 0;

        while (offset < buffer.length && running) {

            int n = input.read(
                    buffer,
                    offset,
                    buffer.length - offset
            );

            if (n < 0)
                return false;

            if (n == 0)
                continue;

            offset += n;
        }

        return offset == buffer.length;
    }

    private void updateBitmap(byte[] frame) {

        Bitmap newBitmap;

        synchronized (bitmapLock) {

            if (bitmap == null ||
                    bitmap.getWidth() != X11_WIDTH ||
                    bitmap.getHeight() != X11_HEIGHT) {

                bitmap = Bitmap.createBitmap(
                        X11_WIDTH,
                        X11_HEIGHT,
                        Bitmap.Config.ARGB_8888
                );
            }

            newBitmap = bitmap;
        }

        newBitmap.setPixels(
                rgbaToArgb(frame),
                0,
                X11_WIDTH,
                0,
                0,
                X11_WIDTH,
                X11_HEIGHT
        );
    }

    /*
     * X11/FFmpeg RGBA -> Android ARGB.
     *
     * The returned array is reused for the current frame.
     */
    private int[] rgbaToArgb(byte[] rgba) {

        int[] pixels =
                new int[X11_WIDTH * X11_HEIGHT];

        int p = 0;

        for (int i = 0; i < pixels.length; i++) {

            int r = rgba[p++] & 0xff;
            int g = rgba[p++] & 0xff;
            int b = rgba[p++] & 0xff;
            int a = rgba[p++] & 0xff;

            pixels[i] =
                    (a << 24) |
                    (r << 16) |
                    (g << 8) |
                    b;
        }

        return pixels;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        String key = KeyEvent.keyCodeToString(keyCode)
                .replace("KEYCODE_", "")
                .toLowerCase();

        sendX11Key(key);
        return true;
    }

    private void sendX11Key(String key) {
        new Thread(() -> {
            try {
                String cmd =
                        "export DISPLAY=:100; " +
                        "export HOME=/root; " +
                        "if command -v xdotool >/dev/null 2>&1; then " +
                        "xdotool key " + quote(key) + "; " +
                        "fi";

                new ProcessBuilder(
                        "su", "-c",
                        "chroot /data/local/linux/rootfs /bin/bash -lc " +
                        quote(cmd)
                ).start();
            } catch (Exception ignored) {
            }
        }).start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN ||
            event.getAction() == MotionEvent.ACTION_MOVE ||
            event.getAction() == MotionEvent.ACTION_UP) {

            final float x = event.getX() / Math.max(1f, getWidth()) * X11_WIDTH;
            final float y = event.getY() / Math.max(1f, getHeight()) * X11_HEIGHT;

            String action;

            if (event.getAction() == MotionEvent.ACTION_DOWN)
                action = "mousemove " + (int)x + " " + (int)y + " click 1";
            else if (event.getAction() == MotionEvent.ACTION_UP)
                action = "mousemove " + (int)x + " " + (int)y;
            else
                action = "mousemove " + (int)x + " " + (int)y;

            new Thread(() -> {
                try {
                    String cmd =
                            "export DISPLAY=:100; " +
                            "export HOME=/root; " +
                            "command -v xdotool >/dev/null 2>&1 && " +
                            "xdotool " + action;

                    new ProcessBuilder(
                            "su", "-c",
                            "chroot /data/local/linux/rootfs /bin/bash -lc " +
                            quote(cmd)
                    ).start();
                } catch (Exception ignored) {
                }
            }).start();

            return true;
        }

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        canvas.drawColor(Color.BLACK);

        Bitmap current;

        synchronized (bitmapLock) {
            current = bitmap;
        }

        if (current == null)
            return;

        Rect src = new Rect(
                0,
                0,
                current.getWidth(),
                current.getHeight()
        );

        Rect dst = new Rect(
                0,
                0,
                getWidth(),
                getHeight()
        );

        canvas.drawBitmap(
                current,
                src,
                dst,
                null
        );
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
