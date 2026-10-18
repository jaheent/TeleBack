package com.teleback.core.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.teleback.core.crypto.ChecksumUtils
import com.teleback.data.model.LocalFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class SafStorageHelper(private val context: Context) {

    suspend fun scanDirectory(treeUri: Uri): List<LocalFileItem> = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext emptyList()

        val results = mutableListOf<LocalFileItem>()
        traverseFolder(rootDoc, "", results)
        results
    }

    private fun traverseFolder(
        parentDoc: DocumentFile,
        currentPath: String,
        results: MutableList<LocalFileItem>
    ) {
        val files = parentDoc.listFiles()
        for (file in files) {
            val fileName = file.name ?: continue
            val relativePath = if (currentPath.isEmpty()) fileName else "$currentPath/$fileName"

            if (file.isDirectory) {
                traverseFolder(file, relativePath, results)
            } else if (file.isFile) {
                results.add(
                    LocalFileItem(
                        uri = file.uri,
                        relativePath = relativePath,
                        name = fileName,
                        size = file.length(),
                        lastModified = file.lastModified()
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

    suspend fun writeRestoredFile(
        treeUri: Uri,
        relativePath: String,
        data: ByteArray
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@runCatching false
            val parts = relativePath.split("/")
            var currentDir = rootDoc

            // Create parent folders if necessary
            for (i in 0 until parts.size - 1) {
                val folderName = parts[i]
                var nextDir = currentDir.findFile(folderName)
                if (nextDir == null || !nextDir.isDirectory) {
                    nextDir = currentDir.createDirectory(folderName)
                        ?: return@runCatching false
                }
                currentDir = nextDir
            }

            val targetFileName = parts.last()
            // Delete existing file if present to overwrite cleanly
            currentDir.findFile(targetFileName)?.delete()
            val newFile = currentDir.createFile("application/octet-stream", targetFileName)
                ?: return@runCatching false

            context.contentResolver.openOutputStream(newFile.uri)?.use { out ->
                out.write(data)
                out.flush()
            }
            true
        }.getOrDefault(false)
    }
}
