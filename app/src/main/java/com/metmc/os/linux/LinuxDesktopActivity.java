package com.metmc.os.linux;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LinuxDesktopActivity extends Activity {

    private FrameLayout desktop;
    private LinearLayout taskbar;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(10,12,16));

        desktop = new FrameLayout(this);
        desktop.setBackgroundColor(Color.rgb(18,22,28));

        root.addView(desktop,
                new FrameLayout.LayoutParams(
                        -1,-1));

        taskbar = new LinearLayout(this);
        taskbar.setOrientation(LinearLayout.HORIZONTAL);
        taskbar.setGravity(Gravity.CENTER_VERTICAL);
        taskbar.setPadding(6,4,6,4);
        taskbar.setBackgroundColor(Color.rgb(28,30,36));

        FrameLayout.LayoutParams taskParams =
                new FrameLayout.LayoutParams(
                        -1,58,
                        Gravity.BOTTOM);

        root.addView(taskbar,taskParams);

        setContentView(root);

        addTaskButton(
                "Terminal",
                v -> openTerminal());

        addTaskButton(
                "Debian",
                v -> openLinuxDisplay());
    }

    private void addTaskButton(
            String name,
            View.OnClickListener listener) {

        Button b = new Button(this);
        b.setText(name);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);

        taskbar.addView(
                b,
                new LinearLayout.LayoutParams(
                        130,52));

        b.setOnClickListener(listener);
    }

    private void openTerminal() {

        TextView content = new TextView(this);
        content.setText(
                "METMC Linux Terminal\n\n" +
                "Use the Terminal button to open the shell."
        );
        content.setTextColor(Color.WHITE);
        content.setTextSize(14);
        content.setPadding(15,15,15,15);
        content.setBackgroundColor(Color.BLACK);

        DesktopWindow window =
                new DesktopWindow(
                        this,
                        desktop,
                        "METMC Terminal",
                        content);

        desktop.addView(window);
        window.bringToFront();
    }

    private void openLinuxDisplay() {

        LinuxDisplayView display =
                new LinuxDisplayView(this);

        DesktopWindow window =
                new DesktopWindow(
                        this,
                        desktop,
                        "Debian Linux",
                        display);

        desktop.addView(window);
        window.bringToFront();

        display.start();
    }

    public void openWindow(
            String title,
            View content) {

        DesktopWindow window =
                new DesktopWindow(
                        this,
                        desktop,
                        title,
                        content);

        desktop.addView(window);
        window.bringToFront();
    }
}
