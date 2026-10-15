package com.denic.geoscan

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denic.geoscan.ui.navigation.AppNavigation
import com.denic.geoscan.ui.theme.DarkBase
import com.denic.geoscan.ui.theme.DarkSurface
import com.denic.geoscan.ui.theme.DenicGeoScanTheme
import com.denic.geoscan.ui.theme.NeonCyan
import com.denic.geoscan.ui.theme.NeonRed

class MainActivity : ComponentActivity() {

    private var fatalErrorMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Global crash catcher to prevent app from closing silently
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("denic_fatal", "Fatal Uncaught Exception", throwable)
            try {
                runOnUiThread {
                    fatalErrorMessage = "${throwable.javaClass.name}: ${throwable.message}\n\n" +
                            Log.getStackTraceString(throwable).take(2000)
                }
            } catch (_: Throwable) {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }

        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            Log.w("denic", "enableEdgeToEdge skipped safely", e)
        }

        setContent {
            val error = fatalErrorMessage
            if (error != null) {
                CrashDiagnosticScreen(
                    errorMessage = error,
                    onRetry = { fatalErrorMessage = null }
                )
            } else {
                DenicGeoScanTheme {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
private fun CrashDiagnosticScreen(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBase)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .padding(16.dp)
        ) {
            Text(
                text = "Dnic-GeoScan - Diagnostic Report",
                color = NeonRed,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = errorMessage,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = onRetry,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
            ) {
                Text("Dismiss / Retry")
            }
        }
    }
}
