package com.metmc.os.desktop

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.ScrollView

class DesktopScrollableContent(
    context: Context,
    content: View
) : ScrollView(context) {

    init {
        isFillViewport = false
        isVerticalScrollBarEnabled = true
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS

        addView(
            FrameLayout(context).apply {
                addView(
                    content,
                    FrameLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT
                    )
                )
            },
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )
    }
}
