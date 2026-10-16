package com.granatum.core.designsystem.theme

import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.chips.toneContent
import com.granatum.core.designsystem.components.chips.toneSoft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AppToneTest {

    @Test
    fun toneContentMapsEveryToneToItsSemanticColour() {
        with(LightAppColors) {
            assertEquals(textSecondary, toneContent(AppTone.NEUTRAL))
            assertEquals(brand, toneContent(AppTone.BRAND))
            assertEquals(success, toneContent(AppTone.SUCCESS))
            assertEquals(warning, toneContent(AppTone.WARNING))
            assertEquals(danger, toneContent(AppTone.DANGER))
            assertEquals(info, toneContent(AppTone.INFO))
        }
    }

    @Test
    fun toneSoftMapsEveryToneToItsSoftColour() {
        with(DarkAppColors) {
            assertEquals(neutralSoft, toneSoft(AppTone.NEUTRAL))
            assertEquals(brandSoft, toneSoft(AppTone.BRAND))
            assertEquals(successSoft, toneSoft(AppTone.SUCCESS))
            assertEquals(warningSoft, toneSoft(AppTone.WARNING))
            assertEquals(dangerSoft, toneSoft(AppTone.DANGER))
            assertEquals(infoSoft, toneSoft(AppTone.INFO))
        }
    }

    @Test
    fun lightAndDarkPalettesAreFlaggedAndDiffer() {
        assertFalse(LightAppColors.isDark)
        assertTrue(DarkAppColors.isDark)
        assertNotEquals(LightAppColors.background, DarkAppColors.background)
    }

    @Test
    fun spacingShapesAndElevationExposeTheirDefaults() {
        assertEquals(20f, AppSpacing().screenHorizontal.value)
        assertEquals(4f, AppSpacing().xxs.value)
        assertEquals(0f, AppElevation().none.value)
        assertNotEquals(AppShapes().small, AppShapes().large)
    }

    @Test
    fun gradientsAreBuiltFromThePalette() {
        // Brush has no structural equality; reaching the getters is what is covered.
        LightAppColors.brandGradient
        LightAppColors.heroGradient
        LightAppColors.cardBorder
    }
}
