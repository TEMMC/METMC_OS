package com.metmc.os.desktop

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileManagerView(private val context: Context) : LinearLayout(context) {

    private enum class Tab { ANDROID, LINUX }

    private var activeTab = Tab.ANDROID

    private var androidPath: File =
        android.os.Environment.getExternalStorageDirectory()

    private var linuxPath: String = "/root"

    private val pathLabel = TextView(context)
    private val listArea = LinearLayout(context)
    private val scroll = ScrollView(context)

    private val tabAndroid = Button(context)
    private val tabLinux = Button(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(18, 20, 26))
        buildToolbar()
        buildTabs()
        buildPathBar()
        buildListArea()
        refresh()
    }

    private fun buildToolbar() {
        val bar = LinearLayout(context)
        bar.orientation = HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(10), dp(8), dp(10), dp(8))
        bar.setBackgroundColor(Color.rgb(28, 30, 38))

        val up = flatButton("\u2191 Up")
        up.setOnClickListener { navigateUp() }

        val refresh = flatButton("\u21bb Refresh")
        refresh.setOnClickListener { refresh() }

        val newFolder = flatButton("+ Folder")
        newFolder.setOnClickListener { promptNewFolder() }

        bar.addView(up, LinearLayout.LayoutParams(0, dp(44), 1f))
        bar.addView(refresh, LinearLayout.LayoutParams(0, dp(44), 1f))
        bar.addView(newFolder, LinearLayout.LayoutParams(0, dp(44), 1f))

        addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)))
    }

    private fun buildTabs() {
        val tabs = LinearLayout(context)
        tabs.orientation = HORIZONTAL
        tabs.setBackgroundColor(Color.rgb(22, 24, 30))

        styleTab(tabAndroid, "Android Storage", true)
        styleTab(tabLinux, "Linux Filesystem", false)

        tabAndroid.setOnClickListener {
            activeTab = Tab.ANDROID
            styleTab(tabAndroid, "Android Storage", true)
            styleTab(tabLinux, "Linux Filesystem", false)
            refresh()
        }

        tabLinux.setOnClickListener {
            activeTab = Tab.LINUX
            styleTab(tabAndroid, "Android Storage", false)
            styleTab(tabLinux, "Linux Filesystem", true)
            refresh()
        }

        tabs.addView(tabAndroid, LinearLayout.LayoutParams(0, dp(48), 1f))
        tabs.addView(tabLinux, LinearLayout.LayoutParams(0, dp(48), 1f))

        addView(tabs, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))
    }

    private fun styleTab(button: Button, text: String, selected: Boolean) {
        button.text = text
        button.isAllCaps = false
        button.textSize = 13f
        button.setTextColor(if (selected) Color.WHITE else Color.rgb(140, 145, 155))
        button.setBackgroundColor(
            if (selected) Color.rgb(40, 44, 54) else Color.rgb(22, 24, 30)
        )
        button.setPadding(0, 0, 0, 0)
    }

    private fun buildPathBar() {
        pathLabel.setTextColor(Color.rgb(150, 200, 255))
        pathLabel.textSize = 12f
        pathLabel.typeface = Typeface.MONOSPACE
        pathLabel.setPadding(dp(12), dp(8), dp(12), dp(8))
        pathLabel.setBackgroundColor(Color.rgb(14, 16, 20))
        pathLabel.setSingleLine(true)
        pathLabel.ellipsize = android.text.TextUtils.TruncateAt.START

        addView(pathLabel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(36)))
    }

    private fun buildListArea() {
        listArea.orientation = VERTICAL
        scroll.addView(
            listArea,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun flatButton(text: String): Button {
        val b = Button(context)
        b.text = text
        b.isAllCaps = false
        b.textSize = 12f
        b.setTextColor(Color.WHITE)
        b.background = GradientDrawable().apply {
            setColor(Color.rgb(40, 44, 54))
            cornerRadius = dp(6).toFloat()
        }
        val params = LinearLayout.LayoutParams(0, dp(40), 1f)
        params.setMargins(dp(4), 0, dp(4), 0)
        b.layoutParams = params
        return b
    }

    private fun navigateUp() {
        when (activeTab) {
            Tab.ANDROID -> {
                val parent = androidPath.parentFile
                if (parent != null && parent.canRead()) {
                    androidPath = parent
                    refresh()
                }
            }
            Tab.LINUX -> {
                if (linuxPath != "/") {
                    val idx = linuxPath.trimEnd('/').lastIndexOf('/')
                    linuxPath = if (idx <= 0) "/" else linuxPath.substring(0, idx)
                    refresh()
                }
            }
        }
    }

    private fun refresh() {
        pathLabel.text = when (activeTab) {
            Tab.ANDROID -> androidPath.absolutePath
            Tab.LINUX -> linuxPath
        }
        listArea.removeAllViews()

        when (activeTab) {
            Tab.ANDROID -> loadAndroidEntries()
            Tab.LINUX -> loadLinuxEntries()
        }
    }

    private fun loadAndroidEntries() {
        val entries = androidPath.listFiles()

        if (entries == null) {
            addMessageRow("Cannot read this folder (permission denied).")
            return
        }

        val sorted = entries.sortedWith(
            compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() }
        )

        if (sorted.isEmpty()) {
            addMessageRow("This folder is empty.")
            return
        }

        val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

        sorted.forEach { file ->
            val subtitle =
                if (file.isDirectory) "Folder"
                else "${formatSize(file.length())} \u2022 ${dateFormat.format(Date(file.lastModified()))}"

            addFileRow(
                icon = if (file.isDirectory) "\ud83d\udcc1" else "\ud83d\udcc4",
                name = file.name,
                subtitle = subtitle,
                onClick = {
                    if (file.isDirectory) {
                        androidPath = file
                        refresh()
                    } else {
                        Toast.makeText(context, file.name, Toast.LENGTH_SHORT).show()
                    }
                },
                onLongClick = { confirmDeleteAndroid(file) }
            )
        }
    }

    private fun confirmDeleteAndroid(file: File) {
        AlertDialog.Builder(context)
            .setTitle("Delete")
            .setMessage("Delete \"${file.name}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val ok = if (file.isDirectory) file.deleteRecursively() else file.delete()
                if (!ok) Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadLinuxEntries() {
        addMessageRow("Loading...")

        val pathAtRequestTime = linuxPath

        Thread {
            val rootfs = "/data/local/linux/rootfs"
            val cmd =
                "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                "ls -la --time-style=+%s " + shellQuote(pathAtRequestTime) + " 2>&1"

            val output = try {
                val process = ProcessBuilder(
                    "su", "-c",
                    "chroot " + shellQuote(rootfs) + " /bin/bash -c " + shellQuote(cmd)
                ).redirectErrorStream(true).start()

                val text = process.inputStream.bufferedReader().readText()
                process.waitFor()
                text
            } catch (e: Exception) {
                "ERROR: $e"
            }

            (context as? Activity)?.runOnUiThread {
                if (pathAtRequestTime == linuxPath) {
                    renderLinuxListing(output)
                }
            }
        }.start()
    }

    private fun renderLinuxListing(raw: String) {
        listArea.removeAllViews()

        if (raw.startsWith("ERROR") || raw.contains("No such file or directory")) {
            addMessageRow("Cannot read this path:\n$raw")
            return
        }

        val lines = raw.trim().lines().drop(1)

        if (lines.isEmpty() || (lines.size == 1 && lines[0].isBlank())) {
            addMessageRow("This folder is empty.")
            return
        }

        var shown = 0

        for (line in lines) {
            val parts = line.trim().split(Regex("\\s+"), limit = 7)
            if (parts.size < 7) continue

            val perms = parts[0]
            val sizeBytes = parts[4].toLongOrNull() ?: 0L
            val epoch = parts[5].toLongOrNull()
            val name = parts[6]

            if (name == "." || name == "..") continue

            val isDir = perms.startsWith("d")
            val isLink = perms.startsWith("l")

            val cleanName = if (isLink && name.contains(" -> ")) name.substringBefore(" -> ") else name

            val subtitle = buildString {
                append(if (isDir) "Directory" else formatSize(sizeBytes))
                if (epoch != null) {
                    append(" \u2022 ")
                    append(SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(epoch * 1000)))
                }
            }

            addFileRow(
                icon = if (isDir) "\ud83d\udcc1" else if (isLink) "\ud83d\udd17" else "\ud83d\udcc4",
                name = cleanName,
                subtitle = subtitle,
                onClick = {
                    if (isDir) {
                        linuxPath =
                            if (linuxPath == "/") "/$cleanName" else "$linuxPath/$cleanName"
                        refresh()
                    } else {
                        Toast.makeText(context, cleanName, Toast.LENGTH_SHORT).show()
                    }
                },
                onLongClick = { confirmDeleteLinux(cleanName, isDir) }
            )
            shown++
        }

        if (shown == 0) {
            addMessageRow("This folder is empty.")
        }
    }

    private fun confirmDeleteLinux(name: String, isDir: Boolean) {
        AlertDialog.Builder(context)
            .setTitle("Delete")
            .setMessage("Delete \"$name\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val target = if (linuxPath == "/") "/$name" else "$linuxPath/$name"
                val flag = if (isDir) "-rf" else "-f"
                runLinuxCommand("rm $flag " + shellQuote(target)) { refresh() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptNewFolder() {
        val input = EditText(context)
        input.hint = "Folder name"
        input.setTextColor(Color.WHITE)
        input.setHintTextColor(Color.LTGRAY)

        AlertDialog.Builder(context)
            .setTitle("New Folder")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton

                when (activeTab) {
                    Tab.ANDROID -> {
                        val ok = File(androidPath, name).mkdir()
                        if (!ok) Toast.makeText(context, "Create failed", Toast.LENGTH_SHORT).show()
                        refresh()
                    }
                    Tab.LINUX -> {
                        val target = if (linuxPath == "/") "/$name" else "$linuxPath/$name"
                        runLinuxCommand("mkdir -p " + shellQuote(target)) { refresh() }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun runLinuxCommand(command: String, onDone: () -> Unit) {
        Thread {
            try {
                val rootfs = "/data/local/linux/rootfs"
                val full =
                    "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " + command

                val process = ProcessBuilder(
                    "su", "-c",
                    "chroot " + shellQuote(rootfs) + " /bin/bash -c " + shellQuote(full)
                ).redirectErrorStream(true).start()

                process.inputStream.bufferedReader().readText()
                process.waitFor()
            } catch (_: Exception) {
            }

            (context as? Activity)?.runOnUiThread { onDone() }
        }.start()
    }

    private fun addFileRow(
        icon: String,
        name: String,
        subtitle: String,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        val row = LinearLayout(context)
        row.orientation = HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(14), dp(10), dp(14), dp(10))
        row.isClickable = true
        row.isFocusable = true

        val iconView = TextView(context)
        iconView.text = icon
        iconView.textSize = 20f

        row.addView(iconView, LinearLayout.LayoutParams(dp(40), dp(40)))

        val textBox = LinearLayout(context)
        textBox.orientation = VERTICAL

        val nameView = TextView(context)
        nameView.text = name
        nameView.setTextColor(Color.WHITE)
        nameView.textSize = 15f
        nameView.setSingleLine(true)
        nameView.ellipsize = android.text.TextUtils.TruncateAt.MIDDLE

        val subtitleView = TextView(context)
        subtitleView.text = subtitle
        subtitleView.setTextColor(Color.rgb(140, 145, 155))
        subtitleView.textSize = 11f

        textBox.addView(nameView)
        textBox.addView(subtitleView)

        row.addView(
            textBox,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(10)
            }
        )

        row.setOnClickListener { onClick() }
        row.setOnLongClickListener { onLongClick(); true }

        listArea.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)))

        val divider = View(context)
        divider.setBackgroundColor(Color.rgb(30, 32, 40))
        listArea.addView(divider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))
    }

    private fun addMessageRow(text: String) {
        val message = TextView(context)
        message.text = text
        message.setTextColor(Color.rgb(140, 145, 155))
        message.textSize = 14f
        message.setPadding(dp(20), dp(30), dp(20), dp(30))
        message.gravity = Gravity.CENTER
        listArea.addView(message)
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.1f GB".format(gb)
    }

    private fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
