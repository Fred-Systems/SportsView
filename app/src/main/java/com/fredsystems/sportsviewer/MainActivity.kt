package com.fredsystems.sportsviewer

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Xml
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.FrameLayout
import androidx.webkit.WebViewAssetLoader
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
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
   userAgentString=userAgentString+" SportsView/1.1"
  }
  val assetLoader=WebViewAssetLoader.Builder().addPathHandler("/assets/",WebViewAssetLoader.AssetsPathHandler(this)).build()
  web.webViewClient=object:WebViewClient(){
   override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest)=assetLoader.shouldInterceptRequest(request.url)
   override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean {
    val u=request.url.toString()
    val allowed=u.startsWith("https://www.youtube.com/") || u.startsWith("https://www.youtube-nocookie.com/") || u.startsWith("https://i.ytimg.com/") || u.startsWith("https://s.ytimg.com/") || u=="https://appassets.androidplatform.net/assets/index.html"
    return !allowed
   }
  }
  web.webChromeClient=object:WebChromeClient(){
   override fun onShowCustomView(view:View,callback:CustomViewCallback){
    if(customView!=null){callback.onCustomViewHidden();return}
    customView=view;customViewCallback=callback
    (web.parent as? android.view.ViewGroup)?.addView(view,FrameLayout.LayoutParams(-1,-1))
    web.visibility=View.GONE
    window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
   }
   override fun onHideCustomView(){hideCustomView()}
  }
  web.addJavascriptInterface(AppBridge(),"Android")
  setContentView(web)
  web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
 }

 inner class AppBridge {
  @JavascriptInterface fun loadVideos(league:String) {
   executor.execute {
    try {
     val channel=when(league.uppercase()){"NBA"->"UCWJ2lWNubArHWmf3FIHbfcQ";"NFL"->"UCDVYQ4Zhbm3S2dlz7P1xGg";"MLB"->"UCoLrcjPV5PbUrUyXq5mjc_A";else->throw IllegalArgumentException("Unknown league")}
     val url="https://www.youtube.com/feeds/videos.xml?channel_id="+URLEncoder.encode(channel,"UTF-8")
     val c=(URL(url).openConnection() as HttpURLConnection).apply{requestMethod="GET";connectTimeout=15000;readTimeout=20000;setRequestProperty("User-Agent","SportsView/1.1")}
     val data=parseFeed(c.inputStream);c.disconnect()
     send("window.receiveVideos("+JSONObject.quote(data.toString())+");")
    }catch(e:Exception){send("window.appApiError("+JSONObject.quote("Could not load the official $league video feed. "+(e.message?:"Please try again."))+");")}
   }
  }
  @JavascriptInterface fun checkForUpdates() {
   executor.execute {
    try {
     val c=(URL("https://api.github.com/repos/Fred-Systems/SportsViewer/releases/latest").openConnection() as HttpURLConnection).apply{requestMethod="GET";connectTimeout=10000;readTimeout=15000;setRequestProperty("Accept","application/vnd.github+json");setRequestProperty("User-Agent","SportsView/1.1")}
     val j=JSONObject(c.inputStream.bufferedReader().use{it.readText()});c.disconnect()
     val out=JSONObject().apply{put("tag",j.optString("tag_name",""));put("url",j.optString("html_url",""));put("name",j.optString("name","Latest release"))}
     send("window.receiveUpdateCheck("+JSONObject.quote(out.toString())+");")
    }catch(e:Exception){send("window.receiveUpdateCheck("+JSONObject.quote("{\"error\":\"No public release is available yet.\"}")+");")}
   }
  }
  private fun parseFeed(input:InputStream):JSONArray {
   val out=JSONArray();val p=Xml.newPullParser();p.setInput(input,"UTF-8");var event=p.eventType;var inEntry=false;var id="";var title="";var published=""
   while(event!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT){
    if(event==org.xmlpull.v1.XmlPullParser.START_TAG){when(p.name){"entry"->{inEntry=true;id="";title="";published=""};"videoId"->if(inEntry)id=p.nextText();"title"->if(inEntry)title=p.nextText();"published"->if(inEntry)published=p.nextText()}}
    else if(event==org.xmlpull.v1.XmlPullParser.END_TAG && p.name=="entry" && inEntry){if(id.isNotBlank())out.put(JSONObject().apply{put("id",id);put("title",title);put("date",published);put("thumb","https://i.ytimg.com/vi/"+id+"/hqdefault.jpg")});inEntry=false}
    event=p.next()
   }
   return out
  }
  private fun send(js:String)=runOnUiThread{web.evaluateJavascript(js,null)}
 }
 private fun hideCustomView(){
  val view=customView ?: return
  (view.parent as? android.view.ViewGroup)?.removeView(view)
  customView=null;customViewCallback?.onCustomViewHidden();customViewCallback=null
  web.visibility=View.VISIBLE;window.decorView.systemUiVisibility=normalSystemUi
 }
 override fun onBackPressed(){
  if(customView!=null){hideCustomView();return}
  if(web.canGoBack()){web.goBack();return}
  super.onBackPressed()
 }
}