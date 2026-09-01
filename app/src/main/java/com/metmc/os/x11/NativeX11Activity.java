package com.metmc.os.x11;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.KeyEvent;

public class NativeX11Activity extends Activity {

    private NativeDisplayView display;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        display = new NativeDisplayView(this);

        setContentView(display);
    }

    @Override
    public boolean dispatchTouchEvent(
            MotionEvent event
    ) {
        if (display != null) {
            display.sendPointer(
                    event.getX(),
                    event.getY(),
                    event.getActionMasked()
            );
        }

        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(
            KeyEvent event
    ) {
        if (display != null) {
            display.sendKey(
                    event.getKeyCode(),
                    event.getAction()
                            == KeyEvent.ACTION_DOWN
            );
        }

        return super.dispatchKeyEvent(event);
    }
}
