package com.granatum.core.designsystem.components.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.granatum.core.designsystem.theme.AppTheme

/**
 * An item's picture (a URL, or picked bytes), or - without one, while it loads or if it fails - the
 * initial of [placeholderText] on a soft brand gradient, so every card keeps the same shape. Used by
 * the inventory and the events, which show the same materials.
 */
@Composable
fun AppItemImage(image: Any?, placeholderText: String, modifier: Modifier = Modifier, corner: Dp = 16.dp) {
    val shape = RoundedCornerShape(corner)
    Box(modifier = modifier.clip(shape)) {
        val placeholder = @Composable { Initial(placeholderText) }
        if (image == null) {
            placeholder()
        } else {
            SubcomposeAsyncImage(
                model = image.toRequest(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = { placeholder() },
                error = { placeholder() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * The server's image addresses are signed and change on every download (backend 010, D-005): the
 * cache is keyed by the address without its token, so the same image is not fetched again.
 */
@Composable
private fun Any.toRequest(): Any {
    val url = this as? String ?: return this
    val key = url.substringBefore('?')
    return ImageRequest.Builder(LocalPlatformContext.current)
        .data(url)
        .memoryCacheKey(key)
        .diskCacheKey(key)
        .build()
}

@Composable
private fun Initial(text: String) {
    val colors = AppTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(colors.brandSoft, colors.surfaceSunken)))
    ) {
        Text(
            text = text.trim().take(1).uppercase().ifEmpty { "?" },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.brand
        )
    }
}
