package com.metmc.os.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.widget.FrameLayout

class DesktopWallpaper(
    context: Context
) : FrameLayout(context) {

    private var currentWallpaper: Uri? = null

    init {
        setDefaultWallpaper()
    }

    fun setDefaultWallpaper() {
        currentWallpaper = null

        val background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.rgb(12, 18, 32),
                Color.rgb(24, 42, 70),
                Color.rgb(18, 22, 35)
            )
        )

        background.cornerRadius = 0f
        this.background = background
    }

    fun setWallpaper(uri: Uri) {
        currentWallpaper = uri

        try {
            val stream =
                context.contentResolver.openInputStream(uri)

            val drawable =
                android.graphics.drawable.Drawable
                    .createFromStream(
                        stream,
                        uri.toString()
                    )

            stream?.close()

            if (drawable != null) {
                background = drawable
            }

        } catch (e: Exception) {
            setDefaultWallpaper()
        }
    }

    fun getWallpaper(): Uri? {
        return currentWallpaper
    }
}
