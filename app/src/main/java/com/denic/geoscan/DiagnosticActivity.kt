package com.denic.geoscan

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Lightweight, 100% native Android Activity (No Compose dependencies)
 * that is guaranteed to display cleanly on any device (including Samsung One UI 6 / S24 Ultra)
 * whenever a crash occurs.
 */
class DiagnosticActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val errorTrace = intent.getStringExtra("error_trace")
            ?: getSharedPreferences("dnic_crash_prefs", Context.MODE_PRIVATE)
                .getString("last_crash", "No detailed stacktrace found.") ?: "Unknown Error"

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#070A12"))
            setPadding(48, 64, 48, 48)
        }

        val title = TextView(this).apply {
            text = "Dnic-GeoScan | Diagnostic Mode"
            textSize = 20f
            setTextColor(Color.parseColor("#FF2A55"))
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 12)
        }

        val subtitle = TextView(this).apply {
            text = "A startup or runtime error was intercepted. Report this to resolve:"
            textSize = 13f
            setTextColor(Color.parseColor("#00E5FF"))
            setPadding(0, 0, 0, 8)
        }

        val deviceInfo = TextView(this).apply {
            text = "Device: ${Build.MANUFACTURER.uppercase()} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})"
            textSize = 11f
            setTextColor(Color.parseColor("#8E99AC"))
            setPadding(0, 0, 0, 20)
        }

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 20)
        }

        val copyButton = Button(this).apply {
            text = "📋 Copy Error Report"
            setBackgroundColor(Color.parseColor("#0F1626"))
            setTextColor(Color.parseColor("#00FF9D"))
            setPadding(32, 16, 32, 16)
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val fullReport = "${deviceInfo.text}\n\n$errorTrace"
                val clip = ClipData.newPlainText("Dnic-GeoScan Crash", fullReport)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this@DiagnosticActivity, "Crash report copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        }

        val spacer = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(24, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val restartButton = Button(this).apply {
            text = "🔄 Restart App"
            setBackgroundColor(Color.parseColor("#0F1626"))
            setTextColor(Color.parseColor("#00E5FF"))
            setPadding(32, 16, 32, 16)
            setOnClickListener {
                val intent = Intent(this@DiagnosticActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                finish()
            }
        }

        buttonRow.addView(copyButton)
        buttonRow.addView(spacer)
        buttonRow.addView(restartButton)

        val logFileNotice = TextView(this).apply {
            val logPath = getExternalFilesDir(null)?.absolutePath ?: "Internal Storage"
            text = "📁 Log saved to: $logPath/crash_log.txt"
            textSize = 11f
            setTextColor(Color.parseColor("#FF9100"))
            setPadding(0, 0, 0, 16)
        }

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0D1322"))
            setPadding(24, 24, 24, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val traceView = TextView(this).apply {
            text = errorTrace
            textSize = 11f
            setTextColor(Color.WHITE)
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }

        scrollView.addView(traceView)

        rootLayout.addView(title)
        rootLayout.addView(subtitle)
        rootLayout.addView(deviceInfo)
        rootLayout.addView(logFileNotice)
        rootLayout.addView(buttonRow)
        rootLayout.addView(scrollView)

        setContentView(rootLayout)
    }
}
