package com.granatum.core.designsystem.components.brand

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.vectorResource
import granatumsuite.core.designsystem.generated.resources.Res
import granatumsuite.core.designsystem.generated.resources.loading_icon

@Composable
fun AppBrandLogo(modifier: Modifier = Modifier) {
    Icon(
        imageVector = vectorResource(Res.drawable.loading_icon), // TODO --> change by App logo
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}