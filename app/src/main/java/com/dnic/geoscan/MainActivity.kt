package com.dnic.geoscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dnic.geoscan.ui.navigation.AppNavigation
import com.dnic.geoscan.ui.theme.DnicGeoScanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DnicGeoScanTheme {
                AppNavigation()
            }
        }
    }
}
