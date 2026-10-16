package com.granatum.core.designsystem.components.controls

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.icons.AppIcons
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.statValue
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * − value + for counts: large targets either side of the number, which rolls up or down as it
 * changes. The minus disables at [min].
 */
@Composable
fun AppQuantityStepper(
    value: Int,
    onChange: (Int) -> Unit,
    decreaseDescription: String,
    increaseDescription: String,
    modifier: Modifier = Modifier,
    min: Int = 0,
    enabled: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        StepButton(onClick = { onChange(value - 1) }, enabled = enabled && value > min, description = decreaseDescription) {
            Icon(AppIcons.Minus, contentDescription = null, modifier = Modifier.size(22.dp))
        }
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith (slideOutVertically { if (up) -it else it } + fadeOut())
            },
            label = "stepperValue"
        ) { shown ->
            Text(
                text = shown.toString(),
                style = MaterialTheme.typography.statValue,
                color = AppTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 64.dp)
            )
        }
        StepButton(onClick = { onChange(value + 1) }, enabled = enabled, description = increaseDescription, filled = true) {
            Icon(AppIcons.Plus, contentDescription = null, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun StepButton(onClick: () -> Unit, enabled: Boolean, description: String, filled: Boolean = false, content: @Composable () -> Unit) {
    val colors = AppTheme.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = AppTheme.shapes.medium,
        color = if (filled) colors.brand else colors.surface,
        contentColor = if (filled) colors.onBrand else colors.textPrimary,
        border = if (filled) null else BorderStroke(1.dp, colors.border),
        modifier = Modifier.size(52.dp).semantics { contentDescription = description }
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Preview
@Composable
private fun AppQuantityStepperPreview() {
    AppTheme { AppQuantityStepper(value = 12, onChange = {}, decreaseDescription = "Menos", increaseDescription = "Más") }
}
