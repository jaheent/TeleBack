package com.teleback.presentation.cloud

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.teleback.core.storage.ManifestManager
import com.teleback.data.local.SnapshotEntity
import com.teleback.data.model.SnapshotManifest
import com.teleback.presentation.theme.MonospaceFontFamily
import com.teleback.presentation.theme.TerminalAmber
import com.teleback.presentation.theme.TerminalBlack
import com.teleback.presentation.theme.TerminalBorder
import com.teleback.presentation.theme.TerminalCyan
import com.teleback.presentation.theme.TerminalMint
import com.teleback.presentation.theme.TerminalRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudManagerDialog(
    snapshots: List<SnapshotEntity>,
    isScanning: Boolean,
    onScanCloud: () -> Unit,
    onDeleteSelected: (List<String>) -> Unit,
    onWipeAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val selectedIds = remember { mutableStateListOf<String>() }
    var expandedSnapshotId by remember { mutableStateOf<String?>(null) }
    var showWipeConfirm by remember { mutableStateOf(false) }

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
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CLOUD // SNAPSHOT MANAGER [4]",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MonospaceFontFamily
                    )
                    Text(
                        text = "${snapshots.size} Remote",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onScanCloud,
                        enabled = !isScanning,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCyan)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = TerminalCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scanning...", fontSize = 10.sp, fontFamily = MonospaceFontFamily)
                        } else {
                            Text("Scan Cloud", fontSize = 10.sp, fontFamily = MonospaceFontFamily)
                        }
                    }

                    if (snapshots.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                if (selectedIds.size == snapshots.size) {
                                    selectedIds.clear()
                                } else {
                                    selectedIds.clear()
                                    selectedIds.addAll(snapshots.map { it.snapshotId })
                                }
                            },
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text(
                                text = if (selectedIds.size == snapshots.size) "Deselect All" else "Select All",
                                fontSize = 10.sp,
                                fontFamily = MonospaceFontFamily
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Snapshots list
                if (snapshots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isScanning) "Discovering cloud snapshots in Telegram..." else "No snapshots found.\nTap 'Scan Cloud' to query Telegram group or run [1] sync / [2] backup.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontFamily = MonospaceFontFamily,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .height((snapshots.size.coerceAtMost(4) * 85).dp)
                    ) {
                        items(snapshots, key = { it.snapshotId }) { snap ->
                            val isSelected = selectedIds.contains(snap.snapshotId)
                            val isExpanded = expandedSnapshotId == snap.snapshotId
                            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(snap.timestamp))
                            val sizeMb = String.format(Locale.US, "%.1f MB", snap.totalBytes / (1024.0 * 1024.0))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) TerminalRed else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) selectedIds.add(snap.snapshotId) else selectedIds.remove(snap.snapshotId)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = TerminalRed,
                                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            checkmarkColor = TerminalBlack
                                        )
                                    )

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                expandedSnapshotId = if (isExpanded) null else snap.snapshotId
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = snap.snapshotId,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = MonospaceFontFamily
                                            )
                                            Text(
                                                text = sizeMb,
                                                color = TerminalCyan,
                                                fontSize = 11.sp,
                                                fontFamily = MonospaceFontFamily
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "$dateStr • ${snap.totalFiles} files • [${snap.tag ?: "Auto"}]",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp,
                                            fontFamily = MonospaceFontFamily
                                        )
                                    }
                                }

                                // Expanded file preview
                                AnimatedVisibility(visible = isExpanded) {
                                    val manifest = runCatching {
                                        ManifestManager.json.decodeFromString<SnapshotManifest>(snap.rawJson)
                                    }.getOrNull()

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp, start = 8.dp, end = 8.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "Root: ${snap.rootFolderName ?: "Unknown"}",
                                            color = TerminalAmber,
                                            fontSize = 10.sp,
                                            fontFamily = MonospaceFontFamily
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (manifest != null && manifest.files.isNotEmpty()) {
                                            manifest.files.take(6).forEach { file ->
                                                Text(
                                                    text = "• ${file.relativePath} (${String.format(Locale.US, "%.1f KB", file.size / 1024.0)})",
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 9.sp,
                                                    fontFamily = MonospaceFontFamily,
                                                    maxLines = 1
                                                )
                                            }
                                            if (manifest.files.size > 6) {
                                                Text(
                                                    text = "+ ${manifest.files.size - 6} more files",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 9.sp,
                                                    fontFamily = MonospaceFontFamily
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "No file details available",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 9.sp,
                                                fontFamily = MonospaceFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Delete selected button
                if (selectedIds.isNotEmpty()) {
                    Button(
                        onClick = {
                            val toDelete = selectedIds.toList()
                            selectedIds.clear()
                            onDeleteSelected(toDelete)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalRed, contentColor = TerminalBlack)
                    ) {
                        Text(
                            text = "Delete Selected (${selectedIds.size})",
                            fontFamily = MonospaceFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Wipe All button
                if (snapshots.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showWipeConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalRed)
                    ) {
                        Text(
                            text = "Wipe Vault / Delete All",
                            fontFamily = MonospaceFontFamily,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Close button
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

    // Confirmation Modal for Wipe All
    if (showWipeConfirm) {
        var confirmText by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showWipeConfirm = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, TerminalRed, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "DANGER // WIPE ALL CLOUD DATA",
                        color = TerminalRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MonospaceFontFamily
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will permanently delete ALL snapshots, manifests, and chunk files from your Telegram Cloud Group.\n\nThis action CANNOT BE UNDONE.",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontFamily = MonospaceFontFamily
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Type 'delete' to confirm:",
                        color = TerminalAmber,
                        fontSize = 10.sp,
                        fontFamily = MonospaceFontFamily
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = TerminalRed, fontSize = 12.sp, fontFamily = MonospaceFontFamily),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TerminalRed,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showWipeConfirm = false },
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Text("Cancel", fontFamily = MonospaceFontFamily, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                if (confirmText.trim() == "delete") {
                                    showWipeConfirm = false
                                    onWipeAll()
                                }
                            },
                            enabled = confirmText.trim() == "delete",
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalRed, contentColor = TerminalBlack)
                        ) {
                            Text("Wipe Vault", fontFamily = MonospaceFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
