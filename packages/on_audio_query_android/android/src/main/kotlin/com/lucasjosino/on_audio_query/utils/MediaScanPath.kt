package com.lucasjosino.on_audio_query.utils

import java.io.File

internal fun isValidMediaFilePath(path: String?): Boolean {
    if (path.isNullOrBlank()) return false
    if (path.endsWith('/') || path.endsWith('\\')) return false

    val file = File(path)
    if (file.name.isBlank()) return false
    if (file.exists() && file.isDirectory) return false

    return true
}
