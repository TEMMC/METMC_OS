package com.metmc.os.desktop;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MetmcWindowManager {

    private final Context context;
    private final ViewGroup desktop;
    private final List<View> windows = new ArrayList<>();

    public MetmcWindowManager(Context context, ViewGroup desktop) {
        this.context = context;
        this.desktop = desktop;
    }

    public View createWindow(String title, View content) {

        LinearLayout window = new LinearLayout(context);
        window.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(30, 30, 35));
        background.setCornerRadius(18);
        window.setBackground(background);

        TextView titleBar = new TextView(context);
        titleBar.setText("  " + title);
        titleBar.setTextColor(Color.WHITE);
        titleBar.setTextSize(15);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setBackgroundColor(Color.rgb(45, 45, 52));

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(42)
                );

        window.addView(titleBar, titleParams);

        window.addView(
                content,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        ViewGroup.LayoutParams params =
                new ViewGroup.LayoutParams(
                        dp(320),
                        dp(240)
                );

        desktop.addView(window, params);

        window.setX(dp(30 + windows.size() * 20));
        window.setY(dp(70 + windows.size() * 20));

        windows.add(window);

        titleBar.setOnTouchListener(new View.OnTouchListener() {

            float downX;
            float downY;
            float startX;
            float startY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        startX = window.getX();
                        startY = window.getY();

                        window.bringToFront();
                        return true;

                    case MotionEvent.ACTION_MOVE:

                        window.setX(
                                startX + event.getRawX() - downX
                        );

                        window.setY(
                                startY + event.getRawY() - downY
                        );

                        return true;
                }

                return true;
            }
        });

        window.setOnClickListener(v -> window.bringToFront());

        return window;
    }

    public void closeWindow(View window) {
        desktop.removeView(window);
        windows.remove(window);
    }

    public void minimizeWindow(View window) {
        window.setVisibility(View.GONE);
    }

    public void restoreWindow(View window) {
        window.setVisibility(View.VISIBLE);
        window.bringToFront();
    }

    public void maximizeWindow(View window) {

        window.setX(0);
        window.setY(0);

        ViewGroup.LayoutParams params =
                window.getLayoutParams();

        params.width = -1;
        params.height = -1;

        window.setLayoutParams(params);

        window.bringToFront();
    }

    private int dp(int value) {
        return (int) (
                value *
                context.getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}
