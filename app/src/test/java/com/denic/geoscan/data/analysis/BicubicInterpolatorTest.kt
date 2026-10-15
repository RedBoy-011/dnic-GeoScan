package com.denic.geoscan.data.analysis

import com.denic.geoscan.data.model.ScanPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BicubicInterpolatorTest {

    private lateinit var interpolator: BicubicInterpolator

    @Before
    fun setUp() {
        interpolator = BicubicInterpolator(subdivisionFactor = 2)
    }

    @Test
    fun interpolate_tooFewPoints_returnsOriginalPoints() {
        val points = listOf(
            ScanPoint(0, 0, 10f),
            ScanPoint(1, 0, 20f)
        )
        val result = interpolator.interpolate(points)
        assertEquals(points.size, result.size)
    }

    @Test
    fun interpolate_2x2Grid_producesHigherDensityGrid() {
        val points = listOf(
            ScanPoint(0, 0, 10f),
            ScanPoint(1, 0, 20f),
            ScanPoint(0, 1, 10f),
            ScanPoint(1, 1, 20f)
        )
        // With subdivisionFactor = 2:
        // (2-1)*2 + 1 = 3 points wide, (2-1)*2 + 1 = 3 points high -> total 9 points
        val result = interpolator.interpolate(points)
        assertEquals(9, result.size)

        // Midpoint at (1, 0) should be approx (10 + 20) / 2 = 15
        val midPoint = result.first { it.x == 1 && it.y == 0 }
        assertEquals(15f, midPoint.zValue, 0.5f)
    }

    @Test
    fun interpolate_smoothsContinuousGradientWithoutNaN() {
        val points = mutableListOf<ScanPoint>()
        for (y in 0 until 4) {
            for (x in 0 until 4) {
                points.add(ScanPoint(x, y, (x * 10 + y * 5).toFloat()))
            }
        }
        val result = interpolator.interpolate(points)
        assertTrue(result.size > points.size)
        assertTrue(result.none { it.zValue.isNaN() })
    }
}
