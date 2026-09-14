package com.metmc.os.desktop

import android.content.Context
import android.graphics.Color
import android.widget.FrameLayout
import android.view.View

class DesktopWorkspace(context: Context) : FrameLayout(context) {

    init {
        setBackgroundColor(Color.TRANSPARENT)
        isClickable = true
        isFocusable = true
        clipChildren = true
        clipToPadding = true
    }

    fun addWindow(
        window: DesktopWindow,
        params: FrameLayout.LayoutParams
    ) {
        if (window.parent != null) {
            (window.parent as? FrameLayout)?.removeView(window)
        }

        addView(window, params)
        window.bringToFront()
    }

    fun removeWindow(window: DesktopWindow) {
        removeView(window)
    }

    fun focusWindow(window: DesktopWindow) {
        if (window.parent === this) {
            window.visibility = VISIBLE
            window.bringToFront()
        }
    }

    fun minimizeWindow(window: DesktopWindow) {
        if (window.parent === this) {
            window.visibility = GONE
        }
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int
    ) {
        super.onSizeChanged(w, h, oldw, oldh)

        post {
            MetmcWindowBounds.constrainAllChildren(
                this,
                findTaskbarView()
            )
        }
    }

    private fun findTaskbarView(): View? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val name = child.tag?.toString()?.lowercase() ?: ""

            if (name.contains("taskbar"))
                return child

            val idName = try {
                resources.getResourceEntryName(child.id).lowercase()
            } catch (_: Exception) {
                ""
            }

            if (idName.contains("taskbar"))
                return child
        }

        return null
    }

}
