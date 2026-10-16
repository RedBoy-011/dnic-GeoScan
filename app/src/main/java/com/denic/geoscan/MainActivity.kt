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
        // Official Android 12+ Splash Screen handler:
        // installSplashScreen() must precede super.onCreate() so Android WindowManager binds correctly.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Smooth fade-out with watchdog safety timer: guarantees removal on OEM ROMs (OneUI, HyperOS, MIUI)
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val splashView = splashScreenViewProvider.view
            var isRemoved = false
            fun safeRemove() {
                if (!isRemoved) {
                    isRemoved = true
                    try {
                        splashScreenViewProvider.remove()
                    } catch (e: Throwable) {
                        Log.w("DnicGeoScan", "Safe remove splash", e)
                    }
                }
            }

            // Watchdog fallback: guaranteed removal within 250ms maximum
            splashView.postDelayed({ safeRemove() }, 250L)

            splashView.animate()
                .alpha(0f)
                .setDuration(120L)
                .withEndAction { safeRemove() }
                .start()
        }
        setTheme(R.style.Theme_DenicGeoScan)

        // Global crash catcher: Immediately renders a native Android view if anything fails
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("DnicGeoScan", "Fatal Uncaught Exception", throwable)
            try {
                runOnUiThread {
                    showFatalDiagnosticView(throwable)
                }
            } catch (_: Throwable) {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }

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
            showFatalDiagnosticView(t)
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
