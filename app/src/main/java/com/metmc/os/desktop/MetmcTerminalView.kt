package com.metmc.os.desktop

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader

class MetmcTerminalView(private val context: Context) : LinearLayout(context) {
    private val output = TextView(context)
    private val input = EditText(context)
    private val history = ArrayList<String>()
    private var historyIndex = 0
    private var cwd = "/root"
    private val prompt = "root@debian"
    private val rootfs = "/data/local/linux/rootfs"
    private val interactive = Regex("^(nano|vim|vi|python3?|htop|top|less|man)(\\s.*)?$")

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(5, 7, 10))
        build()
    }

    private fun build() {
        val top = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(5), dp(8), dp(5))
            setBackgroundColor(Color.rgb(20, 24, 31))
        }
        top.addView(TextView(context).apply {
            text = "  METMC TERMINAL  •  Debian ARM64"
            typeface = Typeface.DEFAULT_BOLD
            textSize = 12f
            setTextColor(Color.rgb(225, 230, 238))
        }, LinearLayout.LayoutParams(0, dp(44), 1f))
        top.addView(tool("⌘", "Copy all") { copyAll() }, LinearLayout.LayoutParams(dp(42), dp(40)))
        top.addView(tool("＋", "New shell") { resetShell() }, LinearLayout.LayoutParams(dp(42), dp(40)))
        top.addView(tool("×", "Clear") { clear() }, LinearLayout.LayoutParams(dp(42), dp(40)))
        addView(top, LinearLayout.LayoutParams(-1, dp(52)))

        output.apply {
            setTextColor(Color.rgb(224, 229, 236))
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            text = "METMC Terminal\nDebian ARM64 • root shell\nType 'help' for METMC commands.\n\n"
        }
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            addView(output, FrameLayout.LayoutParams(-1, -2))
        }
        addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val quick = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(3), dp(6), dp(3))
            setBackgroundColor(Color.rgb(15, 18, 23))
        }
        arrayOf("Tab", "Ctrl", "↑", "↓", "/root", "ls -la").forEach { key ->
            quick.addView(Button(context).apply {
                text = key
                isAllCaps = false
                textSize = 10f
                setPadding(0, 0, 0, 0)
                setOnClickListener { quickKey(key) }
            }, LinearLayout.LayoutParams(0, dp(34), 1f))
        }
        addView(quick, LinearLayout.LayoutParams(-1, dp(40)))

        val bar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setBackgroundColor(Color.rgb(20, 23, 28))
        }
        bar.addView(TextView(context).apply {
            text = "root@debian"
            setTextColor(Color.rgb(105, 210, 135))
            textSize = 12f
            typeface = Typeface.MONOSPACE
        }, LinearLayout.LayoutParams(-2, dp(48)))
        bar.addView(TextView(context).apply {
            text = ":$cwd$ "
            setTextColor(Color.rgb(120, 155, 220))
            textSize = 12f
            typeface = Typeface.MONOSPACE
        }, LinearLayout.LayoutParams(-2, dp(48)))
        input.apply {
            hint = "command…"
            setHintTextColor(Color.rgb(85, 92, 102))
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setSingleLine(true)
            background = null
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
        bar.addView(input, LinearLayout.LayoutParams(0, dp(48), 1f))
        addView(bar, LinearLayout.LayoutParams(-1, dp(56)))

        input.setOnKeyListener { _, key, event ->
            if (event.action != KeyEvent.ACTION_DOWN) false else when (key) {
                KeyEvent.KEYCODE_ENTER -> { execute(input.text.toString()); true }
                KeyEvent.KEYCODE_DPAD_UP -> { historyMove(-1); true }
                KeyEvent.KEYCODE_DPAD_DOWN -> { historyMove(1); true }
                else -> false
            }
        }
        input.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) { execute(input.text.toString()); true } else false
        }
        input.requestFocus()
    }

    private fun execute(command: String) {
        val cmd = command.trim()
        input.setText("")
        if (cmd.isBlank()) { append("\n"); return }
        if (history.lastOrNull() != cmd) history.add(cmd)
        historyIndex = history.size
        append("root@debian:$cwd$ $cmd\n")
        when {
            cmd == "clear" -> clear()
            cmd == "exit" -> append("Desktop shell stays attached to METMC OS. Use the × button to close it.\n")
            cmd == "help" -> append(helpText())
            cmd == "pwd" -> append("$cwd\n")
            cmd == "cd" || cmd.startsWith("cd ") -> changeDirectory(cmd)
            cmd == "cls" -> clear()
            interactive.matches(cmd) -> launchInteractive(cmd)
            else -> runCommand(cmd)
        }
    }

    private fun runCommand(cmd: String) {
        Thread {
            try {
                val bridge = "export HOME=/root; export USER=root; export LOGNAME=root; export TERM=xterm-256color; export COLORTERM=truecolor; export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; export DISPLAY=:100; export XDG_RUNTIME_DIR=/tmp/metmc-runtime; cd ${q(cwd)} 2>/dev/null || cd /root; exec /bin/bash -lc ${q(cmd)}"
                val p = ProcessBuilder("su", "-c", "chroot '$rootfs' /bin/bash -c ${q(bridge)}").redirectErrorStream(true).start()
                val r = BufferedReader(InputStreamReader(p.inputStream))
                val result = StringBuilder()
                var line: String?
                while (r.readLine().also { line = it } != null) result.append(line).append('\n')
                val code = p.waitFor()
                (context as? Activity)?.runOnUiThread {
                    append(result.toString())
                    if (code != 0) append("[exit $code]\n")
                    append("\n")
                }
            } catch (e: Exception) {
                (context as? Activity)?.runOnUiThread { append("Terminal error: ${e.message}\n") }
            }
        }.start()
    }

    private fun launchInteractive(cmd: String) {
        try {
            com.metmc.os.linux.LinuxGuiLauncher.launch(context as Activity, rootfs, "xterm -fa Monospace -fs 12 -e $cmd")
            append("↗ Opened $cmd in a real X terminal window.\n")
        } catch (e: Exception) {
            append("Interactive launch failed: ${e.message}\n")
        }
    }

    private fun changeDirectory(cmd: String) {
        val arg = cmd.removePrefix("cd").trim()
        val target = when {
            arg.isBlank() || arg == "~" -> "/root"
            arg.startsWith("/") -> arg
            arg == ".." -> cwd.substringBeforeLast('/').ifBlank { "/" }
            arg.startsWith("../") -> cwd.substringBeforeLast('/').ifBlank { "/" } + "/" + arg.removePrefix("../")
            else -> "$cwd/$arg"
        }.replace("//", "/")
        Thread {
            val check = "[ -d ${q(target)} ]"
            val bridge = "exec /bin/bash -lc ${q(check)}"
            val p = ProcessBuilder("su", "-c", "chroot '$rootfs' /bin/bash -c ${q(bridge)}").redirectErrorStream(true).start()
            val code = p.waitFor()
            (context as? Activity)?.runOnUiThread {
                if (code == 0) {
                    cwd = target
                    append("cwd → $cwd\n\n")
                } else append("cd: no such directory: $arg\n\n")
            }
        }.start()
    }

    private fun quickKey(key: String) {
        when (key) {
            "Tab" -> input.append("\t")
            "Ctrl" -> input.append("^C ")
            "↑" -> historyMove(-1)
            "↓" -> historyMove(1)
            "/root" -> { input.setText("cd /root"); input.setSelection(input.text.length) }
            "ls -la" -> { input.setText("ls -la"); input.setSelection(input.text.length) }
        }
    }

    private fun historyMove(delta: Int) {
        if (history.isEmpty()) return
        historyIndex = (historyIndex + delta).coerceIn(0, history.size)
        input.setText(if (historyIndex < history.size) history[historyIndex] else "")
        input.setSelection(input.text.length)
    }

    private fun copyAll() {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("METMC Terminal", output.text))
        Toast.makeText(context, "Terminal output copied", Toast.LENGTH_SHORT).show()
    }

    private fun resetShell() {
        cwd = "/root"
        history.clear()
        historyIndex = 0
        output.text = "New METMC shell session\nDebian ARM64 • root\n\n"
    }

    private fun clear() { output.text = "" }

    private fun helpText() = """
METMC Terminal commands
  help        show this help
  pwd         show current directory
  cd PATH     change directory
  clear       clear the terminal

All normal Debian commands run directly in the METMC Debian rootfs.
Interactive programs such as nano, vim, python3, top and htop open in a real X terminal.
Environment: ARM64 • root • xterm-256color • DISPLAY=:100

""".trimIndent() + "\n\n"

    private fun append(value: String) {
        output.append(value)
        (output.parent as? ScrollView)?.post { (output.parent as ScrollView).fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun tool(text: String, hint: String, action: () -> Unit) = Button(context).apply {
        this.text = text
        contentDescription = hint
        isAllCaps = false
        textSize = 15f
        setTextColor(Color.WHITE)
        setPadding(0, 0, 0, 0)
        setOnClickListener { action() }
    }

    private fun q(value: String) = "'" + value.replace("'", "'\\''") + "'"
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
