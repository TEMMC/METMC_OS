package com.metmc.os.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * METMC Shell: GNOME-inspired shell chrome for the unified Android/Linux workspace.
 * It intentionally owns only shell UI; application windows remain normal DesktopWindow instances.
 */
class MetmcShellBar(
    context: Context,
    private val onActivities: () -> Unit,
    private val onWindows: () -> List<DesktopWindow>
) : LinearLayout(context) {

    private val clock = TextView(context)
    private val windowStrip = LinearLayout(context)
    private val clockTick = object : Runnable {
        override fun run() {
            clock.text = SimpleDateFormat("EEE  HH:mm", Locale.getDefault()).format(Date())
            refreshWindows()
            postDelayed(this, 1000)
        }
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), 0, dp(12), 0)
        setBackgroundColor(Color.argb(235, 18, 20, 25))
        elevation = dp(6).toFloat()
        minimumHeight = dp(42)

        val activities = TextView(context).apply {
            text = "Activities"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(10), 0, dp(10), 0)
            setOnClickListener { onActivities() }
        }
        addView(activities, LayoutParams(dp(100), -1))

        windowStrip.orientation = HORIZONTAL
        windowStrip.gravity = Gravity.CENTER_VERTICAL
        addView(windowStrip, LayoutParams(0, -1, 1f))

        clock.textSize = 14f
        clock.typeface = Typeface.DEFAULT_BOLD
        clock.setTextColor(Color.WHITE)
        clock.gravity = Gravity.CENTER
        addView(clock, LayoutParams(dp(115), -1))

        val system = TextView(context).apply {
            text = "⌄  🔊  ◉"
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(4), 0)
        }
        addView(system, LayoutParams(dp(105), -1))

        refreshWindows()
        post(clockTick)
    }

    private fun refreshWindows() {
        windowStrip.removeAllViews()
        for (window in onWindows()) {
            if (window.visibility == View.GONE) continue
            val title = window.windowTitle()
            val item = TextView(context).apply {
                text = "  $title  "
                textSize = 12f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(dp(5), 0, dp(5), 0)
                setOnClickListener { window.bringToFront() }
            }
            windowStrip.addView(item, LayoutParams(dp(125), dp(32)))
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
