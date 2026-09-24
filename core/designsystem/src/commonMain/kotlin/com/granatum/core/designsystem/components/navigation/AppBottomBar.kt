package com.granatum.core.designsystem.components.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.granatum.core.designsystem.theme.extended

data class AppBottomBarItemModel(val label: String, val icon: @Composable () -> Unit)

@Composable
fun AppBottomBar(
    items: List<AppBottomBarItemModel>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.extended.surfaceOutline
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { onItemClick(index) },
                    icon = { item.icon() },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.displaySmall
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.extended.textPlaceholder,
                        unselectedTextColor = MaterialTheme.colorScheme.extended.textPlaceholder
                    )
                )
            }
        }
    }
}
