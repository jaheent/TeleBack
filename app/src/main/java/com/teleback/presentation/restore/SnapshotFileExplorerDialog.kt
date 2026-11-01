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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.teleback.core.util.SizeFormat
import com.teleback.data.local.SnapshotEntity
import com.teleback.data.model.SnapshotManifest
import com.teleback.domain.DownloadDestination
import com.teleback.presentation.theme.MonospaceFontFamily
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

@Composable
fun SnapshotFileExplorerDialog(
    snapshot: SnapshotEntity,
    isBusy: Boolean,
    extractionBanner: ExtractionBanner?,
    onClearBanner: () -> Unit,
    onExtractFiles: (selectedPaths: Set<String>, destination: DownloadDestination, dismissDialog: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val manifest = remember(snapshot.rawJson) {
        runCatching { Json.decodeFromString<SnapshotManifest>(snapshot.rawJson) }.getOrNull()
    }

    val rootTree = remember(manifest) {
        manifest?.let { SnapshotTreeBuilder.buildTree(it) }
    }

    var searchQuery by remember { mutableStateOf("") }
    val expandedFolderPaths = remember { mutableStateListOf<String>() }
    val selectedPaths = remember { mutableStateListOf<String>() }

    val visibleNodes = remember(rootTree, expandedFolderPaths.toList(), searchQuery) {
        rootTree?.let {
            SnapshotTreeBuilder.flattenVisibleTree(it, expandedFolderPaths.toSet(), searchQuery)
        } ?: emptyList()
    }

    val visibleFilePaths = remember(visibleNodes) {
        visibleNodes.filterIsInstance<SnapshotTreeNode.FileNode>().map { it.relativePath }
    }

    val isAllSelected = visibleFilePaths.isNotEmpty() && visibleFilePaths.all { it in selectedPaths }

    // Auto-dismiss banner after 3 seconds if not busy
    LaunchedEffect(extractionBanner, isBusy) {
        if (extractionBanner != null && !isBusy) {
            delay(3000)
            onClearBanner()
        }
    }

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
                            text = "Filter files or folders (.png, src, doc)...",
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
                                    visibleFilePaths.forEach { path ->
                                        if (path !in selectedPaths) selectedPaths.add(path)
                                    }
                                } else {
                                    visibleFilePaths.forEach { path ->
                                        selectedPaths.remove(path)
                                    }
                                }
                            },
                            enabled = visibleFilePaths.isNotEmpty(),
                            colors = CheckboxDefaults.colors(
                                checkedColor = colorScheme.primary,
                                checkmarkColor = colorScheme.onPrimary
                            )
                        )
                        Text(
                            text = "Select All (${visibleFilePaths.size})",
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

                // Virtualized Hierarchical Accordion Tree
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(4.dp))
                        .padding(4.dp)
                ) {
                    if (visibleNodes.isEmpty()) {
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
                            items(visibleNodes, key = { it.relativePath }) { node ->
                                when (node) {
                                    is SnapshotTreeNode.FolderNode -> {
                                        val isExpanded = node.relativePath in expandedFolderPaths
                                        val selectionState = remember(node, selectedPaths.toList()) {
                                            SnapshotTreeBuilder.getFolderSelectionState(node, selectedPaths.toSet())
                                        }

                                        FolderTreeRow(
                                            folder = node,
                                            isExpanded = isExpanded,
                                            selectionState = selectionState,
                                            onToggleExpand = {
                                                if (isExpanded) {
                                                    expandedFolderPaths.remove(node.relativePath)
                                                } else {
                                                    expandedFolderPaths.add(node.relativePath)
                                                }
                                            },
                                            onToggleSelection = {
                                                val descendantPaths = SnapshotTreeBuilder.getAllDescendantFilePaths(node)
                                                if (selectionState == true || selectionState == null) {
                                                    selectedPaths.removeAll(descendantPaths)
                                                } else {
                                                    descendantPaths.forEach {
                                                        if (it !in selectedPaths) selectedPaths.add(it)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                    is SnapshotTreeNode.FileNode -> {
                                        val isSelected = node.relativePath in selectedPaths

                                        FileTreeRow(
                                            fileNode = node,
                                            isSelected = isSelected,
                                            isBusy = isBusy,
                                            onToggleSelect = {
                                                if (isSelected) selectedPaths.remove(node.relativePath)
                                                else selectedPaths.add(node.relativePath)
                                            },
                                            onDownloadSingle = { dest ->
                                                onExtractFiles(setOf(node.relativePath), dest, false)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // In-Dialog Status Banner
                if (extractionBanner != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (extractionBanner.isSuccess) colorScheme.primary.copy(alpha = 0.15f)
                                else colorScheme.error.copy(alpha = 0.15f)
                            )
                            .border(
                                1.dp,
                                if (extractionBanner.isSuccess) colorScheme.primary.copy(alpha = 0.7f)
                                else colorScheme.error.copy(alpha = 0.7f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = colorScheme.primary
                                )
                            }
                            Text(
                                text = extractionBanner.message,
                                color = if (extractionBanner.isSuccess) colorScheme.primary else colorScheme.error,
                                fontSize = 11.sp,
                                fontFamily = MonospaceFontFamily
                            )
                        }
                        Text(
                            text = "✕",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontFamily = MonospaceFontFamily,
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .clickable { onClearBanner() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Buttons for Batch Download
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (selectedPaths.isNotEmpty()) {
                                onExtractFiles(selectedPaths.toSet(), DownloadDestination.SYNCED_FOLDER, false)
                            }
                        },
                        enabled = selectedPaths.isNotEmpty() && !isBusy,
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
                                onExtractFiles(selectedPaths.toSet(), DownloadDestination.DOWNLOADS_FOLDER, false)
                            }
                        },
                        enabled = selectedPaths.isNotEmpty() && !isBusy,
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
private fun FolderTreeRow(
    folder: SnapshotTreeNode.FolderNode,
    isExpanded: Boolean,
    selectionState: Boolean?,
    onToggleExpand: () -> Unit,
    onToggleSelection: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val indent = minOf(folder.depth, 4) * 12

    val toggleState = when (selectionState) {
        true -> ToggleableState.On
        false -> ToggleableState.Off
        null -> ToggleableState.Indeterminate
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .clickable(onClick = onToggleExpand)
            .padding(start = indent.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TriStateCheckbox(
            state = toggleState,
            onClick = onToggleSelection,
            enabled = folder.totalFilesCount > 0,
            colors = CheckboxDefaults.colors(
                checkedColor = colorScheme.primary,
                checkmarkColor = colorScheme.onPrimary
            )
        )

        // Chevron expand/collapse toggle
        Text(
            text = if (isExpanded) "▾" else "▸",
            color = colorScheme.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        // Folder badge [DIR]
        Text(
            text = "[DIR]",
            color = colorScheme.secondary,
            fontSize = 10.sp,
            fontFamily = MonospaceFontFamily,
            modifier = Modifier.padding(end = 4.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                color = colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonospaceFontFamily
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (folder.isEmptyFolder) "(empty)" else "${folder.totalFilesCount} files • ${SizeFormat.format(folder.totalBytes)}",
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontFamily = MonospaceFontFamily
                )
                if (folder.depth > 4) {
                    val segments = folder.relativePath.split('/')
                    val parentHint = if (segments.size >= 2) "../${segments[segments.size - 2]}/" else ""
                    if (parentHint.isNotEmpty()) {
                        Text(
                            text = parentHint,
                            color = colorScheme.outline,
                            fontSize = 8.sp,
                            fontFamily = MonospaceFontFamily
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileTreeRow(
    fileNode: SnapshotTreeNode.FileNode,
    isSelected: Boolean,
    isBusy: Boolean,
    onToggleSelect: () -> Unit,
    onDownloadSingle: (DownloadDestination) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val file = fileNode.fileEntry
    val indent = minOf(fileNode.depth, 4) * 12

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) colorScheme.primary.copy(alpha = 0.12f) else colorScheme.surfaceVariant.copy(alpha = 0.08f))
            .border(
                1.dp,
                if (isSelected) colorScheme.primary.copy(alpha = 0.5f) else colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onToggleSelect)
            .padding(start = indent.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelect() },
            modifier = Modifier.padding(end = 2.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = colorScheme.primary,
                checkmarkColor = colorScheme.onPrimary
            )
        )

        // File badge [FILE]
        Text(
            text = "[FILE]",
            color = colorScheme.primary,
            fontSize = 10.sp,
            fontFamily = MonospaceFontFamily,
            modifier = Modifier.padding(end = 4.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileNode.name,
                color = colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MonospaceFontFamily
            )
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

        // Inline Quick Action Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(
                onClick = { onDownloadSingle(DownloadDestination.SYNCED_FOLDER) },
                enabled = !isBusy,
                modifier = Modifier.height(26.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.secondary.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.secondary),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
                Text("Folder", fontSize = 9.sp, fontFamily = MonospaceFontFamily)
            }

            OutlinedButton(
                onClick = { onDownloadSingle(DownloadDestination.DOWNLOADS_FOLDER) },
                enabled = !isBusy,
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
