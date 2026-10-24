package com.teleback.domain

import android.content.Context
import android.net.Uri
import android.os.Build
import com.teleback.core.cli.LogLevel
import com.teleback.core.cli.TerminalLog
import com.teleback.core.crypto.ChecksumUtils
import com.teleback.core.crypto.CryptoEngine
import com.teleback.core.network.ConnectivityObserver
import com.teleback.core.network.RetryPolicy
import com.teleback.core.network.TelegramApiClient
import com.teleback.core.network.TelegramApiException
import com.teleback.core.network.TelegramMessage
import com.teleback.core.storage.FileChunker
import com.teleback.core.storage.ManifestManager
import com.teleback.core.storage.SafStorageHelper
import com.teleback.core.util.SizeFormat
import com.teleback.data.local.AppDatabase
import com.teleback.data.local.AppPreferences
import com.teleback.data.local.PendingUploadEntity
import com.teleback.data.local.SnapshotEntity
import com.teleback.data.local.UploadJournal
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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.AEADBadTagException
import kotlin.coroutines.cancellation.CancellationException

class SyncManager(private val context: Context) {
    companion object {
        const val MAX_SNAPSHOT_LIMIT = 10
        private const val OFFLINE_WAIT_TIMEOUT_MS = 30 * 60 * 1000L
        private const val MAX_SKIPPED_NAMES_LOGGED = 5
    }

    private val prefs = AppPreferences(context)
    private val db = AppDatabase.getDatabase(context)
    private val safHelper = SafStorageHelper(context)
    private val connectivity = ConnectivityObserver(context)

    /** Raised when the very first decryption of a restore fails: almost certainly the wrong Master Key. */
    private class WrongKeyException : IllegalStateException(
        "Decryption failed - the Master Key does not match the one used for this backup. " +
            "Open [8] keys and enter the original key or passphrase, then try again."
    )

    private class RestoreState {
        var decryptedOk = false
    }

    private enum class RestoreOutcome { RESTORED, UP_TO_DATE }

