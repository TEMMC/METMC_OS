package com.metmc.os.windows

import android.content.Context

class WindowManager(private val context: Context) {

    data class MetmcWindow(
        val id: String,
        val title: String,
        var minimized: Boolean = false,
        var maximized: Boolean = false
    )

    private val windows = mutableListOf<MetmcWindow>()

    fun createWindow(id: String, title: String): MetmcWindow {
        val window = MetmcWindow(id, title)
        windows.add(window)
        return window
    }

    fun closeWindow(id: String) {
        windows.removeAll { it.id == id }
    }

    fun listWindows(): List<MetmcWindow> = windows.toList()
}
