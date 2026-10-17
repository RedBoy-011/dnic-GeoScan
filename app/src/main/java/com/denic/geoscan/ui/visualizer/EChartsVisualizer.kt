package com.denic.geoscan.ui.visualizer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.CircularProgressIndicator
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
import com.denic.geoscan.util.GeoLogger

/**
 * 2026 Commercial-Grade Geophysical Visualizer.
 * Integrates dual-engine WebGL (Apache ECharts-GL) + hardware-accelerated Canvas fallback,
 * persistent console diagnostics via GeoLogger, and dynamic Compose lifecycle synchronization.
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
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
    var isEngineMounted by remember { mutableStateOf(false) }
    var engineError by remember { mutableStateOf<String?>(null) }

    // Deferred mounting: Ensures Compose performs first layout frame smoothly
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(80)
        isEngineMounted = true
        GeoLogger.d("EChartsVisualizer", "Visualizer engine mounted in Compose tree")
    }

    fun sendScanData(webView: WebView?) {
        if (webView == null) {
            GeoLogger.d("EChartsVisualizer", "sendScanData skipped: webView is null")
            return
        }
        if (!isChartReady) {
            GeoLogger.d("EChartsVisualizer", "sendScanData queued: visualizer chart not ready yet")
            return
        }

        try {
            val is3D = viewMode == VisualizerViewMode.SURFACE_3D
            val safeBaseline = if (baseline.isNaN() || baseline.isInfinite()) 0.0f else baseline

            val script = if (points.isEmpty()) {
                GeoLogger.d("EChartsVisualizer", "Dispatching empty scan array (Awaiting data)")
                "if (typeof renderScanData === 'function') { renderScanData([], $is3D, $safeBaseline); }"
            } else {
                val jsonBuilder = StringBuilder(points.size * 22 + 10)
                jsonBuilder.append("[")
                points.forEachIndexed { index, p ->
                    val safeZ = if (p.zValue.isNaN() || p.zValue.isInfinite()) 0.0f else p.zValue
                    jsonBuilder.append("[${p.x},${p.y},$safeZ]")
                    if (index < points.size - 1) jsonBuilder.append(",")
                }
                jsonBuilder.append("]")
                "if (typeof renderScanData === 'function') { renderScanData($jsonBuilder, $is3D, $safeBaseline); }"
            }

            GeoLogger.d("EChartsVisualizer", "Evaluating JS renderScanData (${points.size} points, is3D=$is3D, baseline=$safeBaseline)")
            webView.evaluateJavascript(script) { result ->
                GeoLogger.d("EChartsVisualizer", "JS evaluation completed: $result")
            }
        } catch (e: Throwable) {
            GeoLogger.e("EChartsVisualizer", "evaluateJavascript execution error", e)
        }
    }

    // Reactive bridge: Automatically dispatches new scan data whenever points or viewMode change
    LaunchedEffect(points, viewMode, isChartReady, isEngineMounted) {
        if (isChartReady && isEngineMounted) {
            sendScanData(webViewInstance)
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
                    onClick = {
                        viewMode = VisualizerViewMode.SURFACE_3D
                        GeoLogger.d("EChartsVisualizer", "User switched mode to 3D Surface")
                    }
                )
                ToggleOption(
                    text = "2D Heatmap",
                    isSelected = viewMode == VisualizerViewMode.HEATMAP_2D,
                    activeColor = NeonCyan,
                    onClick = {
                        viewMode = VisualizerViewMode.HEATMAP_2D
                        GeoLogger.d("EChartsVisualizer", "User switched mode to 2D Heatmap")
                    }
                )
            }
        }

        // WebView ECharts / WebGL Canvas Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when {
                engineError != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Visualizer Diagnostics Mode",
                            style = Typography.titleMedium,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = engineError ?: "Hardware acceleration limited",
                            style = Typography.bodyMedium,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                !isEngineMounted -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "INITIALIZING 3D WEBGL ENGINE...",
                            style = Typography.labelSmall,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    }
                }
                else -> {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            try {
                                GeoLogger.d("EChartsVisualizer", "Creating Android WebView instance")
                                WebView(context).apply {
                                    // 1. Enable Hardware Acceleration
                                    try {
                                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                    } catch (e: Throwable) {
                                        GeoLogger.w("EChartsVisualizer", "Hardware layer failed, fallback to none", e)
                                        try { setLayerType(View.LAYER_TYPE_NONE, null) } catch (_: Throwable) {}
                                    }

                                    setBackgroundColor(Color.parseColor("#070A12"))

                                    // Intercept touch to prevent parent scroll conflict during 3D rotation
                                    setOnTouchListener { v, _ ->
                                        v.parent?.requestDisallowInterceptTouchEvent(true)
                                        false
                                    }

                                    // 2. Configure Modern WebSettings
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        allowFileAccess = true
                                        allowContentAccess = true
                                        @Suppress("DEPRECATION")
                                        allowFileAccessFromFileURLs = true
                                        @Suppress("DEPRECATION")
                                        allowUniversalAccessFromFileURLs = true
                                        cacheMode = WebSettings.LOAD_NO_CACHE
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                    }

                                    // 3. Register JavaScript Bridge
                                    addJavascriptInterface(
                                        object {
                                            @JavascriptInterface
                                            fun onChartReady() {
                                                post {
                                                    GeoLogger.i("EChartsVisualizer", "Received onChartReady callback from JS bridge")
                                                    isChartReady = true
                                                    sendScanData(this@apply)
                                                }
                                            }
                                        },
                                        "AndroidBridge"
                                    )

                                    // 4. Capture all JavaScript console messages to GeoLogger
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                            consoleMessage?.let { msg ->
                                                val level = when (msg.messageLevel()) {
                                                    ConsoleMessage.MessageLevel.ERROR -> "ERROR"
                                                    ConsoleMessage.MessageLevel.WARNING -> "WARN"
                                                    ConsoleMessage.MessageLevel.DEBUG -> "DEBUG"
                                                    else -> "INFO"
                                                }
                                                GeoLogger.logJsConsole(
                                                    level = level,
                                                    sourceId = msg.sourceId() ?: "index.html",
                                                    lineNumber = msg.lineNumber(),
                                                    message = msg.message() ?: ""
                                                )
                                            }
                                            return true
                                        }
                                    }

                                    // 5. WebView Lifecycle Client
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            GeoLogger.i("EChartsVisualizer", "WebView onPageFinished: $url")
                                            isChartReady = true
                                            sendScanData(this@apply)
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                            error: WebResourceError?
                                        ) {
                                            super.onReceivedError(view, request, error)
                                            val desc = error?.description?.toString() ?: "Unknown web error"
                                            val failingUrl = request?.url?.toString() ?: "unknown"
                                            GeoLogger.w("EChartsVisualizer", "WebView error at $failingUrl: $desc")
                                        }
                                    }

                                    loadUrl("file:///android_asset/echarts/index.html")
                                    webViewInstance = this
                                }
                            } catch (e: Throwable) {
                                GeoLogger.e("EChartsVisualizer", "Failed to create WebView", e)
                                engineError = e.localizedMessage ?: "WebView initialization error"
                                View(context).apply {
                                    setBackgroundColor(Color.parseColor("#070A12"))
                                }
                            }
                        },
                        update = { webView ->
                            // Guaranteed Compose update callback on live WebView
                            if (isChartReady) {
                                sendScanData(webView)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleOption(
    text: String,
    isSelected: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) activeColor.copy(alpha = 0.22f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = Typography.labelSmall,
            color = if (isSelected) activeColor else TextMuted,
            fontSize = 11.sp
        )
    }
}
