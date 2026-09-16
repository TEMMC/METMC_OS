package com.metmc.os.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
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
    private val windowButtons = HashMap<View, Button>()
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(7), dp(5), dp(7), dp(5))
        setBackgroundColor(Color.rgb(18, 20, 25))
        elevation = dp(8).toFloat()
        createMenuButton()
        createHomeButton()
        createWindowArea()
        createClock()
        updateClock()
    }

    private fun createMenuButton() {
        val menu = taskButton("▦", 20f)
        menu.contentDescription = "Applications"
        menu.setOnClickListener { onMenuClick() }
        addView(menu, LinearLayout.LayoutParams(dp(48), dp(44)))
    }

    private fun createHomeButton() {
        val button = taskButton("⌂", 20f)
        button.contentDescription = "Home"
        button.setOnClickListener { onHomeClick() }
        addView(button, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginStart = dp(3) })
    }

    private fun createWindowArea() {
        val scroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        windowArea.orientation = HORIZONTAL
        windowArea.gravity = Gravity.CENTER_VERTICAL
        scroll.addView(windowArea, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        addView(scroll, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { marginStart = dp(5); marginEnd = dp(5) })
    }

    private fun createClock() {
        clock.gravity = Gravity.CENTER
        clock.textSize = 13f
        clock.setTextColor(Color.WHITE)
        clock.typeface = Typeface.DEFAULT_BOLD
        clock.setPadding(dp(8), 0, dp(8), 0)
        addView(clock, LinearLayout.LayoutParams(dp(66), dp(44)))
    }

    fun addWindow(title: String, window: View): Button {
        windowButtons[window]?.let { return it }
        val button = taskButton(appGlyph(title), 15f).apply {
            contentDescription = title
            tag = title
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setOnClickListener {
                if (window.visibility != View.VISIBLE) window.visibility = View.VISIBLE
                window.bringToFront()
            }
            setOnLongClickListener {
                window.visibility = View.GONE
                true
            }
        }
        val params = LinearLayout.LayoutParams(dp(52), dp(42)).apply { marginStart = dp(2); marginEnd = dp(2) }
        windowArea.addView(button, params)
        windowButtons[window] = button
        return button
    }

    fun removeWindow(window: View) {
        windowButtons.remove(window)?.let { windowArea.removeView(it) }
    }

    fun minimizeWindow(window: View) { window.visibility = View.GONE }

    fun restoreWindow(window: View) {
        window.visibility = View.VISIBLE
        window.bringToFront()
    }

    fun setWindowActive(window: View, active: Boolean) {
        windowButtons[window]?.alpha = if (active) 1f else 0.65f
    }

    private fun taskButton(label: String, size: Float) = Button(context).apply {
        text = label
        textSize = size
        isAllCaps = false
        setTextColor(Color.WHITE)
        minWidth = 0
        minimumWidth = 0
        setPadding(0, 0, 0, 0)
        setBackgroundColor(Color.TRANSPARENT)
        stateListAnimator = null
    }

    private fun appGlyph(title: String): String {
        val t = title.lowercase(Locale.ROOT)
        return when {
            "browser" in t -> "◎"
            "media" in t || "player" in t -> "♫"
            "terminal" in t -> "›_"
            "file" in t -> "▣"
            "setting" in t -> "⚙"
            "linux" in t -> "⌘"
            "office" in t || "document" in t -> "▤"
            else -> "•"
        }
    }

    private fun updateClock() {
        clock.text = timeFormat.format(Date())
        handler.postDelayed({ updateClock() }, 1000)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
