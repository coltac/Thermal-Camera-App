package com.example.uvcthermal.camera

import android.graphics.Color

enum class ThermalPalette(
    val title: String,
    val subtitle: String,
    val accentColor: Long
) {
    BLACK_HOT(
        title = "Black Hot",
        subtitle = "Darkest areas render hottest",
        accentColor = 0xFF8C98A6
    ),
    WHITE_HOT(
        title = "White Hot",
        subtitle = "Brightest areas render hottest",
        accentColor = 0xFFF4F3EE
    ),
    IRON_HOT(
        title = "Iron Hot",
        subtitle = "Amber-heavy thermal gradient",
        accentColor = 0xFFFF8A3D
    );

    fun colorTable(): IntArray {
        return when (this) {
            IRON_HOT -> IRON_HOT_TABLE
            WHITE_HOT -> WHITE_HOT_TABLE
            BLACK_HOT -> BLACK_HOT_TABLE
        }
    }

    companion object {
        private val WHITE_HOT_TABLE = IntArray(256) { level ->
            Color.argb(255, level, level, level)
        }

        private val BLACK_HOT_TABLE = IntArray(256) { level ->
            val inverted = 255 - level
            Color.argb(255, inverted, inverted, inverted)
        }

        private val IRON_HOT_TABLE = buildGradientTable(
            0 to Color.rgb(2, 4, 10),
            28 to Color.rgb(18, 6, 36),
            72 to Color.rgb(80, 12, 52),
            120 to Color.rgb(156, 28, 22),
            176 to Color.rgb(229, 88, 12),
            224 to Color.rgb(255, 184, 67),
            255 to Color.rgb(255, 248, 234)
        )

        private fun buildGradientTable(vararg anchors: Pair<Int, Int>): IntArray {
            val table = IntArray(256)
            for (index in 0 until anchors.lastIndex) {
                val start = anchors[index]
                val end = anchors[index + 1]
                val startIndex = start.first
                val endIndex = end.first
                val steps = (endIndex - startIndex).coerceAtLeast(1)
                for (offset in 0..steps) {
                    val fraction = offset / steps.toFloat()
                    val red = lerp(Color.red(start.second), Color.red(end.second), fraction)
                    val green = lerp(Color.green(start.second), Color.green(end.second), fraction)
                    val blue = lerp(Color.blue(start.second), Color.blue(end.second), fraction)
                    table[(startIndex + offset).coerceIn(0, 255)] = Color.argb(255, red, green, blue)
                }
            }
            return table
        }

        private fun lerp(start: Int, end: Int, fraction: Float): Int {
            return (start + ((end - start) * fraction)).toInt().coerceIn(0, 255)
        }
    }
}
