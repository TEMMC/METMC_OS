package com.metmc.os.linux;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;

public class LinuxDesktopActivity extends Activity {

    private LinuxDisplayView display;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("  METMC Linux Desktop  ");
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);

        Button terminal = new Button(this);
        terminal.setText("Terminal");
        terminal.setOnClickListener(v -> {
            startActivity(
                new android.content.Intent(
                    this,
                    LinuxTerminalActivity.class
                )
            );
        });

        bar.addView(
            title,
            new LinearLayout.LayoutParams(
                0, 60, 1
            )
        );

        bar.addView(
            terminal,
            new LinearLayout.LayoutParams(
                180, 60
            )
        );

        root.addView(bar);

        display = new LinuxDisplayView(this);

        root.addView(
            display,
            new LinearLayout.LayoutParams(
                -1, 0, 1
            )
        );

        setContentView(root);
        display.start();
    }

    @Override
    protected void onDestroy() {
        if (display != null)
            display.stop();

        super.onDestroy();
    }
}
