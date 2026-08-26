package com.metmc.os.desktop

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import com.metmc.os.linux.LinuxDesktopActivity

class MetmcDesktop(context: Context) : FrameLayout(context) {

    private val activity = context as? Activity

    private val desktopArea = FrameLayout(context)
    private val taskbar = LinearLayout(context)
    private val windows = ArrayList<View>()

    private var wallpaperUri: Uri? = null

    init {
        buildDesktop()
    }

    private fun buildDesktop() {
        setBackgroundColor(Color.rgb(10, 12, 18))

        desktopArea.setBackgroundColor(Color.rgb(10, 12, 18))

        addView(
            desktopArea,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )

        createDesktopContent()
        createTaskbar()
    }

    private fun createDesktopContent() {
        val center = LinearLayout(context)
        center.orientation = LinearLayout.VERTICAL
        center.gravity = Gravity.CENTER

        val title = TextView(context)
        title.text = "METMC OS"
        title.textSize = 36f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER

        val subtitle = TextView(context)
        subtitle.text = "Android + Debian Linux Desktop"
        subtitle.textSize = 17f
        subtitle.setTextColor(Color.LTGRAY)
        subtitle.gravity = Gravity.CENTER

        center.addView(title)
        center.addView(
            subtitle,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )

        desktopArea.addView(
            center,
            FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun createTaskbar() {
        taskbar.removeAllViews()

        taskbar.orientation = LinearLayout.HORIZONTAL
        taskbar.gravity = Gravity.CENTER_VERTICAL
        taskbar.setPadding(dp(6), dp(4), dp(6), dp(4))
        taskbar.setBackgroundColor(Color.rgb(25, 27, 34))

        val params = LayoutParams(
            LayoutParams.MATCH_PARENT,
            dp(60)
        )
        params.gravity = Gravity.BOTTOM

        addView(taskbar, params)

        addTaskButton("◈", "METMC Launcher") {
            showLauncher()
        }

        addTaskButton("🐧", "Debian Linux") {
            openLinuxDesktop()
        }

        addTaskButton("▣", "Android") {
            createAndroidWindow()
        }

        addTaskButton("🖼", "Wallpaper") {
            chooseWallpaper()
        }

        addTaskButton("⚙", "Settings") {
            createSettingsWindow()
        }
    }

    private fun addTaskButton(
        icon: String,
        description: String,
        action: () -> Unit
    ) {
        val button = Button(context)

        button.text = icon
        button.textSize = 19f
        button.setTextColor(Color.WHITE)
        button.setAllCaps(false)
        button.contentDescription = description

        button.setOnClickListener {
            action()
        }

        taskbar.addView(
            button,
            LinearLayout.LayoutParams(
                dp(58),
                dp(50)
            )
        )
    }

    private fun showLauncher() {
        val box = LinearLayout(context)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(10), dp(10), dp(10), dp(10))

        addLauncherItem(box, "🐧  Debian / Linux") {
            openLinuxDesktop()
        }

        addLauncherItem(box, "📱  Android Applications") {
            createAndroidWindow()
        }

        addLauncherItem(box, "🖼  Change Wallpaper") {
            chooseWallpaper()
        }

        addLauncherItem(box, "⚙  Settings") {
            createSettingsWindow()
        }

        createWindow("METMC Launcher", box)
    }

    private fun addLauncherItem(
        parent: LinearLayout,
        name: String,
        action: () -> Unit
    ) {
        val button = Button(context)

        button.text = name
        button.setTextColor(Color.WHITE)
        button.setAllCaps(false)

        button.setOnClickListener {
            action()
        }

        parent.addView(
            button,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )
    }

    private fun openLinuxDesktop() {
        try {
            val intent = Intent(
                context,
                LinuxDesktopActivity::class.java
            )

            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to open Debian Desktop: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun createAndroidWindow() {
        val content = LinearLayout(context)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(18), dp(18), dp(18), dp(18))

        val title = TextView(context)
        title.text = "Android Applications"
        title.textSize = 19f
        title.setTextColor(Color.WHITE)

        content.addView(title)

        val info = TextView(context)
        info.text =
            "Android applications will appear here as METMC desktop windows."
        info.setTextColor(Color.LTGRAY)
        info.setPadding(0, dp(15), 0, 0)

        content.addView(info)

        createWindow(
            "Android Apps",
            content
        )
    }

    private fun createSettingsWindow() {
        val box = LinearLayout(context)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(12), dp(12), dp(12), dp(12))

        val title = TextView(context)
        title.text = "METMC OS Settings"
        title.textSize = 22f
        title.setTextColor(Color.WHITE)

        box.addView(title)

        val wallpaper = Button(context)
        wallpaper.text = "🖼  Change Wallpaper"
        wallpaper.setAllCaps(false)

        wallpaper.setOnClickListener {
            chooseWallpaper()
        }

        box.addView(wallpaper)

        createWindow(
            "Settings",
            box
        )
    }

    private fun createWindow(
        title: String,
        content: View
    ): View {

        val window = LinearLayout(context)
        window.orientation = LinearLayout.VERTICAL

        val background = GradientDrawable()
        background.setColor(Color.rgb(30, 32, 40))
        background.cornerRadius = dp(12).toFloat()

        window.background = background
        window.elevation = dp(10).toFloat()

        val titleBar = LinearLayout(context)
        titleBar.gravity = Gravity.CENTER_VERTICAL
        titleBar.setBackgroundColor(Color.rgb(45, 47, 57))

        val titleText = TextView(context)
        titleText.text = title
        titleText.textSize = 15f
        titleText.setTextColor(Color.WHITE)
        titleText.gravity = Gravity.CENTER_VERTICAL
        titleText.setPadding(dp(12), 0, dp(8), 0)

        titleBar.addView(
            titleText,
            LinearLayout.LayoutParams(
                0,
                dp(45),
                1f
            )
        )

        val minimize = windowButton("−")
        val maximize = windowButton("□")
        val close = windowButton("×")

        titleBar.addView(
            minimize,
            LinearLayout.LayoutParams(dp(48), dp(45))
        )

        titleBar.addView(
            maximize,
            LinearLayout.LayoutParams(dp(48), dp(45))
        )

        titleBar.addView(
            close,
            LinearLayout.LayoutParams(dp(48), dp(45))
        )

        window.addView(titleBar)

        window.addView(
            content,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val params = LayoutParams(
            dp(360),
            dp(260)
        )

        params.leftMargin = dp(30 + windows.size * 20)
        params.topMargin = dp(30 + windows.size * 20)

        desktopArea.addView(window, params)
        windows.add(window)

        var maximized = false
        var oldW = params.width
        var oldH = params.height
        var oldX = params.leftMargin
        var oldY = params.topMargin

        minimize.setOnClickListener {
            window.visibility = View.GONE
        }

        maximize.setOnClickListener {
            if (!maximized) {
                oldW = window.width
                oldH = window.height
                oldX = window.left
                oldY = window.top

                val p = window.layoutParams
                p.width = desktopArea.width
                p.height = desktopArea.height
                window.layoutParams = p

                window.x = 0f
                window.y = 0f

                maximized = true
            } else {
                val p = window.layoutParams
                p.width = oldW
                p.height = oldH
                window.layoutParams = p

                window.x = oldX.toFloat()
                window.y = oldY.toFloat()

                maximized = false
            }

            window.bringToFront()
        }

        close.setOnClickListener {
            desktopArea.removeView(window)
            windows.remove(window)
        }

        makeDraggable(window, titleBar)

        window.setOnClickListener {
            window.bringToFront()
        }

        window.bringToFront()

        return window
    }

    private fun windowButton(text: String): Button {
        val b = Button(context)
        b.text = text
        b.textSize = 16f
        b.setTextColor(Color.WHITE)
        b.setAllCaps(false)
        b.setPadding(0, 0, 0, 0)
        return b
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

                MotionEvent.ACTION_UP -> true

                else -> true
            }
        }
    }

    private fun chooseWallpaper() {
        if (activity == null)
            return

        val intent = Intent(
            Intent.ACTION_OPEN_DOCUMENT
        )

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
    }

    fun applyWallpaper(uri: Uri) {
        wallpaperUri = uri

        try {
            val stream =
                context.contentResolver
                    .openInputStream(uri)
                    ?: return

            val bitmap =
                android.graphics.BitmapFactory
                    .decodeStream(stream)

            stream.close()

            if (bitmap != null) {
                desktopArea.background =
                    BitmapDrawable(
                        resources,
                        bitmap
                    )
            }

        } catch (e: Exception) {

            Toast.makeText(
                context,
                "Wallpaper error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun dp(value: Int): Int {
        return (
            value *
            resources.displayMetrics.density
        ).toInt()
    }
}
