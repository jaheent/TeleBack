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
}
