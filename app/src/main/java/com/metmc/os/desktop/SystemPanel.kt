package com.metmc.os.desktop

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemPanel(
    context: Context,
    private val onMenuClick: () -> Unit,
    private val onHomeClick: () -> Unit
) : LinearLayout(context) {

    private val clock = TextView(context)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(8), 0, dp(8), 0)
        setBackgroundColor(Color.rgb(28, 30, 38))

        val menu = Button(context)
        menu.text = "METMC"
        menu.setAllCaps(false)
        menu.setOnClickListener { onMenuClick() }

        addView(
            menu,
            LinearLayout.LayoutParams(
                dp(90),
                dp(48)
            )
        )

        val home = Button(context)
        home.text = "⌂"
        home.textSize = 20f
        home.setOnClickListener { onHomeClick() }

        addView(
            home,
            LinearLayout.LayoutParams(
                dp(56),
                dp(48)
            )
        )

        val spacer = LinearLayout(context)

        addView(
            spacer,
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            )
        )

        clock.gravity = Gravity.CENTER
        clock.textSize = 14f
        clock.setTextColor(Color.WHITE)

        addView(
            clock,
            LinearLayout.LayoutParams(
                dp(150),
                dp(48)
            )
        )

        updateClock()

        post(object : Runnable {
            override fun run() {
                updateClock()
                postDelayed(this, 1000)
            }
        })
    }

    private fun updateClock() {
        clock.text = SimpleDateFormat(
            "EEE HH:mm",
            Locale.getDefault()
        ).format(Date())
    }

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).toInt()
    }
}
