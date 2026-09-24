package com.granatum.core.presentation.util

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

@Composable
fun Modifier.clearFocusOnTap(): Modifier {
    val focusManager = LocalFocusManager.current
    return this.pointerInput(key1 = Unit) {
        detectTapGestures {
            focusManager.clearFocus()
        }
    }
}