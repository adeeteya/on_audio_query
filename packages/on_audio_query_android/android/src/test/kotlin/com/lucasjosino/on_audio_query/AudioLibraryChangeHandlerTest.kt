package com.lucasjosino.on_audio_query

import android.content.ContentResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioLibraryChangeHandlerTest {

    @Test
    fun `change flags map to event types on Android 11`() {
        assertEquals("insert", audioChangeTypeForFlags(ContentResolver.NOTIFY_INSERT, 30))
        assertEquals("update", audioChangeTypeForFlags(ContentResolver.NOTIFY_UPDATE, 30))
        assertEquals("delete", audioChangeTypeForFlags(ContentResolver.NOTIFY_DELETE, 30))
    }

    @Test
    fun `old and ambiguous notifications are unknown`() {
        assertEquals("unknown", audioChangeTypeForFlags(ContentResolver.NOTIFY_INSERT, 29))
        assertEquals(
            "unknown",
            audioChangeTypeForFlags(
                ContentResolver.NOTIFY_INSERT or ContentResolver.NOTIFY_UPDATE,
                30
            )
        )
        assertEquals("unknown", audioChangeTypeForFlags(0, 30))
    }
}
