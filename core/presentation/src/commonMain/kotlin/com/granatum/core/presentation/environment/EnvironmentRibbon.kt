package com.granatum.core.presentation.environment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.granatum.core.domain.environment.AppEnvironment
import granatumsuite.core.presentation.generated.resources.Res
import granatumsuite.core.presentation.generated.resources.environment_ribbon
import org.jetbrains.compose.resources.stringResource

/**
 * PRE build only: a thin strip above the session saying which backend is in use, in the error
 * colour when it is production, so nobody changes real data thinking it is a test (FR-006).
 */
@Composable
fun EnvironmentRibbon(environment: AppEnvironment, modifier: Modifier = Modifier) {
    val production = environment == AppEnvironment.PRO
    val background = if (production) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer
    val content = if (production) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth().background(background).statusBarsPadding().padding(vertical = 2.dp)
    ) {
        Text(
            text = stringResource(Res.string.environment_ribbon, stringResource(environment.label())),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = content
        )
    }
}
