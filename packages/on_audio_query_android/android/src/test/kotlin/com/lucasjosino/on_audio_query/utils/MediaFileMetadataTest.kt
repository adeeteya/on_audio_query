package com.lucasjosino.on_audio_query.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaFileMetadataTest {

    @Test
    fun `display name supplies filename and extension`() {
        assertEquals(
            DisplayNameMetadata("track", "mp3"),
            parseDisplayName("track.mp3")
        )
        assertEquals(
            DisplayNameMetadata("archive.track", "flac"),
            parseDisplayName("archive.track.flac")
        )
    }

    @Test
    fun `display name without extension remains intact`() {
        assertEquals(
            DisplayNameMetadata("track", ""),
            parseDisplayName("track")
        )
        assertEquals(
            DisplayNameMetadata(".hidden", ""),
            parseDisplayName(".hidden")
        )
    }

    @Test
    fun `null and empty display names produce empty metadata`() {
        assertEquals(DisplayNameMetadata("", ""), parseDisplayName(null))
        assertEquals(DisplayNameMetadata("", ""), parseDisplayName(""))
    }
}