    suspend fun getApiClient(): TelegramApiClient? {
        val token = prefs.botToken.first() ?: return null
        return TelegramApiClient(token)
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val client = getApiClient() ?: return@withContext Result.failure(Exception("Bot token not configured. Run [0] setup."))
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
        val folderUriStr = prefs.folderUri.first() ?: throw IllegalStateException("Target folder not set. Run [0] setup.")
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

    /** How much of Telegram the stored snapshots occupy. Computed locally, works offline. */
    suspend fun cloudUsage(): CloudUsage = withContext(Dispatchers.IO) {
        val manifests = db.snapshotDao().getAllSnapshotsDirect().mapNotNull { entity ->
            runCatching { ManifestManager.json.decodeFromString<SnapshotManifest>(entity.rawJson) }.getOrNull()
        }
        UsageCalculator.compute(manifests, MAX_SNAPSHOT_LIMIT)
    }

    /** Number of already-uploaded parts waiting to be reused by an interrupted sync. */
    suspend fun pendingUploadParts(): Int = withContext(Dispatchers.IO) {
        db.pendingUploadDao().count()
    }

    suspend fun runBackupOrSync(
        isForceFullBackup: Boolean,
        tag: String? = null,
        dryRun: Boolean = false,
        onLog: (TerminalLog) -> Unit
    ): Result<SnapshotManifest> = withContext(Dispatchers.IO) {
        runCatching {
            val client = getApiClient() ?: throw IllegalStateException("Bot token missing. Run [0] setup.")
            val chatId = prefs.chatId.first() ?: throw IllegalStateException("Telegram Group ID missing. Run [0] setup.")
            val folderUriStr = prefs.folderUri.first() ?: throw IllegalStateException("Target folder missing. Run [0] setup.")
            val folderName = prefs.folderName.first() ?: "BackupFolder"
            val folderUri = Uri.parse(folderUriStr)
            val isEncrypted = prefs.isEncryptionEnabled.first()
            val masterKey = prefs.getOrGenerateMasterKey()
            val journalKey = if (isEncrypted) keyFingerprint(masterKey) else ""

            val latestSnapshot = db.snapshotDao().getLatestSnapshot()
            var effectiveFullBackup = isForceFullBackup

            if (!effectiveFullBackup && latestSnapshot == null) {
                onLog(TerminalLog(level = LogLevel.WARN, message = "No previous cloud backup found. Establishing initial full backup..."))
                effectiveFullBackup = true
            }

            onLog(TerminalLog(level = LogLevel.INFO, message = if (effectiveFullBackup) "Starting FULL BACKUP..." else "Starting DIFFERENTIAL SYNC..."))
            onLog(TerminalLog(level = LogLevel.PLAIN, message = "Target: $folderName | Encrypted: $isEncrypted"))

            val diff = calculateDiff(onLog)
            onLog(TerminalLog(
                level = LogLevel.INFO,
                message = "Diff: ${diff.addedCount} added, ${diff.modifiedCount} modified, ${diff.unchangedCount} unchanged, ${diff.deletedCount} deleted (${formatSize(diff.bytesToUpload)} to upload)"
            ))

            // If differential sync has zero modifications/additions/deletions, skip redundant upload
            if (!effectiveFullBackup && diff.addedCount == 0 && diff.modifiedCount == 0 && diff.deletedCount == 0) {
                onLog(TerminalLog(level = LogLevel.OK, message = "Nothing to Sync, already up to date."))
                return@runCatching ManifestManager.json.decodeFromString<SnapshotManifest>(latestSnapshot!!.rawJson)
            }

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

            purgeExpiredJournal(client, chatId, onLog)
            val pending = db.pendingUploadDao().count()
            if (pending > 0) {
                onLog(TerminalLog(level = LogLevel.INFO, message = "Found $pending part(s) from an interrupted sync - they will be reused, not uploaded again."))
            }

            val filesToUpload = diff.items.filter {
                effectiveFullBackup || it.status == DiffStatus.ADDED || it.status == DiffStatus.MODIFIED
            }

            val uploadedFileEntries = mutableListOf<FileEntry>()
            val skippedFiles = mutableListOf<String>()
            var uploadedCount = 0
            var filesProcessed = 0

            for (item in filesToUpload) {
                val localFile = item.localFile ?: continue
                filesProcessed++
                onLog(TerminalLog(
                    level = LogLevel.SYNC,
                    message = "[$filesProcessed/${filesToUpload.size}] Uploading ${localFile.relativePath} (${formatSize(localFile.size)})..."
                ))

                try {
                    uploadedFileEntries.add(
                        uploadFile(client, chatId, localFile, isEncrypted, masterKey, journalKey, onLog)
                    )
                    uploadedCount++
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // One bad file must not block the whole backup: skip it, keep the previous
                    // version in the snapshot (if any), and retry it on the next sync.
                    skippedFiles.add(localFile.relativePath)
                    onLog(TerminalLog(level = LogLevel.ERR, message = "Skipped ${localFile.relativePath}: ${describe(e)}"))
                    item.remoteEntry?.let { uploadedFileEntries.add(it) }
                }
            }

            if (filesToUpload.isNotEmpty() && uploadedCount == 0) {
                throw IllegalStateException(
                    "All ${filesToUpload.size} upload(s) failed, so no snapshot was created. " +
                        "Check your connection and run sync again - finished parts are kept and will be resumed."
                )
            }

            // Carry over unchanged files if this was incremental sync
            if (!effectiveFullBackup) {
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

            val previousManifestMessageId = db.snapshotDao().getLatestSnapshot()?.manifestMessageId
                ?: client.getChat(chatId).getOrNull()?.pinnedMessage?.messageId

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
                previousManifestMessageId = previousManifestMessageId,
                files = uploadedFileEntries
            )

            // The manifest is uploaded last: until it lands, the previous snapshot stays intact.
            onLog(TerminalLog(level = LogLevel.SNAP, message = "Creating snapshot manifest [$snapshotId]..."))
            val manifestBytes = ManifestManager.serializeManifest(manifest, if (isEncrypted) masterKey else null)

            val manifestMessage = RetryPolicy.execute(onRetry = retryLogger(onLog, "Manifest upload")) {
                client.sendDocument(
                    chatId = chatId,
                    fileName = "manifest_$snapshotId.json${if (isEncrypted) ".tbk" else ""}",
                    fileBytes = manifestBytes,
                    caption = "${ManifestManager.MANIFEST_TAG} $snapshotId [${tag ?: "Auto"}] - ${uploadedFileEntries.size} files"
                ).getOrThrow()
            }

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

            // Everything in this manifest is now referenced: its resume-journal rows are done.
            val committedIds = uploadedFileEntries
                .flatMap { entry -> listOf(entry.messageId) + entry.chunkParts.map { it.messageId } }
                .toSet()
            db.pendingUploadDao().all()
                .filter { it.messageId in committedIds }
                .forEach { db.pendingUploadDao().delete(it.fileSha256, it.chunkIndex) }

            onLog(TerminalLog(level = LogLevel.OK, message = "Snapshot [$snapshotId] committed & pinned successfully!"))

            if (skippedFiles.isNotEmpty()) {
                val names = skippedFiles.take(MAX_SKIPPED_NAMES_LOGGED).joinToString(", ")
                val more = if (skippedFiles.size > MAX_SKIPPED_NAMES_LOGGED) " and ${skippedFiles.size - MAX_SKIPPED_NAMES_LOGGED} more" else ""
                onLog(TerminalLog(
                    level = LogLevel.WARN,
                    message = "${skippedFiles.size} file(s) skipped: $names$more. Run sync again to retry them."
                ))
            }

            // Enforce 10-snapshot limit with smart orphan pruning
            pruneOldSnapshots(client, chatId, onLog)

            manifest
        }
    }

    /**
     * Streams one file to Telegram in 19 MB windows. Only one window is ever held in memory,
     * each window is encrypted on its own, and every finished part is journaled so an
     * interrupted upload resumes from the first missing part.
     */
    private suspend fun uploadFile(
        client: TelegramApiClient,
        chatId: Long,
        localFile: LocalFileItem,
        isEncrypted: Boolean,
        masterKey: String,
        journalKey: String,
        onLog: (TerminalLog) -> Unit
    ): FileEntry {
        val preSha = localFile.sha256.orEmpty()
        val expectedChunks = FileChunker.chunkCountFor(localFile.size)
        val journalDao = db.pendingUploadDao()

        val resumable: Map<Int, PendingUploadEntity> = if (preSha.isNotBlank()) {
            journalDao.forFile(preSha)
                .filter { UploadJournal.isReusable(it, isEncrypted, journalKey, expectedChunks) }
                .associateBy { it.chunkIndex }
        } else {
            emptyMap()
        }

        if (localFile.size > FileChunker.LARGE_FILE_THRESHOLD) {
            onLog(TerminalLog(
                level = LogLevel.WARN,
                message = "Large file (${formatSize(localFile.size)}): $expectedChunks parts of ${FileChunker.DEFAULT_CHUNK_SIZE / (1024 * 1024)} MB. " +
                    "This can take a while - keep TeleBack open. If it is interrupted, finished parts are reused."
            ))
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val parts = mutableListOf<ChunkPart>()
        var totalRead = 0L
        var uploadedBytes = 0L
        val startedAt = System.currentTimeMillis()
        var etaLogged = false
        var index = 0

        val input = safHelper.openInputStream(localFile.uri)
            ?: throw IllegalStateException("Could not read local file ${localFile.relativePath}")

        input.use { stream ->
            while (true) {
                val window = FileChunker.readWindow(stream)
                if (window.isEmpty() && index > 0) break
                index++
                val isLast = window.size < FileChunker.DEFAULT_CHUNK_SIZE
                totalRead += window.size
                digest.update(window)

                val cached = resumable[index]
                if (cached != null) {
                    parts.add(
                        ChunkPart(
                            partIndex = index,
                            size = cached.partSize,
                            sha256 = cached.partSha256,
                            messageId = cached.messageId,
                            fileId = cached.fileId
                        )
                    )
                    onLog(TerminalLog(level = LogLevel.PLAIN, message = "  part $index/$expectedChunks already in Telegram - resumed"))
                } else {
                    val payload = if (isEncrypted) CryptoEngine.encryptBytes(window, masterKey) else window
                    val partName = if (expectedChunks > 1) {
                        "${localFile.name}.part$index"
                    } else {
                        if (isEncrypted) "${localFile.name}.tbk" else localFile.name
                    }
                    val caption = if (expectedChunks > 1) {
                        "Part $index/$expectedChunks of ${localFile.relativePath}"
                    } else {
                        "TeleBack File: ${localFile.relativePath}"
                    }

                    val msg = RetryPolicy.execute(
                        onRetry = retryLogger(onLog, "Upload of ${localFile.name} part $index/$expectedChunks")
                    ) {
                        client.sendDocument(chatId = chatId, fileName = partName, fileBytes = payload, caption = caption).getOrThrow()
                    }
                    val doc = msg.document ?: throw IllegalStateException("No document received in Telegram response")
                    val partSha = ChecksumUtils.sha256(payload)

                    parts.add(
                        ChunkPart(
                            partIndex = index,
                            size = payload.size.toLong(),
                            sha256 = partSha,
                            messageId = msg.messageId,
                            fileId = doc.fileId
                        )
                    )
                    if (preSha.isNotBlank()) {
                        journalDao.insert(
                            PendingUploadEntity(
                                fileSha256 = preSha,
                                chunkIndex = index,
                                relativePath = localFile.relativePath,
                                chunkCount = expectedChunks,
                                partSize = payload.size.toLong(),
                                partSha256 = partSha,
                                messageId = msg.messageId,
                                fileId = doc.fileId,
                                encrypted = isEncrypted,
                                keyFingerprint = journalKey,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }

                    uploadedBytes += window.size
                    if (expectedChunks > 1) {
                        onLog(TerminalLog(level = LogLevel.PLAIN, message = "  part $index/$expectedChunks uploaded"))
                        if (!etaLogged && index < expectedChunks) {
                            val elapsedSec = ((System.currentTimeMillis() - startedAt) / 1000.0).coerceAtLeast(0.5)
                            val bytesPerSec = uploadedBytes / elapsedSec
                            val remainingBytes = (localFile.size - totalRead).coerceAtLeast(0L)
                            if (bytesPerSec > 0) {
                                val etaMin = (remainingBytes / bytesPerSec / 60.0).toInt()
                                onLog(TerminalLog(
                                    level = LogLevel.PLAIN,
                                    message = "  speed ~${formatSize(bytesPerSec.toLong())}/s, about ${if (etaMin < 1) "<1" else etaMin.toString()} min left for this file"
                                ))
                            }
                            etaLogged = true
                        }
                    }
                }

                if (isLast) break
            }
        }

        val actualSha = digest.digest().joinToString("") { "%02x".format(it) }
        if (preSha.isNotBlank() && actualSha != preSha) {
            onLog(TerminalLog(
                level = LogLevel.WARN,
                message = "${localFile.relativePath} changed while it was being backed up; it will be re-synced next time."
            ))
        }

        val first = parts.first()
        return FileEntry(
            relativePath = localFile.relativePath,
            size = totalRead,
            sha256 = actualSha,
            modifiedAt = localFile.lastModified,
            messageId = first.messageId,
            fileId = first.fileId,
            chunkCount = parts.size,
            chunkParts = if (parts.size > 1) parts else emptyList(),
            chunkEncrypted = true
        )
    }

    /** Removes parts of interrupted syncs that were never resumed within 7 days. */
    private suspend fun purgeExpiredJournal(
        client: TelegramApiClient,
        chatId: Long,
        onLog: (TerminalLog) -> Unit
    ) {
        val dao = db.pendingUploadDao()
        val now = System.currentTimeMillis()
        val expired = dao.all().filter { UploadJournal.isExpired(it, now) }
        if (expired.isEmpty()) return

        val referenced = mutableSetOf<Long>()
        for (snap in db.snapshotDao().getAllSnapshotsDirect()) {
            referenced.add(snap.manifestMessageId)
            val manifest = runCatching {
                ManifestManager.json.decodeFromString<SnapshotManifest>(snap.rawJson)
            }.getOrNull() ?: continue
            for (file in manifest.files) {
                referenced.add(file.messageId)
                file.chunkParts.forEach { referenced.add(it.messageId) }
            }
        }

        for (entry in expired) {
            if (entry.messageId !in referenced) {
                client.deleteMessage(chatId, entry.messageId)
            }
            dao.delete(entry.fileSha256, entry.chunkIndex)
        }
        onLog(TerminalLog(level = LogLevel.INFO, message = "Cleaned ${expired.size} abandoned upload part(s) older than 7 days."))
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

    suspend fun scanCloudSnapshots(
        onLog: ((TerminalLog) -> Unit)? = null
    ): Result<List<SnapshotEntity>> = withContext(Dispatchers.IO) {
        runCatching {
            val client = getApiClient() ?: throw IllegalStateException("Bot token missing.")
            val chatId = prefs.chatId.first() ?: throw IllegalStateException("Chat ID not configured.")
            val masterKey = prefs.getOrGenerateMasterKey()

            onLog?.invoke(TerminalLog(level = LogLevel.INFO, message = "Scanning Telegram Cloud for snapshots..."))
            val chat = client.getChat(chatId).getOrThrow()
            var currentMsg: TelegramMessage? = chat.pinnedMessage
            var currentMsgId: Long? = currentMsg?.messageId
            val visitedMsgIds = mutableSetOf<Long>()
            var discoveredCount = 0

            while (currentMsgId != null && visitedMsgIds.add(currentMsgId)) {
                val doc = currentMsg?.document
                val caption = currentMsg?.caption.orEmpty()
                if (doc != null && (doc.fileName?.startsWith("manifest_") == true || caption.contains(ManifestManager.MANIFEST_TAG))) {
                    val fileInfo = client.getFile(doc.fileId).getOrNull()
                    if (fileInfo?.filePath != null) {
                        val bytes = client.downloadFileBytes(fileInfo.filePath).getOrNull()
                        if (bytes != null) {
                            val manifestResult = runCatching { ManifestManager.deserializeManifest(bytes, masterKey) }
                            val manifest = manifestResult.getOrNull()
                            if (manifest != null) {
                                db.snapshotDao().insertSnapshot(
                                    SnapshotEntity(
                                        snapshotId = manifest.snapshotId,
                                        tag = manifest.tag,
                                        timestamp = manifest.timestamp,
                                        rootFolderName = manifest.rootFolderName,
                                        totalFiles = manifest.totalFiles,
                                        totalBytes = manifest.totalBytes,
                                        isEncrypted = manifest.isEncrypted,
                                        manifestMessageId = currentMsgId,
                                        manifestFileId = doc.fileId,
                                        rawJson = ManifestManager.json.encodeToString(manifest)
                                    )
                                )
                                discoveredCount++
                                onLog?.invoke(TerminalLog(level = LogLevel.SNAP, message = "Discovered snapshot [${manifest.snapshotId}] (${manifest.totalFiles} files)"))

                                val prevId = manifest.previousManifestMessageId
                                if (prevId != null && !visitedMsgIds.contains(prevId)) {
                                    val fwd = client.forwardMessage(chatId, chatId, prevId).getOrNull()
                                    if (fwd != null) {
                                        client.deleteMessage(chatId, fwd.messageId)
                                        currentMsg = fwd
                                        currentMsgId = prevId
                                        continue
                                    }
                                }
                            } else {
                                val err = manifestResult.exceptionOrNull()?.message ?: "Decryption or parse error"
                                onLog?.invoke(TerminalLog(level = LogLevel.WARN, message = "Manifest at msg #$currentMsgId skipped: $err"))
                            }
                        }
                    }
                }
                break
            }
            onLog?.invoke(TerminalLog(level = LogLevel.OK, message = "Scan complete. Discovered $discoveredCount remote snapshots."))
            db.snapshotDao().getAllSnapshotsDirect()
        }
    }

    suspend fun deleteCloudSnapshots(
        snapshotIds: List<String>,
        onLog: (TerminalLog) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val client = getApiClient() ?: throw IllegalStateException("Bot token missing.")
            val chatId = prefs.chatId.first() ?: throw IllegalStateException("Chat ID not configured.")
            val allSnapshots = db.snapshotDao().getAllSnapshotsDirect()
            val targetSnapshots = allSnapshots.filter { it.snapshotId in snapshotIds }
            val remainingSnapshots = allSnapshots.filter { it.snapshotId !in snapshotIds }

            val activeMessageIds = mutableSetOf<Long>()
            for (snap in remainingSnapshots) {
                activeMessageIds.add(snap.manifestMessageId)
                val m = runCatching { ManifestManager.json.decodeFromString<SnapshotManifest>(snap.rawJson) }.getOrNull()
                if (m != null) {
                    for (file in m.files) {
                        activeMessageIds.add(file.messageId)
                        for (chunk in file.chunkParts) {
                            activeMessageIds.add(chunk.messageId)
                        }
                    }
                }
            }

            var deletedFilesCount = 0
            for (snap in targetSnapshots) {
                onLog(TerminalLog(level = LogLevel.WARN, message = "Deleting cloud snapshot [${snap.snapshotId}]..."))
                // Delete manifest message
                client.deleteMessage(chatId, snap.manifestMessageId)

                // Delete orphaned chunks
                val m = runCatching { ManifestManager.json.decodeFromString<SnapshotManifest>(snap.rawJson) }.getOrNull()
                if (m != null) {
                    for (file in m.files) {
                        if (!activeMessageIds.contains(file.messageId)) {
                            client.deleteMessage(chatId, file.messageId)
                            deletedFilesCount++
                        }
                        for (chunk in file.chunkParts) {
                            if (!activeMessageIds.contains(chunk.messageId)) {
                                client.deleteMessage(chatId, chunk.messageId)
                                deletedFilesCount++
                            }
                        }
                    }
                }
                // Delete from DB
                db.snapshotDao().deleteSnapshot(snap.snapshotId)
            }

            // Update pinned message: if pinned was one of target, pin the latest remaining snapshot if any
            val chat = client.getChat(chatId).getOrNull()
            val pinnedId = chat?.pinnedMessage?.messageId
            if (pinnedId != null && targetSnapshots.any { it.manifestMessageId == pinnedId }) {
                client.unpinChatMessage(chatId, pinnedId)
                if (remainingSnapshots.isNotEmpty()) {
                    client.pinChatMessage(chatId, remainingSnapshots.first().manifestMessageId)
                }
            }

            onLog(TerminalLog(level = LogLevel.OK, message = "Deleted ${targetSnapshots.size} snapshots ($deletedFilesCount cloud files removed)."))
            targetSnapshots.size
        }
    }

    suspend fun wipeAllCloudVault(
        onLog: (TerminalLog) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val client = getApiClient() ?: throw IllegalStateException("Bot token missing.")
            val chatId = prefs.chatId.first() ?: throw IllegalStateException("Chat ID not configured.")
            val allSnapshots = db.snapshotDao().getAllSnapshotsDirect()

            onLog(TerminalLog(level = LogLevel.WARN, message = "Wiping entire cloud vault (${allSnapshots.size} snapshots)..."))

            // Unpin chat message if any
            val chat = client.getChat(chatId).getOrNull()
            chat?.pinnedMessage?.messageId?.let { pinnedId ->
                client.unpinChatMessage(chatId, pinnedId)
            }

            var deletedCount = 0
            val deletedMsgIds = mutableSetOf<Long>()
            for (snap in allSnapshots) {
                if (deletedMsgIds.add(snap.manifestMessageId)) {
                    client.deleteMessage(chatId, snap.manifestMessageId)
                    deletedCount++
                }
                val m = runCatching { ManifestManager.json.decodeFromString<SnapshotManifest>(snap.rawJson) }.getOrNull()
                if (m != null) {
                    for (file in m.files) {
                        if (deletedMsgIds.add(file.messageId)) {
                            client.deleteMessage(chatId, file.messageId)
                            deletedCount++
                        }
                        for (chunk in file.chunkParts) {
                            if (deletedMsgIds.add(chunk.messageId)) {
                                client.deleteMessage(chatId, chunk.messageId)
                                deletedCount++
                            }
                        }
                    }
                }
            }

            // Also clean any pending uploads messages
            val pending = db.pendingUploadDao().all()
            for (p in pending) {
                if (p.messageId > 0 && deletedMsgIds.add(p.messageId)) {
                    client.deleteMessage(chatId, p.messageId)
                    deletedCount++
                }
            }

            // Wipe database entries
            db.snapshotDao().deleteAllSnapshots()
            db.pendingUploadDao().clearAll()

            onLog(TerminalLog(level = LogLevel.OK, message = "Cloud vault wiped. $deletedCount messages permanently deleted from Telegram."))
            deletedCount
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

            val state = RestoreState()
            var restoredCount = 0
            var upToDateCount = 0
            val failedFiles = mutableListOf<String>()

            for ((idx, fileEntry) in manifest.files.withIndex()) {
                onLog(TerminalLog(
                    level = LogLevel.SYNC,
                    message = "[${idx + 1}/${manifest.files.size}] Restoring ${fileEntry.relativePath} (${formatSize(fileEntry.size)})..."
                ))

                try {
                    when (restoreFile(client, folderUri, manifest, fileEntry, masterKey, state, onLog)) {
                        RestoreOutcome.RESTORED -> restoredCount++
                        RestoreOutcome.UP_TO_DATE -> {
                            upToDateCount++
                            onLog(TerminalLog(level = LogLevel.PLAIN, message = "  already up to date - skipped"))
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: WrongKeyException) {
                    throw e
                } catch (e: Exception) {
                    failedFiles.add(fileEntry.relativePath)
                    onLog(TerminalLog(level = LogLevel.ERR, message = "Could not restore ${fileEntry.relativePath}: ${describe(e)}"))
                }
            }

            if (manifest.files.isNotEmpty() && restoredCount + upToDateCount == 0) {
                throw IllegalStateException("Restore failed for all ${manifest.files.size} file(s). Nothing was written.")
            }

            onLog(TerminalLog(
                level = LogLevel.OK,
                message = "Restore complete: $restoredCount restored, $upToDateCount already up to date, ${failedFiles.size} failed."
            ))
            if (failedFiles.isNotEmpty()) {
                onLog(TerminalLog(level = LogLevel.WARN, message = "Existing files were left untouched for the failed ones. Run restore again to retry."))
            }
            restoredCount
        }
    }

    /**
     * Restores one file by streaming it into a `.tbpart` temp file, verifying the whole-file
     * SHA-256, and only then replacing the destination. A failure leaves any existing file intact.
     */
    private suspend fun restoreFile(
        client: TelegramApiClient,
        folderUri: Uri,
        manifest: SnapshotManifest,
        entry: FileEntry,
        masterKey: String,
        state: RestoreState,
        onLog: (TerminalLog) -> Unit
    ): RestoreOutcome {
        val existing = safHelper.findExistingFile(folderUri, entry.relativePath)
        if (existing != null && existing.length() == entry.size &&
            safHelper.calculateSha256(existing.uri) == entry.sha256
        ) {
            return RestoreOutcome.UP_TO_DATE
        }

        val target = safHelper.openRestoreTarget(folderUri, entry.relativePath)
            ?: throw IllegalStateException("Cannot create destination file")
        val digest = MessageDigest.getInstance("SHA-256")

        fun plain(bytes: ByteArray): ByteArray {
            if (!manifest.isEncrypted) return bytes
            return try {
                CryptoEngine.decryptBytes(bytes, masterKey).also { state.decryptedOk = true }
            } catch (e: AEADBadTagException) {
                if (!state.decryptedOk) throw WrongKeyException()
                throw IllegalStateException("Data is corrupted (authentication failed)")
            }
        }

        fun emit(bytes: ByteArray) {
            target.write(bytes)
            digest.update(bytes)
        }

        try {
            if (entry.chunkCount > 1 && entry.chunkParts.isNotEmpty()) {
                val parts = entry.chunkParts.sortedBy { it.partIndex }
                if (entry.chunkEncrypted) {
                    // Streamed format: each part is independent, so memory use stays at one part.
                    for (part in parts) {
                        onLog(TerminalLog(level = LogLevel.PLAIN, message = "  part ${part.partIndex}/${parts.size}"))
                        emit(plain(downloadPart(client, part.fileId, part.sha256, onLog)))
                    }
                } else {
                    // Legacy format: whole file was encrypted, then split.
                    val downloaded = parts.map { downloadPart(client, it.fileId, it.sha256, onLog) }
                    emit(plain(FileChunker.reassemble(downloaded)))
                }
            } else {
                emit(plain(downloadPart(client, entry.fileId, null, onLog)))
            }

            val actualSha = digest.digest().joinToString("") { "%02x".format(it) }
            if (actualSha != entry.sha256) {
                throw IllegalStateException("Checksum mismatch after download - file was not written")
            }
            if (!target.commit()) {
                throw IllegalStateException("Could not move the verified file into place")
            }
        } catch (e: Throwable) {
            target.abort()
            throw e
        }
        return RestoreOutcome.RESTORED
    }

    private suspend fun downloadPart(
        client: TelegramApiClient,
        fileId: String,
        expectedSha: String?,
        onLog: (TerminalLog) -> Unit
    ): ByteArray = RetryPolicy.execute(onRetry = retryLogger(onLog, "Download")) {
        val info = try {
            client.getFile(fileId).getOrThrow()
        } catch (e: TelegramApiException) {
            if (e.httpCode == 400 && e.message.orEmpty().contains("too big", ignoreCase = true)) {
                throw IllegalStateException(
                    "Telegram will not serve this part to bots because it is over 20 MB. " +
                        "It was uploaded by an older TeleBack build that used 45 MB parts."
                )
            }
            throw e
        }
        val path = info.filePath ?: throw IllegalStateException("Telegram returned no file_path")
        val bytes = client.downloadFileBytes(path).getOrThrow()
        if (!expectedSha.isNullOrBlank() && ChecksumUtils.sha256(bytes) != expectedSha) {
            throw IOException("Downloaded part is corrupted (checksum mismatch)")
        }
        bytes
    }

    private fun keyFingerprint(masterKey: String): String =
        ChecksumUtils.sha256(masterKey.toByteArray(Charsets.UTF_8)).take(16)

    private fun describe(error: Throwable): String =
        error.message?.takeIf { it.isNotBlank() } ?: error::class.java.simpleName

    /** Logs each retry; on connectivity errors it also waits (up to 30 min) for the network to return. */
    private fun retryLogger(
        onLog: (TerminalLog) -> Unit,
        what: String
    ): suspend (Int, Int, Long, Throwable) -> Unit = { attempt, max, delayMs, error ->
        onLog(TerminalLog(
            level = LogLevel.WARN,
            message = "$what failed (attempt $attempt/$max): ${describe(error)}. Retrying in ${delayMs / 1000}s..."
        ))
        if (error is IOException) awaitOnline(onLog)
    }

    private suspend fun awaitOnline(onLog: (TerminalLog) -> Unit) {
        if (connectivity.isOnline.first()) return
        onLog(TerminalLog(level = LogLevel.WARN, message = "Offline - waiting for the connection to come back, then resuming..."))
        val back = withTimeoutOrNull(OFFLINE_WAIT_TIMEOUT_MS) { connectivity.isOnline.first { it } }
        if (back == true) {
            onLog(TerminalLog(level = LogLevel.INFO, message = "Back online - resuming."))
        } else {
            onLog(TerminalLog(level = LogLevel.ERR, message = "Still offline after 30 min - giving up on this step."))
        }
    }

    private fun formatSize(bytes: Long): String = SizeFormat.format(bytes)
}
