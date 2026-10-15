package com.denic.geoscan

import com.denic.geoscan.data.analysis.BicubicInterpolator
import com.denic.geoscan.data.analysis.DatasetNormalizer
import com.denic.geoscan.data.bluetooth.BluetoothService
import com.denic.geoscan.data.model.ScanPoint
import com.denic.geoscan.data.parser.ScanDataParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIntegrityTest {

    @Test
    fun testMainActivityClassPresent() {
        val activityClass = MainActivity::class.java
        assertNotNull(activityClass)
        assertEquals("com.denic.geoscan.MainActivity", activityClass.name)
    }

    @Test
    fun testBluetoothSppUuidIntegrity() {
        assertEquals(
            "00001101-0000-1000-8000-00805f9b34fb",
            BluetoothService.SPP_UUID.toString().lowercase()
        )
    }

    @Test
    fun testBicubicInterpolationStability() {
        val interpolator = BicubicInterpolator(subdivisionFactor = 2)
        val testPoints = listOf(
            ScanPoint(0, 0, 10f), ScanPoint(0, 1, 20f),
            ScanPoint(1, 0, 15f), ScanPoint(1, 1, 25f)
        )
        val interpolated = interpolator.interpolate(testPoints)
        assertTrue(interpolated.isNotEmpty())
        for (pt in interpolated) {
            assertFalse("Interpolated zValue should not be NaN", pt.zValue.isNaN())
            assertFalse("Interpolated zValue should not be Infinite", pt.zValue.isInfinite())
        }
    }

    @Test
    fun testDatasetNormalizationGroundZero() {
        val normalizer = DatasetNormalizer()
        val rawPoints = listOf(
            ScanPoint(0, 0, 100f),
            ScanPoint(0, 1, 200f),
            ScanPoint(1, 0, 300f), // median = 200f
            ScanPoint(1, 1, 400f),
            ScanPoint(2, 0, 500f)
        )
        val dataset = normalizer.normalize(rawPoints, "test.csv", "1 KB")
        assertEquals(200f, dataset.statistics.groundZeroMedian, 0.001f)
        assertEquals(1, dataset.statistics.cavityCount) // 100 is below 200 - threshold
        assertEquals(1, dataset.statistics.metalCount)  // 500 is above 200 + threshold
    }

    @Test
    fun testParserPacketStream() {
        val parser = ScanDataParser()
        val points = parser.parseChunk("\$10,20,1050.5#\n")
        assertEquals(1, points.size)
        assertEquals(10, points[0].x)
        assertEquals(20, points[0].y)
        assertEquals(1050.5f, points[0].zValue, 0.001f)
    }
}
