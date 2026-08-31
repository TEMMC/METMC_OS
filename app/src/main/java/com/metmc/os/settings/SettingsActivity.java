package com.metmc.os.settings;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density);
    }

    private TextView text(String value, float size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(Color.WHITE);
        t.setTextSize(size);
        t.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private LinearLayout section(String title) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(14), dp(18), dp(14));

        TextView heading = text(title, 17);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        box.addView(
            heading,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        );

        return box;
    }

    private void addSwitch(
        LinearLayout parent,
        String title,
        boolean checked
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), 0, dp(8), 0);

        TextView label = text(title, 16);

        Switch toggle = new Switch(this);
        toggle.setChecked(checked);

        row.addView(
            label,
            new LinearLayout.LayoutParams(
                0,
                dp(56),
                1
            )
        );

        row.addView(
            toggle,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(56)
            )
        );

        parent.addView(row);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(18, 20, 26));

        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(dp(18), 0, dp(8), 0);
        titleBar.setBackgroundColor(Color.rgb(45, 48, 61));

        TextView title = text("METMC Settings", 18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        titleBar.addView(
            title,
            new LinearLayout.LayoutParams(
                0,
                dp(54),
                1
            )
        );

        TextView close = text("×", 30);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> finish());

        titleBar.addView(
            close,
            new LinearLayout.LayoutParams(
                dp(60),
                dp(54)
            )
        );

        root.addView(
            titleBar,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54)
            )
        );

        ScrollView scroll = new ScrollView(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(10), dp(10), dp(10), dp(30));

        LinearLayout desktop = section("Desktop");
        addSwitch(desktop, "Window animations", true);
        addSwitch(desktop, "Window shadows", true);
        addSwitch(desktop, "Auto focus windows", true);
        content.addView(desktop);

        LinearLayout linux = section("Linux");
        addSwitch(linux, "Start Linux environment", true);
        addSwitch(linux, "Linux application windows", true);
        addSwitch(linux, "Use root environment", true);
        content.addView(linux);

        LinearLayout terminal = section("Terminal");
        addSwitch(terminal, "Monospace font", true);
        addSwitch(terminal, "Auto scroll", true);
        addSwitch(terminal, "Show root prompt", true);
        content.addView(terminal);

        LinearLayout system = section("System");
        addSwitch(system, "Dark theme", true);
        addSwitch(system, "Desktop mode", true);
        content.addView(system);

        TextView version = text(
            "METMC OS\nVersion 6.0\n\nAndroid desktop environment",
            14
        );
        version.setTextColor(Color.LTGRAY);
        version.setPadding(dp(18), dp(25), dp(18), dp(25));

        content.addView(version);

        scroll.addView(content);

        root.addView(
            scroll,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
            )
        );

        setContentView(root);
    }
}
