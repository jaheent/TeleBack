package com.teleback.presentation.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.teleback.core.crypto.CryptoEngine
import com.teleback.core.util.BiometricHelper
import com.teleback.presentation.theme.MonospaceFontFamily
import com.teleback.presentation.theme.TerminalAmber
import com.teleback.presentation.theme.TerminalBlack
import com.teleback.presentation.theme.TerminalCyan
import com.teleback.presentation.theme.TerminalMint
import com.teleback.presentation.theme.TerminalRed

@Composable
fun KeysDialog(
    currentMasterKey: String,
    isBiometricEnabled: Boolean = false,
    onToggleBiometric: (Boolean) -> Unit = {},
    onSaveKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isKeyRevealed by remember { mutableStateOf(!isBiometricEnabled) }
    var restorePassphrase by remember { mutableStateOf("") }
    var newPassphrase by remember { mutableStateOf("") }
    var confirmNewPassphrase by remember { mutableStateOf("") }
    var understood by remember { mutableStateOf(false) }
    var inputKey by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

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
                        text = "SECURITY // MASTER KEY [8]",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MonospaceFontFamily
                    )
                    Text(
                        text = "AES-256-GCM",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Biometric Security Toggle Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Biometric Vault Security",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MonospaceFontFamily
                        )
                        Text(
                            text = if (isBiometricEnabled) "Active: App launch, key export & wipe protected" else "Disabled: No biometric challenge required",
                            color = if (isBiometricEnabled) TerminalMint else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp,
                            fontFamily = MonospaceFontFamily
                        )
                    }

                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { enable ->
                            val activity = context as? FragmentActivity
                            if (activity != null) {
                                BiometricHelper.authenticate(
                                    activity = activity,
                                    title = if (enable) "Enable Biometric Security" else "Disable Biometric Security",
                                    subtitle = "Confirm identity to update vault security",
                                    onSuccess = { onToggleBiometric(enable) },
                                    onError = { errorMessage = it }
                                )
                            } else {
                                onToggleBiometric(enable)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (isKeyRevealed) currentMasterKey else "••••••••••••••••••••••••••••••••••••••••••••",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = {
                        val proceedCopy = {
                            isKeyRevealed = true
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("TeleBack Master Key", currentMasterKey)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Master Key copied to clipboard", Toast.LENGTH_SHORT).show()
                        }

                        if (isBiometricEnabled && !isKeyRevealed) {
                            val activity = context as? FragmentActivity
                            if (activity != null) {
                                BiometricHelper.authenticate(
                                    activity = activity,
                                    title = "Export Master Key",
                                    subtitle = "Authenticate to reveal and copy master encryption key",
                                    onSuccess = proceedCopy,
                                    onError = { errorMessage = it }
                                )
                            } else {
                                proceedCopy()
                            }
                        } else {
                            proceedCopy()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (isBiometricEnabled && !isKeyRevealed) "Authenticate to Reveal & Copy Key" else "Copy Base64 Key to Clipboard",
                        fontFamily = MonospaceFontFamily,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // OPTION 1: Restore Vault with Existing Passphrase
                Text(
                    text = "1. RESTORE WITH PASSPHRASE",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonospaceFontFamily
                )
                Text(
                    text = "Reinstalling on a new phone? Enter your vault passphrase to restore your encryption key.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontFamily = MonospaceFontFamily
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = restorePassphrase,
                    onValueChange = {
                        restorePassphrase = it
                        errorMessage = null
                    },
                    label = { Text("Vault Passphrase", fontSize = 11.sp, fontFamily = MonospaceFontFamily) },
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

                Button(
                    onClick = {
                        val trimmed = restorePassphrase.trim()
                        if (trimmed.isNotBlank()) {
                            val derived = CryptoEngine.deriveKeyFromPassphrase(trimmed)
                            onSaveKey(derived)
                            successMessage = "Vault key derived from passphrase and applied!"
                            restorePassphrase = ""
                        }
                    },
                    enabled = restorePassphrase.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text("Unlock & Apply Key", fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // OPTION 2: Create New Passphrase
                Text(
                    text = "2. CREATE NEW PASSPHRASE",
                    color = TerminalCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonospaceFontFamily
                )
                Text(
                    text = "Derives a fresh 256-bit AES key via PBKDF2.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = MonospaceFontFamily
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = newPassphrase,
                    onValueChange = {
                        newPassphrase = it
                        errorMessage = null
                    },
                    label = { Text("New Passphrase (min 6 chars)", fontSize = 11.sp, fontFamily = MonospaceFontFamily) },
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
                    value = confirmNewPassphrase,
                    onValueChange = {
                        confirmNewPassphrase = it
                        errorMessage = null
                    },
                    label = { Text("Confirm New Passphrase", fontSize = 11.sp, fontFamily = MonospaceFontFamily) },
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
                        .clickable { understood = !understood },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = understood,
                        onCheckedChange = { understood = it },
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

                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        if (newPassphrase.length < 6) {
                            errorMessage = "Passphrase must be at least 6 characters"
                        } else if (newPassphrase != confirmNewPassphrase) {
                            errorMessage = "Passphrases do not match"
                        } else {
                            val derived = CryptoEngine.deriveKeyFromPassphrase(newPassphrase)
                            onSaveKey(derived)
                            successMessage = "New master key derived and saved!"
                            newPassphrase = ""
                            confirmNewPassphrase = ""
                            understood = false
                        }
                    },
                    enabled = newPassphrase.isNotBlank() && confirmNewPassphrase.isNotBlank() && understood,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan, contentColor = TerminalBlack)
                ) {
                    Text("Derive & Set Key", fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // OPTION 3: Import Raw Base64 Key
                Text(
                    text = "3. IMPORT RAW BASE64 KEY",
                    color = TerminalAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
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
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontFamily = MonospaceFontFamily),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        val trimmed = inputKey.trim()
                        if (CryptoEngine.isValidKey(trimmed)) {
                            onSaveKey(trimmed)
                            successMessage = "Imported Base64 master key successfully!"
                            inputKey = ""
                        } else {
                            errorMessage = "Invalid key! Must be a valid 256-bit Base64 key."
                        }
                    },
                    enabled = inputKey.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Import Base64 Key", fontFamily = MonospaceFontFamily, fontSize = 11.sp)
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMessage!!, color = TerminalRed, fontSize = 10.sp, fontFamily = MonospaceFontFamily)
                }

                if (successMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = successMessage!!, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontFamily = MonospaceFontFamily)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("Close", fontFamily = MonospaceFontFamily, fontSize = 11.sp)
                }
            }
        }
    }
}
