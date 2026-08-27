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

class DesktopWindow(
    context: Context,
    private val title: String,
    private val content: View,
    private val workspace: DesktopWorkspace
) : LinearLayout(context) {

    private var maximized = false

    private var normalX = 0f
    private var normalY = 0f
    private var normalWidth = 0
    private var normalHeight = 0

    init {
        orientation = VERTICAL
        elevation = dp(12).toFloat()

        val background = GradientDrawable()
        background.setColor(Color.rgb(30, 32, 40))
        background.cornerRadius = dp(10).toFloat()
        this.background = background

        createTitleBar()

        addView(
            content,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
    }

    private fun createTitleBar() {

        val titleBar = LinearLayout(context)

        titleBar.orientation = HORIZONTAL
        titleBar.gravity = Gravity.CENTER_VERTICAL

        titleBar.setBackgroundColor(
            Color.rgb(42, 45, 55)
        )

        val titleText = TextView(context)
        titleText.text = title
        titleText.textSize = 14f
        titleText.setTextColor(Color.WHITE)

        titleText.setPadding(
            dp(14),
            0,
            dp(8),
            0
        )

        titleBar.addView(
            titleText,
            LinearLayout.LayoutParams(
                0,
                dp(44),
                1f
            )
        )

        val minimize = createButton("—")
        val maximize = createButton("□")
        val close = createButton("×")

        titleBar.addView(minimize)
        titleBar.addView(maximize)
        titleBar.addView(close)

        minimize.setOnClickListener {
            visibility = View.GONE
        }

        maximize.setOnClickListener {
            toggleMaximize()
        }

        close.setOnClickListener {
            workspace.removeWindow(this)
        }

        titleBar.setOnTouchListener(
            DragListener()
        )

        addView(
            titleBar,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(44)
            )
        )
    }

    private fun createButton(
        text: String
    ): Button {

        return Button(context).apply {

            this.text = text
            textSize = 16f
            setTextColor(Color.WHITE)
            setAllCaps(false)

            layoutParams =
                LinearLayout.LayoutParams(
                    dp(48),
                    dp(44)
                )
        }
    }

    private fun toggleMaximize() {

        val params =
            layoutParams as? FrameLayout.LayoutParams
                ?: return

        if (!maximized) {

            normalX = x
            normalY = y
            normalWidth = params.width
            normalHeight = params.height

            params.width =
                FrameLayout.LayoutParams.MATCH_PARENT

            params.height =
                workspace.height

            params.leftMargin = 0
            params.topMargin = 0

            layoutParams = params

            maximized = true

        } else {

            params.width = normalWidth
            params.height = normalHeight

            layoutParams = params

            x = normalX
            y = normalY

            maximized = false
        }

        bringToFront()
    }

    private inner class DragListener :
        View.OnTouchListener {

        private var downX = 0f
        private var downY = 0f
        private var startX = 0f
        private var startY = 0f

        override fun onTouch(
            view: View,
            event: MotionEvent
        ): Boolean {

            if (maximized) return false

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {

                    downX = event.rawX
                    downY = event.rawY

                    startX = x
                    startY = y

                    bringToFront()

                    return true
                }

                MotionEvent.ACTION_MOVE -> {

                    x =
                        startX +
                        event.rawX - downX

                    y =
                        startY +
                        event.rawY - downY

                    return true
                }

                MotionEvent.ACTION_UP -> {
                    return true
                }
            }

            return false
        }
    }

    fun restore() {
        visibility = View.VISIBLE
        bringToFront()
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
            resources.displayMetrics.density
        ).toInt()
    }
}
