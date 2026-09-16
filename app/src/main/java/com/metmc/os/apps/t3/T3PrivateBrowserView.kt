package com.metmc.os.apps.t3

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout

class T3PrivateBrowserView(context: Context) : LinearLayout(context) {

    private val browser: WebView
    private val address: EditText

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(18, 20, 26))

        val toolbar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setBackgroundColor(Color.rgb(28, 30, 38))
        }

        fun button(symbol: String): ImageButton =
            ImageButton(context).apply {
                contentDescription = symbol
                setImageDrawable(
                    GradientDrawable().apply {
                        setColor(Color.rgb(45, 48, 58))
                        cornerRadius = dp(7).toFloat()
                    }
                )
                setBackgroundColor(Color.TRANSPARENT)
                setColorFilter(Color.WHITE)
            }

        val back = button("Back")
        val forward = button("Forward")
        val reload = button("Reload")
        val privateMode = button("Private browsing")

        address = EditText(context).apply {
            hint = "Search or enter address"
            textSize = 14f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(150, 155, 165))
            setPadding(dp(12), 0, dp(12), 0)
            background = GradientDrawable().apply {
                setColor(Color.rgb(38, 41, 50))
                cornerRadius = dp(9).toFloat()
            }
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_GO
        }

        toolbar.addView(back, size(42))
        toolbar.addView(forward, size(42))
        toolbar.addView(reload, size(42))
        toolbar.addView(address, LinearLayout.LayoutParams(0, dp(44), 1f))
        toolbar.addView(privateMode, size(42))

        addView(toolbar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))

        browser = WebView(context).apply {
            setBackgroundColor(Color.WHITE)
            settings.apply {
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
            }
            CookieManager.getInstance().setAcceptCookie(true)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false
                override fun onPageFinished(view: WebView, url: String) {
                    address.setText(url)
                    address.setSelection(address.length())
                }
            }
        }

        back.setOnClickListener { if (browser.canGoBack()) browser.goBack() }
        forward.setOnClickListener { if (browser.canGoForward()) browser.goForward() }
        reload.setOnClickListener { browser.reload() }
        privateMode.setOnClickListener {
            clearPrivateData()
            address.setText("")
            browser.loadUrl("about:blank")
        }
        address.setOnEditorActionListener { _, _, _ -> navigate(address.text.toString()); true }

        addView(browser, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        browser.loadUrl("https://www.google.com")
    }

    private fun navigate(value: String) {
        val input = value.trim()
        if (input.isEmpty()) return
        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(" ") -> "https://www.google.com/search?q=" + android.net.Uri.encode(input)
            else -> "https://$input"
        }
        browser.loadUrl(url)
    }

    private fun clearPrivateData() {
        browser.clearHistory()
        browser.clearCache(true)
        browser.clearFormData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    override fun onDetachedFromWindow() {
        browser.stopLoading()
        browser.clearHistory()
        browser.clearCache(true)
        browser.destroy()
        super.onDetachedFromWindow()
    }

    private fun size(dp: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(this.dp(dp), this.dp(44)).apply {
            marginStart = this@T3PrivateBrowserView.dp(3)
            marginEnd = this@T3PrivateBrowserView.dp(3)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
