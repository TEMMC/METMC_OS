package com.metmc.os.desktop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import java.io.ByteArrayInputStream

class BrowserView(private val context: Context) : LinearLayout(context) {

    private val webView = WebView(context)
    private val addressBar = EditText(context)
    private val progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
    private val privacyBadge = TextView(context)

    // Lightweight built-in tracker/ad blocklist -- private browsing means
    // not loading third-party trackers in the first place, not just hiding cookies.
    private val blockedHosts = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "google-analytics.com", "googletagmanager.com", "googletagservices.com",
        "facebook.com/tr", "connect.facebook.net", "adnxs.com", "scorecardresearch.com",
        "outbrain.com", "taboola.com", "criteo.com", "amazon-adsystem.com",
        "adsrvr.org", "moatads.com", "quantserve.com", "mopub.com"
    )

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(18, 20, 26))

        // Never persist cookies -- cleared on every window close, and third-party
        // cookies rejected outright.
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)

        buildToolbar()
        buildProgressBar()
        buildWebView()

        loadUrl("https://duckduckgo.com/")
    }

    private fun buildToolbar() {
        val bar = LinearLayout(context)
        bar.orientation = HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(8), dp(6), dp(8), dp(6))
        bar.setBackgroundColor(Color.rgb(28, 30, 38))

        val back = navButton("\u2190")
        back.setOnClickListener { if (webView.canGoBack()) webView.goBack() }

        val forward = navButton("\u2192")
        forward.setOnClickListener { if (webView.canGoForward()) webView.goForward() }

        val reload = navButton("\u21bb")
        reload.setOnClickListener { webView.reload() }

        addressBar.setTextColor(Color.WHITE)
        addressBar.setHintTextColor(Color.rgb(140, 145, 155))
        addressBar.hint = "Search or enter address"
        addressBar.setSingleLine(true)
        addressBar.background = null
        addressBar.setPadding(dp(12), 0, dp(12), 0)
        addressBar.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_GO
        addressBar.setOnEditorActionListener { _, _, _ ->
            loadUrl(addressBar.text.toString().trim())
            true
        }

        privacyBadge.text = "\ud83d\udd12 T3 Private"
        privacyBadge.setTextColor(Color.rgb(120, 200, 140))
        privacyBadge.textSize = 11f
        privacyBadge.setPadding(dp(8), 0, dp(8), 0)

        bar.addView(back, LinearLayout.LayoutParams(dp(44), dp(44)))
        bar.addView(forward, LinearLayout.LayoutParams(dp(44), dp(44)))
        bar.addView(reload, LinearLayout.LayoutParams(dp(44), dp(44)))

        val addressBox = LinearLayout(context)
        addressBox.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.rgb(40, 44, 54))
            cornerRadius = dp(20).toFloat()
        }
        addressBox.addView(addressBar, LinearLayout.LayoutParams(0, dp(40), 1f))

        bar.addView(
            addressBox,
            LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginStart = dp(6); marginEnd = dp(6) }
        )
        bar.addView(privacyBadge, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(44)))

        addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))
    }

    private fun buildProgressBar() {
        progress.max = 100
        progress.visibility = GONE
        addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))
    }

    private fun buildWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.saveFormData = false           // never remember typed form data
        settings.setGeolocationEnabled(false)     // no location access by default
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.databaseEnabled = false
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val host = request.url.host ?: return null
                val blocked = blockedHosts.any { host.contains(it) }
                return if (blocked) {
                    WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                } else {
                    null
                }
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                progress.visibility = VISIBLE
                addressBar.setText(url)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                progress.visibility = GONE
            }
        }

        webView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.progress = newProgress
            }
        }

        addView(webView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun loadUrl(input: String) {
        if (input.isEmpty()) return

        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> "https://duckduckgo.com/html/?q=${Uri.encode(input)}"
        }

        webView.loadUrl(url)
    }

    private fun navButton(label: String): Button {
        val b = Button(context)
        b.text = label
        b.setTextColor(Color.WHITE)
        b.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.rgb(40, 44, 54))
            cornerRadius = dp(6).toFloat()
        }
        b.setPadding(0, 0, 0, 0)
        return b
    }

    /** Called by the window host on close -- wipes everything private-mode implies. */
    fun destroy() {
        webView.clearHistory()
        webView.clearCache(true)
        webView.clearFormData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        webView.destroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
