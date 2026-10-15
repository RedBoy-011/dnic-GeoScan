package com.denic.geoscan.data.analysis

import com.denic.geoscan.data.model.ScanPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeophysicalGradientFilterTest {

    private lateinit var filter: GeophysicalGradientFilter

    @Before
    fun setUp() {
        filter = GeophysicalGradientFilter()
    }

    @Test
    fun testRawNormalizedReturnsIdenticalPoints() {
        val points = listOf(
            ScanPoint(0, 0, 10f),
            ScanPoint(1, 0, 20f),
            ScanPoint(0, 1, 15f),
            ScanPoint(1, 1, 25f)
        )
        val result = filter.applyFilter(points, GeophysicalFilterType.RAW_NORMALIZED)
        assertEquals(points.size, result.size)
        assertEquals(10f, result[0].zValue, 0.001f)
    }

    @Test
    fun testFirstVerticalDerivativeEnhancesCenterAnomaly() {
        // 3x3 grid with high center peak at (1, 1)
        val points = listOf(
            ScanPoint(0, 0, 10f), ScanPoint(1, 0, 10f), ScanPoint(2, 0, 10f),
            ScanPoint(0, 1, 10f), ScanPoint(1, 1, 50f), ScanPoint(2, 1, 10f),
            ScanPoint(0, 2, 10f), ScanPoint(1, 2, 10f), ScanPoint(2, 2, 10f)
        )
        val result = filter.applyFilter(points, GeophysicalFilterType.FIRST_VERTICAL_DERIVATIVE)
        assertEquals(9, result.size)

        // Center point (1, 1): 4*50 - (10+10+10+10) = 200 - 40 = 160
        val center = result.first { it.x == 1 && it.y == 1 }
        assertEquals(160f, center.zValue, 0.01f)
    }

    @Test
    fun testAnalyticSignalComputesGradientMagnitude() {
        val points = listOf(
            ScanPoint(0, 0, 10f), ScanPoint(1, 0, 20f), ScanPoint(2, 0, 30f),
            ScanPoint(0, 1, 10f), ScanPoint(1, 1, 20f), ScanPoint(2, 1, 30f),
            ScanPoint(0, 2, 10f), ScanPoint(1, 2, 20f), ScanPoint(2, 2, 30f)
        )
        val result = filter.applyFilter(points, GeophysicalFilterType.ANALYTIC_SIGNAL)
        assertEquals(9, result.size)

        // Center point has horizontal gradient: dx = (30 - 10) / 2 = 10, dy = (20 - 20) / 2 = 0 -> AS = 10
        val center = result.first { it.x == 1 && it.y == 1 }
        assertEquals(10f, center.zValue, 0.01f)
        assertTrue(center.zValue >= 0f)
    }
}
