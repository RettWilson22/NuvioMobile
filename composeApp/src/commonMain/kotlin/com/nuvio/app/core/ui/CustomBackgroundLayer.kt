package com.nuvio.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/** Full-screen user background drawn behind every screen when a custom background is active. */
@Composable
internal fun CustomBackgroundLayer(
    state: CustomBackgroundUiState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        when (state.mode) {
            CustomBackgroundMode.SolidColor -> Box(Modifier.fillMaxSize().background(state.color))
            CustomBackgroundMode.Gradient -> Box(
                Modifier.fillMaxSize().background(Brush.verticalGradient(state.gradient)),
            )
            CustomBackgroundMode.Image -> AsyncImage(
                model = state.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (state.blur > 0) Modifier.blur(state.blur.dp) else Modifier),
            )
            CustomBackgroundMode.Off -> Unit
        }
        if (state.dim > 0) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = state.dim / 100f)))
        }
    }
}
