package com.metmc.os.apps.t3

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

class T3PrivateBrowserView(private val context: Context) : LinearLayout(context) {
    private lateinit var browser: WebView
    private lateinit var address: EditText
    private lateinit var progress: ProgressBar
    private lateinit var title: TextView
    private lateinit var status: TextView

    private val yahooHome = "https://www.yahoo.com/"
    private val yahooSearch = "https://search.yahoo.com/search?p="

    init { orientation = VERTICAL; setBackgroundColor(Color.rgb(10, 13, 18)); build() }

    private fun build() {
        val top = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(7), dp(6), dp(7), dp(4))
            setBackgroundColor(Color.rgb(24, 29, 38))
        }
        fun button(label: String, description: String, action: () -> Unit) = Button(context).apply {
            text = label
            contentDescription = description
            isAllCaps = false
            textSize = 13f
            setTextColor(Color.WHITE)
            minWidth = 0
            minimumWidth = 0
            setPadding(dp(7), 0, dp(7), 0)
            setOnClickListener { action() }
        }
        top.addView(button("‹", "Back") { if (browser.canGoBack()) browser.goBack() }, sizeParams(42))
        top.addView(button("›", "Forward") { if (browser.canGoForward()) browser.goForward() }, sizeParams(42))
        top.addView(button("↻", "Reload") { browser.reload() }, sizeParams(42))
        top.addView(button("⌂", "Yahoo Home") { navigate(yahooHome) }, sizeParams(42))

        address = EditText(context)
        address.setHint("Search Yahoo or enter website")
        address.textSize = 14f
        address.setSingleLine(true)
        address.setTextColor(Color.WHITE)
        address.setHintTextColor(Color.rgb(145, 152, 164))
        address.setPadding(dp(12), 0, dp(12), 0)
        address.background = rounded(Color.rgb(38, 43, 53), dp(10))
        address.imeOptions = EditorInfo.IME_ACTION_GO
        top.addView(address, LinearLayout.LayoutParams(0, dp(44), 1f))
        top.addView(button("Private", "New private session") { newPrivateSession() }, LinearLayout.LayoutParams(dp(78), dp(44)))
        top.addView(button("Clear", "Clear page data") { clearData() }, LinearLayout.LayoutParams(dp(58), dp(44)))
        addView(top, LinearLayout.LayoutParams(-1, dp(58)))

        progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
        progress.max = 100
        progress.progress = 0
        addView(progress, LinearLayout.LayoutParams(-1, dp(3)))

        title = TextView(context)
        title.text = "T3 Private Browser • Yahoo"
        title.textSize = 11f
        title.setTextColor(Color.rgb(160, 168, 182))
        title.setPadding(dp(12), 0, dp(12), 0)
        title.gravity = Gravity.CENTER_VERTICAL
        addView(title, LinearLayout.LayoutParams(-1, dp(27)))

        status = TextView(context)
        status.text = "Private session • cookies disabled"
        status.textSize = 11f
        status.setTextColor(Color.rgb(145, 152, 164))
        status.setPadding(dp(12), 0, dp(12), 0)
        status.gravity = Gravity.CENTER_VERTICAL

        browser = WebView(context)
        browser.setBackgroundColor(Color.WHITE)
        browser.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadsImagesAutomatically = true
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = false
            allowContentAccess = false
            setSupportZoom(true)
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            userAgentString = userAgentString.replace("; wv", "")
        }
        CookieManager.getInstance().setAcceptCookie(false)
        browser.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                this@T3PrivateBrowserView.address.setText(url)
                this@T3PrivateBrowserView.title.text = "Loading…"
                this@T3PrivateBrowserView.status.text = "Loading"
            }
            override fun onPageFinished(view: WebView, url: String) {
                this@T3PrivateBrowserView.address.setText(url)
                this@T3PrivateBrowserView.address.setSelection(this@T3PrivateBrowserView.address.length())
                this@T3PrivateBrowserView.title.text = view.title?.takeIf { it.isNotBlank() } ?: url
                this@T3PrivateBrowserView.status.text = "Ready • Yahoo search"
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    this@T3PrivateBrowserView.title.text = "Page error"
                    this@T3PrivateBrowserView.status.text = error.description?.toString() ?: "Unable to load page"
                }
            }
        }
        browser.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) { this@T3PrivateBrowserView.progress.progress = newProgress }
        }
        browser.setDownloadListener(DownloadListener { url, _, _, _, _ ->
            try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))) }
            catch (_: Exception) { Toast.makeText(context, "No app can handle this download", Toast.LENGTH_SHORT).show() }
        })
        addView(browser, LinearLayout.LayoutParams(-1, 0, 1f))
        addView(status, LinearLayout.LayoutParams(-1, dp(25)))

        address.setOnEditorActionListener { _, _, _ -> navigate(address.text.toString()); true }
        address.setOnFocusChangeListener { _, focused -> if (focused) address.selectAll() }
        newPrivateSession()
    }

    private fun navigate(value: String) {
        val input = value.trim()
        if (input.isEmpty()) return
        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(" ") -> yahooSearch + Uri.encode(input)
            input.contains(".") -> "https://$input"
            else -> yahooSearch + Uri.encode(input)
        }
        browser.loadUrl(url)
    }

    private fun newPrivateSession() {
        browser.stopLoading()
        browser.clearHistory()
        browser.clearCache(true)
        browser.clearFormData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        address.setText("")
        title.text = "T3 Private Browser • Yahoo"
        status.text = "Private session • cookies disabled"
        browser.loadUrl(yahooHome)
    }

    private fun clearData() {
        browser.clearHistory()
        browser.clearCache(true)
        browser.clearFormData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        Toast.makeText(context, "Private browser data cleared", Toast.LENGTH_SHORT).show()
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = radius.toFloat() }
    override fun onDetachedFromWindow() { browser.stopLoading(); browser.clearHistory(); browser.clearCache(true); try { browser.destroy() } catch (_: Exception) {}; super.onDetachedFromWindow() }
    private fun sizeParams(width: Int) = LinearLayout.LayoutParams(dp(width), dp(44)).apply { marginStart = dp(2); marginEnd = dp(2) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
