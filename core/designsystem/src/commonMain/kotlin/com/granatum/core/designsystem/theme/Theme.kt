package com.granatum.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

val ColorScheme.extended: ExtendedColors
    @ReadOnlyComposable
    @Composable
    get() = LocalExtendedColors.current

@Immutable
data class ExtendedColors(
    // Button states
    val primaryHover: Color,
    val destructiveHover: Color,
    val destructiveSecondaryOutline: Color,
    val disabledOutline: Color,
    val disabledFill: Color,
    val successOutline: Color,
    val success: Color,
    val onSuccess: Color,
    val secondaryFill: Color,

    // Text variants
    val textPrimary: Color,
    val textTertiary: Color,
    val textSecondary: Color,
    val textPlaceholder: Color,
    val textDisabled: Color,

    // Surface variants
    val surfaceLower: Color,
    val surfaceHigher: Color,
    val surfaceOutline: Color,
    val overlay: Color,

    // Accent colors
    val accentBlue: Color,
    val accentPurple: Color,
    val accentViolet: Color,
    val accentPink: Color,
    val accentOrange: Color,
    val accentYellow: Color,
    val accentGreen: Color,
    val accentTeal: Color,
    val accentLightBlue: Color,
    val accentGrey: Color,

    // Disciplinary card colors
    val yellowCardBackground: Color,
    val yellowCardText: Color,
    val redCardBackground: Color,
    val redCardText: Color,

    // Extra accent colors (rename/reuse as needed)
    val cakeViolet: Color,
    val cakeGreen: Color,
    val cakeBlue: Color,
    val cakePink: Color,
    val cakeOrange: Color,
    val cakeYellow: Color,
    val cakeTeal: Color,
    val cakePurple: Color,
    val cakeRed: Color,
    val cakeMint: Color,
)

private val LightBrandSoft = Color(0xFFFFE9EA)
private val LightDangerSoft = Color(0xFFF8DDE1)
private val LightSuccessSoft = Color(0xFFDDF0E8)
private val LightWarningSoft = Color(0xFFFFF0D6)
private val LightInfoSoft = Color(0xFFDCEEF4)
private val LightNeutralSoft = Color(0x14002C39)
private val DarkBrandSoft = Color(0x2EFF636C)
private val DarkDangerSoft = Color(0x2EFF8D92)
private val DarkSuccessSoft = Color(0x2E62D6A8)
private val DarkWarningSoft = Color(0x2EFFC266)
private val DarkInfoSoft = Color(0x2E7CC7DD)
private val DarkNeutralSoft = Color(0x1FF7F4F5)

val LightExtendedColors = ExtendedColors(
    primaryHover = CoralDeep,
    destructiveHover = DangerLight,
    destructiveSecondaryOutline = DangerLight,
    disabledOutline = Color(0x1F002C39),
    disabledFill = WellLight,
    successOutline = LightSuccessSoft,
    success = SuccessLight,
    onSuccess = BrandWhite,
    secondaryFill = WellLight,

    textPrimary = BrandPetroleum,
    textTertiary = InkTertiary,
    textSecondary = InkSecondary,
    textPlaceholder = InkTertiary,
    textDisabled = InkDisabled,

    surfaceLower = CanvasLight,
    surfaceHigher = BrandWhite,
    surfaceOutline = Color(0x1A002C39),
    overlay = Color(0xCC002C39),

    accentBlue = LightInfoSoft,
    accentPurple = LightBrandSoft,
    accentViolet = LightBrandSoft,
    accentPink = LightBrandSoft,
    accentOrange = LightWarningSoft,
    accentYellow = LightWarningSoft,
    accentGreen = LightSuccessSoft,
    accentTeal = LightInfoSoft,
    accentLightBlue = LightInfoSoft,
    accentGrey = LightNeutralSoft,

    yellowCardBackground = LightWarningSoft,
    yellowCardText = WarningLight,
    redCardBackground = LightDangerSoft,
    redCardText = DangerLight,

    cakeViolet = LightBrandSoft,
    cakeGreen = LightSuccessSoft,
    cakeBlue = LightInfoSoft,
    cakePink = LightBrandSoft,
    cakeOrange = LightWarningSoft,
    cakeYellow = LightWarningSoft,
    cakeTeal = LightInfoSoft,
    cakePurple = LightBrandSoft,
    cakeRed = LightDangerSoft,
    cakeMint = LightSuccessSoft,
)

