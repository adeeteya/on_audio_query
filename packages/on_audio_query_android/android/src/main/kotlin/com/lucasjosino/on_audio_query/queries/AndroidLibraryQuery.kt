package com.lucasjosino.on_audio_query.queries

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore

/** A snapshot is returned only if every mounted volume was queried. */
object AndroidLibraryQuery {
    fun query(context: Context): Map<String, Any> {
        val volumes = if (Build.VERSION.SDK_INT >= 29)
            MediaStore.getExternalVolumeNames(context).sorted()
        else listOf("external")
        val songs = arrayListOf<Map<String, Any?>>()
        for (volume in volumes) {
            val uri = if (Build.VERSION.SDK_INT >= 29)
                MediaStore.Audio.Media.getContentUri(volume)
            else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val columns = mutableListOf("_id", "_data", "_size", "date_modified",
                "title", "artist", "album", "duration", "track", "year",
                "mime_type", "_display_name")
            if (Build.VERSION.SDK_INT >= 30) columns.addAll(listOf("album_artist", "genre"))
            val cursor = context.contentResolver.query(uri, columns.toTypedArray(),
                null, null, "_id ASC") ?: error("Cannot query volume $volume")
            cursor.use {
                while (it.moveToNext()) {
                    val values = mutableMapOf<String, Any?>()
                    for (column in columns) {
                        val index = it.getColumnIndex(column)
                        values[column] = if (index < 0 || it.isNull(index)) null
                            else if (column in listOf("_id", "_size", "date_modified",
                                "duration", "track", "year")) it.getLong(index)
                            else it.getString(index)
                    }
                    songs.add(mapOf(
                        "uri" to ContentUris.withAppendedId(uri, values["_id"] as Long).toString(),
                        "volume" to volume, "path" to (values["_data"] as? String)?.takeIf { it.isNotBlank() },
                        "size" to (values["_size"] ?: 0L),
                        "modified" to (values["date_modified"] ?: 0L),
                        "metadata" to values))
                }
            }
        }
        return mapOf("songs" to songs, "volumes" to volumes,
            "sdkVersion" to Build.VERSION.SDK_INT)
    }
}
