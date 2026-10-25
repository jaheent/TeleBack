package com.teleback.domain

import com.teleback.core.crypto.ChecksumUtils
import com.teleback.core.crypto.CryptoEngine
import com.teleback.data.model.FileEntry
import com.teleback.data.model.SnapshotManifest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectiveExtractionTest {

    @Test
    fun testManifestParsingAndSelectiveFiltering() {
        val file1 = FileEntry(
            relativePath = "documents/contract.pdf",
            size = 1024,
            sha256 = "abc123sha",
            modifiedAt = 1792500000000L,
            messageId = 101L,
            fileId = "tg_doc_1",
            chunkCount = 1,
            chunkParts = emptyList()
        )
        val file2 = FileEntry(
            relativePath = "images/photo.png",
            size = 2048,
            sha256 = "def456sha",
            modifiedAt = 1792500001000L,
            messageId = 102L,
            fileId = "tg_doc_2",
            chunkCount = 1,
            chunkParts = emptyList()
        )
        val file3 = FileEntry(
            relativePath = "notes.txt",
            size = 100,
            sha256 = "789ghi",
            modifiedAt = 1792500002000L,
            messageId = 103L,
            fileId = "tg_doc_3",
            chunkCount = 1,
            chunkParts = emptyList()
        )

        val manifest = SnapshotManifest(
            snapshotId = "snap_20261025_001",
            tag = "Test Snapshot",
            timestamp = 1792600000000L,
            deviceName = "Pixel 9",
            rootFolderName = "Vault",
            totalFiles = 3,
            totalBytes = 3172,
            isEncrypted = true,
            files = listOf(file1, file2, file3)
        )

        val jsonString = Json.encodeToString(manifest)
        val decoded = Json.decodeFromString<SnapshotManifest>(jsonString)

        assertEquals("snap_20261025_001", decoded.snapshotId)
        assertEquals("Test Snapshot", decoded.tag)
        assertEquals(3, decoded.files.size)

        // Test selective filtering by relative paths
        val selectedSet = setOf("documents/contract.pdf", "notes.txt")
        val filtered = decoded.files.filter { it.relativePath in selectedSet }

        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.relativePath == "documents/contract.pdf" })
        assertTrue(filtered.any { it.relativePath == "notes.txt" })
    }

    @Test
    fun testSelectiveDecryptionAndShaVerification() {
        val masterKey = CryptoEngine.generateMasterKeyBase64()
        val originalContent = "Confidential business document content for selective download."
        val originalBytes = originalContent.toByteArray(Charsets.UTF_8)
        val expectedSha = ChecksumUtils.sha256(originalBytes)

        val encryptedBytes = CryptoEngine.encryptBytes(originalBytes, masterKey)
        val decryptedBytes = CryptoEngine.decryptBytes(encryptedBytes, masterKey)
        val actualSha = ChecksumUtils.sha256(decryptedBytes)

        assertEquals(expectedSha, actualSha)
        assertEquals(originalContent, String(decryptedBytes, Charsets.UTF_8))
    }

    @Test
    fun testSelectiveChunkReassembly() {
        val masterKey = CryptoEngine.generateMasterKeyBase64()
        val bigData = ByteArray(50_000) { (it % 256).toByte() }
        val originalSha = ChecksumUtils.sha256(bigData)

        // Split into chunks
        val chunkSize = 20_000
        val rawChunks = bigData.toList().chunked(chunkSize).map { it.toByteArray() }
        assertEquals(3, rawChunks.size)

        // Encrypt parts individually (streamed format)
        val encryptedParts = rawChunks.map { CryptoEngine.encryptBytes(it, masterKey) }

        // Selective extraction reassembles and decrypts part-by-part
        val reassembled = mutableListOf<Byte>()
        for (part in encryptedParts) {
            val decryptedPart = CryptoEngine.decryptBytes(part, masterKey)
            reassembled.addAll(decryptedPart.toList())
        }

        val reassembledBytes = reassembled.toByteArray()
        assertEquals(bigData.size, reassembledBytes.size)
        assertEquals(originalSha, ChecksumUtils.sha256(reassembledBytes))
    }

    @Test
    fun testDownloadDestinationEnumValues() {
        val folderDest = DownloadDestination.SYNCED_FOLDER
        val downloadsDest = DownloadDestination.DOWNLOADS_FOLDER

        assertNotNull(folderDest)
        assertNotNull(downloadsDest)
        assertEquals(2, DownloadDestination.entries.size)
    }

    @Test
    fun testManifestMergingInPlaceWithoutNewSnapshot() {
        val file1 = FileEntry("doc1.pdf", 100, "sha1", 1000L, 1L, "tg1")
        val file2 = FileEntry("doc2.pdf", 200, "sha2", 1000L, 2L, "tg2")
        val file3 = FileEntry("doc3.pdf", 300, "sha3", 1000L, 3L, "tg3")

        val existingManifest = SnapshotManifest(
            snapshotId = "snap_20261025_001",
            tag = "Base Vault",
            timestamp = 1000L,
            deviceName = "Pixel 9",
            rootFolderName = "Vault",
            totalFiles = 3,
            totalBytes = 600,
            isEncrypted = true,
            files = listOf(file1, file2, file3)
        )

        // Selective upload of updated doc2 and new doc4
        val updatedFile2 = FileEntry("doc2.pdf", 250, "sha2_updated", 2000L, 22L, "tg22")
        val newFile4 = FileEntry("doc4.pdf", 400, "sha4", 2000L, 4L, "tg4")
        val selectiveUploads = listOf(updatedFile2, newFile4)

        // In-place merge logic
        val mergedFiles = existingManifest.files.toMutableList()
        for (newEntry in selectiveUploads) {
            mergedFiles.removeAll { it.relativePath == newEntry.relativePath }
            mergedFiles.add(newEntry)
        }

        val updatedManifest = existingManifest.copy(
            timestamp = 2000L,
            totalFiles = mergedFiles.size,
            totalBytes = mergedFiles.sumOf { it.size },
            files = mergedFiles
        )

        // Verifications
        assertEquals("snap_20261025_001", updatedManifest.snapshotId) // Snapshot ID strictly retained!
        assertEquals("Base Vault", updatedManifest.tag)
        assertEquals(4, updatedManifest.totalFiles)
        assertEquals(1050L, updatedManifest.totalBytes) // 100 + 250 + 300 + 400

        val doc2 = updatedManifest.files.first { it.relativePath == "doc2.pdf" }
        assertEquals("sha2_updated", doc2.sha256)
        assertEquals(250L, doc2.size)

        assertTrue(updatedManifest.files.any { it.relativePath == "doc1.pdf" })
        assertTrue(updatedManifest.files.any { it.relativePath == "doc3.pdf" })
        assertTrue(updatedManifest.files.any { it.relativePath == "doc4.pdf" })
    }
}
