package com.teleback.presentation.restore

import com.teleback.data.model.FileEntry
import com.teleback.data.model.SnapshotManifest

/**
 * Immutable tree node representation of files and directories within a snapshot.
 */
sealed class SnapshotTreeNode {
    abstract val name: String
    abstract val relativePath: String
    abstract val depth: Int

    data class FolderNode(
        override val name: String,
        override val relativePath: String,
        override val depth: Int,
        val children: List<SnapshotTreeNode>,
        val totalFilesCount: Int,
        val totalBytes: Long,
        val isEmptyFolder: Boolean = false
    ) : SnapshotTreeNode()

    data class FileNode(
        override val name: String,
        override val relativePath: String,
        override val depth: Int,
        val fileEntry: FileEntry
    ) : SnapshotTreeNode()
}

/**
 * Transient in-dialog status banner data.
 */
data class ExtractionBanner(
    val message: String,
    val isSuccess: Boolean
)

/**
 * Pure builder and flattening utility for snapshot directory trees.
 */
object SnapshotTreeBuilder {

    private class MutableFolder(
        val name: String,
        val relativePath: String,
        val depth: Int
    ) {
        val subfolders = mutableMapOf<String, MutableFolder>()
        val files = mutableListOf<SnapshotTreeNode.FileNode>()
        var isEmptyDirectory = false

        fun toImmutable(): SnapshotTreeNode.FolderNode {
            val immutableSubfolders = subfolders.values.map { it.toImmutable() }
            val allChildren = (immutableSubfolders + files).sortedWith(
                compareBy({ it !is SnapshotTreeNode.FolderNode }, { it.name.lowercase() })
            )
            val fileCount = files.size + immutableSubfolders.sumOf { it.totalFilesCount }
            val byteCount = files.sumOf { it.fileEntry.size } + immutableSubfolders.sumOf { it.totalBytes }
            return SnapshotTreeNode.FolderNode(
                name = name,
                relativePath = relativePath,
                depth = depth,
                children = allChildren,
                totalFilesCount = fileCount,
                totalBytes = byteCount,
                isEmptyFolder = isEmptyDirectory && fileCount == 0
            )
        }
    }

    /**
     * Builds an immutable hierarchical tree from the snapshot manifest.
     * The root folder is a virtual container with depth = -1, name = "", relativePath = "".
     * Top-level files and directories in the snapshot linked folder are direct children at depth = 0.
     */
    fun buildTree(manifest: SnapshotManifest): SnapshotTreeNode.FolderNode {
        val root = MutableFolder(name = "", relativePath = "", depth = -1)

        // 1. Register explicit directories (including empty ones)
        for (rawDir in manifest.directories) {
            val sanitized = sanitizePath(rawDir)
            if (sanitized.isEmpty()) continue
            val segments = sanitized.split('/')
            var current = root
            var pathAcc = ""
            for ((idx, segment) in segments.withIndex()) {
                pathAcc = if (pathAcc.isEmpty()) segment else "$pathAcc/$segment"
                current = current.subfolders.getOrPut(segment) {
                    MutableFolder(name = segment, relativePath = pathAcc, depth = idx)
                }
            }
            current.isEmptyDirectory = true
        }

        // 2. Register all files
        for (file in manifest.files) {
            val sanitized = sanitizePath(file.relativePath)
            if (sanitized.isEmpty()) continue
            val segments = sanitized.split('/')
            var current = root
            var pathAcc = ""
            for (idx in 0 until segments.lastIndex) {
                val segment = segments[idx]
                pathAcc = if (pathAcc.isEmpty()) segment else "$pathAcc/$segment"
                current = current.subfolders.getOrPut(segment) {
                    MutableFolder(name = segment, relativePath = pathAcc, depth = idx)
                }
            }
            val fileName = segments.last()
            current.files.add(
                SnapshotTreeNode.FileNode(
                    name = fileName,
                    relativePath = sanitized,
                    depth = current.depth + 1,
                    fileEntry = file
                )
            )
        }

        return root.toImmutable()
    }

