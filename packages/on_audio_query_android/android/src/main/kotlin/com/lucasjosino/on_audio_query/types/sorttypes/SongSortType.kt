package com.lucasjosino.on_audio_query.types.sorttypes

import android.annotation.SuppressLint
import android.provider.MediaStore

@SuppressLint("InlinedApi")
fun checkSongSortType(sortType: Int?, order: Int, ignoreCase: Boolean): String {
    return resolveSongSortSpec(sortType, order, ignoreCase).mediaStoreOrder
}

@Suppress("DEPRECATION")
internal fun resolveSongSortSpec(
    sortType: Int?,
    order: Int,
    ignoreCase: Boolean
): SongSortSpec {
    val (column, numeric, transient) = when (sortType) {
        0 -> Triple(MediaStore.Audio.Media.TITLE, false, false)
        1 -> Triple(MediaStore.Audio.Media.ARTIST, false, false)
        2 -> Triple(MediaStore.Audio.Media.ALBUM, false, false)
        3 -> Triple(MediaStore.Audio.Media.DURATION, true, false)
        4 -> Triple(MediaStore.Audio.Media.DATE_ADDED, true, false)
        5 -> Triple(MediaStore.Audio.Media.SIZE, true, false)
        6 -> Triple(MediaStore.Audio.Media.DISPLAY_NAME, false, false)
        else -> Triple(MediaStore.Audio.Media.TITLE_KEY, false, true)
    }

    return SongSortSpec(
        column = column,
        ascending = order == 0,
        ignoreCase = ignoreCase,
        numeric = numeric,
        transientColumn = transient
    )
}

internal data class SongSortSpec(
    val column: String,
    val ascending: Boolean,
    val ignoreCase: Boolean,
    val numeric: Boolean,
    val transientColumn: Boolean
) {
    val mediaStoreOrder: String
        get() {
            val caseClause = if (ignoreCase && !numeric) " COLLATE NOCASE" else ""
            val direction = if (ascending) " ASC" else " DESC"
            return column + caseClause + direction
        }

    fun comparator(): Comparator<MutableMap<String, Any?>> = Comparator { first, second ->
        val primary = compareValues(first[column], second[column])
        if (primary != 0) {
            if (ascending) primary else -primary
        } else {
            compareText(first["_uri"], second["_uri"], false)
        }
    }

    private fun compareValues(first: Any?, second: Any?): Int {
        if (first == null && second == null) return 0
        if (first == null) return -1
        if (second == null) return 1

        return if (numeric) {
            (first as Number).toLong().compareTo((second as Number).toLong())
        } else {
            compareText(first, second, ignoreCase)
        }
    }

    private fun compareText(first: Any?, second: Any?, ignoreCase: Boolean): Int {
        return first.toString().compareTo(second.toString(), ignoreCase = ignoreCase)
    }
}
