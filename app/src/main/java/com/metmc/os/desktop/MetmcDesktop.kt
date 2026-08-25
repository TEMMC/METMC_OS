package com.metmc.os.desktop

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.metmc.os.linux.LinuxBridge
import com.metmc.os.windows.WindowManager

class MetmcDesktop(context: Context) : LinearLayout(context) {

    private val linuxBridge = LinuxBridge(context)
    private val windowManager = WindowManager(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(18, 20, 24))

        val desktop = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val title = TextView(context).apply {
            text = "METMC OS v6"
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(context).apply {
            text = "Android + Linux Desktop"
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        desktop.addView(title)
        desktop.addView(subtitle)

        val taskbar = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 12, 24, 12)
            setBackgroundColor(Color.rgb(28, 30, 36))
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                64
            )
        }

        val start = TextView(context).apply {
            text = "  METMC  "
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val status = TextView(context).apply {
            text = "  Linux: ready  |  Android: ready"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        taskbar.addView(start)
        taskbar.addView(status)

        addView(desktop)
        addView(taskbar)
    }
}
