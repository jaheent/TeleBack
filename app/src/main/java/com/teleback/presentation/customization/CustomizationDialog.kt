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
import com.teleback.presentation.theme.TerminalBlack
import com.teleback.presentation.theme.TerminalBorder
import com.teleback.presentation.theme.TerminalCyan
import com.teleback.presentation.theme.TerminalDark
import com.teleback.presentation.theme.TerminalMint
import com.teleback.presentation.theme.TerminalSurface
import com.teleback.presentation.theme.TerminalTextMuted
import com.teleback.presentation.theme.TerminalTextPrimary
import com.teleback.presentation.theme.TerminalTextSecondary
import com.teleback.presentation.theme.getThemeColorScheme

@Composable
fun CustomizationDialog(
    currentThemeId: String,
    currentIconId: String,
    onSelectTheme: (String) -> Unit,
    onSelectIcon: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, TerminalCyan, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = TerminalDark)
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
                        color = TerminalCyan,
                        fontSize = 13.sp,
                        fontFamily = MonospaceFontFamily
                    )
                    Text(
                        text = "ESC",
                        color = TerminalTextMuted,
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 1: Themes
                Text(
                    text = "THEMES (3 Dark · 3 Light)",
                    color = TerminalMint,
                    fontSize = 11.sp,
                    fontFamily = MonospaceFontFamily
                )
                Spacer(modifier = Modifier.height(6.dp))

                TeleBackThemeId.entries.forEach { theme ->
                    val isSelected = theme.id == currentThemeId
                    val scheme = getThemeColorScheme(theme)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) TerminalSurface else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) TerminalMint else TerminalBorder,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { onSelectTheme(theme.id) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Swatches
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(scheme.background)
                                .border(1.dp, TerminalBorder, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(scheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = theme.displayName,
                            color = if (isSelected) TerminalMint else TerminalTextPrimary,
                            fontSize = 11.sp,
                            fontFamily = MonospaceFontFamily,
                            modifier = Modifier.weight(1f)
                        )

                        if (isSelected) {
                            Text(
                                text = "ACTIVE",
                                color = TerminalMint,
                                fontSize = 9.sp,
                                fontFamily = MonospaceFontFamily
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = TerminalBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: App Icons
                Text(
                    text = "APP ICONS (3 Modern Designs)",
                    color = TerminalMint,
                    fontSize = 11.sp,
                    fontFamily = MonospaceFontFamily
                )
                Spacer(modifier = Modifier.height(6.dp))

                IconOptionRow(
                    id = "icon_a",
                    name = "Icon A: Neon Shield (Default)",
                    description = "Cyan security contour with matrix mint chevron >_",
                    isSelected = currentIconId == "icon_a",
                    onSelect = { onSelectIcon("icon_a") }
                )

                Spacer(modifier = Modifier.height(4.dp))

                IconOptionRow(
                    id = "icon_b",
                    name = "Icon B: Dual-Orbit Sync",
                    description = "Electric mint & cyan orbit ring with paper plane",
                    isSelected = currentIconId == "icon_b",
                    onSelect = { onSelectIcon("icon_b") }
                )

                Spacer(modifier = Modifier.height(4.dp))

                IconOptionRow(
                    id = "icon_c",
                    name = "Icon C: Monolithic Vault",
                    description = "Isometric wireframe data cube with amber status LED",
                    isSelected = currentIconId == "icon_c",
                    onSelect = { onSelectIcon("icon_c") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalMint, contentColor = TerminalBlack)
                ) {
                    Text("Done", fontFamily = MonospaceFontFamily)
                }
            }
        }
    }
}

@Composable
private fun IconOptionRow(
    id: String,
    name: String,
    description: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) TerminalSurface else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isSelected) TerminalCyan else TerminalBorder,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onSelect)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                color = if (isSelected) TerminalCyan else TerminalTextPrimary,
                fontSize = 11.sp,
                fontFamily = MonospaceFontFamily
            )
            if (isSelected) {
                Text(
                    text = "SELECTED",
                    color = TerminalCyan,
                    fontSize = 9.sp,
                    fontFamily = MonospaceFontFamily
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            color = TerminalTextMuted,
            fontSize = 9.sp,
            lineHeight = 13.sp,
            fontFamily = MonospaceFontFamily
        )
    }
}
