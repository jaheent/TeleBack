package com.teleback.presentation.terminal.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleback.core.cli.CliCommand
import com.teleback.presentation.theme.MonospaceFontFamily

@Composable
fun CommandGrid(
    onCommandSelected: (CliCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    // 5 rows x 2 columns
    val pairs = listOf(
        Pair(CliCommand.SYNC, CliCommand.BACKUP),
        Pair(CliCommand.RESTORE, CliCommand.CLOUD),
        Pair(CliCommand.STATUS, CliCommand.TEST),
        Pair(CliCommand.CUSTOM, CliCommand.KEYS),
        Pair(CliCommand.CLEAR, CliCommand.SETUP)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
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
    }
}

@Composable
private fun CommandGridCell(
    command: CliCommand,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
                text = "[${command.number}]",
                color = colorScheme.primary,
                fontSize = 11.sp,
                fontFamily = MonospaceFontFamily
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = command.commandName,
                color = colorScheme.onSurface,
                fontSize = 11.sp,
                fontFamily = MonospaceFontFamily
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = command.shortLabel,
                color = colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                maxLines = 1,
                fontFamily = MonospaceFontFamily
            )
        }
    }
}
