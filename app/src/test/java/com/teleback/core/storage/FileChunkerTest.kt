package com.teleback.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileChunkerTest {

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
