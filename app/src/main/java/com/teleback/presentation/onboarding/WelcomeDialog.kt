package com.teleback.presentation.onboarding

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.teleback.presentation.theme.MonospaceFontFamily
import com.teleback.presentation.theme.TerminalBlack
import com.teleback.presentation.theme.TerminalBorder
import com.teleback.presentation.theme.TerminalCyan
import com.teleback.presentation.theme.TerminalDark
import com.teleback.presentation.theme.TerminalMint
import com.teleback.presentation.theme.TerminalSurface
import com.teleback.presentation.theme.TerminalTextMuted
import com.teleback.presentation.theme.TerminalTextPrimary
import com.teleback.presentation.theme.TerminalTextSecondary

@Composable
fun WelcomeDialog(
    onLaunchSetup: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, TerminalMint, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = TerminalDark)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TELEBACK // WELCOME",
                        color = TerminalCyan,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        fontFamily = MonospaceFontFamily
                    )
                    Text(
                        text = "v${com.teleback.BuildConfig.VERSION_NAME}",
                        color = TerminalTextMuted,
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "◈ YOUR PRIVATE CLOUD ON TELEGRAM",
                    color = TerminalMint,
                    fontSize = 13.sp,
                    fontFamily = MonospaceFontFamily
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "TeleBack turns any private Telegram Group into an unlimited, encrypted personal cloud storage with zero account signups.",
                    color = TerminalTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontFamily = MonospaceFontFamily
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = TerminalBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "HOW IT WORKS:",
                    color = TerminalCyan,
                    fontSize = 12.sp,
                    fontFamily = MonospaceFontFamily
                )

                Spacer(modifier = Modifier.height(8.dp))

                BulletFeature(
                    title = "1. Zero-Signup Cloud",
                    desc = "Connect your Telegram Bot to a private group. No emails, passwords, or external servers."
                )

                Spacer(modifier = Modifier.height(6.dp))

                BulletFeature(
                    title = "2. Client-Side AES-256 Encryption",
                    desc = "All files and directory trees are encrypted on your device before upload. Telegram only receives ciphertext blobs."
                )

                Spacer(modifier = Modifier.height(6.dp))

                BulletFeature(
                    title = "3. Numbered Terminal Commands",
                    desc = "Control everything with single-digit shortcuts! Tap any command chip (e.g. [1] sync, [2] backup) or type the number."
                )

                Spacer(modifier = Modifier.height(6.dp))

                BulletFeature(
                    title = "4. Point-in-Time Snapshots",
                    desc = "Restore full folder trees or browse previous versions with automatic 10-snapshot cloud pruning."
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = TerminalBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "QUICK START:\nLink your Telegram Bot and select a folder to begin.",
                    color = TerminalTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontFamily = MonospaceFontFamily
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onLaunchSetup,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerminalMint,
                        contentColor = TerminalBlack
                    )
                ) {
                    Text("Start Guided Setup [0]", fontFamily = MonospaceFontFamily)
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalTextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
                ) {
                    Text("Explore Terminal", fontFamily = MonospaceFontFamily)
                }
            }
        }
    }
}

@Composable
private fun BulletFeature(title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Text(
            text = "• $title",
            color = TerminalMint,
            fontSize = 11.sp,
            fontFamily = MonospaceFontFamily
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = desc,
            color = TerminalTextSecondary,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            fontFamily = MonospaceFontFamily
        )
    }
}
