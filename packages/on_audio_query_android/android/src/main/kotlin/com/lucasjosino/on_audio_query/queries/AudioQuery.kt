package com.lucasjosino.on_audio_query.queries

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucasjosino.on_audio_query.PluginProvider
import com.lucasjosino.on_audio_query.queries.helper.MediaScannerHelper
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
        val uriConfig = resolveAudioUris(context, uriType)

        Log.d(TAG, "Query config: ")
        Log.d(TAG, "\tsortType: $sortType")
        Log.d(TAG, "\tselection: $selection")
        Log.d(TAG, "\turi(s): ${uriConfig.uris.joinToString()}")

        // Query everything in background for a better performance.
        viewModelScope.launch {
            val queryResult = loadSongs(context, uriConfig, projection)
            result.success(queryResult)
        }
    }

    //Loading in Background
    private suspend fun loadSongs(
        context: Context,
        config: AudioUriConfig,
        projection: Array<String>
    ): ArrayList<MutableMap<String, Any?>> =
        withContext(Dispatchers.IO) {
            MediaScannerHelper.ensureVolumesIndexed(context, config.volumeRoots)

            val songList: ArrayList<MutableMap<String, Any?>> = ArrayList()

            for (targetUri in config.uris) {
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

    private fun resolveAudioUris(context: Context, uriType: Int): AudioUriConfig {
        if (uriType != 0) {
            return AudioUriConfig(
                uris = listOf(checkAudiosUriType(uriType)),
                volumeRoots = emptySet()
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val volumeNames = MediaStore.getExternalVolumeNames(context)
            if (volumeNames.isNotEmpty()) {
                val uris = volumeNames.map { volume ->
                    MediaStore.Audio.Media.getContentUri(volume)
                }
                val roots = resolveVolumeRoots(context)
                return AudioUriConfig(uris, roots)
            }
        }

        val legacyRoot = Environment.getExternalStorageDirectory().absolutePath
        return AudioUriConfig(
            uris = listOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
            volumeRoots = setOf(legacyRoot)
        )
    }

    private fun resolveVolumeRoots(context: Context): Set<String> {
        val roots = mutableSetOf<String>()
        context.getExternalFilesDirs(null)?.forEach { dir ->
            if (dir == null) return@forEach
            val path = dir.absolutePath
            val androidIndex = path.indexOf("/Android/")
            if (androidIndex > 0) {
                roots.add(path.substring(0, androidIndex))
            } else {
                roots.add(path)
            }
        }
        return roots
    }

    private data class AudioUriConfig(
        val uris: List<Uri>,
        val volumeRoots: Set<String>
    )
}
