package com.metmc.os.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File
import java.util.Locale

class MetmcMediaPlayerView(private val context: Context) : LinearLayout(context) {
    private var player: android.media.MediaPlayer? = null
    private val title = TextView(context)
    private val status = TextView(context)
    private val seek = SeekBar(context)
    private val video = VideoView(context)
    private val play = Button(context)
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val mediaExt = setOf("mp3","m4a","aac","wav","ogg","flac","mp4","mkv","webm","3gp","avi","mov")
    private val videoExt = setOf("mp4","mkv","webm","3gp","avi","mov")

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(12, 14, 18))
        build()
        setOnDragListener(dragListener())
    }

    private fun build() {
        val top = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(10), dp(8))
            setBackgroundColor(Color.rgb(22, 25, 31))
        }
        top.addView(TextView(context).apply {
            text = "♫"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(42), dp(46)))
        top.addView(TextView(context).apply {
            text = "METMC Player"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(0, dp(46), 1f))
        top.addView(iconButton("＋") { openPicker() }, buttonParams(50))
        top.addView(iconButton("☷") { scanLibrary() }, buttonParams(50))
        addView(top, LinearLayout.LayoutParams(-1, dp(62)))

        val stage = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
            setOnDragListener(dragListener())
        }
        video.setBackgroundColor(Color.BLACK)
        video.visibility = GONE
        stage.addView(video, FrameLayout.LayoutParams(-1, -1))
        val drop = TextView(context).apply {
            text = "DROP MEDIA HERE"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(180, 185, 195))
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        stage.addView(drop, FrameLayout.LayoutParams(-1, -1))
        stage.tag = drop
        addView(stage, LinearLayout.LayoutParams(-1, 0, 1f))

        title.apply {
            text = ""
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            maxLines = 1
        }
        addView(title, LinearLayout.LayoutParams(-1, dp(42)))
        status.apply {
            text = ""
            textSize = 11f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        addView(status, LinearLayout.LayoutParams(-1, dp(22)))
        seek.max = 1000
        addView(seek, LinearLayout.LayoutParams(-1, dp(34)))

        val controls = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(4), dp(10), dp(10))
        }
        controls.addView(iconButton("↶") { seekBy(-10000) }, controlParams())
        play.apply {
            text = "▶"
            textSize = 18f
            isAllCaps = false
            setTextColor(Color.WHITE)
            minWidth = 0
            minimumWidth = 0
            setOnClickListener { toggle() }
        }
        controls.addView(play, LinearLayout.LayoutParams(0, dp(48), 1f))
        controls.addView(iconButton("■") { stop() }, controlParams())
        controls.addView(iconButton("↷") { seekBy(10000) }, controlParams())
        addView(controls, LinearLayout.LayoutParams(-1, dp(64)))

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                if (fromUser) player?.let { if (it.duration > 0) it.seekTo(it.duration * p / 1000) }
            }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {}
        })
    }

    private fun dragListener() = OnDragListener { _, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> event.clipDescription?.hasMimeType("*/*") == true || event.clipDescription?.hasMimeType("text/uri-list") == true
            DragEvent.ACTION_DROP -> {
                val uri = event.clipData?.getItemAt(0)?.uri
                if (uri != null) openUri(uri) else false
            }
            else -> true
        }
    }

    private fun iconButton(label: String, action: () -> Unit) = Button(context).apply {
        text = label
        textSize = 17f
        isAllCaps = false
        setTextColor(Color.WHITE)
        minWidth = 0
        minimumWidth = 0
        setPadding(0, 0, 0, 0)
        setOnClickListener { action() }
    }

    private fun buttonParams(w: Int) = LinearLayout.LayoutParams(dp(w), dp(46)).apply { marginStart = dp(3); marginEnd = dp(3) }
    private fun controlParams() = LinearLayout.LayoutParams(dp(58), dp(48)).apply { marginStart = dp(3); marginEnd = dp(3) }

    private fun openPicker() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/*"))
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try { (context as Activity).startActivityForResult(i, 9010) }
        catch (_: Exception) { Toast.makeText(context, "File picker unavailable", Toast.LENGTH_SHORT).show() }
    }

    fun openUri(uri: Uri) {
        try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
        release()
        title.text = uri.lastPathSegment?.substringAfterLast('/') ?: "Media"
        video.visibility = GONE
        setDropVisible(false)
        try {
            val m = android.media.MediaPlayer()
            m.setDataSource(context, uri)
            player = m
            m.setOnPreparedListener { p -> status.text = format(p.duration); play.text = "❚❚"; p.start(); updateProgress() }
            m.setOnCompletionListener { play.text = "▶"; seek.progress = 0 }
            m.setOnErrorListener { _, _, _ -> status.text = "Unsupported media"; true }
            m.prepareAsync()
        } catch (_: Exception) { status.text = "Cannot open media" }
    }

    private fun scanLibrary() {
        status.text = "Scanning…"
        Thread {
            val roots = arrayListOf("/storage/emulated/0")
            try {
                val p = ProcessBuilder("su", "-c", "find /storage -mindepth 1 -maxdepth 1 -type d 2>/dev/null").redirectErrorStream(true).start()
                roots.addAll(p.inputStream.bufferedReader().readLines().filter { it != "/storage/emulated" && it != "/storage/self" }); p.waitFor()
            } catch (_: Exception) {}
            val files = ArrayList<File>()
            fun walk(f: File, depth: Int) {
                if (depth > 6 || files.size >= 500) return
                (try { f.listFiles() } catch (_: Exception) { null })?.forEach { c -> if (c.isDirectory) walk(c, depth + 1) else if (c.extension.lowercase(Locale.ROOT) in mediaExt) files.add(c) }
            }
            roots.distinct().forEach { walk(File(it), 0) }
            (context as? Activity)?.runOnUiThread { showLibrary(files.distinctBy { it.absolutePath }.sortedBy { it.name.lowercase(Locale.ROOT) }) }
        }.start()
    }

    private fun showLibrary(files: List<File>) {
        val box = LinearLayout(context).apply { orientation = VERTICAL; setPadding(dp(10), dp(8), dp(10), dp(8)) }
        box.addView(TextView(context).apply { text = "Media Library"; textSize = 17f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(dp(6), dp(6), dp(6), dp(10)) })
        val scroll = ScrollView(context)
        val list = LinearLayout(context).apply { orientation = VERTICAL }
        files.forEach { f -> list.addView(iconButton("${if (f.extension.lowercase(Locale.ROOT) in videoExt) "▣" else "♫"}  ${f.name}") { openFile(f) }, LinearLayout.LayoutParams(-1, dp(50))) }
        if (files.isEmpty()) list.addView(TextView(context).apply { text = "No media found"; setTextColor(Color.LTGRAY); setPadding(dp(12), dp(20), dp(12), dp(20)) })
        scroll.addView(list); box.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); AlertDialogCompat.show(context, box)
    }

    private fun openFile(file: File) {
        if (!file.exists()) return
        release(); title.text = file.name; setDropVisible(false)
        if (file.extension.lowercase(Locale.ROOT) in videoExt) {
            video.visibility = VISIBLE; video.setVideoPath(file.absolutePath)
            video.setOnPreparedListener { m -> player = m; status.text = format(m.duration); play.text = "❚❚"; m.start(); updateProgress() }
            video.setOnCompletionListener { play.text = "▶" }
            video.setOnErrorListener { _, _, _ -> status.text = "Unsupported video"; true }
        } else {
            video.visibility = GONE
            try {
                val m = android.media.MediaPlayer(); player = m; m.setDataSource(file.absolutePath)
                m.setOnPreparedListener { p -> status.text = format(p.duration); play.text = "❚❚"; p.start(); updateProgress() }
                m.setOnCompletionListener { play.text = "▶" }
                m.setOnErrorListener { _, _, _ -> status.text = "Unsupported audio"; true }; m.prepareAsync()
            } catch (_: Exception) { status.text = "Cannot open media" }
        }
    }

    private fun setDropVisible(visible: Boolean) { (findViewById<FrameLayout>(android.R.id.content)) }
    private fun toggle() { player?.let { if (it.isPlaying) { it.pause(); play.text = "▶" } else { it.start(); play.text = "❚❚"; updateProgress() } } }
    private fun stop() { player?.let { try { it.pause(); it.seekTo(0) } catch (_: Exception) {} }; play.text = "▶"; seek.progress = 0 }
    private fun seekBy(ms: Int) { player?.let { if (it.duration > 0) it.seekTo((it.currentPosition + ms).coerceIn(0, it.duration)) } }
    private fun updateProgress() { val m = player ?: return; if (!m.isPlaying) return; if (m.duration > 0) seek.progress = m.currentPosition * 1000 / m.duration; handler.postDelayed({ updateProgress() }, 500) }
    private fun release() { try { player?.release() } catch (_: Exception) {}; player = null; video.stopPlayback() }
    override fun onDetachedFromWindow() { handler.removeCallbacksAndMessages(null); release(); super.onDetachedFromWindow() }
    private fun format(ms: Int) = "${ms / 60000}:${String.format(Locale.US, "%02d", (ms / 1000) % 60)}"
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

private object AlertDialogCompat {
    fun show(context: Context, content: View) {
        val d = android.app.Dialog(context)
        d.setContentView(content); d.show()
        d.window?.setLayout((minOf(720, context.resources.displayMetrics.widthPixels - 32)), (minOf(520, context.resources.displayMetrics.heightPixels - 80)))
    }
}
