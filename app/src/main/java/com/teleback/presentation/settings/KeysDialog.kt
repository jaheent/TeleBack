package com.teleback.presentation.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.teleback.core.crypto.CryptoEngine
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
fun KeysDialog(
    currentMasterKey: String,
    onSaveKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var inputKey by remember { mutableStateOf("") }
    var passphraseInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, TerminalAmber, RoundedCornerShape(8.dp)),
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
                        text = "SECURITY // MASTER KEY [8]",
                        color = TerminalAmber,
                        fontSize = 13.sp,
                        fontFamily = MonospaceFontFamily
                    )
                    Text(
                        text = "AES-GCM",
                        color = TerminalTextMuted,
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "ACTIVE MASTER KEY (BASE64):",
                    color = TerminalCyan,
                    fontSize = 11.sp,
                    fontFamily = MonospaceFontFamily
                )
                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(TerminalSurface)
                        .border(1.dp, TerminalBorder, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = currentMasterKey,
                        color = TerminalMint,
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("TeleBack Master Key", currentMasterKey)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Master Key copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalMint),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalMint)
                ) {
                    Text("Copy Key to Clipboard", fontFamily = MonospaceFontFamily)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Save this key if switching phones or reinstalling. Without it, encrypted cloud backups cannot be decrypted.",
                    color = TerminalAmber,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontFamily = MonospaceFontFamily
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = TerminalBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Option A: Set Custom Memorable Passphrase
                Text(
                    text = "OPTION A: SET CUSTOM PASSPHRASE",
                    color = TerminalCyan,
                    fontSize = 11.sp,
                    fontFamily = MonospaceFontFamily
                )
                Text(
                    text = "Derives a 256-bit AES key via PBKDF2. You can type this same passphrase on any phone.",
                    color = TerminalTextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    fontFamily = MonospaceFontFamily
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = passphraseInput,
                    onValueChange = {
                        passphraseInput = it
                        errorMessage = null
                    },
                    label = { Text("Enter Memorable Passphrase", fontSize = 11.sp, fontFamily = MonospaceFontFamily) },
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

                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        if (passphraseInput.length < 6) {
                            errorMessage = "Passphrase must be at least 6 characters"
                        } else {
                            val derived = CryptoEngine.deriveKeyFromPassphrase(passphraseInput)
                            onSaveKey(derived)
                            successMessage = "New key derived from passphrase and saved!"
                            passphraseInput = ""
                        }
                    },
                    enabled = passphraseInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan, contentColor = TerminalBlack)
                ) {
                    Text("Derive & Set Key", fontFamily = MonospaceFontFamily)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = TerminalBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Option B: Import Existing Key
                Text(
                    text = "OPTION B: IMPORT SAVED BASE64 KEY",
                    color = TerminalCyan,
                    fontSize = 11.sp,
                    fontFamily = MonospaceFontFamily
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = {
                        inputKey = it
                        errorMessage = null
                    },
                    label = { Text("Paste 44-char Base64 Key", fontSize = 11.sp, fontFamily = MonospaceFontFamily) },
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

                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        val trimmed = inputKey.trim()
                        if (CryptoEngine.isValidKey(trimmed)) {
                            onSaveKey(trimmed)
                            successMessage = "Imported master key successfully!"
                            inputKey = ""
                        } else {
                            errorMessage = "Invalid key! Must be a valid 256-bit Base64 key."
                        }
                    },
                    enabled = inputKey.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalMint, contentColor = TerminalBlack)
                ) {
                    Text("Import & Apply Key", fontFamily = MonospaceFontFamily)
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMessage!!, color = TerminalRed, fontSize = 10.sp, fontFamily = MonospaceFontFamily)
                }

                if (successMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = successMessage!!, color = TerminalMint, fontSize = 10.sp, fontFamily = MonospaceFontFamily)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalTextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
                ) {
                    Text("Close", fontFamily = MonospaceFontFamily)
                }
            }
        }
    }
}
