package com.granatum.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// The brand kit (granatum-branding/LEEME.md) defines exactly three colours. Everything else in
// the app is a tint, a shade or an alpha mix of them.
val BrandCoral = Color(0xFFFF636C)
val BrandPetroleum = Color(0xFF002C39)
val BrandWhite = Color(0xFFFFFFFF)

// Coral, lightened with white (gradient end, soft highlights) and deepened for small text on
// light grounds (AA on white and on the soft coral tint: 5.5 and 4.9:1; plain coral on white is only 2.9:1).
val CoralLight = Color(0xFFFF8F95)
val CoralDeep = Color(0xFFC2303E)

// Petróleo scale, from the darkest ground to the lifted tones used for cards in dark mode.
val PetroleumNight = Color(0xFF001A22) // 40% darker: dark canvas and splash
val PetroleumDeep = Color(0xFF001116) // wells and sunken fields in dark mode
val PetroleumLift = Color(0xFF143F4B) // raised surfaces in dark mode
val PetroleumMid = Color(0xFF0E4658) // hero gradient end

// Light grounds: white tinted by 3% coral and 3% petróleo, and the same mixed deeper for wells.
val CanvasLight = Color(0xFFF7F4F5)
val WellLight = Color(0xFFE8EDEF)

// Text tints. Petróleo over white at 100/78/70/35% and white over petróleo at 100/78/58/35%.
val InkSecondary = Color(0xFF385A65)
val InkTertiary = Color(0xFF4C6B74)
val InkDisabled = Color(0xFFA6B5BA)
val PaperPrimary = Color(0xFFF7F4F5)
val PaperSecondary = Color(0xFFC7D1D4)
val PaperTertiary = Color(0xFF94A7AC)
val PaperDisabled = Color(0xFF59767E)

// Functional hues, harmonised with the brand: danger a crimson cousin of the coral (darker and
// more saturated, so it never reads as the accent), success a deep leaf, warning amber, info a
// teal derived from the petróleo.
val DangerLight = Color(0xFFC62839)
val DangerDark = Color(0xFFFF8D92)
val SuccessLight = Color(0xFF1B7358)
val SuccessDark = Color(0xFF62D6A8)
val WarningLight = Color(0xFF9A5B00)
val WarningDark = Color(0xFFFFC266)
val InfoLight = Color(0xFF1F6F8B)
val InfoDark = Color(0xFF7CC7DD)
