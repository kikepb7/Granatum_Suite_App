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

val LightExtendedColors = ExtendedColors(
    primaryHover = GranatumCoralDeep,
    destructiveHover = AppRed600,
    destructiveSecondaryOutline = AppRed200,
    disabledOutline = AppBase200,
    disabledFill = AppBase150,
    successOutline = GranatumLeafLight,
    success = GranatumLeaf,
    onSuccess = AppBase0,
    secondaryFill = GranatumMist,

    textPrimary = GranatumTeal,
    textTertiary = GranatumStone,
    textSecondary = GranatumTealMid,
    textPlaceholder = GranatumStone,
    textDisabled = AppBase400,

    surfaceLower = GranatumBone,
    surfaceHigher = AppBase0,
    surfaceOutline = AppBase1000Alpha14,
    overlay = AppBase1000Alpha80,

    accentBlue = AppBlue,
    accentPurple = AppPurple,
    accentViolet = AppViolet,
    accentPink = AppPink,
    accentOrange = AppOrange,
    accentYellow = AppYellow,
    accentGreen = AppGreen,
    accentTeal = AppTeal,
    accentLightBlue = AppLightBlue,
    accentGrey = AppGrey,

    yellowCardBackground = AppYellowCardBgLight,
    yellowCardText = AppYellowCardTextLight,
    redCardBackground = AppRedCardBgLight,
    redCardText = AppRedCardTextLight,

    cakeViolet = AppCakeLightViolet,
    cakeGreen = AppCakeLightGreen,
    cakeBlue = AppCakeLightBlue,
    cakePink = AppCakeLightPink,
    cakeOrange = AppCakeLightOrange,
    cakeYellow = AppCakeLightYellow,
    cakeTeal = AppCakeLightTeal,
    cakePurple = AppCakeLightPurple,
    cakeRed = AppCakeLightRed,
    cakeMint = AppCakeLightMint,
)

val DarkExtendedColors = ExtendedColors(
    primaryHover = GranatumCoralDeep,
    destructiveHover = AppRed600,
    destructiveSecondaryOutline = AppRed200,
    disabledOutline = AppBase900,
    disabledFill = GranatumNightSurface,
    successOutline = GranatumLeafDarkAlpha40,
    success = GranatumLeafDark,
    onSuccess = GranatumNight,
    secondaryFill = GranatumTealMid,

    textPrimary = GranatumBoneWarm,
    textTertiary = AppBase200,
    textSecondary = AppBase150,
    textPlaceholder = AppBase400,
    textDisabled = AppBase500,

    surfaceLower = GranatumNight,
    surfaceHigher = GranatumNightSurface,
    surfaceOutline = AppBase100Alpha10Alt,
    overlay = AppBase1000Alpha80,

    accentBlue = AppBlue,
    accentPurple = AppPurple,
    accentViolet = AppViolet,
    accentPink = AppPink,
    accentOrange = AppOrange,
    accentYellow = AppYellow,
    accentGreen = AppGreen,
    accentTeal = AppTeal,
    accentLightBlue = AppLightBlue,
    accentGrey = AppGrey,

    yellowCardBackground = AppYellowCardBgDark,
    yellowCardText = AppYellowCardTextDark,
    redCardBackground = AppRedCardBgDark,
    redCardText = AppRedCardTextDark,

    cakeViolet = AppCakeDarkViolet,
    cakeGreen = AppCakeDarkGreen,
    cakeBlue = AppCakeDarkBlue,
    cakePink = AppCakeDarkPink,
    cakeOrange = AppCakeDarkOrange,
    cakeYellow = AppCakeDarkYellow,
    cakeTeal = AppCakeDarkTeal,
    cakePurple = AppCakeDarkPurple,
    cakeRed = AppCakeDarkRed,
    cakeMint = AppCakeDarkMint,
)

val LightColorScheme = lightColorScheme(
    primary = GranatumCoral,
    onPrimary = GranatumTeal,
    primaryContainer = GranatumPulp,
    onPrimaryContainer = GranatumCoralDeep,

    secondary = GranatumStone,
    onSecondary = AppBase0,
    secondaryContainer = GranatumMist,
    onSecondaryContainer = GranatumTeal,

    tertiary = GranatumCoralDeep,
    onTertiary = AppBase0,
    tertiaryContainer = GranatumPulp,
    onTertiaryContainer = GranatumTeal,

    error = GranatumRuby,
    onError = AppBase0,
    errorContainer = AppRed200,
    onErrorContainer = AppRed600,

    background = GranatumTeal,
    onBackground = GranatumBone,
    surface = AppBase0,
    onSurface = GranatumTeal,
    surfaceVariant = GranatumBone,
    onSurfaceVariant = GranatumTealMid,

    outline = AppBase1000Alpha8,
    outlineVariant = AppBase200,
)

val DarkColorScheme = darkColorScheme(
    primary = GranatumCoral,
    onPrimary = GranatumTeal,
    primaryContainer = GranatumTealMid,
    onPrimaryContainer = GranatumCoral,

    secondary = AppBase400,
    onSecondary = AppBase1000,
    secondaryContainer = GranatumTealMid,
    onSecondaryContainer = AppBase150,

    tertiary = GranatumCoral,
    onTertiary = GranatumTeal,
    tertiaryContainer = GranatumTealMid,
    onTertiaryContainer = GranatumCoral,

    error = GranatumRubyDark,
    onError = GranatumNight,
    errorContainer = AppRed600,
    onErrorContainer = AppRed200,

    background = GranatumNight,
    onBackground = GranatumBoneWarm,
    surface = GranatumNightSurface,
    onSurface = GranatumBoneWarm,
    surfaceVariant = GranatumTealMid,
    onSurfaceVariant = AppBase150,

    outline = AppBase100Alpha10,
    outlineVariant = AppBase800,
)