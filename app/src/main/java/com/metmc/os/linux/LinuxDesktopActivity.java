package com.metmc.os.linux;

import android.content.Intent;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class LinuxDesktopActivity extends Activity {
    private FrameLayout desktop;
    private LinearLayout taskbar;
    private final ArrayList<DesktopWindow> windows=new ArrayList<>();

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);

        FrameLayout root=new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(18,22,28));

        desktop=new FrameLayout(this);
        root.addView(desktop,new FrameLayout.LayoutParams(-1,-1));

        desktop.setOnLongClickListener(v->{
            showDesktopMenu();
            return true;
        });

        taskbar=new LinearLayout(this);
        taskbar.setGravity(Gravity.CENTER_VERTICAL);
        taskbar.setPadding(6,3,6,3);
        taskbar.setBackgroundColor(Color.rgb(28,30,36));

        FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-1,58,Gravity.BOTTOM);
        root.addView(taskbar,tp);
        setContentView(root);

        // Single METMC OS taskbar.
        taskbar.removeAllViews();

        addTaskButton("☰",v->showStartMenu());
        addTaskButton("Terminal",v->openTerminal());
        addTaskButton("Files",v->openFiles());

        String requestedCommand =
                getIntent().getStringExtra("METMC_LINUX_COMMAND");

        String requestedAppName =
                getIntent().getStringExtra("METMC_APP_NAME");

        if (
                requestedCommand != null &&
                !requestedCommand.trim().isEmpty()
        ) {

            String finalName =
                    requestedAppName == null ||
                    requestedAppName.trim().isEmpty()
                            ? "Linux Application"
                            : requestedAppName;

            new Handler().postDelayed(
                    () -> launchLinuxCommand(
                            requestedCommand,
                            finalName
                    ),
                    500
            );

        } else {

            loadLinuxApps();
        }
    }

    private void addTaskButton(String name,View.OnClickListener l){
        Button b=new Button(this);
        b.setText(name);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setAllCaps(false);
        taskbar.addView(b,new LinearLayout.LayoutParams(120,52));
        b.setOnClickListener(l);
    }

    private void addWindowTask(DesktopWindow w){
        taskbar.addView(w.getTaskButton(),
                new LinearLayout.LayoutParams(150,52));
    }

    private void showStartMenu(){
        final PopupWindow p=new PopupWindow(this);
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(15,15,15,15);
        box.setBackgroundColor(Color.rgb(30,32,40));

        Button apps=new Button(this);
        apps.setText("Applications");
        Button terminal=new Button(this);
        terminal.setText("Terminal");
        Button files=new Button(this);
        files.setText("Files");
        Button close=new Button(this);
        close.setText("Close menu");

        box.addView(apps);box.addView(terminal);box.addView(files);box.addView(close);

        apps.setOnClickListener(v->{p.dismiss();showApps();});
        terminal.setOnClickListener(v->{p.dismiss();openTerminal();});
        files.setOnClickListener(v->{p.dismiss();openFiles();});
        close.setOnClickListener(v->p.dismiss());

        p.setContentView(box);
        p.setWidth(500);
        p.setHeight(-2);
        p.setBackgroundDrawable(new ColorDrawable(Color.rgb(30,32,40)));
        p.setOutsideTouchable(true);
        p.setFocusable(true);
        p.showAtLocation(desktop,Gravity.BOTTOM|Gravity.LEFT,6,64);
    }

    private void showDesktopMenu(){
        new AlertDialog.Builder(this)
                .setItems(new String[]{"Refresh applications","Open Terminal","Open Files"},
                        (d,w)->{
                            if(w==0)loadLinuxApps();
                            if(w==1)openTerminal();
                            if(w==2)openFiles();
                        }).show();
    }

    private void showApps(){
        ScrollView s=new ScrollView(this);
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        for(DesktopWindow w:windows){
            Button b=new Button(this);
            b.setText(w.getWindowTitle());
            b.setOnClickListener(v->{
                w.setVisibility(View.VISIBLE);
                w.bringToFront();
            });
            box.addView(b);
        }
        s.addView(box);
        new AlertDialog.Builder(this).setTitle("Open Windows").setView(s)
                .setPositiveButton("Close",null).show();
    }

    private void loadLinuxApps(){
        new Thread(()->{
            try{
                String cmd =
                        "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                        "for f in /usr/share/applications/*.desktop; do " +
                        "[ -f \"$f\" ] || continue; " +
                        "name=$(grep -m1 '^Name=' \"$f\" | cut -d= -f2-); " +
                        "[ -n \"$name\" ] || continue; " +
                        "printf '%s|%s\\n' \"$f\" \"$name\"; " +
                        "done";

                java.lang.Process p = new ProcessBuilder(
                        "su",
                        "-c",
                        "chroot /data/local/linux/rootfs /bin/bash -c " + quote(cmd)
                ).redirectErrorStream(true).start();

                BufferedReader r = new BufferedReader(
                        new InputStreamReader(p.getInputStream())
                );

                ArrayList<String[]> apps = new ArrayList<>();
                String line;

                while((line = r.readLine()) != null){
                    if(line.trim().isEmpty()) continue;

                    String[] parts = line.split("\\|", 2);

                    if(parts.length == 2){
                        apps.add(parts);
                    }
                }

                p.waitFor();

                runOnUiThread(()->{
                    if(apps.isEmpty()){
                        new AlertDialog.Builder(this)
                                .setTitle("🐧 Debian Applications")
                                .setMessage(
                                        "No Debian applications found.\\n\\n" +
                                        "Check that /usr/share/applications " +
                                        "contains .desktop files."
                                )
                                .setPositiveButton("OK", null)
                                .show();
                        return;
                    }

                    for(String[] app : apps){
                        String file = app[0];
                        String name = app[1];

                        addTaskButton(
                                name,
                                v -> launchDesktopFile(file, name)
                        );
                    }
                });

            }catch(Exception e){
                runOnUiThread(()->new AlertDialog.Builder(this)
                        .setTitle("🐧 Debian Applications")
                        .setMessage("Application scan failed:\\n\\n" + e)
                        .setPositiveButton("OK", null)
                        .show());
            }
        }).start();
    }

    private String desktopName(String file){
        try{
            String cmd="grep -m1 '^Name=' "+quote(file)+" | cut -d= -f2-";
            java.lang.Process p=new ProcessBuilder("su","-c",
                    "chroot /data/local/linux/rootfs /bin/bash -lc "+quote(cmd))
                    .redirectErrorStream(true).start();
            BufferedReader r=new BufferedReader(new InputStreamReader(p.getInputStream()));
            String n=r.readLine();
            p.waitFor();
            return n;
        }catch(Exception e){return null;}
    }

    private void ensureOpenbox(){
        try{
            String cmd="export DISPLAY=:100; export HOME=/root; "+
                    "export XDG_RUNTIME_DIR=/tmp/metmc-runtime; "+
                    "mkdir -p \"$XDG_RUNTIME_DIR\"; chmod 700 \"$XDG_RUNTIME_DIR\"; "+
                    "if ! pgrep -x openbox >/dev/null 2>&1; then "+
                    "DISPLAY=:100 openbox >/tmp/metmc-openbox.log 2>&1 & sleep 2; fi";
            new ProcessBuilder("su","-c",
                    "chroot /data/local/linux/rootfs /bin/bash -lc "+quote(cmd)).start();
        }catch(Exception ignored){}
    }


    private void launchLinuxCommand(
            String appCommand,
            String name
    ) {

        new Thread(() -> {

            try {

                String cleanCommand =
                        appCommand
                                .replaceAll(
                                        "\\\\s+%[a-zA-Z]",
                                        ""
                                )
                                .trim();

                String command =
                        "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                        "export HOME=/root; " +
                        "export USER=root; " +
                        "export DISPLAY=:100; " +
                        "export XDG_RUNTIME_DIR=/tmp/metmc-runtime; " +

                        "mkdir -p /tmp/.X11-unix /tmp/metmc-runtime; " +
                        "chmod 1777 /tmp/.X11-unix; " +
                        "chmod 700 /tmp/metmc-runtime; " +

                        "if ! pgrep -x Xvfb >/dev/null 2>&1; then " +
                        "rm -f /tmp/.X100-lock /tmp/.X11-unix/X100; " +
                        "Xvfb :100 -screen 0 1280x720x24 -ac +extension GLX +extension RANDR " +
                        ">/tmp/metmc-xvfb.log 2>&1 & " +
                        "sleep 2; " +
                        "fi; " +

                        "DISPLAY=:100 xdpyinfo >/dev/null 2>&1 || exit 20; " +

                        "if ! pgrep -x openbox >/dev/null 2>&1; then " +
                        "DISPLAY=:100 openbox " +
                        ">/tmp/metmc-openbox.log 2>&1 & " +
                        "sleep 2; " +
                        "fi; " +

                        "DISPLAY=:100 sh -c " +
                        quote(cleanCommand) +
                        " >/tmp/metmc-app.log 2>&1 &";

                new ProcessBuilder(
                        "su",
                        "-c",
                        "chroot /data/local/linux/rootfs " +
                        "/bin/bash -c " +
                        quote(command)
                )
                .redirectErrorStream(true)
                .start();

                runOnUiThread(() -> {

                    LinuxDisplayView display =
                            new LinuxDisplayView(this);

                    DesktopWindow w =
                            new DesktopWindow(
                                    this,
                                    desktop,
                                    name,
                                    display
                            );

                    windows.add(w);
                    desktop.addView(w);
                    addWindowTask(w);

                    w.bringToFront();
                    display.start();
                });

            } catch(Exception e) {

                runOnUiThread(() ->
                        showError(
                                "Failed to launch " +
                                name +
                                "\\n\\n" +
                                e
                        )
                );
            }

        }, "METMC-Linux-Command").start();
    }


    private void launchDesktopFile(String file,String name){

        new Thread(() -> {
            try {

                String execCommand =
                        "grep -m1 '^Exec=' " +
                        quote(file) +
                        " | sed 's/^Exec=//; " +
                        "s/ %F//g; " +
                        "s/ %U//g; " +
                        "s/ %f//g; " +
                        "s/ %u//g; " +
                        "s/ %i//g; " +
                        "s/ %c//g'";

                String command =
                        "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                        "export HOME=/root; " +
                        "export USER=root; " +
                        "export DISPLAY=:100; " +
                        "export XDG_RUNTIME_DIR=/tmp/metmc-runtime; " +

                        "mkdir -p /tmp/.X11-unix /tmp/metmc-runtime; " +
                        "chmod 1777 /tmp/.X11-unix; " +
                        "chmod 700 /tmp/metmc-runtime; " +

                        "if ! pgrep -x Xvfb >/dev/null 2>&1; then " +
                        "rm -f /tmp/.X100-lock /tmp/.X11-unix/X100; " +
                        "Xvfb :100 -screen 0 1280x720x24 -ac +extension GLX +extension RANDR " +
                        ">/tmp/metmc-xvfb.log 2>&1 & " +
                        "sleep 2; " +
                        "fi; " +

                        "DISPLAY=:100 xdpyinfo >/dev/null 2>&1 || exit 20; " +

                        "if ! pgrep -x openbox >/dev/null 2>&1; then " +
                        "DISPLAY=:100 openbox " +
                        ">/tmp/metmc-openbox.log 2>&1 & " +
                        "sleep 2; " +
                        "fi; " +

                        "APP=$(" + execCommand + "); " +
                        "[ -n \"$APP\" ] || exit 21; " +
                        "DISPLAY=:100 sh -c \"$APP\" " +
                        ">/tmp/metmc-app.log 2>&1 &";

                new ProcessBuilder(
                        "su",
                        "-c",
                        "chroot /data/local/linux/rootfs " +
                        "/bin/bash -c " +
                        quote(command)
                )
                .redirectErrorStream(true)
                .start();

                runOnUiThread(() -> {

                    LinuxDisplayView display =
                            new LinuxDisplayView(this);

                    DesktopWindow w =
                            new DesktopWindow(
                                    this,
                                    desktop,
                                    name,
                                    display
                            );

                    windows.add(w);
                    desktop.addView(w);
                    addWindowTask(w);

                    w.bringToFront();
                    display.start();
                });

            } catch(Exception e) {

                runOnUiThread(() ->
                        showError(
                                "Failed to launch " +
                                name +
                                "\n\n" +
                                e
                        )
                );
            }

        }, "METMC-Linux-App").start();
    }

    private void openTerminal(){
        EditText command=new EditText(this);
        command.setSingleLine(true);
        command.setTextColor(Color.WHITE);
        command.setHint("Linux command");
        command.setHintTextColor(Color.GRAY);

        TextView output=new TextView(this);
        output.setTextColor(Color.WHITE);
        output.setBackgroundColor(Color.BLACK);
        output.setPadding(12,12,12,12);
        output.setText("root@debian:~$ ");

        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(output,new LinearLayout.LayoutParams(-1,0,1));
        box.addView(command,new LinearLayout.LayoutParams(-1,55));

        command.setOnEditorActionListener((v,a,e)->{
            String cmd=command.getText().toString().trim();
            if(cmd.isEmpty())return true;
            output.append(cmd+"\n");
            command.setText("");
            new Thread(()->{
                try{
                    String full="export HOME=/root; export DISPLAY=:100; "+cmd;
                    java.lang.Process p=new ProcessBuilder("su","-c",
                            "chroot /data/local/linux/rootfs /bin/bash -lc "+quote(full))
                            .redirectErrorStream(true).start();
                    BufferedReader r=new BufferedReader(new InputStreamReader(p.getInputStream()));
                    StringBuilder out=new StringBuilder();
                    String line;
                    while((line=r.readLine())!=null)out.append(line).append('\n');
                    p.waitFor();
                    runOnUiThread(()->output.append(out.toString()+"root@debian:~$ "));
                }catch(Exception ex){runOnUiThread(()->output.append(ex+"\n"));}}
            ).start();
            return true;
        });

        openWindow("METMC Terminal",box);
        command.requestFocus();
    }

    private void openFiles(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("*/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try{startActivityForResult(i,1001);}catch(Exception e){showError(e.toString());}
    }

    private void openWindow(String title,View content){
        DesktopWindow w=new DesktopWindow(this,desktop,title,content);
        windows.add(w);
        desktop.addView(w);
        addWindowTask(w);
        w.bringToFront();
    }

    private void showError(String s){
        new AlertDialog.Builder(this).setTitle("Linux error")
                .setMessage(s).setPositiveButton("Close",null).show();
    }

    private static String quote(String s){
        return "'"+s.replace("'","'\\''")+"'";
    }
}