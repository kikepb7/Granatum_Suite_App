package com.granatum.core.designsystem.components.feedback

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.chips.toneContent
import com.granatum.core.designsystem.components.chips.toneSoft
import com.granatum.core.designsystem.theme.AppTheme
import granatumsuite.core.designsystem.generated.resources.Res
import granatumsuite.core.designsystem.generated.resources.granatum_symbol_star
import org.jetbrains.compose.resources.vectorResource

/** Nothing to show yet: a soft brand disc, a title and an optional hint or action. */
@Composable
fun AppEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: @Composable (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        modifier = modifier.fillMaxSize().padding(32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(64.dp).clip(CircleShape).background(AppTheme.colors.brandSoft)
        ) {
            if (icon != null) {
                icon()
            } else {
                Image(
                    imageVector = vectorResource(Res.drawable.granatum_symbol_star),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(AppTheme.colors.brand),
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textTertiary,
                textAlign = TextAlign.Center
            )
        }
        action?.invoke()
    }
}

/** The data could not be loaded: the reason and a way to try again. */
@Composable
fun AppErrorState(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppEmptyState(
        title = message,
        modifier = modifier,
        action = { AppButton(text = retryLabel, onClick = onRetry, style = AppButtonStyle.SECONDARY) }
    )
}

@Composable
fun AppLoadingState(modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
        CircularProgressIndicator(color = AppTheme.colors.brand, trackColor = AppTheme.colors.brandSoft)
    }
}

/** An inline notice that must not be buried: unsent or refused work, missing set-up. */
@Composable
fun AppBanner(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    tone: AppTone = AppTone.WARNING,
    action: @Composable (() -> Unit)? = null
) {
    val content = AppTheme.colors.toneContent(tone)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.medium)
            .background(AppTheme.colors.toneSoft(tone))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(Modifier.size(8.dp).background(content, CircleShape))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = content
            )
            if (caption != null) {
                Text(text = caption, style = MaterialTheme.typography.bodySmall, color = content)
            }
        }
        action?.invoke()
    }
}
