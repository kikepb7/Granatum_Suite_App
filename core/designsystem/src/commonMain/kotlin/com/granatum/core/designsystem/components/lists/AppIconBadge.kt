package com.granatum.core.designsystem.components.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.chips.toneContent
import com.granatum.core.designsystem.components.chips.toneSoft
import com.granatum.core.designsystem.theme.AppTheme

/** A tinted rounded square holding an icon: the leading mark of a list row. */
@Composable
fun AppIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: AppTone = AppTone.BRAND
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppTheme.colors.toneSoft(tone))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppTheme.colors.toneContent(tone),
            modifier = Modifier.size(22.dp)
        )
    }
}

/** A thin rounded bar: how full something is, from 0 to 1. */
@Composable
fun AppProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    tone: AppTone = AppTone.BRAND
) {
    val fraction = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(AppTheme.shapes.pill)
            .background(AppTheme.colors.surfaceSunken)
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(5.dp)
                    .clip(AppTheme.shapes.pill)
                    .background(AppTheme.colors.toneContent(tone))
            )
        }
    }
}
