package com.granatum.core.designsystem.components.topbar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.theme.AppTheme
import granatumsuite.core.designsystem.generated.resources.Res
import granatumsuite.core.designsystem.generated.resources.app_topbar_account
import org.jetbrains.compose.resources.stringResource

/** The signed-in person's entry to their account, provided by the app shell. */
@Immutable
data class AccountAction(val initial: String, val onClick: () -> Unit)

/**
 * Set by the app shell for the main screens. With it, the top bar of a screen without a back
 * button shows the account button, so no feature module needs to know about the account.
 */
val LocalAccountAction = staticCompositionLocalOf<AccountAction?> { null }

/** The account button, or nothing when the shell provides no [LocalAccountAction]. */
@Composable
fun AppAccountButton(modifier: Modifier = Modifier) {
    val action = LocalAccountAction.current ?: return
    val description = stringResource(Res.string.app_topbar_account)
    Surface(
        onClick = action.onClick,
        shape = CircleShape,
        color = AppTheme.colors.brandSoft,
        contentColor = AppTheme.colors.brand,
        border = BorderStroke(2.dp, AppTheme.colors.brandGradient),
        modifier = modifier.size(42.dp).semantics { contentDescription = description }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = action.initial, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}
