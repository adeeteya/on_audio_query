package com.lucasjosino.on_audio_query.queries

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioQueryOptionsTest {

    @Test
    fun `query values are parameterized and wildcard characters are escaped`() {
        val hostilePrefix = "Music/100%_mix\\' OR 1=1"
        val selection = buildAudioSelection(
            NativeAudioQueryOptions(
                isMusic = true,
                includeAlarms = false,
                minimumDuration = 30_000,
                volumeNames = listOf("external_primary"),
                paths = listOf(
                    NativeAudioPathFilter("external_primary", hostilePrefix)
                ),
                modifiedAfter = 100
            ),
            legacyPath = null,
            sdkInt = 30
        )

        assertFalse(selection.selection!!.contains(hostilePrefix))
        assertTrue(selection.selection!!.contains("is_music = ?"))
        assertTrue(selection.selection!!.contains("relative_path LIKE ?"))
        assertEquals(
            listOf(
                "1",
                "0",
                "30000",
                "100",
                "external_primary",
                "external_primary",
                "Music/100\\%\\_mix\\\\' OR 1=1%"
            ),
            selection.arguments!!.toList()
        )
    }

    @Test
    fun `path filters are grouped with or and other filters with and`() {
        val selection = buildAudioSelection(
            NativeAudioQueryOptions(
                isMusic = true,
                paths = listOf(
                    NativeAudioPathFilter("one", "Music/"),
                    NativeAudioPathFilter("two", "Podcasts/")
                )
            ),
            legacyPath = null,
            sdkInt = 29
        )

        assertTrue(selection.selection!!.startsWith("is_music = ? AND ("))
        assertTrue(selection.selection!!.contains(" OR "))
    }

    @Test
    fun `legacy paths use data with selection arguments`() {
        val selection = buildAudioSelection(
            NativeAudioQueryOptions(
                paths = listOf(NativeAudioPathFilter("external", "Music/_mix/"))
            ),
            legacyPath = "/storage/100%music",
            sdkInt = 28
        )

        assertTrue(selection.selection!!.contains("_data LIKE ?"))
        assertFalse(selection.selection!!.contains("100%music"))
        assertEquals(
            listOf(
                "%/storage/100\\%music/%",
                "%/Music/\\_mix/%"
            ),
            selection.arguments!!.toList()
        )
    }

    @Test
    fun `empty volume set always produces an empty selection`() {
        val selection = buildAudioSelection(
            NativeAudioQueryOptions(volumeNames = emptyList()),
            legacyPath = null,
            sdkInt = 30
        )

        assertEquals("0", selection.selection)
        assertNull(selection.arguments)
    }

    @Test
    fun `page offset is only returned when rows remain`() {
        assertEquals(500, nextPageOffset(250, 250, 501))
        assertNull(nextPageOffset(500, 1, 501))
        assertNull(nextPageOffset(700, 0, 501))
    }
}
