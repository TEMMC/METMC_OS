package com.metmc.os.media

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MetmcMediaPlayerView(private val context: android.content.Context) : LinearLayout(context) {
    private var player: MediaPlayer? = null
    private var currentUri: Uri? = null
    private val title = TextView(context)
    private val status = TextView(context)
    private val seek = SeekBar(context)
    private val video = VideoView(context)
    private val play = Button(context)
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(10, 12, 17))
        val top = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(6), dp(8), dp(6)); setBackgroundColor(Color.rgb(25,29,37)) }
        top.addView(TextView(context).apply { text = "METMC Media Player"; textSize = 16f; setTextColor(Color.WHITE); gravity = Gravity.CENTER_VERTICAL }, LinearLayout.LayoutParams(0, dp(48), 1f))
        val open = Button(context).apply { text = "Open"; isAllCaps = false; setOnClickListener { choose() } }
        top.addView(open, LinearLayout.LayoutParams(dp(82), dp(44)))
        addView(top, LayoutParams(-1, dp(60)))

        video.setBackgroundColor(Color.BLACK)
        video.visibility = GONE
        addView(video, LinearLayout.LayoutParams(-1, 0, 1f))

        title.text = "No media selected"; title.textSize = 17f; title.setTextColor(Color.WHITE); title.gravity = Gravity.CENTER; title.setPadding(dp(12), dp(10), dp(12), dp(4))
        addView(title, LayoutParams(-1, dp(48)))
        status.text = "Choose an audio or video file"; status.textSize = 12f; status.setTextColor(Color.LTGRAY); status.gravity = Gravity.CENTER
        addView(status, LayoutParams(-1, dp(28)))
        seek.max = 1000; seek.progress = 0; addView(seek, LayoutParams(-1, dp(40)))

        val controls = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(6), dp(4), dp(6), dp(8)) }
        val back = control("↶") { seekBy(-10000) }
        play.text = "▶ Play"; play.isAllCaps = false; play.setOnClickListener { toggle() }
        val stop = control("■") { stop() }
        val forward = control("↷") { seekBy(10000) }
        controls.addView(back, buttonParams()); controls.addView(play, LinearLayout.LayoutParams(0, dp(46), 1f)); controls.addView(stop, buttonParams()); controls.addView(forward, buttonParams())
        addView(controls, LayoutParams(-1, dp(62)))
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener { override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) { if (fromUser) player?.let { it.seekTo((it.duration * p) / 1000) } }; override fun onStartTrackingTouch(s: SeekBar) {}; override fun onStopTrackingTouch(s: SeekBar) {} })
    }

    private fun choose() {
        val a = context as? Activity ?: return
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "*/*"; putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/*")); addCategory(Intent.CATEGORY_OPENABLE); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) }
        a.startActivityForResult(i, 9311)
    }

    fun open(uri: Uri) { currentUri = uri; title.text = displayName(uri); status.text = "Loading…"; release();
        try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
        val mime = context.contentResolver.getType(uri).orEmpty()
        if (mime.startsWith("video/")) { video.visibility = VISIBLE; video.setVideoURI(uri); video.setOnPreparedListener { mp -> player = mp; status.text = format(mp.duration); play.text = "❚❚ Pause"; mp.start(); updateProgress() }; video.setOnCompletionListener { play.text = "▶ Play" } }
        else { video.visibility = GONE; player = MediaPlayer(); player!!.setDataSource(context, uri); player!!.setOnPreparedListener { mp -> status.text = format(mp.duration); play.text = "❚❚ Pause"; mp.start(); updateProgress() }; player!!.setOnCompletionListener { play.text = "▶ Play" }; player!!.prepareAsync() }
    }
    private fun toggle() { val p = player ?: return; if (p.isPlaying) { p.pause(); play.text = "▶ Play" } else { p.start(); play.text = "❚❚ Pause"; updateProgress() } }
    private fun stop() { player?.let { try { it.pause(); it.seekTo(0) } catch (_: Exception) {} }; play.text = "▶ Play"; seek.progress = 0 }
    private fun seekBy(ms: Int) { player?.let { val n = (it.currentPosition + ms).coerceIn(0, it.duration); it.seekTo(n) } }
    private fun updateProgress() { val p = player ?: return; if (!p.isPlaying) return; if (p.duration > 0) seek.progress = (p.currentPosition * 1000 / p.duration); handler.postDelayed({ updateProgress() }, 500) }
    private fun release() { try { player?.release() } catch (_: Exception) {}; player = null; video.stopPlayback() }
    override fun onDetachedFromWindow() { handler.removeCallbacksAndMessages(null); release(); super.onDetachedFromWindow() }
    private fun displayName(uri: Uri): String { var n: String? = null; try { context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) n = it.getString(0) } } catch (_: Exception) {}; return n ?: (uri.lastPathSegment ?: "Media") }
    private fun format(ms: Int): String { val s = ms / 1000; return "${s / 60}:${String.format("%02d", s % 60)}" }
    private fun control(t: String, action: () -> Unit) = Button(context).apply { text = t; textSize = 18f; isAllCaps = false; setOnClickListener { action() } }
    private fun buttonParams() = LinearLayout.LayoutParams(dp(60), dp(46)).apply { marginStart = dp(3); marginEnd = dp(3) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
