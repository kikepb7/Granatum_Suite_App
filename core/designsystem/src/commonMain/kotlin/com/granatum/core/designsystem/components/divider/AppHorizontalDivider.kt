package com.granatum.core.designsystem.components.divider

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AppHorizontalDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.outline
    )
}