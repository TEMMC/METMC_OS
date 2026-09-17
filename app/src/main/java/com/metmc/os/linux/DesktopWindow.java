package com.metmc.os.linux;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

public class DesktopWindow extends FrameLayout {

    private final FrameLayout desktop;
    private final String title;
    private final Button taskButton;
    private float downX, downY, startX, startY;
    private int oldW, oldH, oldX, oldY;
    private boolean maximized=false;

    public DesktopWindow(Context context, FrameLayout desktop,
                         String title, View content) {
        super(context);

        this.desktop=desktop;
        this.title=title;

        GradientDrawable bg=new GradientDrawable();
        bg.setColor(Color.rgb(35,38,45));
        bg.setCornerRadius(12);
        setBackground(bg);
        setElevation(12);

        LinearLayout root=new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout bar=new LinearLayout(context);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(10,0,4,0);
        bar.setBackgroundColor(Color.rgb(45,48,58));

        TextView name=new TextView(context);
        name.setText(title);
        name.setTextColor(Color.WHITE);
        name.setTextSize(14);
        name.setSingleLine(true);

        Button min=new Button(context);
        min.setText("—");
        min.setTextSize(16);

        Button max=new Button(context);
        max.setText("□");
        max.setTextSize(16);

        Button close=new Button(context);
        close.setText("×");
        close.setTextSize(20);

        bar.addView(name,new LinearLayout.LayoutParams(0,58,1));
        bar.addView(min,new LinearLayout.LayoutParams(58,58));
        bar.addView(max,new LinearLayout.LayoutParams(58,58));
        bar.addView(close,new LinearLayout.LayoutParams(58,58));

        root.addView(bar,new LinearLayout.LayoutParams(-1,58));
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1));

        addView(root,new FrameLayout.LayoutParams(-1,-1));

        taskButton=new Button(context);
        taskButton.setText(title);
        taskButton.setAllCaps(false);

        taskButton.setOnClickListener(v -> {
            if(getVisibility()!=VISIBLE) {
                setVisibility(VISIBLE);
            }
            bringToFront();
        });

        min.setOnClickListener(v -> setVisibility(GONE));

        close.setOnClickListener(v -> {
            if(content instanceof LinuxDisplayView) {
                ((LinuxDisplayView)content).stop();
            }
            desktop.removeView(this);
            ViewParent parent=taskButton.getParent();
            if(parent instanceof ViewGroup) {
                ((ViewGroup)parent).removeView(taskButton);
            }
        });

        max.setOnClickListener(v -> toggleMaximize());

        bar.setOnTouchListener((v,e)->{
            FrameLayout.LayoutParams lp=
                    (FrameLayout.LayoutParams)getLayoutParams();

            switch(e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX=e.getRawX();
                    downY=e.getRawY();
                    startX=lp.leftMargin;
                    startY=lp.topMargin;
                    bringToFront();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if(!maximized) {
                        lp.leftMargin=(int)(startX+e.getRawX()-downX);
                        lp.topMargin=(int)(startY+e.getRawY()-downY);
                        setLayoutParams(lp);
                    }
                    return true;
            }
            return true;
        });
    }

    private void toggleMaximize() {
        FrameLayout.LayoutParams lp=
                (FrameLayout.LayoutParams)getLayoutParams();

        if(!maximized) {
            oldW=lp.width;
            oldH=lp.height;
            oldX=lp.leftMargin;
            oldY=lp.topMargin;

            lp.width=FrameLayout.LayoutParams.MATCH_PARENT;
            lp.height=FrameLayout.LayoutParams.MATCH_PARENT;
            lp.leftMargin=0;
            lp.topMargin=0;

            maximized=true;
        } else {
            lp.width=oldW;
            lp.height=oldH;
            lp.leftMargin=oldX;
            lp.topMargin=oldY;

            maximized=false;
        }

        setLayoutParams(lp);
    }

    public Button getTaskButton() {
        return taskButton;
    }

    public String getWindowTitle() {
        return title;
    }
}
