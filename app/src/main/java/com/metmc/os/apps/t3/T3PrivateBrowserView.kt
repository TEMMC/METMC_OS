package com.metmc.os.apps.t3

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*

class T3PrivateBrowserView(private val context: Context) : LinearLayout(context) {
    private lateinit var browser: WebView
    private lateinit var address: EditText
    private lateinit var progress: ProgressBar
    private lateinit var title: TextView
    private var privateSession = true

    init { build() }

    private fun build() {
        orientation = VERTICAL; setBackgroundColor(Color.rgb(12,15,21))
        val top = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(7),dp(6),dp(7),dp(4)); setBackgroundColor(Color.rgb(25,29,38)) }
        fun button(text: String, description: String, action: () -> Unit) = Button(context).apply { this.text=text; contentDescription=description; isAllCaps=false; textSize=13f; setTextColor(Color.WHITE); minWidth=0; minimumWidth=0; setPadding(dp(8),0,dp(8),0); setOnClickListener { action() } }
        val back=button("‹","Back") { if(browser.canGoBack()) browser.goBack() }
        val forward=button("›","Forward") { if(browser.canGoForward()) browser.goForward() }
        val reload=button("↻","Reload") { browser.reload() }
        val home=button("⌂","Home") { navigate("https://www.google.com") }
        address=EditText(context).apply { hint="Search or enter address"; textSize=14f; setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(Color.rgb(145,152,164)); setPadding(dp(12),0,dp(12),0); background=rounded(Color.rgb(38,43,53),dp(10)); imeOptions=EditorInfo.IME_ACTION_GO; selectAllOnFocus=true }
        val privacy=button("Private","Private browsing") { startPrivateSession() }
        top.addView(back,size(42)); top.addView(forward,size(42)); top.addView(reload,size(42)); top.addView(home,size(42)); top.addView(address,LinearLayout.LayoutParams(0,dp(44),1f)); top.addView(privacy,LinearLayout.LayoutParams(dp(72),dp(44)))
        addView(top,LayoutParams(-1,dp(58)))
        progress=ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal).apply { max=100; progress=0 }
        addView(progress,LayoutParams(-1,dp(3)))
        title=TextView(context).apply { text="T3 Private Browser"; textSize=11f; setTextColor(Color.rgb(160,168,182)); setPadding(dp(12),0,dp(12),0); gravity=Gravity.CENTER_VERTICAL }
        addView(title,LayoutParams(-1,dp(28)))
        browser=WebView(context).apply {
            setBackgroundColor(Color.WHITE)
            settings.apply { javaScriptEnabled=true; domStorageEnabled=true; databaseEnabled=true; loadsImagesAutomatically=true; builtInZoomControls=true; displayZoomControls=false; useWideViewPort=true; loadWithOverviewMode=true; mediaPlaybackRequiresUserGesture=false; cacheMode=WebSettings.LOAD_DEFAULT; allowFileAccess=false; allowContentAccess=false; setSupportZoom(true); javaScriptCanOpenWindowsAutomatically=false }
            CookieManager.getInstance().setAcceptCookie(false)
            webViewClient=object: WebViewClient() {
                override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest)=false
                override fun onPageStarted(view:WebView,url:String,favicon:android.graphics.Bitmap?) { address.setText(url); title.text="Loading…" }
                override fun onPageFinished(view:WebView,url:String) { address.setText(url); address.setSelection(address.length()); title.text=view.title?.takeIf{it.isNotBlank()} ?: url; progress.progress=0 }
            }
            webChromeClient=object: WebChromeClient() { override fun onProgressChanged(view:WebView,newProgress:Int) { progress.progress=newProgress } }
            setDownloadListener(DownloadListener { url,_,_,_,_ -> try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,Uri.parse(url))) } catch (_:Exception) { Toast.makeText(context,"No app can handle this download",Toast.LENGTH_SHORT).show() } })
        }
        addView(browser,LinearLayout.LayoutParams(-1,0,1f))
        address.setOnEditorActionListener { _,_,_ -> navigate(address.text.toString()); true }
        browser.setOnLongClickListener { false }
        startPrivateSession()
    }

    private fun navigate(value:String) { val input=value.trim(); if(input.isEmpty()) return; val url=when { input.startsWith("http://")||input.startsWith("https://")->input; input.contains(" ")->"https://www.google.com/search?q="+Uri.encode(input); else->"https://$input" }; browser.loadUrl(url) }
    private fun startPrivateSession() { privateSession=true; browser.stopLoading(); browser.clearHistory(); browser.clearCache(true); browser.clearFormData(); CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush(); address.setText(""); title.text="Private session • cookies disabled"; browser.loadUrl("https://www.google.com") }
    private fun rounded(color:Int,radius:Int)=GradientDrawable().apply{setColor(color);cornerRadius=radius.toFloat()}
    override fun onDetachedFromWindow(){browser.stopLoading();browser.clearHistory();browser.clearCache(true);try{browser.destroy()}catch(_:Exception){};super.onDetachedFromWindow()}
    private fun size(v:Int)=LinearLayout.LayoutParams(dp(v),dp(44)).apply{marginStart=dp(2);marginEnd=dp(2)}
    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
}
