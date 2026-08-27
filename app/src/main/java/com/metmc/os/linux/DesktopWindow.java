package com.metmc.os.linux;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

/**
 * Compatibility window class.
 *
 * METMC OS now uses MetmcDesktop as the primary desktop/window manager.
 * This class remains so older Linux components can compile.
 */
public class DesktopWindow extends FrameLayout {

    private final String windowTitle;
    private final Button taskButton;

    public DesktopWindow(
            Context context,
            ViewGroup desktop,
            String windowTitle,
            View content
    ) {
        super(context);

        this.windowTitle = windowTitle;

        setBackgroundColor(Color.rgb(30, 32, 40));
        setElevation(20f);

        if (content != null) {
            addView(
                content,
                new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
                )
            );
        }

        taskButton = new Button(context);
        taskButton.setText(windowTitle);
        taskButton.setTextColor(Color.WHITE);
        taskButton.setAllCaps(false);
        taskButton.setTextSize(12f);
    }

    public Button getTaskButton() {
        return taskButton;
    }

    public String getWindowTitle() {
        return windowTitle;
    }
}
