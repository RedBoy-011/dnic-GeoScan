package com.denic.geoscan

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.denic.geoscan.ui.navigation.AppNavigation
import com.denic.geoscan.ui.theme.DenicGeoScanTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            installSplashScreen()
        } catch (e: Throwable) {
            Log.w("DnicGeoScan", "installSplashScreen skipped safely", e)
        }
        super.onCreate(savedInstanceState)

        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            Log.w("DnicGeoScan", "enableEdgeToEdge skipped safely", e)
        }

        val prefs = getSharedPreferences("dnic_prefs", android.content.Context.MODE_PRIVATE)
        val defaultStartScreen = prefs.getString("start_screen", "hub") ?: "hub"

        try {
            setContent {
                DenicGeoScanTheme {
                    AppNavigation(startDestination = defaultStartScreen)
                }
            }
        } catch (t: Throwable) {
            Log.e("DnicGeoScan", "Fatal error during setContent", t)
            val intent = android.content.Intent(this, DiagnosticActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("error_trace", android.util.Log.getStackTraceString(t))
            }
            startActivity(intent)
            finish()
        }
    }

    private fun showFatalDiagnosticView(throwable: Throwable) {
        try {
            val scrollView = android.widget.ScrollView(this).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#070A12"))
                setPadding(40, 80, 40, 40)
            }
            val layout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
            }
            val title = android.widget.TextView(this).apply {
                text = "Dnic-GeoScan - Diagnostic Report"
                textSize = 20f
                setTextColor(android.graphics.Color.parseColor("#FF2A55"))
                setPadding(0, 0, 0, 20)
            }
            val subtitle = android.widget.TextView(this).apply {
                text = "An initialization error occurred. Please report this trace:"
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#00E5FF"))
                setPadding(0, 0, 0, 16)
            }
            val body = android.widget.TextView(this).apply {
                text = "${throwable.javaClass.name}: ${throwable.message}\n\n${Log.getStackTraceString(throwable)}"
                textSize = 11f
                setTextColor(android.graphics.Color.WHITE)
                typeface = android.graphics.Typeface.MONOSPACE
            }
            layout.addView(title)
            layout.addView(subtitle)
            layout.addView(body)
            scrollView.addView(layout)
            setContentView(scrollView)
        } catch (_: Throwable) {
            // Last-resort fallback
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            window?.decorView?.visibility = android.view.View.VISIBLE
        } catch (_: Throwable) {
            // Safe fallback
        }
    }
}
