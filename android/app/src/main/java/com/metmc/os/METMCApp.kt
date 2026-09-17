package com.metmc.os

import android.app.Application
import android.util.Log

/**
 * METMC OS Application class.
 *
 * Initializes the bridge service infrastructure and chroot runtime
 * at application startup.
 */
class METMCApp : Application() {

    companion object {
        const val TAG = "METMC OS"

        @Volatile
        lateinit var instance: METMCApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "METMC OS application initialized")
    }
}
