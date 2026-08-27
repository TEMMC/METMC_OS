package com.metmc.os.desktop

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.MotionEvent
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
        setBackgroundColor(Color.rgb(10,12,18))
        buildDesktop()
    }

    private fun buildDesktop() {

        desktopArea.setBackgroundColor(
            Color.rgb(10,12,18)
        )

        addView(
            desktopArea,
            LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT
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
        title.textSize = 38f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER

        val subtitle = TextView(context)
        subtitle.text = "Android + Linux Desktop"
        subtitle.textSize = 17f
        subtitle.setTextColor(Color.LTGRAY)
        subtitle.gravity = Gravity.CENTER

        center.addView(
            title,
            LinearLayout.LayoutParams(
                MATCH_PARENT,
                dp(60)
            )
        )

        center.addView(
            subtitle,
            LinearLayout.LayoutParams(
                MATCH_PARENT,
                dp(45)
            )
        )

        desktopArea.addView(
            center,
            FrameLayout.LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT
            )
        )
    }

    /*
     * CLEAN TASKBAR
     *
     * No Linux button.
     * No Android button.
     * No wallpaper button.
     * No settings button.
     *
     * Applications create their own taskbar buttons.
     */
    private fun createTaskbar() {

        taskbar.orientation = LinearLayout.HORIZONTAL
        taskbar.gravity = Gravity.CENTER_VERTICAL

        taskbar.setPadding(
            dp(8),
            dp(5),
            dp(8),
            dp(5)
        )

        taskbar.setBackgroundColor(
            Color.rgb(25,27,34)
        )

        val params = LayoutParams(
            MATCH_PARENT,
            dp(58)
        )

        params.gravity = Gravity.BOTTOM

        addView(taskbar, params)
    }

    /*
     * Add an application to the shared desktop.
     *
     * Both Android and Linux applications use this same
     * window/taskbar mechanism.
     */
    fun addApplicationWindow(
        title: String,
        content: View
    ): View {

        return createWindow(
            title,
            content
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
            Color.rgb(30,32,40)
        )
        background.cornerRadius =
            dp(12).toFloat()

        window.background = background
        window.elevation = dp(10).toFloat()

        val titleBar = LinearLayout(context)
        titleBar.gravity =
            Gravity.CENTER_VERTICAL

        titleBar.setBackgroundColor(
            Color.rgb(45,47,57)
        )

        val titleText = TextView(context)
        titleText.text = title
        titleText.textSize = 14f
        titleText.setTextColor(Color.WHITE)
        titleText.setPadding(
            dp(14),
            0,
            dp(8),
            0
        )

        titleBar.addView(
            titleText,
            LinearLayout.LayoutParams(
                0,
                dp(44),
                1f
            )
        )

        val minimize = windowButton("—")
        val maximize = windowButton("□")
        val close = windowButton("×")

        titleBar.addView(
            minimize,
            LinearLayout.LayoutParams(
                dp(48),
                dp(44)
            )
        )

        titleBar.addView(
            maximize,
            LinearLayout.LayoutParams(
                dp(48),
                dp(44)
            )
        )

        titleBar.addView(
            close,
            LinearLayout.LayoutParams(
                dp(48),
                dp(44)
            )
        )

        window.addView(titleBar)

        window.addView(
            content,
            LinearLayout.LayoutParams(
                MATCH_PARENT,
                0,
                1f
            )
        )

        val params = LayoutParams(
            dp(520),
            dp(360)
        )

        params.leftMargin =
            dp(30 + windows.size * 18)

        params.topMargin =
            dp(30 + windows.size * 18)

        desktopArea.addView(
            window,
            params
        )

        windows.add(window)

        /*
         * Every application automatically gets
         * a taskbar button.
         */
        val taskButton = Button(context)

        taskButton.text = title
        taskButton.textSize = 12f
        taskButton.setTextColor(Color.WHITE)
        taskButton.setAllCaps(false)
        taskButton.maxLines = 1
        taskButton.ellipsize =
            android.text.TextUtils.TruncateAt.END

        taskButton.setOnClickListener {

            if (window.visibility != VISIBLE) {
                window.visibility = VISIBLE
                window.bringToFront()
            } else {
                window.bringToFront()
            }
        }

        taskbar.addView(
            taskButton,
            LinearLayout.LayoutParams(
                dp(150),
                dp(46)
            )
        )

        window.setTag(taskButton)

        minimize.setOnClickListener {
            window.visibility = GONE
        }

        maximize.setOnClickListener {

            val p = window.layoutParams

            if (window.getTag(R.id.metmc_maximized_tag) == true) {

                p.width = dp(520)
                p.height = dp(360)

                window.layoutParams = p

                window.x =
                    dp(30 + (windows.indexOf(window).coerceAtLeast(0) * 18)).toFloat()

                window.y =
                    dp(30 + (windows.indexOf(window).coerceAtLeast(0) * 18)).toFloat()

                window.setTag(
                    R.id.metmc_maximized_tag,
                    false
                )

            } else {

                p.width = MATCH_PARENT
                p.height =
                    desktopArea.height - dp(58)

                window.layoutParams = p

                window.x = 0f
                window.y = 0f

                window.setTag(
                    R.id.metmc_maximized_tag,
                    true
                )
            }

            window.bringToFront()
        }

        close.setOnClickListener {

            desktopArea.removeView(window)
            taskbar.removeView(taskButton)
            windows.remove(window)
        }

        makeDraggable(
            window,
            titleBar
        )

        window.bringToFront()

        return window
    }

    private fun windowButton(
        text: String
    ): Button {

        val b = Button(context)

        b.text = text
        b.textSize = 16f
        b.setTextColor(Color.WHITE)
        b.setAllCaps(false)
        b.setPadding(0,0,0,0)

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

            when(event.action) {

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

    /*
     * Optional launcher window.
     *
     * Linux is NOT a separate desktop feature anymore.
     * Linux applications should be launched through the
     * same application/window system as Android applications.
     */
    fun showLauncher() {

        val box = LinearLayout(context)
        box.orientation =
            LinearLayout.VERTICAL

        box.setPadding(
            dp(16),
            dp(12),
            dp(16),
            dp(12)
        )

        val title = TextView(context)
        title.text = "METMC Applications"
        title.textSize = 21f
        title.setTextColor(Color.WHITE)
        title.setPadding(
            dp(4),
            dp(4),
            dp(4),
            dp(12)
        )

        box.addView(title)

        val android = Button(context)
        android.text = "Android Applications"
        android.setAllCaps(false)
        android.setTextColor(Color.WHITE)

        box.addView(
            android,
            LinearLayout.LayoutParams(
                MATCH_PARENT,
                dp(55)
            )
        )

        val linux = Button(context)
        linux.text = "Linux Applications"
        linux.setAllCaps(false)
        linux.setTextColor(Color.WHITE)

        box.addView(
            linux,
            LinearLayout.LayoutParams(
                MATCH_PARENT,
                dp(55)
            )
        )

        createWindow(
            "Applications",
            box
        )
    }

    private fun chooseWallpaper() {

        if (activity == null)
            return

        val intent =
            Intent(
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

            val stream: InputStream =
                context.contentResolver
                    .openInputStream(uri)
                    ?: return

            val bitmap =
                android.graphics.BitmapFactory
                    .decodeStream(stream)

            stream.close()

            if(bitmap != null) {

                desktopArea.background =
                    android.graphics.drawable.BitmapDrawable(
                        resources,
                        bitmap
                    )
            }

        } catch(e: Exception) {

            Toast.makeText(
                context,
                "Wallpaper error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun dp(value: Int): Int =
        (
            value *
            resources.displayMetrics.density
        ).toInt()
}
