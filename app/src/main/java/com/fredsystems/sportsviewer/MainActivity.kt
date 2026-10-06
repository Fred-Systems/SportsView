package com.fredsystems.sportsviewer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject
import javax.xml.parsers.DocumentBuilderFactory

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
                    val data = try {
                        httpGet(buildApiUrl("/videos", league.lowercase(), pageToken, null))
                    } catch (e: Exception) {
                        if (league.equals("NFL", ignoreCase = true) && pageToken.isNullOrBlank()) {
                            fetchNflFeed()
                        } else {
                            throw e
                        }
                    }
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
                    val data = try {
                        httpGet(buildApiUrl("/search", league.lowercase(), pageToken, query))
                    } catch (e: Exception) {
                        if (league.equals("NFL", ignoreCase = true) && pageToken.isNullOrBlank()) {
                            searchNflFeed(query)
                        } else {
                            throw e
                        }
                    }
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
        fun setLandscape(enabled: Boolean) {
            runOnUiThread {
                requestedOrientation = if (enabled) {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        }

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

        private fun fetchNflFeed(): String {
            val feedUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=UCDVYQ4Zhbm3S2dlz7P1xGg"
            val xml = httpGet(feedUrl)
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val document = factory.newDocumentBuilder().parse(xml.byteInputStream())
            val entries = document.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry")
            val videos = JSONArray()
            for (i in 0 until entries.length) {
                val entry = entries.item(i)
                val videoId = entry.childNodes.item(0)
                var id = ""
                var title = ""
                var published = ""
                var thumbnail = ""
                for (j in 0 until entry.childNodes.length) {
                    val node = entry.childNodes.item(j)
                    when (node.localName) {
                        "videoId" -> id = node.textContent
                        "title" -> title = node.textContent
                        "published" -> published = node.textContent
                        "group" -> {
                            for (k in 0 until node.childNodes.length) {
                                val child = node.childNodes.item(k)
                                if (child.localName == "thumbnail") thumbnail = child.attributes?.getNamedItem("url")?.nodeValue ?: ""
                            }
                        }
                    }
                }
                if (id.isNotBlank()) {
                    videos.put(JSONObject().apply {
                        put("id", id)
                        put("title", title)
                        put("publishedAt", published)
                        put("thumbnail", thumbnail.ifBlank { "https://i.ytimg.com/vi/$id/hqdefault.jpg" })
                    })
                }
            }
            return JSONObject().apply { put("videos", videos) }.toString()
        }

        private fun searchNflFeed(query: String): String {
            val data = JSONObject(fetchNflFeed())
            val source = data.optJSONArray("videos") ?: JSONArray()
            val filtered = JSONArray()
            val q = query.trim().lowercase()
            for (i in 0 until source.length()) {
                val video = source.getJSONObject(i)
                if (video.optString("title").lowercase().contains(q)) filtered.put(video)
            }
            return JSONObject().apply { put("videos", filtered) }.toString()
        }

        private fun send(js: String) {
            runOnUiThread { webView.evaluateJavascript(js, null) }
        }
    }
}
