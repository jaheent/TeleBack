package com.teleback.presentation.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleback.presentation.theme.TerminalAmber
import com.teleback.presentation.theme.TerminalRed

@Composable
fun TerminalHeader(
    isOnline: Boolean,
    botName: String?,
    chatTitle: String?,
    folderName: String?,
    isEncrypted: Boolean,
    isBusy: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outline)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // App title & live status indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "TELEBACK",
                    color = colorScheme.primary,
                    fontSize = 14.sp,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = " v1.0",
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val (statusText, statusDotColor) = when {
                    !isOnline -> Pair("OFFLINE", TerminalRed)
                    isBusy -> Pair("SYNCING", TerminalAmber)
                    else -> Pair("ONLINE", colorScheme.primary)
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    color = statusDotColor,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Metadata badges row (horizontally scrollable to avoid clipping)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderBadge(
                label = "BOT",
                value = botName ?: "None",
                accentColor = if (botName != null) colorScheme.primary else TerminalAmber
            )
            HeaderBadge(
                label = "GROUP",
                value = chatTitle ?: "Unlinked",
                accentColor = if (chatTitle != null) colorScheme.secondary else colorScheme.onSurfaceVariant
            )
            HeaderBadge(
                label = "DIR",
                value = folderName ?: "Unset",
                accentColor = if (folderName != null) colorScheme.primary else TerminalAmber
            )
            if (isEncrypted) {
                HeaderBadge(
                    label = "CIPHER",
                    value = "AES-GCM",
                    accentColor = colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun HeaderBadge(
    label: String,
    value: String,
    accentColor: Color
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(colorScheme.surfaceVariant)
            .border(1.dp, colorScheme.outline, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label: ",
                color = colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
            Text(
                text = value,
                color = accentColor,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}
