package com.denic.geoscan.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denic.geoscan.data.engine.RSurfaceReport
import com.denic.geoscan.ui.localization.AppLanguage
import com.denic.geoscan.ui.localization.LocalAppLanguage
import com.denic.geoscan.ui.localization.LocalAppStrings
import com.denic.geoscan.ui.theme.DarkBase
import com.denic.geoscan.ui.theme.DarkSurface
import com.denic.geoscan.ui.theme.GlassBorder
import com.denic.geoscan.ui.theme.GlassBorderCyan
import com.denic.geoscan.ui.theme.GlassBorderHighlighted
import com.denic.geoscan.ui.theme.NeonAmber
import com.denic.geoscan.ui.theme.NeonCyan
import com.denic.geoscan.ui.theme.NeonGreen
import com.denic.geoscan.ui.theme.NeonRed
import com.denic.geoscan.ui.theme.TextMuted
import com.denic.geoscan.ui.theme.TextPrimary
import com.denic.geoscan.ui.theme.Typography

/**
 * High-end Glassmorphic Geophysical Interpretation & Analysis Box (باکس تحلیل و نظریه ژئوفیزیک)
 * Aligned with R surface3d Cartesian calibration and standard geophysics anomaly physics.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeophysicalInterpretationCard(
    report: RSurfaceReport?,
    modifier: Modifier = Modifier
) {
    if (report == null || report.surfacePoints.isEmpty()) return

    val currentLang = LocalAppLanguage.current
    val isFa = currentLang == AppLanguage.FA
    var isExpanded by remember { mutableStateOf(true) }

    val classificationTitle = if (isFa) report.targetClassificationFa else report.targetClassificationEn
    val interpretationText = if (isFa) report.interpretationFa else report.interpretationEn

    val badgeColor = when {
        report.maxMetalPeak > 20.0 && kotlin.math.abs(report.deepestCavity) > 20.0 -> NeonAmber
        report.maxMetalPeak > 15.0 -> NeonRed
        kotlin.math.abs(report.deepestCavity) > 15.0 -> NeonCyan
        else -> NeonGreen
    }

    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = badgeColor.copy(alpha = 0.5f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Analytics,
                        contentDescription = "R surface3d Analysis",
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFa) "تحلیل و نظریه ژئوفیزیک (R surface3d)" else "GEOPHYSICAL INTERPRETATION (R surface3d)",
                        style = Typography.labelLarge,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .border(1.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isFa) "کالیبره دکارتی R" else "R CALIBRATED",
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = "Toggle",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Primary Target Classification Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(badgeColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFa) "طبقه‌بندی ساختار تارگت زیرسطحی" else "Subsurface Target Classification",
                            style = Typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = classificationTitle,
                            style = Typography.titleMedium,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Core Survey Metric Chips (FlowRow)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    icon = Icons.Outlined.Layers,
                    label = if (isFa) "ابعاد شبکه R" else "R Grid Matrix",
                    value = "${report.gridDimensions.second} × ${report.gridDimensions.first}",
                    color = NeonCyan
                )

                MetricChip(
                    icon = Icons.Outlined.CheckCircle,
                    label = if (isFa) "خط مبنا (میانه)" else "Ground Zero",
                    value = String.format("%.1f µT", report.median),
                    color = NeonGreen
                )

                MetricChip(
                    icon = Icons.Outlined.VerticalAlignTop,
                    label = if (isFa) "بیشینه پیک فلز" else "Max Metal Peak",
                    value = String.format("+%.1f µT", report.maxMetalPeak),
                    color = NeonRed
                )

                MetricChip(
                    icon = Icons.Outlined.VerticalAlignBottom,
                    label = if (isFa) "عمیق‌ترین افت فضا" else "Deepest Cavity",
                    value = String.format("%.1f µT", report.deepestCavity),
                    color = NeonCyan
                )

                if (report.estimatedDepthMeters > 0) {
                    MetricChip(
                        icon = Icons.Outlined.Straighten,
                        label = if (isFa) "عمق تخمینی" else "Est. Depth",
                        value = String.format("~%.1fm", report.estimatedDepthMeters),
                        color = NeonAmber
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Geophysical Theory Explanation Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBase)
                            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Psychology,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFa) "نظریه و تفسیر فیزیکی گرادیومتر:" else "Geophysical Interpretation Theory:",
                                    style = Typography.labelSmall,
                                    color = NeonGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = interpretationText,
                                style = Typography.bodySmall,
                                color = TextPrimary.copy(alpha = 0.9f),
                                lineHeight = 19.sp,
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = if (isFa) {
                                    "⚡ انطباق دکارتی: محور Y مطابق تابع surface3d پلتفرم R از پایین به بالا کالیبره شده تا وارونگی دیداری کامپوز خنثی گردد."
                                } else {
                                    "⚡ Cartesian Alignment: Y axis inverted (rowsCount - 1 - r) matching R surface3d coordinate standard."
                                },
                                style = Typography.labelSmall,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(text = label, style = Typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                Text(text = value, style = Typography.labelMedium, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}
