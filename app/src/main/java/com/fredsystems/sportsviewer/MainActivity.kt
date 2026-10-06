package com.fredsystems.sportsviewer

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.FrameLayout
import androidx.webkit.WebViewAssetLoader
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

class MainActivity : Activity() {
 private lateinit var web: WebView
 private val executor=Executors.newCachedThreadPool()
 private var customView: View? = null
 private var customViewCallback: WebChromeClient.CustomViewCallback? = null
 private var normalSystemUi = 0

 @SuppressLint("SetJavaScriptEnabled")
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  normalSystemUi = window.decorView.systemUiVisibility
  web=WebView(this)
  web.settings.apply {
   javaScriptEnabled=true
   domStorageEnabled=true
   mediaPlaybackRequiresUserGesture=true
   allowFileAccess=false
   allowContentAccess=false
   mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
  }

  val assetLoader = WebViewAssetLoader.Builder()
   .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
   .build()

  web.webViewClient=object:WebViewClient(){
   override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest)=assetLoader.shouldInterceptRequest(request.url)
   override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean {
    val u=request.url.toString()
    return !(u.startsWith("https://www.youtube.com/embed/") ||
      u.startsWith("https://www.youtube-nocookie.com/embed/") ||
      u.startsWith("https://www.youtube.com/iframe_api") ||
      u.startsWith("https://www.youtube-nocookie.com/") ||
      u.startsWith("https://i.ytimg.com/") ||
      u.startsWith("https://s.ytimg.com/") ||
      u=="https://appassets.androidplatform.net/assets/index.html")
   }
  }

  web.webChromeClient=object:WebChromeClient(){
   override fun onShowCustomView(view:View,callback:CustomViewCallback){
    if(customView!=null){callback.onCustomViewHidden();return}
    customView=view
    customViewCallback=callback
    (web.parent as? android.view.ViewGroup)?.let{parent->
     parent.addView(view,FrameLayout.LayoutParams(-1,-1))
    }
    web.visibility=View.GONE
    window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or
      View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
      View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
   }
   override fun onHideCustomView(){hideCustomView()}
  }

  web.addJavascriptInterface(YouTubeBridge(),"Android")
  setContentView(web)
  web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
 }

 private fun hideCustomView(){
  val view=customView ?: return
  (view.parent as? android.view.ViewGroup)?.removeView(view)
  customView=null
  customViewCallback?.onCustomViewHidden()
  customViewCallback=null
  web.visibility=View.VISIBLE
  window.decorView.systemUiVisibility=normalSystemUi
 }

 inner class YouTubeBridge {
  @JavascriptInterface fun apiKeyAvailable():Boolean=BuildConfig.YOUTUBE_API_KEY.isNotBlank()
  @JavascriptInterface fun loadVideos(league:String,pageToken:String?) {
   executor.execute {
    try {
     val channelId=when(league.uppercase()){
      "NBA"->"UCWJ2lWNubArHWmf3FIHbfcQ"
      "NFL"->"UCDVYQ4Zhbm3S2dlz7P1xGg"
      "MLB"->"UCoLrcjPV5PbUrUyXq5mjc_A"
      else->throw IllegalArgumentException("Unknown league")
     }
     if(BuildConfig.YOUTUBE_API_KEY.isBlank()){
      send("window.appApiError('Add the YOUTUBE_API_KEY GitHub secret before building the APK.');")
      return@execute
     }
     val playlistId="UU"+channelId.removePrefix("UC")
     val p=StringBuilder("part=snippet,contentDetails&maxResults=50&playlistId=")
      .append(URLEncoder.encode(playlistId,"UTF-8"))
      .append("&key=").append(URLEncoder.encode(BuildConfig.YOUTUBE_API_KEY,"UTF-8"))
     if(!pageToken.isNullOrBlank())p.append("&pageToken=").append(URLEncoder.encode(pageToken,"UTF-8"))
     val c=(URL("https://www.googleapis.com/youtube/v3/playlistItems?$p").openConnection() as HttpURLConnection).apply{
      requestMethod="GET";connectTimeout=15000;readTimeout=20000
     }
     val body=c.inputStream.bufferedReader().use{it.readText()}
     c.disconnect()
     val safe=body.replace("\\","\\\\").replace("'","\\'")
     send("window.receiveVideos('$safe')")
    }catch(e:Exception){
     send("window.appApiError("+JSONObject.quote(e.message?:"Unable to load videos")+")")
    }
   }
  }
  private fun send(js:String){runOnUiThread{web.evaluateJavascript(js,null)}}
 }

 override fun onBackPressed(){
  if(customView!=null){hideCustomView();return}
  web.evaluateJavascript("window.exitFullscreenIfNeeded()",null)
 }
}
