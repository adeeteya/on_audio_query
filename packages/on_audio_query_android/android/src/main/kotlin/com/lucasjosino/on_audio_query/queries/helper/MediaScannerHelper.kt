package com.lucasjosino.on_audio_query.queries.helper

import android.content.Context
import android.media.MediaScannerConnection
import android.util.Log
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

object MediaScannerHelper {
    private const val TAG = "MediaScannerHelper"

    private val scannedRoots = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun ensureVolumesIndexed(context: Context, volumeRoots: Set<String>) {
        if (volumeRoots.isEmpty()) return

        val pendingScan = volumeRoots.filter { root ->
            root.isNotBlank() && scannedRoots.add(root)
        }

        if (pendingScan.isEmpty()) return

        try {
            MediaScannerConnection.scanFile(
                context,
                pendingScan.toTypedArray(),
                Array(pendingScan.size) { "audio/*" },
                null
            )
        } catch (error: Exception) {
            Log.w(TAG, "Failed to trigger media scan", error)
        }
    }
}
