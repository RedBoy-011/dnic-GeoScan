package com.denic.geoscan.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denic.geoscan.data.analysis.GeophysicalDepthEstimator
import com.denic.geoscan.data.model.ScanPoint
import com.denic.geoscan.ui.theme.DarkBase
import com.denic.geoscan.ui.theme.DarkSurface
import com.denic.geoscan.ui.theme.GlassBorder
import com.denic.geoscan.ui.theme.NeonCyan
import com.denic.geoscan.ui.theme.NeonGreen
import com.denic.geoscan.ui.theme.NeonOrange
import com.denic.geoscan.ui.theme.NeonRed
import com.denic.geoscan.ui.theme.NeonYellow
import com.denic.geoscan.ui.theme.TelemetryTextStyle
import com.denic.geoscan.ui.theme.TextMuted
import com.denic.geoscan.ui.theme.TextSecondary
import com.denic.geoscan.ui.theme.Typography
import kotlin.math.abs

@Composable
fun MagneticCrossSectionCard(
    points: List<ScanPoint>,
    gridWidth: Int,
    gridHeight: Int,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    var isRowSlice by remember { mutableStateOf(true) }
    var sliceIndex by remember { mutableIntStateOf(gridHeight / 2) }
    var stationSpacingMeters by remember { mutableFloatStateOf(1.0f) }

    // Clamp sliceIndex within valid boundaries
    val maxIndex = if (isRowSlice) (gridHeight - 1).coerceAtLeast(0) else (gridWidth - 1).coerceAtLeast(0)
    val currentIndex = sliceIndex.coerceIn(0, maxIndex)

    // Filter points belonging to the chosen transect
    val slicePoints = if (isRowSlice) {
        points.filter { it.y == currentIndex }.sortedBy { it.x }
    } else {
        points.filter { it.x == currentIndex }.sortedBy { it.y }
    }

    if (slicePoints.size < 2) return

    val minZ = slicePoints.minOf { it.zValue }
    val maxZ = slicePoints.maxOf { it.zValue }
    val spanZ = (maxZ - minZ).coerceAtLeast(0.1f)
    val peakZ = slicePoints.maxByOrNull { abs(it.zValue) }?.zValue ?: 0f

    // Geophysical Depth Estimation
    val depthEstimator = remember { GeophysicalDepthEstimator() }
    val depthResult = remember(slicePoints, stationSpacingMeters) {
        depthEstimator.estimateDepth(slicePoints, stationSpacingMeters)
    }

    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = NeonCyan.copy(alpha = 0.5f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.ShowChart,
                        contentDescription = "Cross-Section",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "2D TRANSECT PROFILE",
                        style = Typography.labelLarge,
                        color = NeonCyan
                    )
                }

                // Slice Axis Toggle: Row (X-Profile) vs Col (Y-Profile)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SlicePill(
                        text = "Row Y=$currentIndex",
                        isSelected = isRowSlice,
                        onClick = {
                            isRowSlice = true
                            sliceIndex = sliceIndex.coerceIn(0, (gridHeight - 1).coerceAtLeast(0))
                        }
                    )
                    SlicePill(
                        text = "Col X=$currentIndex",
                        isSelected = !isRowSlice,
                        onClick = {
                            isRowSlice = false
                            sliceIndex = sliceIndex.coerceIn(0, (gridWidth - 1).coerceAtLeast(0))
                        }
                    )
                }
            }

            // Slider to scrub across survey grid
            if (maxIndex > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRowSlice) "Y Line:" else "X Line:",
                        style = Typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.width(50.dp)
                    )
                    Slider(
                        value = currentIndex.toFloat(),
                        onValueChange = { sliceIndex = it.toInt() },
                        valueRange = 0f..maxIndex.toFloat(),
                        steps = (maxIndex - 1).coerceAtLeast(0),
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = GlassBorder
                        )
                    )
                    Text(
                        text = "$currentIndex / $maxIndex",
                        style = TelemetryTextStyle,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.width(45.dp)
                    )
                }
            }

            // Survey Station Spacing Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Station Spacing (Δx):",
                    style = Typography.labelSmall,
                    color = TextMuted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.5f to "0.5m", 1.0f to "1.0m", 2.0f to "2.0m").forEach { (spacing, label) ->
                        SlicePill(
                            text = label,
                            isSelected = stationSpacingMeters == spacing,
                            onClick = { stationSpacingMeters = spacing }
                        )
                    }
                }
            }

            // High-Performance Native Compose Canvas Profile Graph
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBase)
                    .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    val w = size.width
                    val h = size.height

                    // Draw baseline Z=0 reference line
                    val zeroY = if (minZ <= 0f && maxZ >= 0f) {
                        h - ((0f - minZ) / spanZ) * h
                    } else if (minZ > 0f) {
                        h
                    } else {
                        0f
                    }

                    drawLine(
                        color = NeonGreen.copy(alpha = 0.5f),
                        start = Offset(0f, zeroY),
                        end = Offset(w, zeroY),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw curve line
                    val path = Path()
                    slicePoints.forEachIndexed { i, pt ->
                        val px = (i.toFloat() / (slicePoints.size - 1).coerceAtLeast(1)) * w
                        val py = h - ((pt.zValue - minZ) / spanZ) * h

                        if (i == 0) {
                            path.moveTo(px, py)
                        } else {
                            path.lineTo(px, py)
                        }

                        // Draw node dot
                        drawCircle(
                            color = if (pt.zValue >= 0f) NeonRed else NeonCyan,
                            radius = 2.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }

                    drawPath(
                        path = path,
                        color = NeonCyan,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Geophysical Depth & Source Estimation Card
            if (depthResult != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ESTIMATED DEPTH (HALF-WIDTH RULE)",
                                style = Typography.labelSmall,
                                color = NeonYellow,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "~${String.format("%.2f", depthResult.estimatedDepthMeters)} meters",
                                style = TelemetryTextStyle,
                                color = NeonGreen,
                                fontSize = 14.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (depthResult.isCavity) "TARGET: VOID / CAVITY" else "TARGET: FERROUS / METAL",
                                style = Typography.labelSmall,
                                color = if (depthResult.isCavity) NeonCyan else NeonRed,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Peak: ${String.format("%+.1f", depthResult.peakValue)} µT (Conf: ${depthResult.confidence})",
                                style = Typography.labelSmall,
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlicePill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, if (isSelected) NeonCyan else GlassBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = Typography.labelSmall,
            color = if (isSelected) NeonCyan else TextMuted,
            fontSize = 10.sp
        )
    }
}
