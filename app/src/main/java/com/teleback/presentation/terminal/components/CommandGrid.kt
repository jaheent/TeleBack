package com.teleback.presentation.terminal.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleback.core.cli.CliCommand
import com.teleback.presentation.theme.MonospaceFontFamily

@Composable
fun CommandGrid(
    isSettingsView: Boolean,
    onCommandSelected: (CliCommand) -> Unit,
    onOpenSettings: () -> Unit,
    onBackFromSettings: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenKeys: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = isSettingsView,
        transitionSpec = {
            if (targetState) {
                // Glitchy transition into Settings at 0.5x speed (600ms)
                (fadeIn(animationSpec = keyframes {
                    durationMillis = 600
                    0.0f at 0
                    0.3f at 120
                    0.1f at 240
                    0.85f at 380
                    0.55f at 480
                    1.0f at 600
                }) + slideInHorizontally(
                    initialOffsetX = { it / 6 },
                    animationSpec = tween(600, easing = LinearOutSlowInEasing)
                )).togetherWith(
                    fadeOut(animationSpec = tween(300))
                )
            } else {
                // Glitchy transition back to Main console at 0.5x speed (600ms)
                (fadeIn(animationSpec = keyframes {
                    durationMillis = 600
                    0.0f at 0
                    0.4f at 140
                    0.2f at 260
                    0.9f at 400
                    0.6f at 500
                    1.0f at 600
                }) + slideInHorizontally(
                    initialOffsetX = { -it / 6 },
                    animationSpec = tween(600, easing = LinearOutSlowInEasing)
                )).togetherWith(
                    fadeOut(animationSpec = tween(300))
                )
            }
        },
        label = "CommandGridGlitchTransition",
        modifier = modifier.fillMaxWidth()
    ) { inSettings ->
        if (inSettings) {
            SettingsSubGrid(
                onBack = onBackFromSettings,
                onOpenThemes = onOpenThemes,
                onOpenKeys = onOpenKeys,
                onOpenSchedule = onOpenSchedule,
                onOpenFeedback = onOpenFeedback
            )
        } else {
            MainCommandGrid(
                onCommandSelected = { cmd ->
                    if (cmd == CliCommand.SETTINGS) {
                        onOpenSettings()
                    } else {
                        onCommandSelected(cmd)
                    }
                }
            )
        }
    }
}

@Composable
private fun MainCommandGrid(
    onCommandSelected: (CliCommand) -> Unit
) {
    val pairs = listOf(
        Pair(CliCommand.SYNC, CliCommand.BACKUP),
        Pair(CliCommand.RESTORE, CliCommand.CLOUD),
        Pair(CliCommand.STATUS, CliCommand.TEST),
        Pair(CliCommand.CLEAR, CliCommand.SETTINGS)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        pairs.forEach { (cmdLeft, cmdRight) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CommandGridCell(
                    command = cmdLeft,
                    onClick = { onCommandSelected(cmdLeft) },
                    modifier = Modifier.weight(1f)
                )
                CommandGridCell(
                    command = cmdRight,
                    onClick = { onCommandSelected(cmdRight) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // [0] Setup as full width option after Settings [8]
        CommandGridCell(
            command = CliCommand.SETUP,
            onClick = { onCommandSelected(CliCommand.SETUP) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettingsSubGrid(
    onBack: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenKeys: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenFeedback: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Settings Mode Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ [SETTINGS CONFIGURATION]",
                color = colorScheme.primary,
                fontSize = 10.sp,
                fontFamily = MonospaceFontFamily
            )
            Text(
                text = "Tap [<] or type 'back' to exit",
                color = colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontFamily = MonospaceFontFamily
            )
        }

        // Row 1: Themes / Icons & Vault Keys
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CustomGridCell(
                badge = "[1]",
                name = "theme",
                label = "Themes & styles",
                onClick = onOpenThemes,
                modifier = Modifier.weight(1f)
            )
            CustomGridCell(
                badge = "[2]",
                name = "keys",
                label = "Vault Keys",
                onClick = onOpenKeys,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Auto-Sync Scheduler & Bug/Feedback Reporter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CustomGridCell(
                badge = "[3]",
                name = "auto",
                label = "Auto-Sync & sched",
                onClick = onOpenSchedule,
                modifier = Modifier.weight(1f)
            )
            CustomGridCell(
                badge = "[4]",
                name = "report",
                label = "Feedback & crash",
                badgeColor = colorScheme.secondary,
                onClick = onOpenFeedback,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Return Back to Main Command Console
        CustomGridCell(
            badge = "[<]",
            name = "back",
            label = "Return to Main Console",
            badgeColor = colorScheme.secondary,
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CommandGridCell(
    command: CliCommand,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CustomGridCell(
        badge = "[${command.number}]",
        name = command.commandName,
        label = command.shortLabel,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun CustomGridCell(
    badge: String,
    name: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeColor: Color = MaterialTheme.colorScheme.primary
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outline, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = badge,
                color = badgeColor,
                fontSize = 11.sp,
                fontFamily = MonospaceFontFamily
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                color = colorScheme.onSurface,
                fontSize = 11.sp,
                fontFamily = MonospaceFontFamily
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = label,
                color = colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                maxLines = 1,
                fontFamily = MonospaceFontFamily
            )
        }
    }
}
