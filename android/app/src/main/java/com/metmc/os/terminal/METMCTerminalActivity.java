package com.metmc.os.terminal;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

import java.nio.charset.StandardCharsets;

/**
 * METMC terminal using the actual Termux terminal emulator view.
 *
 * There is deliberately NO EditText command-entry field. The TerminalView owns
 * the input connection, PTY, cursor, selection, clipboard and keyboard events.
 */
public final class METMCTerminalActivity extends Activity
        implements TerminalSessionClient, TerminalViewClient {

    private static final String ROOTFS = "/data/local/linux/rootfs";
    private static final String SU = "/system/bin/su";

    private TerminalView terminalView;
    private TerminalSession session;
    private boolean ctrlDown;
    private boolean altDown;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        window.setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = window.getInsetsController();
            if (c != null) c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        buildUi();
        startDebianSession();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(8, 12, 10));

        terminalView = new TerminalView(this, null);
        terminalView.setBackgroundColor(Color.rgb(8, 12, 10));
        terminalView.setFocusable(true);
        terminalView.setFocusableInTouchMode(true);
        terminalView.setTerminalViewClient(this);
        terminalView.setTextSize(13);

        root.addView(terminalView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(buildExtraKeys());

        setContentView(root);
        terminalView.requestFocus();
    }

    private View buildExtraKeys() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setFillViewport(false);
        scroll.setBackgroundColor(Color.rgb(20, 25, 23));

        LinearLayout keys = new LinearLayout(this);
        keys.setGravity(Gravity.CENTER_VERTICAL);
        keys.setPadding(6, 5, 6, 5);

        addKey(keys, "ESC", () -> send("\u001b"));
        addKey(keys, "CTRL", () -> ctrlDown = !ctrlDown);
        addKey(keys, "ALT", () -> altDown = !altDown);
        addKey(keys, "TAB", () -> send("\t"));
        addKey(keys, "←", () -> send("\u001b[D"));
        addKey(keys, "→", () -> send("\u001b[C"));
        addKey(keys, "↑", () -> send("\u001b[A"));
        addKey(keys, "↓", () -> send("\u001b[B"));
        addKey(keys, "HOME", () -> send("\u001b[H"));
        addKey(keys, "END", () -> send("\u001b[F"));
        addKey(keys, "PGUP", () -> send("\u001b[5~"));
        addKey(keys, "PGDN", () -> send("\u001b[6~"));
        addKey(keys, "PASTE", this::pasteFromClipboard);
        addKey(keys, "CTRL+C", () -> sendByte(3));
        addKey(keys, "CTRL+D", () -> sendByte(4));
        addKey(keys, "CTRL+Z", () -> sendByte(26));

        scroll.addView(keys);
        return scroll;
    }

    private void addKey(LinearLayout parent, String label, final Runnable action) {
        TextView b = new TextView(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setGravity(Gravity.CENTER);
        b.setSingleLine(true);
        b.setPadding(18, 11, 18, 11);
        b.setOnClickListener(v -> action.run());
        parent.addView(b, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void startDebianSession() {
        String command =
                "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                "export HOME=/root; export TERM=xterm-256color; " +
                "export COLORTERM=truecolor; export LANG=C.UTF-8; " +
                "exec chroot " + ROOTFS + " /bin/bash --login";

        String[] args = new String[]{"-c", command};
        String[] env = new String[]{
                "TERM=xterm-256color",
                "COLORTERM=truecolor",
                "HOME=/root",
                "LANG=C.UTF-8",
                "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                "SHELL=/bin/bash"
        };

        session = new TerminalSession(SU, ROOTFS, args, env, 5000, this);
        terminalView.attachSession(session);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            terminalView.requestFocus();
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT);
        }, 350);
    }

    private void send(String value) {
        if (session == null || !session.isRunning()) return;
        byte[] data = value.getBytes(StandardCharsets.UTF_8);
        session.write(data, 0, data.length);
        ctrlDown = false;
        altDown = false;
    }

    private void sendByte(int value) {
        if (session == null || !session.isRunning()) return;
        byte[] data = new byte[]{(byte) value};
        session.write(data, 0, 1);
        ctrlDown = false;
        altDown = false;
    }

    private void pasteFromClipboard() {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip()) return;
        ClipData clip = cm.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return;
        CharSequence text = clip.getItemAt(0).coerceToText(this);
        if (!TextUtils.isEmpty(text)) send(text.toString());
    }

    @Override
    public void onCopyTextToClipboard(@NonNull TerminalSession s, String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("METMC Terminal", text));
    }

    @Override
    public void onPasteTextFromClipboard(@Nullable TerminalSession s) {
        pasteFromClipboard();
    }

    @Override public void onTextChanged(@NonNull TerminalSession s) { terminalView.invalidate(); }
    @Override public void onTitleChanged(@NonNull TerminalSession s) { }
    @Override public void onSessionFinished(@NonNull TerminalSession s) { }
    @Override public void onBell(@NonNull TerminalSession s) { }
    @Override public void onColorsChanged(@NonNull TerminalSession s) { terminalView.invalidate(); }
    @Override public void onTerminalCursorStateChange(boolean state) { terminalView.invalidate(); }
    @Override public void setTerminalShellPid(@NonNull TerminalSession s, int pid) { }

    @Override public Integer getTerminalCursorStyle() { return null; }

    @Override public float onScale(float scale) { return scale; }

    @Override public void onSingleTapUp(MotionEvent e) {
        terminalView.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override public boolean shouldBackButtonBeMappedToEscape() { return false; }
    @Override public boolean shouldEnforceCharBasedInput() { return true; }
    @Override public boolean shouldUseCtrlSpaceWorkaround() { return false; }
    @Override public boolean isTerminalViewSelected() { return terminalView != null && terminalView.hasFocus(); }
    @Override public void copyModeChanged(boolean copyMode) { }
    @Override public boolean onKeyDown(int keyCode, KeyEvent e, TerminalSession s) { return false; }
    @Override public boolean onKeyUp(int keyCode, KeyEvent e) { return false; }
    @Override public boolean onLongPress(MotionEvent event) { return false; }

    @Override public boolean readControlKey() { return ctrlDown; }
    @Override public boolean readAltKey() { return altDown; }
    @Override public boolean readShiftKey() { return false; }
    @Override public boolean readFnKey() { return false; }

    @Override
    public boolean onCodePoint(int codePoint, boolean terminalCtrlDown, TerminalSession s) {
        boolean c = ctrlDown || terminalCtrlDown;
        boolean a = altDown;
        if (c || a) {
            s.writeCodePoint(a, c && codePoint >= 'a' && codePoint <= 'z'
                    ? codePoint - 'a' + 1 : codePoint);
            ctrlDown = false;
            altDown = false;
            return true;
        }
        return false;
    }

    @Override public void onEmulatorSet() { }

    @Override public void logError(String tag, String message) { }
    @Override public void logWarn(String tag, String message) { }
    @Override public void logInfo(String tag, String message) { }
    @Override public void logDebug(String tag, String message) { }
    @Override public void logVerbose(String tag, String message) { }
    @Override public void logStackTraceWithMessage(String tag, String message, Exception e) { }
    @Override public void logStackTrace(String tag, Exception e) { }

    @Override
    protected void onDestroy() {
        if (session != null) session.finishIfRunning();
        super.onDestroy();
    }
}
