package com.metmc.os.desktop

import com.metmc.os.update.MetmcUpdater
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.InputStream

class MetmcDesktop(context: Context) : FrameLayout(context) {
    companion object { @JvmStatic var active: MetmcDesktop? = null }
    private val activity = context as? Activity
    private val desktopArea = DesktopWorkspace(context)
    private val windows = ArrayList<View>()
    private val taskbar = XfceTaskbar(context, { showLauncher() }, { windows.forEach { it.visibility = View.GONE } })
    private var wallpaperUri: Uri? = null

    init { active = this; setBackgroundColor(Color.rgb(10, 12, 18)); buildDesktop(); loadSavedWallpaper() }

    private fun buildDesktop() {
        desktopArea.setBackgroundColor(Color.rgb(10, 12, 18))
        addView(desktopArea, FrameLayout.LayoutParams(-1, -1).apply { bottomMargin = dp(58) })
        createDesktopContent()
        addView(taskbar, FrameLayout.LayoutParams(-1, dp(58)).apply { gravity = Gravity.BOTTOM })
    }

    private fun createDesktopContent() {
        val center = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        center.addView(TextView(context).apply { text = "METMC OS"; textSize = 38f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, dp(60)))
        center.addView(TextView(context).apply { text = "A unified Android + Linux workspace"; textSize = 17f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, dp(45)))
        desktopArea.addView(center, FrameLayout.LayoutParams(-1, -1))
    }

    fun addApplicationWindow(title: String, content: View): View = createWindow(title, content)

    private fun createWindow(title: String, content: View): View {
        lateinit var window: DesktopWindow
        window = DesktopWindow(context, title, content, desktopArea, { if (content is com.metmc.os.linux.LinuxDisplayView) content.stop(); windows.remove(window); taskbar.removeWindow(window) })
        val w = when { title.contains("Browser") || title.contains("Media") || title.contains("Settings") -> dp(700); title.contains("Terminal") -> dp(650); else -> dp(520) }
        val h = when { title.contains("Browser") || title.contains("Media") || title.contains("Settings") -> dp(500); title.contains("Terminal") -> dp(430); else -> dp(360) }
        val aw = if (desktopArea.width > 0) desktopArea.width else resources.displayMetrics.widthPixels
        val ah = if (desktopArea.height > 0) desktopArea.height else resources.displayMetrics.heightPixels - dp(58)
        val mx = (aw - w).coerceAtLeast(0)
        val my = (ah - h).coerceAtLeast(0)
        val c = windows.size * dp(28)
        val params = FrameLayout.LayoutParams(w.coerceAtMost(aw.coerceAtLeast(1)), h.coerceAtMost(ah.coerceAtLeast(1))).apply {
            leftMargin = if (mx > 0) (dp(20) + c) % (mx + 1) else 0
            topMargin = if (my > 0) (dp(20) + c) % (my + 1) else 0
        }
        desktopArea.addWindow(window, params)
        windows.add(window)
        taskbar.addWindow(title, window)
        taskbar.bringToFront()
        return window
    }

    fun showLauncher() {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12)) }
        box.addView(TextView(context).apply { text = "METMC Applications"; textSize = 21f; setTextColor(Color.WHITE); setPadding(dp(4), dp(4), dp(4), dp(12)) })
        fun button(text: String, action: () -> Unit) = Button(context).apply { this.text = text; isAllCaps = false; setTextColor(Color.WHITE); setOnClickListener { action() } }
        box.addView(button("T3 Private Browser") { createWindow("T3 Private Browser", com.metmc.os.apps.t3.T3PrivateBrowserView(context)) }, LinearLayout.LayoutParams(-1, dp(52)))
        box.addView(button("METMC Office — File Opener") { activity?.startActivity(Intent(context, com.metmc.os.office.MetmcOfficeActivity::class.java)) }, LinearLayout.LayoutParams(-1, dp(52)))
        box.addView(button("Media Player") { createWindow("METMC Media Player", com.metmc.os.media.MetmcMediaPlayerView(context)) }, LinearLayout.LayoutParams(-1, dp(52)))
        box.addView(button("Settings") { createWindow("METMC OS Settings", com.metmc.os.settings.MetmcSettingsView(context)) }, LinearLayout.LayoutParams(-1, dp(52)))
        box.addView(button("Terminal") { createWindow("METMC Terminal", MetmcTerminalView(context)) }, LinearLayout.LayoutParams(-1, dp(52)))
        box.addView(button("Android Applications") { DesktopAppMenu(context) { title, content -> createWindow(title, content) }.showAndroidApps() }, LinearLayout.LayoutParams(-1, dp(52)))
        box.addView(button("Linux Applications") { DesktopAppMenu(context) { title, content -> createWindow(title, content) }.showLinuxApps() }, LinearLayout.LayoutParams(-1, dp(52)))
        box.addView(button("Files") { createWindow("Files", MetmcFileManagerView(context)) }, LinearLayout.LayoutParams(-1, dp(52)))
        box.addView(button("Wallpaper") { openWallpaperPicker() }, LinearLayout.LayoutParams(-1, dp(52)))
        box.addView(button("Updates") { activity?.let { MetmcUpdater.checkForUpdate(it, true) } }, LinearLayout.LayoutParams(-1, dp(52)))
        createWindow("Applications", box)
    }

    fun openWallpaperPicker() {
        activity?.let {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            it.startActivityForResult(intent, 9001)
        }
    }

    @JvmOverloads
    fun applyWallpaper(uri: Uri, save: Boolean = true) {
        if (save) context.getSharedPreferences("metmc_prefs", Context.MODE_PRIVATE).edit().putString("wallpaper_uri", uri.toString()).apply()
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
        context.getSharedPreferences("metmc_prefs", Context.MODE_PRIVATE).getString("wallpaper_uri", null)?.let {
            try { applyWallpaper(Uri.parse(it), false) } catch (_: Exception) { }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
