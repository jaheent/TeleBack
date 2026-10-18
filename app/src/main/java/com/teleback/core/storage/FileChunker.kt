package com.teleback.core.storage

object FileChunker {
    const val DEFAULT_CHUNK_SIZE = 45 * 1024 * 1024 // 45 MB

    fun shouldChunk(size: Long): Boolean = size > DEFAULT_CHUNK_SIZE

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
