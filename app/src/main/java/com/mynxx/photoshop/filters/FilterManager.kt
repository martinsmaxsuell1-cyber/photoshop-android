package com.mynxx.photoshop.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

object FilterManager {

    enum class Preset {
        ORIGINAL,
        GRAYSCALE,
        SEPIA,
        VINTAGE,
        INVERT,
        WARM,
        COOL
    }

    /**
     * Builds a comprehensive ColorMatrix combining preset and fine adjustments
     * (brightness: -100..100, contrast: 0.5..2.0, saturation: 0.0..2.0).
     */
    fun createCombinedColorMatrix(
        preset: Preset = Preset.ORIGINAL,
        brightness: Float = 0f,
        contrast: Float = 1f,
        saturation: Float = 1f
    ): ColorMatrix {
        val resultMatrix = ColorMatrix()

        // 1. Apply Preset Base
        when (preset) {
            Preset.ORIGINAL -> {
                resultMatrix.reset()
            }
            Preset.GRAYSCALE -> {
                resultMatrix.setSaturation(0f)
            }
            Preset.SEPIA -> {
                val sepia = ColorMatrix(
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f,     0f,     0f,     1f, 0f
                    )
                )
                resultMatrix.set(sepia)
            }
            Preset.VINTAGE -> {
                val vintage = ColorMatrix(
                    floatArrayOf(
                        0.9f, 0.5f, 0.1f, 0f, 10f,
                        0.3f, 0.8f, 0.1f, 0f, 5f,
                        0.2f, 0.3f, 0.5f, 0f, 0f,
                        0f,   0f,   0f,   1f, 0f
                    )
                )
                resultMatrix.set(vintage)
            }
            Preset.INVERT -> {
                val invert = ColorMatrix(
                    floatArrayOf(
                        -1f,  0f,  0f, 0f, 255f,
                         0f, -1f,  0f, 0f, 255f,
                         0f,  0f, -1f, 0f, 255f,
                         0f,  0f,  0f, 1f,   0f
                    )
                )
                resultMatrix.set(invert)
            }
            Preset.WARM -> {
                val warm = ColorMatrix(
                    floatArrayOf(
                        1.2f, 0f,   0f,   0f, 20f,
                        0f,   1.0f, 0f,   0f, 10f,
                        0f,   0f,   0.8f, 0f, 0f,
                        0f,   0f,   0f,   1f, 0f
                    )
                )
                resultMatrix.set(warm)
            }
            Preset.COOL -> {
                val cool = ColorMatrix(
                    floatArrayOf(
                        0.8f, 0f,   0f,   0f, 0f,
                        0f,   1.0f, 0f,   0f, 10f,
                        0f,   0f,   1.2f, 0f, 25f,
                        0f,   0f,   0f,   1f, 0f
                    )
                )
                resultMatrix.set(cool)
            }
        }

        // 2. Fine Adjustments: Contrast & Brightness
        val adjMatrix = ColorMatrix()
        val scale = contrast
        val translate = (1f - contrast) * 128f + brightness

        adjMatrix.set(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        resultMatrix.postConcat(adjMatrix)

        // 3. Fine Adjustments: Saturation (if not original or grayscale)
        if (saturation != 1f && preset != Preset.GRAYSCALE) {
            val satMatrix = ColorMatrix()
            satMatrix.setSaturation(saturation)
            resultMatrix.postConcat(satMatrix)
        }

        return resultMatrix
    }

    /**
     * Bakes the ColorMatrix into a new filtered Bitmap.
     */
    fun applyMatrixToBitmap(source: Bitmap, colorMatrix: ColorMatrix): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}
