package com.nuvio.app.core.ui

import androidx.compose.runtime.Composable

internal expect object CustomBackgroundStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)

    /** Writes the picked photo to app storage and returns a `file://` URL Coil can load, or null on failure. */
    fun saveImage(bytes: ByteArray): String?
    fun deleteImage(url: String)
}

/** Returns a launcher that opens the system photo picker and hands back the chosen image's bytes. */
@Composable
internal expect fun rememberBackgroundImagePicker(onPicked: (ByteArray) -> Unit): () -> Unit
