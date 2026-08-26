package org.wut.items.collector.data

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue









class MediaCleanerTest {

    private val mediaDir = "/cache/media".toPath()

    
    private fun setupDir(fs: FakeFileSystem) {
        fs.createDirectories(mediaDir)
    }

    
    private fun writeFile(fs: FakeFileSystem, name: String, content: String = "x") {
        fs.write(mediaDir / name) { writeUtf8(content) }
    }

    @Test
    fun `cleanOrphans returns 0 when directory does not exist`() {
        val fs = FakeFileSystem()
        
        val cleaner = MediaCleaner(fs) { emptyList() }

        val deleted = cleaner.cleanOrphans(mediaDir.toString())

        assertEquals(0, deleted)
    }

    @Test
    fun `cleanOrphans deletes orphan editor files when nothing referenced`() {
        val fs = FakeFileSystem()
        setupDir(fs)
        writeFile(fs, "edit_aaaa.jpg")
        writeFile(fs, "cam_bbbb.jpg")
        writeFile(fs, "img_cccc.jpg")
        val cleaner = MediaCleaner(fs) { emptyList() }

        val deleted = cleaner.cleanOrphans(mediaDir.toString())

        assertEquals(3, deleted)
        assertFalse(fs.exists(mediaDir / "edit_aaaa.jpg"))
        assertFalse(fs.exists(mediaDir / "cam_bbbb.jpg"))
        assertFalse(fs.exists(mediaDir / "img_cccc.jpg"))
    }

    @Test
    fun `cleanOrphans keeps referenced files`() {
        val fs = FakeFileSystem()
        setupDir(fs)
        writeFile(fs, "edit_keep.jpg")
        writeFile(fs, "edit_orphan.jpg")
        
        val cleaner = MediaCleaner(fs) { listOf((mediaDir / "edit_keep.jpg").toString()) }

        val deleted = cleaner.cleanOrphans(mediaDir.toString())

        assertEquals(1, deleted)
        assertTrue(fs.exists(mediaDir / "edit_keep.jpg"))
        assertFalse(fs.exists(mediaDir / "edit_orphan.jpg"))
    }

    @Test
    fun `cleanOrphans ignores files with unknown prefix`() {
        val fs = FakeFileSystem()
        setupDir(fs)
        writeFile(fs, "random.txt")
        writeFile(fs, "thumbnail.png")
        writeFile(fs, "edit_orphan.jpg")
        val cleaner = MediaCleaner(fs) { emptyList() }

        val deleted = cleaner.cleanOrphans(mediaDir.toString())

        
        assertEquals(1, deleted)
        assertTrue(fs.exists(mediaDir / "random.txt"))
        assertTrue(fs.exists(mediaDir / "thumbnail.png"))
        assertFalse(fs.exists(mediaDir / "edit_orphan.jpg"))
    }

    @Test
    fun `cleanOrphans handles all three known prefixes`() {
        val fs = FakeFileSystem()
        setupDir(fs)
        writeFile(fs, "edit_1.jpg")
        writeFile(fs, "cam_1.jpg")
        writeFile(fs, "img_1.jpg")
        
        val cleaner = MediaCleaner(fs) { listOf((mediaDir / "cam_1.jpg").toString()) }

        val deleted = cleaner.cleanOrphans(mediaDir.toString())

        assertEquals(2, deleted)
        assertFalse(fs.exists(mediaDir / "edit_1.jpg"))
        assertTrue(fs.exists(mediaDir / "cam_1.jpg"))
        assertFalse(fs.exists(mediaDir / "img_1.jpg"))
    }

    @Test
    fun `cleanOrphans returns 0 when no orphan files exist`() {
        val fs = FakeFileSystem()
        setupDir(fs)
        writeFile(fs, "edit_a.jpg")
        writeFile(fs, "edit_b.jpg")
        
        val cleaner = MediaCleaner(fs) {
            listOf(
                (mediaDir / "edit_a.jpg").toString(),
                (mediaDir / "edit_b.jpg").toString()
            )
        }

        val deleted = cleaner.cleanOrphans(mediaDir.toString())

        assertEquals(0, deleted)
        assertTrue(fs.exists(mediaDir / "edit_a.jpg"))
        assertTrue(fs.exists(mediaDir / "edit_b.jpg"))
    }

    @Test
    fun `cleanOrphans handles empty directory`() {
        val fs = FakeFileSystem()
        setupDir(fs)
        val cleaner = MediaCleaner(fs) { emptyList() }

        val deleted = cleaner.cleanOrphans(mediaDir.toString())

        assertEquals(0, deleted)
    }
}
