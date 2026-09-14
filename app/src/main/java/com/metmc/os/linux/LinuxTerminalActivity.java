package com.metmc.os.linux;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LinuxTerminalActivity extends Activity {

    private LinearLayout root;
    private ScrollView scroll;
    private TextView output;
    private EditText input;

    private Process shell;
    private BufferedWriter shellIn;

    private volatile boolean running = false;

    private final String ROOTFS = "/data/local/linux/rootfs";

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        buildTerminal();
        startInteractiveShell();
    }

    private void buildTerminal() {

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(7, 9, 12));

        LinearLayout title = new LinearLayout(this);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(16), 0, dp(8), 0);
        title.setBackgroundColor(Color.rgb(25, 28, 34));

        TextView titleText = new TextView(this);
        titleText.setText("METMC Terminal");
        titleText.setTextColor(Color.WHITE);
        titleText.setTextSize(16);
        titleText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);

        title.addView(
                titleText,
                new LinearLayout.LayoutParams(
                        0,
                        dp(52),
                        1
                )
        );

        TextView close = new TextView(this);
        close.setText("×");
        close.setTextColor(Color.WHITE);
        close.setTextSize(28);
        close.setGravity(Gravity.CENTER);

        close.setOnClickListener(v -> finish());

        title.addView(
                close,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(52)
                )
        );

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(52)
                )
        );

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(7, 9, 12));

        output = new TextView(this);
        output.setTextColor(Color.rgb(225, 230, 235));
        output.setTextSize(15);
        output.setTypeface(Typeface.MONOSPACE);
        output.setTextIsSelectable(true);
        output.setPadding(dp(14), dp(14), dp(14), dp(14));
        output.setText(
                "METMC Linux Terminal\n" +
                "Debian interactive shell\n\n"
        );

        scroll.addView(
                output,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        LinearLayout commandBar = new LinearLayout(this);
        commandBar.setGravity(Gravity.CENTER_VERTICAL);
        commandBar.setPadding(dp(10), dp(6), dp(10), dp(6));
        commandBar.setBackgroundColor(Color.rgb(20, 23, 28));

        TextView prompt = new TextView(this);
        prompt.setText("metmc@metmc:~$ ");
        prompt.setTextColor(Color.rgb(110, 205, 135));
        prompt.setTextSize(14);
        prompt.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);

        commandBar.addView(
                prompt,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        dp(48)
                )
        );

        input = new EditText(this);
        input.setSingleLine(true);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.rgb(100, 105, 112));
        input.setTextSize(15);
        input.setTypeface(Typeface.MONOSPACE);
        input.setHint("command");
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setImeOptions(EditorInfo.IME_ACTION_NONE);
        input.setPadding(0, 0, 0, 0);

        commandBar.addView(
                input,
                new LinearLayout.LayoutParams(
                        0,
                        dp(48),
                        1
                )
        );

        root.addView(
                commandBar,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(60)
                )
        );

        input.setOnKeyListener((v, keyCode, event) -> {

            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }

            if (!running || shellIn == null) {
                return false;
            }

            try {

                if (keyCode == KeyEvent.KEYCODE_ENTER) {

                    sendBytes("\r");

                    String command = input.getText().toString();
                    input.setText("");

                    return true;
                }

                if (keyCode == KeyEvent.KEYCODE_TAB) {
                    sendBytes("\t");
                    return true;
                }

                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    sendBytes("\033[A");
                    return true;
                }

                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    sendBytes("\033[B");
                    return true;
                }

                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    sendBytes("\033[C");
                    return true;
                }

                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    sendBytes("\033[D");
                    return true;
                }

                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    sendBytes("\177");
                    return true;
                }

                if (event.isCtrlPressed()) {

                    if (keyCode == KeyEvent.KEYCODE_C) {
                        sendBytes("\003");
                        return true;
                    }

                    if (keyCode == KeyEvent.KEYCODE_D) {
                        sendBytes("\004");
                        return true;
                    }

                    if (keyCode == KeyEvent.KEYCODE_Z) {
                        sendBytes("\032");
                        return true;
                    }

                    if (keyCode == KeyEvent.KEYCODE_L) {
                        sendBytes("\014");
                        return true;
                    }
                }

            } catch (Exception ignored) {
            }

            return false;
        });

        setContentView(root);
    }

    private void startInteractiveShell() {

        new Thread(() -> {

            try {

                String command =
                        "export HOME=/home/metmc; " +
                        "export USER=metmc; " +
                        "export LOGNAME=metmc; " +
                        "export SHELL=/bin/bash; " +
                        "export TERM=xterm-256color; " +
                        "export LANG=C.UTF-8; " +
                        "export LC_ALL=C.UTF-8; " +
                        "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                        "export XDG_RUNTIME_DIR=/tmp/metmc-runtime; " +
                        "export PS1=\"\\u@metmc:\\w$ \"; " +
                        "mkdir -p /tmp/metmc-runtime; " +
                        "chmod 700 /tmp/metmc-runtime; " +
                        "cd /home/metmc; " +
                        "exec /bin/bash -i";

                String wrapped;

                /*
                 * util-linux 'script' creates a real PTY.
                 * This is the important difference from the old
                 * BufferedReader/BufferedWriter command runner.
                 */
                wrapped =
                        "if command -v script >/dev/null 2>&1; then " +
                        "script -qefc " + quote(command) + " /dev/null; " +
                        "else " +
                        command + "; " +
                        "fi";

                String chroot =
                        "chroot --userspec=1000:1000 " + quote(ROOTFS) +
                        " /bin/bash -lc " + quote(wrapped);

                shell = new ProcessBuilder(
                        "su",
                        "-c",
                        chroot
                )
                        .redirectErrorStream(true)
                        .start();

                shellIn = new BufferedWriter(
                        new OutputStreamWriter(
                                shell.getOutputStream(),
                                StandardCharsets.UTF_8
                        )
                );

                running = true;

                readShellOutput(shell.getInputStream());

            } catch (Exception e) {

                appendOutput(
                        "\n[METMC] Terminal error: " +
                        e.getMessage() +
                        "\n"
                );

            } finally {

                running = false;

                runOnUiThread(() -> {
                    if (input != null) {
                        input.setEnabled(false);
                    }
                });
            }

        }, "METMC-PTY").start();
    }

    private void readShellOutput(InputStream stream) {

        new Thread(() -> {

            try {

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        stream,
                                        StandardCharsets.UTF_8
                                )
                        );

                char[] buffer = new char[2048];

                int count;

                while ((count = reader.read(buffer)) != -1) {

                    String text =
                            new String(
                                    buffer,
                                    0,
                                    count
                            );

                    appendOutput(stripUnsafeTerminalSequences(text));
                }

            } catch (Exception ignored) {
            }

        }, "METMC-PTY-Reader").start();
    }

    private String stripUnsafeTerminalSequences(String text) {

        /*
         * Keep normal ANSI colour/control sequences useful enough
         * for shell output, while removing terminal-title changes
         * that should never be displayed as text.
         */
        return text
                .replaceAll("\\u001B\\][0-9;]*;?.*?(\\u0007|\\u001B\\\\)", "")
                .replace("\u001B[?25h", "")
                .replace("\u001B[?25l", "");
    }

    private void sendBytes(String data) throws Exception {

        if (!running || shellIn == null) {
            return;
        }

        shellIn.write(data);
        shellIn.flush();
    }

    private void appendOutput(String text) {

        runOnUiThread(() -> {

            if (output == null) {
                return;
            }

            output.append(text);

            scroll.post(
                    () -> scroll.fullScroll(View.FOCUS_DOWN)
            );
        });
    }

    private String quote(String value) {

        return "'" +
                value.replace(
                        "'",
                        "'\\''"
                ) +
                "'";
    }

    @Override
    protected void onDestroy() {

        running = false;

        try {
            if (shellIn != null) {
                shellIn.close();
            }
        } catch (Exception ignored) {
        }

        if (shell != null) {
            try {
                shell.destroy();
            } catch (Exception ignored) {
            }
        }

        super.onDestroy();
    }
}
