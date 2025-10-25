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
import com.lucasjosino.on_audio_query.types.sorttypes.checkSongSortType
import com.lucasjosino.on_audio_query.utils.songProjection
import io.flutter.Log
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
        sortType = checkSongSortType(
            call.argument<Int>("sortType"),
            call.argument<Int>("orderType")!!,
            call.argument<Boolean>("ignoreCase")!!
        )

        // Reset selection filter every call and re-apply the optional path filter.
        selection = null
        val projection = songProjection()
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
            result.success(queryResult)
        }
    }

    //Loading in Background
    private suspend fun loadSongs(
        uris: List<Uri>,
        projection: Array<String>
    ): ArrayList<MutableMap<String, Any?>> =
        withContext(Dispatchers.IO) {
            val songList: ArrayList<MutableMap<String, Any?>> = ArrayList()

            for (targetUri in uris) {
                val cursor = resolver.query(targetUri, projection, selection, null, sortType)
                Log.d(TAG, "Cursor count for $targetUri: ${cursor?.count}")

                // For each item(song) inside this "cursor", take one and "format"
                // into a 'Map<String, dynamic>'.
                while (cursor != null && cursor.moveToNext()) {
                    val tempData: MutableMap<String, Any?> = HashMap()

                    for (audioMedia in cursor.columnNames) {
                        tempData[audioMedia] = helper.loadSongItem(audioMedia, cursor)
                    }

                    //Get a extra information from audio, e.g: extension, uri, etc..
                    val tempExtraData = helper.loadSongExtraInfo(targetUri, tempData)
                    tempData.putAll(tempExtraData)

                    songList.add(tempData)
                }

                // Close cursor to avoid memory leaks.
                cursor?.close()
            }

            return@withContext songList
        }

    private fun resolveAudioUris(context: Context, uriType: Int): List<Uri> {
        // Internal queries or legacy devices can continue to rely on the single URI path.
        if (uriType != 0) return listOf(checkAudiosUriType(uriType))
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
}
