package com.granatum.feature.staff.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppFloatingActionButton
import com.granatum.core.designsystem.components.chips.AppFilterChip
import com.granatum.core.designsystem.components.chips.AppStatusChip
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.feedback.AppEmptyState
import com.granatum.core.designsystem.components.feedback.AppErrorState
import com.granatum.core.designsystem.components.feedback.AppLoadingState
import com.granatum.core.designsystem.components.icons.AppTabIcons
import com.granatum.core.designsystem.components.inputs.AppSearchField
import com.granatum.core.designsystem.components.lists.AppIconBadge
import com.granatum.core.designsystem.components.lists.AppListItem
import com.granatum.core.designsystem.components.motion.appEntrance
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.components.topbar.TopBarIconButton
import com.granatum.core.designsystem.theme.AppTheme
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
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.staff_title),
                actions = {
                    TopBarIconButton(onClick = onOpenTeamAttendance) {
                        Icon(Icons.Default.DateRange, contentDescription = stringResource(Res.string.staff_team_attendance))
                    }
                },
            )
        },
        floatingActionButton = {
            AppFloatingActionButton(onClick = onOnboard) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.staff_onboard))
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier.padding(horizontal = AppTheme.spacing.screenHorizontal, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppSearchField(state = viewModel.query, placeholder = stringResource(Res.string.staff_search))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(true to Res.string.filter_active, false to Res.string.filter_inactive, null to Res.string.filter_all).forEach { (value, label) ->
                        AppFilterChip(
                            selected = state.active == value,
                            onClick = { viewModel.filter(value) },
                            text = stringResource(label),
                        )
                    }
                }
            }
            val visible = state.visible(query)
            when {
                state.isLoading -> AppLoadingState()
                state.error != null && state.members.isEmpty() ->
                    AppErrorState(
                        message = state.error!!.asString(),
                        retryLabel = stringResource(Res.string.retry),
                        onRetry = viewModel::refresh,
                    )
                visible.isEmpty() ->
                    AppEmptyState(
                        title =
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
                    )
                else ->
                    LazyColumn(
                        contentPadding =
                            PaddingValues(
                                start = AppTheme.spacing.screenHorizontal,
                                end = AppTheme.spacing.screenHorizontal,
                                top = 8.dp,
                                bottom = 120.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        itemsIndexed(visible, key = { _, member -> member.id }) { index, member ->
                            MemberRow(member, Modifier.appEntrance(index)) { onOpenMember(member.id) }
                        }
                    }
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: StaffMember,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    AppListItem(
        title = member.name,
        subtitle = "${member.position} · ${stringResource(member.contract.label())}",
        onClick = onClick,
        modifier = modifier,
        tone = if (member.active) null else AppTone.NEUTRAL,
        leading = { AppIconBadge(icon = AppTabIcons.Team, tone = if (member.active) AppTone.BRAND else AppTone.NEUTRAL) },
        trailing = {
            if (!member.active) {
                AppStatusChip(text = stringResource(Res.string.staff_inactive), tone = AppTone.DANGER)
            }
        },
    )
}
