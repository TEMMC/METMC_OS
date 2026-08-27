package com.metmc.os.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

class DesktopLauncher(
    context: Context
) : LinearLayout(context) {

    private var popup: PopupWindow? = null

    init {
        orientation = VERTICAL
        setPadding(dp(8), dp(8), dp(8), dp(8))
    }

    fun show(
        anchor: View,
        onLaunch: (String) -> Unit
    ) {

        if (popup?.isShowing == true) {
            popup?.dismiss()
            return
        }

        val menu = LinearLayout(context)
        menu.orientation = VERTICAL
        menu.setPadding(dp(10), dp(10), dp(10), dp(10))

        val background = GradientDrawable()
        background.setColor(Color.rgb(32, 34, 42))
        background.cornerRadius = dp(12).toFloat()

        menu.background = background
        menu.elevation = dp(12).toFloat()

        addSection(
            menu,
            "ANDROID",
            listOf("Apps", "Files"),
            onLaunch
        )

        addSection(
            menu,
            "LINUX",
            listOf("Terminal", "Linux Apps"),
            onLaunch
        )

        addSection(
            menu,
            "SYSTEM",
            listOf("Settings", "System Info", "Wallpaper"),
            onLaunch
        )

        popup = PopupWindow(
            menu,
            dp(280),
            LayoutParams.WRAP_CONTENT,
            true
        )

        popup?.isOutsideTouchable = true
        popup?.elevation = dp(12).toFloat()

        popup?.showAtLocation(
            anchor,
            Gravity.BOTTOM or Gravity.START,
            dp(8),
            dp(66)
        )
    }

    private fun addSection(
        parent: LinearLayout,
        title: String,
        apps: List<String>,
        onLaunch: (String) -> Unit
    ) {

        val label = TextView(context)
        label.text = title
        label.textSize = 11f
        label.setTextColor(Color.LTGRAY)
        label.setPadding(
            dp(10),
            dp(12),
            dp(10),
            dp(4)
        )

        parent.addView(
            label,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        apps.forEach { app ->

            val button = TextView(context)

            button.text = app
            button.textSize = 15f
            button.gravity = Gravity.CENTER_VERTICAL
            button.setTextColor(Color.WHITE)

            button.setPadding(
                dp(14),
                0,
                dp(14),
                0
            )

            button.setOnClickListener {
                popup?.dismiss()
                onLaunch(app)
            }

            parent.addView(
                button,
                LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    dp(48)
                )
            )
        }
    }

    private fun dp(value: Int): Int {
        return (
            value *
            resources.displayMetrics.density
        ).toInt()
    }
}
