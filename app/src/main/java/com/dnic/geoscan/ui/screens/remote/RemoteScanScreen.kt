package com.dnic.geoscan.ui.screens.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnic.geoscan.ui.components.GlassmorphicCard
import com.dnic.geoscan.ui.theme.DarkBase
import com.dnic.geoscan.ui.theme.DarkSurface
import com.dnic.geoscan.ui.theme.GlassBorderCyan
import com.dnic.geoscan.ui.theme.GlassBorderHighlighted
import com.dnic.geoscan.ui.theme.NeonCyan
import com.dnic.geoscan.ui.theme.NeonGreen
import com.dnic.geoscan.ui.theme.NeonRed
import com.dnic.geoscan.ui.theme.StatusDisconnected
import com.dnic.geoscan.ui.theme.TelemetryTextStyle
import com.dnic.geoscan.ui.theme.TextMuted
import com.dnic.geoscan.ui.theme.TextPrimary
import com.dnic.geoscan.ui.theme.TextSecondary
import com.dnic.geoscan.ui.theme.Typography

@Composable
fun RemoteScanScreen(
    modifier: Modifier = Modifier,
    onConnectClick: () -> Unit = {},
    onStartScanClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBase)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
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

            // Real-time Pulse Indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(StatusDisconnected)
            )
        }

        // Bluetooth Hardware Status Card
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = GlassBorderHighlighted
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.BluetoothSearching,
                            contentDescription = "Bluetooth Status",
                            tint = NeonGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RECEIVER STATUS",
                            style = Typography.labelLarge,
                            color = NeonGreen
                        )
                    }

                    Text(
                        text = "STANDBY",
                        style = Typography.labelSmall,
                        color = TextMuted
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Paired Device", style = Typography.bodyMedium, color = TextMuted)
                        Text(text = "No HC-05 Selected", style = Typography.titleMedium, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Baud Rate", style = Typography.bodyMedium, color = TextMuted)
                        Text(text = "9600 bps (SPP)", style = Typography.titleMedium, color = NeonCyan)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onConnectClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                    ) {
                        Text(text = "Scan Devices", style = Typography.labelLarge)
                    }

                    ElevatedButton(
                        onClick = onStartScanClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = NeonGreen,
                            contentColor = DarkBase
                        )
                    ) {
                        Text(text = "Start Stream", style = Typography.labelLarge)
                    }
                }
            }
        }

        // Live Telemetry Readout ($X,Y,Z# Protocol)
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
                            contentDescription = "Telemetry Protocol",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STREAM MONITOR (\$X,Y,Z#)",
                            style = Typography.labelLarge,
                            color = NeonCyan
                        )
                    }

                    Text(
                        text = "CH1 ACTIVE",
                        style = Typography.labelSmall,
                        color = TextMuted
                    )
                }

                // Monospace Coordinate Telemetry Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(label = "X-AXIS (COL)", value = "00", unit = "pts")
                    MetricItem(label = "Y-AXIS (ROW)", value = "00", unit = "pts")
                    MetricItem(label = "Z-AXIS (uT)", value = "+000.0", unit = "nT", valueColor = NeonRed)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "RAW: \$00,00,000.0#\\n (Awaiting packet...)",
                        style = TelemetryTextStyle,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 3D Visualizer Surface Viewport Placeholder (Target for ECharts 3D in Phase 3)
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            borderColor = GlassBorderHighlighted
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ViewInAr,
                    contentDescription = "3D Visualizer Placeholder",
                    tint = NeonGreen.copy(alpha = 0.7f),
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "3D MAGNETIC SURFACE VIEWPORT",
                    style = Typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Apache ECharts 3D Engine ready for Phase 3 integration",
                    style = Typography.bodyMedium,
                    color = TextSecondary
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
