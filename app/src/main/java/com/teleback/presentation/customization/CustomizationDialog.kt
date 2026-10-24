package com.teleback.presentation.customization

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.teleback.presentation.theme.MonospaceFontFamily
import com.teleback.presentation.theme.TeleBackThemeId
import com.teleback.presentation.theme.ThemeColor
import com.teleback.presentation.theme.ThemeMode

@Composable
fun CustomizationDialog(
    currentThemeId: String,
    onSelectTheme: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val activeTheme = TeleBackThemeId.fromId(currentThemeId)
    val colorScheme = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CUSTOMIZATION SUITE [7]",
                        color = colorScheme.primary,
                        fontSize = 13.sp,
                        fontFamily = MonospaceFontFamily
                    )
                    Text(
                        text = "ESC",
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Mode (Dark vs Light)
                Text(
                    text = "THEME MODE",
                    color = colorScheme.primary,
                    fontSize = 11.sp,
                    fontFamily = MonospaceFontFamily
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val isModeSelected = activeTheme.mode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isModeSelected) colorScheme.primary.copy(alpha = 0.15f) else colorScheme.surfaceVariant)
                                .border(
                                    width = if (isModeSelected) 1.5.dp else 1.dp,
                                    color = if (isModeSelected) colorScheme.primary else colorScheme.outline,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable {
                                    val newTheme = TeleBackThemeId.fromModeAndColor(mode, activeTheme.color)
                                    onSelectTheme(newTheme.id)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label.uppercase(),
                                color = if (isModeSelected) colorScheme.primary else colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontFamily = MonospaceFontFamily
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: 3 Color Accents
                Text(
                    text = "ACCENT COLOR",
                    color = colorScheme.primary,
                    fontSize = 11.sp,
                    fontFamily = MonospaceFontFamily
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeColor.entries.forEach { color ->
                        val isColorSelected = activeTheme.color == color
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isColorSelected) color.previewColor.copy(alpha = 0.18f) else colorScheme.surfaceVariant)
                                .border(
                                    width = if (isColorSelected) 1.5.dp else 1.dp,
                                    color = if (isColorSelected) color.previewColor else colorScheme.outline,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable {
                                    val newTheme = TeleBackThemeId.fromModeAndColor(activeTheme.mode, color)
                                    onSelectTheme(newTheme.id)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color.previewColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = color.label.split(" ").last(),
                                    color = if (isColorSelected) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontFamily = MonospaceFontFamily
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    )
                ) {
                    Text("Done", fontFamily = MonospaceFontFamily)
                }
            }
        }
    }
}
