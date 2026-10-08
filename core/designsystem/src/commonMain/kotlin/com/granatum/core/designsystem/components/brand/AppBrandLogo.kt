package com.granatum.core.designsystem.components.brand

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import granatumsuite.core.designsystem.generated.resources.Res
import granatumsuite.core.designsystem.generated.resources.granatum_symbol
import org.jetbrains.compose.resources.vectorResource

/** The brand symbol: the coral circle with the white star knot (48 dp unless sized). */
@Composable
fun AppBrandLogo(modifier: Modifier = Modifier) {
    Image(
        imageVector = vectorResource(Res.drawable.granatum_symbol),
        contentDescription = null,
        modifier = modifier
    )
}
