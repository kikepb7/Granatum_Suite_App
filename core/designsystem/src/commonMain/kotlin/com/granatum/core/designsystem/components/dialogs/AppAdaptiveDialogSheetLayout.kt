package com.granatum.core.designsystem.components.dialogs

import androidx.compose.runtime.Composable
import com.granatum.core.presentation.util.currentDeviceConfiguration

@Composable
fun AppAdaptiveDialogSheetLayout(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val configuration = currentDeviceConfiguration()

    if (configuration.isMobile) {
        AppBottomSheet(
            onDismiss = onDismiss,
            content = content
        )
    } else {
        AppDialogContent(
            onDismiss = onDismiss,
            content = content
        )
    }
}