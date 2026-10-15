package com.denic.geoscan.ui.screens.hub

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denic.geoscan.ui.components.GlassmorphicCard
import com.denic.geoscan.ui.navigation.Screen
import com.denic.geoscan.ui.theme.DarkBase
import com.denic.geoscan.ui.theme.DarkSurface
import com.denic.geoscan.ui.theme.DarkSurfaceElevated
import com.denic.geoscan.ui.theme.GlassBorder
import com.denic.geoscan.ui.theme.GlassBorderCyan
import com.denic.geoscan.ui.theme.GlassBorderHighlighted
import com.denic.geoscan.ui.theme.NeonCyan
import com.denic.geoscan.ui.theme.NeonGreen
import com.denic.geoscan.ui.theme.NeonOrange
import com.denic.geoscan.ui.theme.NeonRed
import com.denic.geoscan.ui.theme.NeonYellow
import com.denic.geoscan.ui.theme.StatusDisconnected
import com.denic.geoscan.ui.theme.TelemetryTextStyle
import com.denic.geoscan.ui.theme.TextMuted
import com.denic.geoscan.ui.theme.TextPrimary
import com.denic.geoscan.ui.theme.TextSecondary
import com.denic.geoscan.ui.theme.Typography

/**
 * High-performance, instant-loading Landing Hub Screen.
 * Prioritizes Local Analysis while showing clear, non-blocking hardware connectivity status.
 */
@Composable
fun HubScreen(
    onNavigateTo: (Screen) -> Unit,
    onNavigateToGridDemo: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBase)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Mission Header
        HubHeader()

        // Prominent Hardware Status Banner: Explicit Disconnected State
        HardwareStatusBanner(
            pulseAlpha = pulseAlpha,
            onClick = { onNavigateTo(Screen.RemoteScan) }
        )

        // Hero Card 1: Local Analysis (Primary & Recommended for Offline Survey Files)
        LocalAnalysisHeroCard(
            onClick = { onNavigateTo(Screen.LocalAnalysis) },
            onGridDemoClick = onNavigateToGridDemo
        )

        // Hero Card 2: Remote Scan (Real-time SPP Bluetooth Stream)
        RemoteScanHeroCard(
            onClick = { onNavigateTo(Screen.RemoteScan) }
        )

        // Geophysical Processing Engine Specifications Footer
        GeophysicalEngineSpecsCard()
    }
}

