package com.fredsystems.sportsviewer

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val report = File(filesDir, "startup_crash_report.txt")
            val text = if (report.exists()) {
                "STARTUP CRASH DIAGNOSTIC\n\nThe previous launch crashed.\n\n" + report.readText().take(12000)
            } else {
                "STARTUP CRASH DIAGNOSTIC\n\nApplication reached MainActivity successfully.\n\nNo previous crash report exists."
            }
            val root = LinearLayout(this)
            root.orientation = LinearLayout.VERTICAL
            root.gravity = Gravity.CENTER
            root.setPadding(32, 32, 32, 32)
            root.setBackgroundColor(Color.rgb(22, 21, 18))
            val view = TextView(this)
            view.text = text
            view.textSize = 15f
            view.setTextColor(Color.WHITE)
            view.gravity = Gravity.CENTER
            root.addView(view, LinearLayout.LayoutParams(-1, -2))
            setContentView(root)
        } catch (t: Throwable) {
            writeEmergencyReport(t)
            throw t
        }
    }

    private fun writeEmergencyReport(t: Throwable) {
        try {
            File(filesDir, "startup_crash_report.txt").writeText(
                "MainActivity emergency exception\n" +
                "Exception: " + t.javaClass.name + "\n" +
                "Message: " + t.message + "\n\n" +
                t.stackTraceToString()
            )
        } catch (_: Throwable) {}
    }
}
