package com.metmc.os.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class DesktopNotifications(
    context: Context
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        gravity = Gravity.TOP
        setPadding(dp(12), dp(12), dp(12), dp(12))
    }

    fun showNotification(
        title: String,
        message: String
    ) {

        val notification = LinearLayout(context)
        notification.orientation = VERTICAL
        notification.setPadding(
            dp(14),
            dp(10),
            dp(14),
            dp(10)
        )

        val background = GradientDrawable()
        background.setColor(Color.rgb(38, 41, 50))
        background.cornerRadius = dp(10).toFloat()

        notification.background = background
        notification.elevation = dp(8).toFloat()

        val titleView = TextView(context)
        titleView.text = title
        titleView.textSize = 15f
        titleView.setTextColor(Color.WHITE)

        val messageView = TextView(context)
        messageView.text = message
        messageView.textSize = 13f
        messageView.setTextColor(Color.LTGRAY)

        notification.addView(titleView)

        notification.addView(
            messageView,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        addView(
            notification,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        )

        notification.postDelayed({
            removeView(notification)
        }, 4000)
    }

    private fun dp(value: Int): Int {
        return (
            value *
            resources.displayMetrics.density
        ).toInt()
    }
}
