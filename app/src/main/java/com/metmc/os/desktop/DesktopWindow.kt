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
    private val workspace: DesktopWorkspace,
    private val onClose: () -> Unit = {}
) : LinearLayout(context) {

    private var maximized = false

    private var normalX = 0f
    private var normalY = 0f

    private var normalWidth = 0
    private var normalHeight = 0

    init {

        orientation =
            VERTICAL

        elevation =
            dp(12).toFloat()

        clipChildren =
            true

        background =
            GradientDrawable().apply {

                setColor(
                    Color.rgb(
                        30,
                        32,
                        40
                    )
                )

                cornerRadius =
                    dp(10).toFloat()
            }

        createTitleBar()

        addView(
            content,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setOnClickListener {
            workspace.focusWindow(this)
        }
    }

    private fun createTitleBar() {

        val titleBar =
            LinearLayout(context)

        titleBar.orientation =
            HORIZONTAL

        titleBar.gravity =
            Gravity.CENTER_VERTICAL

        titleBar.setBackgroundColor(
            Color.rgb(
                42,
                45,
                55
            )
        )

        val titleText =
            TextView(context)

        titleText.text =
            title

        titleText.textSize =
            14f

        titleText.setTextColor(
            Color.WHITE
        )

        titleText.gravity =
            Gravity.CENTER_VERTICAL

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

        val minimize =
            createButton("—")

        val maximize =
            createButton("□")

        val close =
            createButton("×")

        titleBar.addView(
            minimize
        )

        titleBar.addView(
            maximize
        )

        titleBar.addView(
            close
        )

        minimize.setOnClickListener {

            workspace.minimizeWindow(
                this
            )
        }

        maximize.setOnClickListener {

            toggleMaximize()
        }

        close.setOnClickListener {

            workspace.removeWindow(
                this
            )

            onClose()
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

            this.text =
                text

            textSize =
                16f

            setTextColor(
                Color.WHITE
            )

            setAllCaps(false)

            minWidth =
                0

            minimumWidth =
                0

            setPadding(
                0,
                0,
                0,
                0
            )

            layoutParams =
                LinearLayout.LayoutParams(
                    dp(48),
                    dp(44)
                )
        }
    }

    private fun toggleMaximize() {

        val params =
            layoutParams
                as? FrameLayout.LayoutParams
                ?: return

        if (!maximized) {

            normalX =
                x

            normalY =
                y

            normalWidth =
                params.width

            normalHeight =
                params.height

            params.width =
                FrameLayout.LayoutParams.MATCH_PARENT

            params.height =
                FrameLayout.LayoutParams.MATCH_PARENT

            params.leftMargin =
                0

            params.topMargin =
                0

            layoutParams =
                params

            x =
                0f

            y =
                0f

            maximized =
                true

        } else {

            params.width =
                normalWidth

            params.height =
                normalHeight

            layoutParams =
                params

            x =
                normalX

            y =
                normalY

            maximized =
                false
        }

        workspace.focusWindow(
            this
        )
    }

    private inner class DragListener :
        OnTouchListener {

        private var downX =
            0f

        private var downY =
            0f

        private var startX =
            0f

        private var startY =
            0f

        override fun onTouch(
            view: View,
            event: MotionEvent
        ): Boolean {

            if (maximized) {
                return false
            }

            when (
                event.actionMasked
            ) {

                MotionEvent.ACTION_DOWN -> {

                    downX =
                        event.rawX

                    downY =
                        event.rawY

                    startX =
                        x

                    startY =
                        y

                    workspace.focusWindow(
                        this@DesktopWindow
                    )

                    return true
                }

                MotionEvent.ACTION_MOVE -> {

                    x =
                        startX +
                        event.rawX -
                        downX

                    y =
                        startY +
                        event.rawY -
                        downY

                    return true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    return true
                }
            }

            return false
        }
    }

    fun restore() {

        visibility =
            View.VISIBLE

        workspace.focusWindow(
            this
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
