package com.denic.geoscan.data.analysis

import com.denic.geoscan.data.model.ScanPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeophysicalDepthEstimatorTest {

    private val depthEstimator = GeophysicalDepthEstimator()

    @Test
    fun testDepthEstimationWithDipoleAnomaly() {
        // Bell-shaped magnetic anomaly curve centered at station 3
        val transect = listOf(
            ScanPoint(0, 0, 5f),
            ScanPoint(1, 0, 20f),
            ScanPoint(2, 0, 50f),
            ScanPoint(3, 0, 100f), // Peak at station 3
            ScanPoint(4, 0, 50f),
            ScanPoint(5, 0, 20f),
            ScanPoint(6, 0, 5f)
        )

        val result = depthEstimator.estimateDepth(transect, stationSpacingMeters = 1.0f)

        assertNotNull(result)
        assertEquals(100f, result!!.peakValue, 0.001f)
        assertEquals(3, result.peakIndex)
        assertFalse(result.isCavity)
        // Half-max = 50f, crossing at station 2 and 4, FWHM ≈ 2.0 stations
        assertEquals(2.0f, result.halfWidthStations, 0.2f)
        // Depth = 0.5 * 2.0 * 1.0m ≈ 1.0 meter
        assertEquals(1.0f, result.estimatedDepthMeters, 0.2f)
    }

    @Test
    fun testDepthEstimationWithCavityAnomaly() {
        // Negative anomaly (void/cavity) centered at station 2
        val transect = listOf(
            ScanPoint(0, 0, -5f),
            ScanPoint(1, 0, -35f),
            ScanPoint(2, 0, -70f), // Cavity peak
            ScanPoint(3, 0, -35f),
            ScanPoint(4, 0, -5f)
        )

        val result = depthEstimator.estimateDepth(transect, stationSpacingMeters = 2.0f)

        assertNotNull(result)
        assertEquals(-70f, result!!.peakValue, 0.001f)
        assertTrue(result.isCavity)
        // With station spacing = 2.0m, depth should scale proportionally
        assertTrue(result.estimatedDepthMeters > 0.5f)
    }

    @Test
    fun testFlatBaselineReturnsNull() {
        val flatTransect = listOf(
            ScanPoint(0, 0, 0.01f),
            ScanPoint(1, 0, 0.02f),
            ScanPoint(2, 0, 0.01f)
        )

        val result = depthEstimator.estimateDepth(flatTransect)
        assertNull(result)
    }
}
