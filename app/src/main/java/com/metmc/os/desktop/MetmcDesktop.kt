package com.metmc.os.desktop

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.InputStream

/** METMC unified desktop shell: Android and Linux applications share one workspace. */
class MetmcDesktop(context: Context) : FrameLayout(context) {
    companion object { @JvmStatic var active: MetmcDesktop? = null }

    private val activity = context as? Activity
    private val desktopArea = DesktopWorkspace(context)
    private val windows = ArrayList<DesktopWindow>()
    private val shell = MetmcShellBar(context, { showActivitiesOverview() }, { windows.toList() })
    private var wallpaperUri: Uri? = null

    init {
        active = this
        setBackgroundColor(Color.rgb(8, 10, 16))
        buildDesktop()
        loadSavedWallpaper()
    }

    private fun buildDesktop() {
        desktopArea.setBackgroundColor(Color.rgb(8, 10, 16))
        desktopArea.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> event.clipDescription?.hasMimeType("text/uri-list") == true || event.clipDescription?.hasMimeType("*/*") == true
                DragEvent.ACTION_DRAG_ENTERED, DragEvent.ACTION_DRAG_EXITED -> true
                DragEvent.ACTION_DROP -> {
                    val uri = event.clipData?.getItemAt(0)?.uri
                    if (uri != null) {
                        try {
                            context.startActivity(Intent(context, com.metmc.os.office.MetmcOfficeActivity::class.java).apply {
                                data = uri
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (_: Exception) {}
                        true
                    } else false
                }
                else -> true
            }
        }

        addView(desktopArea, FrameLayout.LayoutParams(-1, -1).apply { topMargin = dp(42) })
        addView(shell, FrameLayout.LayoutParams(-1, dp(42)).apply { gravity = Gravity.TOP })
    }

    fun addApplicationWindow(title: String, content: View): View = createWindow(title, content)

    private fun createWindow(title: String, content: View): View {
        lateinit var window: DesktopWindow
        window = DesktopWindow(context, title, content, desktopArea) {
            windows.remove(window)
            shell.invalidate()
        }
        val width = when {
            title.contains("Browser", true) || title.contains("Media", true) || title.contains("Settings", true) -> dp(700)
            title.contains("Terminal", true) -> dp(650)
            else -> dp(520)
        }
        val height = when {
            title.contains("Browser", true) || title.contains("Media", true) || title.contains("Settings", true) -> dp(500)
            title.contains("Terminal", true) -> dp(430)
            else -> dp(360)
        }
        val aw = desktopArea.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val ah = desktopArea.height.takeIf { it > 0 } ?: (resources.displayMetrics.heightPixels - dp(42))
        val maxX = (aw - width).coerceAtLeast(0)
        val maxY = (ah - height).coerceAtLeast(0)
        val offset = windows.size * dp(26)
        val params = FrameLayout.LayoutParams(width.coerceAtMost(aw), height.coerceAtMost(ah)).apply {
            leftMargin = if (maxX > 0) (dp(24) + offset) % (maxX + 1) else 0
            topMargin = if (maxY > 0) (dp(24) + offset) % (maxY + 1) else 0
        }
        desktopArea.addWindow(window, params)
        windows.add(window)
        window.bringToFront()
        shell.bringToFront()
        return window
    }

    /** GNOME-style overview/launcher: search-first, grouped applications, no legacy taskbar menu. */
    fun showActivitiesOverview() {
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            setBackgroundColor(Color.argb(245, 18, 20, 27))
        }
        panel.addView(TextView(context).apply {
            text = "Activities"
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(10))
        })
        val search = android.widget.EditText(context).apply {
            hint = "Search applications, files and commands"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(dp(14), 0, dp(14), 0)
        }
        panel.addView(search, LinearLayout.LayoutParams(-1, dp(48)))
        fun app(name: String, action: () -> Unit) {
            panel.addView(TextView(context).apply {
                text = name
                textSize = 15f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), 0, dp(12), 0)
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(-1, dp(48)))
        }
        app("T3 Private Browser") { createWindow("T3 Private Browser", com.metmc.os.apps.t3.T3PrivateBrowserView(context)) }
        app("METMC Terminal") { createWindow("METMC Terminal", MetmcTerminalView(context)) }
        app("Files") { createWindow("Files", MetmcFileManagerView(context)) }
        app("Linux Applications") { DesktopAppMenu(context) { title, content -> createWindow(title, content) }.showLinuxApps() }
        app("Android Applications") { createWindow("Android Applications", MetmcAndroidAppsView(context) { pkg -> activity?.let { AndroidWindowLauncher.launch(it, pkg) } }) }
        app("Settings") { createWindow("METMC OS Settings", com.metmc.os.settings.MetmcSettingsView(context)) }
        app("Media Player") { createWindow("METMC Media Player", com.metmc.os.media.MetmcMediaPlayerView(context)) }
        app("Wallpaper") { openWallpaperPicker() }
        app("Updates") { activity?.let { com.metmc.os.update.MetmcUpdater.checkForUpdate(it, true) } }
        createWindow("METMC Activities", panel)
    }

    fun openWallpaperPicker() {
        activity?.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }, 9001)
    }

    @JvmOverloads
    fun applyWallpaper(uri: Uri, save: Boolean = true) {
        if (save) context.getSharedPreferences("metmc_prefs", Context.MODE_PRIVATE).edit().putString("wallpaper_uri", uri.toString()).apply()
        wallpaperUri = uri
        try {
            val stream: InputStream = context.contentResolver.openInputStream(uri) ?: return
            val bitmap = BitmapFactory.decodeStream(stream)
            stream.close()
            if (bitmap != null) desktopArea.background = BitmapDrawable(resources, bitmap)
        } catch (_: Exception) {
            Toast.makeText(context, "Wallpaper could not be loaded", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadSavedWallpaper() {
        context.getSharedPreferences("metmc_prefs", Context.MODE_PRIVATE).getString("wallpaper_uri", null)?.let {
            try { applyWallpaper(Uri.parse(it), false) } catch (_: Exception) {}
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
