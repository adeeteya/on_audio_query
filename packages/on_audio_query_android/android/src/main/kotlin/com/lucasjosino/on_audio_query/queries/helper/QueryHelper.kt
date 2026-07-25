package com.lucasjosino.on_audio_query.queries.helper

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.lucasjosino.on_audio_query.utils.parseDisplayName

class QueryHelper {
    //This method will load some extra information about audio/song
    fun loadSongExtraInfo(
        uri: Uri,
        songData: MutableMap<String, Any?>
    ): MutableMap<String, Any?> {
        val displayName = parseDisplayName(songData["_display_name"] as? String)

        songData["_display_name_wo_ext"] = displayName.nameWithoutExtension
        songData["file_extension"] = displayName.extension

        //A different type of "data"
        val itemUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val volumeName = songData[MediaStore.MediaColumns.VOLUME_NAME] as? String
            if (volumeName == null) uri else MediaStore.Audio.Media.getContentUri(volumeName)
        } else {
            uri
        }
        val tempUri = ContentUris.withAppendedId(
            itemUri,
            (songData["_id"] as Number).toLong()
        )
        songData["_uri"] = tempUri.toString()

        return songData
    }

    //This method will separate [String] from [Int]
    fun loadSongItem(itemProperty: String, cursor: Cursor): Any? {
        return when {
            isLongSongColumn(itemProperty) -> {
                val index = cursor.getColumnIndexOrThrow(itemProperty)
                if (cursor.isNull(index)) null else cursor.getLong(index)
            }
            // Boolean
            itemProperty in booleanSongColumns -> {
                val value = cursor.getString(cursor.getColumnIndex(itemProperty))
                if (value == "0") return false
                return true
            }
            // String
            else -> cursor.getString(cursor.getColumnIndex(itemProperty))
        }
    }

    //This method will separate [String] from [Int]
    fun loadAlbumItem(itemProperty: String, cursor: Cursor): Any? {
        return when (itemProperty) {
            "_id",
            "artist_id" -> cursor.getLong(cursor.getColumnIndex(itemProperty))
            "numsongs" -> cursor.getInt(cursor.getColumnIndex(itemProperty))
            else -> cursor.getString(cursor.getColumnIndex(itemProperty))
        }
    }

    //This method will separate [String] from [Int]
    fun loadPlaylistItem(itemProperty: String, cursor: Cursor): Any? {
        return when (itemProperty) {
            "_id",
            "date_added",
            "date_modified" -> cursor.getLong(cursor.getColumnIndex(itemProperty))
            else -> cursor.getString(cursor.getColumnIndex(itemProperty))
        }
    }

    //This method will separate [String] from [Int]
    fun loadArtistItem(itemProperty: String, cursor: Cursor): Any? {
        return when (itemProperty) {
            "_id" -> cursor.getLong(cursor.getColumnIndex(itemProperty))
            "number_of_albums",
            "number_of_tracks" -> cursor.getInt(cursor.getColumnIndex(itemProperty))
            else -> cursor.getString(cursor.getColumnIndex(itemProperty))
        }
    }

    //This method will separate [String] from [Int]
    fun loadGenreItem(itemProperty: String, cursor: Cursor): Any? {
        return when (itemProperty) {
            "_id" -> cursor.getLong(cursor.getColumnIndex(itemProperty))
            else -> cursor.getString(cursor.getColumnIndex(itemProperty))
        }
    }

    fun getMediaCount(type: Int, arg: String, resolver: ContentResolver): Int {
        val uri: Uri = if (type == 0) {
            MediaStore.Audio.Genres.Members.getContentUri("external", arg.toLong())
        } else {
            MediaStore.Audio.Playlists.Members.getContentUri("external", arg.toLong())
        }
        val cursor = resolver.query(uri, null, null, null, null)
        val count = cursor?.count ?: -1
        cursor?.close()
        return count
    }

    @Suppress("DEPRECATION")
    fun loadFirstAudioUri(type: Int, id: Number, resolver: ContentResolver): Uri? {
        if (type == 0) {
            return ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                id.toLong()
            )
        }

        val queryUri: Uri
        val idColumn: String
        val selection: String?
        val selectionArgs: Array<String>?

        when (type) {
            1 -> {
                queryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                idColumn = MediaStore.Audio.Media._ID
                selection = "${MediaStore.Audio.Media.ALBUM_ID}=?"
                selectionArgs = arrayOf(id.toString())
            }
            2 -> {
                queryUri = MediaStore.Audio.Playlists.Members.getContentUri(
                    "external",
                    id.toLong()
                )
                idColumn = MediaStore.Audio.Playlists.Members.AUDIO_ID
                selection = null
                selectionArgs = null
            }
            3 -> {
                queryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                idColumn = MediaStore.Audio.Media._ID
                selection = "${MediaStore.Audio.Media.ARTIST_ID}=?"
                selectionArgs = arrayOf(id.toString())
            }
            4 -> {
                queryUri = MediaStore.Audio.Genres.Members.getContentUri(
                    "external",
                    id.toLong()
                )
                idColumn = MediaStore.Audio.Genres.Members.AUDIO_ID
                selection = null
                selectionArgs = null
            }
            else -> return null
        }

        resolver.query(
            queryUri,
            arrayOf(idColumn),
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val audioId = cursor.getLong(cursor.getColumnIndexOrThrow(idColumn))
            return ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                audioId
            )
        }

        return null
    }

    fun chooseWithFilterType(uri: Uri, itemProperty: String, cursor: Cursor): Any? {
        return when (uri) {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI -> loadSongItem(itemProperty, cursor)
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI -> loadAlbumItem(itemProperty, cursor)
            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI -> loadPlaylistItem(
                itemProperty,
                cursor
            )
            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI -> loadArtistItem(itemProperty, cursor)
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI -> loadGenreItem(itemProperty, cursor)
            else -> null
        }
    }
}

private val longSongColumns = setOf(
    "_id",
    "_size",
    "album_id",
    "artist_id",
    "bookmark",
    "date_added",
    "date_modified",
    "duration",
    "track",
    "year",
    "generation_added",
    "generation_modified"
)

private val booleanSongColumns = setOf(
    "is_alarm",
    "is_audiobook",
    "is_music",
    "is_notification",
    "is_podcast",
    "is_ringtone"
)

internal fun isLongSongColumn(column: String): Boolean = column in longSongColumns
