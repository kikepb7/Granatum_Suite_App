@file:OptIn(ExperimentalMaterial3Api::class)

package com.granatum.core.designsystem.components.dialogs

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(key1 = sheetState.isVisible) {
        if (sheetState.isVisible) {
            sheetState.expand()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        contentWindowInsets = { WindowInsets() },
        modifier = modifier.statusBarsPadding()
    ) {
        content()
    }
}