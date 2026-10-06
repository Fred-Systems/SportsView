package com.fredsystems.sportsviewer

import android.app.Application
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SportsViewApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val report = buildString {
                    append("SportsView STARTUP CRASH REPORT\n")
                    append("Time: ")
                    append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date()))
                    append("\nThread: ").append(thread.name)
                    append("\nAndroid: ").append(Build.VERSION.RELEASE)
                    append(" (API ").append(Build.VERSION.SDK_INT).append(")")
                    append("\nDevice: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
                    append("\nPackage: ").append(packageName)
                    append("\n\nException: ").append(throwable.javaClass.name)
                    append("\nMessage: ").append(throwable.message)
                    append("\n\nSTACK TRACE:\n")
                    append(throwable.stackTraceToString())
                    var cause = throwable.cause
                    var level = 1
                    while (cause != null && level <= 5) {
                        append("\nCAUSE ").append(level).append(": ").append(cause.javaClass.name)
                        append("\nMessage: ").append(cause.message)
                        append("\n").append(cause.stackTraceToString())
                        cause = cause.cause
                        level++
                    }
                }
                File(filesDir, "startup_crash_report.txt").writeText(report)
            } catch (_: Throwable) {
            }
            if (previous != null) previous.uncaughtException(thread, throwable)
            else Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, throwable)
        }
    }
}
