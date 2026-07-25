package com.lucasjosino.on_audio_query.queries.helper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryHelperTest {

    @Test
    fun `potentially large numeric song columns use long reads`() {
        listOf(
            "_id",
            "_size",
            "album_id",
            "artist_id",
            "date_added",
            "date_modified",
            "duration",
            "generation_added",
            "generation_modified"
        ).forEach {
            assertTrue("$it should use getLong", isLongSongColumn(it))
        }
        assertFalse(isLongSongColumn("title"))
        assertFalse(isLongSongColumn("mime_type"))
    }
}
