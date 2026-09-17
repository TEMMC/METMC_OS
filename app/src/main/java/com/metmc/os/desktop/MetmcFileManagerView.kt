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
import androidx.core.content.FileProvider
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
    private var showHidden = true
    private var sortFoldersFirst = true
    private val path = TextView(context)
    private val list = LinearLayout(context)
    private val tabs = ArrayList<Button>()
    private val count = TextView(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(11, 14, 19))
        toolbar()
        tabs()
        path.apply {
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setTextColor(Color.rgb(178, 205, 255))
            setSingleLine(true)
            setPadding(dp(12), dp(7), dp(12), 0)
            setBackgroundColor(Color.rgb(9, 11, 16))
        }
        addView(path, LinearLayout.LayoutParams(-1, dp(30)))
        count.apply {
            textSize = 10f
            setTextColor(Color.rgb(120, 130, 145))
            setPadding(dp(12), 0, dp(12), dp(5))
        }
        addView(count, LinearLayout.LayoutParams(-1, dp(24)))
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            addView(list, FrameLayout.LayoutParams(-1, -2))
        }
        addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        detectSd()
        refresh()
    }

    private fun toolbar() {
        val bar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(5), dp(6), dp(5))
            setBackgroundColor(Color.rgb(24, 28, 37))
        }
        bar.addView(button("‹") { up() }, LinearLayout.LayoutParams(dp(42), dp(42)))
        bar.addView(button("↻") { refresh() }, LinearLayout.LayoutParams(dp(44), dp(42)))
        bar.addView(button("＋ Folder") { newFolder() }, LinearLayout.LayoutParams(dp(88), dp(42)))
        bar.addView(button("Search") { searchDialog() }, LinearLayout.LayoutParams(0, dp(42), 1f))
        bar.addView(button("⋮") { menu() }, LinearLayout.LayoutParams(dp(44), dp(42)))
        addView(bar, LinearLayout.LayoutParams(-1, dp(52)))
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
                setPadding(0, 0, 0, 0)
                setOnClickListener {
                    tab = Tab.values()[i]
                    refresh()
                }
            }
            tabs.add(b)
            row.addView(b, LinearLayout.LayoutParams(0, dp(44), 1f))
        }
        addView(row, LinearLayout.LayoutParams(-1, dp(44)))
    }

    private fun detectSd() {
        Thread {
            val out = runRoot("find /storage -mindepth 1 -maxdepth 1 -type d -print 2>/dev/null")
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
            Tab.INTERNAL -> "INTERNAL:$internalPath"
            Tab.SD -> "SD:${sdCurrent ?: sdPath ?: "/storage"}"
            Tab.LINUX -> "LINUX:$linuxPath"
            Tab.ROOT -> "ROOT:$rootPath"
        }
        list.removeAllViews()
        count.text = "Loading filesystem…"
        when (tab) {
            Tab.INTERNAL -> load(internalPath, false)
            Tab.SD -> sdCurrent?.let { load(it, false) } ?: show("No removable SD card detected.")
            Tab.LINUX -> load(linuxPath, true)
            Tab.ROOT -> load(rootPath, false)
        }
    }

    private fun load(target: String, linux: Boolean) {
        Thread {
            val safeTarget = q(target)
            val scan = "find $safeTarget -mindepth 1 -maxdepth 1 -print 2>/dev/null | while IFS= read -r p; do if [ -d \"$p\" ]; then printf 'd\\t%s\\t0\\n' \"$p\"; elif [ -f \"$p\" ]; then s=\$(stat -c %s \"$p\" 2>/dev/null || echo 0); printf 'f\\t%s\\t%s\\n' \"$p\" \"\$s\"; else printf 'o\\t%s\\t0\\n' \"$p\"; fi; done"
            val cmd = if (linux) {
                "chroot /data/local/linux/rootfs /bin/sh -c ${q(scan)}"
            } else scan
            val out = runRoot(cmd)
            (context as? Activity)?.runOnUiThread {
                list.removeAllViews()
                if (out.startsWith("ERROR")) {
                    count.text = "Filesystem access error"
                    show(out.trim())
                } else render(out)
            }
        }.start()
    }

    private fun render(raw: String) {
        var rows = raw.lines().mapNotNull {
            val p = it.split('\t', limit = 3)
            if (p.size == 3) Triple(p[0], p[1], p[2]) else null
        }
        if (!showHidden) rows = rows.filter { nameOf(it.second) !in listOf(".", "..") && !nameOf(it.second).startsWith(".") }
        rows = if (sortFoldersFirst) rows.sortedWith(compareByDescending<Triple<String, String, String>> { it.first == "d" }.thenBy { nameOf(it.second).lowercase(Locale.ROOT) })
        else rows.sortedBy { nameOf(it.second).lowercase(Locale.ROOT) }
        count.text = "${rows.size} items  •  ${if (showHidden) "hidden shown" else "hidden hidden"}"
        if (rows.isEmpty()) {
            show("This folder is empty.")
            return
        }
        rows.forEach { r ->
            val name = nameOf(r.second)
            val detail = when (r.first) {
                "d" -> "Folder"
                "f" -> size(r.third)
                else -> "Special file"
            }
            row(if (r.first == "d") "▰" else if (r.first == "f") "□" else "◇", name, detail) {
                if (r.first == "d") enter(r.second) else open(r.second)
            }
            list.getChildAt(list.childCount - 1).setOnLongClickListener { itemMenu(r.first, r.second); true }
        }
    }

    private fun itemMenu(type: String, full: String) {
        val actions = if (type == "d") arrayOf("Open", "Rename", "Delete") else arrayOf("Open", "Share", "Rename", "Delete")
        AlertDialog.Builder(context).setTitle(nameOf(full)).setItems(actions) { _, which ->
            when (actions[which]) {
                "Open" -> if (type == "d") enter(full) else open(full)
                "Share" -> share(full)
                "Rename" -> rename(full)
                "Delete" -> delete(full)
            }
        }.show()
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
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { action() }
        }
        r.addView(TextView(context).apply {
            text = icon
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(if (icon == "▰") Color.rgb(90, 160, 255) else Color.rgb(190, 198, 210))
        }, LinearLayout.LayoutParams(dp(44), dp(58)))
        val t = LinearLayout(context).apply { orientation = VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        t.addView(TextView(context).apply {
            text = name
            textSize = 15f
            setTextColor(Color.WHITE)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
        }, LinearLayout.LayoutParams(-1, -2))
        t.addView(TextView(context).apply {
            text = detail
            textSize = 11f
            setTextColor(Color.rgb(135, 145, 160))
            maxLines = 1
        }, LinearLayout.LayoutParams(-1, -2))
        r.addView(t, LinearLayout.LayoutParams(0, dp(58), 1f))
        r.addView(TextView(context).apply { text = "⋮"; textSize = 20f; setTextColor(Color.GRAY); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(30), dp(58)))
        list.addView(r, LinearLayout.LayoutParams(-1, dp(66)))
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
            Tab.SD -> { val b = sdPath; val c = sdCurrent; if (b != null && c != null && c != b) sdCurrent = parent(c, b) }
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
        AlertDialog.Builder(context).setTitle("New folder").setView(input).setNegativeButton("Cancel", null).setPositiveButton("Create") { _, _ ->
            val n = input.text.toString().trim()
            if (n.isBlank() || n.contains('/') || n == "." || n == "..") { Toast.makeText(context, "Invalid folder name", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            val base = currentPath() ?: return@setPositiveButton
            Thread {
                val target = "$base/$n"
                val command = if (tab == Tab.LINUX) {
                    "chroot /data/local/linux/rootfs /bin/mkdir -p ${q(target)}"
                } else {
                    "mkdir -p ${q(target)}"
                }
                runRoot(command)
                (context as? Activity)?.runOnUiThread { refresh() }
            }.start()
        }.show()
    }

    private fun rename(full: String) {
        val input = EditText(context).apply { setText(nameOf(full)); selectAll() }
        AlertDialog.Builder(context).setTitle("Rename").setView(input).setNegativeButton("Cancel", null).setPositiveButton("Rename") { _, _ ->
            val n = input.text.toString().trim()
            if (n.isBlank() || n.contains('/')) return@setPositiveButton
            val target = full.substringBeforeLast('/') + "/" + n
            Thread { runRoot("mv ${q(full)} ${q(target)}"); (context as? Activity)?.runOnUiThread { refresh() } }.start()
        }.show()
    }

    private fun delete(full: String) {
        AlertDialog.Builder(context).setTitle("Delete permanently?").setMessage(full).setNegativeButton("Cancel", null).setPositiveButton("Delete") { _, _ ->
            Thread { runRoot("rm -rf -- ${q(full)}"); (context as? Activity)?.runOnUiThread { refresh() } }.start()
        }.show()
    }

    private fun share(full: String) {
        try {
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", File(full))
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("*/*").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Share file").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) { Toast.makeText(context, "Unable to share: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun open(full: String) {
        try {
            context.startActivity(Intent(context, com.metmc.os.office.MetmcOfficeActivity::class.java).putExtra("path", full).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) { Toast.makeText(context, "Unable to open file: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun searchDialog() {
        val input = EditText(context).apply { hint = "Search this folder…" }
        AlertDialog.Builder(context).setTitle("Search").setView(input).setNegativeButton("Cancel", null).setPositiveButton("Find") { _, _ -> search(input.text.toString().trim()) }.show()
    }

    private fun search(term: String) {
        if (term.isBlank()) return
        val base = currentPath() ?: return
        list.removeAllViews(); count.text = "Searching…"
        Thread {
            val linux = tab == Tab.LINUX
            val command = "find ${q(base)} -iname ${q("*$term*")} -print 2>/dev/null | head -n 500"
            val cmd = if (linux) "chroot /data/local/linux/rootfs /bin/sh -c ${q(command)}" else command
            val out = runRoot(cmd)
            (context as? Activity)?.runOnUiThread {
                list.removeAllViews()
                val paths = out.lines().filter { it.isNotBlank() }
                count.text = "${paths.size} search results"
                if (paths.isEmpty()) show("No matches.") else paths.forEach { p -> row(if (isDir(p)) "▰" else "□", nameOf(p), p) { if (isDir(p)) enter(p) else open(p) } }
            }
        }.start()
    }

    private fun menu() {
        AlertDialog.Builder(context).setTitle("File manager").setItems(arrayOf("Home", "Show hidden files: ${if (showHidden) "ON" else "OFF"}", "Folders first: ${if (sortFoldersFirst) "ON" else "OFF"}", "Refresh")) { _, which ->
            when (which) { 0 -> home(); 1 -> { showHidden = !showHidden; refresh() }; 2 -> { sortFoldersFirst = !sortFoldersFirst; refresh() }; 3 -> refresh() }
        }.show()
    }

    private fun currentPath() = when (tab) { Tab.INTERNAL -> internalPath; Tab.SD -> sdCurrent; Tab.LINUX -> linuxPath; Tab.ROOT -> rootPath }
    private fun isDir(p: String) = runRoot("[ -d ${q(p)} ]").trim().isEmpty()
    private fun nameOf(p: String) = p.trimEnd('/').substringAfterLast('/')
    private fun button(text: String, action: () -> Unit) = Button(context).apply { this.text = text; isAllCaps = false; textSize = 11f; setTextColor(Color.WHITE); setOnClickListener { action() } }
    private fun runRoot(command: String): String = try { val p = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start(); val s = p.inputStream.bufferedReader().readText(); val c = p.waitFor(); if (c == 0) s else "ERROR: $s" } catch (e: Exception) { "ERROR: ${e.message}" }
    private fun q(v: String) = "'" + v.replace("'", "'\\''") + "'"
    private fun size(v: String): String { val n = v.toLongOrNull() ?: 0L; return when { n >= 1073741824L -> String.format(Locale.US, "%.1f GB", n / 1073741824.0); n >= 1048576L -> String.format(Locale.US, "%.1f MB", n / 1048576.0); n >= 1024L -> String.format(Locale.US, "%.1f KB", n / 1024.0); else -> "$n B" } }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
