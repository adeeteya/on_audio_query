package com.lucasjosino.on_audio_query.queries

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucasjosino.on_audio_query.PluginProvider
import com.lucasjosino.on_audio_query.queries.helper.QueryHelper
import com.lucasjosino.on_audio_query.types.checkArtworkFormat
import com.lucasjosino.on_audio_query.types.checkArtworkType
import io.flutter.Log
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

class ArtworkQuery : ViewModel() {

    companion object {
        private const val TAG = "OnArtworksQuery"
    }

    private val helper = QueryHelper()
    private var type: Int = -1
    private var id: Number = 0
    private var quality: Int = 50
    private var size: Int = 200
    private var showDetailedLog: Boolean = false

    private lateinit var context: Context
    private lateinit var uri: Uri
    private lateinit var resolver: ContentResolver
    private lateinit var format: Bitmap.CompressFormat

    /**
     * Queries artwork using a legacy MediaStore ID.
     */
    fun queryArtwork() {
        val call = PluginProvider.call()
        val result = PluginProvider.result()
        configure(call)

        id = call.argument<Number>("id")!!
        type = call.argument<Int>("type")!!
        uri = checkArtworkType(type)

        Log.d(TAG, "Query config: ")
        Log.d(TAG, "\tid: $id")
        Log.d(TAG, "\tquality: $quality")
        Log.d(TAG, "\tformat: $format")
        Log.d(TAG, "\turi: $uri")
        Log.d(TAG, "\ttype: $type")

        viewModelScope.launch {
            val contentUri = withContext(Dispatchers.IO) {
                resolveLegacyArtworkUri()
            }
            complete(result, contentUri, id.toString())
        }
    }

    /**
     * Queries artwork using the complete, volume-specific MediaStore URI.
     */
    fun queryArtworkByUri() {
        val call = PluginProvider.call()
        val result = PluginProvider.result()
        configure(call)

        val contentUriValue = call.argument<String>("uri")
        val contentUri = contentUriValue?.let(Uri::parse)
        if (contentUriValue.isNullOrBlank() ||
            contentUri?.scheme != ContentResolver.SCHEME_CONTENT ||
            contentUri.authority.isNullOrBlank()
        ) {
            result.error(
                "InvalidArtworkUri",
                "Artwork URI must be a valid content URI",
                contentUriValue
            )
            return
        }

        Log.d(TAG, "URI artwork config: ")
        Log.d(TAG, "\turi: $contentUri")
        Log.d(TAG, "\tquality: $quality")
        Log.d(TAG, "\tformat: $format")
        Log.d(TAG, "\tsize: $size")

        viewModelScope.launch {
            complete(result, contentUri, contentUriValue)
        }
    }

    private fun configure(call: MethodCall) {
        context = PluginProvider.context()
        resolver = context.contentResolver
        showDetailedLog = PluginProvider.showDetailedLog
        size = call.argument<Int>("size")?.takeIf { it > 0 } ?: 200
        quality = call.argument<Int>("quality")?.takeIf { it in 0..100 } ?: 50
        format = checkArtworkFormat(call.argument<Int>("format")!!)
    }

    private suspend fun complete(
        result: MethodChannel.Result,
        contentUri: Uri?,
        artworkDescription: String
    ) {
        var artwork = contentUri?.let { loadArtwork(it, artworkDescription) }
        if (artwork != null && artwork.isEmpty()) {
            Log.i(TAG, "Artwork for '$artworkDescription' is empty. Returning null")
            artwork = null
        }
        result.success(artwork)
    }

    private fun resolveLegacyArtworkUri(): Uri? {
        return if (type == 0 || (Build.VERSION.SDK_INT >= 29 && type == 1)) {
            ContentUris.withAppendedId(uri, id.toLong())
        } else {
            helper.loadFirstAudioUri(type, id, resolver)
        }
    }

    private suspend fun loadArtwork(
        contentUri: Uri,
        artworkDescription: String
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                loadThumbnail(contentUri)
            } else {
                loadEmbeddedArtwork(contentUri)
            }
        } catch (error: Exception) {
            if (showDetailedLog) {
                Log.w(TAG, "($artworkDescription) Message: $error")
            }
            null
        }
    }

    private fun loadThumbnail(contentUri: Uri): ByteArray? {
        val bitmap = resolver.loadThumbnail(contentUri, Size(size, size), null)
        return try {
            compress(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private fun loadEmbeddedArtwork(contentUri: Uri): ByteArray? {
        val retriever = MediaMetadataRetriever()
        val embeddedPicture = try {
            retriever.setDataSource(context, contentUri)
            retriever.embeddedPicture
        } finally {
            retriever.release()
        } ?: return null

        val bitmap = decodeSampledBitmap(embeddedPicture, size) ?: return null
        return try {
            compress(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeSampledBitmap(bytes: ByteArray, requestedSize: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                bounds.outWidth,
                bounds.outHeight,
                requestedSize
            )
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: return null

        val largestDimension = max(decoded.width, decoded.height)
        if (largestDimension <= requestedSize) return decoded

        val scale = requestedSize.toFloat() / largestDimension
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            max(1, (decoded.width * scale).roundToInt()),
            max(1, (decoded.height * scale).roundToInt()),
            true
        )
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    private fun compress(bitmap: Bitmap): ByteArray? {
        return ByteArrayOutputStream().use { output ->
            if (!bitmap.compress(format, quality, output)) return@use null
            output.toByteArray()
        }
    }
}

internal fun calculateInSampleSize(
    width: Int,
    height: Int,
    requestedSize: Int
): Int {
    if (width <= 0 || height <= 0 || requestedSize <= 0) return 1

    var sampleSize = 1
    while (max(width / (sampleSize * 2), height / (sampleSize * 2)) >= requestedSize) {
        sampleSize *= 2
    }
    return sampleSize
}
