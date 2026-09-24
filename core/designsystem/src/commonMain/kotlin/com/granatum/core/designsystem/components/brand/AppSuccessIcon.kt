package com.granatum.core.designsystem.components.brand

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.granatum.core.designsystem.theme.extended
import org.jetbrains.compose.resources.vectorResource
import granatumsuite.core.designsystem.generated.resources.Res
import granatumsuite.core.designsystem.generated.resources.success_checkmark

@Composable
fun ChirpSuccessIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = vectorResource(Res.drawable.success_checkmark),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.extended.success,
        modifier = modifier
    )
}