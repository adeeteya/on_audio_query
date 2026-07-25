package com.lucasjosino.on_audio_query.queries

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
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

/** OnAudiosQuery */
class AudioQuery : ViewModel() {

    companion object {
        private const val TAG = "OnAudiosQuery"
    }

    // Main parameters
    private val helper = QueryHelper()
    private var selection: String? = null

    private lateinit var sortType: String
    private lateinit var sortSpec: SongSortSpec
    private lateinit var resolver: ContentResolver

    /**
     * Method to "query" all songs.
     */
    fun querySongs() {
        val call = PluginProvider.call()
        val result = PluginProvider.result()
        val context = PluginProvider.context()
        this.resolver = context.contentResolver

        // Sort: Type and Order.
        val requestedSortType = call.argument<Int>("sortType")
        val requestedOrderType = call.argument<Int>("orderType")!!
        val ignoreCase = call.argument<Boolean>("ignoreCase")!!
        sortSpec = resolveSongSortSpec(
            requestedSortType,
            requestedOrderType,
            ignoreCase
        )
        sortType = sortSpec.mediaStoreOrder

        // Reset selection filter every call and re-apply the optional path filter.
        selection = null
        val projection = songProjection(includeTitleKey = sortSpec.transientColumn)
        val pathFilter = call.argument<String>("path")
        if (!pathFilter.isNullOrEmpty()) {
            selection = "${projection[0]} like '%$pathFilter/%'"
        }

        val uriType = call.argument<Int>("uri")!!
        val queryUris = resolveAudioUris(context, uriType)

        Log.d(TAG, "Query config: ")
        Log.d(TAG, "\tsortType: $sortType")
        Log.d(TAG, "\tselection: $selection")
        Log.d(TAG, "\turi(s): ${queryUris.joinToString()}")

        // Query everything in background for a better performance.
        viewModelScope.launch {
            val queryResult = loadSongs(queryUris, projection)
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

    //Loading in Background
    private suspend fun loadSongs(
        uris: List<Uri>,
        projection: Array<String>
    ): AudioQueryResult =
        withContext(Dispatchers.IO) {
            val songList: ArrayList<MutableMap<String, Any?>> = ArrayList()
            var successfulVolumes = 0
            var failedVolumes = 0

            for (targetUri in uris) {
                try {
                    val cursor = resolver.query(
                        targetUri,
                        projection,
                        selection,
                        null,
                        sortType
                    )
                    if (cursor == null) {
                        failedVolumes++
                        Log.w(TAG, "MediaStore returned a null cursor for $targetUri")
                        continue
                    }

                    val volumeSongs: ArrayList<MutableMap<String, Any?>> = ArrayList()
                    cursor.use {
                        Log.d(TAG, "Cursor count for $targetUri: ${it.count}")

                        // For each item(song) inside this "cursor", take one and "format"
                        // into a 'Map<String, dynamic>'.
                        while (it.moveToNext()) {
                            val tempData: MutableMap<String, Any?> = HashMap()

                            for (audioMedia in it.columnNames) {
                                tempData[audioMedia] = helper.loadSongItem(audioMedia, it)
                            }

                            //Get a extra information from audio, e.g: extension, uri, etc..
                            val tempExtraData = helper.loadSongExtraInfo(targetUri, tempData)
                            tempData.putAll(tempExtraData)

                            volumeSongs.add(tempData)
                        }
                    }

                    songList.addAll(volumeSongs)
                    successfulVolumes++
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    failedVolumes++
                    Log.w(TAG, "Failed to query MediaStore volume $targetUri", error)
                }
            }

            songList.sortWith(sortSpec.comparator())
            if (sortSpec.transientColumn) {
                songList.forEach { it.remove(sortSpec.column) }
            }

            return@withContext AudioQueryResult(
                songs = songList,
                successfulVolumes = successfulVolumes,
                failedVolumes = failedVolumes
            )
        }

    private fun resolveAudioUris(context: Context, uriType: Int): List<Uri> {
        if (uriType != 0) {
            return listOf(checkAudiosUriType(uriType))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val volumeNames = MediaStore.getExternalVolumeNames(context)
            if (volumeNames.isNotEmpty()) {
                return volumeNames.map { volume ->
                    MediaStore.Audio.Media.getContentUri(volume)
                }
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
