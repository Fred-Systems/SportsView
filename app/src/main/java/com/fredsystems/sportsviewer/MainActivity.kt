package com.fredsystems.sportsviewer

import android.app.Activity
import android.content.ComponentName
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private val calculatorAlias by lazy { ComponentName(packageName, packageName + ".CalculatorAlias") }
    private val prefs: SharedPreferences by lazy { getSharedPreferences("sportsview_security", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            phase("1: MainActivity entered")

            val root = LinearLayout(this)
            root.orientation = LinearLayout.VERTICAL
            root.setBackgroundColor(Color.rgb(22, 21, 18))

            val title = TextView(this)
            title.text = "SPORTSVIEW BRIDGE DIAGNOSTIC"
            title.textSize = 20f
            title.setTextColor(Color.WHITE)
            title.gravity = Gravity.CENTER
            title.setPadding(16, 20, 16, 20)
            root.addView(title, LinearLayout.LayoutParams(-1, -2))
            phase("2: Native UI created")

            webView = WebView(this)
            phase("3: WebView object created")
            webView.settings.javaScriptEnabled = true
            phase("4: JavaScript enabled")

            webView.addJavascriptInterface(DiagnosticBridge(), "Android")
            phase("5: Android JavaScript bridge added")

            webView.setBackgroundColor(Color.rgb(6, 9, 18))
            webView.loadUrl("file:///android_asset/index.html")
            phase("6: SportsView HTML load requested")

            root.addView(webView, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            ))
            setContentView(root)
            phase("7: setContentView completed")
        } catch (t: Throwable) {
            writeEmergencyReport(t)
            throw t
        }
    }

    inner class DiagnosticBridge {
        @JavascriptInterface fun loadVideos(sport: String, pageToken: String?) {
            runOnUiThread {
                webView.evaluateJavascript(
                    "window.receiveVideos(" + jsString("{\"videos\":[],\"nextPageToken\":null}") + ")",
                    null
                )
            }
        }

        @JavascriptInterface fun searchVideos(sport: String, query: String, pageToken: String?) {
            runOnUiThread {
                webView.evaluateJavascript(
                    "window.receiveSearchResults(" + jsString("{\"videos\":[],\"nextPageToken\":null}") + ")",
                    null
                )
            }
        }

        @JavascriptInterface fun isCalculatorLaunch(): Boolean =
            intent.component?.className == calculatorAlias.className

        @JavascriptInterface fun hasCalculatorCode(): Boolean =
            !prefs.getString("calculator_code_hash", null).isNullOrBlank()

        @JavascriptInterface fun verifyCalculatorCode(code: String): Boolean = false
        @JavascriptInterface fun openSportsView() {}
        @JavascriptInterface fun saveCalculatorCode(code: String): Boolean = false
        @JavascriptInterface fun setCalculatorLauncher(enabled: Boolean) {}
        @JavascriptInterface fun setLandscape(enabled: Boolean) {}

        @JavascriptInterface fun checkForUpdates() {
            runOnUiThread {
                webView.evaluateJavascript(
                    "window.receiveUpdateCheck(" + jsString("{\"error\":\"Update checking disabled in diagnostic mode.\"}") + ")",
                    null
                )
            }
        }

        @JavascriptInterface fun openUrl(url: String) {}

        private fun jsString(value: String): String =
            org.json.JSONObject.quote(value)
    }

    private fun phase(value: String) {
        try { File(filesDir, "startup_phase.txt").writeText(value) } catch (_: Throwable) {}
    }

    private fun writeEmergencyReport(t: Throwable) {
        try {
            File(filesDir, "startup_crash_report.txt").writeText(
                "Bridge diagnostic exception\n" +
                "Last recorded phase: " +
                File(filesDir, "startup_phase.txt").readText() + "\n" +
                "Exception: " + t.javaClass.name + "\n" +
                "Message: " + t.message + "\n\n" +
                t.stackTraceToString()
            )
        } catch (_: Throwable) {}
    }
}
