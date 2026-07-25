package com.lucasjosino.on_audio_query.queries

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucasjosino.on_audio_query.PluginProvider
import com.lucasjosino.on_audio_query.queries.helper.QueryHelper
import com.lucasjosino.on_audio_query.types.checkAudiosUriType
import com.lucasjosino.on_audio_query.types.sorttypes.SongSortSpec
import com.lucasjosino.on_audio_query.types.sorttypes.resolveSongSortSpec
import com.lucasjosino.on_audio_query.utils.songProjection
import io.flutter.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** MediaStore audio queries. */
class AudioQuery : ViewModel() {

    companion object {
        private const val TAG = "OnAudiosQuery"
    }

    private val helper = QueryHelper()

    fun querySongs(withOptions: Boolean = false) {
        val call = PluginProvider.call()
        val result = PluginProvider.result()
        val context = PluginProvider.context()
        val resolver = context.contentResolver
        val sortSpec = resolveSongSortSpec(
            call.argument("sortType"),
            call.argument<Int>("orderType")!!,
            call.argument<Boolean>("ignoreCase")!!
        )
        val options = if (withOptions) {
            NativeAudioQueryOptions.fromMap(call.argument("options"))
        } else {
            null
        }
        if (!validateOptions(options, result)) return

        val audioSelection = buildAudioSelection(
            options,
            call.argument("path")
        )
        val projection = songProjection(includeTitleKey = sortSpec.transientColumn)
        val queryUris = resolveAudioUris(context, call.argument<Int>("uri")!!)

        viewModelScope.launch {
            val queryResult = loadAllSongs(
                resolver,
                queryUris,
                projection,
                audioSelection,
                sortSpec
            )
            if (queryResult.successfulVolumes == 0) {
                result.error(
                    "AudioQueryFailed",
                    "Failed to query every requested MediaStore volume",
                    "Failed volumes: ${queryResult.failedVolumes}"
                )
                return@launch
            }
            result.success(queryResult.songs)
        }
    }

    fun querySongsPage() {
        val call = PluginProvider.call()
        val result = PluginProvider.result()
        val context = PluginProvider.context()
        val resolver = context.contentResolver
        val limit = call.argument<Int>("limit") ?: 500
        val offset = call.argument<Int>("offset") ?: 0
        if (limit <= 0 || offset < 0) {
            result.error(
                "InvalidPagingArguments",
                "limit must be positive and offset must not be negative",
                null
            )
            return
        }

        val options = NativeAudioQueryOptions.fromMap(call.argument("options"))
        if (!validateOptions(options, result)) return
        val sortSpec = resolveSongSortSpec(
            call.argument("sortType"),
            call.argument<Int>("orderType")!!,
            call.argument<Boolean>("ignoreCase")!!
        )
        val audioSelection = buildAudioSelection(options, null)
        val projection = songProjection(includeTitleKey = sortSpec.transientColumn)
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        viewModelScope.launch {
            try {
                val page = loadSongPage(
                    resolver,
                    uri,
                    projection,
                    audioSelection,
                    sortSpec,
                    limit,
                    offset
                )
                result.success(page)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.w(TAG, "Failed to load a MediaStore song page", error)
                result.error("AudioPageQueryFailed", error.message, null)
            }
        }
    }

    private fun validateOptions(
        options: NativeAudioQueryOptions?,
        result: io.flutter.plugin.common.MethodChannel.Result
    ): Boolean {
        if (options?.generationModifiedAfter != null &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R
        ) {
            result.error(
                "UnsupportedAudioQueryOption",
                "generationModifiedAfter requires Android 11 or newer",
                null
            )
            return false
        }
        if ((options?.minimumDuration ?: 0) < 0 ||
            (options?.modifiedAfter ?: 0) < 0 ||
            (options?.generationModifiedAfter ?: 0) < 0
        ) {
            result.error(
                "InvalidAudioQueryOption",
                "Duration and synchronization thresholds must not be negative",
                null
            )
            return false
        }
        return true
    }

