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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.outlined.Settings
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
import com.denic.geoscan.ui.localization.LocalAppStrings
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
    preferredStartScreen: String? = null,
    onToggleDefaultStartScreen: ((String) -> Unit)? = null,
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBase)
    ) {
        val isTabletLandscape = maxWidth >= 720.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1100.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (isTabletLandscape) 28.dp else 18.dp,
                    vertical = 14.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Mission Header
            HubHeader(
                isLocalDefault = (preferredStartScreen == Screen.LocalAnalysis.route),
                onToggleDefaultLocal = {
                    val next = if (preferredStartScreen == Screen.LocalAnalysis.route) Screen.Hub.route else Screen.LocalAnalysis.route
                    onToggleDefaultStartScreen?.invoke(next)
                }
            )

            // Instant Two-Mode Selector (Direct 1-tap navigation)
            QuickModeSelector(onNavigateTo = onNavigateTo)

            // Prominent Hardware Status Banner: Explicit Disconnected State
            HardwareStatusBanner(
                pulseAlpha = pulseAlpha,
                onNavigateToLocal = { onNavigateTo(Screen.LocalAnalysis) },
                onConnectHardware = { onNavigateTo(Screen.RemoteScan) }
            )

            if (isTabletLandscape) {
                // Adaptive Two-Column Grid for Tablets & Large Screens
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        LocalAnalysisHeroCard(
                            onClick = { onNavigateTo(Screen.LocalAnalysis) },
                            onGridDemoClick = onNavigateToGridDemo
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        RemoteScanHeroCard(
                            onClick = { onNavigateTo(Screen.RemoteScan) }
                        )
                    }
                }
                SettingsHeroCard(
                    onClick = { onNavigateTo(Screen.Settings) }
                )
            } else {
                // Single Column Stack for Smartphones
                LocalAnalysisHeroCard(
                    onClick = { onNavigateTo(Screen.LocalAnalysis) },
                    onGridDemoClick = onNavigateToGridDemo
                )

                RemoteScanHeroCard(
                    onClick = { onNavigateTo(Screen.RemoteScan) }
                )

                SettingsHeroCard(
                    onClick = { onNavigateTo(Screen.Settings) }
                )
            }

            // Geophysical Processing Engine Specifications Footer
            GeophysicalEngineSpecsCard()
        }
    }
}

