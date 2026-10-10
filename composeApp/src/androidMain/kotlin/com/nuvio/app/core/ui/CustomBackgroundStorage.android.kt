package com.nuvio.app.core.ui

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.nuvio.app.core.storage.ProfileScopedKey
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal actual object CustomBackgroundStorage {
    private const val preferencesName = "nuvio_custom_background"
    private const val payloadKey = "custom_background_payload"
    private const val directoryName = "custom_backgrounds"

    private var preferences: SharedPreferences? = null
    private var imageDirectory: File? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        imageDirectory = File(context.filesDir, directoryName)
    }

    actual fun loadPayload(): String? =
        preferences?.getString(ProfileScopedKey.of(payloadKey), null)

    actual fun savePayload(payload: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(payloadKey), payload)
            ?.apply()
    }

    actual fun saveImage(bytes: ByteArray): String? {
        val directory = imageDirectory ?: return null
        return runCatching {
            directory.mkdirs()
            // Unique name per pick so Coil's cache never shows a stale image.
            val file = File(directory, "background_${System.currentTimeMillis()}.jpg")
            file.writeBytes(bytes)
            file.toURI().toString()
        }.getOrNull()
    }

    actual fun imageUrl(fileName: String): String? =
        imageDirectory?.let { File(it, fileName).toURI().toString() }

    actual fun deleteImage(url: String) {
        val directory = imageDirectory ?: return
        runCatching {
            val file = File(Uri.parse(url).path ?: return)
            if (file.parentFile?.canonicalPath == directory.canonicalPath) file.delete()
        }
    }
}

@Composable
internal actual fun rememberBackgroundImagePicker(onPicked: (ByteArray) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
            } ?: return@launch
            currentOnPicked(bytes)
        }
    }
    return {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}
