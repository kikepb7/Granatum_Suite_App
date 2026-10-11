package com.granatum.core.presentation.environment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.granatum.core.domain.environment.AppEnvironment
import com.granatum.core.domain.environment.EnvironmentError
import granatumsuite.core.presentation.generated.resources.environment_unsynced
import granatumsuite.core.presentation.generated.resources.Res
import granatumsuite.core.presentation.generated.resources.environment_current
import granatumsuite.core.presentation.generated.resources.environment_demo
import granatumsuite.core.presentation.generated.resources.environment_demo_description
import granatumsuite.core.presentation.generated.resources.environment_description
import granatumsuite.core.presentation.generated.resources.environment_label
import granatumsuite.core.presentation.generated.resources.environment_local
import granatumsuite.core.presentation.generated.resources.environment_local_description
import granatumsuite.core.presentation.generated.resources.environment_pre
import granatumsuite.core.presentation.generated.resources.environment_pre_description
import granatumsuite.core.presentation.generated.resources.environment_pro
import granatumsuite.core.presentation.generated.resources.environment_pro_description
import granatumsuite.core.presentation.generated.resources.environment_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

fun AppEnvironment.label(): StringResource = when (this) {
    AppEnvironment.LOCAL -> Res.string.environment_local
    AppEnvironment.PRE -> Res.string.environment_pre
    AppEnvironment.PRO -> Res.string.environment_pro
    AppEnvironment.DEMO -> Res.string.environment_demo
}

private fun AppEnvironment.description(): StringResource = when (this) {
    AppEnvironment.LOCAL -> Res.string.environment_local_description
    AppEnvironment.PRE -> Res.string.environment_pre_description
    AppEnvironment.PRO -> Res.string.environment_pro_description
    AppEnvironment.DEMO -> Res.string.environment_demo_description
}

/**
 * The environment selector of the PRE build (specs/010-entornos, US2): a chip with the current
 * environment that opens the list. The caller only shows it when the build can switch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentPicker(
    current: AppEnvironment,
    available: List<AppEnvironment>,
    enabled: Boolean,
    error: EnvironmentError?,
    onPick: (AppEnvironment) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AssistChip(
            onClick = { open = true },
            enabled = enabled,
            label = { Text(stringResource(Res.string.environment_label, stringResource(current.label()))) }
        )
        if (error is EnvironmentError.UnsyncedWork) {
            Text(
                stringResource(Res.string.environment_unsynced, error.count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
    if (open) {
        ModalBottomSheet(onDismissRequest = { open = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(Res.string.environment_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.environment_description), style = MaterialTheme.typography.bodySmall)
                available.forEach { environment ->
                    val selected = environment == current
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !selected) {
                            open = false
                            onPick(environment)
                        }
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(environment.label()), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(stringResource(environment.description()), style = MaterialTheme.typography.bodySmall)
                            }
                            if (selected) Text(stringResource(Res.string.environment_current), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 16.dp))
            }
        }
    }
}
