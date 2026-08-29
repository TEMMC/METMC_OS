package com.metmc.os.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class XfceTaskbar(
    context: Context,
    private val onMenuClick: () -> Unit = {},
    private val onHomeClick: () -> Unit = {}
) : LinearLayout(context) {

    private val windowArea = LinearLayout(context)
    private val clock = TextView(context)

    private val windowButtons =
        HashMap<View, Button>()

    private val handler = Handler()

    private val timeFormat =
        SimpleDateFormat(
            "HH:mm",
            Locale.getDefault()
        )

    init {

        orientation = HORIZONTAL

        gravity =
            Gravity.CENTER_VERTICAL

        setPadding(
            dp(6),
            dp(4),
            dp(6),
            dp(4)
        )

        setBackgroundColor(
            Color.rgb(32, 35, 42)
        )

        createMenuButton()

        createHomeButton()

        createWindowArea()

        createClock()

        updateClock()
    }

    private fun createMenuButton() {

        val menu = Button(context)

        menu.text = "☰"

        menu.textSize = 18f

        menu.setTextColor(
            Color.WHITE
        )

        menu.setAllCaps(false)

        menu.setOnClickListener {
            onMenuClick()
        }

        addView(
            menu,
            LinearLayout.LayoutParams(
                dp(52),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun createHomeButton() {

        val button =
            Button(context)

        button.text = "⌂"

        button.textSize = 18f

        button.setTextColor(
            Color.WHITE
        )

        button.setAllCaps(false)

        button.setOnClickListener {
            onHomeClick()
        }

        addView(
            button,
            LinearLayout.LayoutParams(
                dp(48),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun createWindowArea() {

        val scroll =
            HorizontalScrollView(context)

        scroll.isHorizontalScrollBarEnabled =
            false

        windowArea.orientation =
            HORIZONTAL

        windowArea.gravity =
            Gravity.CENTER_VERTICAL

        scroll.addView(
            windowArea,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val params =
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

        params.weight = 1f

        addView(
            scroll,
            params
        )
    }

    private fun createClock() {

        clock.gravity =
            Gravity.CENTER

        clock.textSize = 14f

        clock.setTextColor(
            Color.WHITE
        )

        clock.setTypeface(
            Typeface.DEFAULT,
            Typeface.BOLD
        )

        addView(
            clock,
            LinearLayout.LayoutParams(
                dp(75),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    fun addWindow(
        title: String,
        window: View
    ): Button {

        val existing =
            windowButtons[window]

        if (existing != null) {
            return existing
        }

        val button =
            Button(context)

        button.text = title

        button.textSize = 12f

        button.setTextColor(
            Color.WHITE
        )

        button.setAllCaps(false)

        button.maxLines = 1

        button.setOnClickListener {

            if (
                window.visibility !=
                View.VISIBLE
            ) {

                window.visibility =
                    View.VISIBLE
            }

            window.bringToFront()
        }

        val params =
            LinearLayout.LayoutParams(
                dp(130),
                dp(42)
            )

        params.setMargins(
            dp(2),
            0,
            dp(2),
            0
        )

        windowArea.addView(
            button,
            params
        )

        windowButtons[window] =
            button

        return button
    }

    fun removeWindow(
        window: View
    ) {

        val button =
            windowButtons.remove(window)
                ?: return

        windowArea.removeView(
            button
        )
    }

    fun minimizeWindow(
        window: View
    ) {

        window.visibility =
            View.GONE
    }

    fun restoreWindow(
        window: View
    ) {

        window.visibility =
            View.VISIBLE

        window.bringToFront()
    }

    private fun updateClock() {

        clock.text =
            timeFormat.format(
                Date()
            )

        handler.postDelayed(
            {
                updateClock()
            },
            1000
        )
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
            resources
                .displayMetrics
                .density
        ).toInt()
    }
}
