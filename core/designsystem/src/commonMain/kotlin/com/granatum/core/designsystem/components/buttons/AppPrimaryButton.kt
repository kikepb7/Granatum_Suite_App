package com.granatum.core.designsystem.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * The main action of a screen: brand gradient, large radius. Falls back to a flat neutral when
 * disabled so a blocked action never looks pressable.
 */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    height: Dp = 52.dp,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    val colors = AppTheme.colors
    val background = if (enabled) colors.brandGradient else SolidColor(colors.neutralSoft)
    val content = if (enabled) colors.onBrand else colors.textDisabled
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = height)
            .then(
                if (enabled) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = AppTheme.shapes.medium,
                        ambientColor = colors.glow,
                        spotColor = colors.glow
                    )
                } else {
                    Modifier
                }
            )
            .clip(AppTheme.shapes.medium)
            .background(background)
            .background(
                if (enabled) {
                    Brush.verticalGradient(listOf(colors.onHero.copy(alpha = 0.22f), Color.Transparent), endY = 90f)
                } else {
                    SolidColor(Color.Transparent)
                }
            )
            .clickable(enabled = enabled && !isLoading, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp).alpha(if (isLoading) 1f else 0f),
            strokeWidth = 2.dp,
            color = content
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.alpha(if (isLoading) 0f else 1f)
        ) {
            leadingIcon?.invoke()
            Text(text = text, style = MaterialTheme.typography.titleSmall, color = content)
        }
    }
}

@Preview
@Composable
private fun AppPrimaryButtonLightPreview() {
    AppTheme(darkTheme = false) { AppPrimaryButton(text = "Fichar entrada", onClick = {}) }
}

@Preview
@Composable
private fun AppPrimaryButtonDarkPreview() {
    AppTheme(darkTheme = true) { AppPrimaryButton(text = "Fichar entrada", onClick = {}) }
}
