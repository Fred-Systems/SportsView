package com.fredsystems.sportsviewer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.SharedPreferences
import java.security.MessageDigest
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.util.Xml
import android.webkit.*
import android.widget.FrameLayout
import androidx.webkit.WebViewAssetLoader
import org.json.JSONArray
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
 private val prefs: SharedPreferences by lazy { getSharedPreferences("sportsview_security", MODE_PRIVATE) }
 private val calculatorAlias = ComponentName(packageName, packageName + ".CalculatorAlias")
 private val mainLauncher = ComponentName(this, MainActivity::class.java)

 @SuppressLint("SetJavaScriptEnabled")
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
  window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  val calculatorEnabled=prefs.getBoolean("calculator_mode_enabled",false)
  if(!calculatorEnabled){
   packageManager.setComponentEnabledSetting(
    mainLauncher,PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP
   )
   packageManager.setComponentEnabledSetting(
    calculatorAlias,PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP
   )
  }
  normalSystemUi=window.decorView.systemUiVisibility
  web=WebView(this)
  web.settings.apply {
   javaScriptEnabled=true
   domStorageEnabled=true
   mediaPlaybackRequiresUserGesture=true
   allowFileAccess=false
   allowContentAccess=false
   mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
   userAgentString=userAgentString+" SportsView/1.4"
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

   override fun onRenderProcessGone(view:WebView,detail:RenderProcessGoneDetail):Boolean {
    runOnUiThread {
     try {
      (view.parent as? android.view.ViewGroup)?.removeView(view)
      view.destroy()
     }catch(_:Exception){}
     val message=if(detail.didCrash())
      "SportsView's video/web renderer stopped unexpectedly. The app is still running."
     else
      "SportsView's web renderer was stopped by Android to recover memory."
     val fallback=android.widget.TextView(this@MainActivity).apply{
      text=message+"\n\nPlease reopen SportsView. Your app data is safe."
      textSize=16f
      setTextColor(android.graphics.Color.WHITE)
      setBackgroundColor(android.graphics.Color.rgb(6,9,18))
      gravity=android.view.Gravity.CENTER
      setPadding(48,48,48,48)
     }
     setContentView(fallback)
    }
    return true
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
  fun setLandscape(enabled:Boolean) {
   runOnUiThread {
    requestedOrientation=
     if(enabled) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
     else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
   }
  }

  @JavascriptInterface
  fun loadVideos(league:String,pageToken:String?) {
   executor.execute {
    val sport=league.lowercase()
    try {
     val url=buildApiUrl("/videos",sport,pageToken,null)
     val data=httpGet(url)
     send("window.receiveVideos("+JSONObject.quote(data)+");")
    }catch(e:Exception){
     if(sport=="nfl"){
      try{
       send("window.receiveVideos("+JSONObject.quote(fallbackNflFeed())+");")
       return@execute
      }catch(_:Exception){}
     }
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
    val sport=league.lowercase()
    try {
     val url=buildApiUrl("/search",sport,pageToken,query)
     val data=httpGet(url)
     send("window.receiveSearchResults("+JSONObject.quote(data)+");")
    }catch(e:Exception){
     if(sport=="nfl"){
      try{
       val fallback=JSONObject(fallbackNflFeed())
       val all=fallback.optJSONArray("videos") ?: JSONArray()
       val q=query.trim().lowercase()
       val filtered=JSONArray()
       for(i in 0 until all.length()){
        val item=all.optJSONObject(i)
        if(item!=null && item.optString("title").lowercase().contains(q)) filtered.put(item)
       }
       fallback.put("videos",filtered)
       send("window.receiveSearchResults("+JSONObject.quote(fallback.toString())+");")
       return@execute
      }catch(_:Exception){}
     }
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
  fun isCalculatorLaunch():Boolean =
   intent.component?.className == calculatorAlias.className

  @JavascriptInterface
  fun setCalculatorLauncher(enabled:Boolean) {
   runOnUiThread { setLauncherComponents(enabled) }
  }

  @JavascriptInterface
  fun saveCalculatorCode(code:String):Boolean {
   val clean=code.trim()
   if(clean.length<4 || clean.length>12 || !clean.all{it.isDigit()}) return false
   prefs.edit()
    .putString("calculator_code_hash",hashCode(clean))
    .putBoolean("calculator_mode_enabled",true)
    .apply()
   return true
  }

  @JavascriptInterface
  fun hasCalculatorCode():Boolean =
   !prefs.getString("calculator_code_hash",null).isNullOrBlank()

  @JavascriptInterface
  fun verifyCalculatorCode(code:String):Boolean {
   val saved=prefs.getString("calculator_code_hash",null) ?: return false
   return saved==hashCode(code.trim())
  }

  @JavascriptInterface
  fun openSportsView() {
   runOnUiThread {
    setLauncherComponents(false)
    web.evaluateJavascript("window.exitCalculatorMode&&window.exitCalculatorMode()",null)
   }
  }

  private fun hashCode(value:String):String {
   val digest=MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
   return digest.joinToString("") { "%02x".format(it) }
  }

  private fun setLauncherComponents(calculator:Boolean) {
   prefs.edit().putBoolean("calculator_mode_enabled",calculator).apply()
   val pm=packageManager
   pm.setComponentEnabledSetting(
    mainLauncher,
    if(calculator) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
    PackageManager.DONT_KILL_APP
   )
   pm.setComponentEnabledSetting(
    calculatorAlias,
    if(calculator) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
    PackageManager.DONT_KILL_APP
   )
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
      setRequestProperty("User-Agent","SportsView/1.4")
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

  private fun fallbackNflFeed():String {
   val channelId="UCDVYQ4Zhbm3S2dlz7P1xGg"
   val url=URL("https://www.youtube.com/feeds/videos.xml?channel_id="+channelId)
   val c=(url.openConnection() as HttpURLConnection).apply{
    requestMethod="GET"
    connectTimeout=12000
    readTimeout=15000
    setRequestProperty("User-Agent","SportsView/1.4")
   }
   try{
    if(c.responseCode !in 200..299) throw IllegalStateException("NFL fallback feed returned HTTP "+c.responseCode)
    val parser=Xml.newPullParser()
    parser.setInput(c.inputStream,"UTF-8")
    val videos=JSONArray()
    var event=parser.eventType
    var insideEntry=false
    var id=""
    var title=""
    var published=""
    while(event!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT){
     if(event==org.xmlpull.v1.XmlPullParser.START_TAG){
      val name=parser.name
      if(name=="entry"){
       insideEntry=true;id="";title="";published=""
      }else if(insideEntry && (name=="videoId" || name=="title" || name=="published")){
       val value=parser.nextText()
       when(name){
        "videoId"->id=value
        "title"->title=value
        "published"->published=value
       }
      }
     }else if(event==org.xmlpull.v1.XmlPullParser.END_TAG && parser.name=="entry"){
      if(id.isNotBlank()){
       videos.put(JSONObject().apply{
        put("id",id)
        put("title",if(title.isBlank())"NFL video" else title)
        put("description","")
        put("publishedAt",published)
        put("channelId",channelId)
        put("channelTitle","NFL")
        put("thumbnail","https://i.ytimg.com/vi/"+id+"/hqdefault.jpg")
       })
      }
      insideEntry=false
     }
     event=parser.next()
    }
    return JSONObject().apply{
     put("sport","nfl")
     put("channel","NFL")
     put("videos",videos)
     put("nextPageToken",JSONObject.NULL)
    }.toString()
   }finally{
    c.disconnect()
   }
  }

  private fun httpGet(url:String):String {
   val c=(URL(url).openConnection() as HttpURLConnection).apply{
    requestMethod="GET"
    connectTimeout=15000
    readTimeout=20000
    setRequestProperty("Accept","application/json")
    setRequestProperty("User-Agent","SportsView/1.4")
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
