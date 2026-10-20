package com.teleback.core.storage

import java.io.InputStream

object FileChunker {
    /**
     * 19 MB keeps every part safely under Telegram's Bot API limits: 50 MB for uploads and,
     * more importantly, 20 MB for downloads through `getFile` (restore). Each part also gets
     * a small encryption header (~32 bytes), so this leaves comfortable headroom below 20 MB.
     */
    const val DEFAULT_CHUNK_SIZE = 19 * 1024 * 1024 // 19 MB

    /** Files larger than this get an explicit heads-up in the terminal before uploading. */
    const val LARGE_FILE_THRESHOLD = 100L * 1024 * 1024 // 100 MB

    fun shouldChunk(size: Long): Boolean = size > DEFAULT_CHUNK_SIZE

    /** Number of parts a file of [size] bytes is split into (an empty file still uses one part). */
    fun chunkCountFor(size: Long, chunkSize: Int = DEFAULT_CHUNK_SIZE): Int {
        if (size <= 0L) return 1
        return ((size + chunkSize - 1) / chunkSize).toInt()
    }

    /**
     * Reads up to [size] bytes from [input], blocking until the window is full or the stream
     * ends. Returns fewer than [size] bytes only at end-of-stream (empty array when already at EOF).
     * This is what lets us stream multi-GB files with a bounded memory footprint.
     */
    fun readWindow(input: InputStream, size: Int = DEFAULT_CHUNK_SIZE): ByteArray {
        val buffer = ByteArray(size)
        var filled = 0
        while (filled < size) {
            val read = input.read(buffer, filled, size - filled)
            if (read == -1) break
            filled += read
        }
        return if (filled == size) buffer else buffer.copyOf(filled)
    }

    fun splitIntoChunks(data: ByteArray, chunkSize: Int = DEFAULT_CHUNK_SIZE): List<ByteArray> {
        if (data.size <= chunkSize) {
            return listOf(data)
        }
        val chunks = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < data.size) {
            val length = (data.size - offset).coerceAtMost(chunkSize)
            val chunk = ByteArray(length)
            System.arraycopy(data, offset, chunk, 0, length)
            chunks.add(chunk)
            offset += length
        }
        return chunks
    }

    fun reassemble(parts: List<ByteArray>): ByteArray {
        val totalSize = parts.sumOf { it.size }
        val result = ByteArray(totalSize)
        var offset = 0
        for (part in parts) {
            System.arraycopy(part, 0, result, offset, part.size)
            offset += part.size
        }
        return result
    }
}
