package com.metmc.os.files

import java.io.File

object MetmcArchiveManager {

    private val extractors = listOf(
        "7z", "bsdtar", "tar", "unzip"
    )

    private fun commandExists(command: String): Boolean {
        return try {
            val process = ProcessBuilder("sh", "-c", "command -v $command")
                .redirectErrorStream(true)
                .start()
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    private fun run(command: String): Boolean {
        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    fun extract(archive: File, destination: File): Boolean {
        if (!archive.exists()) return false
        if (!destination.exists() && !destination.mkdirs()) return false

        val input = archive.absolutePath.replace("'", "'\\''")
        val output = destination.absolutePath.replace("'", "'\\''")

        return when {
            commandExists("7z") ->
                run("7z x -y '$input' -o'$output'")

            commandExists("bsdtar") ->
                run("bsdtar -xf '$input' -C '$output'")

            archive.name.endsWith(".zip", true) && commandExists("unzip") ->
                run("unzip -o '$input' -d '$output'")

            archive.name.matches(
                Regex(".*\\.(tar|tar\\.gz|tgz|tar\\.bz2|tbz2|tar\\.xz|txz|tar\\.zst)$", RegexOption.IGNORE_CASE)
            ) && commandExists("tar") ->
                run("tar -xf '$input' -C '$output'")

            else -> false
        }
    }

    fun createTar(source: File, output: File): Boolean {
        if (!source.exists()) return false
        if (!commandExists("tar")) return false

        val src = source.absolutePath.replace("'", "'\\''")
        val out = output.absolutePath.replace("'", "'\\''")

        return run("tar -cf '$out' -C '${source.parent}' '${source.name}'")
    }

    fun createZip(source: File, output: File): Boolean {
        if (!source.exists()) return false
        if (!commandExists("zip")) return false

        val src = source.absolutePath.replace("'", "'\\''")
        val out = output.absolutePath.replace("'", "'\\''")

        return run("zip -r '$out' '$src'")
    }

    fun supportedTools(): List<String> {
        return extractors.filter { commandExists(it) } +
            listOf("tar", "zip", "gzip", "bzip2", "xz", "zstd", "cpio")
                .filter { commandExists(it) }
                .distinct()
    }
}
