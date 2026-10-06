package com.fredsystems.sportsviewer

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color

class MainActivity : Activity() {
 private val prefs:SharedPreferences by lazy { getSharedPreferences("sportsview_security",MODE_PRIVATE) }
 private val calculatorAlias=ComponentName(packageName,packageName+".CalculatorAlias")
 private val mainLauncher=ComponentName(this,MainActivity::class.java)

 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
  window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  checkpoint("native_onCreate")
  showNativeOnlyScreen()
 }

 private fun checkpoint(stage:String,detail:String="") {
  try { prefs.edit().putString("last_startup_stage",stage).putString("last_startup_detail",detail.take(1000)).commit() } catch(_:Throwable){}
 }

 private fun showNativeOnlyScreen() {
  val previous=try{prefs.getString("last_startup_stage","")?:""}catch(_:Throwable){"unreadable"}
  val detail=try{prefs.getString("last_startup_detail","")?:""}catch(_:Throwable){""}
  val root=LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL
   gravity=android.view.Gravity.CENTER
   setPadding(40,40,40,40)
   setBackgroundColor(Color.rgb(22,21,18))
  }
  val title=TextView(this).apply{text="SportsView";textSize=32f;setTextColor(Color.WHITE);gravity=android.view.Gravity.CENTER}
  val status=TextView(this).apply{text="NATIVE STARTUP TEST\n\nWebView has NOT been started.\n\nPrevious checkpoint: $previous\n$detail";textSize=16f;setTextColor(Color.LTGRAY);gravity=android.view.Gravity.CENTER;setPadding(0,30,0,0)}
  root.addView(title,LinearLayout.LayoutParams(-1,-2))
  root.addView(status,LinearLayout.LayoutParams(-1,-2))
  setContentView(root)
  checkpoint("native_screen_visible")
 }

 inner class AppBridge {
  @android.webkit.JavascriptInterface fun setLandscape(enabled:Boolean){runOnUiThread{requestedOrientation=if(enabled)ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}}
  @android.webkit.JavascriptInterface fun isCalculatorLaunch():Boolean=intent.component?.className==calculatorAlias.className
  @android.webkit.JavascriptInterface fun setCalculatorLauncher(enabled:Boolean){runOnUiThread{setLauncherComponents(enabled)}}
  @android.webkit.JavascriptInterface fun saveCalculatorCode(code:String):Boolean{val c=code.trim();if(c.length<4||c.length>12||!c.all{it.isDigit()})return false;prefs.edit().putString("calculator_code_hash",hashCode(c)).putBoolean("calculator_mode_enabled",true).apply();return true}
  @android.webkit.JavascriptInterface fun hasCalculatorCode():Boolean=!prefs.getString("calculator_code_hash",null).isNullOrBlank()
  @android.webkit.JavascriptInterface fun verifyCalculatorCode(code:String):Boolean=prefs.getString("calculator_code_hash",null)==hashCode(code.trim())
  @android.webkit.JavascriptInterface fun openSportsView(){runOnUiThread{setLauncherComponents(false)}}
  @android.webkit.JavascriptInterface fun openUrl(url:String){try{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}catch(_:Exception){}}
  private fun hashCode(value:String)=java.security.MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
  private fun setLauncherComponents(calculator:Boolean){prefs.edit().putBoolean("calculator_mode_enabled",calculator).apply();val pm=packageManager;pm.setComponentEnabledSetting(mainLauncher,if(calculator)PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);pm.setComponentEnabledSetting(calculatorAlias,if(calculator)PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP)}
 }
}