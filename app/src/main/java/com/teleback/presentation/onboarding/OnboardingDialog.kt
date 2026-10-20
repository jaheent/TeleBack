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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.teleback.core.crypto.CryptoEngine
import com.teleback.presentation.theme.MonospaceFontFamily
import com.teleback.presentation.theme.TerminalAmber
import com.teleback.presentation.theme.TerminalBlack
import com.teleback.presentation.theme.TerminalCyan
import com.teleback.presentation.theme.TerminalMint
import com.teleback.presentation.theme.TerminalRed

@Composable
fun OnboardingDialog(
    initialBotToken: String?,
    initialChatTitle: String?,
    initialFolderName: String?,
    masterKeyBase64: String?,
    onSaveToken: (String) -> Unit,
    onDetectChat: () -> Unit,
    onPickFolderClick: () -> Unit,
    onCompleteSetup: (derivedKey: String?, isExistingVault: Boolean) -> Unit,
    onOpenGuide: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) }
    var tokenInput by remember { mutableStateOf(initialBotToken ?: "") }

    // Step 3 states
    var isNewVault by remember { mutableStateOf(true) }
    var passphraseInput by remember { mutableStateOf("") }
    var confirmPassphraseInput by remember { mutableStateOf("") }
    var unrecoverableUnderstood by remember { mutableStateOf(false) }
    var step3Error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MonospaceFontFamily
                    )
                    Text(
                        text = "STEP $step OF 3",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
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
                            text = "TeleBack stores your files encrypted in a private Telegram Group using your own bot. No third-party servers.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontFamily = MonospaceFontFamily),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text("Save & Next ->", fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
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
                            text = "1. Create a private Telegram Group.\n2. Add your bot as Administrator.\n3. Send any message in that group.\n4. Tap 'Auto-Detect Chat ID' below.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Linked Group: $initialChatTitle",
                                    color = MaterialTheme.colorScheme.primary,
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
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            ) {
                                Text("<- Back", fontFamily = MonospaceFontFamily)
                            }
                            Button(
                                onClick = { step = 3 },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text("Next ->", fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    3 -> {
                        Text(
                            text = "3. Security & Target Folder",
                            color = TerminalCyan,
                            fontSize = 13.sp,
                            fontFamily = MonospaceFontFamily
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Vault Mode Selector
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { isNewVault = true; step3Error = null },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isNewVault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isNewVault) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text("New Vault", fontSize = 11.sp, fontFamily = MonospaceFontFamily)
                            }
                            Button(
                                onClick = { isNewVault = false; step3Error = null },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isNewVault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (!isNewVault) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text("Existing Vault", fontSize = 11.sp, fontFamily = MonospaceFontFamily)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isNewVault) {
                            Text(
                                text = "Create a memorable Master Passphrase to encrypt your files.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontFamily = MonospaceFontFamily
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = passphraseInput,
                                onValueChange = { passphraseInput = it; step3Error = null },
                                label = { Text("Set Master Passphrase", fontSize = 11.sp, fontFamily = MonospaceFontFamily) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontFamily = MonospaceFontFamily),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = confirmPassphraseInput,
                                onValueChange = { confirmPassphraseInput = it; step3Error = null },
                                label = { Text("Confirm Master Passphrase", fontSize = 11.sp, fontFamily = MonospaceFontFamily) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontFamily = MonospaceFontFamily),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Warning Banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TerminalRed.copy(alpha = 0.15f))
                                    .border(1.dp, TerminalRed, RoundedCornerShape(4.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Forgetting this passphrase, encrypted backups CANNOT be recovered. No reset and No recovery.",
                                    color = TerminalRed,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    fontFamily = MonospaceFontFamily
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { unrecoverableUnderstood = !unrecoverableUnderstood },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = unrecoverableUnderstood,
                                    onCheckedChange = { unrecoverableUnderstood = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = TerminalRed,
                                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        checkmarkColor = TerminalBlack
                                    )
                                )
                                Text(
                                    text = "Unrecoverable, understood.",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontFamily = MonospaceFontFamily
                                )
                            }
                        } else {
                            Text(
                                text = "Enter your vault passphrase from your previous phone or setup. TeleBack will decrypt and restore your cloud snapshots.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                fontFamily = MonospaceFontFamily
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = passphraseInput,
                                onValueChange = { passphraseInput = it; step3Error = null },
                                label = { Text("Enter Vault Passphrase", fontSize = 11.sp, fontFamily = MonospaceFontFamily) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontFamily = MonospaceFontFamily),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Target folder picker
                        OutlinedButton(
                            onClick = onPickFolderClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = if (initialFolderName != null) "Folder: $initialFolderName" else "Select Target Folder (SAF)",
                                fontFamily = MonospaceFontFamily,
                                fontSize = 11.sp
                            )
                        }

                        if (step3Error != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = step3Error!!, color = TerminalRed, fontSize = 10.sp, fontFamily = MonospaceFontFamily)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { step = 2 },
                                modifier = Modifier.weight(1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            ) {
                                Text("<- Back", fontFamily = MonospaceFontFamily)
                            }
                            Button(
                                onClick = {
                                    if (isNewVault) {
                                        if (passphraseInput.length < 6) {
                                            step3Error = "Passphrase must be at least 6 characters"
                                            return@Button
                                        }
                                        if (passphraseInput != confirmPassphraseInput) {
                                            step3Error = "Passphrases do not match"
                                            return@Button
                                        }
                                        if (!unrecoverableUnderstood) {
                                            step3Error = "Please check 'Unrecoverable, understood.'"
                                            return@Button
                                        }
                                        val derived = CryptoEngine.deriveKeyFromPassphrase(passphraseInput)
                                        onCompleteSetup(derived, false)
                                    } else {
                                        if (passphraseInput.isBlank()) {
                                            step3Error = "Please enter your vault passphrase"
                                            return@Button
                                        }
                                        val derived = CryptoEngine.deriveKeyFromPassphrase(passphraseInput.trim())
                                        onCompleteSetup(derived, true)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text("Complete Setup", fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
