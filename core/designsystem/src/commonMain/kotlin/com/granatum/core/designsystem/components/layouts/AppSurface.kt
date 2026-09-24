package com.granatum.core.designsystem.components.layouts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import granatumsuite.core.designsystem.generated.resources.Res
import granatumsuite.core.designsystem.generated.resources.loading_icon

@Composable
fun AppSurface(
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            header()
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
@Preview
fun AppSurfacePreview() {
    AppTheme {
        AppSurface(
            modifier = Modifier.fillMaxSize(),
            header = {
                Icon(
                    imageVector = vectorResource(Res.drawable.loading_icon), // TODO --> change by App logo
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            },
            content = {
                Text(
                    text = "Welcome to App!",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(vertical = 40.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        )
    }
}