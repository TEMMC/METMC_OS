package com.metmc.os.files

object MetmcFileCapabilities {

    fun supportedArchiveTools(): List<String> {
        return MetmcArchiveManager.supportedTools()
    }

    fun canExtract(): Boolean {
        return supportedArchiveTools().any {
            it == "7z" || it == "bsdtar" || it == "tar" || it == "unzip"
        }
    }

    fun canCreateZip(): Boolean {
        return supportedArchiveTools().contains("zip")
    }

    fun canCreateTar(): Boolean {
        return supportedArchiveTools().contains("tar")
    }
}
