package com.teleback.domain

import android.content.Context
import android.net.Uri
import android.os.Build
import com.teleback.core.cli.LogLevel
import com.teleback.core.cli.TerminalLog
import com.teleback.core.crypto.ChecksumUtils
import com.teleback.core.crypto.CryptoEngine
import com.teleback.core.network.TelegramApiClient
import com.teleback.core.storage.FileChunker
import com.teleback.core.storage.ManifestManager
import com.teleback.core.storage.SafStorageHelper
import com.teleback.data.local.AppDatabase
import com.teleback.data.local.AppPreferences
import com.teleback.data.local.SnapshotEntity
import com.teleback.data.model.ChunkPart
import com.teleback.data.model.DiffStatus
import com.teleback.data.model.DiffSummary
import com.teleback.data.model.FileDiffItem
import com.teleback.data.model.FileEntry
import com.teleback.data.model.LocalFileItem
import com.teleback.data.model.SnapshotManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncManager(private val context: Context) {
    companion object {
        const val MAX_SNAPSHOT_LIMIT = 10
    }

    private val prefs = AppPreferences(context)
    private val db = AppDatabase.getDatabase(context)
    private val safHelper = SafStorageHelper(context)

    suspend fun getApiClient(): TelegramApiClient? {
        val token = prefs.botToken.first() ?: return null
        return TelegramApiClient(token)
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val client = getApiClient() ?: return@withContext Result.failure(Exception("Bot token not configured. Run [7] init."))
        val startTime = System.currentTimeMillis()
        val me = client.getMe().getOrThrow()
        val latency = System.currentTimeMillis() - startTime
        Result.success("Connected to @${me.username ?: me.firstName} (Latency: ${latency}ms)")
    }

    suspend fun detectChatId(): Result<Pair<Long, String>> = withContext(Dispatchers.IO) {
        val client = getApiClient() ?: return@withContext Result.failure(Exception("Bot token not configured."))
        val updates = client.getUpdates().getOrThrow()
        val lastUpdate = updates.lastOrNull { it.message?.chat != null || it.channelPost?.chat != null }
            ?: return@withContext Result.failure(Exception("No recent message found. Please send a message or add bot to your group and try again."))

        val chat = lastUpdate.message?.chat ?: lastUpdate.channelPost!!.chat
        val title = chat.title ?: chat.username ?: "Group_${chat.id}"
        prefs.saveChat(chat.id, title)
        Result.success(Pair(chat.id, title))
    }

    suspend fun calculateDiff(onLog: (TerminalLog) -> Unit = {}): DiffSummary = withContext(Dispatchers.IO) {
        val folderUriStr = prefs.folderUri.first() ?: throw IllegalStateException("Target folder not set. Run [7] init.")
        val folderUri = Uri.parse(folderUriStr)

        onLog(TerminalLog(level = LogLevel.INFO, message = "Scanning local folder structure..."))
        val scanResult = safHelper.scanDirectory(folderUri)
        val localFiles = scanResult.files
        onLog(TerminalLog(level = LogLevel.PLAIN, message = "Found ${scanResult.directories.size} folders, ${localFiles.size} files. Computing SHA-256 hashes..."))

        // Compute hashes
        for (file in localFiles) {
            file.sha256 = safHelper.calculateSha256(file.uri)
        }

        // Fetch latest snapshot
        val latestSnapshot = db.snapshotDao().getLatestSnapshot()
        val remoteFiles = if (latestSnapshot != null) {
            try {
                val manifest = ManifestManager.json.decodeFromString<SnapshotManifest>(latestSnapshot.rawJson)
                manifest.files.associateBy { it.relativePath }
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }

        val diffItems = mutableListOf<FileDiffItem>()
        var bytesToUpload = 0L

        // Check local files against remote
        for (local in localFiles) {
            val remote = remoteFiles[local.relativePath]
            when {
                remote == null -> {
                    diffItems.add(FileDiffItem(local.relativePath, DiffStatus.ADDED, localFile = local))
                    bytesToUpload += local.size
                }
                remote.sha256 != local.sha256 -> {
                    diffItems.add(FileDiffItem(local.relativePath, DiffStatus.MODIFIED, localFile = local, remoteEntry = remote))
                    bytesToUpload += local.size
                }
                else -> {
                    diffItems.add(FileDiffItem(local.relativePath, DiffStatus.UNCHANGED, localFile = local, remoteEntry = remote))
                }
            }
        }

        // Check for deleted files
        val localPathSet = localFiles.map { it.relativePath }.toSet()
        for ((remotePath, remoteEntry) in remoteFiles) {
            if (!localPathSet.contains(remotePath)) {
                diffItems.add(FileDiffItem(remotePath, DiffStatus.DELETED, remoteEntry = remoteEntry))
            }
        }

        DiffSummary(
            totalLocalFiles = localFiles.size,
            totalDirectories = scanResult.directories.size,
            addedCount = diffItems.count { it.status == DiffStatus.ADDED },
            modifiedCount = diffItems.count { it.status == DiffStatus.MODIFIED },
            unchangedCount = diffItems.count { it.status == DiffStatus.UNCHANGED },
            deletedCount = diffItems.count { it.status == DiffStatus.DELETED },
            bytesToUpload = bytesToUpload,
            items = diffItems
        )
    }

    suspend fun runBackupOrSync(
        isForceFullBackup: Boolean,
        tag: String? = null,
        dryRun: Boolean = false,
        onLog: (TerminalLog) -> Unit
    ): Result<SnapshotManifest> = withContext(Dispatchers.IO) {
        runCatching {
            val client = getApiClient() ?: throw IllegalStateException("Bot token missing. Run [7] init.")
            val chatId = prefs.chatId.first() ?: throw IllegalStateException("Telegram Group ID missing. Run [7] init.")
            val folderUriStr = prefs.folderUri.first() ?: throw IllegalStateException("Target folder missing. Run [7] init.")
            val folderName = prefs.folderName.first() ?: "BackupFolder"
            val folderUri = Uri.parse(folderUriStr)
            val isEncrypted = prefs.isEncryptionEnabled.first()
            val masterKey = prefs.getOrGenerateMasterKey()

            onLog(TerminalLog(level = LogLevel.INFO, message = if (isForceFullBackup) "Starting FULL BACKUP..." else "Starting DIFFERENTIAL SYNC..."))
            onLog(TerminalLog(level = LogLevel.PLAIN, message = "Target: $folderName | Encrypted: $isEncrypted"))

            val diff = calculateDiff(onLog)
            onLog(TerminalLog(
                level = LogLevel.INFO,
                message = "Diff: ${diff.addedCount} added, ${diff.modifiedCount} modified, ${diff.unchangedCount} unchanged, ${diff.deletedCount} deleted (${formatSize(diff.bytesToUpload)} to upload)"
            ))

            if (dryRun) {
                onLog(TerminalLog(level = LogLevel.WARN, message = "[DRY-RUN] No files were uploaded."))
                return@runCatching SnapshotManifest(
                    snapshotId = "dry_run",
                    timestamp = System.currentTimeMillis(),
                    deviceName = Build.MODEL,
                    rootFolderName = folderName,
                    totalFiles = diff.totalLocalFiles,
                    totalBytes = diff.bytesToUpload,
                    files = emptyList()
                )
            }

            val filesToUpload = diff.items.filter {
                isForceFullBackup || it.status == DiffStatus.ADDED || it.status == DiffStatus.MODIFIED
            }

            val uploadedFileEntries = mutableListOf<FileEntry>()
            var filesProcessed = 0

            for (item in filesToUpload) {
                val localFile = item.localFile ?: continue
                filesProcessed++
                onLog(TerminalLog(
                    level = LogLevel.SYNC,
                    message = "[$filesProcessed/${filesToUpload.size}] Uploading ${localFile.relativePath} (${formatSize(localFile.size)})..."
                ))

                val rawBytes = safHelper.openInputStream(localFile.uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Could not read local file ${localFile.relativePath}")

                // Encrypt if enabled
                val processedBytes = if (isEncrypted) {
                    CryptoEngine.encryptBytes(rawBytes, masterKey)
                } else {
                    rawBytes
                }

                // Check chunking
                if (FileChunker.shouldChunk(processedBytes.size.toLong())) {
                    val chunks = FileChunker.splitIntoChunks(processedBytes)
                    onLog(TerminalLog(level = LogLevel.PLAIN, message = "File >45MB: Split into ${chunks.size} chunks"))
                    val chunkParts = mutableListOf<ChunkPart>()
                    for ((idx, chunkBytes) in chunks.withIndex()) {
                        val partName = "${localFile.name}.part${idx + 1}"
                        val msg = client.sendDocument(
                            chatId = chatId,
                            fileName = partName,
                            fileBytes = chunkBytes,
                            caption = "Part ${idx + 1}/${chunks.size} of ${localFile.relativePath}"
                        ).getOrThrow()

                        val doc = msg.document ?: throw IllegalStateException("No document received in Telegram response")
                        chunkParts.add(
                            ChunkPart(
                                partIndex = idx + 1,
                                size = chunkBytes.size.toLong(),
                                sha256 = ChecksumUtils.sha256(chunkBytes),
                                messageId = msg.messageId,
                                fileId = doc.fileId
                            )
                        )
                    }
                    uploadedFileEntries.add(
                        FileEntry(
                            relativePath = localFile.relativePath,
                            size = localFile.size,
                            sha256 = localFile.sha256 ?: ChecksumUtils.sha256(rawBytes),
                            modifiedAt = localFile.lastModified,
                            messageId = chunkParts.first().messageId,
                            fileId = chunkParts.first().fileId,
                            chunkCount = chunks.size,
                            chunkParts = chunkParts
                        )
                    )
                } else {
                    val uploadFileName = if (isEncrypted) "${localFile.name}.tbk" else localFile.name
                    val msg = client.sendDocument(
                        chatId = chatId,
                        fileName = uploadFileName,
                        fileBytes = processedBytes,
                        caption = "TeleBack File: ${localFile.relativePath}"
                    ).getOrThrow()

                    val doc = msg.document ?: throw IllegalStateException("No document received in Telegram response")
                    uploadedFileEntries.add(
                        FileEntry(
                            relativePath = localFile.relativePath,
                            size = localFile.size,
                            sha256 = localFile.sha256 ?: ChecksumUtils.sha256(rawBytes),
                            modifiedAt = localFile.lastModified,
                            messageId = msg.messageId,
                            fileId = doc.fileId,
                            chunkCount = 1
                        )
                    )
                }
            }

            // Carry over unchanged files if this was incremental sync
            if (!isForceFullBackup) {
                for (item in diff.items) {
                    if (item.status == DiffStatus.UNCHANGED && item.remoteEntry != null) {
                        uploadedFileEntries.add(item.remoteEntry)
                    }
                }
            }

            // Create Snapshot Manifest
            val timestamp = System.currentTimeMillis()
            val timeTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(timestamp))
            val snapshotId = "snap_$timeTag"

            val scanResult = safHelper.scanDirectory(folderUri)

            val manifest = SnapshotManifest(
                version = 1,
                snapshotId = snapshotId,
                tag = tag,
                timestamp = timestamp,
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                rootFolderName = folderName,
                totalFiles = uploadedFileEntries.size,
                totalBytes = uploadedFileEntries.sumOf { it.size },
                isEncrypted = isEncrypted,
                directories = scanResult.directories,
                files = uploadedFileEntries
            )

            onLog(TerminalLog(level = LogLevel.SNAP, message = "Creating snapshot manifest [$snapshotId]..."))
            val manifestBytes = ManifestManager.serializeManifest(manifest, if (isEncrypted) masterKey else null)

            val manifestMessage = client.sendDocument(
                chatId = chatId,
                fileName = "manifest_$snapshotId.json${if (isEncrypted) ".tbk" else ""}",
                fileBytes = manifestBytes,
                caption = "${ManifestManager.MANIFEST_TAG} $snapshotId [${tag ?: "Auto"}] - ${uploadedFileEntries.size} files"
            ).getOrThrow()

            // Pin manifest for instant retrieval
            client.pinChatMessage(chatId, manifestMessage.messageId)

            // Cache in Room DB
            db.snapshotDao().insertSnapshot(
                SnapshotEntity(
                    snapshotId = snapshotId,
                    tag = tag,
                    timestamp = timestamp,
                    rootFolderName = folderName,
                    totalFiles = manifest.totalFiles,
                    totalBytes = manifest.totalBytes,
                    isEncrypted = isEncrypted,
                    manifestMessageId = manifestMessage.messageId,
                    manifestFileId = manifestMessage.document?.fileId ?: "",
                    rawJson = ManifestManager.json.encodeToString(manifest)
                )
            )

            onLog(TerminalLog(level = LogLevel.OK, message = "Snapshot [$snapshotId] committed & pinned successfully!"))

            // Enforce 10-snapshot limit with smart orphan pruning
            pruneOldSnapshots(client, chatId, onLog)

            manifest
        }
    }

    private suspend fun pruneOldSnapshots(
        client: TelegramApiClient,
        chatId: Long,
        onLog: (TerminalLog) -> Unit
    ) {
        val allSnapshots = db.snapshotDao().getAllSnapshotsDirect()
        if (allSnapshots.size <= MAX_SNAPSHOT_LIMIT) return

        val activeSnapshots = allSnapshots.take(MAX_SNAPSHOT_LIMIT)
        val snapshotsToPrune = allSnapshots.drop(MAX_SNAPSHOT_LIMIT)

        // Collect messageIds that are still in use by the active 10 snapshots
        val activeMessageIds = mutableSetOf<Long>()
        for (snapEntity in activeSnapshots) {
            activeMessageIds.add(snapEntity.manifestMessageId)
            val snapManifest = runCatching {
                ManifestManager.json.decodeFromString<SnapshotManifest>(snapEntity.rawJson)
            }.getOrNull()

            if (snapManifest != null) {
                for (file in snapManifest.files) {
                    activeMessageIds.add(file.messageId)
                    for (chunk in file.chunkParts) {
                        activeMessageIds.add(chunk.messageId)
                    }
                }
            }
        }

        for (oldSnap in snapshotsToPrune) {
            onLog(TerminalLog(level = LogLevel.WARN, message = "Pruning oldest cloud snapshot [${oldSnap.snapshotId}] (limit: $MAX_SNAPSHOT_LIMIT)..."))

            // 1. Unpin & Delete manifest message in Telegram
            client.unpinChatMessage(chatId, oldSnap.manifestMessageId)
            client.deleteMessage(chatId, oldSnap.manifestMessageId)

            // 2. Delete orphaned file messages
            val oldManifest = runCatching {
                ManifestManager.json.decodeFromString<SnapshotManifest>(oldSnap.rawJson)
            }.getOrNull()

            var deletedOrphanCount = 0
            if (oldManifest != null) {
                for (file in oldManifest.files) {
                    if (!activeMessageIds.contains(file.messageId)) {
                        client.deleteMessage(chatId, file.messageId)
                        deletedOrphanCount++
                    }
                    for (chunk in file.chunkParts) {
                        if (!activeMessageIds.contains(chunk.messageId)) {
                            client.deleteMessage(chatId, chunk.messageId)
                            deletedOrphanCount++
                        }
                    }
                }
            }

            // 3. Delete from local database
            db.snapshotDao().deleteSnapshot(oldSnap.snapshotId)
            onLog(TerminalLog(level = LogLevel.OK, message = "Pruned [${oldSnap.snapshotId}]: manifest & $deletedOrphanCount orphaned cloud files removed."))
        }
    }

    suspend fun restoreSnapshot(
        snapshotId: String? = null,
        onLog: (TerminalLog) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val client = getApiClient() ?: throw IllegalStateException("Bot token missing.")
            val folderUriStr = prefs.folderUri.first() ?: throw IllegalStateException("Target folder missing.")
            val folderUri = Uri.parse(folderUriStr)
            val masterKey = prefs.getOrGenerateMasterKey()

            val snapshotEntity = if (snapshotId != null) {
                db.snapshotDao().getSnapshotById(snapshotId)
            } else {
                db.snapshotDao().getLatestSnapshot()
            } ?: throw IllegalStateException("No snapshot found to restore.")

            val manifest = ManifestManager.json.decodeFromString<SnapshotManifest>(snapshotEntity.rawJson)
            onLog(TerminalLog(level = LogLevel.INFO, message = "Restoring snapshot [${manifest.snapshotId}] (${manifest.directories.size} folders, ${manifest.files.size} files)..."))

            // 1. Recreate directory and subfolder hierarchy first
            if (manifest.directories.isNotEmpty()) {
                onLog(TerminalLog(level = LogLevel.PLAIN, message = "Reconstructing ${manifest.directories.size} subdirectories..."))
                safHelper.createDirectoryStructure(folderUri, manifest.directories)
            }

            var restoredCount = 0
            for ((idx, fileEntry) in manifest.files.withIndex()) {
                onLog(TerminalLog(
                    level = LogLevel.SYNC,
                    message = "[${idx + 1}/${manifest.files.size}] Downloading ${fileEntry.relativePath}..."
                ))

                val rawPayload: ByteArray = if (fileEntry.chunkCount > 1 && fileEntry.chunkParts.isNotEmpty()) {
                    val parts = mutableListOf<ByteArray>()
                    for (part in fileEntry.chunkParts) {
                        val fileInfo = client.getFile(part.fileId).getOrThrow()
                        val filePath = fileInfo.filePath ?: throw IllegalStateException("Telegram file_path empty")
                        val partBytes = client.downloadFileBytes(filePath).getOrThrow()
                        parts.add(partBytes)
                    }
                    FileChunker.reassemble(parts)
                } else {
                    val fileInfo = client.getFile(fileEntry.fileId).getOrThrow()
                    val filePath = fileInfo.filePath ?: throw IllegalStateException("Telegram file_path empty")
                    client.downloadFileBytes(filePath).getOrThrow()
                }

                // Decrypt if manifest was encrypted
                val finalBytes = if (manifest.isEncrypted) {
                    CryptoEngine.decryptBytes(rawPayload, masterKey)
                } else {
                    rawPayload
                }

                // Verify integrity
                val actualSha256 = ChecksumUtils.sha256(finalBytes)
                if (actualSha256 != fileEntry.sha256) {
                    onLog(TerminalLog(level = LogLevel.WARN, message = "Checksum mismatch on ${fileEntry.relativePath}!"))
                }

                safHelper.writeRestoredFile(folderUri, fileEntry.relativePath, finalBytes)
                restoredCount++
            }

            onLog(TerminalLog(level = LogLevel.OK, message = "Restore complete: $restoredCount files successfully written!"))
            restoredCount
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return "%.1f %sB".format(bytes / Math.pow(1024.0, exp.toDouble()), pre)
    }
}
