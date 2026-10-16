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

        val appClass = DenicApplication::class.java
        assertNotNull(appClass)
        assertEquals("com.denic.geoscan.DenicApplication", appClass.name)

        val diagClass = DiagnosticActivity::class.java
        assertNotNull(diagClass)
        assertEquals("com.denic.geoscan.DiagnosticActivity", diagClass.name)
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
        assertEquals(300f, dataset.statistics.groundZeroMedian, 0.001f)
        assertEquals(2, dataset.statistics.cavityCount) // 100, 200 below 300
        assertEquals(2, dataset.statistics.metalCount)  // 400, 500 above 300
        assertEquals(1, dataset.statistics.neutralCount) // 300 at baseline
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

    @Test
    fun testMagneticCrossSectionExtraction() {
        val grid = listOf(
            ScanPoint(0, 0, 10f), ScanPoint(1, 0, 20f), ScanPoint(2, 0, 30f),
            ScanPoint(0, 1, 40f), ScanPoint(1, 1, 95f), ScanPoint(2, 1, 50f),
            ScanPoint(0, 2, 60f), ScanPoint(1, 2, 70f), ScanPoint(2, 2, 80f)
        )
        // Extract row Y=1 transect
        val row1 = grid.filter { it.y == 1 }.sortedBy { it.x }
        assertEquals(3, row1.size)
        assertEquals(40f, row1[0].zValue, 0.001f)
        assertEquals(95f, row1[1].zValue, 0.001f)
        assertEquals(50f, row1[2].zValue, 0.001f)

        // Peak anomaly along transect
        val peak = row1.maxByOrNull { kotlin.math.abs(it.zValue) }?.zValue ?: 0f
        assertEquals(95f, peak, 0.001f)

        // Extract column X=1 transect
        val col1 = grid.filter { it.x == 1 }.sortedBy { it.y }
        assertEquals(3, col1.size)
        assertEquals(20f, col1[0].zValue, 0.001f)
        assertEquals(95f, col1[1].zValue, 0.001f)
        assertEquals(70f, col1[2].zValue, 0.001f)
    }

    @Test
    fun testNavigationScreensNonNullAndValid() {
        val screens = com.denic.geoscan.ui.navigation.Screen.items
        assertNotNull("Screen.items must not be null", screens)
        assertEquals("Screen.items must contain exactly 3 items", 3, screens.size)
        for (screen in screens) {
            assertNotNull("Screen item must not be null", screen)
            assertNotNull("Screen route must not be null", screen.route)
            assertFalse("Screen route must not be blank", screen.route.isBlank())
            assertNotNull("Screen title must not be null", screen.title)
            assertFalse("Screen title must not be blank", screen.title.isBlank())
            assertNotNull("Screen icon must not be null", screen.icon)
        }
        val routes = screens.map { it.route }
        assertEquals("Each route must be unique", routes.distinct().size, routes.size)
    }
}
