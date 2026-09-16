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
    private var tab = Tab.INTERNAL
    private var internalPath = "/storage/emulated/0"
    private var sdPath: String? = null
    private var sdCurrent: String? = null
    private var linuxPath = "/"
    private var rootPath = "/"
    private val path = TextView(context)
    private val list = LinearLayout(context)
    private val tabs = ArrayList<Button>()

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(13, 16, 22))
        buildToolbar(); buildTabs()
        path.typeface = Typeface.MONOSPACE; path.textSize = 12f; path.setTextColor(Color.rgb(178, 205, 255)); path.setSingleLine(true); path.setPadding(dp(12), dp(8), dp(12), dp(8)); path.setBackgroundColor(Color.rgb(10, 12, 17))
        addView(path, LinearLayout.LayoutParams(-1, dp(38)))
        val scroll = ScrollView(context); scroll.addView(list, ScrollView.LayoutParams(-1, -2)); addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        detectSd(); refresh()
    }

    private fun buildToolbar() {
        val bar = LinearLayout(context); bar.orientation = HORIZONTAL; bar.gravity = Gravity.CENTER_VERTICAL; bar.setPadding(dp(8), dp(6), dp(8), dp(6)); bar.setBackgroundColor(Color.rgb(24, 28, 37))
        bar.addView(actionButton("‹") { up() }, LinearLayout.LayoutParams(dp(48), dp(42)))
        bar.addView(actionButton("↻ Refresh") { refresh() }, LinearLayout.LayoutParams(0, dp(42), 1f))
        bar.addView(actionButton("＋ Folder") { newFolder() }, LinearLayout.LayoutParams(dp(96), dp(42)))
        bar.addView(actionButton("Home") { home() }, LinearLayout.LayoutParams(dp(70), dp(42)))
        addView(bar, LinearLayout.LayoutParams(-1, dp(56)))
    }

    private fun buildTabs() {
        val row = LinearLayout(context); row.orientation = HORIZONTAL; row.setBackgroundColor(Color.rgb(18, 21, 28))
        arrayOf("Internal", "SD Card", "Linux", "ROOT").forEachIndexed { index, label ->
            val button = Button(context); button.text = label; button.isAllCaps = false; button.textSize = 11f
            button.setOnClickListener { tab = Tab.values()[index]; redrawTabs(); refresh() }; tabs.add(button); row.addView(button, LinearLayout.LayoutParams(0, dp(46), 1f))
        }
        addView(row, LinearLayout.LayoutParams(-1, dp(46))); redrawTabs()
    }

    private fun redrawTabs() {
        tabs.forEachIndexed { index, button ->
            val selected = Tab.values()[index] == tab
            button.setTextColor(if (selected) Color.WHITE else Color.rgb(145, 152, 166)); button.setBackgroundColor(if (selected) Color.rgb(50, 57, 72) else Color.rgb(18, 21, 28)); button.isEnabled = index != 1 || sdPath != null
        }
    }

    private fun detectSd() {
        Thread {
            val command = "for d in /storage/*; do [ -d \"${'$'}d\" ] || continue; case \"${'$'}d\" in /storage/emulated|/storage/self) ;; *) echo \"${'$'}d\";; esac; done 2>/dev/null"
            val output = runRoot(command)
            val candidate = output.lines().map { it.trim() }.firstOrNull { it.startsWith("/storage/") }
            sdPath = candidate; sdCurrent = candidate
            (context as? Activity)?.runOnUiThread { redrawTabs(); refresh() }
        }.start()
    }

    private fun refresh() {
        path.text = when (tab) { Tab.INTERNAL -> internalPath; Tab.SD -> sdCurrent ?: sdPath ?: "/storage (no SD card detected)"; Tab.LINUX -> "LINUX:$linuxPath"; Tab.ROOT -> "ROOT:$rootPath" }
        list.removeAllViews()
        when (tab) { Tab.INTERNAL -> loadAndroidPath(internalPath); Tab.SD -> if (sdCurrent != null) loadAndroidPath(sdCurrent!!) else showMessage("No removable SD card detected."); Tab.LINUX -> loadLinux(linuxPath); Tab.ROOT -> loadAndroidPath(rootPath) }
    }

    private fun loadAndroidPath(target: String) {
        showMessage("Loading…")
        Thread {
            val script = "target=${quote(target)}; for x in \"${'$'}target\"/* \"${'$'}target\"/.[!.]* \"${'$'}target\"/..?*; do [ -e \"${'$'}x\" ] || continue; if [ -d \"${'$'}x\" ]; then printf 'd\\t%s\\t0\\n' \"${'$'}x\"; else size=\$(stat -c %s \"${'$'}x\" 2>/dev/null || echo 0); printf 'f\\t%s\\t%s\\n' \"${'$'}x\" \"${'$'}size\"; fi; done"
            val output = runRoot("sh -c ${quote(script)}")
            (context as? Activity)?.runOnUiThread { list.removeAllViews(); if (output.startsWith("ERROR")) showMessage(output.trim()) else renderRows(output) }
        }.start()
    }

    private fun loadLinux(target: String) {
        showMessage("Loading Debian…")
        Thread {
            val script = "for x in ${quote(target)}/* ${quote(target)}/.[!.]* ${quote(target)}/..?*; do [ -e \"${'$'}x\" ] || continue; if [ -d \"${'$'}x\" ]; then printf 'd\\t%s\\t0\\n' \"${'$'}x\"; else size=\$(stat -c %s \"${'$'}x\" 2>/dev/null || echo 0); printf 'f\\t%s\\t%s\\n' \"${'$'}x\" \"${'$'}size\"; fi; done"
            val output = runRoot("chroot /data/local/linux/rootfs /bin/sh -c ${quote(script)}")
            (context as? Activity)?.runOnUiThread { list.removeAllViews(); if (output.startsWith("ERROR")) showMessage(output.trim()) else renderRows(output) }
        }.start()
    }

    private fun renderRows(raw: String) {
        val rows = raw.lines().mapNotNull { line -> val parts = line.split('\t', limit = 3); if (parts.size == 3) Triple(parts[0], parts[1], parts[2]) else null }.sortedWith(compareByDescending<Triple<String, String, String>> { it.first == "d" }.thenBy { it.second.substringAfterLast('/').lowercase(Locale.ROOT) })
        if (rows.isEmpty()) { showMessage("This folder is empty."); return }
        rows.forEach { row -> val type = row.first; val full = row.second; val bytes = row.third; val name = full.trimEnd('/').substringAfterLast('/'); addRow(if (type == "d") "▰" else "□", name, if (type == "d") "Folder" else formatSize(bytes)) { if (type == "d") enter(full) else open(full) } }
    }

    private fun enter(full: String) { when (tab) { Tab.INTERNAL -> internalPath = full; Tab.SD -> sdCurrent = full; Tab.LINUX -> linuxPath = full; Tab.ROOT -> rootPath = full }; refresh() }
    private fun addRow(icon: String, name: String, detail: String, action: () -> Unit) {
        val row = LinearLayout(context); row.orientation = HORIZONTAL; row.gravity = Gravity.CENTER_VERTICAL; row.setPadding(dp(14), dp(5), dp(14), dp(5)); row.setOnClickListener { action() }
        val iconView = TextView(context); iconView.text = icon; iconView.textSize = 22f; iconView.gravity = Gravity.CENTER; row.addView(iconView, LinearLayout.LayoutParams(dp(44), dp(58)))
        val texts = LinearLayout(context); texts.orientation = VERTICAL; texts.gravity = Gravity.CENTER_VERTICAL
        val nameView = TextView(context); nameView.text = name; nameView.textSize = 15f; nameView.setTextColor(Color.WHITE); nameView.maxLines = 1
        val detailView = TextView(context); detailView.text = detail; detailView.textSize = 11f; detailView.setTextColor(Color.rgb(145, 152, 166)); detailView.maxLines = 1
        texts.addView(nameView); texts.addView(detailView); row.addView(texts, LinearLayout.LayoutParams(0, dp(58), 1f)); list.addView(row, LinearLayout.LayoutParams(-1, dp(68)))
    }
    private fun showMessage(message: String) { val view = TextView(context); view.text = message; view.textSize = 14f; view.setTextColor(Color.LTGRAY); view.setPadding(dp(18), dp(22), dp(18), dp(22)); list.addView(view, LinearLayout.LayoutParams(-1, -2)) }
    private fun home() { when (tab) { Tab.INTERNAL -> internalPath = "/storage/emulated/0"; Tab.SD -> sdCurrent = sdPath; Tab.LINUX -> linuxPath = "/"; Tab.ROOT -> rootPath = "/" }; refresh() }
    private fun up() { when (tab) { Tab.INTERNAL -> if (internalPath != "/storage/emulated/0") internalPath = parentPath(internalPath, "/storage/emulated/0"); Tab.SD -> { val base = sdPath; val current = sdCurrent; if (base != null && current != null && current != base) sdCurrent = parentPath(current, base) }; Tab.LINUX -> if (linuxPath != "/") linuxPath = parentPath(linuxPath, "/"); Tab.ROOT -> if (rootPath != "/") rootPath = parentPath(rootPath, "/") }; refresh() }
    private fun parentPath(value: String, root: String): String { val clean = value.trimEnd('/'); val slash = clean.lastIndexOf('/'); val parent = if (slash <= 0) "/" else clean.substring(0, slash); return if (parent.length < root.length) root else parent }
    private fun newFolder() {
        val input = EditText(context); input.hint = "Folder name"
        AlertDialog.Builder(context).setTitle("New folder").setView(input).setNegativeButton("Cancel", null).setPositiveButton("Create") { _, _ ->
            val name = input.text.toString().trim()
            if (name.isBlank() || name.contains('/') || name == "." || name == "..") { Toast.makeText(context, "Invalid folder name", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            val base = when (tab) { Tab.INTERNAL -> internalPath; Tab.SD -> sdCurrent ?: return@setPositiveButton; Tab.LINUX -> linuxPath; Tab.ROOT -> rootPath }
            val target = "$base/$name"; val command = if (tab == Tab.LINUX) "chroot /data/local/linux/rootfs /bin/mkdir -p ${quote(target)}" else "mkdir -p ${quote(target)}"
            Thread { runRoot(command); (context as? Activity)?.runOnUiThread { refresh() } }.start()
        }.show()
    }
    private fun open(full: String) {
        try { val file = File(full); if (!file.exists()) { Toast.makeText(context, "File is not directly readable by Android", Toast.LENGTH_SHORT).show(); return }; context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(Uri.parse("file://$full"), "*/*"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }) }
        catch (_: Exception) { Toast.makeText(context, "No application can open this file", Toast.LENGTH_SHORT).show() }
    }
    private fun actionButton(label: String, action: () -> Unit) = Button(context).apply { text = label; isAllCaps = false; textSize = 12f; setTextColor(Color.WHITE); setOnClickListener { action() } }
    private fun runRoot(command: String): String = try { val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start(); val text = process.inputStream.bufferedReader().readText(); val code = process.waitFor(); if (code == 0) text else "ERROR: $text" } catch (e: Exception) { "ERROR: ${e.message}" }
    private fun quote(value: String) = "'" + value.replace("'", "'\\''") + "'"
    private fun formatSize(value: String): String { val number = value.toLongOrNull() ?: 0L; return when { number >= 1073741824L -> String.format(Locale.US, "%.1f GB", number / 1073741824.0); number >= 1048576L -> String.format(Locale.US, "%.1f MB", number / 1048576.0); number >= 1024L -> String.format(Locale.US, "%.1f KB", number / 1024.0); else -> "$number B" } }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
