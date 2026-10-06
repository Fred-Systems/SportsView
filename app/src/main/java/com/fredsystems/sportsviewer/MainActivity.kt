package com.fredsystems.sportsviewer

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            phase("1: MainActivity entered")

            val root = LinearLayout(this)
            root.orientation = LinearLayout.VERTICAL
            root.gravity = Gravity.CENTER
            root.setPadding(32, 32, 32, 32)
            root.setBackgroundColor(Color.rgb(22, 21, 18))

            val title = TextView(this)
            title.text = "SPORTSVIEW WEBVIEW DIAGNOSTIC"
            title.textSize = 22f
            title.setTextColor(Color.WHITE)
            title.gravity = Gravity.CENTER
            root.addView(title, LinearLayout.LayoutParams(-1, -2))

            phase("2: Native UI created")

            val web = WebView(this)
            phase("3: WebView object created")

            web.settings.javaScriptEnabled = false
            phase("4: WebView settings applied")

            web.setBackgroundColor(Color.rgb(22, 21, 18))
            web.loadDataWithBaseURL(
                null,
                "<html><body style='background:#161512;color:white;font-family:sans-serif;text-align:center;padding-top:40px'><h2>WebView is working</h2><p>WebView created and rendered successfully.</p></body></html>",
                "text/html",
                "UTF-8",
                null
            )
            phase("5: WebView load requested")

            root.addView(
                web,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    500
                )
            )
            phase("6: WebView added to native layout")

            setContentView(root)
            phase("7: setContentView completed")
        } catch (t: Throwable) {
            writeEmergencyReport(t)
            throw t
        }
    }

    private fun phase(value: String) {
        try {
            File(filesDir, "startup_phase.txt").writeText(value)
        } catch (_: Throwable) {}
    }

    private fun writeEmergencyReport(t: Throwable) {
        try {
            File(filesDir, "startup_crash_report.txt").writeText(
                "WebView diagnostic exception\n" +
                "Last recorded phase: " +
                File(filesDir, "startup_phase.txt").readText() + "\n" +
                "Exception: " + t.javaClass.name + "\n" +
                "Message: " + t.message + "\n\n" +
                t.stackTraceToString()
            )
        } catch (_: Throwable) {}
    }
}
