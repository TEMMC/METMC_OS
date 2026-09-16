package com.metmc.os.desktop

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.widget.*
import java.io.File
import java.util.Locale

class MetmcFileManagerView(private val context: Context) : LinearLayout(context) {
    private enum class Tab { INTERNAL, SD, LINUX, ROOT }
    private val allTabs = Tab.values()
    private var tab = Tab.INTERNAL
    private var internalPath = "/storage/emulated/0"
    private var sdPath: String? = null
    private var sdCurrent: String? = null
    private var linuxPath = "/"
    private var rootPath = "/"
    private val path = TextView(context)
    private val list = LinearLayout(context)
    private val scroll = ScrollView(context)
    private val tabs = ArrayList<Button>()

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(13, 16, 22))
        toolbar()
        tabBar()
        pathBar()
        scroll.addView(list, ScrollView.LayoutParams(-1, -2))
        addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        detectSdAndRefresh()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun toolbar() {
        val bar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            setBackgroundColor(Color.rgb(24, 28, 37))
        }
        fun button(text: String, action: () -> Unit) = Button(context).apply {
            this.text = text
            isAllCaps = false
            textSize = 12f
            setTextColor(Color.WHITE)
            setOnClickListener { action() }
        }
        bar.addView(button("‹") { up() }, LinearLayout.LayoutParams(dp(48), dp(42)))
        bar.addView(button("↻ Refresh") { refresh() }, LinearLayout.LayoutParams(0, dp(42), 1f))
        bar.addView(button("＋ Folder") { newFolder() }, LinearLayout.LayoutParams(dp(96), dp(42)))
        bar.addView(button("Home") { home() }, LinearLayout.LayoutParams(dp(70), dp(42)))
        addView(bar, LinearLayout.LayoutParams(-1, dp(56)))
    }

    private fun tabBar() {
        val row = LinearLayout(context).apply { orientation = HORIZONTAL; setBackgroundColor(Color.rgb(18, 21, 28)) }
        arrayOf("Internal", "SD Card", "Linux", "ROOT").forEachIndexed { index, label ->
            val button = Button(context).apply {
                text = label
                isAllCaps = false
                textSize = 11f
                setOnClickListener { tab = allTabs[index]; redrawTabs(); refresh() }
            }
            tabs.add(button)
            row.addView(button, LinearLayout.LayoutParams(0, dp(46), 1f))
        }
        addView(row, LinearLayout.LayoutParams(-1, dp(46)))
        redrawTabs()
    }

    private fun redrawTabs() {
        tabs.forEachIndexed { index, button ->
            val selected = allTabs[index] == tab
            button.setTextColor(if (selected) Color.WHITE else Color.rgb(145, 152, 166))
            button.setBackgroundColor(if (selected) Color.rgb(50, 57, 72) else Color.rgb(18, 21, 28))
            button.isEnabled = index != 1 || sdPath != null
        }
    }

    private fun pathBar() {
        path.setTextColor(Color.rgb(178, 205, 255))
        path.textSize = 12f
        path.typeface = Typeface.MONOSPACE
        path.setSingleLine(true)
        path.setPadding(dp(12), dp(8), dp(12), dp(8))
        path.setBackgroundColor(Color.rgb(10, 12, 17))
        addView(path, LinearLayout.LayoutParams(-1, dp(38)))
    }

    private fun detectSdAndRefresh() {
        Thread {
            val output = runRoot("for d in /storage/*; do [ -d \"$d\" ] && case \"$d\" in /storage/emulated|/storage/self) ;; *) echo \"$d\";; esac; done 2>/dev/null")
            val candidates = output.lines().map { it.trim() }.filter { it.startsWith("/storage/") }
            sdPath = candidates.firstOrNull()
            sdCurrent = sdPath
            (context as? Activity)?.runOnUiThread { redrawTabs(); refresh() }
        }.start()
    }

    private fun refresh() {
        path.text = when (tab) {
            Tab.INTERNAL -> internalPath
            Tab.SD -> sdCurrent ?: sdPath ?: "/storage (no SD card detected)"
            Tab.LINUX -> "LINUX:$linuxPath"
            Tab.ROOT -> "ROOT:$rootPath"
        }
        list.removeAllViews()
        when (tab) {
            Tab.INTERNAL -> loadAndroidPath(internalPath)
            Tab.SD -> sdCurrent?.let { loadAndroidPath(it) } ?: show("No removable SD card was detected.")
            Tab.LINUX -> loadLinux(linuxPath)
            Tab.ROOT -> loadAndroidPath(rootPath)
        }
    }

    private fun loadAndroidPath(target: String) {
        show("Loading…")
        Thread {
            val script = "target=${quote(target)}; for x in \"$target\"/* \"$target\"/.[!.]* \"$target\"/..?*; do [ -e \"$x\" ] || continue; if [ -d \"$x\" ]; then printf 'd\\t%s\\t0\\n' \"$x\"; else s=$(stat -c %s \"$x\" 2>/dev/null || echo 0); printf 'f\\t%s\\t%s\\n' \"$x\" \"$s\"; fi; done"
            val output = runRoot("sh -c ${quote(script)}")
            (context as? Activity)?.runOnUiThread {
                list.removeAllViews()
                if (output.startsWith("ERROR")) show(output.trim()) else renderFind(output)
            }
        }.start()
    }

    private fun loadLinux(target: String) {
        show("Loading Debian…")
        Thread {
            val script = "for x in \"$target\"/* \"$target\"/.[!.]* \"$target\"/..?*; do [ -e \"$x\" ] || continue; if [ -d \"$x\" ]; then printf 'd\\t%s\\t0\\n' \"$x\"; else s=$(stat -c %s \"$x\" 2>/dev/null || echo 0); printf 'f\\t%s\\t%s\\n' \"$x\" \"$s\"; fi; done"
            val command = "chroot /data/local/linux/rootfs /bin/sh -c ${quote(script)}"
            val output = runRoot(command)
            (context as? Activity)?.runOnUiThread {
                list.removeAllViews()
                if (output.startsWith("ERROR")) show(output.trim()) else renderFind(output)
            }
        }.start()
    }

    private fun renderFind(raw: String) {
        val rows = raw.lines().mapNotNull {
            val parts = it.split('\t', limit = 3)
            if (parts.size < 3) null else Triple(parts[0], parts[1], parts[2])
        }.sortedWith(compareByDescending<Triple<String, String, String>> { it.first == "d" }.thenBy { it.second.substringAfterLast('/').lowercase(Locale.ROOT) })
        if (rows.isEmpty()) { show("This folder is empty."); return }
        rows.forEach { (type, full, bytes) ->
            val name = full.trimEnd('/').substringAfterLast('/')
            addRow(if (type == "d") "▰" else "□", name, if (type == "d") "Folder" else formatSize(bytes)) {
                if (type == "d") enter(full) else open(full)
            }
        }
    }

    private fun enter(full: String) {
        when (tab) {
            Tab.INTERNAL -> internalPath = full
            Tab.SD -> sdCurrent = full
            Tab.LINUX -> linuxPath = full
            Tab.ROOT -> rootPath = full
        }
        refresh()
    }

    private fun addRow(icon: String, name: String, detail: String, action: () -> Unit) {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(5), dp(14), dp(5))
            setOnClickListener { action() }
        }
        row.addView(TextView(context).apply { text = icon; textSize = 22f; gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(44), dp(58)))
        val texts = LinearLayout(context).apply { orientation = VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        texts.addView(TextView(context).apply { text = name; textSize = 15f; setTextColor(Color.WHITE); maxLines = 1 })
        texts.addView(TextView(context).apply { text = detail; textSize = 11f; setTextColor(Color.rgb(145, 152, 166)); maxLines = 1 })
        row.addView(texts, LinearLayout.LayoutParams(0, dp(58), 1f))
        list.addView(row, LinearLayout.LayoutParams(-1, dp(68)))
    }

    private fun show(message: String) {
        list.addView(TextView(context).apply { text = message; textSize = 14f; setTextColor(Color.LTGRAY); setPadding(dp(18), dp(22), dp(18), dp(22)) }, LinearLayout.LayoutParams(-1, -2))
    }

    private fun home() {
        when (tab) {
            Tab.INTERNAL -> internalPath = "/storage/emulated/0"
            Tab.SD -> sdCurrent = sdPath
            Tab.LINUX -> linuxPath = "/"
            Tab.ROOT -> rootPath = "/"
        }
        refresh()
    }

    private fun up() {
        when (tab) {
            Tab.INTERNAL -> if (internalPath != "/storage/emulated/0") internalPath = parent(internalPath, "/storage/emulated/0")
            Tab.SD -> { val base = sdPath; if (base != null && sdCurrent != null && sdCurrent != base) sdCurrent = parent(sdCurrent!!, base) }
            Tab.LINUX -> if (linuxPath != "/") linuxPath = parent(linuxPath, "/")
            Tab.ROOT -> if (rootPath != "/") rootPath = parent(rootPath, "/")
        }
        refresh()
    }

    private fun parent(pathValue: String, root: String): String {
        val clean = pathValue.trimEnd('/')
        val parent = clean.substringBeforeLast('/', '/')
        return if (parent.length < root.length) root else parent.ifBlank { "/" }
    }

    private fun newFolder() {
        val input = EditText(context).apply { hint = "Folder name" }
        AlertDialog.Builder(context).setTitle("New folder").setView(input).setNegativeButton("Cancel", null).setPositiveButton("Create") { _, _ ->
            val name = input.text.toString().trim()
            if (name.isBlank() || name.contains('/') || name == "." || name == "..") {
                Toast.makeText(context, "Invalid folder name", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            val base = when (tab) { Tab.INTERNAL -> internalPath; Tab.SD -> sdCurrent; Tab.LINUX -> linuxPath; Tab.ROOT -> rootPath }
            val target = if (tab == Tab.LINUX) "chroot /data/local/linux/rootfs /bin/mkdir -p ${quote("$base/$name")}" else "mkdir -p ${quote("$base/$name")}"
            Thread {
                val result = runRoot(target)
                (context as? Activity)?.runOnUiThread {
                    if (result.startsWith("ERROR")) Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                    refresh()
                }
            }.start()
        }.show()
    }

    private fun open(full: String) {
        try {
            val file = File(full)
            if (file.exists()) {
                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse("file://$full"), "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            } else Toast.makeText(context, "File is not directly readable by Android", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) { Toast.makeText(context, "No application can open this file", Toast.LENGTH_SHORT).show() }
    }

    private fun runRoot(command: String): String = try {
        val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        if (code == 0) text else "ERROR: $text"
    } catch (e: Exception) { "ERROR: ${e.message}" }

    private fun quote(value: String) = "'" + value.replace("'", "'\\''") + "'"

    private fun formatSize(value: String): String {
        val number = value.toLongOrNull() ?: 0
        return when {
            number >= 1073741824 -> String.format(Locale.US, "%.1f GB", number / 1073741824.0)
            number >= 1048576 -> String.format(Locale.US, "%.1f MB", number / 1048576.0)
            number >= 1024 -> String.format(Locale.US, "%.1f KB", number / 1024.0)
            else -> "$number B"
        }
    }
}
