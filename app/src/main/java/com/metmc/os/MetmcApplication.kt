package com.metmc.os

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import kotlin.math.roundToInt

class MetmcApplication : Application() {
    override fun attachBaseContext(base: Context) {
        val c = Configuration(base.resources.configuration)
        val m = base.resources.displayMetrics
        val s = 0.80f
        c.densityDpi = (m.densityDpi * s).roundToInt().coerceAtLeast(120)
        c.fontScale = (c.fontScale * s).coerceAtLeast(0.70f)
        super.attachBaseContext(base.createConfigurationContext(c))
    }
}