    private suspend fun loadAllSongs(
        resolver: ContentResolver,
        uris: List<Uri>,
        projection: Array<String>,
        selection: AudioSelection,
        sortSpec: SongSortSpec
    ): AudioQueryResult = withContext(Dispatchers.IO) {
        val songs = arrayListOf<MutableMap<String, Any?>>()
        var successfulVolumes = 0
        var failedVolumes = 0

        for (targetUri in uris) {
            try {
                val cursor = resolver.query(
                    targetUri,
                    projection,
                    selection.selection,
                    selection.arguments,
                    sortSpec.mediaStoreOrder
                )
                if (cursor == null) {
                    failedVolumes++
                    continue
                }
                cursor.use {
                    while (it.moveToNext()) {
                        songs += loadSong(it, targetUri)
                    }
                }
                successfulVolumes++
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                failedVolumes++
                Log.w(TAG, "Failed to query MediaStore volume $targetUri", error)
            }
        }

        songs.sortWith(sortSpec.comparator())
        if (sortSpec.transientColumn) {
            songs.forEach { it.remove(sortSpec.column) }
        }
        AudioQueryResult(songs, successfulVolumes, failedVolumes)
    }

    private suspend fun loadSongPage(
        resolver: ContentResolver,
        uri: Uri,
        projection: Array<String>,
        selection: AudioSelection,
        sortSpec: SongSortSpec,
        limit: Int,
        offset: Int
    ): Map<String, Any?> = withContext(Dispatchers.IO) {
        val totalCount = resolver.query(
            uri,
            arrayOf(MediaStore.Audio.Media._ID),
            selection.selection,
            selection.arguments,
            null
        )?.use { it.count } ?: 0

        val songs = arrayListOf<MutableMap<String, Any?>>()
        val stableSort = stableSortOrder(sortSpec)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val args = Bundle().apply {
                selection.selection?.let {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, it)
                }
                selection.arguments?.let {
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, it)
                }
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, stableSort)
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            }
            resolver.query(uri, projection, args, null)?.use { cursor ->
                while (cursor.moveToNext() && songs.size < limit) {
                    songs += loadSong(cursor, uri)
                }
            }
        } else {
            resolver.query(
                uri,
                projection,
                selection.selection,
                selection.arguments,
                stableSort
            )?.use { cursor ->
                if (offset == 0 || cursor.moveToPosition(offset - 1)) {
                    while (cursor.moveToNext() && songs.size < limit) {
                        songs += loadSong(cursor, uri)
                    }
                }
            }
        }

        if (sortSpec.transientColumn) {
            songs.forEach { it.remove(sortSpec.column) }
        }
        mapOf(
            "songs" to songs,
            "nextOffset" to nextPageOffset(offset, songs.size, totalCount),
            "totalCount" to totalCount
        )
    }

    private fun loadSong(
        cursor: android.database.Cursor,
        queryUri: Uri
    ): MutableMap<String, Any?> {
        val song = hashMapOf<String, Any?>()
        for (column in cursor.columnNames) {
            song[column] = helper.loadSongItem(column, cursor)
        }
        song.putAll(helper.loadSongExtraInfo(queryUri, song))
        return song
    }

    private fun stableSortOrder(sortSpec: SongSortSpec): String {
        val tieBreakers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.MediaColumns.VOLUME_NAME} ASC, ${MediaStore.Audio.Media._ID} ASC"
        } else {
            "${MediaStore.Audio.Media._ID} ASC"
        }
        return "${sortSpec.mediaStoreOrder}, $tieBreakers"
    }

    private fun resolveAudioUris(context: Context, uriType: Int): List<Uri> {
        if (uriType != 0) return listOf(checkAudiosUriType(uriType))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val volumes = MediaStore.getExternalVolumeNames(context)
            if (volumes.isNotEmpty()) {
                return volumes.map(MediaStore.Audio.Media::getContentUri)
            }
        }
        return listOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
    }

    private data class AudioQueryResult(
        val songs: ArrayList<MutableMap<String, Any?>>,
        val successfulVolumes: Int,
        val failedVolumes: Int
    )
}

internal fun nextPageOffset(offset: Int, pageSize: Int, totalCount: Int): Int? {
    val consumed = offset + pageSize
    return if (consumed < totalCount) consumed else null
}
