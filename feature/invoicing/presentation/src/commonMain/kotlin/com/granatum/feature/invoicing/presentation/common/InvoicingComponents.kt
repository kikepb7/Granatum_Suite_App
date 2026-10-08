package com.granatum.feature.invoicing.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.theme.extended
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.invoicing.domain.model.InvoiceState
import granatumsuite.feature.invoicing.presentation.generated.resources.Res
import granatumsuite.feature.invoicing.presentation.generated.resources.retry
import org.jetbrains.compose.resources.stringResource

@Composable
fun StateBadge(
    state: InvoiceState,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (background, content) =
        when (state) {
            InvoiceState.PENDING_RECOGNITION -> colors.extended.accentYellow to colors.extended.yellowCardText
            InvoiceState.DRAFT -> colors.primaryContainer to colors.onPrimaryContainer
            InvoiceState.CONFIRMED -> colors.extended.successOutline to colors.extended.success
            InvoiceState.DISCARDED -> colors.extended.secondaryFill to colors.extended.textPlaceholder
        }
    Pill(stringResource(state.label()), background, content, modifier)
}

@Composable
fun Pill(
    text: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Surface(shape = RoundedCornerShape(50), color = background, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) { CircularProgressIndicator() }
}

/** What a screen shows when its data could not be loaded: the reason and a way to try again. */
@Composable
fun ErrorState(
    error: UiText,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = modifier.fillMaxSize().padding(32.dp),
    ) {
        Text(
            text = error.asString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.extended.textSecondary,
            textAlign = TextAlign.Center,
        )
        AppButton(text = stringResource(Res.string.retry), onClick = onRetry, style = AppButtonStyle.SECONDARY)
    }
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.extended.textPrimary,
        modifier = modifier,
    )
}
