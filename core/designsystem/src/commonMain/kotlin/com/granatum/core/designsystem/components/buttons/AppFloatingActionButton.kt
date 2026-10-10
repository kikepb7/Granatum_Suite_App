package com.granatum.core.designsystem.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Gradient squircle with a soft brand glow. */
@Composable
fun AppFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(58.dp)
            .shadow(elevation = 12.dp, shape = shape, ambientColor = colors.glow, spotColor = colors.glow)
            .clip(shape)
            .background(colors.brandGradient)
            .background(Brush.verticalGradient(listOf(colors.onHero.copy(alpha = 0.22f), Color.Transparent), endY = 80f))
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onBrand) { content() }
    }
}

@Composable
@Preview
fun AppFloatingActionButtonPreview() {
    AppTheme {
        AppFloatingActionButton(
            onClick = {}
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )
        }
    }
}
