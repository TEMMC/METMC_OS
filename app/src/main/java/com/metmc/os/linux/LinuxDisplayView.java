package com.metmc.os.linux;

import android.content.Context;
import android.graphics.*;
import android.view.*;

import java.io.*;
import java.util.concurrent.*;

public class LinuxDisplayView extends SurfaceView implements SurfaceHolder.Callback {
    private static final int W=1280,H=720,BPP=4,FRAME=W*H*BPP;
    private volatile boolean running;
    private Thread captureThread;
    private Process ffmpeg;
    private Bitmap bitmap;
    private final Object lock=new Object();
    private long lastMove;

    public LinuxDisplayView(Context c){
        super(c);
        getHolder().addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
    }

    public void surfaceCreated(SurfaceHolder h){ start(); }
    public void surfaceChanged(SurfaceHolder h,int f,int w,int he){}
    public void surfaceDestroyed(SurfaceHolder h){ stop(); }

    public synchronized void start(){
        if(running)return;
        running=true;
        captureThread=new Thread(this::captureLoop,"METMC-X11-Stream");
        captureThread.start();
    }

    public synchronized void stop(){
        running=false;
        stopFFmpeg();
        if(captureThread!=null){
            captureThread.interrupt();
            captureThread=null;
        }
    }

    private void captureLoop(){
        while(running){
            try{
                startFFmpeg();
                if(ffmpeg==null)throw new Exception("FFmpeg unavailable");
                InputStream in=new BufferedInputStream(ffmpeg.getInputStream(),FRAME);
                byte[] frame=new byte[FRAME];
                while(running && readFrame(in,frame)){
                    updateBitmap(frame);
                    postInvalidateOnAnimation();
                }
            }catch(Exception ignored){
                if(running)try{Thread.sleep(300);}catch(Exception e){break;}
            }finally{stopFFmpeg();}
        }
    }

    private void startFFmpeg() throws Exception{
        String cmd="export DISPLAY=:100; export HOME=/root; "+
                "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; "+
                "exec ffmpeg -loglevel error -f x11grab -draw_mouse 1 "+
                "-video_size 1280x720 -framerate 30 -i :100 "+
                "-pix_fmt rgba -f rawvideo -";
        ffmpeg=new ProcessBuilder("su","-c",
                "chroot /data/local/linux/rootfs /bin/bash -lc "+quote(cmd))
                .redirectErrorStream(false).start();
    }

    private boolean readFrame(InputStream in,byte[] b)throws Exception{
        int off=0,n;
        while(off<b.length && running){
            n=in.read(b,off,b.length-off);
            if(n<0)return false;
            off+=n;
        }
        return off==b.length;
    }

    private void updateBitmap(byte[] f){
        int[] p=new int[W*H];
        int j=0;
        for(int i=0;i<p.length;i++){
            int r=f[j++]&255,g=f[j++]&255,b=f[j++]&255,a=f[j++]&255;
            p[i]=(a<<24)|(r<<16)|(g<<8)|b;
        }
        synchronized(lock){
            if(bitmap==null||bitmap.getWidth()!=W||bitmap.getHeight()!=H)
                bitmap=Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888);
            bitmap.setPixels(p,0,W,0,0,W,H);
        }
    }

    @Override public boolean onKeyDown(int code,KeyEvent e){
        if(e.isCtrlPressed()){
            if(code==KeyEvent.KEYCODE_C){xkey("ctrl+c");return true;}
            if(code==KeyEvent.KEYCODE_V){xkey("ctrl+v");return true;}
            if(code==KeyEvent.KEYCODE_A){xkey("ctrl+a");return true;}
            if(code==KeyEvent.KEYCODE_X){xkey("ctrl+x");return true;}
            if(code==KeyEvent.KEYCODE_Z){xkey("ctrl+z");return true;}
            if(code==KeyEvent.KEYCODE_Y){xkey("ctrl+y");return true;}
            if(code==KeyEvent.KEYCODE_S){xkey("ctrl+s");return true;}
        }
        xkey(KeyEvent.keyCodeToString(code).replace("KEYCODE_","").toLowerCase());
        return true;
    }

    private void xkey(String key){
        new Thread(()->{
            try{
                String cmd="export DISPLAY=:100; "+
                        "command -v xdotool >/dev/null 2>&1 && xdotool key "+quote(key);
                new ProcessBuilder("su","-c",
                        "chroot /data/local/linux/rootfs /bin/bash -lc "+quote(cmd)).start();
            }catch(Exception ignored){}
        }).start();
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        float x=e.getX()/Math.max(1,getWidth())*W;
        float y=e.getY()/Math.max(1,getHeight())*H;
        int action=e.getActionMasked();

        if(action==MotionEvent.ACTION_DOWN){
            requestFocus();
            sendMouse("mousemove "+(int)x+" "+(int)y+" mousedown 1");
            return true;
        }

        if(action==MotionEvent.ACTION_MOVE){
            long now=System.currentTimeMillis();
            if(now-lastMove>16){
                lastMove=now;
                sendMouse("mousemove "+(int)x+" "+(int)y);
            }
            return true;
        }

        if(action==MotionEvent.ACTION_UP){
            sendMouse("mousemove "+(int)x+" "+(int)y+" mouseup 1");
            return true;
        }

        return true;
    }

    public boolean onGenericMotionEvent(MotionEvent e){
        if((e.getSource()&InputDevice.SOURCE_CLASS_POINTER)!=0 &&
                e.getAction()==MotionEvent.ACTION_SCROLL){
            float v=e.getAxisValue(MotionEvent.AXIS_VSCROLL);
            sendMouse(v>0?"click 4":"click 5");
            return true;
        }
        return super.onGenericMotionEvent(e);
    }

    private void sendMouse(String action){
        new Thread(()->{
            try{
                String cmd="export DISPLAY=:100; "+
                        "command -v xdotool >/dev/null 2>&1 && xdotool "+action;
                new ProcessBuilder("su","-c",
                        "chroot /data/local/linux/rootfs /bin/bash -lc "+quote(cmd)).start();
            }catch(Exception ignored){}
        }).start();
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        c.drawColor(Color.BLACK);
        Bitmap b;
        synchronized(lock){b=bitmap;}
        if(b!=null)c.drawBitmap(b,null,new Rect(0,0,getWidth(),getHeight()),null);
    }

    private synchronized void stopFFmpeg(){
        if(ffmpeg!=null){
            try{ffmpeg.destroy();}catch(Exception ignored){}
            try{ffmpeg.destroyForcibly();}catch(Exception ignored){}
            ffmpeg=null;
        }
    }

    private static String quote(String s){
        return "'"+s.replace("'","'\\''")+"'";
    }
}