package com.dnic.geoscan.ui.screens.local

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Grid4x4
import androidx.compose.material.icons.outlined.Layers
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dnic.geoscan.data.model.ScanDataset
import com.dnic.geoscan.data.model.ScanStatistics
import com.dnic.geoscan.ui.components.GlassmorphicCard
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
import com.dnic.geoscan.ui.theme.TelemetryTextStyle
import com.dnic.geoscan.ui.theme.TextMuted
import com.dnic.geoscan.ui.theme.TextPrimary
import com.dnic.geoscan.ui.theme.TextSecondary
import com.dnic.geoscan.ui.theme.Typography

@Composable
fun LocalAnalysisScreen(
    modifier: Modifier = Modifier,
    viewModel: LocalAnalysisViewModel = viewModel()
) {
    val dataset by viewModel.dataset.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Storage Access Framework (SAF) document picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.loadFileFromUri(uri)
        }
    }

    val openPicker = {
        filePickerLauncher.launch(
            arrayOf(
                "text/plain",
                "text/csv",
                "text/comma-separated-values",
                "application/octet-stream",
                "*/*"
            )
        )
    }

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
        Column {
            Text(
                text = "LOCAL ANALYSIS",
                style = Typography.displayMedium,
                color = TextPrimary
            )
            Text(
                text = "Geophysical Dataset Offline Inspector (SAF)",
                style = Typography.titleMedium,
                color = TextSecondary
            )
        }

        // Error message banner
        AnimatedVisibility(visible = errorMessage != null) {
            errorMessage?.let { error ->
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = NeonRed
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = "Error",
                            tint = NeonRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = error,
                            style = Typography.bodyMedium,
                            color = NeonRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // File Selector & Import Card
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = if (dataset != null) GlassBorderHighlighted else GlassBorderCyan
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = "File Ingestion",
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DATASET SOURCE",
                            style = Typography.labelLarge,
                            color = NeonCyan
                        )
                    }

                    if (dataset != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Loaded",
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PARSED & NORMALIZED",
                                style = Typography.labelSmall,
                                color = NeonGreen
                            )
                        }
                    } else {
                        Text(
                            text = "TXT / CSV",
                            style = Typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active File",
                                style = Typography.labelSmall,
                                color = TextMuted
                            )
                            Text(
                                text = dataset?.fileName ?: "No scan loaded",
                                style = Typography.bodyLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = if (dataset != null) {
                                    "${dataset!!.fileSizeFormatted} • ${dataset!!.statistics.totalPoints} points • Grid: ${dataset!!.statistics.gridWidth}×${dataset!!.statistics.gridHeight}"
                                } else {
                                    "Expected format: X,Y,Z triplets or 2D value matrix"
                                },
                                style = Typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = NeonCyan,
                                strokeWidth = 2.dp
                            )
                        } else if (dataset == null) {
                            ElevatedButton(
                                onClick = openPicker,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = NeonCyan,
                                    contentColor = DarkBase
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FileUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Open File", style = Typography.labelLarge)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = openPicker,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                                ) {
                                    Text(text = "Change", style = Typography.labelSmall)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { viewModel.clearDataset() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Baseline Normalization & Anomaly Metrics Card (Visible when dataset is loaded)
        if (dataset != null) {
            val stats = dataset!!.statistics
            GroundZeroBaselineCard(stats = stats)
        }

        // Magnetic Spectrum Gradient Scale Bar
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = GlassBorder
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "MAGNETIC SPECTRUM PALETTE",
                    style = Typography.labelLarge,
                    color = TextPrimary
                )

                // Neon Magnetic Color Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(NeonCyan, NeonGreen, NeonYellow, NeonRed)
                            )
                        )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "-LOW (Cavity / Void)", style = Typography.labelSmall, color = NeonCyan)
                    Text(text = "0 (Ground Zero)", style = Typography.labelSmall, color = NeonGreen)
                    Text(text = "+HIGH (Ferrous Metal)", style = Typography.labelSmall, color = NeonRed)
                }
            }
        }

        // Offline 3D Surface Viewport Placeholder
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            borderColor = if (dataset != null) GlassBorderCyan else GlassBorder
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Layers,
                    contentDescription = "Offline 3D Viewport Placeholder",
                    tint = if (dataset != null) NeonCyan else NeonCyan.copy(alpha = 0.5f),
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (dataset != null) {
                        "${dataset!!.points.size} NORMALIZED POINTS READY FOR 3D"
                    } else {
                        "OFFLINE 3D VISUALIZATION CANVAS"
                    },
                    style = Typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (dataset != null) {
                        "Median baseline calculated (${String.format("%.1f", dataset!!.statistics.groundZeroMedian)} µT). Ready for ECharts 3D."
                    } else {
                        "Open a CSV/TXT file to calculate Ground Zero and render 3D surface"
                    },
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun GroundZeroBaselineCard(stats: ScanStatistics) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = GlassBorderHighlighted
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Analytics,
                        contentDescription = "Ground Zero",
                        tint = NeonGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GROUND ZERO BASELINE (MEDIAN)",
                        style = Typography.labelLarge,
                        color = NeonGreen
                    )
                }

                // Median Highlight Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonGreen.copy(alpha = 0.15f))
                        .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${String.format("%.1f", stats.groundZeroMedian)} µT",
                        style = TelemetryTextStyle,
                        color = NeonGreen,
                        fontSize = 14.sp
                    )
                }
            }

            // Anomaly Classification Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnomalyStatBadge(
                    label = "POS. METALS",
                    count = stats.metalCount,
                    total = stats.totalPoints,
                    color = NeonRed,
                    modifier = Modifier.weight(1f)
                )
                AnomalyStatBadge(
                    label = "NOMINAL SOIL",
                    count = stats.neutralCount,
                    total = stats.totalPoints,
                    color = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
                AnomalyStatBadge(
                    label = "NEG. CAVITIES",
                    count = stats.cavityCount,
                    total = stats.totalPoints,
                    color = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
            }

            // High/Low Extremes Breakdown
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Max Metal Anomaly (Peak)", style = Typography.labelSmall, color = TextMuted)
                        Text(
                            text = "+${String.format("%.1f", stats.maxNormalized)} µT",
                            style = TelemetryTextStyle,
                            color = NeonRed,
                            fontSize = 15.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Deepest Void Anomaly", style = Typography.labelSmall, color = TextMuted)
                        Text(
                            text = "${String.format("%.1f", stats.minNormalized)} µT",
                            style = TelemetryTextStyle,
                            color = NeonCyan,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnomalyStatBadge(
    label: String,
    count: Int,
    total: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val percentage = if (total > 0) (count * 100f / total).toInt() else 0

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(text = label, style = Typography.labelSmall, color = color, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "$count", style = TelemetryTextStyle, color = TextPrimary, fontSize = 16.sp)
            Text(text = "$percentage%", style = Typography.labelSmall, color = TextMuted, fontSize = 10.sp)
        }
    }
}
