package com.metmc.os.integration

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import com.metmc.os.settings.MetmcDisplaySettings
import com.metmc.os.wallpaper.MetmcWallpaper

object MetmcSystemIntegration {

    fun applyActivityRules(activity: Activity) {
        MetmcDisplaySettings.applyOrientation(activity)
    }

    fun applyWallpaper(context: Context, target: View) {
        if (target is android.widget.ImageView) {
            MetmcWallpaper.apply(context, target)
        }
    }

    fun constrainChildren(parent: ViewGroup, taskbar: View? = null) {
        try {
            val boundsClass =
                Class.forName("com.metmc.os.desktop.MetmcWindowBounds")

            val method = boundsClass.getMethod(
                "constrainAllChildren",
                ViewGroup::class.java,
                View::class.java
            )

            method.invoke(null, parent, taskbar)
        } catch (_: Throwable) {
            // Window-boundary support is optional for activities that
            // do not expose the METMC desktop workspace.
        }
    }

    fun setPortrait(context: Context, enabled: Boolean) {
        MetmcDisplaySettings.setPortraitEnabled(context, enabled)
    }

    fun isPortrait(context: Context): Boolean {
        return MetmcDisplaySettings.isPortraitEnabled(context)
    }
}
