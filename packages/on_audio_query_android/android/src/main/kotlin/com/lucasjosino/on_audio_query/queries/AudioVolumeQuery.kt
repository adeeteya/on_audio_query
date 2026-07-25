package com.lucasjosino.on_audio_query.queries

import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucasjosino.on_audio_query.PluginProvider
import io.flutter.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioVolumeQuery : ViewModel() {

    companion object {
        private const val TAG = "AudioVolumeQuery"
    }

    fun queryAudioVolumes() {
        val context = PluginProvider.context()
        val result = PluginProvider.result()
        viewModelScope.launch {
            val volumes = withContext(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    loadModernVolumes(context)
                } else {
                    loadLegacyVolume(context)
                }
            }
            result.success(volumes)
        }
    }

    private fun loadModernVolumes(
        context: android.content.Context
    ): List<Map<String, Any?>> {
        val current = try {
            MediaStore.getExternalVolumeNames(context)
        } catch (error: Exception) {
            Log.w(TAG, "Unable to read mounted MediaStore volumes", error)
            emptySet()
        }
        val recent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                MediaStore.getRecentExternalVolumeNames(context)
            } catch (error: Exception) {
                Log.w(TAG, "Unable to read recent MediaStore volumes", error)
                emptySet()
            }
        } else {
            emptySet()
        }

        return (current + recent).sorted().map { name ->
            if (name !in current) {
                return@map volumeMap(name, null, null, false)
            }
            var version: String? = null
            var generation: Long? = null
            try {
                version = MediaStore.getVersion(context, name)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    generation = MediaStore.getGeneration(context, name)
                }
            } catch (error: Exception) {
                Log.w(TAG, "Unable to read MediaStore state for $name", error)
            }
            volumeMap(name, version, generation, true)
        }
    }

    @Suppress("DEPRECATION")
    private fun loadLegacyVolume(
        context: android.content.Context
    ): List<Map<String, Any?>> {
        val state = Environment.getExternalStorageState()
        val mounted = state == Environment.MEDIA_MOUNTED ||
            state == Environment.MEDIA_MOUNTED_READ_ONLY
        val version = try {
            MediaStore.getVersion(context)
        } catch (error: Exception) {
            Log.w(TAG, "Unable to read legacy MediaStore version", error)
            null
        }
        return listOf(volumeMap("external", version, null, mounted))
    }

    private fun volumeMap(
        name: String,
        version: String?,
        generation: Long?,
        mounted: Boolean
    ): Map<String, Any?> = mapOf(
        "name" to name,
        "version" to version,
        "generation" to generation,
        "currentlyMounted" to mounted
    )
}
