package com.lucasjosino.on_audio_query.types.sorttypes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongSortTypeTest {

    @Test
    fun `public song sort types map to the expected value kinds`() {
        for (sortType in 0..6) {
            val spec = resolveSongSortSpec(sortType, order = 0, ignoreCase = true)

            assertEquals(sortType in 3..5, spec.numeric)
            assertFalse(spec.transientColumn)
            assertTrue(spec.mediaStoreOrder.endsWith(" ASC"))
        }
    }

    @Test
    fun `string values from different volumes are globally sorted`() {
        val spec = resolveSongSortSpec(sortType = 0, order = 0, ignoreCase = true)
        val songs = arrayListOf(
            song(spec.column, "beta", "content://media/primary/audio/2"),
            song(spec.column, "Alpha", "content://media/sd/audio/2"),
            song(spec.column, "alpha", "content://media/primary/audio/1")
        )

        songs.sortWith(spec.comparator())

        assertEquals(
            listOf(
                "content://media/primary/audio/1",
                "content://media/sd/audio/2",
                "content://media/primary/audio/2"
            ),
            songs.map { it["_uri"] }
        )
    }

    @Test
    fun `case-sensitive and case-insensitive ordering differ`() {
        val caseSensitive = resolveSongSortSpec(sortType = 0, order = 0, ignoreCase = false)
        val caseInsensitive = resolveSongSortSpec(sortType = 0, order = 0, ignoreCase = true)
        val original = listOf(
            song(caseSensitive.column, "apple", "content://media/audio/2"),
            song(caseSensitive.column, "Zebra", "content://media/audio/1")
        )

        val sensitiveSongs = ArrayList(original).apply {
            sortWith(caseSensitive.comparator())
        }
        val insensitiveSongs = ArrayList(original).apply {
            sortWith(caseInsensitive.comparator())
        }

        assertEquals("Zebra", sensitiveSongs.first()[caseSensitive.column])
        assertEquals("apple", insensitiveSongs.first()[caseInsensitive.column])
    }

    @Test
    fun `numeric values support descending order and nulls`() {
        for (sortType in 3..5) {
            val spec = resolveSongSortSpec(sortType, order = 1, ignoreCase = true)
            val songs = arrayListOf(
                song(spec.column, 10, "content://media/audio/2"),
                song(spec.column, null, "content://media/audio/3"),
                song(spec.column, 30, "content://media/audio/1")
            )

            songs.sortWith(spec.comparator())

            assertEquals(listOf(30, 10, null), songs.map { it[spec.column] })
        }
    }

    @Test
    fun `default sorting uses a transient title key`() {
        val spec = resolveSongSortSpec(sortType = null, order = 0, ignoreCase = true)
        val songs = arrayListOf(
            song(spec.column, "charlie", "content://media/audio/2"),
            song(spec.column, "bravo", "content://media/audio/1")
        )

        songs.sortWith(spec.comparator())

        assertTrue(spec.transientColumn)
        assertEquals(listOf("bravo", "charlie"), songs.map { it[spec.column] })
    }

    @Test
    fun `uri is a deterministic tie breaker`() {
        val spec = resolveSongSortSpec(sortType = 0, order = 1, ignoreCase = true)
        val songs = arrayListOf(
            song(spec.column, "same", "content://media/sd/audio/1"),
            song(spec.column, "same", "content://media/primary/audio/1")
        )

        songs.sortWith(spec.comparator())

        assertEquals(
            listOf(
                "content://media/primary/audio/1",
                "content://media/sd/audio/1"
            ),
            songs.map { it["_uri"] }
        )
    }

    private fun song(
        column: String,
        value: Any?,
        uri: String
    ): MutableMap<String, Any?> {
        return mutableMapOf(
            column to value,
            "_uri" to uri
        )
    }
}
