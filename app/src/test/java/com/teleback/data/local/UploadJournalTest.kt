package com.teleback.data.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadJournalTest {

    private fun entry(
        encrypted: Boolean = true,
        key: String = "abc",
        chunks: Int = 4,
        createdAt: Long = 1_000L
    ) = PendingUploadEntity(
        fileSha256 = "sha", chunkIndex = 1, relativePath = "a.bin", chunkCount = chunks,
        partSize = 10, partSha256 = "p", messageId = 5, fileId = "f",
        encrypted = encrypted, keyFingerprint = key, createdAt = createdAt
    )

    @Test
    fun reusableWhenSettingsMatch() {
        assertTrue(UploadJournal.isReusable(entry(), encrypted = true, keyFingerprint = "abc", expectedChunkCount = 4))
    }

    @Test
    fun notReusableAfterMasterKeyChange() {
        // Parts encrypted with an old key would be undecryptable: they must never be reused.
        assertFalse(UploadJournal.isReusable(entry(), encrypted = true, keyFingerprint = "other", expectedChunkCount = 4))
    }

    @Test
    fun notReusableWhenEncryptionModeOrChunkCountDiffers() {
        assertFalse(UploadJournal.isReusable(entry(), encrypted = false, keyFingerprint = "abc", expectedChunkCount = 4))
        assertFalse(UploadJournal.isReusable(entry(), encrypted = true, keyFingerprint = "abc", expectedChunkCount = 5))
    }

    @Test
    fun expiresAfterSevenDays() {
        val e = entry(createdAt = 0L)
        assertFalse(UploadJournal.isExpired(e, UploadJournal.EXPIRY_MILLIS))
        assertTrue(UploadJournal.isExpired(e, UploadJournal.EXPIRY_MILLIS + 1))
    }
}
