package com.vibra.navigation.data.settings

import com.vibra.navigation.data.settings.HapticSettingsRepository.Companion.initialIntensityPercent
import org.junit.Assert.assertEquals
import org.junit.Test

class IntensityMigrationTest {

    @Test
    fun `a saved percentage wins`() {
        assertEquals(40, initialIntensityPercent(savedPercent = 40, legacyLevel = 5))
    }

    @Test
    fun `the old 1 to 5 level is migrated at 20 percent per level`() {
        assertEquals(60, initialIntensityPercent(savedPercent = null, legacyLevel = 3))
        assertEquals(100, initialIntensityPercent(savedPercent = null, legacyLevel = 5))
    }

    @Test
    fun `fresh installs start at full strength`() {
        assertEquals(100, initialIntensityPercent(savedPercent = null, legacyLevel = null))
    }

    @Test
    fun `corrupt values are clamped`() {
        assertEquals(100, initialIntensityPercent(savedPercent = 250, legacyLevel = null))
        assertEquals(0, initialIntensityPercent(savedPercent = -10, legacyLevel = null))
    }
}
