package com.metmc.os.linux

import android.content.Context
import java.io.File

class LinuxBridge(private val context: Context) {

    private val sharedStorage = File("/data/media/0/METMC_OS")

    fun isSharedStorageAvailable(): Boolean {
        return sharedStorage.exists() && sharedStorage.isDirectory
    }

    fun projectPath(): String {
        return sharedStorage.absolutePath
    }

    fun linuxRuntimePath(): File {
        return File("/data/local/linux/rootfs")
    }
}
