package com.metmc.os.desktop

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.InputStream

class MetmcDesktop(
    context: Context
) : FrameLayout(context) {

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

        desktopArea.setBackgroundColor(
            Color.rgb(10, 12, 18)
        )

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

        subtitle.text = "Android + Linux Desktop"
        subtitle.textSize = 17f
        subtitle.setTextColor(Color.LTGRAY)
        subtitle.gravity = Gravity.CENTER

        center.addView(title)

        center.addView(
            subtitle,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                60
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

        taskbar.orientation = LinearLayout.HORIZONTAL
        taskbar.gravity = Gravity.CENTER_VERTICAL

        taskbar.setPadding(
            dp(8),
            dp(6),
            dp(8),
            dp(6)
        )

        taskbar.setBackgroundColor(
            Color.rgb(25, 27, 34)
        )

        val params = LayoutParams(
            LayoutParams.MATCH_PARENT,
            dp(64)
        )

        params.gravity = Gravity.BOTTOM

        addView(taskbar, params)

        addTaskButton(
            "◈",
            "METMC Launcher"
        ) {
            showLauncher()
        }

        addTaskButton(
            "🐧",
            "Linux"
        ) {
            createLinuxWindow()
        }

        addTaskButton(
            "📱",
            "Android"
        ) {
            createAndroidWindow()
        }

        addTaskButton(
            "🖼",
            "Wallpaper"
        ) {
            chooseWallpaper()
        }

        addTaskButton(
            "⚙",
            "Settings"
        ) {
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
        button.textSize = 20f

        button.setTextColor(Color.WHITE)

        button.setOnClickListener {
            action()
        }

        button.contentDescription = description

        taskbar.addView(
            button,
            LinearLayout.LayoutParams(
                dp(62),
                dp(52)
            )
        )
    }

    private fun showLauncher() {

        val box = LinearLayout(context)

        box.orientation = LinearLayout.VERTICAL

        val title = TextView(context)

        title.text = "METMC Launcher"
        title.textSize = 22f
        title.setTextColor(Color.WHITE)
        title.setPadding(
            dp(16),
            dp(12),
            dp(16),
            dp(12)
        )

        box.addView(title)

        addLauncherItem(
            box,
            "🐧  Debian / Linux"
        ) {
            createLinuxWindow()
        }

        addLauncherItem(
            box,
            "📱  Android Applications"
        ) {
            createAndroidWindow()
        }

        addLauncherItem(
            box,
            "🖼  Change Wallpaper"
        ) {
            chooseWallpaper()
        }

        createWindow(
            "METMC Launcher",
            box
        )
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

    private fun createLinuxWindow() {

        val content = LinearLayout(context)

        content.orientation = LinearLayout.VERTICAL

        val title = TextView(context)

        title.text =
            "Debian / Linux\n\nLinux application launcher"

        title.setTextColor(Color.WHITE)
        title.textSize = 17f
        title.setPadding(dp(18), dp(18), dp(18), dp(18))

        content.addView(title)

        val terminal = Button(context)

        terminal.text = "Open Debian Terminal"
        terminal.setAllCaps(false)

        terminal.setOnClickListener {

            Toast.makeText(
                context,
                "Debian launcher connected",
                Toast.LENGTH_SHORT
            ).show()
        }

        content.addView(terminal)

        createWindow(
            "Linux",
            content
        )
    }

    private fun createAndroidWindow() {

        val content = LinearLayout(context)

        content.orientation = LinearLayout.VERTICAL

        val title = TextView(context)

        title.text =
            "Android Applications"

        title.textSize = 19f
        title.setTextColor(Color.WHITE)
        title.setPadding(dp(18), dp(18), dp(18), dp(18))

        content.addView(title)

        val info = TextView(context)

        info.text =
            "Android applications will appear here as METMC desktop windows."

        info.setTextColor(Color.LTGRAY)
        info.setPadding(dp(18), dp(10), dp(18), dp(18))

        content.addView(info)

        createWindow(
            "Android Apps",
            content
        )
    }

    private fun createSettingsWindow() {

        val box = LinearLayout(context)

        box.orientation = LinearLayout.VERTICAL

        val title = TextView(context)

        title.text = "METMC OS Settings"
        title.textSize = 22f
        title.setTextColor(Color.WHITE)
        title.setPadding(dp(18), dp(18), dp(18), dp(18))

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

        background.setColor(
            Color.rgb(30, 32, 40)
        )

        background.cornerRadius = dp(14).toFloat()

        window.background = background

        val titleBar = LinearLayout(context)

        titleBar.gravity = Gravity.CENTER_VERTICAL

        titleBar.setBackgroundColor(
            Color.rgb(45, 47, 57)
        )

        val titleText = TextView(context)

        titleText.text = title
        titleText.textSize = 15f
        titleText.setTextColor(Color.WHITE)

        titleBar.addView(
            titleText,
            LinearLayout.LayoutParams(
                0,
                dp(45),
                1f
            )
        )

        val minimize = Button(context)

        minimize.text = "−"

        minimize.setOnClickListener {
            window.visibility = View.GONE
        }

        titleBar.addView(
            minimize,
            LinearLayout.LayoutParams(
                dp(48),
                dp(45)
            )
        )

        val maximize = Button(context)

        maximize.text = "□"

        maximize.setOnClickListener {

            val params = window.layoutParams

            params.width = LayoutParams.MATCH_PARENT
            params.height = desktopArea.height - dp(64)

            window.layoutParams = params

            window.x = 0f
            window.y = 0f

            window.bringToFront()
        }

        titleBar.addView(
            maximize,
            LinearLayout.LayoutParams(
                dp(48),
                dp(45)
            )
        )

        val close = Button(context)

        close.text = "×"

        close.setOnClickListener {

            desktopArea.removeView(window)

            windows.remove(window)

        }

        titleBar.addView(
            close,
            LinearLayout.LayoutParams(
                dp(48),
                dp(45)
            )
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
            dp(340),
            dp(250)
        )

        params.leftMargin =
            dp(30 + windows.size * 20)

        params.topMargin =
            dp(30 + windows.size * 20)

        desktopArea.addView(window, params)

        windows.add(window)

        makeDraggable(
            window,
            titleBar
        )

        window.bringToFront()

        return window
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

                android.view.MotionEvent.ACTION_DOWN -> {

                    downX = event.rawX
                    downY = event.rawY

                    startX = window.x
                    startY = window.y

                    window.bringToFront()

                    true
                }

                android.view.MotionEvent.ACTION_MOVE -> {

                    window.x =
                        startX +
                        event.rawX -
                        downX

                    window.y =
                        startY +
                        event.rawY -
                        downY

                    true
                }

                else -> true
            }
        }
    }

    private fun chooseWallpaper() {

        if (activity == null)
            return

        val intent =
            android.content.Intent(
                android.content.Intent.ACTION_OPEN_DOCUMENT
            )

        intent.type = "image/*"

        intent.addCategory(
            android.content.Intent.CATEGORY_OPENABLE
        )

        intent.addFlags(
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
            android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )

        activity.startActivityForResult(
            intent,
            9001
        )
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

                drawable.gravity =
                    Gravity.CENTER

                drawable.setTileModeX(
                    android.graphics.Shader.TileMode.CLAMP
                )

                drawable.setTileModeY(
                    android.graphics.Shader.TileMode.CLAMP
                )

                desktopArea.background =
                    drawable
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
