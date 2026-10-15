package com.denic.geoscan

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.denic.geoscan.ui.navigation.AppNavigation
import com.denic.geoscan.ui.theme.DenicGeoScanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            Log.w("denic", "enableEdgeToEdge skipped or failed safely", e)
        }
        setContent {
            DenicGeoScanTheme {
                AppNavigation()
            }
        }
    }
}
