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
    primaryHover = AppBrand600,
    destructiveHover = AppRed600,
    destructiveSecondaryOutline = AppRed200,
    disabledOutline = AppBase200,
    disabledFill = AppBase150,
    successOutline = AppBrand100,
    success = AppBrand600,
    onSuccess = AppBase0,
    secondaryFill = AppBase100,

    textPrimary = AppBase1000,
    textTertiary = AppBase800,
    textSecondary = AppBase900,
    textPlaceholder = AppBase700,
    textDisabled = AppBase400,

    surfaceLower = AppBase100,
    surfaceHigher = AppBase100,
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
    primaryHover = AppBrand600,
    destructiveHover = AppRed600,
    destructiveSecondaryOutline = AppRed200,
    disabledOutline = AppBase900,
    disabledFill = AppBase1000,
    successOutline = AppBrand500Alpha40,
    success = AppBrand500,
    onSuccess = AppBase1000,
    secondaryFill = AppBase900,

    textPrimary = AppBase0,
    textTertiary = AppBase200,
    textSecondary = AppBase150,
    textPlaceholder = AppBase400,
    textDisabled = AppBase500,

    surfaceLower = AppBase1000,
    surfaceHigher = AppBase900,
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
    primary = AppBrand500,
    onPrimary = AppBrand1000,
    primaryContainer = AppBrand100,
    onPrimaryContainer = AppBrand900,

    secondary = AppBase700,
    onSecondary = AppBase0,
    secondaryContainer = AppBase100,
    onSecondaryContainer = AppBase900,

    tertiary = AppBrand900,
    onTertiary = AppBase0,
    tertiaryContainer = AppBrand100,
    onTertiaryContainer = AppBrand1000,

    error = AppRed500,
    onError = AppBase0,
    errorContainer = AppRed200,
    onErrorContainer = AppRed600,

    background = AppBrand1000,
    onBackground = AppBase0,
    surface = AppBase0,
    onSurface = AppBase1000,
    surfaceVariant = AppBase100,
    onSurfaceVariant = AppBase900,

    outline = AppBase1000Alpha8,
    outlineVariant = AppBase200,
)

val DarkColorScheme = darkColorScheme(
    primary = AppBrand500,
    onPrimary = AppBrand1000,
    primaryContainer = AppBrand900,
    onPrimaryContainer = AppBrand500,

    secondary = AppBase400,
    onSecondary = AppBase1000,
    secondaryContainer = AppBase900,
    onSecondaryContainer = AppBase150,

    tertiary = AppBrand500,
    onTertiary = AppBase1000,
    tertiaryContainer = AppBrand900,
    onTertiaryContainer = AppBrand500,

    error = AppRed500,
    onError = AppBase0,
    errorContainer = AppRed600,
    onErrorContainer = AppRed200,

    background = AppBase1000,
    onBackground = AppBase0,
    surface = AppBase950,
    onSurface = AppBase0,
    surfaceVariant = AppBase900,
    onSurfaceVariant = AppBase150,

    outline = AppBase100Alpha10,
    outlineVariant = AppBase800,
)