package com.metmc.os.linux;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;

import java.io.*;

public class LinuxTerminalActivity extends Activity {

    private TextView output;
    private EditText command;

    private Process shell;
    private BufferedWriter shellIn;
    private BufferedReader shellOut;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(8, 8, 10));
        root.setPadding(12, 12, 12, 12);

        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(12, 0, 4, 0);
        titleBar.setBackgroundColor(Color.rgb(28, 28, 32));

        TextView title = new TextView(this);
        title.setText("  METMC Linux Terminal");
        title.setTextColor(Color.WHITE);
        title.setTextSize(15);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);

        titleBar.addView(title,
                new LinearLayout.LayoutParams(0, 52, 1));

        Button close = new Button(this);
        close.setText("×");
        close.setTextColor(Color.WHITE);
        close.setTextSize(20);
        close.setBackgroundColor(Color.TRANSPARENT);

        titleBar.addView(close,
                new LinearLayout.LayoutParams(58, 52));

        root.addView(titleBar,
                new LinearLayout.LayoutParams(-1, 52));

        close.setOnClickListener(v -> finish());

        ScrollView scroll = new ScrollView(this);

        output = new TextView(this);
        output.setTextColor(Color.WHITE);
        output.setTypeface(Typeface.MONOSPACE);
        output.setTextSize(13);
        output.setText("METMC Linux Terminal\n");
        scroll.addView(output);

        root.addView(scroll,
                new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout row = new LinearLayout(this);

        command = new EditText(this);
        command.setSingleLine(true);
        command.setTextColor(Color.WHITE);
        command.setHintTextColor(Color.GRAY);
        command.setHint("Linux command");
        command.setTypeface(Typeface.MONOSPACE);

        Button run = new Button(this);
        run.setText("Run");

        row.addView(command,
                new LinearLayout.LayoutParams(0, 60, 1));

        row.addView(run,
                new LinearLayout.LayoutParams(150, 60));

        root.addView(row);

        setContentView(root);

        WindowManager.LayoutParams lp = getWindow().getAttributes();

        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;

        // Standard METMC OS desktop window size.
        lp.width = Math.min((int)(screenW * 0.82f), dp(720));
        lp.height = Math.min((int)(screenH * 0.70f), dp(520));

        lp.gravity = Gravity.CENTER;
        lp.dimAmount = 0.20f;

        getWindow().setAttributes(lp);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.rgb(8, 8, 10))
        );

        startShell();

        run.setOnClickListener(v -> sendCommand());

        command.setOnEditorActionListener((v, actionId, event) -> {
            sendCommand();
            return true;
        });
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void startShell() {
        new Thread(() -> {
            try {
                String cmd =
                        "export HOME=/root; " +
                        "export USER=root; " +
                        "export TERM=xterm-256color; " +
                        "export DISPLAY=:100; " +
                        "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                        "cd /root; " +
                        "exec script -qfec '/bin/bash --noprofile --norc -i' /dev/null";

                shell = new ProcessBuilder(
                        "su",
                        "-c",
                        "chroot /data/local/linux/rootfs /bin/bash -c " +
                                quoteShell(cmd)
                ).redirectErrorStream(true).start();

                shellIn = new BufferedWriter(
                        new OutputStreamWriter(shell.getOutputStream())
                );

                shellOut = new BufferedReader(
                        new InputStreamReader(shell.getInputStream())
                );

                shellIn.write(
                        "export PS1='root@debian:\\w# '\n"
                );
                shellIn.flush();

                char[] buffer = new char[4096];
                int count;

                while ((count = shellOut.read(buffer)) != -1) {
                    final String text = new String(buffer, 0, count);

                    runOnUiThread(() -> {
                        output.append(text);
                        output.invalidate();
                    });
                }

            } catch (Exception e) {
                runOnUiThread(() ->
                        output.append("\n[Terminal error] " + e + "\n")
                );
            }
        }).start();
    }

    private String quoteShell(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private void sendCommand() {
        String cmd = command.getText().toString();

        if (cmd.isEmpty() || shellIn == null)
            return;

        try {
            shellIn.write(cmd);
            shellIn.newLine();
            shellIn.flush();

            command.setText("");

        } catch (Exception e) {
            output.append("\n[Write error] " + e + "\n");
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (shellIn != null) {
                shellIn.write("exit\n");
                shellIn.flush();
            }
        } catch (Exception ignored) {}

        if (shell != null)
            shell.destroy();

        super.onDestroy();
    }
}
