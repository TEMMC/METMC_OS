package com.metmc.os.settings;

import android.app.Activity;
import androidx.appcompat.app.AppCompatDelegate;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class SettingsActivity extends Activity {
    private LinearLayout root;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildMain();
    }

    private TextView title(String text) {
        TextView v = new TextView(this);
        v.setText(text); v.setTextColor(Color.WHITE); v.setTextSize(22);
        v.setGravity(Gravity.CENTER_VERTICAL); v.setPadding(24,24,24,24);
        return v;
    }

    private Button item(String text) {
        Button b = new Button(this); b.setText(text); b.setTextColor(Color.WHITE);
        b.setAllCaps(false); b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        return b;
    }

    private Switch settingSwitch(String text, String key, boolean def) {
        Switch s = new Switch(this); s.setText(text); s.setTextColor(Color.WHITE);
        s.setPadding(24,18,24,18); s.setChecked(MetmcSettingsStore.getBoolean(this,key,def));
        return s;
    }

    private void base() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(18,18,18)); setContentView(root);
    }

    private void applyTheme() {
        String theme = MetmcSettingsStore.getTheme(this);
        AppCompatDelegate.setDefaultNightMode(
            "light".equals(theme) ? AppCompatDelegate.MODE_NIGHT_NO :
            "system".equals(theme) ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM :
            AppCompatDelegate.MODE_NIGHT_YES);
    }

    private void buildMain() {
        applyTheme(); base(); root.addView(title("METMC OS Settings"));
        Button security = item("🔐  Security & Lock Screen"); security.setOnClickListener(v -> buildSecurity()); root.addView(security);
        Button appearance = item("🎨  Appearance"); appearance.setOnClickListener(v -> buildAppearance()); root.addView(appearance);

        root.addView(title("Desktop"));
        root.addView(settingSwitch("Window animations", "window_animations", true));
        root.addView(settingSwitch("Window shadows", "window_shadows", true));
        root.addView(settingSwitch("Auto focus windows", "auto_focus", true));
        root.addView(settingSwitch("Desktop mode", "desktop_mode", true));

        root.addView(title("Linux"));
        Switch linuxStart = settingSwitch("Start Linux environment", "linux_start", true);
        Switch linuxWindows = settingSwitch("Linux application windows", "linux_windows", true);
        Switch linuxRoot = settingSwitch("Use root environment", "linux_root", true);
        root.addView(linuxStart); root.addView(linuxWindows); root.addView(linuxRoot);
        View.OnClickListener sync = v -> {
            boolean enabled = linuxStart.isChecked();
            if (!enabled) {
                linuxWindows.setChecked(false); linuxRoot.setChecked(false);
                MetmcSettingsStore.setBoolean(this,"linux_windows",false);
                MetmcSettingsStore.setBoolean(this,"linux_root",false);
            }
            linuxWindows.setEnabled(enabled); linuxRoot.setEnabled(enabled);
        };
        linuxStart.setOnCheckedChangeListener((b, checked) -> {
            MetmcSettingsStore.setBoolean(this,"linux_start",checked);
            linuxWindows.setEnabled(checked); linuxRoot.setEnabled(checked);
            if (!checked) { linuxWindows.setChecked(false); linuxRoot.setChecked(false); MetmcSettingsStore.setBoolean(this,"linux_windows",false); MetmcSettingsStore.setBoolean(this,"linux_root",false); }
        });
        boolean start = linuxStart.isChecked(); linuxWindows.setEnabled(start); linuxRoot.setEnabled(start);
        if (!start) { linuxWindows.setChecked(false); linuxRoot.setChecked(false); }

        root.addView(title("Terminal"));
        root.addView(settingSwitch("Monospace font", "terminal_mono", true));
        root.addView(settingSwitch("Auto scroll", "terminal_scroll", true));
        root.addView(settingSwitch("Show root prompt", "terminal_root_prompt", true));

        root.addView(title("System"));
        TextView version = new TextView(this); version.setText("METMC OS  •  Settings"); version.setTextColor(Color.LTGRAY); version.setPadding(24,20,24,20); root.addView(version);
    }

    private void buildSecurity() {
        base(); root.addView(title("Security & Lock Screen"));
        Button back = item("← Back"); back.setOnClickListener(v -> buildMain()); root.addView(back);
        String current = MetmcSettingsStore.getLockType(this);
        RadioGroup group = new RadioGroup(this); group.setPadding(24,10,24,10);
        String[] labels={"Password","PIN","Pattern","None"}; String[] values={"password","pin","pattern","none"};
        for(int i=0;i<labels.length;i++){ RadioButton r=new RadioButton(this); r.setText(labels[i]); r.setTextColor(Color.WHITE); r.setTag(values[i]); r.setChecked(values[i].equals(current)); group.addView(r); }
        root.addView(group);
        Button change=item("Change credential"); change.setOnClickListener(v->{
            int id=group.getCheckedRadioButtonId(); if(id<0)return; RadioButton selected=findViewById(id); String type=String.valueOf(selected.getTag());
            if("none".equals(type)){MetmcSettingsStore.setLock(this,"none",""); Toast.makeText(this,"Lock screen disabled",Toast.LENGTH_SHORT).show(); return;}
            EditText input=new EditText(this); input.setTextColor(Color.WHITE); input.setHintTextColor(Color.GRAY);
            input.setHint("password".equals(type)?"Enter password":"pin".equals(type)?"Enter PIN":"Enter pattern");
            input.setInputType("pin".equals(type)?InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
            new android.app.AlertDialog.Builder(this).setTitle("Set "+labels[id]).setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
                String value=input.getText().toString(); if(value.isEmpty()){Toast.makeText(this,"Credential cannot be empty",Toast.LENGTH_SHORT).show();return;}
                MetmcSettingsStore.setLock(this,type,value); Toast.makeText(this,"Security settings saved",Toast.LENGTH_SHORT).show();
            }).show();
        }); root.addView(change);
    }

    private void buildAppearance() {
        base(); root.addView(title("Appearance"));
        Button back=item("← Back"); back.setOnClickListener(v->buildMain()); root.addView(back); root.addView(title("Theme"));
        RadioGroup themes=new RadioGroup(this); String[] names={"Dark","Light","System"}; String[] values={"dark","light","system"}; String current=MetmcSettingsStore.getTheme(this);
        for(int i=0;i<names.length;i++){RadioButton r=new RadioButton(this);r.setText(names[i]);r.setTextColor(Color.WHITE);r.setTag(values[i]);r.setChecked(values[i].equals(current));themes.addView(r);}
        themes.setOnCheckedChangeListener((g,id)->{RadioButton r=g.findViewById(id);if(r!=null){MetmcSettingsStore.setTheme(this,String.valueOf(r.getTag()));applyTheme();Toast.makeText(this,"Theme saved",Toast.LENGTH_SHORT).show();}}); root.addView(themes);
        root.addView(title("Wallpaper"));
        Button choose=item("Choose wallpaper"); choose.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,9002);}); root.addView(choose);
        Button def=item("METMC default wallpaper"); def.setOnClickListener(v->{MetmcSettingsStore.setWallpaper(this,"default");Toast.makeText(this,"Wallpaper set to default",Toast.LENGTH_SHORT).show();}); root.addView(def);
        Button dark=item("Dark wallpaper"); dark.setOnClickListener(v->{MetmcSettingsStore.setWallpaper(this,"dark");Toast.makeText(this,"Dark wallpaper selected",Toast.LENGTH_SHORT).show();}); root.addView(dark);
    }

    private void buildUpdatesPage() {
        TextView t=new TextView(this);t.setText("Updates");t.setTextSize(24);t.setPadding(24,24,24,16);t.setTypeface(null,android.graphics.Typeface.BOLD);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(24,16,24,24);page.addView(t);
        Button check=new Button(this);check.setText("Check for Updates");check.setOnClickListener(v->{try{Class<?> updater=Class.forName("com.metmc.os.update.MetmcUpdater");for(java.lang.reflect.Method m:updater.getDeclaredMethods())if((m.getName().toLowerCase().contains("check")||m.getName().toLowerCase().contains("update"))&&m.getParameterCount()==0){m.setAccessible(true);m.invoke(null);return;}Toast.makeText(this,"Update checker is unavailable",Toast.LENGTH_LONG).show();}catch(Throwable e){Toast.makeText(this,"Unable to check for updates",Toast.LENGTH_LONG).show();}});page.addView(check);setContentView(page);
    }
}
