package com.granatum.core.designsystem.components.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import granatumsuite.core.designsystem.generated.resources.Res
import granatumsuite.core.designsystem.generated.resources.ic_box
import granatumsuite.core.designsystem.generated.resources.ic_calendar
import granatumsuite.core.designsystem.generated.resources.ic_camera
import granatumsuite.core.designsystem.generated.resources.ic_check
import granatumsuite.core.designsystem.generated.resources.ic_close
import granatumsuite.core.designsystem.generated.resources.ic_image
import granatumsuite.core.designsystem.generated.resources.ic_minus
import granatumsuite.core.designsystem.generated.resources.ic_pin
import granatumsuite.core.designsystem.generated.resources.ic_plus
import granatumsuite.core.designsystem.generated.resources.ic_ruler
import granatumsuite.core.designsystem.generated.resources.ic_sort
import granatumsuite.core.designsystem.generated.resources.ic_tune
import granatumsuite.core.designsystem.generated.resources.ic_warehouse
import org.jetbrains.compose.resources.vectorResource

/** General-purpose icons in the stroke style of [AppTabIcons]. */
object AppIcons {
    val Filters: ImageVector @Composable get() = vectorResource(Res.drawable.ic_tune)
    val Minus: ImageVector @Composable get() = vectorResource(Res.drawable.ic_minus)
    val Plus: ImageVector @Composable get() = vectorResource(Res.drawable.ic_plus)
    val Close: ImageVector @Composable get() = vectorResource(Res.drawable.ic_close)
    val Location: ImageVector @Composable get() = vectorResource(Res.drawable.ic_pin)
    val Ruler: ImageVector @Composable get() = vectorResource(Res.drawable.ic_ruler)
    val Image: ImageVector @Composable get() = vectorResource(Res.drawable.ic_image)
    val Camera: ImageVector @Composable get() = vectorResource(Res.drawable.ic_camera)
    val Calendar: ImageVector @Composable get() = vectorResource(Res.drawable.ic_calendar)
    val Box: ImageVector @Composable get() = vectorResource(Res.drawable.ic_box)
    val Sort: ImageVector @Composable get() = vectorResource(Res.drawable.ic_sort)
    val Warehouse: ImageVector @Composable get() = vectorResource(Res.drawable.ic_warehouse)
    val Check: ImageVector @Composable get() = vectorResource(Res.drawable.ic_check)
}
