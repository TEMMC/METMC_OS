package com.metmc.os.linux;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DesktopWindow extends FrameLayout {

    private float downX, downY;
    private float startX, startY;
    private boolean maximized = false;

    private int normalW = 800;
    private int normalH = 500;
    private int normalX = 40;
    private int normalY = 40;

    private final FrameLayout desktop;
    private final TextView title;

    public DesktopWindow(
            Context context,
            FrameLayout desktop,
            String windowTitle,
            View content) {

        super(context);

        this.desktop = desktop;

        setBackgroundColor(Color.rgb(18,18,22));
        setElevation(20);
        setPadding(1,1,1,1);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        LinearLayout bar = new LinearLayout(context);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Color.rgb(35,35,42));

        title = new TextView(context);
        title.setText(windowTitle);
        title.setTextColor(Color.WHITE);
        title.setTextSize(14);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(14,0,8,0);

        bar.addView(title,
                new LinearLayout.LayoutParams(0,48,1));

        Button minimize = button("—");
        Button maximize = button("□");
        Button close = button("×");

        bar.addView(minimize,
                new LinearLayout.LayoutParams(48,48));
        bar.addView(maximize,
                new LinearLayout.LayoutParams(48,48));
        bar.addView(close,
                new LinearLayout.LayoutParams(48,48));

        container.addView(bar);

        FrameLayout contentHolder = new FrameLayout(context);
        contentHolder.addView(content,
                new FrameLayout.LayoutParams(
                        -1,-1));

        container.addView(contentHolder,
                new LinearLayout.LayoutParams(
                        -1,0,1));

        addView(container,
                new FrameLayout.LayoutParams(-1,-1));

        setWindowPosition();

        bar.setOnTouchListener((v,event) -> {
            switch(event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN:
                    bringToFront();
                    downX = event.getRawX();
                    downY = event.getRawY();
                    startX = getX();
                    startY = getY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (!maximized) {
                        setX(startX + event.getRawX() - downX);
                        setY(startY + event.getRawY() - downY);
                    }
                    return true;
            }

            return true;
        });

        minimize.setOnClickListener(v ->
                setVisibility(INVISIBLE));

        maximize.setOnClickListener(v ->
                toggleMaximize());

        close.setOnClickListener(v ->
                desktop.removeView(this));

        setOnClickListener(v -> bringToFront());
    }

    private Button button(String text) {
        Button b = new Button(getContext());
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(16);
        b.setPadding(0,0,0,0);
        return b;
    }

    private void setWindowPosition() {
        FrameLayout.LayoutParams lp =
                new FrameLayout.LayoutParams(
                        normalW,
                        normalH);

        lp.leftMargin = normalX;
        lp.topMargin = normalY;

        setLayoutParams(lp);
    }

    private void toggleMaximize() {

        if (!maximized) {

            normalX = (int)getX();
            normalY = (int)getY();
            normalW = getWidth();
            normalH = getHeight();

            FrameLayout.LayoutParams lp =
                    new FrameLayout.LayoutParams(
                            -1,-1);

            lp.leftMargin = 0;
            lp.topMargin = 0;

            setLayoutParams(lp);

            maximized = true;

        } else {

            setWindowPosition();
            maximized = false;
        }
    }

    public String getWindowTitle() {
        return title.getText().toString();
    }
}
