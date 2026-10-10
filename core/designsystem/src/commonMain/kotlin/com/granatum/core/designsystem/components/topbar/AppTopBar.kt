package com.granatum.core.designsystem.components.topbar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.labelXSmall
import com.granatum.core.designsystem.theme.largeTitle
import granatumsuite.core.designsystem.generated.resources.app_topbar_back
import granatumsuite.core.designsystem.generated.resources.app_topbar_settings
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import granatumsuite.core.designsystem.generated.resources.Res.string as RString

/**
 * Large-title top bar on the canvas, with no divider. Main screens (no back button) show the
 * title big on the left and the account button on the right; detail screens show a round back
 * button and a compact title. [actions] go before the trailing button.
 */
@Composable
fun AppTopBar(
    title: String = "App",
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    subtitle: String? = null,
    overline: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = AppTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
    ) {
        if (onBackClick != null) {
            TopBarIconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(RString.app_topbar_back),
                    modifier = Modifier.size(size = 20.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            if (overline != null) {
                Text(
                    text = overline.uppercase(),
                    style = MaterialTheme.typography.labelXSmall.copy(letterSpacing = 1.2.sp),
                    color = colors.brand,
                    maxLines = 1
                )
            }
            Text(
                text = title,
                style = if (onBackClick == null) {
                    MaterialTheme.typography.largeTitle
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary
                )
            }
        }
        actions()
        if (onSettingsClick != null) {
            TopBarIconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(RString.app_topbar_settings),
                    modifier = Modifier.size(size = 20.dp)
                )
            }
        } else if (onBackClick == null) {
            AppAccountButton()
        }
    }
}

/** Round, bordered icon button used in bars. Public so screens can add actions that match. */
@Composable
fun TopBarIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    Surface(
        onClick = onClick,
        shape = AppTheme.shapes.pill,
        color = colors.surface,
        contentColor = colors.textSecondary,
        border = BorderStroke(width = 1.dp, color = colors.border),
        modifier = modifier.size(size = 40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Preview
@Composable
private fun AppTopBarHomePreview() {
    AppTheme { AppTopBar(title = "Fichajes", onSettingsClick = {}) }
}

@Preview
@Composable
private fun AppTopBarDetailPreview() {
    AppTheme { AppTopBar(title = "Mi Perfil", onBackClick = {}) }
}

@Preview
@Composable
private fun AppTopBarDarkPreview() {
    AppTheme(darkTheme = true) { AppTopBar(title = "Histórico", subtitle = "Octubre", onSettingsClick = {}) }
}
