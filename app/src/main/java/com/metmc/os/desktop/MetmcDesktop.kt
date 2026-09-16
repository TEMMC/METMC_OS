package com.metmc.os.desktop

import com.metmc.os.R
import com.metmc.os.update.MetmcUpdater

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
    private val desktopArea: DesktopWorkspace = DesktopWorkspace(context)
    private val windows: ArrayList<View> = ArrayList()

    private val taskbar: XfceTaskbar = XfceTaskbar(
        context,
        { showLauncher() },
        { windows.forEach { it.visibility = View.GONE } }
    )

    private var wallpaperUri: Uri? = null

    init {
        setBackgroundColor(Color.rgb(10,12,18))
        buildDesktop()
        loadSavedWallpaper()
    }

    private fun buildDesktop() {
        desktopArea.setBackgroundColor(Color.rgb(10,12,18))
        addView(desktopArea, LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply { bottomMargin = dp(58) })
        createDesktopContent()
        createTaskbar()
    }

    private fun createDesktopContent() {
        val center = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        val title = TextView(context).apply {
            text = "METMC OS"
            textSize = 38f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(context).apply {
            text = "Android + Linux Desktop"
            textSize = 17f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        center.addView(title, LinearLayout.LayoutParams(-1, dp(60)))
        center.addView(subtitle, LinearLayout.LayoutParams(-1, dp(45)))
        desktopArea.addView(center, FrameLayout.LayoutParams(-1, -1))
    }

    private fun createTaskbar() {
        addView(taskbar, LayoutParams(-1, dp(58)).apply { gravity = Gravity.BOTTOM })
    }

    fun addApplicationWindow(title: String, content: View): View = createWindow(title, content)

    private fun createWindow(title: String, content: View): View {
        lateinit var window: DesktopWindow
        window = DesktopWindow(context, title, content, desktopArea, {
            if (content is com.metmc.os.linux.LinuxDisplayView) content.stop()
            windows.remove(window)
            taskbar.removeWindow(window)
        })

        val windowWidth = dp(520)
        val windowHeight = dp(360)
        val availableWidth = if (desktopArea.width > 0) desktopArea.width else resources.displayMetrics.widthPixels
        val availableHeight = if (desktopArea.height > 0) desktopArea.height else resources.displayMetrics.heightPixels - dp(58)
        val maxLeft = (availableWidth - windowWidth).coerceAtLeast(0)
        val maxTop = (availableHeight - windowHeight).coerceAtLeast(0)
        val cascade = windows.size * dp(28)
        val params = FrameLayout.LayoutParams(windowWidth, windowHeight).apply {
            leftMargin = if (maxLeft > 0) (dp(20) + cascade) % (maxLeft + 1) else 0
            topMargin = if (maxTop > 0) (dp(20) + cascade) % (maxTop + 1) else 0
        }
        desktopArea.addWindow(window, params)
        windows.add(window)
        taskbar.addWindow(title, window)
        taskbar.bringToFront()
        return window
    }

    fun showLauncher() {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        box.addView(TextView(context).apply {
            text = "METMC Applications"
            textSize = 21f
            setTextColor(Color.WHITE)
            setPadding(dp(4), dp(4), dp(4), dp(12))
        })

        fun launcherButton(text: String, action: () -> Unit): Button = Button(context).apply {
            this.text = text
            isAllCaps = false
            setTextColor(Color.WHITE)
            setOnClickListener { action() }
        }

        box.addView(launcherButton("T3 Private Browser") {
            createWindow("T3 Private Browser", com.metmc.os.apps.t3.T3PrivateBrowserView(context))
        }, LinearLayout.LayoutParams(-1, dp(55)))

        box.addView(launcherButton("Android Applications") {
            DesktopAppMenu(context) { title, content -> createWindow(title, content) }.showAndroidApps()
        }, LinearLayout.LayoutParams(-1, dp(55)))

        box.addView(launcherButton("Linux Applications") {
            DesktopAppMenu(context) { title, content -> createWindow(title, content) }.showLinuxApps()
        }, LinearLayout.LayoutParams(-1, dp(55)))

        box.addView(launcherButton("Wallpaper") { chooseWallpaper() }, LinearLayout.LayoutParams(-1, dp(55)))
        box.addView(launcherButton("Files") { createWindow("Files", FileManagerView(context)) }, LinearLayout.LayoutParams(-1, dp(55)))
        box.addView(launcherButton("Updates") {
            activity?.let { MetmcUpdater.checkForUpdate(it, true) }
        }, LinearLayout.LayoutParams(-1, dp(55)))

        createWindow("Applications", box)
    }

    private fun chooseWallpaper() {
        if (activity == null) return
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        activity.startActivityForResult(intent, 9001)
    }

    @JvmOverloads
    fun applyWallpaper(uri: Uri, save: Boolean = true) {
        if (save) context.getSharedPreferences("metmc_prefs", Context.MODE_PRIVATE)
            .edit().putString("wallpaper_uri", uri.toString()).apply()
        wallpaperUri = uri
        try {
            val stream: InputStream = context.contentResolver.openInputStream(uri) ?: return
            val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
            stream.close()
            if (bitmap != null) desktopArea.background = android.graphics.drawable.BitmapDrawable(resources, bitmap)
        } catch (_: Exception) {
            Toast.makeText(context, "Wallpaper could not be loaded", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadSavedWallpaper() {
        context.getSharedPreferences("metmc_prefs", Context.MODE_PRIVATE)
            .getString("wallpaper_uri", null)?.let {
                try { applyWallpaper(Uri.parse(it), save = false) } catch (_: Exception) { }
            }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
