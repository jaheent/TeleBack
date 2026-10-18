package com.teleback.core.storage

import com.teleback.data.model.ChunkPart
import com.teleback.data.model.FileEntry
import com.teleback.data.model.SnapshotManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotPruningTest {

    @Test
    fun testOrphanFileDetection() {
        // Active snapshots share messageId 1001
        val activeSnapshots = listOf(
            SnapshotManifest(
                snapshotId = "snap_2",
                timestamp = 2000L,
                deviceName = "TestDevice",
                rootFolderName = "Vault",
                totalFiles = 1,
                totalBytes = 500,
                directories = listOf("subfolder"),
                files = listOf(
                    FileEntry(
                        relativePath = "subfolder/shared_doc.txt",
                        size = 500,
                        sha256 = "hash_shared",
                        modifiedAt = 2000L,
                        messageId = 1001L,
                        fileId = "file_shared"
                    )
                )
            )
        )

        val activeMessageIds = activeSnapshots.flatMap { snap ->
            snap.files.flatMap { file ->
                listOf(file.messageId) + file.chunkParts.map { it.messageId }
            }
        }.toSet()

        // Oldest snapshot being pruned has shared file (1001) AND an obsolete file (999)
        val oldSnapshot = SnapshotManifest(
            snapshotId = "snap_1",
            timestamp = 1000L,
            deviceName = "TestDevice",
            rootFolderName = "Vault",
            totalFiles = 2,
            totalBytes = 800,
            directories = listOf("subfolder"),
            files = listOf(
                FileEntry(
                    relativePath = "subfolder/shared_doc.txt",
                    size = 500,
                    sha256 = "hash_shared",
                    modifiedAt = 1000L,
                    messageId = 1001L,
                    fileId = "file_shared"
                ),
                FileEntry(
                    relativePath = "subfolder/deleted_old.txt",
                    size = 300,
                    sha256 = "hash_deleted",
                    modifiedAt = 1000L,
                    messageId = 999L,
                    fileId = "file_deleted"
                )
            )
        )

        val orphanedFiles = oldSnapshot.files.filter { !activeMessageIds.contains(it.messageId) }
        val sharedFiles = oldSnapshot.files.filter { activeMessageIds.contains(it.messageId) }

        // Assert shared file is protected and NOT treated as orphan
        assertEquals(1, sharedFiles.size)
        assertEquals(1001L, sharedFiles[0].messageId)

        // Assert deleted/replaced file is identified as orphan for cloud deletion
        assertEquals(1, orphanedFiles.size)
        assertEquals(999L, orphanedFiles[0].messageId)
    }

    @Test
    fun testPruningThresholdLimit() {
        val totalSnapshots = 13
        val maxLimit = 10
        val excessCount = (totalSnapshots - maxLimit).coerceAtLeast(0)
        assertEquals(3, excessCount)
    }
}
