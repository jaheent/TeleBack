package com.teleback.data.model

import android.net.Uri

data class LocalFileItem(
    val uri: Uri,
    val relativePath: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    var sha256: String? = null
)

enum class DiffStatus {
    ADDED,
    MODIFIED,
    UNCHANGED,
    DELETED
}

data class FileDiffItem(
    val relativePath: String,
    val status: DiffStatus,
    val localFile: LocalFileItem? = null,
    val remoteEntry: FileEntry? = null
)

data class DiffSummary(
    val totalLocalFiles: Int,
    val addedCount: Int,
    val modifiedCount: Int,
    val unchangedCount: Int,
    val deletedCount: Int,
    val bytesToUpload: Long,
    val items: List<FileDiffItem>
)
