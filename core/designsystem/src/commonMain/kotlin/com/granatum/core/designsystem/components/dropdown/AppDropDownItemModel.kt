package com.granatum.core.designsystem.components.dropdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class AppDropDownItemModel(
    val title: String,
    val icon: ImageVector,
    val contentColor: Color,
    val onClick: () -> Unit
)