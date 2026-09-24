package com.granatum.core.designsystem.components.layouts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.icons.AppSuccessIcon
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.extended
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AppSimpleSuccessLayout(
    title: String,
    description: String,
    secondaryError: String? = null,
    icon: @Composable () -> Unit,
    primaryButton: @Composable () -> Unit,
    secondaryButton: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = -(25).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.extended.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.extended.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            primaryButton()

            if(secondaryButton != null) {
                Spacer(modifier = Modifier.height(8.dp))
                secondaryButton()
                if (secondaryError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = secondaryError,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
@Preview
fun AppSimpleSuccessLayoutPreview() {
    AppTheme(darkTheme = true) {
        AppSimpleSuccessLayout(
            title = "Hello world!",
            description = "Test description",
            icon = {
                AppSuccessIcon()
            },
            primaryButton = {
                AppButton(
                    text = "Log In",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            },
            secondaryButton = {
                AppButton(
                    text = "Resend verification email",
                    onClick = {},
                    style = AppButtonStyle.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}