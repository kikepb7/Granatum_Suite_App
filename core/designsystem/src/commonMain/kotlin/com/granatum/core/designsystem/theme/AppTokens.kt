package com.granatum.core.designsystem.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Semantic colours of the Granatum look. Screens read these through `AppTheme.colors` and never
 * hardcode a colour, so light and dark stay in step. Surfaces step up in tiers: [background] is
 * the canvas, [surface] the cards laid on it, [surfaceRaised] what floats above (bars, sheets),
 * [surfaceSunken] the wells (fields, segmented tracks).
 */
@Immutable
data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceSunken: Color,
    val border: Color,
    val borderStrong: Color,
    val scrim: Color,

    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,

    val brand: Color,
    val brandStrong: Color,
    val brandSoft: Color,
    val onBrand: Color,
    val brandGradientStart: Color,
    val brandGradientEnd: Color,

    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val info: Color,
    val infoSoft: Color,
    val neutralSoft: Color,

    /** Translucent raised surface for floating bars. */
    val surfaceGlass: Color,
    /** Top-edge light of a card; fades to [border] down the sides. */
    val highlight: Color,
    /** Soft brand tint used for glows behind hero blocks and shadows. */
    val glow: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color,
    val onHeroMuted: Color,

    /** Tint of the soft layered shadow under cards (light) and of the glass bar. */
    val cardShadow: Color,

    /** The pure brand coral: fills, nodes and gradients. For small text use [brand]. */
    val coral: Color,
    /** The floating bar: petróleo in light (inverted), lifted petróleo glass in dark. */
    val barSurface: Color,
    val onBar: Color,
    val onBarMuted: Color
) {
    /** Diagonal brand gradient for the main action and hero blocks. */
    val brandGradient: Brush
        get() = Brush.linearGradient(listOf(brandGradientStart, brandGradientEnd))

    /** Deep tonal gradient of the hero block (petróleo), the same idea in light and dark. */
    val heroGradient: Brush
        get() = Brush.linearGradient(listOf(heroStart, heroEnd))

    /** Hairline of a card: bright on the top edge, quiet below. */
    val cardBorder: Brush
        get() = Brush.verticalGradient(listOf(highlight, border))
}

@Immutable
data class AppSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    /** Side margin of every screen. */
    val screenHorizontal: Dp = 20.dp,
    /** Room a list leaves at its end so nothing hides under a floating control. */
    val listBottom: Dp = 24.dp
)

@Immutable
data class AppShapes(
    val small: CornerBasedShape = RoundedCornerShape(10.dp),
    val medium: CornerBasedShape = RoundedCornerShape(16.dp),
    val large: CornerBasedShape = RoundedCornerShape(24.dp),
    /** Radius of every card of the app. */
    val card: CornerBasedShape = RoundedCornerShape(26.dp),
    val pill: CornerBasedShape = RoundedCornerShape(percent = 50)
)

@Immutable
data class AppElevation(
    val none: Dp = 0.dp,
    val card: Dp = 10.dp,
    val floating: Dp = 10.dp
)

val LightAppColors = AppColors(
    isDark = false,
    background = CanvasLight,
    surface = BrandWhite,
    surfaceRaised = BrandWhite,
    surfaceSunken = WellLight,
    border = Color(0x1A002C39),
    borderStrong = Color(0x33002C39),
    scrim = Color(0x66002C39),

    textPrimary = BrandPetroleum,
    textSecondary = InkSecondary,
    textTertiary = InkTertiary,
    textDisabled = InkDisabled,

    brand = CoralDeep,
    brandStrong = CoralDeep,
    brandSoft = Color(0xFFFFE9EA),
    onBrand = BrandPetroleum,
    brandGradientStart = BrandCoral,
    brandGradientEnd = CoralLight,

    success = SuccessLight,
    successSoft = Color(0xFFDDF0E8),
    warning = WarningLight,
    warningSoft = Color(0xFFFFF0D6),
    danger = DangerLight,
    dangerSoft = Color(0xFFF8DDE1),
    info = InfoLight,
    infoSoft = Color(0xFFDCEEF4),
    neutralSoft = Color(0x14002C39),

    surfaceGlass = Color(0xE6FFFFFF),
    highlight = Color(0x33002C39),
    glow = Color(0x40FF636C),
    heroStart = BrandPetroleum,
    heroEnd = PetroleumMid,
    onHero = BrandWhite,
    onHeroMuted = Color(0xB3FFFFFF),
    cardShadow = BrandPetroleum,

    coral = BrandCoral,
    barSurface = BrandPetroleum,
    onBar = BrandWhite,
    onBarMuted = Color(0x99FFFFFF)
)

val DarkAppColors = AppColors(
    isDark = true,
    background = PetroleumNight,
    surface = BrandPetroleum,
    surfaceRaised = PetroleumLift,
    surfaceSunken = PetroleumDeep,
    border = Color(0x1FF7F4F5),
    borderStrong = Color(0x3DF7F4F5),
    scrim = Color(0x99001116),

    textPrimary = PaperPrimary,
    textSecondary = PaperSecondary,
    textTertiary = PaperTertiary,
    textDisabled = PaperDisabled,

    brand = BrandCoral,
    brandStrong = CoralLight,
    brandSoft = Color(0x2EFF636C),
    onBrand = BrandPetroleum,
    brandGradientStart = BrandCoral,
    brandGradientEnd = CoralLight,

    success = SuccessDark,
    successSoft = Color(0x2E62D6A8),
    warning = WarningDark,
    warningSoft = Color(0x2EFFC266),
    danger = DangerDark,
    dangerSoft = Color(0x2EFF8D92),
    info = InfoDark,
    infoSoft = Color(0x2E7CC7DD),
    neutralSoft = Color(0x1FF7F4F5),

    surfaceGlass = Color(0xE6143F4B),
    highlight = Color(0x40F7F4F5),
    glow = Color(0x40FF636C),
    heroStart = PetroleumMid,
    heroEnd = BrandPetroleum,
    onHero = PaperPrimary,
    onHeroMuted = Color(0xB3F7F4F5),
    cardShadow = Color(0xFF000000),

    coral = BrandCoral,
    barSurface = Color(0xF2143F4B),
    onBar = PaperPrimary,
    onBarMuted = PaperTertiary
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
val LocalAppShapes = staticCompositionLocalOf { AppShapes() }
val LocalAppElevation = staticCompositionLocalOf { AppElevation() }

/**
 * Entry to the design tokens: `AppTheme.colors.surface`, `AppTheme.spacing.md`... The composable
 * of the same name (AppTheme.kt) installs them; MaterialTheme stays available for the rest.
 */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current

    val spacing: AppSpacing
        @Composable @ReadOnlyComposable get() = LocalAppSpacing.current

    val shapes: AppShapes
        @Composable @ReadOnlyComposable get() = LocalAppShapes.current

    val elevation: AppElevation
        @Composable @ReadOnlyComposable get() = LocalAppElevation.current
}
