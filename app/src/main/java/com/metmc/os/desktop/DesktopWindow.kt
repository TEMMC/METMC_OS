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

    private enum class ResizeDirection {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    init {
        elevation = dp(12).toFloat()

        clipChildren = true

        background = GradientDrawable().apply {
            setColor(
                Color.rgb(
                    30,
                    32,
                    40
                )
            )

            cornerRadius = dp(10).toFloat()
        }

        createWindowLayout()
        createResizeHandles()

        setOnClickListener {
            workspace.focusWindow(this)
        }
    }

    private fun createWindowLayout() {

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        addView(
            root,
            FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )

        val titleBar = createTitleBar()

        root.addView(
            titleBar,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(44)
            )
        )

        root.addView(
            DesktopScrollableContent(
                context,
                content
            ),
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
    }

    private fun createTitleBar(): View {

        val titleBar = LinearLayout(context)

        titleBar.orientation = LinearLayout.HORIZONTAL

        titleBar.gravity = Gravity.CENTER_VERTICAL

        titleBar.setBackgroundColor(
            Color.rgb(
                42,
                45,
                55
            )
        )

        val titleText = TextView(context)

        titleText.text = title

        titleText.textSize = 14f

        titleText.setTextColor(Color.WHITE)

        titleText.gravity = Gravity.CENTER_VERTICAL

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
                LayoutParams.MATCH_PARENT,
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
            workspace.minimizeWindow(this)
        }

        maximize.setOnClickListener {
            toggleMaximize()
        }

        close.setOnClickListener {
            workspace.removeWindow(this)
            onClose()
        }

        titleBar.setOnTouchListener(object : View.OnTouchListener {
            private var downRawX = 0f
            private var downRawY = 0f
            private var startX = 0f
            private var startY = 0f

            override fun onTouch(
                v: View?,
                event: MotionEvent
            ): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downRawX = event.rawX
                        downRawY = event.rawY
                        startX = x
                        startY = y
                        bringToFront()
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downRawX
                        val dy = event.rawY - downRawY

                        x = (startX + dx).coerceIn(
                            0f,
                            (workspace.width - width)
                                .coerceAtLeast(0)
                                .toFloat()
                        )

                        y = (startY + dy).coerceIn(
                            0f,
                            (workspace.height - height)
                                .coerceAtLeast(0)
                                .toFloat()
                        )

                        return true
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        clampToWorkspace()
                        return true
                    }
                }

                return true
            }
        })

        return titleBar
    }

    private fun createButton(
        text: String
    ): Button {

        return Button(context).apply {

            this.text = text

            textSize = 16f

            setTextColor(Color.WHITE)

            setAllCaps(false)

            minWidth = 0
            minimumWidth = 0

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

    private fun createResizeHandles() {

        addResizeHandle(
            ResizeDirection.TOP_LEFT,
            Gravity.TOP or Gravity.START
        )

        addResizeHandle(
            ResizeDirection.TOP,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        )

        addResizeHandle(
            ResizeDirection.TOP_RIGHT,
            Gravity.TOP or Gravity.END
        )

        addResizeHandle(
            ResizeDirection.LEFT,
            Gravity.CENTER_VERTICAL or Gravity.START
        )

        addResizeHandle(
            ResizeDirection.RIGHT,
            Gravity.CENTER_VERTICAL or Gravity.END
        )

        addResizeHandle(
            ResizeDirection.BOTTOM_LEFT,
            Gravity.BOTTOM or Gravity.START
        )

        addResizeHandle(
            ResizeDirection.BOTTOM,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        )

        addResizeHandle(
            ResizeDirection.BOTTOM_RIGHT,
            Gravity.BOTTOM or Gravity.END
        )
    }

    private fun addResizeHandle(
        direction: ResizeDirection,
        gravity: Int
    ) {

        val handle = View(context)

        handle.setBackgroundColor(
            Color.TRANSPARENT
        )

        val width: Int
        val height: Int

        when (direction) {

            ResizeDirection.LEFT,
            ResizeDirection.RIGHT -> {
                width = resizeHandleSize
                height = LayoutParams.MATCH_PARENT
            }

            ResizeDirection.TOP,
            ResizeDirection.BOTTOM -> {
                width = LayoutParams.MATCH_PARENT
                height = resizeHandleSize
            }

            ResizeDirection.TOP_LEFT,
            ResizeDirection.TOP_RIGHT,
            ResizeDirection.BOTTOM_LEFT,
            ResizeDirection.BOTTOM_RIGHT -> {
                width = resizeHandleSize
                height = resizeHandleSize
            }
        }

        val params = FrameLayout.LayoutParams(
            width,
            height
        )

        params.gravity = gravity

        /*
         * Keep the top resize strip below the title bar so
         * it does not block the window controls.
         */
        if (direction == ResizeDirection.TOP) {
            params.topMargin = dp(44)
        }

        addView(
            handle,
            params
        )

        handle.setOnTouchListener(
            ResizeListener(direction)
        )
    }

    private fun toggleMaximize() {

        val params =
            layoutParams
                as? FrameLayout.LayoutParams
                ?: return

        if (!maximized) {

            normalX = x
            normalY = y

            normalWidth = width
            normalHeight = height

            params.width =
                workspace.width.coerceAtLeast(1)

            params.height =
                workspace.height.coerceAtLeast(1)

            params.leftMargin = 0
            params.topMargin = 0

            layoutParams = params

            x = 0f
            y = 0f

            maximized = true

        } else {

            params.width =
                normalWidth.coerceAtLeast(
                    minimumWidth
                )

            params.height =
                normalHeight.coerceAtLeast(
                    minimumHeight
                )

            layoutParams = params

            x = normalX
            y = normalY

            maximized = false

            clampToWorkspace()
        }

        workspace.focusWindow(this)
    }

    private inner class ResizeListener(
        private val direction: ResizeDirection
    ) : OnTouchListener {

        private var downX = 0f
        private var downY = 0f

        private var startX = 0f
        private var startY = 0f

        private var startWidth = 0
        private var startHeight = 0

        override fun onTouch(
            view: View,
            event: MotionEvent
        ): Boolean {

            if (maximized) {
                return false
            }

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    downX = event.rawX
                    downY = event.rawY

                    startX = x
                    startY = y

                    startWidth = width
                    startHeight = height

                    workspace.focusWindow(
                        this@DesktopWindow
                    )

                    return true
                }

                MotionEvent.ACTION_MOVE -> {

                    resizeWindow(
                        direction,
                        event.rawX - downX,
                        event.rawY - downY,
                        startX,
                        startY,
                        startWidth,
                        startHeight
                    )

                    return true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    clampToWorkspace()

                    return true
                }
            }

            return false
        }
    }

    private fun resizeWindow(
        direction: ResizeDirection,
        deltaX: Float,
        deltaY: Float,
        startX: Float,
        startY: Float,
        startWidth: Int,
        startHeight: Int
    ) {

        val workspaceWidth =
            workspace.width.coerceAtLeast(1)

        val workspaceHeight =
            workspace.height.coerceAtLeast(1)

        var newX = startX
        var newY = startY

        var newWidth = startWidth
        var newHeight = startHeight

        /*
         * LEFT SIDE
         */
        if (
            direction == ResizeDirection.LEFT ||
            direction == ResizeDirection.TOP_LEFT ||
            direction == ResizeDirection.BOTTOM_LEFT
        ) {

            val proposedX =
                startX + deltaX

            val maximumX =
                startX +
                    startWidth -
                    minimumWidth

            newX =
                proposedX.coerceIn(
                    0f,
                    maximumX.coerceAtLeast(0f)
                )

            newWidth =
                (
                    startWidth +
                        startX -
                        newX
                    ).toInt()
        }

        /*
         * RIGHT SIDE
         */
        if (
            direction == ResizeDirection.RIGHT ||
            direction == ResizeDirection.TOP_RIGHT ||
            direction == ResizeDirection.BOTTOM_RIGHT
        ) {

            newWidth =
                (
                    startWidth +
                        deltaX
                    ).toInt()

            newWidth =
                newWidth.coerceAtLeast(
                    minimumWidth
                )

            val maximumWidth =
                workspaceWidth -
                    newX.toInt()

            newWidth =
                newWidth.coerceAtMost(
                    maximumWidth.coerceAtLeast(
                        minimumWidth
                    )
                )
        }

        /*
         * TOP SIDE
         */
        if (
            direction == ResizeDirection.TOP ||
            direction == ResizeDirection.TOP_LEFT ||
            direction == ResizeDirection.TOP_RIGHT
        ) {

            val proposedY =
                startY + deltaY

            val maximumY =
                startY +
                    startHeight -
                    minimumHeight

            newY =
                proposedY.coerceIn(
                    0f,
                    maximumY.coerceAtLeast(0f)
                )

            newHeight =
                (
                    startHeight +
                        startY -
                        newY
                    ).toInt()
        }

        /*
         * BOTTOM SIDE
         */
        if (
            direction == ResizeDirection.BOTTOM ||
            direction == ResizeDirection.BOTTOM_LEFT ||
            direction == ResizeDirection.BOTTOM_RIGHT
        ) {

            newHeight =
                (
                    startHeight +
                        deltaY
                    ).toInt()

            newHeight =
                newHeight.coerceAtLeast(
                    minimumHeight
                )

            val maximumHeight =
                workspaceHeight -
                    newY.toInt()

            newHeight =
                newHeight.coerceAtMost(
                    maximumHeight.coerceAtLeast(
                        minimumHeight
                    )
                )
        }

        newWidth =
            newWidth.coerceAtLeast(
                minimumWidth
            )

        newHeight =
            newHeight.coerceAtLeast(
                minimumHeight
            )

        val params =
            layoutParams
                as? FrameLayout.LayoutParams
                ?: return

        params.width = newWidth
        params.height = newHeight

        layoutParams = params

        x = newX
        y = newY

        clampToWorkspace()
    }

    private fun clampToWorkspace() {

        post {

            val maxX =
                (
                    workspace.width -
                        width
                    ).coerceAtLeast(0)

            val maxY =
                (
                    workspace.height -
                        height
                    ).coerceAtLeast(0)

            x =
                x.coerceIn(
                    0f,
                    maxX.toFloat()
                )

            y =
                y.coerceIn(
                    0f,
                    maxY.toFloat()
                )
        }
    }

    fun restore() {

        visibility = View.VISIBLE

        workspace.focusWindow(this)
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
