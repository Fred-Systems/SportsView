package com.fredsystems.sportsviewer

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
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
            root.setBackgroundColor(Color.rgb(22, 21, 18))

            val title = TextView(this)
            title.text = "SPORTSVIEW HTML DIAGNOSTIC"
            title.textSize = 20f
            title.setTextColor(Color.WHITE)
            title.gravity = Gravity.CENTER
            title.setPadding(16, 20, 16, 20)
            root.addView(title, LinearLayout.LayoutParams(-1, -2))

            phase("2: Native UI created")

            val web = WebView(this)
            phase("3: WebView object created")

            web.settings.javaScriptEnabled = true
            phase("4: JavaScript enabled")

            web.setBackgroundColor(Color.rgb(6, 9, 18))
            phase("5: Loading local index.html")
            web.loadUrl("file:///android_asset/index.html")
            phase("6: loadUrl requested")

            root.addView(
                web,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
            phase("7: WebView added to native layout")

            setContentView(root)
            phase("8: setContentView completed")
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
                "HTML diagnostic exception\n" +
                "Last recorded phase: " +
                File(filesDir, "startup_phase.txt").readText() + "\n" +
                "Exception: " + t.javaClass.name + "\n" +
                "Message: " + t.message + "\n\n" +
                t.stackTraceToString()
            )
        } catch (_: Throwable) {}
    }
}
