package com.lucasjosino.on_audio_query.queries

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkQueryTest {

    @Test
    fun `large square artwork is sampled near the requested size`() {
        assertEquals(16, calculateInSampleSize(4096, 4096, 200))
    }

    @Test
    fun `panoramic artwork samples using its largest dimension`() {
        assertEquals(16, calculateInSampleSize(4096, 256, 200))
    }

    @Test
    fun `small artwork is not sampled`() {
        assertEquals(1, calculateInSampleSize(100, 100, 200))
    }

    @Test
    fun `invalid dimensions use the safe default sample`() {
        assertEquals(1, calculateInSampleSize(0, 4096, 200))
        assertEquals(1, calculateInSampleSize(4096, 0, 200))
        assertEquals(1, calculateInSampleSize(4096, 4096, 0))
    }
}
