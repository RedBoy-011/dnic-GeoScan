package com.denic.geoscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.denic.geoscan.ui.navigation.AppNavigation
import com.denic.geoscan.ui.theme.DenicGeoScanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DenicGeoScanTheme {
                AppNavigation()
            }
        }
    }
}
