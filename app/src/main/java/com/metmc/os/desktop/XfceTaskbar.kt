package com.metmc.os.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
    private val windowButtons = HashMap<View, TextView>()
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEE d MMM", Locale.getDefault())

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(6), dp(5), dp(6), dp(5))
        background = GradientDrawable().apply { setColor(Color.rgb(18, 20, 26)); cornerRadius = dp(12).toFloat() }
        elevation = dp(10).toFloat()
        createMenuButton(); createHomeButton(); createWindowArea(); createClock(); updateClock()
    }

    private fun createMenuButton() {
        val menu = taskButton("▦", 20f).apply { contentDescription = "Applications"; background = pill(false) }
        menu.setOnClickListener { onMenuClick() }
        addView(menu, LinearLayout.LayoutParams(dp(48), dp(44)))
    }

    private fun createHomeButton() {
        val button = taskButton("⌂", 20f).apply { contentDescription = "Home"; background = pill(false) }
        button.setOnClickListener { onHomeClick() }
        addView(button, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginStart = dp(4) })
    }

    private fun createWindowArea() {
        val scroll = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER }
        windowArea.orientation = HORIZONTAL; windowArea.gravity = Gravity.CENTER_VERTICAL
        scroll.addView(windowArea, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        addView(scroll, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { marginStart = dp(6); marginEnd = dp(6) })
    }

    private fun createClock() {
        clock.gravity = Gravity.CENTER
        clock.textSize = 11f
        clock.setTextColor(Color.WHITE)
        clock.typeface = Typeface.DEFAULT_BOLD
        clock.maxLines = 2
        clock.setPadding(dp(5), 0, dp(5), 0)
        addView(clock, LinearLayout.LayoutParams(dp(82), dp(44)))
    }

    fun addWindow(title: String, window: View): TextView {
        windowButtons[window]?.let { return it }
        val button = TextView(context).apply {
            text = "${appGlyph(title)}  ${shortTitle(title)}"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(dp(8), 0, dp(8), 0)
            background = pill(true)
            contentDescription = title
            setOnClickListener { restoreWindow(window) }
            setOnLongClickListener { minimizeWindow(window); true }
        }
        val params = LinearLayout.LayoutParams(dp(118), dp(40)).apply { marginStart = dp(2); marginEnd = dp(2) }
        windowArea.addView(button, params); windowButtons[window] = button; return button
    }

    fun removeWindow(window: View) { windowButtons.remove(window)?.let { windowArea.removeView(it) } }
    fun minimizeWindow(window: View) { window.visibility = View.GONE; setWindowActive(window, false) }
    fun restoreWindow(window: View) { window.visibility = View.VISIBLE; window.bringToFront(); setWindowActive(window, true) }
    fun setWindowActive(window: View, active: Boolean) { windowButtons[window]?.background = pill(active); windowButtons[window]?.alpha = if (active) 1f else .72f }

    private fun taskButton(label: String, size: Float) = Button(context).apply {
        text = label; textSize = size; isAllCaps = false; setTextColor(Color.WHITE); minWidth = 0; minimumWidth = 0; setPadding(0, 0, 0, 0); stateListAnimator = null
    }

    private fun pill(active: Boolean) = GradientDrawable().apply {
        setColor(if (active) Color.rgb(52, 58, 72) else Color.rgb(31, 34, 42))
        cornerRadius = dp(9).toFloat()
        if (active) setStroke(dp(1), Color.rgb(90, 98, 116))
    }

    private fun shortTitle(title: String): String {
        val t = title.replace("METMC ", "").replace("T3 Private ", "")
        return if (t.length > 14) t.take(13) + "…" else t
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
        clock.text = "${timeFormat.format(Date())}\n${dateFormat.format(Date())}"
        handler.postDelayed({ updateClock() }, 1000)
    }

    override fun onDetachedFromWindow() { handler.removeCallbacksAndMessages(null); super.onDetachedFromWindow() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
