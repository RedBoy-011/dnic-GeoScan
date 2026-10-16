package com.denic.geoscan.ui.components

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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.denic.geoscan.ui.localization.AppLanguage
import com.denic.geoscan.ui.localization.getStrings
import com.denic.geoscan.ui.navigation.DenicBrandLogo
import com.denic.geoscan.ui.theme.DarkBase
import com.denic.geoscan.ui.theme.DarkSurface
import com.denic.geoscan.ui.theme.DarkSurfaceElevated
import com.denic.geoscan.ui.theme.GlassBorder
import com.denic.geoscan.ui.theme.NeonCyan
import com.denic.geoscan.ui.theme.NeonGreen
import com.denic.geoscan.ui.theme.TextMuted
import com.denic.geoscan.ui.theme.TextPrimary
import com.denic.geoscan.ui.theme.TextSecondary
import com.denic.geoscan.ui.theme.Typography

/**
 * First-launch onboarding and manual language selection dialog.
 * Prompts user to choose between English and Persian.
 */
@Composable
fun LanguagePickerDialog(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismissRequest: (() -> Unit)? = null
) {
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }
    val strings = remember(selectedLanguage) { getStrings(selectedLanguage) }

    Dialog(
        onDismissRequest = { onDismissRequest?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = onDismissRequest != null,
            dismissOnClickOutside = onDismissRequest != null
        )
    ) {
        val shape = RoundedCornerShape(24.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(
                    width = 1.2.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            NeonGreen.copy(alpha = 0.5f),
                            GlassBorder,
                            NeonCyan.copy(alpha = 0.5f)
                        )
                    ),
                    shape = shape
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DarkSurfaceElevated.copy(alpha = 0.98f),
                            DarkBase.copy(alpha = 0.98f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Denic Logo & Globe Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(NeonCyan.copy(alpha = 0.12f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = "Language",
                        tint = NeonCyan,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = strings.selectLanguageTitle,
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = strings.selectLanguageSubtitle,
                    style = Typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // English Option Card
                LanguageOptionCard(
                    title = "English (Global)",
                    description = "Standard international scientific terminology (LTR)",
                    flag = "🇬🇧",
                    isSelected = selectedLanguage == AppLanguage.ENGLISH,
                    onClick = { selectedLanguage = AppLanguage.ENGLISH }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Persian Option Card
                LanguageOptionCard(
                    title = "فارسی (Persian)",
                    description = "رابط کاربری و اصطلاحات تخصصی ژئوفیزیک (راست‌چین)",
                    flag = "🇮🇷",
                    isSelected = selectedLanguage == AppLanguage.PERSIAN,
                    onClick = { selectedLanguage = AppLanguage.PERSIAN }
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Confirm Button
                ElevatedButton(
                    onClick = { onLanguageSelected(selectedLanguage) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = NeonGreen,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.startApp,
                        style = Typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionCard(
    title: String,
    description: String,
    flag: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val activeBorder = if (isSelected) NeonCyan else GlassBorder
    val activeBg = if (isSelected) NeonCyan.copy(alpha = 0.12f) else DarkSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, activeBorder, shape)
            .background(activeBg)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = flag, fontSize = 26.sp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) NeonCyan else TextPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = Typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) NeonCyan else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
