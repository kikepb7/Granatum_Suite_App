package com.granatum.core.designsystem.components.topbar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.granatum.core.designsystem.components.brand.AppBrandLogo
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.extended
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import granatumsuite.core.designsystem.generated.resources.app_topbar_back
import granatumsuite.core.designsystem.generated.resources.app_topbar_settings
import granatumsuite.core.designsystem.generated.resources.Res.string as RString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String = "App",
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = Bold, letterSpacing = (-0.4).sp),
                    color = MaterialTheme.colorScheme.extended.textPrimary
                )
            },
            navigationIcon = {
                if (onBackClick != null) {
                    TopBarIconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(RString.app_topbar_back),
                            modifier = Modifier.size(size = 18.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.size(size = 38.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppBrandLogo(modifier = Modifier.size(size = 22.dp))
                    }
                }
            },
            actions = {
                if (onSettingsClick != null) {
                    TopBarIconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(RString.app_topbar_settings),
                            modifier = Modifier.size(size = 18.dp)
                        )
                    }
                } else {
                    Box(modifier = Modifier.size(size = 38.dp))
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.extended.surfaceOutline)
    }
}

@Composable
private fun TopBarIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    OutlinedIconButton(
        onClick = onClick,
        modifier = modifier.size(size = 38.dp),
        shape = RoundedCornerShape(size = 10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.extended.surfaceOutline
        ),
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
            contentColor = MaterialTheme.colorScheme.extended.textSecondary
        )
    ) {
        content()
    }
}

@Preview
@Composable
private fun AppTopBarHomePreview() {
    AppTheme { AppTopBar(title = "Inicio", onSettingsClick = {}) }
}

@Preview
@Composable
private fun AppTopBarDetailPreview() {
    AppTheme { AppTopBar(title = "Mi Perfil", onBackClick = {}, onSettingsClick = {}) }
}

@Preview
@Composable
private fun AppTopBarDarkPreview() {
    AppTheme(darkTheme = true) { AppTopBar(title = "Últimos Partidos", onBackClick = {}, onSettingsClick = {}) }
}
