package com.granatum.feature.invoicing.presentation.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.chips.AppStatusChip
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.feedback.AppErrorState
import com.granatum.core.designsystem.components.feedback.AppLoadingState
import com.granatum.core.designsystem.components.lists.AppSectionHeader
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.caption
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
    val tone =
        when (state) {
            InvoiceState.PENDING_RECOGNITION -> AppTone.WARNING
            InvoiceState.DRAFT -> AppTone.INFO
            InvoiceState.CONFIRMED -> AppTone.SUCCESS
            InvoiceState.DISCARDED -> AppTone.NEUTRAL
        }
    AppStatusChip(text = stringResource(state.label()), tone = tone, showDot = true, modifier = modifier)
}

@Composable
fun Pill(
    text: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Surface(shape = AppTheme.shapes.pill, color = background, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    AppLoadingState(modifier = modifier)
}

/** What a screen shows when its data could not be loaded: the reason and a way to try again. */
@Composable
fun ErrorState(
    error: UiText,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppErrorState(
        message = error.asString(),
        retryLabel = stringResource(Res.string.retry),
        onRetry = onRetry,
        modifier = modifier,
    )
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    AppSectionHeader(title = text, modifier = modifier)
}
