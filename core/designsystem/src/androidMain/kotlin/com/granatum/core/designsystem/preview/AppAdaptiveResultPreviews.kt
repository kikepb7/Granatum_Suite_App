package com.granatum.core.designsystem.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.granatum.core.designsystem.components.layouts.AppAdaptiveResultLayout
import com.granatum.core.designsystem.theme.AppTheme

@Composable
@PreviewLightDark
@PreviewScreenSizes
fun AppAdaptiveResultLayoutPreview() {
    AppTheme {
        AppAdaptiveResultLayout(
            modifier = Modifier.fillMaxSize(),
            content = {
                Text(
                    text = "Registration successful!",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        )
    }
}