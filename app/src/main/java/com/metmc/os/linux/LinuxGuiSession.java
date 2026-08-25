package com.metmc.os.linux;

import android.content.Context;
import android.widget.FrameLayout;

public final class LinuxGuiSession {

    private final Context context;
    private final LinuxDisplayView display;

    public LinuxGuiSession(Context context) {
        this.context = context;
        this.display = new LinuxDisplayView(context);
    }

    public FrameLayout createView() {
        FrameLayout root = new FrameLayout(context);
        root.addView(
            display,
            new FrameLayout.LayoutParams(
                -1,
                -1
            )
        );
        return root;
    }

    public void start() {
        display.start();
    }

    public void stop() {
        display.stop();
    }

    public LinuxDisplayView getDisplay() {
        return display;
    }
}
