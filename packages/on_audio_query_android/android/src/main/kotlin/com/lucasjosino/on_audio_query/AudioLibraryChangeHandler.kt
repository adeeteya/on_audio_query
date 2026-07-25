package com.lucasjosino.on_audio_query

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.lucasjosino.on_audio_query.controllers.PermissionController
import io.flutter.Log
import io.flutter.plugin.common.EventChannel

internal class AudioLibraryChangeHandler(
    private val context: Context
) : EventChannel.StreamHandler {

    companion object {
        private const val TAG = "AudioLibraryChanges"
    }

    private val resolver = context.contentResolver
    private val observers = mutableMapOf<String, ContentObserver>()
    private var receiverRegistered = false
    private var eventSink: EventChannel.EventSink? = null

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                refreshObservers()
            } catch (error: Exception) {
                Log.w(TAG, "Unable to refresh observers after a volume change", error)
            }
            emit(null, null, "unknown")
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
        dispose()
        val missingPermission = PermissionController.requiredPermissionsForSdk(
            Build.VERSION.SDK_INT
        ).any {
            ContextCompat.checkSelfPermission(context, it) !=
                PackageManager.PERMISSION_GRANTED
        }
        if (missingPermission) {
            events.error(
                "MissingPermissions",
                "Application doesn't have access to the audio library",
                null
            )
            return
        }

        eventSink = events
        try {
            refreshObservers()
            registerVolumeReceiver()
        } catch (error: Exception) {
            Log.w(TAG, "Unable to observe MediaStore audio changes", error)
            events.error("AudioChangeObserverFailed", error.message, null)
            dispose()
        }
    }

    override fun onCancel(arguments: Any?) {
        dispose()
    }

    fun dispose() {
        observers.values.forEach {
            try {
                resolver.unregisterContentObserver(it)
            } catch (_: Exception) {
                // The observer may already have been removed by the framework.
            }
        }
        observers.clear()
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(volumeReceiver)
            } catch (_: Exception) {
                // The receiver may already have been removed by the framework.
            }
            receiverRegistered = false
        }
        eventSink = null
    }

    private fun refreshObservers() {
        observers.values.forEach {
            try {
                resolver.unregisterContentObserver(it)
            } catch (_: Exception) {
                // Continue rebuilding the observer set.
            }
        }
        observers.clear()

        val targets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.getExternalVolumeNames(context).associateWith {
                MediaStore.Audio.Media.getContentUri(it)
            }
        } else {
            mapOf("external" to MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        }
        targets.forEach { (volumeName, uri) ->
            val observer = createObserver(volumeName)
            resolver.registerContentObserver(uri, true, observer)
            observers[volumeName] = observer
        }
    }

    private fun createObserver(observerVolume: String): ContentObserver {
        return object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                emit(resolveVolumeName(uri, observerVolume), uri, "unknown")
            }

            override fun onChange(selfChange: Boolean, uri: Uri?, flags: Int) {
                emit(
                    resolveVolumeName(uri, observerVolume),
                    uri,
                    audioChangeTypeForFlags(flags)
                )
            }
        }
    }

    private fun resolveVolumeName(uri: Uri?, fallback: String): String {
        if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val volumeName = MediaStore.getVolumeName(uri)
                if (volumeName != MediaStore.VOLUME_EXTERNAL) {
                    return volumeName
                }
            } catch (_: Exception) {
                // Some provider notifications use a non-MediaStore child URI.
            }
        }
        return fallback
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerVolumeReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addDataScheme("file")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                volumeReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(volumeReceiver, filter)
        }
        receiverRegistered = true
    }

    private fun emit(volumeName: String?, uri: Uri?, type: String) {
        eventSink?.success(
            mapOf(
                "volumeName" to volumeName,
                "contentUri" to uri?.toString(),
                "type" to type
            )
        )
    }
}

@SuppressLint("InlinedApi")
internal fun audioChangeTypeForFlags(
    flags: Int,
    sdkInt: Int = Build.VERSION.SDK_INT
): String {
    if (sdkInt < Build.VERSION_CODES.R) return "unknown"
    val known = listOf(
        ContentResolver.NOTIFY_INSERT to "insert",
        ContentResolver.NOTIFY_UPDATE to "update",
        ContentResolver.NOTIFY_DELETE to "delete"
    ).filter { (flag, _) -> flags and flag != 0 }
    return if (known.size == 1) known.single().second else "unknown"
}
