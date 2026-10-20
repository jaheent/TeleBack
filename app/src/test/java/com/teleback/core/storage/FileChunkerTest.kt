package com.teleback.core.storage

import com.teleback.core.crypto.CryptoEngine
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class FileChunkerTest {

    @Test
    fun chunkSizeStaysUnderTelegramDownloadLimit() {
        // Bot API getFile serves at most 20 MB, so parts (+ ~32 bytes encryption header) must fit.
        assertTrue(FileChunker.DEFAULT_CHUNK_SIZE + 64 < 20 * 1024 * 1024)
    }

    @Test
    fun chunkCountMatchesSize() {
        val cs = 1000
        assertEquals(1, FileChunker.chunkCountFor(0, cs))
        assertEquals(1, FileChunker.chunkCountFor(1, cs))
        assertEquals(1, FileChunker.chunkCountFor(1000, cs))
        assertEquals(2, FileChunker.chunkCountFor(1001, cs))
        assertEquals(3, FileChunker.chunkCountFor(2500, cs))
    }

    @Test
    fun readWindowStreamsFullWindowsThenRemainder() {
        val data = ByteArray(2500) { (it % 251).toByte() }
        val stream = ByteArrayInputStream(data)
        val w1 = FileChunker.readWindow(stream, 1000)
        val w2 = FileChunker.readWindow(stream, 1000)
        val w3 = FileChunker.readWindow(stream, 1000)
        val w4 = FileChunker.readWindow(stream, 1000)
        assertEquals(1000, w1.size)
        assertEquals(1000, w2.size)
        assertEquals(500, w3.size)
        assertEquals(0, w4.size)
        assertArrayEquals(data, w1 + w2 + w3)
    }

    @Test
    fun perPartEncryptionRoundTripsAndPartsAreIndependent() {
        val key = CryptoEngine.generateMasterKeyBase64()
        val data = ByteArray(5000) { (it * 7).toByte() }
        val stream = ByteArrayInputStream(data)
        val encryptedParts = mutableListOf<ByteArray>()
        while (true) {
            val window = FileChunker.readWindow(stream, 2000)
            if (window.isEmpty()) break
            encryptedParts.add(CryptoEngine.encryptBytes(window, key))
            if (window.size < 2000) break
        }
        assertEquals(3, encryptedParts.size)
        // Each part decrypts on its own, in any order (this is what makes parts retryable/resumable)
        val restored = encryptedParts.map { CryptoEngine.decryptBytes(it, key) }
        assertArrayEquals(data, restored.reduce { a, b -> a + b })
        assertArrayEquals(restored[2], CryptoEngine.decryptBytes(encryptedParts[2], key))
    }

    @Test
    fun testChunkingAndReassembly() {
        // Create 100KB dummy data
        val data = ByteArray(100 * 1024) { (it % 256).toByte() }
        // Split with small chunk size for testing: 16KB
        val chunkSize = 16 * 1024
        val chunks = FileChunker.splitIntoChunks(data, chunkSize)

        assertEquals(7, chunks.size) // 6 * 16KB + 1 * 4KB
        val reassembled = FileChunker.reassemble(chunks)

        assertEquals(data.size, reassembled.size)
        for (i in data.indices) {
            assertEquals(data[i], reassembled[i])
        }
    }
}
