package com.granatum.feature.inventory.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.media.AppItemImage

/** The product's image, or its category's initial without one (see [AppItemImage]). */
@Composable
fun ProductImage(image: Any?, categoryName: String, modifier: Modifier = Modifier, corner: Dp = 16.dp) =
    AppItemImage(image = image, placeholderText = categoryName, modifier = modifier, corner = corner)
