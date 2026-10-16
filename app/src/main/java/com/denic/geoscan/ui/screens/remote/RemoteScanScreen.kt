package com.denic.geoscan.ui.screens.remote

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.denic.geoscan.data.bluetooth.BluetoothConnectionState
import com.denic.geoscan.data.bluetooth.BluetoothDeviceModel
import com.denic.geoscan.ui.components.GlassmorphicCard
import com.denic.geoscan.ui.theme.DarkBase
import com.denic.geoscan.ui.theme.DarkSurface
import com.denic.geoscan.ui.theme.DarkSurfaceElevated
import com.denic.geoscan.ui.theme.GlassBorder
import com.denic.geoscan.ui.theme.GlassBorderCyan
import com.denic.geoscan.ui.theme.GlassBorderHighlighted
import com.denic.geoscan.ui.theme.NeonCyan
import com.denic.geoscan.ui.theme.NeonGreen
import com.denic.geoscan.ui.theme.NeonRed
import com.denic.geoscan.ui.theme.NeonYellow
import com.denic.geoscan.ui.theme.StatusConnected
import com.denic.geoscan.ui.theme.StatusDisconnected
import com.denic.geoscan.ui.theme.StatusScanning
import com.denic.geoscan.ui.theme.TelemetryTextStyle
import com.denic.geoscan.ui.theme.TextMuted
import com.denic.geoscan.ui.theme.TextPrimary
import com.denic.geoscan.ui.theme.TextSecondary
import com.denic.geoscan.ui.theme.Typography
import com.denic.geoscan.ui.visualizer.EChartsVisualizer
import com.denic.geoscan.ui.visualizer.VisualizerViewMode

@Composable
fun RemoteScanScreen(
    modifier: Modifier = Modifier,
    onNavigateToLocal: (() -> Unit)? = null,
    viewModel: RemoteScanViewModel = viewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val latestPoint by viewModel.latestPoint.collectAsState()
    val scannedPoints by viewModel.scannedPoints.collectAsState()
    val visualizerPoints by viewModel.visualizerPoints.collectAsState()
    val rawPacketPreview by viewModel.rawPacketPreview.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val isDevicePickerVisible by viewModel.isDevicePickerVisible.collectAsState()
    val isSmoothingEnabled by viewModel.isSmoothingEnabled.collectAsState()
    val exportStatusMessage by viewModel.exportStatusMessage.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    val context = LocalContext.current
    val lastAutoSavePath by viewModel.lastAutoSavePath.collectAsState()

    // Permission launcher for Bluetooth
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            viewModel.openDevicePicker()
        }
    }

    val requestPermissionsAndOpenPicker = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val isConnected = connectionState is BluetoothConnectionState.Connected
    val isConnecting = connectionState is BluetoothConnectionState.Connecting

    val statusColor by animateColorAsState(
        targetValue = when (connectionState) {
            is BluetoothConnectionState.Connected -> if (isScanning) StatusScanning else StatusConnected
            is BluetoothConnectionState.Connecting -> NeonYellow
            is BluetoothConnectionState.Error -> NeonRed
            is BluetoothConnectionState.Disconnected -> com.denic.geoscan.ui.theme.NeonOrange
        },
        label = "statusColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBase)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        val isTabletLandscape = maxWidth >= 720.dp

        if (isTabletLandscape) {
            // Adaptive Two-Column Layout for Tablets
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Telemetry Controls & Live Stream
                Column(
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ScreenHeader(statusColor = statusColor, isConnected = isConnected, pulseAlpha = pulseAlpha)

                    ExportNotificationBanner(
                        message = exportStatusMessage,
                        onDismiss = { viewModel.clearExportMessage() }
                    )

                    HardwareStatusCard(
                        connectionState = connectionState,
                        isConnected = isConnected,
                        isConnecting = isConnecting,
                        statusColor = statusColor,
                        scannedPointsCount = scannedPoints.size,
                        isScanning = isScanning,
                        onConnectClick = requestPermissionsAndOpenPicker,
                        onDisconnectClick = { viewModel.disconnectDevice() },
                        onToggleScan = { viewModel.toggleScan() },
                        onNavigateToLocal = onNavigateToLocal
                    )

                    LiveStreamCard(
                        latestPoint = latestPoint,
                        rawPacketPreview = rawPacketPreview,
                        onClearData = { viewModel.clearData() }
                    )

                    SessionActionsCard(
                        hasPoints = scannedPoints.isNotEmpty(),
                        isExporting = isExporting,
                        isSmoothingEnabled = isSmoothingEnabled,
                        onExportClick = { viewModel.exportSession() },
                        onShareClick = { viewModel.shareCurrentSession(context) },
                        onToggleSmoothing = { viewModel.toggleSmoothing() }
                    )
                }

                // Right Column: Full-Height ECharts 3D Visualizer
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    EChartsVisualizer(
                        points = visualizerPoints,
                        modifier = Modifier.fillMaxSize(),
                        initialMode = VisualizerViewMode.SURFACE_3D,
                        baseline = 0f
                    )
                }
            }
        } else {
            // Standard Single-Column Stack for Mobile Screens
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ScreenHeader(statusColor = statusColor, isConnected = isConnected, pulseAlpha = pulseAlpha)

                ExportNotificationBanner(
                    message = exportStatusMessage,
                    onDismiss = { viewModel.clearExportMessage() }
                )

                HardwareStatusCard(
                    connectionState = connectionState,
                    isConnected = isConnected,
                    isConnecting = isConnecting,
                    statusColor = statusColor,
                    scannedPointsCount = scannedPoints.size,
                    isScanning = isScanning,
                    onConnectClick = requestPermissionsAndOpenPicker,
                    onDisconnectClick = { viewModel.disconnectDevice() },
                    onToggleScan = { viewModel.toggleScan() },
                    onNavigateToLocal = onNavigateToLocal
                )

                LiveStreamCard(
                    latestPoint = latestPoint,
                    rawPacketPreview = rawPacketPreview,
                    onClearData = { viewModel.clearData() }
                )

                SessionActionsCard(
                    hasPoints = scannedPoints.isNotEmpty(),
                    isExporting = isExporting,
                    isSmoothingEnabled = isSmoothingEnabled,
                    onExportClick = { viewModel.exportSession() },
                    onShareClick = { viewModel.shareCurrentSession(context) },
                    onToggleSmoothing = { viewModel.toggleSmoothing() }
                )

                EChartsVisualizer(
                    points = visualizerPoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    initialMode = VisualizerViewMode.SURFACE_3D,
                    baseline = 0f
                )
            }
        }
    }

    // Bluetooth Paired Devices Selection Dialog
    if (isDevicePickerVisible) {
        BluetoothDevicePickerDialog(
            devices = pairedDevices,
            onDeviceSelected = { device -> viewModel.connectDevice(device.address) },
            onRefresh = { viewModel.refreshPairedDevices() },
            onDismiss = { viewModel.closeDevicePicker() }
        )
    }
}

