package com.fredsystems.sportsviewer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private val executor = Executors.newCachedThreadPool()
    private val calculatorAlias by lazy { ComponentName(packageName, packageName + ".CalculatorAlias") }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(AppBridge(), "Android")
        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
    }

    inner class AppBridge {
        @JavascriptInterface
        fun loadVideos(league: String, pageToken: String?) {
            executor.execute {
                try {
                    val data = httpGet(buildApiUrl("/videos", league.lowercase(), pageToken, null))
                    send("window.receiveVideos(" + JSONObject.quote(data) + ");")
                } catch (e: Exception) {
                    send("window.appApiError(" + JSONObject.quote("Could not load official $league videos. " + (e.message ?: "Please try again.")) + ");")
                }
            }
        }

        @JavascriptInterface
        fun searchVideos(league: String, query: String, pageToken: String?) {
            executor.execute {
                try {
                    val data = httpGet(buildApiUrl("/search", league.lowercase(), pageToken, query))
                    send("window.receiveSearchResults(" + JSONObject.quote(data) + ");")
                } catch (e: Exception) {
                    send("window.appApiError(" + JSONObject.quote("Search failed. " + (e.message ?: "Please try again.")) + ");")
                }
            }
        }

        @JavascriptInterface
        fun isCalculatorLaunch(): Boolean =
            intent.component?.className == calculatorAlias.className

        @JavascriptInterface
        fun hasCalculatorCode(): Boolean = false

        @JavascriptInterface
        fun verifyCalculatorCode(code: String): Boolean = false

        @JavascriptInterface
        fun openSportsView() {}

        @JavascriptInterface
        fun saveCalculatorCode(code: String): Boolean = false

        @JavascriptInterface
        fun setCalculatorLauncher(enabled: Boolean) {}

        @JavascriptInterface
        fun setLandscape(enabled: Boolean) {}

        @JavascriptInterface
        fun checkForUpdates() {}

        @JavascriptInterface
        fun openUrl(url: String) {}

        private fun buildApiUrl(endpoint: String, sport: String, pageToken: String?, query: String?): String {
            val base = "https://sportsview-api.nextext-app.workers.dev$endpoint"
            val params = StringBuilder("?sport=").append(URLEncoder.encode(sport, "UTF-8"))
            if (!query.isNullOrBlank()) params.append("&q=").append(URLEncoder.encode(query, "UTF-8"))
            if (!pageToken.isNullOrBlank()) params.append("&pageToken=").append(URLEncoder.encode(pageToken, "UTF-8"))
            return base + params
        }

        private fun httpGet(url: String): String {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "SportsView/1.4")
            try {
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream.bufferedReader().use { it.readText() }
                if (code !in 200..299) throw IllegalStateException("HTTP $code")
                return body
            } finally {
                connection.disconnect()
            }
        }

        private fun send(js: String) {
            runOnUiThread { webView.evaluateJavascript(js, null) }
        }
    }
}
