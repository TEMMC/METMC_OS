package com.metmc.os.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max

class DesktopWindow(
    context: Context,
    private val title: String,
    private val content: View,
    private val workspace: DesktopWorkspace,
    private val onClose: () -> Unit = {}
) : FrameLayout(context) {

    private var maximized = false
    private var normalX = 0f
    private var normalY = 0f
    private var normalWidth = 0
    private var normalHeight = 0
    private val minimumWidth = dp(280)
    private val minimumHeight = dp(180)
    private val resizeHandleSize = dp(20)

    private enum class ResizeDirection { LEFT, RIGHT, TOP, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    fun windowTitle(): String = title

    init {
        elevation = dp(12).toFloat()
        clipChildren = true
        background = GradientDrawable().apply {
            setColor(Color.rgb(30, 32, 40))
            cornerRadius = dp(10).toFloat()
        }
        createWindowLayout()
        createResizeHandles()
        setOnClickListener { workspace.focusWindow(this) }
    }

    private fun createWindowLayout() {
        val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        addView(root, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        root.addView(createTitleBar(), LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(44)))
        root.addView(DesktopScrollableContent(context, content), LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun createTitleBar(): View {
        val titleBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.rgb(42, 45, 55))
        }
        val titleText = TextView(context).apply {
            text = title
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(8), 0)
        }
        titleBar.addView(titleText, LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        val minimize = createButton("—")
        val maximize = createButton("□")
        val close = createButton("×")
        titleBar.addView(minimize); titleBar.addView(maximize); titleBar.addView(close)
        minimize.setOnClickListener { workspace.minimizeWindow(this) }
        maximize.setOnClickListener { toggleMaximize() }
        close.setOnClickListener { workspace.removeWindow(this); onClose() }
        titleBar.setOnTouchListener(object : View.OnTouchListener {
            private var downRawX = 0f; private var downRawY = 0f; private var startX = 0f; private var startY = 0f
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { downRawX = event.rawX; downRawY = event.rawY; startX = x; startY = y; bringToFront(); return true }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downRawX; val dy = event.rawY - downRawY
                        x = (startX + dx).coerceIn(0f, (workspace.width - width).coerceAtLeast(0).toFloat())
                        y = (startY + dy).coerceIn(0f, (workspace.height - height).coerceAtLeast(0).toFloat())
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { clampToWorkspace(); return true }
                }
                return true
            }
        })
        return titleBar
    }

    private fun createButton(text: String): Button = Button(context).apply {
        this.text = text; textSize = 15f; setTextColor(Color.WHITE); setPadding(0, 0, 0, 0); minWidth = dp(42); minHeight = dp(42)
        background = GradientDrawable().apply { setColor(Color.TRANSPARENT) }
    }

    private fun createResizeHandles() {
        val directions = arrayOf(ResizeDirection.LEFT, ResizeDirection.RIGHT, ResizeDirection.TOP, ResizeDirection.BOTTOM, ResizeDirection.TOP_LEFT, ResizeDirection.TOP_RIGHT, ResizeDirection.BOTTOM_LEFT, ResizeDirection.BOTTOM_RIGHT)
        for (direction in directions) {
            val handle = View(context)
            handle.setOnTouchListener(ResizeTouchListener(direction))
            val lp = FrameLayout.LayoutParams(resizeHandleSize, resizeHandleSize)
            when (direction) {
                ResizeDirection.LEFT -> { lp.leftMargin = 0; lp.topMargin = dp(44); lp.height = heightOr(resizeHandleSize) }
                ResizeDirection.RIGHT -> lp.gravity = Gravity.RIGHT
                ResizeDirection.TOP -> lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                ResizeDirection.BOTTOM -> lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                ResizeDirection.TOP_LEFT -> lp.gravity = Gravity.TOP or Gravity.LEFT
                ResizeDirection.TOP_RIGHT -> lp.gravity = Gravity.TOP or Gravity.RIGHT
                ResizeDirection.BOTTOM_LEFT -> lp.gravity = Gravity.BOTTOM or Gravity.LEFT
                ResizeDirection.BOTTOM_RIGHT -> lp.gravity = Gravity.BOTTOM or Gravity.RIGHT
            }
            addView(handle, lp)
        }
    }

    private fun heightOr(fallback: Int): Int = fallback

    private inner class ResizeTouchListener(private val direction: ResizeDirection) : View.OnTouchListener {
        private var downX = 0f; private var downY = 0f; private var startW = 0; private var startH = 0; private var startLeft = 0f; private var startTop = 0f
        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; startW = width; startH = height; startLeft = x; startTop = y; bringToFront(); return true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX; val dy = event.rawY - downY
                    var newW = startW; var newH = startH; var newX = startLeft; var newY = startTop
                    if (direction.name.contains("RIGHT")) newW = (startW + dx).toInt().coerceAtLeast(minimumWidth)
                    if (direction.name.contains("LEFT")) { newW = (startW - dx).toInt().coerceAtLeast(minimumWidth); newX = startLeft + dx }
                    if (direction.name.contains("BOTTOM")) newH = (startH + dy).toInt().coerceAtLeast(minimumHeight)
                    if (direction.name.contains("TOP")) { newH = (startH - dy).toInt().coerceAtLeast(minimumHeight); newY = startTop + dy }
                    layoutParams = layoutParams.apply { width = newW.coerceAtMost(workspace.width); height = newH.coerceAtMost(workspace.height) }
                    x = newX.coerceAtLeast(0f); y = newY.coerceAtLeast(0f); return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { clampToWorkspace(); return true }
            }
            return true
        }
    }

    private fun toggleMaximize() {
        if (!maximized) {
            normalX = x; normalY = y; normalWidth = width; normalHeight = height
            x = 0f; y = 0f
            layoutParams = layoutParams.apply { width = workspace.width; height = workspace.height }
            maximized = true
        } else {
            x = normalX; y = normalY
            layoutParams = layoutParams.apply { width = normalWidth; height = normalHeight }
            maximized = false
        }
        requestLayout()
    }

    private fun clampToWorkspace() {
        x = x.coerceIn(0f, (workspace.width - width).coerceAtLeast(0).toFloat())
        y = y.coerceIn(0f, (workspace.height - height).coerceAtLeast(0).toFloat())
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
