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
import android.widget.*

class T3PrivateBrowserView(private val context: Context) : LinearLayout(context) {
    private lateinit var browser: WebView
    private lateinit var address: EditText
    private lateinit var progress: ProgressBar
    private lateinit var title: TextView
    private lateinit var status: TextView
    private val home = "https://duckduckgo.com/"
    private val search = "https://duckduckgo.com/?q="

    init { orientation = VERTICAL; setBackgroundColor(Color.rgb(10,13,18)); build() }

    private fun build() {
        val top = LinearLayout(context).apply { orientation=HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(dp(7),dp(6),dp(7),dp(4)); setBackgroundColor(Color.rgb(24,29,38)) }
        fun button(label:String, description:String, action:()->Unit) = Button(context).apply { text=label; contentDescription=description; isAllCaps=false; textSize=13f; setTextColor(Color.WHITE); minWidth=0; minimumWidth=0; setOnClickListener{action()} }
        top.addView(button("‹","Back"){if(browser.canGoBack()) browser.goBack()}, LinearLayout.LayoutParams(dp(42),dp(44)))
        top.addView(button("›","Forward"){if(browser.canGoForward()) browser.goForward()}, LinearLayout.LayoutParams(dp(42),dp(44)))
        top.addView(button("↻","Reload"){browser.reload()}, LinearLayout.LayoutParams(dp(42),dp(44)))
        top.addView(button("⌂","Home"){navigate(home)}, LinearLayout.LayoutParams(dp(42),dp(44)))
        address=EditText(context).apply { hint="Search or enter website"; textSize=14f; setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(Color.rgb(145,152,164)); setPadding(dp(12),0,dp(12),0); background=rounded(Color.rgb(38,43,53),dp(10)); imeOptions=EditorInfo.IME_ACTION_GO }
        top.addView(address, LinearLayout.LayoutParams(0,dp(44),1f))
        top.addView(button("Private","New private session"){newPrivateSession()}, LinearLayout.LayoutParams(dp(78),dp(44)))
        top.addView(button("Clear","Clear page data"){clearData()}, LinearLayout.LayoutParams(dp(58),dp(44)))
        addView(top,LinearLayout.LayoutParams(-1,dp(58)))
        progress=ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal); progress.max=100; addView(progress,LinearLayout.LayoutParams(-1,dp(3)))
        title=TextView(context).apply{text="T3 Private Browser"; textSize=11f; setTextColor(Color.rgb(160,168,182)); setPadding(dp(12),0,dp(12),0); gravity=Gravity.CENTER_VERTICAL}; addView(title,LinearLayout.LayoutParams(-1,dp(27)))
        status=TextView(context).apply{text="Private session"; textSize=11f; setTextColor(Color.rgb(145,152,164)); setPadding(dp(12),0,dp(12),0); gravity=Gravity.CENTER_VERTICAL}
        browser=WebView(context).apply{setBackgroundColor(Color.WHITE)}
        browser.settings.apply {
            javaScriptEnabled=true; domStorageEnabled=true; databaseEnabled=true; loadsImagesAutomatically=true
            builtInZoomControls=true; displayZoomControls=false; useWideViewPort=true; loadWithOverviewMode=true
            mediaPlaybackRequiresUserGesture=false; cacheMode=WebSettings.LOAD_DEFAULT; allowFileAccess=false
            allowContentAccess=true; setSupportZoom(true); javaScriptCanOpenWindowsAutomatically=false
            setSupportMultipleWindows(false); mixedContentMode=WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString=userAgentString.replace("; wv","")
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(browser,true)
        browser.webViewClient=object:WebViewClient(){
            override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest)=false
            override fun onPageStarted(view:WebView,url:String,favicon:android.graphics.Bitmap?){address.setText(url);title.text="Loading…";status.text="Loading"}
            override fun onPageFinished(view:WebView,url:String){address.setText(url);address.setSelection(address.text?.length ?: 0);title.text=view.title?.takeIf{it.isNotBlank()}?:url;status.text="Ready"}
            override fun onReceivedError(view:WebView,request:WebResourceRequest,error:WebResourceError){if(request.isForMainFrame){title.text="Page error";status.text=error.description?.toString()?:"Unable to load page"}}
        }
        browser.webChromeClient=object:WebChromeClient(){override fun onProgressChanged(view:WebView,newProgress:Int){progress.progress=newProgress}}
        browser.setDownloadListener(DownloadListener{url,_,_,_,_->try{context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,Uri.parse(url)))}catch(_:Exception){Toast.makeText(context,"No app can handle this download",Toast.LENGTH_SHORT).show()}})
        addView(browser,LinearLayout.LayoutParams(-1,0,1f)); addView(status,LinearLayout.LayoutParams(-1,dp(25)))
        address.setOnEditorActionListener{_,_,_->navigate(address.text?.toString() ?: "");true}
        address.setOnFocusChangeListener{_,focused->if(focused) address.selectAll()}
        newPrivateSession()
    }

    private fun navigate(value:String){
        val input=value.trim(); if(input.isEmpty())return
        val url=when{input.startsWith("http://")||input.startsWith("https://")->input; input.contains(" ")->search+Uri.encode(input); input.contains(".")->"https://$input"; else->search+Uri.encode(input)}
        browser.loadUrl(url)
    }
    private fun newPrivateSession(){browser.stopLoading();browser.clearHistory();browser.clearCache(true);browser.clearFormData();address.setText("");title.text="T3 Private Browser";status.text="Private session • cookies cleared";CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();browser.loadUrl(home)}
    private fun clearData(){browser.clearHistory();browser.clearCache(true);browser.clearFormData();CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();Toast.makeText(context,"Private browser data cleared",Toast.LENGTH_SHORT).show();browser.loadUrl(home)}
    private fun rounded(color:Int,radius:Int)=GradientDrawable().apply{setColor(color);cornerRadius=radius.toFloat()}
    override fun onDetachedFromWindow(){browser.stopLoading();browser.clearHistory();browser.clearCache(true);try{browser.destroy()}catch(_:Exception){};super.onDetachedFromWindow()}
    private fun dp(value:Int)=(value*resources.displayMetrics.density).toInt()
}
