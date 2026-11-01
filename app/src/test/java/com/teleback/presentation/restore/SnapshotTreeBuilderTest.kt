package com.teleback.presentation.restore

import com.teleback.data.model.FileEntry
import com.teleback.data.model.SnapshotManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotTreeBuilderTest {

    private fun createFile(path: String, size: Long = 1000L): FileEntry {
        return FileEntry(
            relativePath = path,
            size = size,
            sha256 = "dummy_sha256_${path.hashCode()}",
            modifiedAt = 1792500000000L,
            messageId = 100L,
            fileId = "tg_file_id",
            chunkCount = 1,
            chunkParts = emptyList()
        )
    }

    @Test
    fun testBuildTreeHierarchyAndAggregation() {
        val files = listOf(
            createFile("src/main/App.kt", size = 200L),
            createFile("src/main/res/logo.png", size = 800L),
            createFile("src/test/AppTest.kt", size = 300L),
            createFile("README.md", size = 100L)
        )
        val manifest = SnapshotManifest(
            snapshotId = "snap_1",
            deviceName = "test-device",
            rootFolderName = "Project",
            timestamp = 1000L,
            totalFiles = files.size,
            totalBytes = files.sumOf { it.size },
            directories = listOf("src", "src/main", "src/main/res", "src/test"),
            files = files
        )

        val root = SnapshotTreeBuilder.buildTree(manifest)

        // Virtual root checks
        assertEquals(-1, root.depth)
        assertEquals("", root.name)
        assertEquals(4, root.totalFilesCount)
        assertEquals(1400L, root.totalBytes)

        // Root direct children: "src" (folder) and "README.md" (file)
        assertEquals(2, root.children.size)
        val srcFolder = root.children[0] as SnapshotTreeNode.FolderNode
        val readmeFile = root.children[1] as SnapshotTreeNode.FileNode

        assertEquals("src", srcFolder.name)
        assertEquals("src", srcFolder.relativePath)
        assertEquals(0, srcFolder.depth)
        assertEquals(3, srcFolder.totalFilesCount)
        assertEquals(1300L, srcFolder.totalBytes)

        assertEquals("README.md", readmeFile.name)
        assertEquals("README.md", readmeFile.relativePath)
        assertEquals(0, readmeFile.depth)

        // Nested children inside "src"
        assertEquals(2, srcFolder.children.size)
        val mainFolder = srcFolder.children[0] as SnapshotTreeNode.FolderNode
        val testFolder = srcFolder.children[1] as SnapshotTreeNode.FolderNode

        assertEquals("main", mainFolder.name)
        assertEquals("src/main", mainFolder.relativePath)
        assertEquals(1, mainFolder.depth)
        assertEquals(2, mainFolder.totalFilesCount)
        assertEquals(1000L, mainFolder.totalBytes)

        assertEquals("test", testFolder.name)
        assertEquals(1, testFolder.depth)
        assertEquals(1, testFolder.totalFilesCount)
        assertEquals(300L, testFolder.totalBytes)
    }

    @Test
    fun testEmptyFolderPreservation() {
        val manifest = SnapshotManifest(
            snapshotId = "snap_2",
            deviceName = "test-device",
            rootFolderName = "EmptyTest",
            timestamp = 1000L,
            totalFiles = 1,
            totalBytes = 50L,
            directories = listOf("emptyDir", "nonEmptyDir"),
            files = listOf(createFile("nonEmptyDir/file.txt", size = 50L))
        )

        val root = SnapshotTreeBuilder.buildTree(manifest)
        assertEquals(2, root.children.size)

        val emptyFolder = root.children[0] as SnapshotTreeNode.FolderNode
        val nonEmptyFolder = root.children[1] as SnapshotTreeNode.FolderNode

        assertEquals("emptyDir", emptyFolder.name)
        assertTrue(emptyFolder.isEmptyFolder)
        assertEquals(0, emptyFolder.totalFilesCount)
        assertEquals(0L, emptyFolder.totalBytes)

        assertEquals("nonEmptyDir", nonEmptyFolder.name)
        assertFalse(nonEmptyFolder.isEmptyFolder)
        assertEquals(1, nonEmptyFolder.totalFilesCount)
    }

    @Test
    fun testDeterministicSiblingSorting() {
        // Files and folders intermixed out of order
        val files = listOf(
            createFile("zebra.txt"),
            createFile("apple.txt"),
            createFile("docs/notes.txt"),
            createFile("build/output.apk"),
            createFile("alpha/config.json")
        )
        val manifest = SnapshotManifest(
            snapshotId = "snap_3",
            deviceName = "test-device",
            rootFolderName = "SortTest",
            timestamp = 1000L,
            totalFiles = files.size,
            totalBytes = 5000L,
            directories = listOf("docs", "build", "alpha"),
            files = files
        )

        val root = SnapshotTreeBuilder.buildTree(manifest)

        // Folders first alphabetically (alpha, build, docs), then files alphabetically (apple.txt, zebra.txt)
        val childNames = root.children.map { it.name }
        assertEquals(listOf("alpha", "build", "docs", "apple.txt", "zebra.txt"), childNames)
    }

    @Test
    fun testVirtualRootOmissionAndDepthZero() {
        val files = listOf(createFile("rootFile.txt"))
        val manifest = SnapshotManifest(
            snapshotId = "snap_4",
            deviceName = "test-device",
            rootFolderName = "RootTest",
            timestamp = 1000L,
            totalFiles = 1,
            totalBytes = 1000L,
            directories = emptyList(),
            files = files
        )

        val root = SnapshotTreeBuilder.buildTree(manifest)
        val visible = SnapshotTreeBuilder.flattenVisibleTree(root, expandedPaths = emptySet())

        assertEquals(1, visible.size)
        val item = visible[0]
        assertEquals("rootFile.txt", item.name)
        assertEquals(0, item.depth) // Direct root children have depth = 0
    }

    @Test
    fun testFlattenRespectsExpandedPaths() {
        val files = listOf(
            createFile("folder/sub/child.txt"),
            createFile("folder/sibling.txt"),
            createFile("outside.txt")
        )
        val manifest = SnapshotManifest(
            snapshotId = "snap_5",
            deviceName = "test-device",
            rootFolderName = "ExpandTest",
            timestamp = 1000L,
            totalFiles = 3,
            totalBytes = 3000L,
            directories = listOf("folder", "folder/sub"),
            files = files
        )

        val root = SnapshotTreeBuilder.buildTree(manifest)

        // Initially nothing expanded
        val collapsed = SnapshotTreeBuilder.flattenVisibleTree(root, expandedPaths = emptySet())
        assertEquals(listOf("folder", "outside.txt"), collapsed.map { it.name })

        // Expand "folder" only
        val expandedFolder = SnapshotTreeBuilder.flattenVisibleTree(root, expandedPaths = setOf("folder"))
        assertEquals(listOf("folder", "sub", "sibling.txt", "outside.txt"), expandedFolder.map { it.name })

        // Expand "folder" and "folder/sub"
        val fullyExpanded = SnapshotTreeBuilder.flattenVisibleTree(root, expandedPaths = setOf("folder", "folder/sub"))
        assertEquals(listOf("folder", "sub", "child.txt", "sibling.txt", "outside.txt"), fullyExpanded.map { it.name })
    }

    @Test
    fun testSearchFilterEphemeralAutoExpansion() {
        val files = listOf(
            createFile("src/main/res/target.png"),
            createFile("src/main/ignore.kt"),
            createFile("docs/manual.pdf")
        )
        val manifest = SnapshotManifest(
            snapshotId = "snap_6",
            deviceName = "test-device",
            rootFolderName = "SearchTest",
            timestamp = 1000L,
            totalFiles = 3,
            totalBytes = 3000L,
            directories = listOf("src", "src/main", "src/main/res", "docs"),
            files = files
        )

        val root = SnapshotTreeBuilder.buildTree(manifest)
        val userExpanded = setOf<String>() // user has nothing expanded

        // Search for ".png"
        val searchResults = SnapshotTreeBuilder.flattenVisibleTree(root, expandedPaths = userExpanded, searchQuery = ".png")
        val names = searchResults.map { it.name }

        // Must show ancestors: src -> main -> res -> target.png
        assertEquals(listOf("src", "main", "res", "target.png"), names)

        // Clear query -> must revert to collapsed state with zero mutations
        val cleared = SnapshotTreeBuilder.flattenVisibleTree(root, expandedPaths = userExpanded, searchQuery = "")
        assertEquals(listOf("docs", "src"), cleared.map { it.name })
    }

    @Test
    fun testTriStateFolderSelection() {
        val files = listOf(
            createFile("docs/a.txt"),
            createFile("docs/b.txt"),
            createFile("docs/sub/c.txt")
        )
        val manifest = SnapshotManifest(
            snapshotId = "snap_7",
            deviceName = "test-device",
            rootFolderName = "TriStateTest",
            timestamp = 1000L,
            totalFiles = 3,
            totalBytes = 3000L,
            directories = listOf("docs", "docs/sub", "emptyFolder"),
            files = files
        )

        val root = SnapshotTreeBuilder.buildTree(manifest)
        val docsFolder = root.children.first { it.name == "docs" } as SnapshotTreeNode.FolderNode
        val emptyFolder = root.children.first { it.name == "emptyFolder" } as SnapshotTreeNode.FolderNode

        // Empty folder must always return false
        assertEquals(false, SnapshotTreeBuilder.getFolderSelectionState(emptyFolder, selectedPaths = emptySet()))
        assertEquals(false, SnapshotTreeBuilder.getFolderSelectionState(emptyFolder, selectedPaths = setOf("docs/a.txt")))

        // 0 of 3 files selected -> false
        assertEquals(false, SnapshotTreeBuilder.getFolderSelectionState(docsFolder, selectedPaths = emptySet()))

        // 1 of 3 files selected -> null (indeterminate)
        assertEquals(null, SnapshotTreeBuilder.getFolderSelectionState(docsFolder, selectedPaths = setOf("docs/a.txt")))

        // 2 of 3 files selected -> null (indeterminate)
        assertEquals(null, SnapshotTreeBuilder.getFolderSelectionState(docsFolder, selectedPaths = setOf("docs/a.txt", "docs/b.txt")))

        // 3 of 3 files selected -> true
        assertEquals(true, SnapshotTreeBuilder.getFolderSelectionState(docsFolder, selectedPaths = setOf("docs/a.txt", "docs/b.txt", "docs/sub/c.txt")))
    }

    @Test
    fun testPathSanitization() {
        assertEquals("docs/report.pdf", SnapshotTreeBuilder.sanitizePath("docs\\report.pdf"))
        assertEquals("docs/report.pdf", SnapshotTreeBuilder.sanitizePath("/docs/report.pdf/"))
        assertEquals("docs/sub/report.pdf", SnapshotTreeBuilder.sanitizePath("\\docs\\sub\\report.pdf\\"))
    }
}
