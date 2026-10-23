package com.mynxx.photoshop

import com.mynxx.photoshop.filters.FilterManager
import org.junit.Assert.assertNotNull
import org.junit.Test

class FilterTest {

    @Test
    fun testCombinedColorMatrixCreation() {
        val matrix = FilterManager.createCombinedColorMatrix(
            preset = FilterManager.Preset.SEPIA,
            brightness = 10f,
            contrast = 1.2f,
            saturation = 1.1f
        )
        assertNotNull(matrix)
        assertNotNull(matrix.array)
    }

    @Test
    fun testPresets() {
        for (preset in FilterManager.Preset.values()) {
            val m = FilterManager.createCombinedColorMatrix(preset = preset)
            assertNotNull(m)
        }
    }
}