@Composable
private fun ScreenHeader(
    statusColor: Color,
    isConnected: Boolean,
    pulseAlpha: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "REMOTE SCAN",
                style = Typography.displayMedium,
                color = TextPrimary
            )
            Text(
                text = "HC-05 Bluetooth SPP Real-time Telemetry",
                style = Typography.titleMedium,
                color = TextSecondary
            )
        }

        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .alpha(if (isConnected) pulseAlpha else 1f)
                .background(statusColor)
        )
    }
}

@Composable
private fun ExportNotificationBanner(
    message: String?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = message != null) {
        message?.let { msg ->
            val isSuccess = !msg.startsWith("Export failed") && !msg.startsWith("Cannot export")
            val bannerColor = if (isSuccess) NeonGreen else NeonRed

            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = bannerColor
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Outlined.Check else Icons.Outlined.Close,
                            contentDescription = null,
                            tint = bannerColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            style = Typography.bodyMedium,
                            color = TextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HardwareStatusCard(
    connectionState: BluetoothConnectionState,
    isConnected: Boolean,
    isConnecting: Boolean,
    statusColor: Color,
    scannedPointsCount: Int,
    isScanning: Boolean,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onToggleScan: () -> Unit,
    onNavigateToLocal: (() -> Unit)? = null
) {
    val borderStrokeColor = when (connectionState) {
        is BluetoothConnectionState.Connected -> GlassBorderHighlighted
        is BluetoothConnectionState.Connecting -> NeonYellow.copy(alpha = 0.7f)
        is BluetoothConnectionState.Error -> NeonRed.copy(alpha = 0.8f)
        is BluetoothConnectionState.Disconnected -> com.denic.geoscan.ui.theme.NeonOrange.copy(alpha = 0.75f)
    }

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = borderStrokeColor
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = when {
                            isConnected -> Icons.Filled.BluetoothConnected
                            isConnecting -> Icons.Outlined.BluetoothSearching
                            else -> Icons.Filled.BluetoothDisabled
                        },
                        contentDescription = "Bluetooth Status",
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (connectionState) {
                            is BluetoothConnectionState.Connected -> "CONNECTED: ${connectionState.deviceName}"
                            is BluetoothConnectionState.Connecting -> "CONNECTING..."
                            is BluetoothConnectionState.Error -> "ERROR: ${connectionState.message}"
                            is BluetoothConnectionState.Disconnected -> "STATUS: DISCONNECTED • NO HARDWARE CONNECTED"
                        },
                        style = Typography.labelLarge,
                        color = statusColor
                    )
                }

                Text(text = "SPP UUID 1101", style = Typography.labelSmall, color = TextMuted)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Target Sensor", style = Typography.labelSmall, color = TextMuted)
                    val addressText = when (connectionState) {
                        is BluetoothConnectionState.Connected -> connectionState.deviceAddress
                        is BluetoothConnectionState.Connecting -> connectionState.deviceAddress
                        else -> "No HC-05 Connected (Standby)"
                    }
                    Text(text = addressText, style = Typography.bodyLarge, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Logged Points", style = Typography.labelSmall, color = TextMuted)
                    Text(
                        text = "$scannedPointsCount pts",
                        style = TelemetryTextStyle,
                        color = NeonCyan,
                        fontSize = 15.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isConnected) {
                    OutlinedButton(
                        onClick = onConnectClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                    ) {
                        Icon(imageVector = Icons.Outlined.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Connect HC-05", style = Typography.labelLarge)
                    }
                } else {
                    OutlinedButton(
                        onClick = onDisconnectClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.6f))
                    ) {
                        Text(text = "Disconnect", style = Typography.labelLarge)
                    }
                }

                ElevatedButton(
                    onClick = onToggleScan,
                    enabled = isConnected,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = if (isScanning) NeonRed else NeonGreen,
                        contentColor = DarkBase,
                        disabledContainerColor = DarkSurfaceElevated,
                        disabledContentColor = TextMuted
                    )
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isScanning) "Stop Scan" else "Start Scan",
                        style = Typography.labelLarge
                    )
                }
            }

            if (!isConnected && onNavigateToLocal != null) {
                OutlinedButton(
                    onClick = onNavigateToLocal,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonGreen,
                        containerColor = NeonGreen.copy(alpha = 0.08f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Offline? Switch to Local Analysis Mode",
                        style = Typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveStreamCard(
    latestPoint: com.denic.geoscan.data.model.ScanPoint?,
    rawPacketPreview: String,
    onClearData: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = GlassBorderCyan
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.GraphicEq,
                        contentDescription = "Stream Monitor",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE PROTOCOL STREAM (\$X,Y,Z#)",
                        style = Typography.labelLarge,
                        color = NeonCyan
                    )
                }

                IconButton(
                    onClick = onClearData,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Clear Data",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(label = "X-AXIS", value = latestPoint?.x?.toString() ?: "--", unit = "col")
                MetricItem(label = "Y-AXIS", value = latestPoint?.y?.toString() ?: "--", unit = "row")
                MetricItem(
                    label = "Z-AXIS",
                    value = latestPoint?.zValue?.let { String.format("%.1f", it) } ?: "--",
                    unit = "µT",
                    valueColor = if ((latestPoint?.zValue ?: 0f) > 0) NeonRed else NeonCyan
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "RAW: $rawPacketPreview",
                    style = TelemetryTextStyle,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun SessionActionsCard(
    hasPoints: Boolean,
    isExporting: Boolean,
    isSmoothingEnabled: Boolean,
    onExportClick: () -> Unit,
    onShareClick: () -> Unit,
    onToggleSmoothing: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = GlassBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Save Session to CSV button
            OutlinedButton(
                onClick = onExportClick,
                enabled = hasPoints && !isExporting,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NeonCyan,
                    disabledContentColor = TextMuted
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (hasPoints) NeonCyan.copy(alpha = 0.6f) else GlassBorder
                )
            ) {
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Save", style = Typography.labelLarge, fontSize = 12.sp)
                }
            }

            // Direct Share Button
            OutlinedButton(
                onClick = onShareClick,
                enabled = hasPoints && !isExporting,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NeonGreen,
                    disabledContentColor = TextMuted
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (hasPoints) NeonGreen.copy(alpha = 0.6f) else GlassBorder
                )
            ) {
                Icon(imageVector = Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Share", style = Typography.labelLarge, fontSize = 12.sp)
            }

            // Bicubic Smoothing Filter Toggle
            OutlinedButton(
                onClick = onToggleSmoothing,
                modifier = Modifier.weight(1.1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isSmoothingEnabled) NeonGreen else TextSecondary,
                    containerColor = if (isSmoothingEnabled) NeonGreen.copy(alpha = 0.12f) else Color.Transparent
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSmoothingEnabled) NeonGreen.copy(alpha = 0.7f) else GlassBorder
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (isSmoothingEnabled) NeonGreen else TextMuted
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isSmoothingEnabled) "Bicubic ON" else "Smooth",
                    style = Typography.labelLarge,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    unit: String,
    valueColor: Color = NeonGreen
) {
    Column {
        Text(text = label, style = Typography.labelSmall, color = TextMuted)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, style = TelemetryTextStyle, color = valueColor, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = unit, style = Typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
private fun BluetoothDevicePickerDialog(
    devices: List<BluetoothDeviceModel>,
    onDeviceSelected: (BluetoothDeviceModel) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            borderColor = GlassBorderCyan
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PAIRED DEVICES",
                            style = Typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Select HC-05 magnetometer",
                            style = Typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh Devices",
                            tint = NeonCyan
                        )
                    }
                }

                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No Paired Devices Found",
                                style = Typography.bodyLarge,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Please pair your HC-05 in Android Settings first",
                                style = Typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(devices) { device ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurface)
                                    .clickable { onDeviceSelected(device) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Bluetooth,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = device.name, style = Typography.titleMedium, color = TextPrimary)
                                        Text(text = device.address, style = Typography.labelSmall, color = TextMuted)
                                    }
                                }

                                Text(
                                    text = "CONNECT",
                                    style = Typography.labelSmall,
                                    color = NeonCyan
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Text(text = "Close", style = Typography.labelLarge)
                }
            }
        }
    }
}
