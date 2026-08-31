package com.metmc.os.linux;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class LinuxTerminalActivity extends Activity {

    private LinearLayout terminalContent;
    private ScrollView scrollView;
    private EditText commandInput;

    private Process shell;
    private BufferedWriter shellIn;
    private BufferedReader shellOut;

    private final String prompt = "root@metmc:~# ";
    private final String END_MARKER = "__METMC_COMMAND_END__";

    private int dp(int value) {
        return (int) (
                value *
                getResources().getDisplayMetrics().density
        );
    }

    private TextView terminalText(String text) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextColor(
                Color.rgb(220, 220, 220)
        );

        view.setTextSize(17);
        view.setTypeface(Typeface.MONOSPACE);

        view.setPadding(
                0,
                0,
                0,
                0
        );

        return view;
    }

    private void scrollToBottom() {

        if (scrollView == null) {
            return;
        }

        scrollView.post(
                () -> scrollView.fullScroll(
                        View.FOCUS_DOWN
                )
        );
    }

    @Override
    public void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.rgb(10, 11, 16)
        );

        LinearLayout titleBar =
                new LinearLayout(this);

        titleBar.setOrientation(
                LinearLayout.HORIZONTAL
        );

        titleBar.setGravity(
                Gravity.CENTER_VERTICAL
        );

        titleBar.setPadding(
                dp(18),
                dp(8),
                dp(12),
                dp(8)
        );

        titleBar.setBackgroundColor(
                Color.rgb(48, 51, 65)
        );

        TextView title =
                terminalText(
                        "METMC Linux Terminal"
                );

        title.setTextSize(18);
        title.setTypeface(
                Typeface.MONOSPACE,
                Typeface.BOLD
        );

        titleBar.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        dp(58),
                        1f
                )
        );

        TextView close =
                terminalText("×");

        close.setTextSize(30);
        close.setGravity(
                Gravity.CENTER
        );

        close.setOnClickListener(
                v -> finish()
        );

        titleBar.addView(
                close,
                new LinearLayout.LayoutParams(
                        dp(70),
                        dp(58)
                )
        );

        root.addView(
                titleBar,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(58)
                )
        );

        scrollView =
                new ScrollView(this);

        scrollView.setFillViewport(true);

        HorizontalScrollView horizontal =
                new HorizontalScrollView(this);

        horizontal.setFillViewport(true);

        terminalContent =
                new LinearLayout(this);

        terminalContent.setOrientation(
                LinearLayout.VERTICAL
        );

        terminalContent.setPadding(
                dp(18),
                dp(14),
                dp(18),
                dp(18)
        );

        horizontal.addView(
                terminalContent,
                new HorizontalScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        scrollView.addView(
                horizontal,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );

        setContentView(root);

        startShell();
    }

    private void addPromptLine() {

        LinearLayout line =
                new LinearLayout(this);

        line.setOrientation(
                LinearLayout.HORIZONTAL
        );

        line.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView promptView =
                terminalText(prompt);

        promptView.setTextColor(
                Color.rgb(
                        130,
                        190,
                        145
                )
        );

        commandInput =
                new EditText(this);

        commandInput.setTextColor(
                Color.rgb(
                        235,
                        235,
                        235
                )
        );

        commandInput.setTextSize(17);

        commandInput.setTypeface(
                Typeface.MONOSPACE
        );

        commandInput.setSingleLine(true);

        commandInput.setBackgroundColor(
                Color.TRANSPARENT
        );

        commandInput.setPadding(
                0,
                0,
                0,
                0
        );

        commandInput.setHint("");
        commandInput.setHintTextColor(
                Color.TRANSPARENT
        );

        commandInput.setOnEditorActionListener(
                (v, actionId, event) -> {

                    submitCommand();

                    return true;
                }
        );

        commandInput.setOnKeyListener(
                (v, keyCode, event) -> {

                    if (
                            keyCode ==
                                    KeyEvent.KEYCODE_ENTER &&
                            event.getAction() ==
                                    KeyEvent.ACTION_DOWN
                    ) {

                        submitCommand();

                        return true;
                    }

                    return false;
                }
        );

        line.addView(
                promptView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        line.addView(
                commandInput,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        terminalContent.addView(
                line,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        commandInput.requestFocus();

        scrollToBottom();
    }

    private void submitCommand() {

        if (
                commandInput == null ||
                !commandInput.isEnabled()
        ) {
            return;
        }

        String command =
                commandInput
                        .getText()
                        .toString()
                        .trim();

        if (command.isEmpty()) {
            return;
        }

        commandInput.setEnabled(false);

        new Thread(
                () -> {

                    try {

                        if (shellIn == null) {
                            return;
                        }

                        shellIn.write(
                                command
                        );

                        shellIn.newLine();

                        shellIn.write(
                                "printf '\\n" +
                                END_MARKER +
                                "\\n'"
                        );

                        shellIn.newLine();

                        shellIn.flush();

                    } catch (Exception e) {

                        printOutput(
                                "\nError: " +
                                e.getMessage() +
                                "\n"
                        );

                        runOnUiThread(
                                this::addPromptLine
                        );
                    }

                }
        ).start();
    }

    private void startShell() {

        new Thread(
                () -> {

                    try {

                        String rootfs =
                                "/data/local/linux/rootfs";

                        ProcessBuilder builder;

                        if (
                                new File(rootfs).isDirectory()
                        ) {

                            String command =
                                    "export HOME=/root; " +
                                    "export USER=root; " +
                                    "export TERM=xterm-256color; " +
                                    "export PATH=/usr/local/sbin:" +
                                    "/usr/local/bin:" +
                                    "/usr/sbin:" +
                                    "/usr/bin:" +
                                    "/sbin:" +
                                    "/bin; " +
                                    "exec chroot " +
                                    rootfs +
                                    " /bin/bash --noprofile --norc";

                            builder =
                                    new ProcessBuilder(
                                            "/debug_ramdisk/su",
                                            "-c",
                                            command
                                    );

                        } else {

                            builder =
                                    new ProcessBuilder(
                                            "/debug_ramdisk/su",
                                            "-c",
                                            "exec /system/bin/sh"
                                    );
                        }

                        builder.redirectErrorStream(
                                true
                        );

                        shell =
                                builder.start();

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

                        runOnUiThread(
                                () -> {

                                    printOutput(
                                            "METMC Linux Terminal\n\n"
                                    );

                                    addPromptLine();
                                }
                        );

                        String line;

                        while (
                                (
                                        line =
                                                shellOut.readLine()
                                ) != null
                        ) {

                            if (
                                    line.equals(
                                            END_MARKER
                                    )
                            ) {

                                runOnUiThread(
                                        this::addPromptLine
                                );

                            } else {

                                printOutput(
                                        line + "\n"
                                );
                            }
                        }

                    } catch (Exception e) {

                        printOutput(
                                "\nTerminal failed to start:\n" +
                                e.getMessage() +
                                "\n"
                        );
                    }
                }
        ).start();
    }

    private void printOutput(
            String text
    ) {

        runOnUiThread(
                () -> {

                    if (
                            terminalContent == null
                    ) {
                        return;
                    }

                    terminalContent.addView(
                            terminalText(text)
                    );

                    scrollToBottom();
                }
        );
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        try {

            if (
                    shellIn != null
            ) {
                shellIn.close();
            }

        } catch (Exception ignored) {
        }

        try {

            if (
                    shellOut != null
            ) {
                shellOut.close();
            }

        } catch (Exception ignored) {
        }

        if (
                shell != null
        ) {
            shell.destroy();
        }
    }
}
