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

        TextView title = new TextView(this);
        title.setText("METMC Linux Terminal");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);

        root.addView(title,
                new LinearLayout.LayoutParams(-1, 60));

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
        lp.width = (int)(getResources().getDisplayMetrics().widthPixels * 0.75f);
        lp.height = (int)(getResources().getDisplayMetrics().heightPixels * 0.60f);
        lp.gravity = Gravity.CENTER;
        lp.dimAmount = 0.35f;
        getWindow().setAttributes(lp);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.rgb(8, 8, 10)));

        startShell();

        run.setOnClickListener(v -> sendCommand());

        command.setOnEditorActionListener((v, actionId, event) -> {
            sendCommand();
            return true;
        });
    }

    private void startShell() {
        new Thread(() -> {
            try {
                shell = new ProcessBuilder(
                        "su",
                        "-c",
                        "chroot /data/local/linux/rootfs /bin/bash -i"
                ).redirectErrorStream(true).start();

                shellIn = new BufferedWriter(
                        new OutputStreamWriter(shell.getOutputStream())
                );

                shellOut = new BufferedReader(
                        new InputStreamReader(shell.getInputStream())
                );

                shellIn.write(
                        "export HOME=/root\n" +
                        "export USER=root\n" +
                        "export TERM=xterm-256color\n" +
                        "export DISPLAY=:100\n" +
                        "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\n" +
                        "cd /root\n" +
                        "PS1='root@metmc:\\w# '; export PS1\n"
                );
                shellIn.flush();

                char[] buffer = new char[4096];
                int count;

                while ((count = shellOut.read(buffer)) != -1) {
                    final String text = new String(buffer, 0, count);

                    runOnUiThread(() -> {
                        output.append(text);
                    });
                }

            } catch (Exception e) {
                runOnUiThread(() ->
                        output.append("\n[Shell error] " + e + "\n")
                );
            }
        }).start();
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
