package com.fredsystems.sportsviewer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
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
 private var customView: View?=null
 private var customViewCallback: WebChromeClient.CustomViewCallback?=null
 private var normalSystemUi=0

 @SuppressLint("SetJavaScriptEnabled")
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  normalSystemUi=window.decorView.systemUiVisibility
  web=WebView(this)
  web.settings.apply {
   javaScriptEnabled=true
   domStorageEnabled=true
   mediaPlaybackRequiresUserGesture=true
   allowFileAccess=false
   allowContentAccess=false
   mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
   userAgentString=userAgentString+" SportsView/1.2"
  }

  val assetLoader=WebViewAssetLoader.Builder()
   .addPathHandler("/assets/",WebViewAssetLoader.AssetsPathHandler(this))
   .build()

  web.webViewClient=object:WebViewClient(){
   override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest)=
    assetLoader.shouldInterceptRequest(request.url)

   override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean {
    val u=request.url.toString()
    val allowed=
     u.startsWith("https://www.youtube.com/") ||
     u.startsWith("https://www.youtube-nocookie.com/") ||
     u.startsWith("https://i.ytimg.com/") ||
     u.startsWith("https://s.ytimg.com/") ||
     u.startsWith("https://sportsview-api.nextext-app.workers.dev/") ||
     u=="https://appassets.androidplatform.net/assets/index.html"
    return !allowed
   }
  }

  web.webChromeClient=object:WebChromeClient(){
   override fun onShowCustomView(view:View,callback:CustomViewCallback){
    if(customView!=null){callback.onCustomViewHidden();return}
    customView=view
    customViewCallback=callback
    (web.parent as? android.view.ViewGroup)?.addView(
     view,
     FrameLayout.LayoutParams(-1,-1)
    )
    web.visibility=View.GONE
    window.decorView.systemUiVisibility=
     View.SYSTEM_UI_FLAG_FULLSCREEN or
     View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
     View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
   }

   override fun onHideCustomView(){hideCustomView()}
  }

  web.addJavascriptInterface(AppBridge(),"Android")
  setContentView(web)

  web.loadUrl(
   "https://appassets.androidplatform.net/assets/index.html",
   mapOf("Referer" to "https://com.fredsystems.sportsviewer/")
  )
 }

 inner class AppBridge {
  @JavascriptInterface
  fun loadVideos(league:String,pageToken:String?) {
   executor.execute {
    try {
     val sport=league.lowercase()
     val url=buildApiUrl("/videos",sport,pageToken,null)
     val data=httpGet(url)
     send("window.receiveVideos("+JSONObject.quote(data)+");")
    }catch(e:Exception){
     send(
      "window.appApiError("+
      JSONObject.quote("Could not load official $league videos. "+(e.message?:"Please try again."))+
      ");"
     )
    }
   }
  }

  @JavascriptInterface
  fun searchVideos(league:String,query:String,pageToken:String?) {
   executor.execute {
    try {
     val sport=league.lowercase()
     val url=buildApiUrl("/search",sport,pageToken,query)
     val data=httpGet(url)
     send("window.receiveSearchResults("+JSONObject.quote(data)+");")
    }catch(e:Exception){
     send(
      "window.appApiError("+
      JSONObject.quote("Search failed. "+(e.message?:"Please try again."))+
      ");"
     )
    }
   }
  }

  @JavascriptInterface
  fun openUrl(url:String) {
   try {
    startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))
   }catch(_:Exception){}
  }

  @JavascriptInterface
  fun checkForUpdates() {
   executor.execute {
    try {
     val c=(URL(
      "https://api.github.com/repos/Fred-Systems/SportsViewer/releases/latest"
     ).openConnection() as HttpURLConnection).apply{
      requestMethod="GET"
      connectTimeout=10000
      readTimeout=15000
      setRequestProperty("Accept","application/vnd.github+json")
      setRequestProperty("User-Agent","SportsView/1.2")
     }
     val j=JSONObject(c.inputStream.bufferedReader().use{it.readText()})
     c.disconnect()
     val out=JSONObject().apply{
      put("tag",j.optString("tag_name",""))
      put("url",j.optString("html_url",""))
      put("name",j.optString("name","Latest release"))
     }
     send("window.receiveUpdateCheck("+JSONObject.quote(out.toString())+");")
    }catch(e:Exception){
     send(
      "window.receiveUpdateCheck("+
      JSONObject.quote("{\"error\":\"No public release is available yet.\"}")+
      ");"
     )
    }
   }
  }

  private fun buildApiUrl(
   endpoint:String,
   sport:String,
   pageToken:String?,
   query:String?
  ):String {
   val base="https://sportsview-api.nextext-app.workers.dev$endpoint"
   val params=StringBuilder()
   params.append("?sport=").append(URLEncoder.encode(sport,"UTF-8"))

   if(!query.isNullOrBlank()){
    params.append("&q=").append(URLEncoder.encode(query,"UTF-8"))
   }

   if(!pageToken.isNullOrBlank()){
    params.append("&pageToken=").append(URLEncoder.encode(pageToken,"UTF-8"))
   }

   return base+params.toString()
  }

  private fun httpGet(url:String):String {
   val c=(URL(url).openConnection() as HttpURLConnection).apply{
    requestMethod="GET"
    connectTimeout=15000
    readTimeout=20000
    setRequestProperty("Accept","application/json")
    setRequestProperty("User-Agent","SportsView/1.2")
   }

   try{
    val code=c.responseCode
    val body=(if(code in 200..299)c.inputStream else c.errorStream)
     .bufferedReader().use{it.readText()}

    if(code !in 200..299){
     val message=try{JSONObject(body).optString("error","HTTP $code")}
      catch(_:Exception){"HTTP $code"}
     throw IllegalStateException(message)
    }

    return body
   }finally{
    c.disconnect()
   }
  }

  private fun send(js:String)=runOnUiThread{
   web.evaluateJavascript(js,null)
  }
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

 override fun onBackPressed(){
  if(customView!=null){
   hideCustomView()
   return
  }
  if(web.canGoBack()){
   web.goBack()
   return
  }
  super.onBackPressed()
 }
}