val DarkExtendedColors = ExtendedColors(
    primaryHover = CoralLight,
    destructiveHover = DangerDark,
    destructiveSecondaryOutline = DangerDark,
    disabledOutline = Color(0x1FF7F4F5),
    disabledFill = PetroleumDeep,
    successOutline = DarkSuccessSoft,
    success = SuccessDark,
    onSuccess = PetroleumNight,
    secondaryFill = PetroleumLift,

    textPrimary = PaperPrimary,
    textTertiary = PaperTertiary,
    textSecondary = PaperSecondary,
    textPlaceholder = PaperTertiary,
    textDisabled = PaperDisabled,

    surfaceLower = PetroleumNight,
    surfaceHigher = BrandPetroleum,
    surfaceOutline = Color(0x1FF7F4F5),
    overlay = Color(0xCC001116),

    accentBlue = DarkInfoSoft,
    accentPurple = DarkBrandSoft,
    accentViolet = DarkBrandSoft,
    accentPink = DarkBrandSoft,
    accentOrange = DarkWarningSoft,
    accentYellow = DarkWarningSoft,
    accentGreen = DarkSuccessSoft,
    accentTeal = DarkInfoSoft,
    accentLightBlue = DarkInfoSoft,
    accentGrey = DarkNeutralSoft,

    yellowCardBackground = DarkWarningSoft,
    yellowCardText = WarningDark,
    redCardBackground = DarkDangerSoft,
    redCardText = DangerDark,

    cakeViolet = DarkBrandSoft,
    cakeGreen = DarkSuccessSoft,
    cakeBlue = DarkInfoSoft,
    cakePink = DarkBrandSoft,
    cakeOrange = DarkWarningSoft,
    cakeYellow = DarkWarningSoft,
    cakeTeal = DarkInfoSoft,
    cakePurple = DarkBrandSoft,
    cakeRed = DarkDangerSoft,
    cakeMint = DarkSuccessSoft,
)

val LightColorScheme = lightColorScheme(
    // Deep coral so Material text and controls reach AA on white; fills use the pure coral.
    primary = CoralDeep,
    onPrimary = BrandWhite,
    primaryContainer = LightBrandSoft,
    onPrimaryContainer = CoralDeep,

    secondary = InkTertiary,
    onSecondary = BrandWhite,
    secondaryContainer = WellLight,
    onSecondaryContainer = BrandPetroleum,

    tertiary = CoralDeep,
    onTertiary = BrandWhite,
    tertiaryContainer = LightBrandSoft,
    onTertiaryContainer = BrandPetroleum,

    error = DangerLight,
    onError = BrandWhite,
    errorContainer = LightDangerSoft,
    onErrorContainer = DangerLight,

    // The brand ground: petróleo behind the login, the splash and the adaptive layouts.
    background = BrandPetroleum,
    onBackground = PaperPrimary,
    surface = BrandWhite,
    onSurface = BrandPetroleum,
    surfaceVariant = CanvasLight,
    onSurfaceVariant = InkSecondary,

    outline = Color(0x1A002C39),
    outlineVariant = Color(0x33002C39),
)

val DarkColorScheme = darkColorScheme(
    primary = BrandCoral,
    onPrimary = BrandPetroleum,
    primaryContainer = PetroleumLift,
    onPrimaryContainer = BrandCoral,

    secondary = PaperTertiary,
    onSecondary = PetroleumNight,
    secondaryContainer = PetroleumLift,
    onSecondaryContainer = PaperSecondary,

    tertiary = BrandCoral,
    onTertiary = BrandPetroleum,
    tertiaryContainer = PetroleumLift,
    onTertiaryContainer = BrandCoral,

    error = DangerDark,
    onError = PetroleumNight,
    errorContainer = DarkDangerSoft,
    onErrorContainer = DangerDark,

    background = PetroleumNight,
    onBackground = PaperPrimary,
    surface = BrandPetroleum,
    onSurface = PaperPrimary,
    surfaceVariant = PetroleumLift,
    onSurfaceVariant = PaperSecondary,

    outline = Color(0x1FF7F4F5),
    outlineVariant = Color(0x3DF7F4F5),
)
