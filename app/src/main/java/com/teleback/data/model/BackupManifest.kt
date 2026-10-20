package com.teleback.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChunkPart(
    val partIndex: Int,
    val size: Long,
    val sha256: String,
    val messageId: Long,
    val fileId: String
)

@Serializable
data class FileEntry(
    val relativePath: String,
    val size: Long,
    val sha256: String,
    val modifiedAt: Long,
    val messageId: Long,
    val fileId: String,
    val chunkCount: Int = 1,
    val chunkParts: List<ChunkPart> = emptyList(),
    /**
     * True when every part was encrypted independently (streamed upload, TeleBack 1.1+).
     * False/absent for legacy entries, where the whole file was encrypted first and then split.
     */
    val chunkEncrypted: Boolean = false
)

@Serializable
data class SnapshotManifest(
    val version: Int = 1,
    val snapshotId: String,
    val tag: String? = null,
    val timestamp: Long,
    val deviceName: String,
    val rootFolderName: String,
    val totalFiles: Int,
    val totalBytes: Long,
    val isEncrypted: Boolean = true,
    val directories: List<String> = emptyList(),
    val files: List<FileEntry>
)
