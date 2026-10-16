package com.granatum.core.designsystem.components.chips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.theme.AppColors
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.caption
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Meaning of a chip or banner; the colours come from the theme so dark mode follows. */
enum class AppTone { NEUTRAL, BRAND, SUCCESS, WARNING, DANGER, INFO }

fun AppColors.toneContent(tone: AppTone): Color = when (tone) {
    AppTone.NEUTRAL -> textSecondary
    AppTone.BRAND -> brand
    AppTone.SUCCESS -> success
    AppTone.WARNING -> warning
    AppTone.DANGER -> danger
    AppTone.INFO -> info
}

fun AppColors.toneSoft(tone: AppTone): Color = when (tone) {
    AppTone.NEUTRAL -> neutralSoft
    AppTone.BRAND -> brandSoft
    AppTone.SUCCESS -> successSoft
    AppTone.WARNING -> warningSoft
    AppTone.DANGER -> dangerSoft
    AppTone.INFO -> infoSoft
}

/** A small pill: a status, a flag, a count. With [showDot] it leads with a coloured dot. */
@Composable
fun AppStatusChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: AppTone = AppTone.NEUTRAL,
    showDot: Boolean = false
) {
    val content = AppTheme.colors.toneContent(tone)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(AppTheme.shapes.pill)
            .background(AppTheme.colors.toneSoft(tone))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        if (showDot) Box(Modifier.size(6.dp).background(content, CircleShape))
        Text(
            text = text,
            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = content
        )
    }
}

@Preview
@Composable
private fun AppStatusChipDarkPreview() {
    AppTheme(darkTheme = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppStatusChip("Confirmada", tone = AppTone.SUCCESS, showDot = true)
            AppStatusChip("Pendiente", tone = AppTone.WARNING)
            AppStatusChip("Rechazado", tone = AppTone.DANGER)
        }
    }
}
