package com.dnic.geoscan.ui.screens.local

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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Grid4x4
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnic.geoscan.ui.components.GlassmorphicCard
import com.dnic.geoscan.ui.theme.DarkBase
import com.dnic.geoscan.ui.theme.DarkSurface
import com.dnic.geoscan.ui.theme.GlassBorder
import com.dnic.geoscan.ui.theme.GlassBorderCyan
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
    onOpenFilePicker: () -> Unit = {}
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

        // File Selector & Import Card
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

                    Text(
                        text = "TXT / CSV",
                        style = Typography.labelSmall,
                        color = TextMuted
                    )
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
                        Column {
                            Text(text = "Active File", style = Typography.labelSmall, color = TextMuted)
                            Text(text = "No scan loaded", style = Typography.bodyLarge, color = TextPrimary)
                            Text(text = "Expected format: X,Y,Z or legacy matrix", style = Typography.labelSmall, color = TextMuted)
                        }

                        ElevatedButton(
                            onClick = onOpenFilePicker,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = NeonCyan,
                                contentColor = DarkBase
                            )
                        ) {
                            Icon(imageVector = Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Open File", style = Typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Magnetic Spectrum Gradient Scale Bar
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = GlassBorder
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Text(text = "NOMINAL", style = Typography.labelSmall, color = NeonGreen)
                    Text(text = "+HIGH (Ferrous Metal)", style = Typography.labelSmall, color = NeonRed)
                }
            }
        }

        // Offline 3D Surface Viewport Placeholder
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            borderColor = GlassBorderCyan
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Layers,
                    contentDescription = "Offline 3D Viewport Placeholder",
                    tint = NeonCyan.copy(alpha = 0.7f),
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "OFFLINE 3D VISUALIZATION CANVAS",
                    style = Typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ready to render 3D Surface & Heatmap upon dataset load",
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
