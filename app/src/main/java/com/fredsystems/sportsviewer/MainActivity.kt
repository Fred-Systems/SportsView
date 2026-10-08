package com.fredsystems.sportsviewer

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.os.Bundle
import android.os.Environment
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.text.InputType
import android.widget.EditText
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject
import javax.xml.parsers.DocumentBuilderFactory

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private val executor = Executors.newCachedThreadPool()
    private val calculatorAlias by lazy { ComponentName(packageName, packageName + ".CalculatorLauncher") }
    private val mainComponent by lazy { ComponentName(packageName, packageName + ".SportsViewLauncher") }
    private val prefs by lazy { getSharedPreferences("sportsview", MODE_PRIVATE) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(AppBridge(), "Android")
        setContentView(webView)
        val html = assets.open("index.html").bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        webView.loadDataWithBaseURL(
            "https://com.fredsystems.sportsviewer/",
            html,
            "text/html",
            "UTF-8",
            "https://com.fredsystems.sportsviewer/"
        )
    }

    inner class AppBridge {
        @JavascriptInterface
        fun loadVideos(league: String, pageToken: String?) {
            executor.execute {
                try {
                    val data = if (league.equals("NFL", ignoreCase = true)) {
                        // The NFL Worker has been intermittently returning HTTP 500.
                        // Go straight to the fast NFL mirror/fallback instead of making
                        // the user wait through a failing 15-second network request.
                        fetchNflMirror(pageToken)
                    } else {
                        httpGet(buildApiUrl("/videos", league.lowercase(), pageToken, null))
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
                        if (league.equals("NFL", ignoreCase = true) && pageToken.isNullOrBlank()) searchNflMirror(query) else throw e
                    }
                    send("window.receiveSearchResults(" + JSONObject.quote(data) + ");")
                } catch (e: Exception) {
                    send("window.appApiError(" + JSONObject.quote("Search failed. " + (e.message ?: "Please try again.")) + ");")
                }
            }
        }

        @JavascriptInterface fun isCalculatorLaunch(): Boolean =
            intent.component?.className == calculatorAlias.className

        @JavascriptInterface fun hasCalculatorCode(): Boolean =
            prefs.getString("calculator_code", null)?.matches(Regex("\\d{4,12}")) == true

        @JavascriptInterface fun verifyCalculatorCode(code: String): Boolean =
            hasCalculatorCode() && code == prefs.getString("calculator_code", null)

        @JavascriptInterface fun saveCalculatorCode(code: String): Boolean {
            if (!code.matches(Regex("\\d{4,12}"))) return false
            return prefs.edit().putString("calculator_code", code).commit()
        }

        @JavascriptInterface fun openSportsView() {
            runOnUiThread {
                val i = Intent(this@MainActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(i)
            }
        }

        @JavascriptInterface fun setLandscape(enabled: Boolean) {
            runOnUiThread {
                requestedOrientation = if (enabled) {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        }

        @JavascriptInterface fun checkForUpdates() {
            executor.execute {
                try {
                    val data = httpGet("https://api.github.com/repos/Fred-Systems/SportsViewer/releases/latest", "application/vnd.github+json")
                    val json = JSONObject(data)
                    val tag = json.optString("tag_name", "")
                    val name = json.optString("name", tag)
                    val url = json.optString("html_url", "https://github.com/Fred-Systems/SportsViewer/releases")
                    val current = "v" + BuildConfig.VERSION_NAME
                    val assets = json.optJSONArray("assets") ?: JSONArray()
                    var apkUrl = ""
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val assetName = asset.optString("name", "")
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                    runOnUiThread {
                        val title = if (tag == current) "SportsView is up to date" else "SportsView update available"
                        val message = if (tag == current) {
                            "You are running " + current + ". There is no newer release."
                        } else {
                            "Installed: " + current + "\nLatest: " + tag + "\n\n" + if (apkUrl.isBlank()) "The release is available, but no APK asset was found." else "You can download or install the new APK now."
                        }
                        val builder = AlertDialog.Builder(this@MainActivity).setTitle(title).setMessage(message)
                        if (tag != current && apkUrl.isNotBlank()) {
                            builder.setPositiveButton("Install now") { _, _ -> downloadAndInstall(apkUrl, tag) }
                                .setNeutralButton("Download APK") { _, _ -> openUrl(apkUrl) }
                                .setNegativeButton("Later", null)
                        } else {
                            builder.setPositiveButton("OK", null)
                        }
                        builder.show()
                    }

                    val result = JSONObject().apply {
                        put("tag", tag)
                        put("name", name)
                        put("url", url)
                        put("current", current)
                        put("apkUrl", apkUrl)
                        put("updateAvailable", tag.isNotBlank() && tag != current)
                    }
                    send("window.receiveUpdateCheck(" + JSONObject.quote(result.toString()) + ");")
                } catch (e: Exception) {
                    val result = JSONObject().put("error", "Update check failed: " + (e.message ?: "network error"))
                    send("window.receiveUpdateCheck(" + JSONObject.quote(result.toString()) + ");")
                }
            }
        }

        @JavascriptInterface fun openSettingsNative() {
            runOnUiThread {
                val options = arrayOf("Appearance","Accent color","Animations","Landscape video mode","Video player method","Viewer size","Calculator mode","Updates")
                AlertDialog.Builder(this@MainActivity).setTitle("SportsView Settings").setItems(options) { _, which ->
                    when (which) {
                        0 -> showAppearanceDialog()
                        1 -> showAccentDialog()
                        2 -> send("window.toggleSetting('animations');")
                        3 -> send("window.toggleLandscape();")
                        4 -> showPlayerMethodDialog()
                        5 -> showViewerDialog()
                        6 -> showCalculatorCodeDialog(calculatorLauncherEnabled())
                        7 -> checkForUpdates()
                    }
                }.setNegativeButton("Close", null).show()
            }
        }

        private fun showAppearanceDialog() {
            val items = arrayOf("Dark","Light","System")
            AlertDialog.Builder(this@MainActivity).setTitle("Appearance").setItems(items) { _, which ->
                send("window.setThemeFromAndroid(" + JSONObject.quote(items[which].lowercase()) + ");")
            }.show()
        }

        private fun showAccentDialog() {
            val items = arrayOf("Emerald","Violet","Blue","Orange")
            val colors = arrayOf("#19d3ae","#6c63ff","#3b82f6","#f97316")
            AlertDialog.Builder(this@MainActivity).setTitle("Accent color").setItems(items) { _, which ->
                send("window.setAccentFromAndroid(" + JSONObject.quote(colors[which]) + ");")
            }.show()
        }

        private fun showPlayerMethodDialog() {
            val items = arrayOf("IFrame API","Direct Embed","Privacy Embed")
            val values = arrayOf("api","embed","nocookie")
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Video player method")
                .setSingleChoiceItems(items, -1) { dialog, which ->
                    val method = values[which]
                    send("window.setPlayerMethod(" + JSONObject.quote(method) + ");")
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        @JavascriptInterface fun showPlayerMethodPicker() {
            runOnUiThread { showPlayerMethodDialog() }
        }

        private fun showViewerDialog() {
            val items = arrayOf("Compact","Standard","Cinema")
            val values = arrayOf("compact","standard","cinema")
            AlertDialog.Builder(this@MainActivity).setTitle("Viewer size").setItems(items) { _, which ->
                send("window.setViewerFromAndroid(" + JSONObject.quote(values[which]) + ");")
            }.show()
        }

        @JavascriptInterface fun setViewerMode(mode: String) {
            runOnUiThread {
                val safe = when (mode.lowercase()) {
                    "compact" -> "compact"
                    "cinema" -> "cinema"
                    else -> "standard"
                }
                send("window.setViewerFromAndroid(" + JSONObject.quote(safe) + ");")
            }
        }

        @JavascriptInterface fun setCalculatorLauncher(enabled: Boolean) {
            val pm = packageManager
            if (enabled) {
                pm.setComponentEnabledSetting(mainComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                pm.setComponentEnabledSetting(calculatorAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            } else {
                pm.setComponentEnabledSetting(calculatorAlias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                pm.setComponentEnabledSetting(mainComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            }
        }

        @JavascriptInterface fun downloadAndInstall(apkUrl: String, tag: String) {
            executor.execute {
                try {
                    val request = DownloadManager.Request(Uri.parse(apkUrl))
                        .setTitle("SportsView $tag")
                        .setDescription("Downloading the SportsView update")
                        .setMimeType("application/vnd.android.package-archive")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "SportsView-$tag.apk")
                    val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                    val id = manager.enqueue(request)
                    var finished = false
                    var error = false
                    while (!finished && !error) {
                        Thread.sleep(500)
                        val cursor = manager.query(DownloadManager.Query().setFilterById(id))
                        cursor.use {
                            if (it.moveToFirst()) {
                                when (it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                                    DownloadManager.STATUS_SUCCESSFUL -> finished = true
                                    DownloadManager.STATUS_FAILED -> error = true
                                }
                            }
                        }
                    }
                    if (error) throw IllegalStateException("Download failed")
                    val uri = manager.getUriForDownloadedFile(id)
                        ?: throw IllegalStateException("Downloaded APK could not be opened")
                    runOnUiThread {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/vnd.android.package-archive")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (e: Exception) {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Allow APK installation")
                                .setMessage("Android is blocking app installation from SportsView. Open the permission page, allow it, then tap Install now again.")
                                .setNegativeButton("Cancel", null)
                                .setPositiveButton("Open settings") { _, _ ->
                                    try {
                                        startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                                    } catch (_: Exception) {}
                                }.show()
                        }
                    }
                } catch (e: Exception) {
                    send("window.receiveUpdateInstallError(" + JSONObject.quote(e.message ?: "Could not download the update.") + ");")
                }
            }
        }

        private fun calculatorLauncherEnabled(): Boolean =
            packageManager.getComponentEnabledSetting(calculatorAlias) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        private fun showCalculatorCodeDialog(disableAfterSuccess: Boolean) {
            val input = EditText(this@MainActivity).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                hint = if (disableAfterSuccess) "Enter current 4–12 digit code" else "4–12 digit code"
                setSingleLine(true)
                filters = arrayOf(android.text.InputFilter.LengthFilter(12))
                setPadding(32, 20, 32, 20)
            }
            val active = calculatorLauncherEnabled()
            AlertDialog.Builder(this@MainActivity)
                .setTitle(if (active) "Exit Calculator mode" else "Calculator mode")
                .setMessage(
                    if (active) "Enter your current calculator code to return the launcher to SportsView."
                    else "Choose a 4–12 digit code. The Calculator icon will remain active until you enter this code here again."
                )
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(if (active) "Reset to SportsView" else "Enable Calculator") { _, _ ->
                    val code = input.text.toString()
                    if (active) {
                        if (verifyCalculatorCode(code)) {
                            setCalculatorLauncher(false)
                            send("window.closeSettings();")
                        } else {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Incorrect code")
                                .setMessage("That code is not correct.")
                                .setPositiveButton("OK", null).show()
                        }
                    } else if (code.matches(Regex("\\d{4,12}"))) {
                        if (saveCalculatorCode(code)) {
                            setCalculatorLauncher(true)
                            startActivity(Intent(this@MainActivity, CalculatorActivity::class.java))
                            finish()
                        }
                    } else {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Invalid code")
                            .setMessage("Use 4–12 digits.")
                            .setPositiveButton("OK", null).show()
                    }
                }.show()
        }

        @JavascriptInterface fun configureCalculator() {
            runOnUiThread { showCalculatorCodeDialog(calculatorLauncherEnabled()) }
        }

        @JavascriptInterface fun disableCalculatorMode() {
            runOnUiThread { showCalculatorCodeDialog(true) }
        }

        @JavascriptInterface fun openUrl(url: String) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Exception) {}
        }

        private fun buildApiUrl(endpoint: String, sport: String, pageToken: String?, query: String?): String {
            val base = "https://sportsview-api.nextext-app.workers.dev$endpoint"
            val params = StringBuilder("?sport=").append(URLEncoder.encode(sport, "UTF-8"))
            if (!query.isNullOrBlank()) params.append("&q=").append(URLEncoder.encode(query, "UTF-8"))
            if (!pageToken.isNullOrBlank()) params.append("&pageToken=").append(URLEncoder.encode(pageToken, "UTF-8"))
            return base + params
        }

        private fun httpGet(url: String, accept: String = "application/json"): String {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("Accept", accept)
            connection.setRequestProperty("User-Agent", "SportsView/1.7")
            try {
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = (stream ?: connection.inputStream).bufferedReader().use { it.readText() }
                if (code !in 200..299) throw IllegalStateException("HTTP $code")
                return body
            } finally {
                connection.disconnect()
            }
        }

        private fun fetchPipedNfl(): JSONArray {
            val instances = listOf("https://pipedapi.kavin.rocks","https://api.piped.yt")
            val channelId = "UCDVYQ4Zhbm3S2dlz7P1xGg"
            for (base in instances) {
                try {
                    val all = JSONArray()
                    var nextPage: String? = null
                    var pageCount = 0
                    while (pageCount < 3 && all.length() < 25) {
                        val endpoint = if (nextPage == null) "$base/channel/$channelId"
                        else "$base/nextpage/channel/$channelId?nextpage=" + java.net.URLEncoder.encode(nextPage, "UTF-8")
                        val data = JSONObject(httpGetFast(endpoint, 3000))
                        val streams = data.optJSONArray("relatedStreams") ?: JSONArray()
                        for (i in 0 until streams.length()) {
                            if (all.length() >= 25) break
                            val item = streams.getJSONObject(i)
                            val path = item.optString("url")
                            val videoId = if (path.contains("v=")) path.substringAfter("v=").substringBefore("&") else ""
                            if (videoId.isNotBlank()) {
                                all.put(JSONObject().apply {
                                    put("id", videoId)
                                    put("title", item.optString("title"))
                                    put("publishedAt", item.optString("uploadedDate"))
                                    put("thumbnail", item.optString("thumbnail"))
                                })
                            }
                        }
                        nextPage = data.optString("nextpage").takeIf { it.isNotBlank() }
                        pageCount++
                        if (nextPage == null || streams.length() == 0) break
                    }
                    if (all.length() >= 15) return all
                } catch (_: Exception) {}
            }
            return JSONArray()
        }

        private fun httpGetFast(url: String, timeout: Int): String {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "SportsView/1.8.5")
            try {
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = (stream ?: connection.inputStream).bufferedReader().use { it.readText() }
                if (code !in 200..299) throw IllegalStateException("HTTP $code")
                return body
            } finally { connection.disconnect() }
        }

        private fun fetchNflMirror(pageToken: String?): String {
            val source = JSONObject(httpGet("https://raw.githubusercontent.com/Fred-Systems/SportsViewer/main/nfl_videos.json")).optJSONArray("videos") ?: JSONArray()
            if (source.length() <= 6 && pageToken == null) {
                val piped = fetchPipedNfl()
                if (piped.length() >= 15) {
                    return JSONObject().put("videos", piped).toString()
                }
                // Never leave the NFL tab spinning while a long historical lookup
                // is unavailable. Return the known official recent mirror immediately.
                return JSONObject().put("videos", source).toString()
            }
            val start = pageToken?.toIntOrNull() ?: 0
            val end = minOf(start + 50, source.length())
            val page = JSONArray()
            for (i in start until end) page.put(source.getJSONObject(i))
            val result = JSONObject().put("videos", page)
            if (end < source.length()) result.put("nextPageToken", end.toString())
            return result.toString()
        }

        private fun searchNflMirror(query: String): String {
            val source = JSONObject(httpGet("https://raw.githubusercontent.com/Fred-Systems/SportsViewer/main/nfl_videos.json")).optJSONArray("videos") ?: JSONArray()
            val base = if (source.length() <= 6) fetchPipedNfl() else source
            val filtered = JSONArray()
            val q = query.trim().lowercase()
            for (i in 0 until base.length()) {
                val video = base.getJSONObject(i)
                if (video.optString("title").lowercase().contains(q)) filtered.put(video)
            }
            return JSONObject().put("videos", filtered).toString()
        }

        private fun fetchNflFeed(): String {
            val feedUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=UCDVYQ4Zhbm3S2dlz7P1xGg"
            val connection = URL(feedUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("Accept", "application/atom+xml,application/xml,text/xml")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 Chrome/83.0 Mobile Safari/537.36")
            val xml = try {
                val code = connection.responseCode
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream).bufferedReader().use { it.readText() }
                if (code !in 200..299) throw IllegalStateException("HTTP $code")
                body
            } finally { connection.disconnect() }
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val document = factory.newDocumentBuilder().parse(xml.byteInputStream())
            val entries = document.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry")
            val videos = JSONArray()
            for (i in 0 until entries.length) {
                val entry = entries.item(i)
                var id = ""; var title = ""; var published = ""; var thumbnail = ""
                for (j in 0 until entry.childNodes.length) {
                    val node = entry.childNodes.item(j)
                    when (node.localName) {
                        "videoId" -> id = node.textContent
                        "title" -> title = node.textContent
                        "published" -> published = node.textContent
                        "group" -> for (k in 0 until node.childNodes.length) {
                            val child = node.childNodes.item(k)
                            if (child.localName == "thumbnail") thumbnail = child.attributes?.getNamedItem("url")?.nodeValue ?: ""
                        }
                    }
                }
                if (id.isNotBlank()) videos.put(JSONObject().apply {
                    put("id", id); put("title", title); put("publishedAt", published)
                    put("thumbnail", thumbnail.ifBlank { "https://i.ytimg.com/vi/$id/hqdefault.jpg" })
                })
            }
            return JSONObject().put("videos", videos).toString()
        }

        private fun searchNflFeed(query: String): String {
            val source = JSONObject(fetchNflFeed()).optJSONArray("videos") ?: JSONArray()
            val filtered = JSONArray(); val q = query.trim().lowercase()
            for (i in 0 until source.length()) {
                val video = source.getJSONObject(i)
                if (video.optString("title").lowercase().contains(q)) filtered.put(video)
            }
            return JSONObject().put("videos", filtered).toString()
        }

        private fun send(js: String) { runOnUiThread { webView.evaluateJavascript(js, null) } }
    }
}
