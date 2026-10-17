package com.denic.geoscan

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Custom Application class with a bulletproof UncaughtExceptionHandler.
 * Captures any unexpected early startup or runtime crash, persists the stacktrace to disk,
 * and launches DiagnosticActivity so the user gets an explicit error report instead of a blind force close.
 */
class DenicApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize persistent file logger immediately upon process startup
        com.denic.geoscan.util.GeoLogger.init(this)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("DnicGeoScan", "FATAL UNCAUGHT CRASH INTERCEPTED", throwable)
            com.denic.geoscan.util.GeoLogger.recordException("CRASH", throwable)
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stackTrace = sw.toString()

                // 1. Write crash log to app storage (accessible via My Files / PC)
                try {
                    val logDir = getExternalFilesDir(null) ?: filesDir
                    val crashFile = File(logDir, "crash_log.txt")
                    val reportContent = buildString {
                        appendLine("=== Dnic-GeoScan Diagnostic Crash Report ===")
                        appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (${android.os.Build.PRODUCT})")
                        appendLine("Android Version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
                        appendLine("Fingerprint: ${android.os.Build.FINGERPRINT}")
                        appendLine("Thread: ${thread.name}")
                        appendLine("Time: ${java.util.Date()}")
                        appendLine("===========================================")
                        appendLine()
                        appendLine(stackTrace)
                    }
                    crashFile.writeText(reportContent)
                } catch (_: Throwable) {}

                // 2. Persist in SharedPreferences
                try {
                    getSharedPreferences("dnic_crash_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("last_crash", stackTrace)
                        .commit()
                } catch (_: Throwable) {}

                // 3. Launch native DiagnosticActivity (Bypasses Compose to guarantee presentation)
                val intent = Intent(this, DiagnosticActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra("error_trace", stackTrace)
                }
                startActivity(intent)

                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            } catch (fatal: Throwable) {
                Log.e("DnicGeoScan", "Failed to launch DiagnosticActivity", fatal)
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
