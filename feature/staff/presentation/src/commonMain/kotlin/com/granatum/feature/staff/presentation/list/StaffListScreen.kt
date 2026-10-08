package com.granatum.feature.staff.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.components.topbar.AppAccountButton
import com.granatum.core.designsystem.theme.extended
import com.granatum.feature.staff.domain.StaffMember
import com.granatum.feature.staff.presentation.common.label
import granatumsuite.feature.staff.presentation.generated.resources.*
import granatumsuite.feature.staff.presentation.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StaffListRoot(
    onOpenMember: (String) -> Unit,
    onOnboard: () -> Unit,
    onOpenTeamAttendance: () -> Unit,
    viewModel: StaffListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Every time the screen shows: edits made in a record are seen on the way back.
    LaunchedEffect(Unit) { viewModel.refresh() }
    val query = viewModel.query.text.toString()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                ) {
                    Text(
                        stringResource(Res.string.staff_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.extended.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onOpenTeamAttendance) {
                        Icon(Icons.Default.DateRange, contentDescription = stringResource(Res.string.staff_team_attendance))
                    }
                    AppAccountButton()
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOnboard,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.staff_onboard)) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    state = viewModel.query,
                    placeholder = stringResource(Res.string.staff_search),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(true to Res.string.filter_active, false to Res.string.filter_inactive, null to Res.string.filter_all).forEach { (value, label) ->
                        FilterChip(
                            selected = state.active == value,
                            onClick = { viewModel.filter(value) },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
            }
            val visible = state.visible(query)
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.error != null && state.members.isEmpty() ->
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    ) {
                        Text(state.error!!.asString(), color = MaterialTheme.colorScheme.extended.textSecondary)
                        AppButton(stringResource(Res.string.retry), onClick = viewModel::refresh, style = AppButtonStyle.SECONDARY)
                    }
                visible.isEmpty() ->
                    Text(
                        stringResource(
                            if (query.isBlank() &&
                                state.active == true &&
                                state.members.size <= 1
                            ) {
                                Res.string.staff_empty
                            } else {
                                Res.string.staff_empty_filtered
                            },
                        ),
                        color = MaterialTheme.colorScheme.extended.textPlaceholder,
                        modifier = Modifier.padding(24.dp),
                    )
                else ->
                    LazyColumn(
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(visible, key = { it.id }) { member -> MemberRow(member) { onOpenMember(member.id) } }
                    }
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: StaffMember,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.extended.surfaceHigher,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    member.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (!member.active) {
                    Text(
                        stringResource(Res.string.staff_inactive),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(
                "${member.position} · ${stringResource(member.contract.label())}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.extended.textSecondary,
            )
        }
    }
}
