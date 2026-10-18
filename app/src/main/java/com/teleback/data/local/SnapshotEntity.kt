package com.teleback.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snapshots")
data class SnapshotEntity(
    @PrimaryKey val snapshotId: String,
    val tag: String?,
    val timestamp: Long,
    val rootFolderName: String,
    val totalFiles: Int,
    val totalBytes: Long,
    val isEncrypted: Boolean,
    val manifestMessageId: Long,
    val manifestFileId: String,
    val rawJson: String
)
