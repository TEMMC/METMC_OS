package com.metmc.os.desktop

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modern METMC file manager.
 * Shared storage is rooted at /storage/emulated/0 rather than /storage.
 * A root fallback is used on rooted METMC devices when Android app storage
 * restrictions prevent normal File APIs from reading the directory.
 */
class MetmcFileManagerView(private val context: Context) : LinearLayout(context) {
    private enum class Tab { SHARED, LINUX, ROOT }

    private var tab = Tab.SHARED
    private var sharedPath = File(Environment.getExternalStorageDirectory().absolutePath)
    private var linuxPath = "/root"
    private var rootPath = "/"

    private val path = TextView(context)
    private val list = LinearLayout(context)
    private val scroll = ScrollView(context)
    private val shared = Button(context)
    private val linux = Button(context)
    private val root = Button(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(13, 16, 22))
        toolbar()
        tabs()
        pathBar()
        scroll.addView(list, LayoutParams(-1, -2))
        addView(scroll, LayoutParams(-1, 0, 1f))
        refresh()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun toolbar() {
        val bar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(7), dp(10), dp(7))
            setBackgroundColor(Color.rgb(24, 28, 37))
        }
        fun b(label: String, action: () -> Unit) = Button(context).apply {
            text = label; isAllCaps = false; textSize = 12f
            setTextColor(Color.WHITE); setOnClickListener { action() }
        }
        bar.addView(b("‹", { up() }), LayoutParams(dp(52), dp(42)))
        bar.addView(b("↻ Refresh", { refresh() }), LayoutParams(0, dp(42), 1f))
        bar.addView(b("＋ Folder", { newFolder() }), LayoutParams(0, dp(42), 1f))
        bar.addView(b("⌕", { Toast.makeText(context, "Search is available in the current folder", Toast.LENGTH_SHORT).show() }), LayoutParams(dp(52), dp(42)))
        addView(bar, LayoutParams(-1, dp(56)))
    }

    private fun tabs() {
        val row = LinearLayout(context).apply { orientation = HORIZONTAL; setBackgroundColor(Color.rgb(18, 21, 28)) }
        style(shared, "Shared Storage", true)
        style(linux, "Linux", false)
        style(root, "ROOT", false)
        shared.setOnClickListener { tab = Tab.SHARED; redrawTabs(); refresh() }
        linux.setOnClickListener { tab = Tab.LINUX; redrawTabs(); refresh() }
        root.setOnClickListener {
            if (!hasRoot()) { Toast.makeText(context, "Root access unavailable", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            tab = Tab.ROOT; redrawTabs(); refresh()
        }
        row.addView(shared, LayoutParams(0, dp(46), 1f)); row.addView(linux, LayoutParams(0, dp(46), 1f)); row.addView(root, LayoutParams(0, dp(46), 1f))
        addView(row, LayoutParams(-1, dp(46)))
    }

    private fun style(v: Button, text: String, selected: Boolean) {
        v.text = text; v.isAllCaps = false; v.textSize = 12f
        v.setTextColor(if (selected) Color.WHITE else Color.rgb(145, 152, 166))
        v.setBackgroundColor(if (selected) Color.rgb(50, 57, 72) else Color.rgb(18, 21, 28))
    }

    private fun redrawTabs() {
        style(shared, "Shared Storage", tab == Tab.SHARED)
        style(linux, "Linux", tab == Tab.LINUX)
        style(root, "ROOT", tab == Tab.ROOT)
    }

    private fun pathBar() {
        path.setTextColor(Color.rgb(178, 205, 255)); path.textSize = 12f; path.typeface = Typeface.MONOSPACE
        path.setSingleLine(true); path.setPadding(dp(12), dp(8), dp(12), dp(8)); path.setBackgroundColor(Color.rgb(10, 12, 17))
        addView(path, LayoutParams(-1, dp(38)))
    }

    private fun refresh() {
        path.text = when (tab) { Tab.SHARED -> sharedPath.absolutePath; Tab.LINUX -> "LINUX:$linuxPath"; Tab.ROOT -> "ROOT:$rootPath" }
        list.removeAllViews()
        when (tab) { Tab.SHARED -> loadShared(); Tab.LINUX -> loadLinux(); Tab.ROOT -> loadRoot() }
    }

    private fun loadShared() {
        if (!sharedPath.exists()) { show("Shared storage is unavailable."); return }
        val entries = sharedPath.listFiles()
        if (entries == null) {
            show("Android denied direct filesystem access to this folder.")
            if (BuildVersion.isManageExternalStorageSupported() && !Environment.isExternalStorageManager()) addPermissionButton()
            else if (hasRoot()) loadSharedAsRoot()
            return
        }
        renderFiles(entries)
    }

    private fun addPermissionButton() {
        val b = Button(context).apply {
            text = "Grant Shared Storage Access"; isAllCaps = false
            setOnClickListener {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
                    context.startActivity(intent)
                } catch (_: Exception) { Toast.makeText(context, "Open Android storage settings manually", Toast.LENGTH_LONG).show() }
            }
        }
        list.addView(b, LayoutParams(-1, dp(54)))
        show("Storage access is not granted to METMC OS.")
    }

    private fun loadSharedAsRoot() {
        Thread {
            val output = runRoot("find ${quote(sharedPath.absolutePath)} -maxdepth 1 -mindepth 1 -printf '%y\\t%p\\t%s\\t%T@\\n' 2>&1")
            (context as? Activity)?.runOnUiThread { renderRootLike(output, false) }
        }.start()
    }

    private fun renderFiles(entries: Array<File>) {
        val sorted = entries.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) })
        if (sorted.isEmpty()) { show("This folder is empty."); return }
        val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        sorted.forEach { f ->
            addRow(if (f.isDirectory) "▰" else "□", f.name, if (f.isDirectory) "Folder" else "${size(f.length())} • ${fmt.format(Date(f.lastModified()))}") {
                if (f.isDirectory) { sharedPath = f; refresh() } else open(f)
            }
        }
    }

    private fun loadLinux() { loadShellListing(linuxPath, true) }
    private fun loadRoot() { if (hasRoot()) loadShellListing(rootPath, false) else show("Root access unavailable.") }

    private fun loadShellListing(target: String, chroot: Boolean) {
        show("Loading…")
        Thread {
            val command = if (chroot) {
                "chroot /data/local/linux/rootfs /bin/sh -c ${quote("ls -la --time-style=+%s ${quote(target)} 2>&1")}" 
            } else "ls -la ${quote(target)} 2>&1"
            val output = runRoot(command)
            (context as? Activity)?.runOnUiThread {
                list.removeAllViews()
                if (output.contains("Permission denied") || output.contains("No such file")) show("Cannot read path.\n$output") else renderRootLike(output, !chroot)
            }
        }.start()
    }

    private fun renderRootLike(raw: String, rootFs: Boolean) {
        var count = 0
        raw.lines().forEach { line ->
            val p = line.trim().split(Regex("\\s+"), limit = 9)
            if (p.size < 9 || p[8] == "." || p[8] == ".." || p[8].startsWith("total")) return@forEach
            val name = p[8]; val dir = p[0].startsWith("d")
            addRow(if (dir) "▰" else "□", name, if (rootFs) "ROOT • ${p[0]}" else "Linux • ${p[0]}") {
                if (dir) {
                    if (tab == Tab.LINUX) linuxPath = if (linuxPath == "/") "/$name" else "$linuxPath/$name"
                    else rootPath = if (rootPath == "/") "/$name" else "$rootPath/$name"
                    refresh()
                }
            }
            count++
        }
        if (count == 0) show("This folder is empty.")
    }

    private fun addRow(icon: String, name: String, detail: String, action: () -> Unit) {
        val row = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(6), dp(14), dp(6)); setOnClickListener { action() } }
        val i = TextView(context).apply { text = icon; textSize = 22f; gravity = Gravity.CENTER }
        row.addView(i, LayoutParams(dp(44), dp(58)))
        val texts = LinearLayout(context).apply { orientation = VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        texts.addView(TextView(context).apply { text = name; textSize = 15f; setTextColor(Color.WHITE); maxLines = 1 })
        texts.addView(TextView(context).apply { text = detail; textSize = 11f; setTextColor(Color.rgb(145, 152, 166)); maxLines = 1 })
        row.addView(texts, LayoutParams(0, dp(58), 1f)); list.addView(row, LayoutParams(-1, dp(70)))
    }

    private fun show(message: String) {
        val t = TextView(context).apply { text = message; textSize = 14f; setTextColor(Color.LTGRAY); setPadding(dp(18), dp(22), dp(18), dp(22)) }
        list.addView(t, LayoutParams(-1, -2))
    }

    private fun up() {
        when (tab) {
            Tab.SHARED -> sharedPath.parentFile?.let { if (it.absolutePath.startsWith("/storage/emulated/0")) { sharedPath = it; refresh() } }
            Tab.LINUX -> if (linuxPath != "/") { linuxPath = linuxPath.trimEnd('/').substringBeforeLast('/').ifBlank { "/" }; refresh() }
            Tab.ROOT -> if (rootPath != "/") { rootPath = rootPath.trimEnd('/').substringBeforeLast('/').ifBlank { "/" }; refresh() }
        }
    }

    private fun newFolder() {
        val input = EditText(context).apply { hint = "Folder name" }
        AlertDialog.Builder(context).setTitle("New folder").setView(input).setNegativeButton("Cancel", null).setPositiveButton("Create") { _, _ ->
            val name = input.text.toString().trim()
            if (name.isBlank() || name.contains("/") || name == "." || name == "..") { Toast.makeText(context, "Invalid folder name", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            if (tab == Tab.SHARED) {
                val f = File(sharedPath, name); if (!f.mkdirs()) Toast.makeText(context, "Unable to create folder", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(context, "Folder creation is available in Shared Storage", Toast.LENGTH_SHORT).show()
            refresh()
        }.show()
    }

    private fun open(file: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(file), "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try { context.startActivity(intent) } catch (_: Exception) { Toast.makeText(context, "No Android app can open this file", Toast.LENGTH_SHORT).show() }
    }

    private fun runRoot(command: String): String = try {
        val p = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
        val text = p.inputStream.bufferedReader().readText(); p.waitFor(); text
    } catch (e: Exception) { "ERROR: ${e.message ?: "root command failed"}" }

    private fun quote(s: String) = "'" + s.replace("'", "'\\''") + "'"
    private fun size(v: Long): String = when { v >= 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f GB", v / 1073741824.0); v >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", v / 1048576.0); v >= 1024L -> String.format(Locale.US, "%.1f KB", v / 1024.0); else -> "$v B" }
    private fun hasRoot(): Boolean = try { runRoot("id").contains("uid=0") } catch (_: Exception) { false }

    private object BuildVersion {
        fun isManageExternalStorageSupported(): Boolean = android.os.Build.VERSION.SDK_INT >= 30
    }
}
