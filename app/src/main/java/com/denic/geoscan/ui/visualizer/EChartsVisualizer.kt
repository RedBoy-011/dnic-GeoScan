package com.denic.geoscan.ui.visualizer

import android.annotation.SuppressLint
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Grid3x3
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.denic.geoscan.data.model.ScanPoint
import com.denic.geoscan.ui.theme.DarkBase
import com.denic.geoscan.ui.theme.DarkSurface
import com.denic.geoscan.ui.theme.GlassBorder
import com.denic.geoscan.ui.theme.NeonCyan
import com.denic.geoscan.ui.theme.NeonGreen
import com.denic.geoscan.ui.theme.TextMuted
import com.denic.geoscan.ui.theme.TextPrimary
import com.denic.geoscan.ui.theme.Typography

/**
 * Hardware-accelerated Apache ECharts 3D / 2D rendering canvas embedded via WebView.
 *
 * @param points Raw or normalized spatial scan coordinates to plot.
 * @param initialMode Whether to start in 3D Surface View or 2D Heatmap View.
 * @param baseline Ground Zero baseline value (median) for color spectrum mapping.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EChartsVisualizer(
    points: List<ScanPoint>,
    modifier: Modifier = Modifier,
    initialMode: VisualizerViewMode = VisualizerViewMode.SURFACE_3D,
    baseline: Float = 0f
) {
    var viewMode by remember { mutableStateOf(initialMode) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isChartReady by remember { mutableStateOf(false) }

    // Update WebView whenever points or projection view mode changes
    LaunchedEffect(points, viewMode, isChartReady) {
        val webView = webViewInstance
        if (webView != null && isChartReady) {
            val jsonBuilder = StringBuilder("[")
            points.forEachIndexed { index, p ->
                jsonBuilder.append("[${p.x},${p.y},${p.zValue}]")
                if (index < points.size - 1) jsonBuilder.append(",")
            }
            jsonBuilder.append("]")

            val is3D = viewMode == VisualizerViewMode.SURFACE_3D
            val script = "renderScanData($jsonBuilder, $is3D, $baseline);"
            webView.evaluateJavascript(script, null)
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .background(DarkBase)
    ) {
        // Mode Switcher Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (viewMode == VisualizerViewMode.SURFACE_3D) Icons.Outlined.ViewInAr else Icons.Outlined.Grid3x3,
                    contentDescription = "Projection",
                    tint = if (viewMode == VisualizerViewMode.SURFACE_3D) NeonGreen else NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (viewMode == VisualizerViewMode.SURFACE_3D) "3D SURFACE PLOT" else "2D CONTOUR HEATMAP",
                    style = Typography.labelLarge,
                    color = TextPrimary
                )
            }

            // Segmented View Toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBase)
                    .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ToggleOption(
                    text = "3D Surface",
                    isSelected = viewMode == VisualizerViewMode.SURFACE_3D,
                    activeColor = NeonGreen,
                    onClick = { viewMode = VisualizerViewMode.SURFACE_3D }
                )
                ToggleOption(
                    text = "2D Heatmap",
                    isSelected = viewMode == VisualizerViewMode.HEATMAP_2D,
                    activeColor = NeonCyan,
                    onClick = { viewMode = VisualizerViewMode.HEATMAP_2D }
                )
            }
        }

        // WebView ECharts Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    try {
                        WebView(context).apply {
                            try {
                                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                            } catch (_: Throwable) {
                                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                            }
                            setBackgroundColor(0x00000000)

                            try { settings.javaScriptEnabled = true } catch (_: Throwable) {}
                            try { settings.domStorageEnabled = true } catch (_: Throwable) {}
                            try { settings.allowFileAccess = true } catch (_: Throwable) {}
                            try {
                                @Suppress("DEPRECATION")
                                settings.allowFileAccessFromFileURLs = true
                            } catch (_: Throwable) {}
                            try {
                                @Suppress("DEPRECATION")
                                settings.allowUniversalAccessFromFileURLs = true
                            } catch (_: Throwable) {}
                            try { settings.cacheMode = WebSettings.LOAD_NO_CACHE } catch (_: Throwable) {}

                            addJavascriptInterface(
                                object {
                                    @JavascriptInterface
                                    fun onChartReady() {
                                        post {
                                            isChartReady = true
                                        }
                                    }
                                },
                                "AndroidBridge"
                            )

                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isChartReady = true
                                }
                            }

                            loadUrl("file:///android_asset/echarts/index.html")
                            webViewInstance = this
                        }
                    } catch (_: Throwable) {
                        View(context).apply {
                            setBackgroundColor(0x00000000)
                        }
                    }
                },
                update = { view ->
                    webViewInstance = view as? WebView
                }
            )
        }
    }
}

@Composable
private fun ToggleOption(
    text: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) activeColor.copy(alpha = 0.6f) else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = Typography.labelSmall,
            color = if (isSelected) TextPrimary else TextMuted,
            fontSize = 10.sp
        )
    }
}
