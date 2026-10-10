package com.granatum.core.designsystem.components.textfields

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.extended

@Composable
fun AppTextFieldLayout(
    title: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    supportingConditions: List<Pair<String, Boolean>>? = null,
    enabled: Boolean = true,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    textField: @Composable (Modifier, MutableInteractionSource) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) { onFocusChanged(isFocused) }

    val textFieldStyleModifier = Modifier
        .fillMaxWidth()
        .background(
            color = when {
                isFocused -> MaterialTheme.colorScheme.primary.copy(
                    alpha = 0.05f
                )
                enabled -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.extended.secondaryFill
            },
            shape = RoundedCornerShape(14.dp)
        )
        .border(
            width = if (isFocused) 2.dp else 1.dp,
            brush = when {
                isError -> SolidColor(MaterialTheme.colorScheme.error)
                isFocused -> AppTheme.colors.brandGradient
                else -> SolidColor(MaterialTheme.colorScheme.outline)
            },
            shape = RoundedCornerShape(14.dp)
        )
        .padding(12.dp)

    Column(modifier = modifier) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.extended.textSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        textField(textFieldStyleModifier, interactionSource)

        if(!supportingConditions.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            supportingConditions.forEach { (label, ok) ->
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    val icon = if (ok) Icons.Filled.Check else Icons.Filled.Close
                    val iconColor = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor
                    )

                    Text(
                        text = label,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.extended.textTertiary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            return@Column
        }

        if (!supportingText.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = supportingText,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.extended.textTertiary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}