package com.teleback.domain

import com.teleback.data.model.ChunkPart
import com.teleback.data.model.FileEntry
import com.teleback.data.model.SnapshotManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageCalculatorTest {

    private fun manifest(id: String, ts: Long, encrypted: Boolean, vararg files: FileEntry) = SnapshotManifest(
        snapshotId = id, timestamp = ts, deviceName = "d", rootFolderName = "r",
        totalFiles = files.size, totalBytes = files.sumOf { it.size },
        isEncrypted = encrypted, files = files.toList()
    )

    private fun file(path: String, size: Long, msg: Long) = FileEntry(
        relativePath = path, size = size, sha256 = "h$msg", modifiedAt = 0, messageId = msg, fileId = "f$msg"
    )

    @Test
    fun sharedFilesAreCountedOnceAcrossSnapshots() {
        val shared = file("shared.txt", 1000, 1)
        val newer = manifest("snap_2", 2000, true, shared, file("new.txt", 500, 2))
        val older = manifest("snap_1", 1000, true, shared)

        val usage = UsageCalculator.compute(listOf(newer, older), snapshotLimit = 10)

        assertEquals(2, usage.snapshotCount)
        assertEquals(10, usage.snapshotLimit)
        assertEquals(2, usage.uniqueFiles) // shared.txt + new.txt, not 3
        assertEquals(1500L, usage.uniqueLogicalBytes)
        assertEquals(1500L + 2 * UsageCalculator.ENCRYPTION_OVERHEAD_PER_PART, usage.storedBytes)
        assertEquals(2000L, usage.lastSyncMillis)
        assertTrue(usage.isEncrypted)
    }

    @Test
    fun chunkedFilesUseActualPartSizes() {
        val chunked = FileEntry(
            relativePath = "big.bin", size = 300, sha256 = "h", modifiedAt = 0,
            messageId = 10, fileId = "f10", chunkCount = 2,
            chunkParts = listOf(
                ChunkPart(1, 132, "a", 10, "f10"),
                ChunkPart(2, 232, "b", 11, "f11")
            ),
            chunkEncrypted = true
        )
        val usage = UsageCalculator.compute(listOf(manifest("s", 1, true, chunked)), 10)
        assertEquals(1, usage.uniqueFiles)
        assertEquals(364L, usage.storedBytes)
        assertEquals(300L, usage.uniqueLogicalBytes)
        assertTrue(usage.overheadPercent > 0.0)
    }

    @Test
    fun unencryptedHasNoOverheadAndEmptyIsSafe() {
        val plain = UsageCalculator.compute(listOf(manifest("s", 1, false, file("a", 100, 1))), 10)
        assertEquals(100L, plain.storedBytes)
        assertEquals(0.0, plain.overheadPercent, 0.0001)

        val empty = UsageCalculator.compute(emptyList(), 10)
        assertEquals(0, empty.snapshotCount)
        assertEquals(0L, empty.storedBytes)
        assertEquals(null, empty.lastSyncMillis)
        assertEquals(0.0, empty.overheadPercent, 0.0001)
    }
}