    /**
     * Flattens the visible tree nodes for consumption in a virtualized Compose LazyColumn.
     * The virtual root (depth = -1) is omitted; top-level nodes start at depth = 0.
     * When searchQuery is active, matching ancestor folders are ephemerally expanded without
     * mutating the user's persistent expandedPaths set.
     */
    fun flattenVisibleTree(
        root: SnapshotTreeNode.FolderNode,
        expandedPaths: Set<String>,
        searchQuery: String = ""
    ): List<SnapshotTreeNode> {
        val trimmedQuery = searchQuery.trim().lowercase()

        // If search is active, compute ephemeral expanded ancestor paths and matching node paths
        val effectiveExpanded: Set<String>
        val matchingPaths: Set<String>?

        if (trimmedQuery.isNotEmpty()) {
            val matched = mutableSetOf<String>()
            val ancestors = mutableSetOf<String>()

            fun inspectMatches(node: SnapshotTreeNode): Boolean {
                var hasMatch = false
                when (node) {
                    is SnapshotTreeNode.FileNode -> {
                        if (node.name.lowercase().contains(trimmedQuery) ||
                            node.relativePath.lowercase().contains(trimmedQuery)
                        ) {
                            matched.add(node.relativePath)
                            hasMatch = true
                        }
                    }
                    is SnapshotTreeNode.FolderNode -> {
                        val folderNameMatches = node.name.lowercase().contains(trimmedQuery)
                        var childMatched = false
                        for (child in node.children) {
                            if (inspectMatches(child)) {
                                childMatched = true
                            }
                        }
                        if (folderNameMatches || childMatched) {
                            matched.add(node.relativePath)
                            hasMatch = true
                        }
                    }
                }
                return hasMatch
            }

            // Inspect all nodes from root
            for (child in root.children) {
                inspectMatches(child)
            }

            // Collect ancestors of all matched nodes
            for (path in matched) {
                val parts = path.split('/')
                var prefix = ""
                for (i in 0 until parts.lastIndex) {
                    prefix = if (prefix.isEmpty()) parts[i] else "$prefix/${parts[i]}"
                    ancestors.add(prefix)
                }
            }

            effectiveExpanded = expandedPaths + ancestors
            matchingPaths = matched
        } else {
            effectiveExpanded = expandedPaths
            matchingPaths = null
        }

        val result = mutableListOf<SnapshotTreeNode>()

        fun traverse(folder: SnapshotTreeNode.FolderNode) {
            for (child in folder.children) {
                if (matchingPaths != null && child.relativePath !in matchingPaths) {
                    continue
                }
                result.add(child)
                if (child is SnapshotTreeNode.FolderNode && effectiveExpanded.contains(child.relativePath)) {
                    traverse(child)
                }
            }
        }

        traverse(root)
        return result
    }

    /**
     * Recursively retrieves all descendant file relative paths for a given folder.
     */
    fun getAllDescendantFilePaths(folder: SnapshotTreeNode.FolderNode): Set<String> {
        val result = mutableSetOf<String>()
        fun collect(node: SnapshotTreeNode) {
            when (node) {
                is SnapshotTreeNode.FileNode -> result.add(node.relativePath)
                is SnapshotTreeNode.FolderNode -> node.children.forEach { collect(it) }
            }
        }
        folder.children.forEach { collect(it) }
        return result
    }

    /**
     * Computes the tri-state checkbox state for a folder:
     * - false: no files in folder are selected, OR folder contains 0 files.
     * - true: all descendant files in folder are selected.
     * - null: some (but not all) descendant files in folder are selected.
     */
    fun getFolderSelectionState(
        folder: SnapshotTreeNode.FolderNode,
        selectedPaths: Set<String>
    ): Boolean? {
        if (folder.totalFilesCount == 0) return false
        val descendantFiles = getAllDescendantFilePaths(folder)
        if (descendantFiles.isEmpty()) return false

        var selectedCount = 0
        for (path in descendantFiles) {
            if (path in selectedPaths) {
                selectedCount++
            }
        }

        return when (selectedCount) {
            0 -> false
            descendantFiles.size -> true
            else -> null
        }
    }

    /**
     * Sanitizes relative paths by converting Windows backslashes and stripping redundant slashes.
     */
    fun sanitizePath(path: String): String {
        return path.replace('\\', '/').trim('/')
    }
}
