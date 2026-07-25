package com.lucasjosino.on_audio_query.utils

internal data class DisplayNameMetadata(
    val nameWithoutExtension: String,
    val extension: String
)

internal fun parseDisplayName(displayName: String?): DisplayNameMetadata {
    if (displayName.isNullOrEmpty()) {
        return DisplayNameMetadata("", "")
    }

    val extensionSeparator = displayName.lastIndexOf('.')
    if (extensionSeparator <= 0) {
        return DisplayNameMetadata(displayName, "")
    }

    return DisplayNameMetadata(
        nameWithoutExtension = displayName.substring(0, extensionSeparator),
        extension = displayName.substring(extensionSeparator + 1)
    )
}
