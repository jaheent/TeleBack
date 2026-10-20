package com.teleback.presentation.onboarding

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.teleback.presentation.theme.MonospaceFontFamily
import com.teleback.presentation.theme.TerminalAmber
import com.teleback.presentation.theme.TerminalBlack
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
fun OnboardingDialog(
    initialBotToken: String?,
    initialChatTitle: String?,
    initialFolderName: String?,
    masterKeyBase64: String?,
    onSaveToken: (String) -> Unit,
    onDetectChat: () -> Unit,
    onPickFolderClick: () -> Unit,
    onOpenGuide: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) }
    var tokenInput by remember { mutableStateOf(initialBotToken ?: "") }

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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TELEBACK // SETUP WIZARD",
                        color = TerminalMint,
                        fontSize = 14.sp,
                        fontFamily = MonospaceFontFamily
                    )
                    Text(
                        text = "STEP $step OF 3",
                        color = TerminalTextMuted,
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Need help? Open the visual guide",
                    color = TerminalCyan,
                    fontSize = 10.sp,
                    fontFamily = MonospaceFontFamily,
                    modifier = Modifier.clickable { onOpenGuide() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (step) {
                    1 -> {
                        Text(
                            text = "1. Telegram Bot Token",
                            color = TerminalCyan,
                            fontSize = 13.sp,
                            fontFamily = MonospaceFontFamily
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "TeleBack stores your files in a private Telegram Group using a custom bot. No TeleBack account or phone registration is required in the app.",
                            color = TerminalTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontFamily = MonospaceFontFamily
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/BotFather"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCyan)
                        ) {
                            Text("Open @BotFather in Telegram", fontSize = 11.sp, fontFamily = MonospaceFontFamily)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            label = { Text("Paste HTTP API Token", fontSize = 11.sp, fontFamily = MonospaceFontFamily) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = TerminalTextPrimary, fontSize = 11.sp, fontFamily = MonospaceFontFamily),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TerminalMint,
                                unfocusedBorderColor = TerminalBorder,
                                focusedLabelColor = TerminalMint,
                                unfocusedLabelColor = TerminalTextSecondary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (tokenInput.isNotBlank()) {
                                    onSaveToken(tokenInput.trim())
                                    step = 2
                                }
                            },
                            enabled = tokenInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalMint, contentColor = TerminalBlack)
                        ) {
                            Text("Save & Next ->", fontFamily = MonospaceFontFamily)
                        }
                    }

                    2 -> {
                        Text(
                            text = "2. Telegram Group Link",
                            color = TerminalCyan,
                            fontSize = 13.sp,
                            fontFamily = MonospaceFontFamily
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "1. Create a private Telegram Group or Channel.\n2. Add your bot as Administrator.\n3. Send any message in that group.\n4. Tap 'Auto-Detect Chat ID' below.",
                            color = TerminalTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            fontFamily = MonospaceFontFamily
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        if (initialChatTitle != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TerminalSurface)
                                    .border(1.dp, TerminalMint, RoundedCornerShape(4.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Linked Group: $initialChatTitle",
                                    color = TerminalMint,
                                    fontSize = 12.sp,
                                    fontFamily = MonospaceFontFamily
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onDetectChat,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCyan)
                        ) {
                            Text("Auto-Detect Chat ID", fontFamily = MonospaceFontFamily)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { step = 1 },
                                modifier = Modifier.weight(1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalTextSecondary)
                            ) {
                                Text("<- Back", fontFamily = MonospaceFontFamily)
                            }
                            Button(
                                onClick = { step = 3 },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = TerminalMint, contentColor = TerminalBlack)
                            ) {
                                Text("Next ->", fontFamily = MonospaceFontFamily)
                            }
                        }
                    }

                    3 -> {
                        Text(
                            text = "3. Target Folder & Security",
                            color = TerminalCyan,
                            fontSize = 13.sp,
                            fontFamily = MonospaceFontFamily
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Choose the local folder on your Android device to backup/sync (Documents, Notes, etc.) via Storage Access Framework.",
                            color = TerminalTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontFamily = MonospaceFontFamily
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onPickFolderClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalMint),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalMint)
                        ) {
                            Text(
                                text = if (initialFolderName != null) "Folder: $initialFolderName" else "Select Target Folder",
                                fontFamily = MonospaceFontFamily
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "AES-256-GCM Master Key: ENABLED",
                            color = TerminalMint,
                            fontSize = 11.sp,
                            fontFamily = MonospaceFontFamily
                        )
                        Text(
                            text = "Files are encrypted on-device before uploading.",
                            color = TerminalTextMuted,
                            fontSize = 10.sp,
                            fontFamily = MonospaceFontFamily
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalMint, contentColor = TerminalBlack)
                        ) {
                            Text("Complete Setup", fontFamily = MonospaceFontFamily)
                        }
                    }
                }
            }
        }
    }
}
