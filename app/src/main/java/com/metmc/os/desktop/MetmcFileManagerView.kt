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
        toolbar()
        tabs()
        path.apply {
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setTextColor(Color.rgb(178, 205, 255))
            setSingleLine(true)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.rgb(10, 12, 17))
        }
        addView(path, LinearLayout.LayoutParams(-1, dp(38)))
        val scroll = ScrollView(context)
        scroll.addView(list, ScrollView.LayoutParams(-1, -2))
        addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        detectSd()
        refresh()
    }

    private fun toolbar() {
        val bar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            setBackgroundColor(Color.rgb(24, 28, 37))
        }
        bar.addView(button("‹") { up() }, LinearLayout.LayoutParams(dp(48), dp(42)))
        bar.addView(button("↻ Refresh") { refresh() }, LinearLayout.LayoutParams(0, dp(42), 1f))
        bar.addView(button("＋ Folder") { newFolder() }, LinearLayout.LayoutParams(dp(96), dp(42)))
        bar.addView(button("Home") { home() }, LinearLayout.LayoutParams(dp(70), dp(42)))
        addView(bar, LinearLayout.LayoutParams(-1, dp(56)))
    }

    private fun tabs() {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor(Color.rgb(18, 21, 28))
        }
        arrayOf("Internal", "SD Card", "Linux", "ROOT").forEachIndexed { i, label ->
            val b = Button(context).apply {
                text = label
                isAllCaps = false
                textSize = 11f
                setOnClickListener {
                    tab = Tab.values()[i]
                    refresh()
                }
            }
            tabs.add(b)
            row.addView(b, LinearLayout.LayoutParams(0, dp(46), 1f))
        }
        addView(row, LinearLayout.LayoutParams(-1, dp(46)))
    }

    private fun detectSd() {
        Thread {
            val out = runRoot("find /storage -mindepth 1 -maxdepth 1 -type d 2>/dev/null")
            sdPath = out.lines().firstOrNull {
                it.startsWith("/storage/") && !it.contains("/storage/emulated") && !it.contains("/storage/self")
            }
            sdCurrent = sdPath
            (context as? Activity)?.runOnUiThread { refresh() }
        }.start()
    }

    private fun refresh() {
        tabs.forEachIndexed { i, b ->
            b.isEnabled = i != 1 || sdPath != null
            b.setTextColor(if (Tab.values()[i] == tab) Color.WHITE else Color.rgb(145, 152, 166))
        }
        path.text = when (tab) {
            Tab.INTERNAL -> internalPath
            Tab.SD -> sdCurrent ?: sdPath ?: "/storage (no SD card detected)"
            Tab.LINUX -> "LINUX:$linuxPath"
            Tab.ROOT -> "ROOT:$rootPath"
        }
        list.removeAllViews()
        when (tab) {
            Tab.INTERNAL -> load(internalPath, false)
            Tab.SD -> sdCurrent?.let { load(it, false) } ?: show("No removable SD card detected.")
            Tab.LINUX -> load(linuxPath, true)
            Tab.ROOT -> load(rootPath, false)
        }
    }

    private fun load(target: String, linux: Boolean) {
        show("Loading…")
        Thread {
            val command = "find ${q(target)} -mindepth 1 -maxdepth 1 -type d -printf 'd\\t%p\\t0\\n' 2>/dev/null; find ${q(target)} -mindepth 1 -maxdepth 1 -type f -printf 'f\\t%p\\t%s\\n' 2>/dev/null"
            val cmd = if (linux) {
                "chroot /data/local/linux/rootfs /bin/sh -c ${q(command)}"
            } else {
                "sh -c ${q(command)}"
            }
            val out = runRoot(cmd)
            (context as? Activity)?.runOnUiThread {
                list.removeAllViews()
                if (out.startsWith("ERROR")) show(out.trim()) else render(out)
            }
        }.start()
    }

    private fun render(raw: String) {
        val rows = raw.lines().mapNotNull {
            val p = it.split('\t', limit = 3)
            if (p.size == 3) Triple(p[0], p[1], p[2]) else null
        }.sortedWith(
            compareByDescending<Triple<String, String, String>> { it.first == "d" }
                .thenBy { it.second.substringAfterLast('/').lowercase(Locale.ROOT) }
        )
        if (rows.isEmpty()) {
            show("This folder is empty.")
            return
        }
        rows.forEach { r ->
            val name = r.second.trimEnd('/').substringAfterLast('/')
            row(if (r.first == "d") "▰" else "□", name, if (r.first == "d") "Folder" else size(r.third)) {
                if (r.first == "d") enter(r.second) else open(r.second)
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

    private fun row(icon: String, name: String, detail: String, action: () -> Unit) {
        val r = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(5), dp(14), dp(5))
            setOnClickListener { action() }
        }
        r.addView(TextView(context).apply {
            text = icon
            textSize = 22f
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(44), dp(58)))
        val t = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        t.addView(TextView(context).apply {
            text = name
            textSize = 15f
            setTextColor(Color.WHITE)
            maxLines = 1
        }, LinearLayout.LayoutParams(-1, -2))
        t.addView(TextView(context).apply {
            text = detail
            textSize = 11f
            setTextColor(Color.rgb(145, 152, 166))
            maxLines = 1
        }, LinearLayout.LayoutParams(-1, -2))
        r.addView(t, LinearLayout.LayoutParams(0, dp(58), 1f))
        list.addView(r, LinearLayout.LayoutParams(-1, dp(68)))
    }

    private fun show(message: String) {
        list.addView(TextView(context).apply {
            text = message
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(dp(18), dp(22), dp(18), dp(22))
        }, LinearLayout.LayoutParams(-1, -2))
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
            Tab.SD -> {
                val b = sdPath
                val c = sdCurrent
                if (b != null && c != null && c != b) sdCurrent = parent(c, b)
            }
            Tab.LINUX -> if (linuxPath != "/") linuxPath = parent(linuxPath, "/")
            Tab.ROOT -> if (rootPath != "/") rootPath = parent(rootPath, "/")
        }
        refresh()
    }

    private fun parent(v: String, root: String): String {
        val clean = v.trimEnd('/')
        val slash = clean.lastIndexOf('/')
        val p = if (slash <= 0) "/" else clean.substring(0, slash)
        return if (p.length < root.length) root else p
    }

    private fun newFolder() {
        val input = EditText(context).apply { hint = "Folder name" }
        AlertDialog.Builder(context)
            .setTitle("New folder")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create") { _, _ ->
                val n = input.text.toString().trim()
                if (n.isBlank() || n.contains('/') || n == "." || n == "..") {
                    Toast.makeText(context, "Invalid folder name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val base = when (tab) {
                    Tab.INTERNAL -> internalPath
                    Tab.SD -> sdCurrent ?: return@setPositiveButton
                    Tab.LINUX -> linuxPath
                    Tab.ROOT -> rootPath
                }
                val target = "$base/$n"
                Thread {
                    runRoot(if (tab == Tab.LINUX) {
                        "chroot /data/local/linux/rootfs /bin/mkdir -p ${q(target)}"
                    } else {
                        "mkdir -p ${q(target)}"
                    })
                    (context as? Activity)?.runOnUiThread { refresh() }
                }.start()
            }
            .show()
    }

    private fun open(full: String) {
        try {
            val f = File(full)
            if (!f.exists()) {
                Toast.makeText(context, "File is not directly readable by Android", Toast.LENGTH_SHORT).show()
                return
            }
            context.startActivity(
                Intent(context, com.metmc.os.office.MetmcOfficeActivity::class.java)
                    .putExtra("path", full)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun button(text: String, action: () -> Unit) = Button(context).apply {
        this.text = text
        isAllCaps = false
        textSize = 12f
        setTextColor(Color.WHITE)
        setOnClickListener { action() }
    }

    private fun runRoot(command: String): String = try {
        val p = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
        val s = p.inputStream.bufferedReader().readText()
        val c = p.waitFor()
        if (c == 0) s else "ERROR: $s"
    } catch (e: Exception) {
        "ERROR: ${e.message}"
    }

    private fun q(v: String) = "'" + v.replace("'", "'\\''") + "'"

    private fun size(v: String): String {
        val n = v.toLongOrNull() ?: 0L
        return when {
            n >= 1073741824L -> String.format(Locale.US, "%.1f GB", n / 1073741824.0)
            n >= 1048576L -> String.format(Locale.US, "%.1f MB", n / 1048576.0)
            n >= 1024L -> String.format(Locale.US, "%.1f KB", n / 1024.0)
            else -> "$n B"
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
