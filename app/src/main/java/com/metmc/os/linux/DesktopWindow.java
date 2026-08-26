package com.metmc.os.linux;

import android.content.Context;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

public class DesktopWindow extends FrameLayout {
    private final FrameLayout desktop;
    private final TextView title;
    private final Button taskButton;
    private float downX, downY, startX, startY;
    private boolean maximized=false, minimized=false, resizing=false;
    private int normalW=800, normalH=500, normalX=40, normalY=40;
    private float resizeStartX, resizeStartY;
    private int resizeStartW, resizeStartH;

    public DesktopWindow(Context c, FrameLayout d, String windowTitle, View content) {
        super(c);
        desktop=d;
        setBackgroundColor(Color.rgb(18,18,22));
        setElevation(20);

        LinearLayout container=new LinearLayout(c);
        container.setOrientation(LinearLayout.VERTICAL);

        LinearLayout bar=new LinearLayout(c);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Color.rgb(35,35,42));

        title=new TextView(c);
        title.setText(windowTitle);
        title.setTextColor(Color.WHITE);
        title.setTextSize(14);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(14,0,8,0);
        bar.addView(title,new LinearLayout.LayoutParams(0,48,1));

        Button min=button("—"), max=button("□"), close=button("×");
        bar.addView(min,new LinearLayout.LayoutParams(48,48));
        bar.addView(max,new LinearLayout.LayoutParams(48,48));
        bar.addView(close,new LinearLayout.LayoutParams(48,48));
        container.addView(bar);

        FrameLayout holder=new FrameLayout(c);
        holder.addView(content,new FrameLayout.LayoutParams(-1,-1));
        container.addView(holder,new LinearLayout.LayoutParams(-1,0,1));

        View resize=new View(c);
        resize.setBackgroundColor(Color.TRANSPARENT);
        FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(28,28,Gravity.RIGHT|Gravity.BOTTOM);
        addView(container,new FrameLayout.LayoutParams(-1,-1));
        addView(resize,rp);

        taskButton=button(windowTitle);
        taskButton.setTextSize(12);
        taskButton.setOnClickListener(v->{
            if(minimized){
                setVisibility(VISIBLE);
                minimized=false;
                bringToFront();
            }else{
                setVisibility(INVISIBLE);
                minimized=true;
            }
        });

        setLayoutParams(new FrameLayout.LayoutParams(normalW,normalH));
        setX(normalX);
        setY(normalY);

        bar.setOnTouchListener((v,e)->{
            if(e.getAction()==MotionEvent.ACTION_DOWN){
                bringToFront();
                downX=e.getRawX(); downY=e.getRawY();
                startX=getX(); startY=getY();
                return true;
            }
            if(e.getAction()==MotionEvent.ACTION_MOVE && !maximized){
                setX(Math.max(0,startX+e.getRawX()-downX));
                setY(Math.max(0,startY+e.getRawY()-downY));
                return true;
            }
            return true;
        });

        resize.setOnTouchListener((v,e)->{
            if(e.getAction()==MotionEvent.ACTION_DOWN){
                resizing=true;
                resizeStartX=e.getRawX();
                resizeStartY=e.getRawY();
                resizeStartW=getWidth();
                resizeStartH=getHeight();
                bringToFront();
                return true;
            }
            if(e.getAction()==MotionEvent.ACTION_MOVE && resizing && !maximized){
                int w=Math.max(360,(int)(resizeStartW+e.getRawX()-resizeStartX));
                int h=Math.max(240,(int)(resizeStartH+e.getRawY()-resizeStartY));
                getLayoutParams().width=w;
                getLayoutParams().height=h;
                requestLayout();
                return true;
            }
            if(e.getAction()==MotionEvent.ACTION_UP){
                resizing=false;
                return true;
            }
            return true;
        });

        min.setOnClickListener(v->{
            setVisibility(INVISIBLE);
            minimized=true;
        });
        max.setOnClickListener(v->toggleMaximize());
        close.setOnClickListener(v->{
            desktop.removeView(this);
            if(taskButton.getParent()!=null)
                ((ViewGroup)taskButton.getParent()).removeView(taskButton);
        });

        bar.setOnLongClickListener(v->{
            toggleMaximize();
            return true;
        });

        setOnTouchListener((v,e)->{
            if(e.getAction()==MotionEvent.ACTION_DOWN) bringToFront();
            return false;
        });
    }

    private Button button(String s){
        Button b=new Button(getContext());
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setPadding(0,0,0,0);
        return b;
    }

    private void toggleMaximize(){
        if(!maximized){
            normalX=(int)getX();
            normalY=(int)getY();
            normalW=getWidth();
            normalH=getHeight();
            FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-1,-1);
            p.leftMargin=0; p.topMargin=0;
            setLayoutParams(p);
            setX(0); setY(0);
            maximized=true;
        }else{
            FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(normalW,normalH);
            p.leftMargin=0; p.topMargin=0;
            setLayoutParams(p);
            setX(normalX); setY(normalY);
            maximized=false;
        }
        bringToFront();
    }

    public Button getTaskButton(){ return taskButton; }
    public String getWindowTitle(){ return title.getText().toString(); }
}