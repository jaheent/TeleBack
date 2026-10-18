package com.teleback.presentation.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleback.presentation.theme.TerminalAmber
import com.teleback.presentation.theme.TerminalBorder
import com.teleback.presentation.theme.TerminalCyan
import com.teleback.presentation.theme.TerminalDark
import com.teleback.presentation.theme.TerminalMint
import com.teleback.presentation.theme.TerminalRed
import com.teleback.presentation.theme.TerminalSurface
import com.teleback.presentation.theme.TerminalTextMuted
import com.teleback.presentation.theme.TerminalTextPrimary
import com.teleback.presentation.theme.TerminalTextSecondary

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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalDark)
            .border(1.dp, TerminalBorder)
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
                    color = TerminalMint,
                    fontSize = 14.sp,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = " v1.0",
                    color = TerminalTextMuted,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val (statusText, statusDotColor) = when {
                    !isOnline -> Pair("OFFLINE", TerminalRed)
                    isBusy -> Pair("SYNCING", TerminalAmber)
                    else -> Pair("ONLINE", TerminalMint)
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

        // Metadata badges row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeaderBadge(
                label = "BOT",
                value = botName ?: "None",
                accentColor = if (botName != null) TerminalMint else TerminalAmber
            )
            HeaderBadge(
                label = "GROUP",
                value = chatTitle ?: "Unlinked",
                accentColor = if (chatTitle != null) TerminalCyan else TerminalTextMuted
            )
            HeaderBadge(
                label = "DIR",
                value = folderName ?: "Unset",
                accentColor = if (folderName != null) TerminalMint else TerminalAmber
            )
            if (isEncrypted) {
                HeaderBadge(
                    label = "CIPHER",
                    value = "AES-GCM",
                    accentColor = TerminalMint
                )
            }
        }
    }
}

@Composable
private fun HeaderBadge(
    label: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label: ",
                color = TerminalTextSecondary,
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
