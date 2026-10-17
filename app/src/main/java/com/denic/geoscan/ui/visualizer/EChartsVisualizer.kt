package com.denic.geoscan.ui.visualizer

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Grid3x3
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.denic.geoscan.data.model.ScanPoint
import com.denic.geoscan.ui.localization.LocalAppStrings
import com.denic.geoscan.ui.theme.DarkBase
import com.denic.geoscan.ui.theme.DarkSurface
import com.denic.geoscan.ui.theme.GlassBorder
import com.denic.geoscan.ui.theme.NeonCyan
import com.denic.geoscan.ui.theme.NeonGreen
import com.denic.geoscan.ui.theme.TextMuted
import com.denic.geoscan.ui.theme.TextPrimary
import com.denic.geoscan.ui.theme.Typography
import com.denic.geoscan.util.GeoLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 2026 Commercial-Grade Geophysical Visualizer.
 * Integrates:
 * - Dual-Engine WebGL (Apache ECharts-GL) + Canvas Fallback
 * - Fullscreen Immersive Mode with Tablet-Adaptive Responsive Dimensions
 * - High-Resolution Snapshot Export & Share Intent
 * - Seamless System Back-Navigation & Diagnostics Logging
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun EChartsVisualizer(
    points: List<ScanPoint>,
    modifier: Modifier = Modifier,
    initialMode: VisualizerViewMode = VisualizerViewMode.SURFACE_3D,
    baseline: Float = 0f
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current

    var viewMode by remember { mutableStateOf(initialMode) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    var normalWebViewInstance by remember { mutableStateOf<WebView?>(null) }
    var fullscreenWebViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isNormalChartReady by remember { mutableStateOf(false) }
    var isFullscreenChartReady by remember { mutableStateOf(false) }
    var isEngineMounted by remember { mutableStateOf(false) }
    var engineError by remember { mutableStateOf<String?>(null) }

    // Deferred mounting: Ensures Compose performs first layout frame smoothly
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(80)
        isEngineMounted = true
        GeoLogger.d("EChartsVisualizer", "Visualizer engine mounted in Compose tree")
    }

    fun sendScanData(webView: WebView?, isReady: Boolean) {
        if (webView == null) {
            GeoLogger.d("EChartsVisualizer", "sendScanData skipped: webView is null")
            return
        }
        if (!isReady) {
            GeoLogger.d("EChartsVisualizer", "sendScanData queued: chart not ready yet")
            return
        }

        try {
            val is3D = viewMode == VisualizerViewMode.SURFACE_3D
            val safeBaseline = if (baseline.isNaN() || baseline.isInfinite()) 0.0f else baseline

            val density = webView.resources.displayMetrics.density
            val widthPx = if (webView.width > 50) (webView.width / density).toInt() else 360
            val heightPx = if (webView.height > 50) (webView.height / density).toInt() else 340

            val script = if (points.isEmpty()) {
                GeoLogger.d("EChartsVisualizer", "Dispatching empty scan array")
                "if (typeof renderScanData === 'function') { renderScanData([], $is3D, $safeBaseline, $widthPx, $heightPx); }"
            } else {
                val jsonBuilder = StringBuilder(points.size * 22 + 10)
                jsonBuilder.append("[")
                points.forEachIndexed { index, p ->
                    val safeZ = if (p.zValue.isNaN() || p.zValue.isInfinite()) 0.0f else p.zValue
                    jsonBuilder.append("[${p.x},${p.y},$safeZ]")
                    if (index < points.size - 1) jsonBuilder.append(",")
                }
                jsonBuilder.append("]")
                "if (typeof renderScanData === 'function') { renderScanData($jsonBuilder, $is3D, $safeBaseline, $widthPx, $heightPx); }"
            }

            GeoLogger.d("EChartsVisualizer", "Evaluating JS renderScanData (${points.size} points, is3D=$is3D, size=${widthPx}x${heightPx})")
            webView.evaluateJavascript(script) { result ->
                GeoLogger.d("EChartsVisualizer", "JS evaluation completed: $result")
            }
        } catch (e: Throwable) {
            GeoLogger.e("EChartsVisualizer", "evaluateJavascript execution error", e)
        }
    }

    fun handleSnapshotExport(dataUrl: String) {
        isExporting = false
        try {
            val base64 = if (dataUrl.contains(",")) dataUrl.substringAfter(",") else dataUrl
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val modeLabel = if (viewMode == VisualizerViewMode.SURFACE_3D) "3D_Surface" else "2D_Heatmap"
            val fileName = "geoscan_${modeLabel}_$timeStamp.png"

            // 1. Save to cache directory for FileProvider sharing
            val cacheDir = File(context.cacheDir, "shared_scans")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val cacheFile = File(cacheDir, fileName)
            cacheFile.writeBytes(bytes)

            // 2. Save copy to MediaStore Pictures/GeoScan on Android 10+
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GeoScan")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { os ->
                            os.write(bytes)
                        }
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                    }
                }
            } catch (e: Throwable) {
                GeoLogger.w("EChartsVisualizer", "MediaStore Pictures write optional fallback", e)
            }

            // 3. Launch System Share Intent
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share GeoScan Analysis Snapshot").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })

            Toast.makeText(context, strings.visualizerExportSuccess, Toast.LENGTH_LONG).show()
        } catch (e: Throwable) {
            GeoLogger.e("EChartsVisualizer", "Failed to process exported snapshot", e)
            Toast.makeText(context, strings.visualizerExportError, Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerExport(targetWebView: WebView?) {
        if (points.isEmpty()) {
            Toast.makeText(context, strings.localNoDataTitle, Toast.LENGTH_SHORT).show()
            return
        }
        if (targetWebView == null) {
            Toast.makeText(context, strings.visualizerExportError, Toast.LENGTH_SHORT).show()
            return
        }
        isExporting = true
        GeoLogger.i("EChartsVisualizer", "Triggering snapshot export from active WebView")
        targetWebView.evaluateJavascript("if (typeof exportChartImage === 'function') { exportChartImage(); }", null)
    }

    // Reactive bridge for normal WebView
    LaunchedEffect(points, viewMode, isNormalChartReady, isEngineMounted) {
        if (isNormalChartReady && isEngineMounted) {
            sendScanData(normalWebViewInstance, isNormalChartReady)
        }
    }

    // Reactive bridge for fullscreen WebView
    LaunchedEffect(points, viewMode, isFullscreenChartReady, isFullscreen) {
        if (isFullscreenChartReady && isFullscreen) {
            sendScanData(fullscreenWebViewInstance, isFullscreenChartReady)
        }
    }

    // --- Normal In-Screen Card View ---
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .background(DarkBase)
    ) {
        // Mode Switcher & Tools Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
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
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (viewMode == VisualizerViewMode.SURFACE_3D) "3D SURFACE" else "2D HEATMAP",
                    style = Typography.labelLarge,
                    color = TextPrimary
                )
            }

            // Controls Group: 2D/3D Segmented Toggle + Export Button + Fullscreen Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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
                        text = "3D",
                        isSelected = viewMode == VisualizerViewMode.SURFACE_3D,
                        activeColor = NeonGreen,
                        onClick = {
                            viewMode = VisualizerViewMode.SURFACE_3D
                            GeoLogger.d("EChartsVisualizer", "Switched to 3D Surface")
                        }
                    )
                    ToggleOption(
                        text = "2D",
                        isSelected = viewMode == VisualizerViewMode.HEATMAP_2D,
                        activeColor = NeonCyan,
                        onClick = {
                            viewMode = VisualizerViewMode.HEATMAP_2D
                            GeoLogger.d("EChartsVisualizer", "Switched to 2D Heatmap")
                        }
                    )
                }

                // Snapshot Export Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBase)
                        .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                        .clickable(enabled = !isExporting) {
                            triggerExport(normalWebViewInstance)
                        }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = strings.visualizerExportImage,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Fullscreen Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyan.copy(alpha = 0.12f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable {
                            isFullscreen = true
                            GeoLogger.d("EChartsVisualizer", "User entered Fullscreen mode")
                        }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Fullscreen,
                        contentDescription = strings.visualizerFullscreen,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
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
                        factory = { ctx ->
                            try {
                                GeoLogger.d("EChartsVisualizer", "Creating Normal WebView instance")
                                createConfiguredWebView(
                                    context = ctx,
                                    onChartReady = {
                                        isNormalChartReady = true
                                        sendScanData(normalWebViewInstance, true)
                                    },
                                    onSnapshotExported = { dataUrl -> handleSnapshotExport(dataUrl) },
                                    onSnapshotExportError = { err ->
                                        isExporting = false
                                        GeoLogger.w("EChartsVisualizer", "Export error: $err")
                                        Toast.makeText(context, strings.visualizerExportError, Toast.LENGTH_SHORT).show()
                                    },
                                    onSizeChangedCallback = { wv ->
                                        if (isNormalChartReady) sendScanData(wv, isNormalChartReady)
                                    }
                                ).also { normalWebViewInstance = it }
                            } catch (e: Throwable) {
                                GeoLogger.e("EChartsVisualizer", "Failed to create normal WebView", e)
                                engineError = e.localizedMessage ?: "WebView initialization error"
                                View(ctx).apply { setBackgroundColor(Color.parseColor("#070A12")) }
                            }
                        },
                        update = { view ->
                            if (isNormalChartReady) {
                                sendScanData(view as? WebView, isNormalChartReady)
                            }
                        }
                    )
                }
            }
        }
    }

    // --- Tablet-Responsive Immersive Fullscreen Mode ---
    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false, // Critical for tablets: expands to 100% of device screen
                decorFitsSystemWindows = false
            )
        ) {
            // Gracefully handle physical/gesture back button to exit fullscreen
            BackHandler {
                isFullscreen = false
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBase)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Fullscreen Header Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Exit Fullscreen / Back button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBase)
                                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    isFullscreen = false
                                    GeoLogger.d("EChartsVisualizer", "Exited Fullscreen mode")
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FullscreenExit,
                                contentDescription = strings.visualizerExitFullscreen,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.visualizerExitFullscreen,
                                style = Typography.labelMedium,
                                color = TextPrimary
                            )
                        }

                        // Center: Points badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonGreen.copy(alpha = 0.15f))
                                .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${points.size} PTS • FULLSCREEN",
                                style = Typography.labelSmall,
                                color = NeonGreen,
                                fontSize = 11.sp
                            )
                        }

                        // Right: 2D/3D Mode Switcher + Export Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 2D / 3D Toggle
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

                            // High-Res Snapshot Export Button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isExporting) DarkBase else NeonCyan.copy(alpha = 0.15f))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isExporting) {
                                        triggerExport(fullscreenWebViewInstance)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isExporting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = NeonCyan,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = strings.visualizerExportImage,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.visualizerExportImage,
                                    style = Typography.labelSmall,
                                    color = NeonCyan
                                )
                            }
                        }
                    }

                    // Fullscreen Viewport Canvas (100% device width/height)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                try {
                                    GeoLogger.d("EChartsVisualizer", "Creating Fullscreen WebView instance")
                                    createConfiguredWebView(
                                        context = ctx,
                                        onChartReady = {
                                            isFullscreenChartReady = true
                                            sendScanData(fullscreenWebViewInstance, true)
                                        },
                                        onSnapshotExported = { dataUrl -> handleSnapshotExport(dataUrl) },
                                        onSnapshotExportError = { err ->
                                            isExporting = false
                                            GeoLogger.w("EChartsVisualizer", "Export error in fullscreen: $err")
                                            Toast.makeText(context, strings.visualizerExportError, Toast.LENGTH_SHORT).show()
                                        },
                                        onSizeChangedCallback = { wv ->
                                            if (isFullscreenChartReady) sendScanData(wv, isFullscreenChartReady)
                                        }
                                    ).also { fullscreenWebViewInstance = it }
                                } catch (e: Throwable) {
                                    GeoLogger.e("EChartsVisualizer", "Failed to create fullscreen WebView", e)
                                    View(ctx).apply { setBackgroundColor(Color.parseColor("#070A12")) }
                                }
                            },
                            update = { view ->
                                if (isFullscreenChartReady) {
                                    sendScanData(view as? WebView, isFullscreenChartReady)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Instantiates and configures a high-performance, hardware-accelerated WebView instance
 * for ECharts WebGL and Canvas fallback rendering with two-way JS Bridge.
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
private fun createConfiguredWebView(
    context: Context,
    onChartReady: () -> Unit,
    onSnapshotExported: (String) -> Unit,
    onSnapshotExportError: (String) -> Unit,
    onSizeChangedCallback: (WebView) -> Unit
): WebView {
    return object : WebView(context) {
        override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
            super.onSizeChanged(w, h, ow, oh)
            if (w > 50 && h > 50) {
                val wv = this
                post { onSizeChangedCallback(wv) }
            }
        }
    }.apply {
        // 1. Enable Hardware Acceleration
        try {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } catch (e: Throwable) {
            GeoLogger.w("EChartsVisualizer", "Hardware layer failed, fallback to none", e)
            try { setLayerType(View.LAYER_TYPE_NONE, null) } catch (_: Throwable) {}
        }

        setBackgroundColor(Color.parseColor("#070A12"))

        // Intercept touch to prevent parent scroll conflict during 3D rotation & gestures
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
                        onChartReady()
                    }
                }

                @JavascriptInterface
                fun onSnapshotExported(dataUrl: String) {
                    post {
                        GeoLogger.i("EChartsVisualizer", "Received onSnapshotExported callback (${dataUrl.length} chars)")
                        onSnapshotExported(dataUrl)
                    }
                }

                @JavascriptInterface
                fun onSnapshotExportError(err: String) {
                    post {
                        GeoLogger.w("EChartsVisualizer", "Received onSnapshotExportError callback: $err")
                        onSnapshotExportError(err)
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
                onChartReady()
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
