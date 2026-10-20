package com.teleback.core.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.teleback.core.crypto.ChecksumUtils
import com.teleback.data.model.LocalFileItem
import com.teleback.data.model.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class SafStorageHelper(private val context: Context) {

    suspend fun scanDirectory(treeUri: Uri): ScanResult = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext ScanResult(emptyList(), emptyList())

        val directories = mutableListOf<String>()
        val files = mutableListOf<LocalFileItem>()
        traverseFolder(rootDoc, "", directories, files)
        ScanResult(directories = directories, files = files)
    }

    private fun traverseFolder(
        parentDoc: DocumentFile,
        currentPath: String,
        directories: MutableList<String>,
        files: MutableList<LocalFileItem>
    ) {
        val childDocs = parentDoc.listFiles()
        for (child in childDocs) {
            val childName = child.name ?: continue
            val relativePath = if (currentPath.isEmpty()) childName else "$currentPath/$childName"

            if (child.isDirectory) {
                // Record directory path so empty folders and folder structures are preserved
                directories.add(relativePath)
                traverseFolder(child, relativePath, directories, files)
            } else if (child.isFile) {
                files.add(
                    LocalFileItem(
                        uri = child.uri,
                        relativePath = relativePath,
                        name = childName,
                        size = child.length(),
                        lastModified = child.lastModified()
                    )
                )
            }
        }
    }

    fun openInputStream(uri: Uri): InputStream? {
        return context.contentResolver.openInputStream(uri)
    }

    suspend fun calculateSha256(uri: Uri): String = withContext(Dispatchers.IO) {
        openInputStream(uri)?.use { stream ->
            ChecksumUtils.sha256(stream)
        } ?: ""
    }

    suspend fun createDirectoryStructure(
        treeUri: Uri,
        directories: List<String>
    ): Unit = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext
        for (dirPath in directories.sortedBy { it.length }) {
            ensureDirectoryPath(rootDoc, dirPath)
        }
    }

    private fun ensureDirectoryPath(rootDoc: DocumentFile, relativeDirPath: String): DocumentFile? {
        if (relativeDirPath.isBlank()) return rootDoc
        val segments = relativeDirPath.split("/")
        var current = rootDoc
        for (segment in segments) {
            val trimmed = segment.trim()
            if (trimmed.isEmpty()) continue
            var next = current.findFile(trimmed)
            if (next == null || !next.isDirectory) {
                next = current.createDirectory(trimmed) ?: return null
            }
            current = next
        }
        return current
    }

    suspend fun writeRestoredFile(
        treeUri: Uri,
        relativePath: String,
        data: ByteArray
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@runCatching false
            val lastSlash = relativePath.lastIndexOf('/')
            val parentDir = if (lastSlash != -1) {
                val parentPath = relativePath.substring(0, lastSlash)
                ensureDirectoryPath(rootDoc, parentPath) ?: return@runCatching false
            } else {
                rootDoc
            }

            val targetFileName = if (lastSlash != -1) relativePath.substring(lastSlash + 1) else relativePath
            // Delete existing file if present to overwrite cleanly
            parentDir.findFile(targetFileName)?.delete()
            val newFile = parentDir.createFile("application/octet-stream", targetFileName)
                ?: return@runCatching false

            context.contentResolver.openOutputStream(newFile.uri)?.use { out ->
                out.write(data)
                out.flush()
            }
            true
        }.getOrDefault(false)
    }

    /** Looks up an already existing file inside the tree without creating anything. */
    suspend fun findExistingFile(treeUri: Uri, relativePath: String): DocumentFile? = withContext(Dispatchers.IO) {
        runCatching {
            var current = DocumentFile.fromTreeUri(context, treeUri) ?: return@runCatching null
            for (segment in relativePath.split("/").filter { it.isNotBlank() }) {
                current = current.findFile(segment) ?: return@runCatching null
            }
            if (current.isFile) current else null
        }.getOrNull()
    }

    /**
     * Opens a temporary `<name>.tbpart` file next to the final destination. Data is streamed into
     * it and only moved over the real file by [RestoreTarget.commit] after the checksum matches,
     * so a failed or interrupted restore never destroys an existing file.
     */
    suspend fun openRestoreTarget(treeUri: Uri, relativePath: String): RestoreTarget? = withContext(Dispatchers.IO) {
        runCatching {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@runCatching null
            val lastSlash = relativePath.lastIndexOf('/')
            val parentDir = if (lastSlash != -1) {
                ensureDirectoryPath(rootDoc, relativePath.substring(0, lastSlash)) ?: return@runCatching null
            } else {
                rootDoc
            }
            val finalName = if (lastSlash != -1) relativePath.substring(lastSlash + 1) else relativePath
            val tempName = "$finalName.tbpart"

            parentDir.findFile(tempName)?.delete() // leftover from an interrupted restore
            val tempFile = parentDir.createFile("application/octet-stream", tempName) ?: return@runCatching null
            val out = context.contentResolver.openOutputStream(tempFile.uri) ?: run {
                tempFile.delete()
                return@runCatching null
            }
            RestoreTarget(context, parentDir, tempFile, finalName, out)
        }.getOrNull()
    }

    class RestoreTarget internal constructor(
        private val context: Context,
        private val parentDir: DocumentFile,
        private val tempFile: DocumentFile,
        private val finalName: String,
        private val output: java.io.OutputStream
    ) {
        fun write(bytes: ByteArray) = output.write(bytes)

        /** Replaces any existing file with the verified temp file. Returns false on failure. */
        fun commit(): Boolean {
            return try {
                output.flush()
                output.close()
                parentDir.findFile(finalName)?.delete()
                if (tempFile.renameTo(finalName)) {
                    true
                } else {
                    // Some providers cannot rename: fall back to copying the verified bytes.
                    val finalFile = parentDir.createFile("application/octet-stream", finalName)
                        ?: return false
                    context.contentResolver.openInputStream(tempFile.uri)?.use { input ->
                        context.contentResolver.openOutputStream(finalFile.uri)?.use { out ->
                            input.copyTo(out)
                        } ?: return false
                    } ?: return false
                    tempFile.delete()
                    true
                }
            } catch (e: Exception) {
                abort()
                false
            }
        }

        /** Discards the partial file; any pre-existing destination file stays untouched. */
        fun abort() {
            runCatching { output.close() }
            runCatching { tempFile.delete() }
        }
    }
}

