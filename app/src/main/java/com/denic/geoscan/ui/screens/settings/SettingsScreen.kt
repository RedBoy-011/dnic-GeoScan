package com.denic.geoscan.ui.screens.settings

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denic.geoscan.data.audio.TelemetryAudioMode
import com.denic.geoscan.ui.components.GlassmorphicCard
import com.denic.geoscan.ui.localization.AppLanguage
import com.denic.geoscan.ui.localization.LocalAppLanguage
import com.denic.geoscan.ui.localization.LocalAppStrings
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
import com.denic.geoscan.ui.theme.TextMuted
import com.denic.geoscan.ui.theme.TextPrimary
import com.denic.geoscan.ui.theme.TextSecondary
import com.denic.geoscan.ui.theme.Typography
import com.denic.geoscan.util.GeoLogger

/**
 * Modern 2026 Settings & System Diagnostics Screen.
 * Provides live log inspection, public log exporting, system sharing, audio tuning, and bilingual controls.
 */
@Composable
fun SettingsScreen(
    onSelectLanguage: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val currentLanguage = LocalAppLanguage.current
    val scrollState = rememberScrollState()

    val logCount by GeoLogger.logCount.collectAsState()
    val recentLogs by GeoLogger.recentLogs.collectAsState()
    var isLiveLogExpanded by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("dnic_prefs", Context.MODE_PRIVATE) }
    var selectedAudioMode by remember {
        mutableStateOf(
            when (prefs.getString("telemetry_audio_mode", "DYNAMIC_Z_PITCH")) {
                "SIMPLE_BEEP" -> TelemetryAudioMode.SIMPLE_BEEP
                "MUTE" -> TelemetryAudioMode.MUTE
                else -> TelemetryAudioMode.DYNAMIC_Z_PITCH
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBase)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column {
                Text(
                    text = strings.settingsTitle,
                    style = Typography.displayMedium,
                    color = TextPrimary
                )
                Text(
                    text = strings.settingsSubtitle,
                    style = Typography.bodyMedium,
                    color = TextMuted
                )
            }

            // 1. Language Selection Card
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = GlassBorderCyan
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = strings.settingsLanguageSection,
                                style = Typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = strings.settingsLanguageDesc,
                                style = Typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LanguageSelectorOption(
                            title = "🇮🇷 فارسی (Persian)",
                            subtitle = "راست‌چین کامل (RTL)",
                            isSelected = currentLanguage == AppLanguage.PERSIAN,
                            onClick = { onSelectLanguage(AppLanguage.PERSIAN) },
                            modifier = Modifier.weight(1f)
                        )
                        LanguageSelectorOption(
                            title = "🇬🇧 English (Global)",
                            subtitle = "Left-to-Right (LTR)",
                            isSelected = currentLanguage == AppLanguage.ENGLISH,
                            onClick = { onSelectLanguage(AppLanguage.ENGLISH) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. Persistent Diagnostic Logger Card (Crucial for Project Owner Debugging)
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = NeonGreen
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Code,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = strings.settingsDebugLogSection,
                                    style = Typography.titleMedium,
                                    color = NeonGreen
                                )
                                Text(
                                    text = "denic_debug_log.txt",
                                    style = Typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonGreen.copy(alpha = 0.15f))
                                .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${GeoLogger.getLogSizeFormatted()} • $logCount lines",
                                style = Typography.labelSmall,
                                color = NeonGreen,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Text(
                        text = strings.settingsDebugLogDesc,
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )

                    // Action Buttons: Share Log & Save to Downloads
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Share Log via System Sheet
                        ElevatedButton(
                            onClick = {
                                val intent = GeoLogger.getShareIntent(context)
                                if (intent != null) {
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "Log file is empty or unavailable", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = NeonGreen,
                                contentColor = DarkBase
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.settingsShareLogBtn,
                                style = Typography.labelMedium,
                                fontSize = 11.sp
                            )
                        }

                        // Save Log to Public Downloads
                        OutlinedButton(
                            onClick = {
                                val result = GeoLogger.exportLogToPublicDownloads(context)
                                result.onSuccess { path ->
                                    Toast.makeText(context, "Log saved to $path", Toast.LENGTH_LONG).show()
                                }.onFailure { err ->
                                    Toast.makeText(context, "Export error: ${err.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NeonCyan
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.settingsExportLogBtn,
                                style = Typography.labelMedium,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Secondary Tools: Live Log Viewer Toggle & Clear Log
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { isLiveLogExpanded = !isLiveLogExpanded },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Code,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = NeonCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isLiveLogExpanded) "Hide Live Stream" else strings.settingsLiveLogTitle,
                                style = Typography.labelSmall
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                GeoLogger.clearLog()
                                Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NeonRed
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = NeonRed
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.settingsClearLogBtn,
                                style = Typography.labelSmall,
                                color = NeonRed
                            )
                        }
                    }

                    // Live Log Terminal Console
                    AnimatedVisibility(
                        visible = isLiveLogExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (recentLogs.isEmpty()) {
                                Text(
                                    text = strings.settingsLiveLogEmpty,
                                    style = Typography.labelSmall,
                                    color = TextMuted,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            } else {
                                recentLogs.forEach { logLine ->
                                    val logColor = when {
                                        logLine.contains("[ERROR]") || logLine.contains("Exception") -> NeonRed
                                        logLine.contains("[WARN]") -> NeonYellow
                                        logLine.contains("[DEBUG]") -> NeonCyan
                                        logLine.contains("BluetoothSPP") -> NeonGreen
                                        else -> TextSecondary
                                    }
                                    Text(
                                        text = logLine,
                                        style = Typography.labelSmall,
                                        color = logColor,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Audio Feedback Configuration Card
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = GlassBorderHighlighted
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.GraphicEq,
                            contentDescription = null,
                            tint = NeonYellow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.settingsAudioSection,
                            style = Typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AudioModeOption(
                            title = strings.audioModeZPitch,
                            isSelected = selectedAudioMode == TelemetryAudioMode.DYNAMIC_Z_PITCH,
                            onClick = {
                                selectedAudioMode = TelemetryAudioMode.DYNAMIC_Z_PITCH
                                prefs.edit().putString("telemetry_audio_mode", "DYNAMIC_Z_PITCH").apply()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        AudioModeOption(
                            title = strings.audioModeSimple,
                            isSelected = selectedAudioMode == TelemetryAudioMode.SIMPLE_BEEP,
                            onClick = {
                                selectedAudioMode = TelemetryAudioMode.SIMPLE_BEEP
                                prefs.edit().putString("telemetry_audio_mode", "SIMPLE_BEEP").apply()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        AudioModeOption(
                            title = strings.audioModeMute,
                            isSelected = selectedAudioMode == TelemetryAudioMode.MUTE,
                            onClick = {
                                selectedAudioMode = TelemetryAudioMode.MUTE
                                prefs.edit().putString("telemetry_audio_mode", "MUTE").apply()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Hardware Diagnostic & Device Info Card
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = GlassBorder
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.settingsHardwareSection,
                            style = Typography.titleSmall,
                            color = TextPrimary
                        )
                    }

                    HardwareInfoRow(label = strings.settingsAppVersionLabel, value = "v2.0.0 (Build 40) - 2026 Commercial")
                    HardwareInfoRow(label = "Device Model", value = "${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})")
                    HardwareInfoRow(label = "Android Version", value = "Android ${Build.VERSION.RELEASE} (API Level ${Build.VERSION.SDK_INT})")
                    HardwareInfoRow(label = "Hardware Board", value = "${Build.HARDWARE} / ${Build.BOARD}")
                    HardwareInfoRow(label = "Supported ABIs", value = Build.SUPPORTED_ABIS.joinToString(", "))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun LanguageSelectorOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) NeonCyan else GlassBorder
    val bgColor = if (isSelected) NeonCyan.copy(alpha = 0.12f) else DarkSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = Typography.titleSmall,
                    color = if (isSelected) NeonCyan else TextPrimary,
                    fontSize = 12.sp
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = subtitle,
                style = Typography.labelSmall,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun AudioModeOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) NeonYellow else GlassBorder
    val bgColor = if (isSelected) NeonYellow.copy(alpha = 0.12f) else DarkSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = Typography.labelSmall,
            color = if (isSelected) NeonYellow else TextSecondary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun HardwareInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = Typography.labelSmall, color = TextMuted, fontSize = 10.sp)
        Text(text = value, style = Typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
    }
}
