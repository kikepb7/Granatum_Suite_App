package com.granatum.core.designsystem.components.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import granatumsuite.core.designsystem.generated.resources.Res
import granatumsuite.core.designsystem.generated.resources.tab_clock_icon
import granatumsuite.core.designsystem.generated.resources.tab_history_icon
import granatumsuite.core.designsystem.generated.resources.tab_inventory_icon
import granatumsuite.core.designsystem.generated.resources.tab_invoice_icon
import granatumsuite.core.designsystem.generated.resources.users_icon
import org.jetbrains.compose.resources.vectorResource

/** Icons of the bottom bar's destinations, drawn in the same stroke style as the rest. */
object AppTabIcons {
    val Clock: ImageVector @Composable get() = vectorResource(Res.drawable.tab_clock_icon)
    val Inventory: ImageVector @Composable get() = vectorResource(Res.drawable.tab_inventory_icon)
    val History: ImageVector @Composable get() = vectorResource(Res.drawable.tab_history_icon)
    val Invoice: ImageVector @Composable get() = vectorResource(Res.drawable.tab_invoice_icon)
    val Team: ImageVector @Composable get() = vectorResource(Res.drawable.users_icon)
}
