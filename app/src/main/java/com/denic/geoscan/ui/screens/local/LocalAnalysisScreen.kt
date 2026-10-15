package com.denic.geoscan.ui.screens.local

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileUpload
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
import com.denic.geoscan.data.model.ScanStatistics
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
import com.denic.geoscan.ui.theme.TelemetryTextStyle
import com.denic.geoscan.ui.theme.TextMuted
import com.denic.geoscan.ui.theme.TextPrimary
import com.denic.geoscan.ui.theme.TextSecondary
import com.denic.geoscan.ui.theme.Typography
import com.denic.geoscan.ui.visualizer.EChartsVisualizer
import com.denic.geoscan.ui.visualizer.VisualizerViewMode

@Composable
fun LocalAnalysisScreen(
    initialGridSize: Int? = null,
    modifier: Modifier = Modifier,
    viewModel: LocalAnalysisViewModel = viewModel()
) {
    androidx.compose.runtime.LaunchedEffect(initialGridSize) {
        if (initialGridSize != null) {
            viewModel.loadSampleGrid(initialGridSize, initialGridSize)
        }
    }
    val dataset by viewModel.dataset.collectAsState()
    val visualizerPoints by viewModel.visualizerPoints.collectAsState()
    val isSmoothingEnabled by viewModel.isSmoothingEnabled.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // SAF File Picker
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
                // Left Column: Controls & Metrics
                Column(
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ScreenHeader()

                    ErrorBanner(errorMessage = errorMessage)

                    FileSourceCard(
                        dataset = dataset,
                        isLoading = isLoading,
                        isSmoothingEnabled = isSmoothingEnabled,
                        onOpenPicker = openPicker,
                        onClearDataset = { viewModel.clearDataset() },
                        onToggleSmoothing = { viewModel.toggleSmoothing() },
                        onLoadSampleGrid = { w, h -> viewModel.loadSampleGrid(w, h) }
                    )

                    dataset?.let { data ->
                        GroundZeroBaselineCard(stats = data.statistics)
                    }

                    SpectrumPaletteCard()
                }

                // Right Column: Full-Height 3D Visualizer Canvas
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
                ScreenHeader()

                ErrorBanner(errorMessage = errorMessage)

                FileSourceCard(
                    dataset = dataset,
                    isLoading = isLoading,
                    isSmoothingEnabled = isSmoothingEnabled,
                    onOpenPicker = openPicker,
                    onClearDataset = { viewModel.clearDataset() },
                    onToggleSmoothing = { viewModel.toggleSmoothing() },
                    onLoadSampleGrid = { w, h -> viewModel.loadSampleGrid(w, h) }
                )

                dataset?.let { data ->
                    GroundZeroBaselineCard(stats = data.statistics)
                }

                SpectrumPaletteCard()

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
}

@Composable
private fun ScreenHeader() {
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
}

@Composable
private fun ErrorBanner(errorMessage: String?) {
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
}

@Composable
private fun FileSourceCard(
    dataset: com.denic.geoscan.data.model.ScanDataset?,
    isLoading: Boolean,
    isSmoothingEnabled: Boolean,
    onOpenPicker: () -> Unit,
    onClearDataset: () -> Unit,
    onToggleSmoothing: () -> Unit,
    onLoadSampleGrid: (Int, Int) -> Unit
) {
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
                    Text(text = "TXT / CSV", style = Typography.labelSmall, color = TextMuted)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Active File", style = Typography.labelSmall, color = TextMuted)
                        Text(
                            text = dataset?.fileName ?: "No scan loaded",
                            style = Typography.bodyLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = if (dataset != null) {
                                "${dataset.fileSizeFormatted} • ${dataset.statistics.totalPoints} pts • ${dataset.statistics.gridWidth}×${dataset.statistics.gridHeight}"
                            } else {
                                "Expected format: X,Y,Z triplets or 2D matrix"
                            },
                            style = Typography.labelSmall,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                    } else if (dataset == null) {
                        ElevatedButton(
                            onClick = onOpenPicker,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = NeonCyan,
                                contentColor = DarkBase
                            )
                        ) {
                            Icon(imageVector = Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Open File", style = Typography.labelLarge)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = onOpenPicker,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                            ) {
                                Text(text = "Change", style = Typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = onClearDataset, modifier = Modifier.size(30.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Demo Survey Grid (Offline test without external files)
            if (dataset == null && !isLoading) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "QUICK OFFLINE SURVEY DEMO (TAP TO TEST)",
                        style = Typography.labelSmall,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onLoadSampleGrid(10, 10) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NeonCyan,
                                containerColor = NeonCyan.copy(alpha = 0.08f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderCyan)
                        ) {
                            Text(text = "⚡ 10×10", style = Typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { onLoadSampleGrid(20, 20) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NeonYellow,
                                containerColor = NeonYellow.copy(alpha = 0.08f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderHighlighted)
                        ) {
                            Text(text = "⚡ 20×20", style = Typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { onLoadSampleGrid(30, 30) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NeonGreen,
                                containerColor = NeonGreen.copy(alpha = 0.08f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                        ) {
                            Text(text = "⚡ 30×30", style = Typography.labelSmall)
                        }
                    }
                }
            }

            // Bicubic Smoothing Filter Toggle (when dataset is loaded)
            if (dataset != null) {
                OutlinedButton(
                    onClick = onToggleSmoothing,
                    modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier.size(16.dp),
                        tint = if (isSmoothingEnabled) NeonGreen else TextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSmoothingEnabled) "Bicubic Smoothing Active (2x Mesh)" else "Enable Bicubic Smoothing Filter",
                        style = Typography.labelLarge
                    )
                }
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GROUND ZERO BASELINE (MEDIAN)",
                        style = Typography.labelLarge,
                        color = NeonGreen
                    )
                }

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
                        fontSize = 13.sp
                    )
                }
            }

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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .padding(10.dp)
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
                            fontSize = 14.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Deepest Void Anomaly", style = Typography.labelSmall, color = TextMuted)
                        Text(
                            text = "${String.format("%.1f", stats.minNormalized)} µT",
                            style = TelemetryTextStyle,
                            color = NeonCyan,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpectrumPaletteCard() {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = GlassBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "MAGNETIC SPECTRUM PALETTE", style = Typography.labelLarge, color = TextPrimary)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
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
                Text(text = "-LOW (Cavity)", style = Typography.labelSmall, color = NeonCyan)
                Text(text = "0 (Ground Zero)", style = Typography.labelSmall, color = NeonGreen)
                Text(text = "+HIGH (Metal)", style = Typography.labelSmall, color = NeonRed)
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
            Text(text = "$count", style = TelemetryTextStyle, color = TextPrimary, fontSize = 15.sp)
            Text(text = "$percentage%", style = Typography.labelSmall, color = TextMuted, fontSize = 10.sp)
        }
    }
}
