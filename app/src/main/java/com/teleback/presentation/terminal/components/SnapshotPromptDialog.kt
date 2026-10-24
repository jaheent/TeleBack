package com.teleback.presentation.terminal.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.teleback.presentation.theme.MonospaceFontFamily

@Composable
fun SnapshotPromptDialog(
    isFullBackup: Boolean,
    onConfirm: (tag: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var tagText by remember { mutableStateOf("") }
    val colorScheme = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, colorScheme.primary, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = if (isFullBackup) "TRIGGER FULL BACKUP [2]" else "TRIGGER DIFF SYNC [1]",
                    color = colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonospaceFontFamily
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Add a custom label to easily identify this snapshot when restoring:",
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = MonospaceFontFamily
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tagText,
                    onValueChange = { tagText = it },
                    label = { Text("Snapshot Label (Optional)", fontSize = 11.sp, fontFamily = MonospaceFontFamily) },
                    placeholder = { Text("e.g. Pre-Update, Trip Photos", fontSize = 11.sp, fontFamily = MonospaceFontFamily) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = colorScheme.onSurface, fontSize = 11.sp, fontFamily = MonospaceFontFamily),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurfaceVariant)
                    ) {
                        Text("Cancel", fontFamily = MonospaceFontFamily, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val cleanTag = tagText.trim().ifEmpty { null }
                            onConfirm(cleanTag)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Proceed",
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