@Composable
private fun QuickModeSelector(
    onNavigateTo: (Screen) -> Unit
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Direct Action 1: Local Analysis (Offline)
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(NeonGreen.copy(alpha = 0.12f))
                .border(1.5.dp, NeonGreen.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .clickable { onNavigateTo(Screen.LocalAnalysis) }
                .padding(vertical = 12.dp, horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = strings.hubLocalCardTitle,
                        style = Typography.labelLarge,
                        color = NeonGreen,
                        fontSize = 11.sp
                    )
                    Text(
                        text = strings.hubLocalCardSubtitle,
                        style = Typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }
        }

        // Direct Action 2: Remote Scan (Hardware)
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceElevated)
                .border(1.dp, GlassBorderCyan, RoundedCornerShape(12.dp))
                .clickable { onNavigateTo(Screen.RemoteScan) }
                .padding(vertical = 12.dp, horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Sensors,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = strings.hubRemoteCardTitle,
                        style = Typography.labelLarge,
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                    Text(
                        text = strings.hubRemoteCardSubtitle,
                        style = Typography.labelSmall,
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HubHeader(
    isLocalDefault: Boolean = false,
    onToggleDefaultLocal: (() -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = strings.hubMissionControl,
                    style = Typography.labelSmall,
                    color = NeonCyan,
                    letterSpacing = 2.sp
                )
                Text(
                    text = strings.hubSubtitle,
                    style = Typography.displayMedium,
                    color = TextPrimary
                )
            }
            if (onToggleDefaultLocal != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLocalDefault) NeonGreen.copy(alpha = 0.15f) else DarkSurface)
                        .border(
                            1.dp,
                            if (isLocalDefault) NeonGreen.copy(alpha = 0.6f) else GlassBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onToggleDefaultLocal() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isLocalDefault) NeonGreen else TextMuted)
                        )
                        Text(
                            text = if (isLocalDefault) strings.hubSetDefaultLocal else strings.hubSetDefaultHub,
                            style = Typography.labelSmall,
                            color = if (isLocalDefault) NeonGreen else TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
        Text(
            text = strings.hubDefaultStartNotice,
            style = Typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun HardwareStatusBanner(
    pulseAlpha: Float,
    onNavigateToLocal: () -> Unit,
    onConnectHardware: () -> Unit
) {
    val strings = LocalAppStrings.current
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = NeonOrange.copy(alpha = 0.6f),
        backgroundColor = DarkSurface
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                text = strings.hubHardwareDisconnected,
                                style = Typography.labelLarge,
                                color = NeonOrange,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = strings.hubHardwareStatusDesc,
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

            // Direct 1-tap quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToLocal,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonGreen,
                        containerColor = NeonGreen.copy(alpha = 0.08f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.hubOpenLocalBtn, style = Typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onConnectHardware,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonCyan,
                        containerColor = NeonCyan.copy(alpha = 0.08f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderCyan)
                ) {
                    Icon(imageVector = Icons.Outlined.Sensors, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.hubConnectHardwareBtn, style = Typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun LocalAnalysisHeroCard(
    onClick: () -> Unit,
    onGridDemoClick: ((Int) -> Unit)? = null
) {
    val strings = LocalAppStrings.current
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
                            text = strings.hubLocalCardTitle,
                            style = Typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = strings.hubLocalCardSubtitle,
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
                text = strings.hubLocalCardDesc,
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
                    text = strings.hubOpenLocalBtn,
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
    val strings = LocalAppStrings.current
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
                            text = strings.hubRemoteCardTitle,
                            style = Typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = strings.hubRemoteCardSubtitle,
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
                text = strings.hubRemoteCardDesc,
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
                    text = strings.hubConnectHardwareBtn,
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
private fun SettingsHeroCard(
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        borderColor = NeonYellow.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                            .background(NeonYellow.copy(alpha = 0.15f))
                            .border(1.dp, NeonYellow.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = NeonYellow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "OPTION C • ",
                                style = Typography.labelSmall,
                                color = NeonYellow,
                                fontSize = 10.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Text(
                                text = strings.settingsTitle,
                                style = Typography.titleLarge,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = strings.settingsSubtitle,
                            style = Typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonYellow.copy(alpha = 0.12f))
                        .border(1.dp, NeonYellow.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "DIAGNOSTICS",
                        style = Typography.labelSmall,
                        color = NeonYellow,
                        fontSize = 10.sp
                    )
                }
            }

            Text(
                text = strings.settingsDebugLogDesc,
                style = Typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            // Feature Highlights Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeaturePill(text = "denic_debug_log.txt", color = NeonYellow)
                FeaturePill(text = "Bilingual i18n", color = NeonCyan)
                FeaturePill(text = "Live Log Stream", color = NeonGreen)
            }

            // Launch Action Button
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NeonYellow
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonYellow.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.settingsTitle,
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
    val strings = LocalAppStrings.current
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = GlassBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = strings.hubSpecsTitle,
                style = Typography.labelSmall,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = strings.hubSpecGroundZero, style = Typography.labelSmall, color = TextMuted)
                    Text(text = "Median Delta (µT)", style = TelemetryTextStyle, color = NeonGreen)
                }
                Column {
                    Text(text = strings.hubSpecBicubic, style = Typography.labelSmall, color = TextMuted)
                    Text(text = "Catmull-Rom Spline", style = TelemetryTextStyle, color = NeonCyan)
                }
                Column {
                    Text(text = strings.hubSpecProjections, style = Typography.labelSmall, color = TextMuted)
                    Text(text = "Apache ECharts GL", style = TelemetryTextStyle, color = TextPrimary)
                }
            }
        }
    }
}
