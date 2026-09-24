package com.granatum.core.designsystem.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.granatum.core.designsystem.components.brand.AppBrandLogo
import com.granatum.core.designsystem.components.layouts.AppAdaptiveFormLayout
import com.granatum.core.designsystem.theme.AppTheme

@Composable
@PreviewLightDark
@PreviewScreenSizes
fun AppAdaptiveFormLayoutLightPreview() {
    AppTheme {
        AppAdaptiveFormLayout(
            headerText = "Welcome to App!",
            errorText = "Login failed!",
            logo = { AppBrandLogo() },
            formContent = {
                Text(
                    text = "Sample form title",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sample form title 2",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        )
    }
}