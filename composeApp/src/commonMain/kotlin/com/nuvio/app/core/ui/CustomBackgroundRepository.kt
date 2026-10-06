package com.nuvio.app.core.ui

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal const val DefaultCustomBackgroundDim = 45
internal const val DefaultCustomBackgroundBlur = 0
internal const val DefaultCustomBackgroundCardOpacity = 85
internal const val MaxCustomBackgroundBlur = 40

enum class CustomBackgroundMode {
    Off,
    SolidColor,
    Gradient,
    Image,
}

/** Preset solid colors offered in Settings → Appearance → Background. */
val CustomBackgroundColorPresets: List<Color> = listOf(
    Color(0xFF0B1426),
    Color(0xFF1B0F2E),
    Color(0xFF0F2A1E),
    Color(0xFF2B0D12),
    Color(0xFF2A1C08),
    Color(0xFF14181F),
    Color(0xFF3A1F3D),
    Color(0xFF0E2F35),
)

/** Preset gradients (top → bottom) offered in Settings → Appearance → Background. */
val CustomBackgroundGradientPresets: List<List<Color>> = listOf(
    listOf(Color(0xFF1E3C72), Color(0xFF0B0F1A)),
    listOf(Color(0xFF42275A), Color(0xFF734B6D), Color(0xFF0D0D0D)),
    listOf(Color(0xFF134E5E), Color(0xFF0B2B26), Color(0xFF050807)),
    listOf(Color(0xFFCB356B), Color(0xFF3A0F1F), Color(0xFF0A0507)),
    listOf(Color(0xFFF7971E), Color(0xFF5A2A00), Color(0xFF0D0804)),
    listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0), Color(0xFF07031A)),
    listOf(Color(0xFF00C9FF), Color(0xFF0D3B66), Color(0xFF020812)),
    listOf(Color(0xFF232526), Color(0xFF414345), Color(0xFF0C0C0C)),
)

@Serializable
private data class StoredCustomBackgroundPreferences(
    val mode: String = CustomBackgroundMode.Off.name,
    val colorIndex: Int = 0,
    val gradientIndex: Int = 0,
    val imageUrl: String? = null,
    val dim: Int = DefaultCustomBackgroundDim,
    val blur: Int = DefaultCustomBackgroundBlur,
    val cardOpacity: Int = DefaultCustomBackgroundCardOpacity,
)

data class CustomBackgroundUiState(
    val mode: CustomBackgroundMode = CustomBackgroundMode.Off,
    val colorIndex: Int = 0,
    val gradientIndex: Int = 0,
    val imageUrl: String? = null,
    /** Black overlay on top of the background, 0–90%. Keeps text readable over bright photos. */
    val dim: Int = DefaultCustomBackgroundDim,
    /** Blur radius in dp applied to photo backgrounds (Android 12+ and iOS). */
    val blur: Int = DefaultCustomBackgroundBlur,
    /** Opacity of cards/surfaces drawn over the background, 30–100%. */
    val cardOpacity: Int = DefaultCustomBackgroundCardOpacity,
) {
    val isActive: Boolean
        get() = when (mode) {
            CustomBackgroundMode.Off -> false
            CustomBackgroundMode.Image -> !imageUrl.isNullOrBlank()
            else -> true
        }

    val color: Color
        get() = CustomBackgroundColorPresets[colorIndex.coerceIn(CustomBackgroundColorPresets.indices)]

    val gradient: List<Color>
        get() = CustomBackgroundGradientPresets[gradientIndex.coerceIn(CustomBackgroundGradientPresets.indices)]
}

object CustomBackgroundRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _uiState = MutableStateFlow(CustomBackgroundUiState())
    val uiState: StateFlow<CustomBackgroundUiState> = _uiState.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        _uiState.value = CustomBackgroundUiState()
    }

    fun setMode(mode: CustomBackgroundMode) {
        update { it.copy(mode = mode) }
    }

    fun setColorIndex(index: Int) {
        update {
            it.copy(
                mode = CustomBackgroundMode.SolidColor,
                colorIndex = index.coerceIn(CustomBackgroundColorPresets.indices),
            )
        }
    }

    fun setGradientIndex(index: Int) {
        update {
            it.copy(
                mode = CustomBackgroundMode.Gradient,
                gradientIndex = index.coerceIn(CustomBackgroundGradientPresets.indices),
            )
        }
    }

    /** Saves the picked photo to app storage and switches the background to it. */
    fun setImage(bytes: ByteArray) {
        ensureLoaded()
        val previousUrl = _uiState.value.imageUrl
        val savedUrl = CustomBackgroundStorage.saveImage(bytes) ?: return
        update { it.copy(mode = CustomBackgroundMode.Image, imageUrl = savedUrl) }
        if (previousUrl != null && previousUrl != savedUrl) {
            CustomBackgroundStorage.deleteImage(previousUrl)
        }
    }

    fun setDim(dim: Int) {
        update { it.copy(dim = dim.coerceIn(0, 90)) }
    }

    fun setBlur(blur: Int) {
        update { it.copy(blur = blur.coerceIn(0, MaxCustomBackgroundBlur)) }
    }

    fun setCardOpacity(opacity: Int) {
        update { it.copy(cardOpacity = opacity.coerceIn(30, 100)) }
    }

    fun resetToDefaults() {
        ensureLoaded()
        _uiState.value.imageUrl?.let(CustomBackgroundStorage::deleteImage)
        _uiState.value = CustomBackgroundUiState()
        persist()
    }

    private fun update(transform: (CustomBackgroundUiState) -> CustomBackgroundUiState) {
        ensureLoaded()
        val next = transform(_uiState.value)
        if (_uiState.value == next) return
        _uiState.value = next
        persist()
    }

    private fun loadFromDisk() {
        hasLoaded = true

        val payload = CustomBackgroundStorage.loadPayload().orEmpty().trim()
        val stored = payload.takeIf { it.isNotEmpty() }?.let {
            runCatching { json.decodeFromString<StoredCustomBackgroundPreferences>(it) }.getOrNull()
        }

        _uiState.value = if (stored != null) {
            CustomBackgroundUiState(
                mode = CustomBackgroundMode.entries.firstOrNull { it.name == stored.mode }
                    ?: CustomBackgroundMode.Off,
                colorIndex = stored.colorIndex.coerceIn(CustomBackgroundColorPresets.indices),
                gradientIndex = stored.gradientIndex.coerceIn(CustomBackgroundGradientPresets.indices),
                imageUrl = stored.imageUrl,
                dim = stored.dim.coerceIn(0, 90),
                blur = stored.blur.coerceIn(0, MaxCustomBackgroundBlur),
                cardOpacity = stored.cardOpacity.coerceIn(30, 100),
            )
        } else {
            CustomBackgroundUiState()
        }
    }

    private fun persist() {
        val state = _uiState.value
        CustomBackgroundStorage.savePayload(
            json.encodeToString(
                StoredCustomBackgroundPreferences(
                    mode = state.mode.name,
                    colorIndex = state.colorIndex,
                    gradientIndex = state.gradientIndex,
                    imageUrl = state.imageUrl,
                    dim = state.dim,
                    blur = state.blur,
                    cardOpacity = state.cardOpacity,
                ),
            ),
        )
    }
}
