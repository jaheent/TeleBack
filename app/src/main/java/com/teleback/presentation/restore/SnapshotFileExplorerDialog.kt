package com.teleback.presentation.restore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.DialogProperties
import com.teleback.core.util.SizeFormat
import com.teleback.data.local.SnapshotEntity
import com.teleback.data.model.FileEntry
import com.teleback.data.model.SnapshotManifest
import com.teleback.domain.DownloadDestination
import com.teleback.presentation.theme.MonospaceFontFamily
import kotlinx.serialization.json.Json

@Composable
fun SnapshotFileExplorerDialog(
    snapshot: SnapshotEntity,
    onExtractFiles: (selectedPaths: Set<String>, destination: DownloadDestination) -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val manifest = remember(snapshot.rawJson) {
        runCatching { Json.decodeFromString<SnapshotManifest>(snapshot.rawJson) }.getOrNull()
    }

    var searchQuery by remember { mutableStateOf("") }
    val selectedPaths = remember { mutableStateListOf<String>() }

    val allFiles = manifest?.files ?: emptyList()
    val filteredFiles = remember(allFiles, searchQuery) {
        if (searchQuery.isBlank()) {
            allFiles
        } else {
            val q = searchQuery.trim().lowercase()
            allFiles.filter { it.relativePath.lowercase().contains(q) }
        }
    }

    val isAllSelected = filteredFiles.isNotEmpty() && filteredFiles.all { it.relativePath in selectedPaths }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, colorScheme.primary, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxHeight()
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SNAPSHOT FILE EXPLORER",
                            color = colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MonospaceFontFamily
                        )
                        Text(
                            text = snapshot.tag?.let { "[$it] • ${snapshot.snapshotId}" } ?: snapshot.snapshotId,
                            color = colorScheme.secondary,
                            fontSize = 11.sp,
                            fontFamily = MonospaceFontFamily
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurfaceVariant),
                        modifier = Modifier.height(30.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("CLOSE", fontSize = 10.sp, fontFamily = MonospaceFontFamily)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Filter TextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Filter files by name or extension (.png, .doc)...",
                            fontSize = 11.sp,
                            fontFamily = MonospaceFontFamily
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Batch Selection Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isAllSelected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    filteredFiles.forEach { file ->
                                        if (file.relativePath !in selectedPaths) selectedPaths.add(file.relativePath)
                                    }
                                } else {
                                    filteredFiles.forEach { file ->
                                        selectedPaths.remove(file.relativePath)
                                    }
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colorScheme.primary,
                                checkmarkColor = colorScheme.onPrimary
                            )
                        )
                        Text(
                            text = "Select All (${filteredFiles.size})",
                            color = colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontFamily = MonospaceFontFamily
                        )
                    }

                    Text(
                        text = "${selectedPaths.size} selected",
                        color = if (selectedPaths.isNotEmpty()) colorScheme.primary else colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = if (selectedPaths.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = MonospaceFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // File List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(4.dp))
                        .padding(4.dp)
                ) {
                    if (filteredFiles.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxHeight().fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "No files found in snapshot." else "No files matching '$searchQuery'.",
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontFamily = MonospaceFontFamily
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
                            items(filteredFiles, key = { it.relativePath }) { file ->
                                val isSelected = file.relativePath in selectedPaths

                                FileItemRow(
                                    file = file,
                                    isSelected = isSelected,
                                    onToggleSelect = {
                                        if (isSelected) selectedPaths.remove(file.relativePath)
                                        else selectedPaths.add(file.relativePath)
                                    },
                                    onDownloadSingle = { dest ->
                                        onExtractFiles(setOf(file.relativePath), dest)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons for Batch Download
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (selectedPaths.isNotEmpty()) {
                                onExtractFiles(selectedPaths.toSet(), DownloadDestination.SYNCED_FOLDER)
                            }
                        },
                        enabled = selectedPaths.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.secondary,
                            contentColor = colorScheme.onSecondary
                        )
                    ) {
                        Text(
                            text = "To Folder (${selectedPaths.size})",
                            fontSize = 11.sp,
                            fontFamily = MonospaceFontFamily
                        )
                    }

                    Button(
                        onClick = {
                            if (selectedPaths.isNotEmpty()) {
                                onExtractFiles(selectedPaths.toSet(), DownloadDestination.DOWNLOADS_FOLDER)
                            }
                        },
                        enabled = selectedPaths.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "To Downloads (${selectedPaths.size})",
                            fontSize = 11.sp,
                            fontFamily = MonospaceFontFamily
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItemRow(
    file: FileEntry,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onDownloadSingle: (DownloadDestination) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) colorScheme.primary.copy(alpha = 0.12f) else colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(
                1.dp,
                if (isSelected) colorScheme.primary.copy(alpha = 0.5f) else colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onToggleSelect)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                modifier = Modifier.padding(end = 4.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = colorScheme.primary,
                    checkmarkColor = colorScheme.onPrimary
                )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.relativePath,
                    color = colorScheme.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MonospaceFontFamily
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SizeFormat.format(file.size),
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontFamily = MonospaceFontFamily
                    )
                    if (file.chunkCount > 1) {
                        Text(
                            text = "${file.chunkCount} parts",
                            color = colorScheme.secondary,
                            fontSize = 9.sp,
                            fontFamily = MonospaceFontFamily
                        )
                    }
                    Text(
                        text = file.sha256.take(8),
                        color = colorScheme.outline,
                        fontSize = 9.sp,
                        fontFamily = MonospaceFontFamily
                    )
                }
            }

            // Quick Single-File Actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = { onDownloadSingle(DownloadDestination.SYNCED_FOLDER) },
                    modifier = Modifier.height(26.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.secondary.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.secondary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text("Folder", fontSize = 9.sp, fontFamily = MonospaceFontFamily)
                }

                OutlinedButton(
                    onClick = { onDownloadSingle(DownloadDestination.DOWNLOADS_FOLDER) },
                    modifier = Modifier.height(26.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.primary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text("Download", fontSize = 9.sp, fontFamily = MonospaceFontFamily)
                }
            }
        }
    }
}
