package com.metmc.os.office

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.webkit.WebView
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

/**
 * Universal METMC file opener.
 *
 * It provides a single desktop entry point for common documents/media and
 * intelligently delegates formats Android already knows how to render.
 * OOXML documents are also given a lightweight readable-text preview without
 * requiring a large third-party office engine.
 */
class MetmcOfficeView(private val context: Context) : LinearLayout(context) {
    private val title = TextView(context)
    private val body = FrameLayout(context)
    private val status = TextView(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(15, 18, 24))
        buildToolbar()
        body.addView(TextView(context).apply {
            text = "Open documents, PDFs, images, archives, code and media from one place.\n\nChoose Open File to begin."
            setTextColor(Color.LTGRAY)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(28), dp(28), dp(28))
        }, FrameLayout.LayoutParams(-1, -1))
        addView(body, LayoutParams(-1, 0, 1f))
        status.textSize = 11f
        status.setTextColor(Color.rgb(150, 158, 174))
        status.setPadding(dp(12), dp(5), dp(12), dp(5))
        addView(status, LayoutParams(-1, dp(30)))
    }

    private fun buildToolbar() {
        val bar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(5), dp(8), dp(5))
            setBackgroundColor(Color.rgb(27, 32, 42))
        }
        title.text = "METMC Office"
        title.textSize = 19f
        title.setTextColor(Color.WHITE)
        bar.addView(title, LayoutParams(0, dp(46), 1f))
        val open = Button(context).apply {
            text = "Open File"
            isAllCaps = false
            setOnClickListener { chooseFile() }
        }
        bar.addView(open, LayoutParams(dp(105), dp(44)))
        val external = Button(context).apply {
            text = "Open With"
            isAllCaps = false
            setOnClickListener { chooseFile(true) }
        }
        bar.addView(external, LayoutParams(dp(105), dp(44)))
        addView(bar, LayoutParams(-1, dp(56)))
    }

    private fun chooseFile(externalOnly: Boolean = false) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        (context as? Activity)?.startActivityForResult(intent, if (externalOnly) 9021 else 9020)
            ?: context.startActivity(Intent.createChooser(intent, "Open file"))
    }

    /** Called by MainActivity when a file was selected for METMC Office. */
    fun openUri(uri: Uri, externalOnly: Boolean = false) {
        if (externalOnly) {
            launchExternal(uri)
            return
        }
        val name = queryName(uri)
        title.text = name
        status.text = uri.toString()
        val lower = name.lowercase()
        when {
            lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".log") ||
                    lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".csv") ||
                    lower.endsWith(".java") || lower.endsWith(".kt") || lower.endsWith(".py") ||
                    lower.endsWith(".sh") || lower.endsWith(".html") || lower.endsWith(".css") -> previewText(uri)
            lower.endsWith(".docx") || lower.endsWith(".xlsx") || lower.endsWith(".pptx") -> previewOoxml(uri, lower)
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif") -> previewImage(uri)
            lower.endsWith(".pdf") -> launchExternal(uri)
            lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".wav") ||
                    lower.endsWith(".ogg") || lower.endsWith(".flac") || lower.endsWith(".mp4") || lower.endsWith(".mkv") ||
                    lower.endsWith(".webm") || lower.endsWith(".3gp") || lower.endsWith(".avi") || lower.endsWith(".mov") -> launchExternal(uri)
            else -> previewBinaryInfo(uri, name)
        }
    }

    private fun previewText(uri: Uri) {
        Thread {
            val text = try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input)).readText().take(500_000)
                } ?: "Unable to read file."
            } catch (e: Exception) { "Unable to read file: ${e.message}" }
            post { showText(text) }
        }.start()
    }

    private fun previewOoxml(uri: Uri, lowerName: String) {
        Thread {
            val text = try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val zip = ZipInputStream(input)
                    val wanted = when {
                        lowerName.endsWith(".docx") -> "word/document.xml"
                        lowerName.endsWith(".xlsx") -> "xl/sharedStrings.xml"
                        else -> "ppt/slides/slide1.xml"
                    }
                    var found: String? = null
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name == wanted) {
                            found = zip.readBytes().toString(Charsets.UTF_8)
                            break
                        }
                        entry = zip.nextEntry
                    }
                    stripXml(found ?: "No readable text was found in this document.")
                } ?: "Unable to read document."
            } catch (e: Exception) { "Unable to preview Office document: ${e.message}" }
            post { showText(text) }
        }.start()
    }

    private fun previewImage(uri: Uri) {
        body.removeAllViews()
        val image = ImageView(context).apply {
            setBackgroundColor(Color.BLACK)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageURI(uri)
        }
        body.addView(image, FrameLayout.LayoutParams(-1, -1))
    }

    private fun previewBinaryInfo(uri: Uri, name: String) {
        showText("$name\n\nMETMC cannot safely render this format internally yet.\n\nUse Open With to send it to an installed compatible application.")
        AlertDialog.Builder(context)
            .setTitle("Unsupported format")
            .setMessage("No built-in renderer is available for this file. Open it with another installed application?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Open With") { _, _ -> launchExternal(uri) }
            .show()
    }

    private fun launchExternal(uri: Uri) {
        try {
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).apply {
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Open with"))
        } catch (_: Exception) {
            Toast.makeText(context, "No compatible application is installed", Toast.LENGTH_LONG).show()
        }
    }

    private fun showText(text: String) {
        body.removeAllViews()
        val scroll = ScrollView(context)
        val tv = TextView(context).apply {
            this.text = text
            setTextColor(Color.rgb(225, 230, 238))
            textSize = 13f
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(dp(18), dp(18), dp(18), dp(18))
            textIsSelectable = true
        }
        scroll.addView(tv)
        body.addView(scroll, FrameLayout.LayoutParams(-1, -1))
    }

    private fun stripXml(value: String): String {
        return value
            .replace(Regex("<w:tab[^>]*/>"), "\t")
            .replace(Regex("</(w:p|a:p|p:t|c|row|si|t)>"), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
            .ifBlank { "No readable text was found in this document." }
    }

    private fun queryName(uri: Uri): String {
        var name = uri.lastPathSegment ?: "Document"
        try {
            context.contentResolver.query(uri, arrayOf("_display_name"), null, null, null)?.use { c ->
                if (c.moveToFirst()) name = c.getString(0) ?: name
            }
        } catch (_: Exception) { }
        return name
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
