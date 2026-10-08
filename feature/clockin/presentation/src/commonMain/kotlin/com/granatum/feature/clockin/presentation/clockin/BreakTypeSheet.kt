package com.granatum.feature.clockin.presentation.clockin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.dialogs.AppBottomSheet
import com.granatum.core.designsystem.theme.extended
import com.granatum.feature.clockin.domain.model.BreakType
import com.granatum.feature.clockin.presentation.mapper.label
import granatumsuite.feature.clockin.presentation.generated.resources.Res
import granatumsuite.feature.clockin.presentation.generated.resources.break_sheet_title
import org.jetbrains.compose.resources.stringResource

/** The server needs the break type (spec 005, FR-003): one tap per type, no extra confirmation. */
@Composable
fun BreakTypeSheet(onChoose: (BreakType) -> Unit, onDismiss: () -> Unit) {
    AppBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.break_sheet_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.extended.textPrimary,
                modifier = Modifier.semantics { heading() }
            )
            BreakType.entries.forEach { type ->
                AppButton(
                    text = stringResource(type.label()),
                    onClick = { onChoose(type) },
                    style = AppButtonStyle.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
