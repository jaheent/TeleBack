package com.teleback.data.local

import androidx.room.Entity

/**
 * Resume journal: one row per part that has already been uploaded to Telegram during a sync
 * that has not yet been committed with a manifest. If a sync is interrupted, the next run
 * reuses these parts instead of uploading them again.
 *
 * Rows are keyed by the whole-file SHA-256 (so an edited file never reuses stale parts) and
 * carry the encryption mode + key fingerprint (so parts encrypted with a previous Master Key
 * are never reused).
 */
@Entity(tableName = "pending_uploads", primaryKeys = ["fileSha256", "chunkIndex"])
data class PendingUploadEntity(
    val fileSha256: String,
    val chunkIndex: Int,
    val relativePath: String,
    val chunkCount: Int,
    val partSize: Long,
    val partSha256: String,
    val messageId: Long,
    val fileId: String,
    val encrypted: Boolean,
    val keyFingerprint: String,
    val createdAt: Long
)

object UploadJournal {
    /** Interrupted uploads that were never resumed are cleaned from the cloud after this long. */
    const val EXPIRY_MILLIS = 7L * 24 * 60 * 60 * 1000

    /** A journaled part may only be reused if it was produced with the same settings. */
    fun isReusable(
        entry: PendingUploadEntity,
        encrypted: Boolean,
        keyFingerprint: String,
        expectedChunkCount: Int
    ): Boolean = entry.encrypted == encrypted &&
        entry.keyFingerprint == keyFingerprint &&
        entry.chunkCount == expectedChunkCount

    fun isExpired(entry: PendingUploadEntity, now: Long): Boolean =
        now - entry.createdAt > EXPIRY_MILLIS
}
