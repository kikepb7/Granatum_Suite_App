package com.granatum.feature.staff.presentation.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.dialogs.AppBottomSheet
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.theme.extended
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.feature.staff.domain.ContractType
import com.granatum.feature.staff.domain.StaffLimits
import com.granatum.feature.staff.domain.TemporaryCredentials
import granatumsuite.feature.staff.presentation.generated.resources.*
import granatumsuite.feature.staff.presentation.generated.resources.Res
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@Composable
fun StaffTextField(
    state: TextFieldState,
    title: StringResource,
    issue: FieldIssue?,
    max: Int,
    enabled: Boolean = true,
    keyboard: KeyboardType = KeyboardType.Text,
    invalidText: StringResource = Res.string.email_invalid,
) {
    val error =
        when (issue) {
            FieldIssue.REQUIRED -> stringResource(Res.string.required)
            FieldIssue.TOO_LONG -> stringResource(Res.string.too_long, max)
            FieldIssue.INVALID -> stringResource(invalidText)
            null -> null
        }
    AppTextField(
        state = state,
        title = stringResource(title),
        singleLine = true,
        enabled = enabled,
        keyboardType = keyboard,
        isError = error != null,
        supportingText = error,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun ContractChips(
    selected: ContractType,
    enabled: Boolean,
    onSelect: (ContractType) -> Unit,
) {
    Text(stringResource(Res.string.field_contract), style = MaterialTheme.typography.labelMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        ContractType.entries.forEach { contract ->
            FilterChip(selected = selected == contract, enabled = enabled, onClick = {
                onSelect(contract)
            }, label = { Text(stringResource(contract.label())) })
        }
    }
}

@Composable
fun RoleChips(
    selected: UserRole,
    enabled: Boolean,
    onSelect: (UserRole) -> Unit,
) {
    Text(stringResource(Res.string.field_role), style = MaterialTheme.typography.labelMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        StaffLimits.ASSIGNABLE_ROLES.forEach { role ->
            FilterChip(
                selected = selected == role,
                enabled = enabled,
                onClick = { onSelect(role) },
                label = { Text(stringResource(role.label())) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartDateField(
    date: LocalDate?,
    enabled: Boolean,
    onDate: (LocalDate) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    Text(stringResource(Res.string.field_start_date), style = MaterialTheme.typography.labelMedium)
    AppButton(text = date?.label() ?: stringResource(Res.string.choose_date), onClick = {
        picking = true
    }, enabled = enabled, style = AppButtonStyle.SECONDARY)
    if (picking) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = date?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    picking = false
                    picker.selectedDateMillis?.let { onDate(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date) }
                }) { Text(stringResource(Res.string.done)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(Res.string.cancel)) } },
        ) { DatePicker(state = picker) }
    }
}

/**
 * The temporary password, once (research D4). Hidden until asked for, so it does not sit on screen
 * for anyone looking over a shoulder; copyable to hand it over.
 */
@Suppress("DEPRECATION") // LocalClipboard needs a platform ClipEntry; plain text is all this needs.
@Composable
fun CredentialsSheet(credentials: TemporaryCredentials, personName: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var visible by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    AppBottomSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(Res.string.credentials_title, personName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(stringResource(Res.string.credentials_email, credentials.email), style = MaterialTheme.typography.bodyMedium)
            if (!credentials.recordCreated) {
                Text(
                    stringResource(Res.string.credentials_linked),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.extended.textSecondary,
                )
            }
            Text(stringResource(Res.string.credentials_password), style = MaterialTheme.typography.labelMedium)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.extended.secondaryFill,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = if (visible) credentials.password else "•".repeat(credentials.password.length),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                    )
                    TextButton(onClick = {
                        visible = !visible
                    }) { Text(stringResource(if (visible) Res.string.credentials_hide else Res.string.credentials_show)) }
                }
            }
            Text(
                stringResource(Res.string.credentials_once),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            AppButton(
                text = stringResource(if (copied) Res.string.credentials_copied else Res.string.credentials_copy),
                onClick = {
                    clipboard.setText(AnnotatedString(credentials.password))
                    copied = true
                },
                style = AppButtonStyle.SECONDARY,
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton(text = stringResource(Res.string.done), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}
