package com.metmc.os.desktop

import com.metmc.os.R

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.ViewGroup
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import java.io.InputStream

class MetmcDesktop(
    context: Context
) : FrameLayout(context) {

    private val activity = context as? Activity

    private val desktopArea = DesktopWorkspace(context)
    private val taskbar = XfceTaskbar(
        context,
        { showLauncher() },
        { windows.forEach { it.visibility = View.GONE } }
    )
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
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
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
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )

        center.addView(
            subtitle,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(45)
            )
        )

        desktopArea.addView(
            center,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
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

        val params = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(58)
        )

        params.gravity = Gravity.BOTTOM

        addView(
            taskbar,
            params
        )
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

        val window = DesktopWindow(
            context,
            title,
            content,
            desktopArea
        )

        val params = FrameLayout.LayoutParams(
            dp(520),
            dp(360)
        )

        val offset =
            windows.size * dp(18)

        params.leftMargin =
            dp(30) + offset

        params.topMargin =
            dp(30) + offset

        desktopArea.addWindow(
            window,
            params
        )

        windows.add(window)

        taskbar.addWindow(
            title,
            window
        )

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
                ViewGroup.LayoutParams.MATCH_PARENT,
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
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        android.setOnClickListener {
            val menu = DesktopAppMenu(context) { title, content ->
                createWindow(title, content)
            }
            menu.showAndroidApps()
        }

        linux.setOnClickListener {
            val menu = DesktopAppMenu(context) { title, content ->
                createWindow(title, content)
            }
            menu.showLinuxApps()
        }

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
