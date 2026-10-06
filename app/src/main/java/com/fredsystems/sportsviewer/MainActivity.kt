package com.fredsystems.sportsviewer

import android.app.Activity
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File

class MainActivity : Activity() {
    private val calculatorAlias by lazy {
        ComponentName(packageName, packageName + ".CalculatorAlias")
    }
    private val mainLauncher by lazy {
        ComponentName(this, MainActivity::class.java)
    }
    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("sportsview_security", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            phase("1: MainActivity entered")
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            phase("2: requestedOrientation set")
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            phase("3: KEEP_SCREEN_ON set")

            val launchedAsCalculator =
                intent.component?.className == calculatorAlias.className
            phase("4: calculator launch check = $launchedAsCalculator")

            val hasSavedCode =
                !prefs.getString("calculator_code_hash", null).isNullOrBlank()
            phase("5: SharedPreferences read = $hasSavedCode")

            val root = LinearLayout(this)
            root.orientation = LinearLayout.VERTICAL
            root.gravity = Gravity.CENTER
            root.setPadding(32, 32, 32, 32)
            root.setBackgroundColor(Color.rgb(22, 21, 18))

            val title = TextView(this)
            title.text = "SPORTSVIEW STARTUP DIAGNOSTIC"
            title.textSize = 22f
            title.setTextColor(Color.WHITE)
            title.gravity = Gravity.CENTER

            val status = TextView(this)
            status.text =
                "Original native startup logic reached successfully.\\n\\n" +
                "Calculator launch: $launchedAsCalculator\\n" +
                "Saved calculator code: $hasSavedCode\\n\\n" +
                "No WebView, YouTube, HTML, network, or JavaScript is being used in this test."
            status.textSize = 15f
            status.setTextColor(Color.WHITE)
            status.gravity = Gravity.CENTER

            root.addView(title, LinearLayout.LayoutParams(-1, -2))
            root.addView(status, LinearLayout.LayoutParams(-1, -2))
            phase("6: Native UI constructed")
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
                "MainActivity emergency exception\\n" +
                "Last recorded phase: " +
                File(filesDir, "startup_phase.txt").readText() + "\\n" +
                "Exception: " + t.javaClass.name + "\\n" +
                "Message: " + t.message + "\\n\\n" +
                t.stackTraceToString()
            )
        } catch (_: Throwable) {}
    }
}
