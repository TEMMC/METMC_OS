package com.metmc.os.desktop

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.metmc.os.linux.LinuxDesktopActivity
import java.io.InputStream

class MetmcDesktop(context: Context) : FrameLayout(context) {

    private val activity = context as? Activity

    private val desktop = FrameLayout(context)
    private val taskbar = LinearLayout(context)
    private val windows = ArrayList<View>()

    private var launcherOpen = false
    private var wallpaperUri: Uri? = null

    private val bg = Color.rgb(9, 11, 15)
    private val panel = Color.rgb(20, 23, 29)
    private val panel2 = Color.rgb(29, 32, 40)
    private val border = Color.rgb(55, 60, 72)
    private val accent = Color.rgb(95, 220, 135)
    private val text = Color.WHITE
    private val muted = Color.rgb(155, 162, 175)

    init {
        build()
    }

    private fun build() {
        setBackgroundColor(bg)

        desktop.setBackgroundColor(bg)

        addView(
            desktop,
            LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT
            )
        )

        buildDesktop()
        buildTaskbar()
    }

    private fun buildDesktop() {
        val content = LinearLayout(context)
        content.orientation = LinearLayout.VERTICAL
        content.gravity = Gravity.CENTER
        content.setPadding(dp(24), dp(20), dp(24), dp(100))

        val logo = TextView(context)
        logo.text = "◈"
        logo.textSize = 52f
        logo.setTextColor(accent)
        logo.gravity = Gravity.CENTER

        val title = TextView(context)
        title.text = "METMC OS"
        title.textSize = 34f
        title.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        title.setTextColor(text)
        title.gravity = Gravity.CENTER

        val subtitle = TextView(context)
        subtitle.text = "ANDROID + LINUX DESKTOP"
        subtitle.textSize = 13f
        subtitle.letterSpacing = 0.18f
        subtitle.setTextColor(muted)
        subtitle.gravity = Gravity.CENTER

        val status = TextView(context)
        status.text = "●  SYSTEM READY"
        status.textSize = 13f
        status.typeface = Typeface.DEFAULT_BOLD
        status.setTextColor(accent)
        status.gravity = Gravity.CENTER

        content.addView(logo, LinearLayout.LayoutParams(-1, dp(70)))
        content.addView(title, LinearLayout.LayoutParams(-1, dp(55)))
        content.addView(subtitle, LinearLayout.LayoutParams(-1, dp(35)))
        content.addView(status, LinearLayout.LayoutParams(-1, dp(40)))

        desktop.addView(
            content,
            LayoutParams(-1, -1)
        )

        addDesktopIcon("▣", "Applications") {
            showLauncher()
        }

        addDesktopIcon("🐧", "Linux") {
            showLinux()
        }

        addDesktopIcon("▤", "Files") {
            showFiles()
        }
    }

    private fun addDesktopIcon(icon: String, name: String, action: () -> Unit) {
        val button = TextView(context)
        button.text = "$icon\n$name"
        button.textSize = 13f
        button.setTextColor(text)
        button.gravity = Gravity.CENTER
        button.setPadding(dp(8), dp(8), dp(8), dp(8))
        button.setOnClickListener { action() }

        val p = LayoutParams(dp(95), dp(82))
        p.leftMargin = dp(18)
        p.topMargin = dp(18 + windows.size * 5)

        desktop.addView(button, p)
    }

    private fun buildTaskbar() {
        taskbar.orientation = LinearLayout.HORIZONTAL
        taskbar.gravity = Gravity.CENTER_VERTICAL
        taskbar.setPadding(dp(8), dp(6), dp(8), dp(6))
        taskbar.background = rounded(panel, dp(16))

        val p = LayoutParams(-1, dp(62))
        p.gravity = Gravity.BOTTOM
        p.setMargins(dp(10), 0, dp(10), dp(10))

        addView(taskbar, p)

        taskbarButton("◈", "Start") {
            showLauncher()
        }

        taskbarButton("▣", "Files") {
            showFiles()
        }

        taskbarButton("🐧", "Linux") {
            showLinux()
        }

        taskbarButton("⌘", "Terminal") {
            showTerminal()
        }

        taskbarButton("⚙", "Settings") {
            showSettings()
        }

        val spacer = Space(context)
        taskbar.addView(
            spacer,
            LinearLayout.LayoutParams(0, 1, 1f)
        )

        val clock = TextView(context)
        clock.text = "METMC OS"
        clock.textSize = 12f
        clock.setTextColor(muted)
        clock.gravity = Gravity.CENTER

        taskbar.addView(
            clock,
            LinearLayout.LayoutParams(dp(90), dp(48))
        )
    }

    private fun taskbarButton(
        icon: String,
        description: String,
        action: () -> Unit
    ) {
        val b = Button(context)
        b.text = icon
        b.textSize = 19f
        b.setTextColor(text)
        b.setAllCaps(false)
        b.contentDescription = description
        b.setOnClickListener { action() }

        taskbar.addView(
            b,
            LinearLayout.LayoutParams(dp(55), dp(48))
        )
    }

    private fun showLauncher() {
        if (launcherOpen) return
        launcherOpen = true

        val box = LinearLayout(context)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(18), dp(18), dp(18), dp(18))
        box.background = rounded(panel, dp(18))

        val header = LinearLayout(context)
        header.gravity = Gravity.CENTER_VERTICAL

        val title = TextView(context)
        title.text = "METMC OS"
        title.textSize = 22f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(text)

        header.addView(
            title,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        val close = button("×")
        close.setOnClickListener {
            box.parent?.let {
                (it as ViewGroup).removeView(box)
            }
            launcherOpen = false
        }

        header.addView(
            close,
            LinearLayout.LayoutParams(dp(50), dp(48))
        )

        box.addView(header)

        val search = EditText(context)
        search.hint = "Search applications"
        search.setHintTextColor(muted)
        search.setTextColor(text)
        search.textSize = 15f
        search.singleLine = true
        search.background = rounded(panel2, dp(10))
        search.setPadding(dp(14), 0, dp(14), 0)

        box.addView(
            search,
            LinearLayout.LayoutParams(-1, dp(50))
        )

        addLauncherItem(box, "🐧", "Linux Applications") {
            showLinux()
        }

        addLauncherItem(box, "⌘", "Debian Terminal") {
            showTerminal()
        }

        addLauncherItem(box, "▤", "Files") {
            showFiles()
        }

        addLauncherItem(box, "⚙", "Settings") {
            showSettings()
        }

        addLauncherItem(box, "📱", "Android Applications") {
            showAndroidApps()
        }

        createWindow(
            "METMC Applications",
            box,
            dp(420),
            dp(500)
        )
    }

    private fun addLauncherItem(
        parent: LinearLayout,
        icon: String,
        name: String,
        action: () -> Unit
    ) {
        val b = Button(context)
        b.text = "$icon   $name"
        b.textSize = 15f
        b.setTextColor(text)
        b.setAllCaps(false)
        b.gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
        b.setOnClickListener { action() }

        parent.addView(
            b,
            LinearLayout.LayoutParams(-1, dp(58))
        )
    }

    private fun showLinux() {
        val box = LinearLayout(context)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(18), dp(18), dp(18), dp(18))

        addSectionHeader(box, "Linux Subsystem", "Debian ARM64")

        addInfo(box, "●  Debian environment")
        addInfo(box, "●  Root integration")
        addInfo(box, "●  Linux application support")

        addLauncherItem(box, "⌘", "Terminal") {
            showTerminal()
        }

        addLauncherItem(box, "▦", "Linux Applications") {
            showLinuxApps()
        }

        addLauncherItem(box, "▣", "Linux Desktop") {
            try {
                context.startActivity(
                    Intent(context, LinuxDesktopActivity::class.java)
                )
            } catch (e: Exception) {
                toast("Linux desktop unavailable")
            }
        }

        createWindow(
            "METMC Linux",
            box,
            dp(460),
            dp(440)
        )
    }

    private fun showTerminal() {
        val box = LinearLayout(context)
        box.orientation = LinearLayout.VERTICAL
        box.background = rounded(Color.rgb(7, 8, 10), dp(12))

        val terminalBar = LinearLayout(context)
        terminalBar.gravity = Gravity.CENTER_VERTICAL
        terminalBar.setPadding(dp(14), 0, dp(6), 0)
        terminalBar.background = rounded(panel2, dp(10))

        val title = TextView(context)
        title.text = "●  Debian Terminal"
        title.textSize = 14f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(accent)

        terminalBar.addView(
            title,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        val clear = button("Clear")
        terminalBar.addView(
            clear,
            LinearLayout.LayoutParams(dp(70), dp(44))
        )

        box.addView(
            terminalBar,
            LinearLayout.LayoutParams(-1, dp(50))
        )

        val scroll = ScrollView(context)

        val output = TextView(context)
        output.text =
            "METMC OS Linux Terminal\n" +
            "Debian ARM64\n" +
            "────────────────────────────\n" +
            "root@metmc:~# "

        output.setTextColor(Color.rgb(225, 230, 235))
        output.textSize = 14f
        output.typeface = Typeface.MONOSPACE
        output.setPadding(dp(14), dp(14), dp(14), dp(20))

        scroll.addView(output)

        box.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        val input = LinearLayout(context)
        input.gravity = Gravity.CENTER_VERTICAL
        input.setPadding(dp(10), dp(6), dp(10), dp(6))
        input.background = rounded(panel2, dp(10))

        val prompt = TextView(context)
        prompt.text = "root@metmc:~$ "
        prompt.textSize = 13f
        prompt.typeface = Typeface.create(
            Typeface.MONOSPACE,
            Typeface.BOLD
        )
        prompt.setTextColor(accent)

        input.addView(
            prompt,
            LinearLayout.LayoutParams(-2, dp(50))
        )

        val command = EditText(context)
        command.singleLine = true
        command.hint = "command"
        command.setHintTextColor(muted)
        command.setTextColor(text)
        command.textSize = 14f
        command.typeface = Typeface.MONOSPACE
        command.background = null

        input.addView(
            command,
            LinearLayout.LayoutParams(0, dp(50), 1f)
        )

        val run = button("▶")
        input.addView(
            run,
            LinearLayout.LayoutParams(dp(55), dp(45))
        )

        box.addView(
            input,
            LinearLayout.LayoutParams(-1, dp(62))
        )

        val execute = {
            val cmd = command.text.toString().trim()

            if (cmd.isNotEmpty()) {
                output.append(cmd + "\n")
                command.setText("")

                runLinuxCommand(cmd) { result ->
                    output.append(
                        result + "\n\nroot@metmc:~# "
                    )

                    scroll.post {
                        scroll.fullScroll(View.FOCUS_DOWN)
                    }
                }
            }
        }

        run.setOnClickListener { execute() }

        command.setOnEditorActionListener { _, _, event ->
            if (event != null &&
                event.keyCode == KeyEvent.KEYCODE_ENTER) {
                execute()
                true
            } else {
                false
            }
        }

        clear.setOnClickListener {
            output.text = "root@metmc:~# "
            command.requestFocus()
        }

        createWindow(
            "Debian Terminal",
            box,
            dp(760),
            dp(520)
        )
    }

    private fun runLinuxCommand(
        command: String,
        callback: (String) -> Unit
    ) {
        Thread {
            val result: String

            try {
                val process = Runtime.getRuntime().exec(
                    arrayOf(
                        "su",
                        "-c",
                        "export HOME=/root; " +
                        "export USER=root; " +
                        "export LANG=C.UTF-8; " +
                        "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                        "test -x /data/local/linux/rootfs/bin/bash && " +
                        "chroot /data/local/linux/rootfs " +
                        "/bin/bash -lc " +
                        shellQuote(command)
                    )
                )

                val input = process.inputStream
                result = input.bufferedReader().use {
                    it.readText()
                }

                process.waitFor()

            } catch (e: Exception) {
                result = "ERROR: ${e.message}"
            }

            activity?.runOnUiThread {
                callback(result)
            }
        }.start()
    }

    private fun showLinuxApps() {
        val box = LinearLayout(context)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(18), dp(18), dp(18), dp(18))

        addSectionHeader(
            box,
            "Linux Applications",
            "Applications installed in Debian"
        )

        addLauncherItem(box, "⌘", "Terminal") {
            showTerminal()
        }

        addLauncherItem(box, "▣", "Linux Desktop") {
            try {
                context.startActivity(
                    Intent(context, LinuxDesktopActivity::class.java)
                )
            } catch (e: Exception) {
                toast("Linux desktop unavailable")
            }
        }

        addInfo(
            box,
            "Application discovery is provided by the Debian subsystem."
        )

        createWindow(
            "Linux Applications",
            box,
            dp(500),
            dp(430)
        )
    }

    private fun showFiles() {
        val box = LinearLayout(context)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(18), dp(18), dp(18), dp(18))

        addSectionHeader(
            box,
            "Files",
            "Android storage"
        )

        addLauncherItem(box, "📁", "Open File Picker") {
            try {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.type = "*/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                activity?.startActivityForResult(intent, 100)
            } catch (e: Exception) {
                toast("File picker unavailable")
            }
        }

        addInfo(box, "Android storage access")
        addInfo(box, "Documents and media")
        addInfo(box, "Linux root filesystem")

        createWindow(
            "Files",
            box,
            dp(480),
            dp(390)
        )
    }

    private fun showAndroidApps() {
        val box = LinearLayout(context)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(18), dp(18), dp(18), dp(18))

        addSectionHeader(
            box,
            "Android Applications",
            "Installed Android apps"
        )

        try {
            val pm = context.packageManager

            val query = Intent(
                Intent.ACTION_MAIN,
                null
            )

            query.addCategory(Intent.CATEGORY_LAUNCHER)

            val apps = pm.queryIntentActivities(query, 0)

            for (info in apps) {
                val name =
                    info.loadLabel(pm).toString()

                addLauncherItem(
                    box,
                    "▣",
                    name
                ) {
                    try {
                        val intent =
                            pm.getLaunchIntentForPackage(
                                info.activityInfo.packageName
                            )

                        if (intent != null)
                            context.startActivity(intent)

                    } catch (e: Exception) {
                        toast("Unable to launch $name")
                    }
                }
            }

        } catch (e: Exception) {
            addInfo(
                box,
                "Unable to enumerate applications."
            )
        }

        createWindow(
            "Android Applications",
            box,
            dp(520),
            dp(560)
        )
    }

    private fun showSettings() {
        val box = LinearLayout(context)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(18), dp(18), dp(18), dp(18))

        addSectionHeader(
            box,
            "Settings",
            "METMC OS configuration"
        )

        addLauncherItem(box, "🖼", "Wallpaper") {
            chooseWallpaper()
        }

        addLauncherItem(box, "▣", "Display") {
            toast("Display settings")
        }

        addLauncherItem(box, "🐧", "Linux Subsystem") {
            showLinux()
        }

        addLauncherItem(box, "ℹ", "About METMC OS") {
            showAbout()
        }

        createWindow(
            "Settings",
            box,
            dp(460),
            dp(450)
        )
    }

    private fun showAbout() {
        val box = LinearLayout(context)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(24), dp(24), dp(24), dp(24))

        val logo = TextView(context)
        logo.text = "◈"
        logo.textSize = 48f
        logo.setTextColor(accent)
        logo.gravity = Gravity.CENTER

        box.addView(
            logo,
            LinearLayout.LayoutParams(-1, dp(70))
        )

        addSectionHeader(
            box,
            "METMC OS",
            "Android Desktop Environment"
        )

        addInfo(box, "Version 6")
        addInfo(box, "Native Java/Kotlin desktop")
        addInfo(box, "64-bit ARM")
        addInfo(box, "Android + Debian Linux")

        createWindow(
            "About METMC OS",
            box,
            dp(420),
            dp(400)
        )
    }

    private fun addSectionHeader(
        parent: LinearLayout,
        titleText: String,
        subtitleText: String
    ) {
        val title = TextView(context)
        title.text = titleText
        title.textSize = 22f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(text)

        parent.addView(
            title,
            LinearLayout.LayoutParams(-1, dp(38))
        )

        val subtitle = TextView(context)
        subtitle.text = subtitleText
        subtitle.textSize = 13f
        subtitle.setTextColor(muted)

        parent.addView(
            subtitle,
            LinearLayout.LayoutParams(-1, dp(35))
        )
    }

    private fun addInfo(
        parent: LinearLayout,
        message: String
    ) {
        val t = TextView(context)
        t.text = message
        t.textSize = 14f
        t.setTextColor(muted)
        t.setPadding(dp(8), dp(7), dp(8), dp(7))

        parent.addView(
            t,
            LinearLayout.LayoutParams(-1, dp(38))
        )
    }

    private fun button(label: String): Button {
        val b = Button(context)
        b.text = label
        b.textSize = 14f
        b.setTextColor(text)
        b.setAllCaps(false)
        b.setPadding(0, 0, 0, 0)
        b.background = rounded(panel2, dp(9))
        return b
    }

    private fun createWindow(
        titleText: String,
        content: View,
        width: Int,
        height: Int
    ) {
        val window = LinearLayout(context)
        window.orientation = LinearLayout.VERTICAL
        window.background = rounded(panel, dp(14))
        window.elevation = dp(12).toFloat()

        val bar = LinearLayout(context)
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(10), 0, dp(5), 0)
        bar.background = rounded(panel2, dp(14))

        val title = TextView(context)
        title.text = titleText
        title.textSize = 14f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(text)

        bar.addView(
            title,
            LinearLayout.LayoutParams(0, dp(46), 1f)
        )

        val minimize = button("—")
        val maximize = button("□")
        val close = button("×")

        bar.addView(
            minimize,
            LinearLayout.LayoutParams(dp(46), dp(42))
        )

        bar.addView(
            maximize,
            LinearLayout.LayoutParams(dp(46), dp(42))
        )

        bar.addView(
            close,
            LinearLayout.LayoutParams(dp(46), dp(42))
        )

        window.addView(
            bar,
            LinearLayout.LayoutParams(-1, dp(50))
        )

        val holder = FrameLayout(context)
        holder.setPadding(dp(3), dp(3), dp(3), dp(3))
        holder.addView(
            content,
            FrameLayout.LayoutParams(-1, -1)
        )

        window.addView(
            holder,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        val p = LayoutParams(width, height)

        p.leftMargin =
            dp(35 + ((windows.size * 22) % 180))

        p.topMargin =
            dp(30 + ((windows.size * 22) % 130))

        desktop.addView(window, p)
        windows.add(window)

        window.bringToFront()

        minimize.setOnClickListener {
            window.visibility = View.GONE
        }

        maximize.setOnClickListener {
            val lp = window.layoutParams as LayoutParams

            if (lp.width == MATCH_PARENT) {
                lp.width = width
                lp.height = height
                lp.leftMargin = dp(35)
                lp.topMargin = dp(30)
                window.layoutParams = lp
            } else {
                lp.width = MATCH_PARENT
                lp.height = desktop.height
                lp.leftMargin = 0
                lp.topMargin = 0
                window.layoutParams = lp
            }

            window.bringToFront()
        }

        close.setOnClickListener {
            desktop.removeView(window)
            windows.remove(window)
        }

        makeDraggable(window, bar)
    }

    private fun makeDraggable(
        window: View,
        bar: View
    ) {
        var downX = 0f
        var downY = 0f
        var startX = 0f
        var startY = 0f

        bar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = window.x
                    startY = window.y
                    window.bringToFront()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    window.x =
                        (startX + event.rawX - downX)
                            .coerceAtLeast(0f)

                    window.y =
                        (startY + event.rawY - downY)
                            .coerceAtLeast(0f)

                    true
                }

                else -> true
            }
        }
    }

    private fun chooseWallpaper() {
        if (activity == null)
            return

        try {
            val intent =
                Intent(Intent.ACTION_OPEN_DOCUMENT)

            intent.type = "image/*"

            intent.addCategory(
                Intent.CATEGORY_OPENABLE
            )

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )

            activity.startActivityForResult(
                intent,
                9001
            )

        } catch (e: Exception) {
            toast("Wallpaper picker unavailable")
        }
    }

    fun applyWallpaper(uri: Uri) {
        wallpaperUri = uri

        try {
            val stream: InputStream =
                context.contentResolver
                    .openInputStream(uri)
                    ?: return

            val bitmap =
                android.graphics.BitmapFactory
                    .decodeStream(stream)

            stream.close()

            if (bitmap != null) {
                val drawable =
                    android.graphics.drawable.BitmapDrawable(
                        resources,
                        bitmap
                    )

                drawable.gravity = Gravity.CENTER

                desktop.background = drawable
            }

        } catch (e: Exception) {
            toast("Wallpaper error: ${e.message}")
        }
    }

    private fun shellQuote(value: String): String {
        return "'" +
            value.replace("'", "'\\''") +
            "'"
    }

    private fun rounded(
        color: Int,
        radius: Int
    ): GradientDrawable {
        val d = GradientDrawable()
        d.setColor(color)
        d.cornerRadius = radius.toFloat()
        d.setStroke(dp(1), border)
        return d
    }

    private fun toast(message: String) {
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun dp(value: Int): Int {
        return (
            value *
            resources.displayMetrics.density
        ).toInt()
    }
}
