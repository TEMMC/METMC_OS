package com.metmc.os.apps.t3

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*

class T3PrivateBrowserView(private val context: Context) : LinearLayout(context) {
    private lateinit var browser: WebView
    private lateinit var address: EditText
    private lateinit var progress: ProgressBar
    private lateinit var title: TextView
    private lateinit var status: TextView

    init { build() }
    private fun build() {
        orientation=VERTICAL;setBackgroundColor(Color.rgb(10,13,18))
        val top=LinearLayout(context).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(7),dp(6),dp(7),dp(4));setBackgroundColor(Color.rgb(24,29,38))}
        fun b(t:String,desc:String,a:()->Unit)=Button(context).apply{text=t;contentDescription=desc;isAllCaps=false;textSize=13f;setTextColor(Color.WHITE);minWidth=0;minimumWidth=0;setPadding(dp(7),0,dp(7),0);setOnClickListener{a()}}
        top.addView(b("‹","Back"){if(browser.canGoBack())browser.goBack()},size(42));top.addView(b("›","Forward"){if(browser.canGoForward())browser.goForward()},size(42));top.addView(b("↻","Reload"){browser.reload()},size(42));top.addView(b("⌂","Home"){navigate("https://www.google.com")},size(42))
        address=EditText(context).apply{hint="Search or enter website";textSize=14f;setSingleLine(true);setTextColor(Color.WHITE);setHintTextColor(Color.rgb(145,152,164));setPadding(dp(12),0,dp(12),0);background=rounded(Color.rgb(38,43,53),dp(10));imeOptions=EditorInfo.IME_ACTION_GO;selectAllOnFocus=true}
        top.addView(address,LinearLayout.LayoutParams(0,dp(44),1f));top.addView(b("Private","New private session"){newPrivateSession()},LinearLayout.LayoutParams(dp(78),dp(44)));top.addView(b("Clear","Clear page data"){clearData()},LinearLayout.LayoutParams(dp(58),dp(44)));addView(top,LayoutParams(-1,dp(58)))
        progress=ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal).apply{max=100;progress=0};addView(progress,LayoutParams(-1,dp(3)))
        title=TextView(context).apply{text="T3 Private Browser";textSize=11f;setTextColor(Color.rgb(160,168,182));setPadding(dp(12),0,dp(12),0);gravity=Gravity.CENTER_VERTICAL};addView(title,LayoutParams(-1,dp(27)))
        browser=WebView(context).apply{
            setBackgroundColor(Color.WHITE)
            settings.apply{javaScriptEnabled=true;domStorageEnabled=true;databaseEnabled=true;loadsImagesAutomatically=true;builtInZoomControls=true;displayZoomControls=false;useWideViewPort=true;loadWithOverviewMode=true;mediaPlaybackRequiresUserGesture=false;cacheMode=WebSettings.LOAD_DEFAULT;allowFileAccess=false;allowContentAccess=false;setSupportZoom(true);javaScriptCanOpenWindowsAutomatically=true;setSupportMultipleWindows(false);userAgentString=userAgentString.replace("; wv","")}
            CookieManager.getInstance().setAcceptCookie(false)
            webViewClient=object:WebViewClient(){override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest)=false;override fun onPageStarted(view:WebView,url:String,favicon:android.graphics.Bitmap?){address.setText(url);title.text="Loading…";status.text="Loading"};override fun onPageFinished(view:WebView,url:String){address.setText(url);address.setSelection(address.length());title.text=view.title?.takeIf{it.isNotBlank()}?:url;status.text="Ready"};override fun onReceivedError(view:WebView,request:WebResourceRequest,error:WebResourceError){if(request.isForMainFrame){title.text="Page error";status.text=error.description}}}
            webChromeClient=object:WebChromeClient(){override fun onProgressChanged(view:WebView,newProgress:Int){progress.progress=newProgress}}
            setDownloadListener(DownloadListener{url,_,_,_,_->try{context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,Uri.parse(url)))}catch(_:Exception){Toast.makeText(context,"No app can handle this download",Toast.LENGTH_SHORT).show()}})
        }
        addView(browser,LinearLayout.LayoutParams(-1,0,1f))
        status=TextView(context).apply{text="Private session • cookies disabled";textSize=11f;setTextColor(Color.rgb(145,152,164));setPadding(dp(12),0,dp(12),0);gravity=Gravity.CENTER_VERTICAL};addView(status,LayoutParams(-1,dp(25)))
        address.setOnEditorActionListener{_,_,_->navigate(address.text.toString());true}
        address.setOnFocusChangeListener{_,has->if(has)address.selectAll()}
        newPrivateSession()
    }
    private fun navigate(value:String){val input=value.trim();if(input.isEmpty())return;val url=when{input.startsWith("http://")||input.startsWith("https://")->input;input.contains(" ")->"https://www.google.com/search?q="+Uri.encode(input);input.contains(".")&&!input.contains(" ")->"https://$input";else->"https://www.google.com/search?q="+Uri.encode(input)};browser.loadUrl(url)}
    private fun newPrivateSession(){browser.stopLoading();browser.clearHistory();browser.clearCache(true);browser.clearFormData();CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();address.setText("");title.text="T3 Private Browser";status.text="Private session • cookies disabled";browser.loadUrl("https://www.google.com")}
    private fun clearData(){browser.clearHistory();browser.clearCache(true);browser.clearFormData();CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();Toast.makeText(context,"Private browser data cleared",Toast.LENGTH_SHORT).show()}
    private fun rounded(color:Int,radius:Int)=GradientDrawable().apply{setColor(color);cornerRadius=radius.toFloat()}
    override fun onDetachedFromWindow(){browser.stopLoading();browser.clearHistory();browser.clearCache(true);try{browser.destroy()}catch(_:Exception){};super.onDetachedFromWindow()}
    private fun size(v:Int)=LinearLayout.LayoutParams(dp(v),dp(44)).apply{marginStart=dp(2);marginEnd=dp(2)}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
