package com.metmc.os.linux;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
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

    private final String PROMPT = "root@metmc:~# ";

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

        LinearLayout titleBar =
                new LinearLayout(this);

        titleBar.setOrientation(
                LinearLayout.HORIZONTAL
        );

        titleBar.setGravity(
                Gravity.CENTER_VERTICAL
        );

        titleBar.setPadding(
                dp(8),
                0,
                dp(4),
                0
        );

        titleBar.setBackgroundColor(
                Color.rgb(45, 48, 58)
        );

        TextView title =
                new TextView(this);

        title.setText(
                "METMC Linux Terminal"
        );

        title.setTextColor(
                Color.WHITE
        );

        title.setTextSize(15);

        title.setTypeface(
                Typeface.MONOSPACE,
                Typeface.BOLD
        );

        title.setGravity(
                Gravity.CENTER_VERTICAL
        );

        title.setPadding(
                dp(12),
                0,
                0,
                0
        );

        titleBar.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        dp(46),
                        1
                )
        );

        Button close =
                new Button(this);

        close.setText("×");
        close.setTextSize(22);
        close.setTextColor(Color.WHITE);
        close.setAllCaps(false);

        titleBar.addView(
                close,
                new LinearLayout.LayoutParams(
                        dp(58),
                        dp(46)
                )
        );

        close.setOnClickListener(
                v -> finish()
        );

        root.addView(
                titleBar,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(46)
                )
        );

        scroll =
                new ScrollView(this);

        scroll.setFillViewport(true);

        output =
                new TextView(this);

        output.setTextColor(
                Color.rgb(225, 225, 225)
        );

        output.setTextSize(14);

        output.setTypeface(
                Typeface.MONOSPACE
        );

        output.setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(8)
        );

        output.setText(
                "METMC Linux\n" +
                "root environment ready\n\n"
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

        LinearLayout inputRow =
                new LinearLayout(this);

        inputRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        inputRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        inputRow.setPadding(
                dp(14),
                dp(2),
                dp(14),
                dp(8)
        );

        TextView prompt =
                new TextView(this);

        prompt.setText(PROMPT);

        prompt.setTextColor(
                Color.rgb(120, 210, 130)
        );

        prompt.setTextSize(14);

        prompt.setTypeface(
                Typeface.MONOSPACE,
                Typeface.BOLD
        );

        prompt.setSingleLine(true);

        inputRow.addView(
                prompt,
                new LinearLayout.LayoutParams(
                        -2,
                        dp(48)
                )
        );

        command =
                new EditText(this);

        command.setSingleLine(true);

        command.setTextColor(
                Color.WHITE
        );

        command.setTextSize(14);

        command.setTypeface(
                Typeface.MONOSPACE
        );

        command.setBackgroundColor(
                Color.TRANSPARENT
        );

        command.setPadding(
                0,
                0,
                0,
                0
        );

        command.setHint("");

        command.setImeOptions(
                EditorInfo.IME_ACTION_GO
        );

        inputRow.addView(
                command,
                new LinearLayout.LayoutParams(
                        0,
                        dp(48),
                        1
                )
        );

        root.addView(
                inputRow,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(58)
                )
        );

        command.setOnEditorActionListener(
                (v, actionId, event) -> {

                    if (
                            actionId ==
                            EditorInfo.IME_ACTION_GO
                    ) {

                        sendCommand();
                        return true;
                    }

                    return false;
                }
        );

        command.setOnKeyListener(
                (v, keyCode, event) -> {

                    if (
                            event.getAction() ==
                            KeyEvent.ACTION_DOWN &&
                            keyCode ==
                            KeyEvent.KEYCODE_ENTER
                    ) {

                        sendCommand();
                        return true;
                    }

                    return false;
                }
        );

        command.requestFocus();
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

        WindowManager.LayoutParams lp =
                getWindow().getAttributes();

        lp.width =
                Math.min(
                        (int)(screenW * 0.80f),
                        dp(900)
                );

        lp.height =
                Math.min(
                        (int)(screenH * 0.76f),
                        dp(620)
                );

        lp.gravity =
                Gravity.CENTER;

        getWindow().setAttributes(lp);

        getWindow().setBackgroundDrawable(
                new ColorDrawable(
                        Color.rgb(8, 8, 10)
                )
        );
    }

    private void startShell() {

        new Thread(() -> {

            try {

                String commandLine =
                        "export HOME=/root; " +
                        "export USER=root; " +
                        "export TERM=xterm-256color; " +
                        "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                        "cd /root; " +
                        "exec /bin/bash --noprofile --norc";

                String chrootCommand =
                        "chroot /data/local/linux/rootfs " +
                        "/bin/bash -c " +
                        quoteShell(commandLine);

                shell =
                        new ProcessBuilder(
                                "/debug_ramdisk/su",
                                "-c",
                                chrootCommand
                        )
                                .redirectErrorStream(true)
                                .start();

                shellIn =
                        new BufferedWriter(
                                new OutputStreamWriter(
                                        shell.getOutputStream()
                                )
                        );

                shellOut =
                        new BufferedReader(
                                new InputStreamReader(
                                        shell.getInputStream()
                                )
                        );

                char[] buffer =
                        new char[4096];

                int count;

                while (
                        (count =
                                shellOut.read(buffer)
                        ) != -1
                ) {

                    final String text =
                            new String(
                                    buffer,
                                    0,
                                    count
                            );

                    runOnUiThread(
                            () -> appendOutput(text)
                    );
                }

            } catch (Exception e) {

                runOnUiThread(
                        () -> appendOutput(
                                "\nTerminal error: " +
                                e +
                                "\n"
                        )
                );
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

            appendOutput(
                    "\n[Shell starting...]\n"
            );

            return;
        }

        appendOutput(
                "\n" +
                PROMPT +
                cmd +
                "\n"
        );

        try {

            shellIn.write(cmd);

            shellIn.newLine();

            shellIn.write(
                    "printf '\\n__METMC_COMMAND_END__\\n'\n"
            );

            shellIn.flush();

            command.setText("");

            new Thread(() -> {

                try {

                    String line;

                    while (
                            (line =
                                    shellOut.readLine()
                            ) != null
                    ) {

                        if (
                                line.equals(
                                        "__METMC_COMMAND_END__"
                                )
                        ) {

                            runOnUiThread(
                                    () -> {

                                        scroll.post(
                                                () ->
                                                        scroll.fullScroll(
                                                                ScrollView.FOCUS_DOWN
                                                        )
                                        );
                                    }
                            );

                            break;
                        }

                        final String result =
                                line;

                        runOnUiThread(
                                () -> appendOutput(
                                        result + "\n"
                                )
                        );
                    }

                } catch (Exception ignored) {
                }

            }).start();

        } catch (Exception e) {

            appendOutput(
                    "\nWrite error: " +
                    e +
                    "\n"
            );
        }
    }

    private void appendOutput(
            String text
    ) {

        output.append(text);

        scroll.post(
                () ->
                        scroll.fullScroll(
                                ScrollView.FOCUS_DOWN
                        )
        );
    }

    private String quoteShell(
            String value
    ) {

        return "'" +
                value.replace(
                        "'",
                        "'\\''"
                ) +
                "'";
    }

    private int dp(
            int value
    ) {

        return (int)(
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
