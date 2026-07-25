package com.lucasjosino.on_audio_query.queries

import android.os.Build
import android.provider.MediaStore

internal data class NativeAudioPathFilter(
    val volumeName: String,
    val relativePathPrefix: String
)

internal data class NativeAudioQueryOptions(
    val isMusic: Boolean? = null,
    val includeAlarms: Boolean = true,
    val includeNotifications: Boolean = true,
    val includeRingtones: Boolean = true,
    val includePodcasts: Boolean = true,
    val includeAudiobooks: Boolean = true,
    val minimumDuration: Long? = null,
    val volumeNames: List<String>? = null,
    val paths: List<NativeAudioPathFilter>? = null,
    val modifiedAfter: Long? = null,
    val generationModifiedAfter: Long? = null
) {
    companion object {
        fun fromMap(map: Map<*, *>?): NativeAudioQueryOptions {
            if (map == null) return NativeAudioQueryOptions()
            val rawPaths = map["paths"] as? List<*>
            return NativeAudioQueryOptions(
                isMusic = map["isMusic"] as? Boolean,
                includeAlarms = map["includeAlarms"] as? Boolean ?: true,
                includeNotifications = map["includeNotifications"] as? Boolean ?: true,
                includeRingtones = map["includeRingtones"] as? Boolean ?: true,
                includePodcasts = map["includePodcasts"] as? Boolean ?: true,
                includeAudiobooks = map["includeAudiobooks"] as? Boolean ?: true,
                minimumDuration = (map["minimumDuration"] as? Number)?.toLong(),
                volumeNames = (map["volumeNames"] as? List<*>)
                    ?.mapNotNull { it as? String },
                paths = rawPaths?.mapNotNull { raw ->
                    val path = raw as? Map<*, *> ?: return@mapNotNull null
                    val volumeName = path["volumeName"] as? String ?: return@mapNotNull null
                    val prefix = path["relativePathPrefix"] as? String
                        ?: return@mapNotNull null
                    NativeAudioPathFilter(volumeName, prefix)
                },
                modifiedAfter = (map["modifiedAfter"] as? Number)?.toLong(),
                generationModifiedAfter =
                (map["generationModifiedAfter"] as? Number)?.toLong()
            )
        }
    }
}

internal data class AudioSelection(
    val selection: String?,
    val arguments: Array<String>?
)

@Suppress("DEPRECATION")
internal fun buildAudioSelection(
    options: NativeAudioQueryOptions?,
    legacyPath: String?,
    sdkInt: Int = Build.VERSION.SDK_INT
): AudioSelection {
    val clauses = mutableListOf<String>()
    val arguments = mutableListOf<String>()

    if (!legacyPath.isNullOrEmpty()) {
        clauses += "${MediaStore.Audio.Media.DATA} LIKE ? ESCAPE '\\'"
        arguments += "%${escapeLikeValue(legacyPath)}/%"
    }

    if (options == null) {
        return AudioSelection(
            clauses.takeIf { it.isNotEmpty() }?.joinToString(" AND "),
            arguments.takeIf { it.isNotEmpty() }?.toTypedArray()
        )
    }

    options.isMusic?.let {
        clauses += "${MediaStore.Audio.Media.IS_MUSIC} = ?"
        arguments += if (it) "1" else "0"
    }
    addExcludedFlag(
        clauses,
        arguments,
        MediaStore.Audio.Media.IS_ALARM,
        options.includeAlarms
    )
    addExcludedFlag(
        clauses,
        arguments,
        MediaStore.Audio.Media.IS_NOTIFICATION,
        options.includeNotifications
    )
    addExcludedFlag(
        clauses,
        arguments,
        MediaStore.Audio.Media.IS_RINGTONE,
        options.includeRingtones
    )
    addExcludedFlag(
        clauses,
        arguments,
        MediaStore.Audio.Media.IS_PODCAST,
        options.includePodcasts
    )
    if (sdkInt >= Build.VERSION_CODES.Q) {
        addExcludedFlag(
            clauses,
            arguments,
            MediaStore.Audio.Media.IS_AUDIOBOOK,
            options.includeAudiobooks
        )
    }

    options.minimumDuration?.let {
        clauses += "${MediaStore.Audio.Media.DURATION} >= ?"
        arguments += it.toString()
    }
    options.modifiedAfter?.let {
        clauses += "${MediaStore.Audio.Media.DATE_MODIFIED} > ?"
        arguments += it.toString()
    }
    options.generationModifiedAfter?.let {
        clauses += "${MediaStore.MediaColumns.GENERATION_MODIFIED} > ?"
        arguments += it.toString()
    }

    options.volumeNames?.let { volumes ->
        if (volumes.isEmpty()) {
            clauses += "0"
        } else if (sdkInt >= Build.VERSION_CODES.Q) {
            clauses += "${MediaStore.MediaColumns.VOLUME_NAME} IN (${placeholders(volumes.size)})"
            arguments += volumes
        } else if (volumes.none { it == "external" || it == "external_primary" }) {
            clauses += "0"
        }
    }

    options.paths?.let { paths ->
        if (paths.isEmpty()) {
            clauses += "0"
        } else {
            val pathClauses = paths.map { path ->
                if (sdkInt >= Build.VERSION_CODES.Q) {
                    arguments += path.volumeName
                    arguments += "${escapeLikeValue(path.relativePathPrefix)}%"
                    "(${MediaStore.MediaColumns.VOLUME_NAME} = ? AND " +
                        "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? ESCAPE '\\')"
                } else {
                    val prefix = path.relativePathPrefix.trimStart('/')
                    arguments += "%/${escapeLikeValue(prefix)}%"
                    "(${MediaStore.Audio.Media.DATA} LIKE ? ESCAPE '\\')"
                }
            }
            clauses += "(${pathClauses.joinToString(" OR ")})"
        }
    }

    return AudioSelection(
        clauses.takeIf { it.isNotEmpty() }?.joinToString(" AND "),
        arguments.takeIf { it.isNotEmpty() }?.toTypedArray()
    )
}

internal fun escapeLikeValue(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
}

private fun placeholders(count: Int): String = List(count) { "?" }.joinToString(",")

private fun addExcludedFlag(
    clauses: MutableList<String>,
    arguments: MutableList<String>,
    column: String,
    included: Boolean
) {
    if (!included) {
        clauses += "$column = ?"
        arguments += "0"
    }
}
