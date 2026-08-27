package com.metmc.os.desktop

import android.content.Context
import android.graphics.Color
import android.widget.FrameLayout

class DesktopWorkspace(context: Context) : FrameLayout(context) {

    init {
        setBackgroundColor(Color.TRANSPARENT)
        isClickable = true
        isFocusable = true
        clipChildren = false
        clipToPadding = false
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
}
