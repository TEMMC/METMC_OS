package com.metmc.os.settings;

import android.app.Activity;
import androidx.appcompat.app.AppCompatDelegate;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class SettingsActivity extends Activity {

    private LinearLayout root;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildMain();
    }

    private TextView title(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(22);
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setPadding(24, 24, 24, 24);
        return v;
    }

    private Button item(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        return b;
    }

    private Switch sw(String text, String key, boolean def) {
        Switch s = new Switch(this);
        s.setText(text);
        s.setTextColor(Color.WHITE);
        s.setPadding(24, 18, 24, 18);
        s.setChecked(MetmcSettingsStore.getBoolean(this, key, def));
        s.setOnCheckedChangeListener((button, checked) ->
                MetmcSettingsStore.setBoolean(this, key, checked));
        return s;
    }

    private void base() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(18,18,18));
        setContentView(root);
    }

    private void applyTheme() {
        String theme = MetmcSettingsStore.getTheme(this);
        if ("light".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if ("system".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    private void buildMain() {
        applyTheme();
        base();

        root.addView(title("METMC OS Settings"));

        Button security = item("🔐  Security & Lock Screen");
        security.setOnClickListener(v -> buildSecurity());
        root.addView(security);

        Button appearance = item("🎨  Appearance");
        appearance.setOnClickListener(v -> buildAppearance());
        root.addView(appearance);

        root.addView(title("Desktop"));
        root.addView(sw("Window animations", "window_animations", true));
        root.addView(sw("Window shadows", "window_shadows", true));
        root.addView(sw("Auto focus windows", "auto_focus", true));
        root.addView(sw("Desktop mode", "desktop_mode", true));

        root.addView(title("Linux"));
        root.addView(sw("Start Linux environment", "linux_start", false));
        root.addView(sw("Linux application windows", "linux_windows", true));
        root.addView(sw("Use root environment", "linux_root", true));

        root.addView(title("Terminal"));
        root.addView(sw("Monospace font", "terminal_mono", true));
        root.addView(sw("Auto scroll", "terminal_scroll", true));
        root.addView(sw("Show root prompt", "terminal_root_prompt", true));

        root.addView(title("System"));
        TextView version = new TextView(this);
        version.setText("METMC OS  •  Settings");
        version.setTextColor(Color.LTGRAY);
        version.setPadding(24, 20, 24, 20);
        root.addView(version);
    }

    private void buildSecurity() {
        base();
        root.addView(title("Security & Lock Screen"));

        Button back = item("← Back");
        back.setOnClickListener(v -> buildMain());
        root.addView(back);

        String current = MetmcSettingsStore.getLockType(this);
        RadioGroup group = new RadioGroup(this);
        group.setPadding(24, 10, 24, 10);

        String[] labels = {"Password", "PIN", "Pattern", "None"};
        String[] values = {"password", "pin", "pattern", "none"};

        for (int i = 0; i < labels.length; i++) {
            RadioButton r = new RadioButton(this);
            r.setText(labels[i]);
            r.setTextColor(Color.WHITE);
            r.setTag(values[i]);
            r.setChecked(values[i].equals(current));
            group.addView(r);
        }

        root.addView(group);

        Button change = item("Change credential");
        change.setOnClickListener(v -> {
            int id = group.getCheckedRadioButtonId();
            if (id < 0) return;

            RadioButton selected = findViewById(id);
            String type = String.valueOf(selected.getTag());

            if ("none".equals(type)) {
                MetmcSettingsStore.setLock(this, "none", "");
                Toast.makeText(this, "Lock screen disabled", Toast.LENGTH_SHORT).show();
                return;
            }

            EditText input = new EditText(this);
            input.setTextColor(Color.WHITE);
            input.setHintTextColor(Color.GRAY);
            input.setHint(
                    "password".equals(type) ? "Enter password" :
                    "pin".equals(type) ? "Enter PIN" : "Enter pattern"
            );
            input.setInputType(
                    "pin".equals(type)
                            ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD
                            : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
            );

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Set " + labels[id])
                    .setView(input)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Save", (d, w) -> {
                        String value = input.getText().toString();
                        if (value.isEmpty()) {
                            Toast.makeText(this, "Credential cannot be empty", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        MetmcSettingsStore.setLock(this, type, value);
                        Toast.makeText(this, "Security settings saved", Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });
        root.addView(change);
    }

    private void buildAppearance() {
        base();
        root.addView(title("Appearance"));

        Button back = item("← Back");
        back.setOnClickListener(v -> buildMain());
        root.addView(back);

        root.addView(title("Theme"));

        RadioGroup themes = new RadioGroup(this);
        String[] themeNames = {"Dark", "Light", "System"};
        String[] themeValues = {"dark", "light", "system"};
        String currentTheme = MetmcSettingsStore.getTheme(this);

        for (int i = 0; i < themeNames.length; i++) {
            RadioButton r = new RadioButton(this);
            r.setText(themeNames[i]);
            r.setTextColor(Color.WHITE);
            r.setTag(themeValues[i]);
            r.setChecked(themeValues[i].equals(currentTheme));
            themes.addView(r);
        }

        themes.setOnCheckedChangeListener((g, id) -> {
            RadioButton r = g.findViewById(id);
            if (r != null) {
                MetmcSettingsStore.setTheme(this, String.valueOf(r.getTag()));
                Toast.makeText(this, "Theme saved", Toast.LENGTH_SHORT).show();
            }
        });

        root.addView(themes);
        root.addView(title("Wallpaper"));

        Button defaultWallpaper = item("METMC default wallpaper");
        defaultWallpaper.setOnClickListener(v -> {
            MetmcSettingsStore.setWallpaper(this, "default");
            Toast.makeText(this, "Wallpaper set to default", Toast.LENGTH_SHORT).show();
        });
        root.addView(defaultWallpaper);

        Button darkWallpaper = item("Dark wallpaper");
        darkWallpaper.setOnClickListener(v -> {
            MetmcSettingsStore.setWallpaper(this, "dark");
            Toast.makeText(this, "Dark wallpaper selected", Toast.LENGTH_SHORT).show();
        });
        root.addView(darkWallpaper);
    }

    private void buildUpdatesPage() {
        TextView title = new TextView(this);
        title.setText("Updates");
        title.setTextSize(24);
        title.setPadding(24, 24, 24, 16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(24, 16, 24, 24);

        page.addView(title);

        Button check = new Button(this);
        check.setText("Check for Updates");
        check.setOnClickListener(v -> {
            try {
                Class<?> updater = Class.forName("com.metmc.os.update.MetmcUpdater");
                java.lang.reflect.Method[] methods = updater.getDeclaredMethods();

                for (java.lang.reflect.Method method : methods) {
                    if (method.getName().toLowerCase().contains("check")
                            || method.getName().toLowerCase().contains("update")) {
                        if (method.getParameterCount() == 0) {
                            method.setAccessible(true);
                            method.invoke(null);
                            return;
                        }
                    }
                }

                android.widget.Toast.makeText(
                        this,
                        "Update checker is unavailable",
                        android.widget.Toast.LENGTH_LONG
                ).show();
            } catch (Throwable e) {
                android.widget.Toast.makeText(
                        this,
                        "Unable to check for updates",
                        android.widget.Toast.LENGTH_LONG
                ).show();
            }
        });

        page.addView(check);
        setContentView(page);
    }

}
