package com.lucasjosino.on_audio_query.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MediaScanPathTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `null blank and directory-shaped paths are rejected`() {
        assertFalse(isValidMediaFilePath(null))
        assertFalse(isValidMediaFilePath(""))
        assertFalse(isValidMediaFilePath("   "))
        assertFalse(isValidMediaFilePath("/storage/emulated/0/Music/"))
        assertFalse(isValidMediaFilePath("C:\\Music\\"))
    }

    @Test
    fun `existing files are accepted`() {
        val mediaFile = temporaryFolder.newFile("song.mp3")

        assertTrue(isValidMediaFilePath(mediaFile.path))
    }

    @Test
    fun `existing directories and storage roots are rejected`() {
        assertFalse(isValidMediaFilePath(temporaryFolder.root.path))
        assertFalse(isValidMediaFilePath(temporaryFolder.newFolder("Music").path))
    }

    @Test
    fun `deleted file paths remain valid for removal rescans`() {
        val deletedMediaFile = temporaryFolder.newFile("deleted-song.mp3")
        val deletedPath = deletedMediaFile.path
        assertTrue(deletedMediaFile.delete())

        assertTrue(isValidMediaFilePath(deletedPath))
    }
}
