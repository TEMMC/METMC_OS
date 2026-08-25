package com.metmc.os;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private final Handler handler = new Handler();
    private TextView clock;
    private TextView battery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(10,10,10));
        window.setNavigationBarColor(Color.rgb(10,10,10));

        buildDesktop();
        updateClock();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateClock();
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, float size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setBackgroundColor(Color.rgb(35,35,40));
        return b;
    }

    private void buildDesktop() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(12,12,16));

        // TOP BAR
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(14), 0, dp(14), 0);
        top.setBackgroundColor(Color.rgb(20,20,25));

        TextView logo = text("METMC OS", 18, Color.WHITE);
        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        top.addView(logo, new LinearLayout.LayoutParams(
                0, -1, 1
        ));

        clock = text("", 14, Color.WHITE);
        top.addView(clock, new LinearLayout.LayoutParams(
                -2, -1
        ));

        root.addView(top, new LinearLayout.LayoutParams(
                -1, dp(52)
        ));

        // DESKTOP
        FrameLayout desktop = new FrameLayout(this);
        desktop.setBackgroundColor(Color.rgb(15,16,22));

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);

        TextView welcome = text("METMC OS", 38, Color.WHITE);
        welcome.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        TextView version = text("Version 6 • Android Desktop", 16,
                Color.rgb(180,180,190));

        TextView ready = text("System ready", 14,
                Color.rgb(120,220,150));

        center.addView(welcome, new LinearLayout.LayoutParams(
                -1, -2
        ));

        center.addView(version, new LinearLayout.LayoutParams(
                -1, -2
        ));

        center.addView(ready, new LinearLayout.LayoutParams(
                -1, -2
        ));

        desktop.addView(center, new FrameLayout.LayoutParams(
                -1, -1
        ));

        // DOCK
        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(8), dp(6), dp(8), dp(6));
        dock.setBackgroundColor(Color.rgb(24,24,30));

        Button apps = button("Apps");
        Button files = button("Files");
        Button linux = button("Linux");
        Button settings = button("Settings");
        Button power = button("Power");

        dock.addView(apps, new LinearLayout.LayoutParams(
                dp(110), dp(55)
        ));

        dock.addView(files, new LinearLayout.LayoutParams(
                dp(110), dp(55)
        ));

        dock.addView(linux, new LinearLayout.LayoutParams(
                dp(110), dp(55)
        ));

        dock.addView(settings, new LinearLayout.LayoutParams(
                dp(110), dp(55)
        ));

        dock.addView(power, new LinearLayout.LayoutParams(
                dp(110), dp(55)
        ));

        FrameLayout.LayoutParams dockParams =
                new FrameLayout.LayoutParams(-1, dp(70));

        dockParams.gravity = Gravity.BOTTOM;
        dockParams.setMargins(dp(80), 0, dp(80), dp(15));

        desktop.addView(dock, dockParams);

        // BUTTON ACTIONS

        apps.setOnClickListener(v ->
                showMessage("METMC OS App Launcher",
                        "Application launcher is ready for integration."));

        files.setOnClickListener(v ->
                showMessage("Files",
                        "File manager integration will be added next."));

        linux.setOnClickListener(v ->
                showMessage("Linux",
                        "Linux subsystem interface is ready for integration."));

        settings.setOnClickListener(v ->
                showMessage("Settings",
                        "METMC OS system settings."));

        power.setOnClickListener(v ->
                showMessage("Power",
                        "Power and session controls."));

        root.addView(desktop, new LinearLayout.LayoutParams(
                -1, 0, 1
        ));

        // BOTTOM STATUS BAR
        LinearLayout status = new LinearLayout(this);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setPadding(dp(12), 0, dp(12), 0);
        status.setBackgroundColor(Color.rgb(20,20,25));

        TextView statusText = text("● ONLINE", 13,
                Color.rgb(100,220,140));

        battery = text("Battery", 13, Color.WHITE);

        status.addView(statusText, new LinearLayout.LayoutParams(
                0, -1, 1
        ));

        status.addView(battery, new LinearLayout.LayoutParams(
                -2, -1
        ));

        root.addView(status, new LinearLayout.LayoutParams(
                -1, dp(38)
        ));

        setContentView(root);
    }

    private void updateClock() {
        if (clock != null) {
            String value = new SimpleDateFormat(
                    "EEE, dd MMM  HH:mm:ss",
                    Locale.getDefault()
            ).format(new Date());

            clock.setText(value);
        }
    }

    private void showMessage(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