@Composable
private fun HubHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "MISSION CONTROL",
            style = Typography.labelSmall,
            color = NeonCyan,
            letterSpacing = 2.sp
        )
        Text(
            text = "Geophysical Survey Hub",
            style = Typography.displayMedium,
            color = TextPrimary
        )
        Text(
            text = "Select an operation mode to begin spatial magnetic inspection",
            style = Typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun HardwareStatusBanner(
    pulseAlpha: Float,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth().then(clickModifier),
        borderColor = NeonOrange.copy(alpha = 0.5f),
        backgroundColor = DarkSurface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .alpha(pulseAlpha)
                        .background(NeonOrange)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "STATUS: DISCONNECTED",
                            style = Typography.labelLarge,
                            color = NeonOrange,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• NO HARDWARE CONNECTED",
                            style = Typography.labelSmall,
                            color = NeonOrange.copy(alpha = 0.85f)
                        )
                    }
                    Text(
                        text = "Bluetooth Standby (SPP 1101) • Local Analysis Ready & Fully Operational",
                        style = Typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Outlined.BluetoothDisabled,
                contentDescription = null,
                tint = NeonOrange.copy(alpha = 0.8f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun LocalAnalysisHeroCard(
    onClick: () -> Unit,
    onGridDemoClick: ((Int) -> Unit)? = null
) {
    val cardShape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(NeonCyan, NeonGreen.copy(alpha = 0.5f))
                ),
                shape = cardShape
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkSurfaceElevated.copy(alpha = 0.95f),
                        DarkSurface.copy(alpha = 0.98f)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = "Local Analysis",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "LOCAL ANALYSIS",
                            style = Typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Offline Magnetometer Data Inspector",
                            style = Typography.labelSmall,
                            color = NeonCyan
                        )
                    }
                }

                // Recommended Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonGreen.copy(alpha = 0.2f))
                        .border(1.dp, NeonGreen.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "100% OFFLINE",
                        style = Typography.labelSmall,
                        color = NeonGreen,
                        fontSize = 10.sp
                    )
                }
            }

            Text(
                text = "Load and inspect gridded survey datasets (10×10, 20×20, custom matrix, CSV/TXT) directly from storage. Runs Median Ground Zero baseline normalization and 3D bicubic rendering without requiring Bluetooth.",
                style = Typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            // Feature Highlights Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeaturePill(text = "10×10 / 20×20 Grids", color = NeonCyan)
                FeaturePill(text = "Median Normalization", color = NeonGreen)
                FeaturePill(text = "Bicubic Spline", color = TextPrimary)
            }

            // Quick Demo Shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (onGridDemoClick != null) onGridDemoClick(5) else onClick()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonCyan,
                        containerColor = NeonCyan.copy(alpha = 0.08f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderCyan)
                ) {
                    Text(text = "⚡ 5×5", style = Typography.labelSmall)
                }
                OutlinedButton(
                    onClick = {
                        if (onGridDemoClick != null) onGridDemoClick(10) else onClick()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonCyan,
                        containerColor = NeonCyan.copy(alpha = 0.08f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderCyan)
                ) {
                    Text(text = "⚡ 10×10", style = Typography.labelSmall)
                }
                OutlinedButton(
                    onClick = {
                        if (onGridDemoClick != null) onGridDemoClick(20) else onClick()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonYellow,
                        containerColor = NeonYellow.copy(alpha = 0.08f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderHighlighted)
                ) {
                    Text(text = "⚡ 20×20", style = Typography.labelSmall)
                }
                OutlinedButton(
                    onClick = {
                        if (onGridDemoClick != null) onGridDemoClick(30) else onClick()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonGreen,
                        containerColor = NeonGreen.copy(alpha = 0.08f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                ) {
                    Text(text = "⚡ 30×30", style = Typography.labelSmall)
                }
            }

            // Launch Action Button
            ElevatedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = NeonCyan,
                    contentColor = DarkBase
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Local File & Analyze",
                    style = Typography.labelLarge,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RemoteScanHeroCard(onClick: () -> Unit) {
    val cardShape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(GlassBorder, GlassBorderCyan)
                ),
                shape = cardShape
            )
            .background(DarkSurface.copy(alpha = 0.85f))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Bluetooth,
                            contentDescription = "Remote Scan",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "REMOTE SCAN",
                            style = Typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "HC-05 Live SPP Telemetry",
                            style = Typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, GlassBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SPP STREAM",
                        style = Typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Text(
                text = "Connect to an HC-05 Bluetooth module to stream and capture live magnetometer readings (\$X,Y,Z#). Real-time discrete grid accumulation with session CSV export to Documents.",
                style = Typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            // Feature Highlights Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeaturePill(text = "SPP UUID 1101", color = TextMuted)
                FeaturePill(text = "Noise Filter", color = TextMuted)
                FeaturePill(text = "CSV Export", color = TextMuted)
            }

            // Launch Action Button
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NeonCyan
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderCyan)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Sensors,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Launch Remote Scanner",
                    style = Typography.labelLarge,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun FeaturePill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkBase)
            .border(1.dp, GlassBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = Typography.labelSmall,
            color = color,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun GeophysicalEngineSpecsCard() {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = GlassBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "GEOPHYSICAL PROCESSING PIPELINE",
                style = Typography.labelSmall,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Ground Zero:", style = Typography.labelSmall, color = TextMuted)
                    Text(text = "Median Anomaly Delta", style = TelemetryTextStyle, color = NeonGreen)
                }
                Column {
                    Text(text = "Smoothing Engine:", style = Typography.labelSmall, color = TextMuted)
                    Text(text = "Catmull-Rom Bicubic", style = TelemetryTextStyle, color = NeonCyan)
                }
                Column {
                    Text(text = "Renderer:", style = Typography.labelSmall, color = TextMuted)
                    Text(text = "Apache ECharts GL", style = TelemetryTextStyle, color = TextPrimary)
                }
            }
        }
    }
}
