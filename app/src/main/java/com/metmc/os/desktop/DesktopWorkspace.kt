package com.metmc.os.desktop

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout

class DesktopWorkspace(
    context: Context
) : FrameLayout(context) {

    init {
        setBackgroundColor(
            Color.rgb(10, 12, 18)
        )

        isClickable = true
        isFocusable = true
    }

    fun addWindow(
        window: View,
        params: FrameLayout.LayoutParams
    ) {
        addView(window, params)
        window.bringToFront()
    }

    fun removeWindow(
        window: View
    ) {
        removeView(window)
    }

    fun focusWindow(
        window: View
    ) {
        if (window.parent == this &&
            window.visibility == View.VISIBLE) {

            window.bringToFront()
        }
    }
}
