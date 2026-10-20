package com.teleback.domain

import com.teleback.data.model.SnapshotManifest

data class SnapshotUsage(
    val snapshotId: String,
    val totalFiles: Int,
    val totalBytes: Long
)

data class CloudUsage(
    val snapshotCount: Int,
    val snapshotLimit: Int,
    /** Distinct files physically stored in Telegram (files shared by several snapshots count once). */
    val uniqueFiles: Int,
    /** Bytes actually stored in Telegram for those files (encrypted size, shared files counted once). */
    val storedBytes: Long,
    /** Plain size of the same unique files, used to derive the encryption overhead. */
    val uniqueLogicalBytes: Long,
    val isEncrypted: Boolean,
    val lastSyncMillis: Long?,
    val perSnapshot: List<SnapshotUsage>
) {
    /** Extra bytes stored because of encryption headers, as a percentage of the plain size. */
    val overheadPercent: Double
        get() = if (uniqueLogicalBytes > 0) {
            ((storedBytes - uniqueLogicalBytes).coerceAtLeast(0L).toDouble() / uniqueLogicalBytes) * 100.0
        } else 0.0
}

/**
 * Computes how much of Telegram a backup set occupies. Pure function over the manifests, so it
 * works offline and is covered by unit tests.
 *
 * Incremental syncs share unchanged files between snapshots, so every Telegram message is
 * counted once no matter how many snapshots reference it.
 */
object UsageCalculator {
    /** magic (4) + IV (12) + GCM tag (16) added to every encrypted payload */
    const val ENCRYPTION_OVERHEAD_PER_PART = 32L

    fun compute(manifests: List<SnapshotManifest>, snapshotLimit: Int): CloudUsage {
        val storedByMessage = HashMap<Long, Long>()
        val logicalByFile = HashMap<Long, Long>()

        for (manifest in manifests) {
            for (file in manifest.files) {
                logicalByFile.putIfAbsent(file.messageId, file.size)
                if (file.chunkParts.isNotEmpty()) {
                    for (part in file.chunkParts) {
                        storedByMessage.putIfAbsent(part.messageId, part.size)
                    }
                } else {
                    val overhead = if (manifest.isEncrypted) ENCRYPTION_OVERHEAD_PER_PART else 0L
                    storedByMessage.putIfAbsent(file.messageId, file.size + overhead)
                }
            }
        }

        return CloudUsage(
            snapshotCount = manifests.size,
            snapshotLimit = snapshotLimit,
            uniqueFiles = logicalByFile.size,
            storedBytes = storedByMessage.values.sum(),
            uniqueLogicalBytes = logicalByFile.values.sum(),
            isEncrypted = manifests.firstOrNull()?.isEncrypted ?: true,
            lastSyncMillis = manifests.maxOfOrNull { it.timestamp },
            perSnapshot = manifests.map { SnapshotUsage(it.snapshotId, it.totalFiles, it.totalBytes) }
        )
    }
}
