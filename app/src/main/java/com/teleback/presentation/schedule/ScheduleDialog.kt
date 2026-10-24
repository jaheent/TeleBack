package com.teleback.presentation.schedule

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.teleback.presentation.theme.MonospaceFontFamily
import com.teleback.presentation.theme.TerminalAmber
import com.teleback.presentation.theme.TerminalMint

@Composable
fun ScheduleDialog(
    initialEnabled: Boolean,
    initialIntervalHours: Int,
    initialRequiresUnmetered: Boolean,
    initialRequiresCharging: Boolean,
    initialRequiresBatteryNotLow: Boolean,
    onSaveSchedule: (enabled: Boolean, intervalHours: Int, unmetered: Boolean, charging: Boolean, batteryNotLow: Boolean) -> Unit,
    onTriggerInstantSync: () -> Unit,
    onDismiss: () -> Unit
) {
    var isEnabled by remember { mutableStateOf(initialEnabled) }
    var intervalHours by remember { mutableIntStateOf(initialIntervalHours) }
    var requiresUnmetered by remember { mutableStateOf(initialRequiresUnmetered) }
    var requiresCharging by remember { mutableStateOf(initialRequiresCharging) }
    var requiresBatteryNotLow by remember { mutableStateOf(initialRequiresBatteryNotLow) }

    val colorScheme = MaterialTheme.colorScheme
    val intervals = listOf(
        Pair(1, "1 Hour"),
        Pair(6, "6 Hours"),
        Pair(12, "12 Hours"),
        Pair(24, "Daily (24h)"),
        Pair(168, "Weekly (7d)")
    )

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
                        text = "AUTO-SYNC & SCHEDULER",
                        color = colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
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

                // Master Toggle: Periodic Sync Enabled/Disabled
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(colorScheme.background)
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Periodic Auto-Sync",
                            color = colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MonospaceFontFamily
                        )
                        Text(
                            text = if (isEnabled) "Active in background" else "Disabled (Manual only)",
                            color = if (isEnabled) TerminalMint else colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontFamily = MonospaceFontFamily
                        )
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.primary,
                            checkedTrackColor = colorScheme.primary.copy(alpha = 0.3f),
                            uncheckedThumbColor = colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = colorScheme.surface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section: Interval Selection
                Text(
                    text = "SYNC INTERVAL",
                    color = colorScheme.primary,
                    fontSize = 11.sp,
                    fontFamily = MonospaceFontFamily
                )
                Spacer(modifier = Modifier.height(6.dp))

                intervals.forEach { (hours, label) ->
                    val isSelected = intervalHours == hours
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) colorScheme.primary else colorScheme.outline.copy(alpha = 0.5f),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { intervalHours = hours }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) colorScheme.primary else colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontFamily = MonospaceFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Text(
                                text = "[ACTIVE]",
                                color = colorScheme.primary,
                                fontSize = 10.sp,
                                fontFamily = MonospaceFontFamily
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section: Constraints
                Text(
                    text = "DEVICE CONSTRAINTS",
                    color = colorScheme.primary,
                    fontSize = 11.sp,
                    fontFamily = MonospaceFontFamily
                )
                Spacer(modifier = Modifier.height(6.dp))

                ConstraintCheckbox(
                    title = "Unmetered Network (Wi-Fi Only)",
                    checked = requiresUnmetered,
                    onCheckedChange = { requiresUnmetered = it }
                )

                ConstraintCheckbox(
                    title = "Require Device Charging",
                    checked = requiresCharging,
                    onCheckedChange = { requiresCharging = it }
                )

                ConstraintCheckbox(
                    title = "Require Battery Not Low (>20%)",
                    checked = requiresBatteryNotLow,
                    onCheckedChange = { requiresBatteryNotLow = it }
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = colorScheme.outline)
                Spacer(modifier = Modifier.height(12.dp))

                // One-Time Trigger Button
                Button(
                    onClick = {
                        onTriggerInstantSync()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.surfaceVariant,
                        contentColor = colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = "⚡ TRIGGER ONE-TIME BACKGROUND SYNC",
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Cancel", fontFamily = MonospaceFontFamily, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            onSaveSchedule(
                                isEnabled,
                                intervalHours,
                                requiresUnmetered,
                                requiresCharging,
                                requiresBatteryNotLow
                            )
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Save Schedule",
                            fontFamily = MonospaceFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConstraintCheckbox(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = colorScheme.primary,
                checkmarkColor = colorScheme.onPrimary,
                uncheckedColor = colorScheme.outline
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = colorScheme.onSurface,
            fontSize = 11.sp,
            fontFamily = MonospaceFontFamily
        )
    }
}
