package com.dnic.geoscan.ui.screens.remote

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dnic.geoscan.data.bluetooth.BluetoothConnectionState
import com.dnic.geoscan.data.bluetooth.BluetoothDeviceModel
import com.dnic.geoscan.ui.components.GlassmorphicCard
import com.dnic.geoscan.ui.visualizer.EChartsVisualizer
import com.dnic.geoscan.ui.visualizer.VisualizerViewMode
import com.dnic.geoscan.ui.theme.DarkBase
import com.dnic.geoscan.ui.theme.DarkSurface
import com.dnic.geoscan.ui.theme.DarkSurfaceElevated
import com.dnic.geoscan.ui.theme.GlassBorder
import com.dnic.geoscan.ui.theme.GlassBorderCyan
import com.dnic.geoscan.ui.theme.GlassBorderHighlighted
import com.dnic.geoscan.ui.theme.NeonCyan
import com.dnic.geoscan.ui.theme.NeonGreen
import com.dnic.geoscan.ui.theme.NeonRed
import com.dnic.geoscan.ui.theme.NeonYellow
import com.dnic.geoscan.ui.theme.StatusConnected
import com.dnic.geoscan.ui.theme.StatusDisconnected
import com.dnic.geoscan.ui.theme.StatusScanning
import com.dnic.geoscan.ui.theme.TelemetryTextStyle
import com.dnic.geoscan.ui.theme.TextMuted
import com.dnic.geoscan.ui.theme.TextPrimary
import com.dnic.geoscan.ui.theme.TextSecondary
import com.dnic.geoscan.ui.theme.Typography

@Composable
fun RemoteScanScreen(
    modifier: Modifier = Modifier,
    viewModel: RemoteScanViewModel = viewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val latestPoint by viewModel.latestPoint.collectAsState()
    val scannedPoints by viewModel.scannedPoints.collectAsState()
    val rawPacketPreview by viewModel.rawPacketPreview.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val isDevicePickerVisible by viewModel.isDevicePickerVisible.collectAsState()

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
            is BluetoothConnectionState.Disconnected -> StatusDisconnected
        },
        label = "statusColor"
    )

    // Pulse animation for active streaming or connection
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

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBase)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
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

            // Connection indicator circle with pulse
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .alpha(if (isConnected || isConnecting) pulseAlpha else 1f)
                    .background(statusColor)
            )
        }

        // Bluetooth Hardware Status & Pairing Card
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = if (isConnected) GlassBorderHighlighted else GlassBorder
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when {
                                isConnected -> Icons.Filled.BluetoothConnected
                                isConnecting -> Icons.Outlined.BluetoothSearching
                                else -> Icons.Filled.BluetoothDisabled
                            },
                            contentDescription = "Bluetooth Status",
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (val state = connectionState) {
                                is BluetoothConnectionState.Connected -> "CONNECTED: ${state.deviceName}"
                                is BluetoothConnectionState.Connecting -> "CONNECTING TO ${state.deviceName}..."
                                is BluetoothConnectionState.Error -> "ERROR: ${state.message.take(24)}"
                                is BluetoothConnectionState.Disconnected -> "RECEIVER DISCONNECTED"
                            },
                            style = Typography.labelLarge,
                            color = statusColor
                        )
                    }

                    Text(
                        text = "SPP UUID 1101",
                        style = Typography.labelSmall,
                        color = TextMuted
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Target Sensor", style = Typography.labelSmall, color = TextMuted)
                        val addressText = when (val state = connectionState) {
                            is BluetoothConnectionState.Connected -> state.deviceAddress
                            is BluetoothConnectionState.Connecting -> state.deviceAddress
                            else -> "No HC-05 Connected"
                        }
                        Text(text = addressText, style = Typography.bodyLarge, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Collected Points", style = Typography.labelSmall, color = TextMuted)
                        Text(
                            text = "${scannedPoints.size} pts",
                            style = TelemetryTextStyle,
                            color = NeonCyan,
                            fontSize = 15.sp
                        )
                    }
                }

                // Action Buttons: Device Select & Disconnect
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isConnected) {
                        OutlinedButton(
                            onClick = requestPermissionsAndOpenPicker,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                        ) {
                            Icon(imageVector = Icons.Outlined.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Connect HC-05", style = Typography.labelLarge)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.disconnectDevice() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.6f))
                        ) {
                            Text(text = "Disconnect", style = Typography.labelLarge)
                        }
                    }

                    // Start/Stop Scan Button
                    ElevatedButton(
                        onClick = { viewModel.toggleScan() },
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
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isScanning) "Stop Scan" else "Start Scan",
                            style = Typography.labelLarge
                        )
                    }
                }
            }
        }

        // Real-Time Telemetry ($X,Y,Z#) Stream Monitor
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = GlassBorderCyan
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LIVE PROTOCOL STREAM (\$X,Y,Z#)",
                            style = Typography.labelLarge,
                            color = NeonCyan
                        )
                    }

                    IconButton(
                        onClick = { viewModel.clearData() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Clear Data",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Grid Coordinates Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(
                        label = "X-AXIS (COL)",
                        value = latestPoint?.x?.toString() ?: "--",
                        unit = "col"
                    )
                    MetricItem(
                        label = "Y-AXIS (ROW)",
                        value = latestPoint?.y?.toString() ?: "--",
                        unit = "row"
                    )
                    MetricItem(
                        label = "Z-AXIS (uT)",
                        value = latestPoint?.zValue?.let { String.format("%.1f", it) } ?: "--",
                        unit = "µT",
                        valueColor = if ((latestPoint?.zValue ?: 0f) > 0) NeonRed else NeonCyan
                    )
                }

                // Raw Buffer Visualizer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "RAW: $rawPacketPreview",
                        style = TelemetryTextStyle,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 3D Visualizer Surface & 2D Heatmap Engine (Apache ECharts 3D)
        EChartsVisualizer(
            points = scannedPoints,
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
            initialMode = VisualizerViewMode.SURFACE_3D,
            baseline = 0f
        )
    }

    // Bluetooth Paired Devices Selection Dialog
    if (isDevicePickerVisible) {
        BluetoothDevicePickerDialog(
            devices = pairedDevices,
            onDeviceSelected = { device ->
                viewModel.connectDevice(device.address)
            },
            onRefresh = {
                viewModel.refreshPairedDevices()
            },
            onDismiss = {
                viewModel.closeDevicePicker()
            }
        )
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
            Text(text = value, style = TelemetryTextStyle, color = valueColor, fontSize = 20.sp)
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
