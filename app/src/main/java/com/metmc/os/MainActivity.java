package com.metmc.os;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    LinearLayout root, desktop, dock;
    TextView clock, status;
    Handler handler = new Handler();

    int BG = Color.rgb(12,13,18);
    int PANEL = Color.rgb(24,25,32);
    int PANEL2 = Color.rgb(32,33,42);
    int WHITE = Color.WHITE;
    int GRAY = Color.rgb(175,178,190);

    int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + .5f);
    }

    TextView tv(String s, float size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(WHITE);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    Button btn(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setBackgroundColor(PANEL2);
        return b;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setStatusBarColor(Color.rgb(8,8,10));
        getWindow().setNavigationBarColor(Color.rgb(8,8,10));

        build();
        tick();
    }

    void build() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // TOP BAR
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(16),0,dp(16),0);
        top.setBackgroundColor(PANEL);

        TextView logo = tv("◈  METMC OS",18);
        logo.setTypeface(Typeface.DEFAULT,Typeface.BOLD);

        top.addView(logo,new LinearLayout.LayoutParams(0,-1,1));

        clock = tv("",14);
        top.addView(clock,new LinearLayout.LayoutParams(-2,-1));

        root.addView(top,new LinearLayout.LayoutParams(-1,dp(52)));

        // DESKTOP
        FrameLayout area = new FrameLayout(this);
        area.setBackgroundColor(BG);

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);

        TextView title = tv("METMC OS",38);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);

        TextView sub = tv("Android Desktop Environment",16);
        sub.setGravity(Gravity.CENTER);
        sub.setTextColor(GRAY);

        TextView ready = tv("● System Ready",14);
        ready.setGravity(Gravity.CENTER);
        ready.setTextColor(Color.rgb(100,220,140));

        center.addView(title,new LinearLayout.LayoutParams(-1,dp(55)));
        center.addView(sub,new LinearLayout.LayoutParams(-1,dp(35)));
        center.addView(ready,new LinearLayout.LayoutParams(-1,dp(35)));

        area.addView(center,new FrameLayout.LayoutParams(-1,-1));

        // DOCK
        dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(8),dp(5),dp(8),dp(5));
        dock.setBackgroundColor(PANEL);

        addDock("☰\nApps", v -> showApps());
        addDock("▣\nFiles", v -> openFiles());
        addDock("🐧\nLinux", v -> linuxPanel());
        addDock("⚙\nSettings", v -> settings());
        addDock("⏻\nPower", v -> power());

        FrameLayout.LayoutParams dp =
            new FrameLayout.LayoutParams(-1,dp(76));
        dp.gravity = Gravity.BOTTOM;
        dp.setMargins(dp(55),0,dp(55),dp(12));

        area.addView(dock,dp);

        root.addView(area,new LinearLayout.LayoutParams(-1,0,1));

        // STATUS
        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(dp(14),0,dp(14),0);
        bottom.setBackgroundColor(PANEL);

        status = tv("● ONLINE",13);
        status.setTextColor(Color.rgb(100,220,140));

        TextView device = tv("  Android Desktop",13);
        device.setTextColor(GRAY);

        bottom.addView(status,new LinearLayout.LayoutParams(0,-1,1));
        bottom.addView(device,new LinearLayout.LayoutParams(-2,-1));

        root.addView(bottom,new LinearLayout.LayoutParams(-1,dp(36)));

        setContentView(root);
    }

    void addDock(String label, View.OnClickListener click) {
        Button b=btn(label);
        b.setGravity(Gravity.CENTER);
        b.setOnClickListener(click);
        dock.addView(b,new LinearLayout.LayoutParams(dp(105),dp(65)));
    }

    void panel(String title,String message) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Close",null)
            .show();
    }

    void showApps() {
        final Dialog d=new Dialog(this);
        d.getWindow();

        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20),dp(20),dp(20),dp(20));
        box.setBackgroundColor(PANEL);

        TextView h=tv("METMC OS • Applications",22);
        h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        box.addView(h,new LinearLayout.LayoutParams(-1,dp(55)));

        EditText search=new EditText(this);
        search.setHint("Search applications");
        search.setHintTextColor(GRAY);
        search.setTextColor(WHITE);
        box.addView(search,new LinearLayout.LayoutParams(-1,dp(55)));

        ScrollView scroll=new ScrollView(this);
        LinearLayout list=new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        PackageManager pm=getPackageManager();
        Intent query=new Intent(Intent.ACTION_MAIN,null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> apps=pm.queryIntentActivities(query,0);

        for(ResolveInfo info:apps) {
            String name=info.loadLabel(pm).toString();

            Button b=btn("▣  "+name);
            b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
            b.setOnClickListener(v -> {
                try {
                    Intent launch=pm.getLaunchIntentForPackage(
                        info.activityInfo.packageName);
                    if(launch!=null) startActivity(launch);
                    d.dismiss();
                } catch(Exception e) {
                    panel("Launch error",e.toString());
                }
            });

            list.addView(b,new LinearLayout.LayoutParams(-1,dp(58)));
        }

        scroll.addView(list);
        box.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        d.setContentView(box);
        Window w=d.getWindow();
        if(w!=null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(dp(700),dp(500));
        }
        d.show();
        if(d.getWindow()!=null)
            d.getWindow().setLayout(dp(700),dp(500));
    }

    void openFiles() {
        try {
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("*/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i,100);
        } catch(Exception e) {
            panel("Files","Android file picker unavailable.");
        }
    }

    void linuxPanel() {
        new AlertDialog.Builder(this)
            .setTitle("METMC Linux")
            .setMessage(
                "Linux subsystem\n\n"+
                "Status: Ready\n"+
                "Architecture: arm64\n"+
                "Display: Android Surface\n\n"+
                "Linux integration layer is reserved for the next subsystem build."
            )
            .setPositiveButton("Start", (d,w) ->
                panel("METMC Linux","Linux subsystem control initialized.")
            )
            .setNegativeButton("Close",null)
            .show();
    }

    void settings() {
        new AlertDialog.Builder(this)
            .setTitle("METMC OS Settings")
            .setItems(new String[]{
                "Display",
                "Desktop",
                "Applications",
                "Linux Subsystem",
                "About METMC OS"
            },(d,which) -> {
                if(which==4)
                    panel("About METMC OS",
                        "METMC OS v6\nAndroid Desktop Environment\n\n"+
                        "Native Java runtime\n64-bit ARM");
                else
                    panel("Settings","Settings module selected.");
            })
            .show();
    }

    void power() {
        new AlertDialog.Builder(this)
            .setTitle("Power")
            .setItems(new String[]{
                "Lock Screen",
                "Restart METMC OS",
                "Exit"
            },(d,w) -> {
                if(w==2) finish();
                if(w==1) {
                    recreate();
                }
            })
            .show();
    }

    void tick() {
        if(clock!=null)
            clock.setText(new SimpleDateFormat(
                "EEE  dd MMM  HH:mm:ss",
                Locale.getDefault()).format(new Date()));

        handler.postDelayed(this::tick,1000);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
