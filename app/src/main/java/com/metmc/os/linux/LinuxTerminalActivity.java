package com.metmc.os.linux;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import java.io.*;

public class LinuxTerminalActivity extends Activity {

    private TextView output;
    private EditText command;
    private ScrollView scroll;

    private Process shell;
    private BufferedWriter shellIn;
    private BufferedReader shellOut;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(8, 8, 10));

        createTerminal(root);
        setContentView(root);

        setupWindow();
        startShell();
    }

    private void createTerminal(LinearLayout root) {

        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(dp(8), 0, dp(4), 0);
        titleBar.setBackgroundColor(Color.rgb(35, 38, 46));

        TextView title = new TextView(this);
        title.setText("METMC Linux Terminal");
        title.setTextColor(Color.WHITE);
        title.setTextSize(15);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(10), 0, 0, 0);

        titleBar.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        dp(44),
                        1
                )
        );

        Button close = new Button(this);
        close.setText("×");
        close.setTextSize(20);
        close.setTextColor(Color.WHITE);
        close.setAllCaps(false);

        titleBar.addView(
                close,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(44)
                )
        );

        close.setOnClickListener(v -> finish());

        root.addView(
                titleBar,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(44)
                )
        );

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        output = new TextView(this);
        output.setTextColor(Color.WHITE);
        output.setTextSize(14);
        output.setTypeface(Typeface.MONOSPACE);
        output.setPadding(
                dp(12),
                dp(10),
                dp(12),
                dp(10)
        );

        output.setText(
                "METMC Linux Terminal\n" +
                "Starting Linux shell...\n\n"
        );

        scroll.addView(
                output,
                new ScrollView.LayoutParams(
                        -1,
                        -2
                )
        );

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setPadding(
                dp(8),
                dp(6),
                dp(8),
                dp(8)
        );

        command = new EditText(this);
        command.setSingleLine(true);
        command.setTextColor(Color.WHITE);
        command.setHintTextColor(Color.GRAY);
        command.setHint("Enter Linux command");
        command.setTypeface(Typeface.MONOSPACE);
        command.setTextSize(14);
        command.setImeOptions(EditorInfo.IME_ACTION_GO);

        inputRow.addView(
                command,
                new LinearLayout.LayoutParams(
                        0,
                        dp(52),
                        1
                )
        );

        Button run = new Button(this);
        run.setText("Run");
        run.setAllCaps(false);

        inputRow.addView(
                run,
                new LinearLayout.LayoutParams(
                        dp(90),
                        dp(52)
                )
        );

        root.addView(
                inputRow,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        run.setOnClickListener(v -> sendCommand());

        command.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                sendCommand();
                return true;
            }
            return false;
        });
    }

    private void setupWindow() {

        int screenW =
                getResources()
                        .getDisplayMetrics()
                        .widthPixels;

        int screenH =
                getResources()
                        .getDisplayMetrics()
                        .heightPixels;

        WindowManager.LayoutParams windowLp =
                getWindow().getAttributes();

        windowLp.width =
                Math.min(
                        (int) (screenW * 0.82f),
                        dp(720)
                );

        windowLp.height =
                Math.min(
                        (int) (screenH * 0.72f),
                        dp(540)
                );

        windowLp.gravity = Gravity.CENTER;
        windowLp.dimAmount = 0.20f;

        getWindow().setAttributes(windowLp);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND
        );

        getWindow().setBackgroundDrawable(
                new ColorDrawable(
                        Color.rgb(8, 8, 10)
                )
        );
    }

    private void startShell() {

        new Thread(() -> {
            try {

                String shellCommand =
                        "export HOME=/root; " +
                        "export USER=root; " +
                        "export TERM=xterm-256color; " +
                        "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                        "cd /root; " +
                        "exec /bin/bash --noprofile --norc -i";

                String chrootCommand =
                        "chroot /data/local/linux/rootfs " +
                        "/bin/bash -c " +
                        quoteShell(shellCommand);

                shell = new ProcessBuilder(
                        "/debug_ramdisk/su",
                        "-c",
                        chrootCommand
                )
                        .redirectErrorStream(true)
                        .start();

                shellIn = new BufferedWriter(
                        new OutputStreamWriter(
                                shell.getOutputStream()
                        )
                );

                shellOut = new BufferedReader(
                        new InputStreamReader(
                                shell.getInputStream()
                        )
                );

                shellIn.write(
                        "export PS1='root@metmc:\\w# '\n"
                );

                shellIn.flush();

                char[] buffer = new char[4096];
                int count;

                while (
                        (count = shellOut.read(buffer))
                                != -1
                ) {

                    final String text =
                            new String(
                                    buffer,
                                    0,
                                    count
                            );

                    runOnUiThread(() -> {

                        output.append(text);

                        scroll.post(() ->
                                scroll.fullScroll(
                                        ScrollView.FOCUS_DOWN
                                )
                        );
                    });
                }

            } catch (Exception e) {

                runOnUiThread(() -> {

                    output.append(
                            "\n[Terminal error]\n" +
                            e.toString() +
                            "\n"
                    );
                });
            }
        }).start();
    }

    private void sendCommand() {

        String cmd =
                command.getText()
                        .toString()
                        .trim();

        if (cmd.isEmpty()) {
            return;
        }

        if (shellIn == null) {

            output.append(
                    "\n[Shell is still starting]\n"
            );

            return;
        }

        try {

            shellIn.write(cmd);
            shellIn.newLine();
            shellIn.flush();

            command.setText("");

        } catch (Exception e) {

            output.append(
                    "\n[Write error] " +
                    e +
                    "\n"
            );
        }
    }

    private String quoteShell(String value) {

        return "'" +
                value.replace(
                        "'",
                        "'\\''"
                ) +
                "'";
    }

    private int dp(int value) {

        return (int) (
                value *
                getResources()
                        .getDisplayMetrics()
                        .density +
                0.5f
        );
    }

    @Override
    protected void onDestroy() {

        try {

            if (shellIn != null) {
                shellIn.write("exit\n");
                shellIn.flush();
            }

        } catch (Exception ignored) {
        }

        if (shell != null) {
            shell.destroy();
        }

        super.onDestroy();
    }
}
