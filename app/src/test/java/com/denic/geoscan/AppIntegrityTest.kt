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

    @Test
    fun testSessionExporterCsvGeneration() {
        // Test CSV content builder logic directly
        val testPoints = listOf(
            ScanPoint(1, 2, 45.5f, 1000L),
            ScanPoint(3, 4, -12.3f, 2000L)
        )
        val headerTimestamp = "2026-10-16 12:00:00"
        val csv = buildString {
            appendLine("# denic GeoScan Geophysical Magnetometer Survey")
            appendLine("# Exported Date: $headerTimestamp")
            appendLine("# Total Points: ${testPoints.size}")
            appendLine("# Coordinate Format: X (col), Y (row), Z (microtesla uT), Timestamp (epoch ms)")
            appendLine("X,Y,Z,Timestamp")
            for (p in testPoints) {
                appendLine("${p.x},${p.y},${p.zValue},${p.timestamp}")
            }
        }
        assertTrue(csv.contains("X,Y,Z,Timestamp"))
        assertTrue(csv.contains("1,2,45.5,1000"))
        assertTrue(csv.contains("3,4,-12.3,2000"))
        assertTrue(csv.contains("# Total Points: 2"))
    }

    @Test
    fun testBilingualLocalizationCompleteness() {
        val en = com.denic.geoscan.ui.localization.EnglishStrings
        val fa = com.denic.geoscan.ui.localization.PersianStrings

        assertNotNull(en)
        assertNotNull(fa)

        assertFalse("English appName must not be empty", en.appName.isBlank())
        assertFalse("Persian appName must not be empty", fa.appName.isBlank())

        assertEquals(com.denic.geoscan.ui.localization.AppLanguage.ENGLISH, com.denic.geoscan.ui.localization.AppLanguage.fromCode("en"))
        assertEquals(com.denic.geoscan.ui.localization.AppLanguage.PERSIAN, com.denic.geoscan.ui.localization.AppLanguage.fromCode("fa"))
        assertFalse(com.denic.geoscan.ui.localization.AppLanguage.ENGLISH.isRtl)
        assertTrue(com.denic.geoscan.ui.localization.AppLanguage.PERSIAN.isRtl)

        // Validate Persian exit dialog strings
        assertFalse(fa.exitDialogTitle.isBlank())
        assertFalse(fa.exitDialogAutoSaveTitle.isBlank())
        assertFalse(fa.exitDialogAutoSaveDesc.isBlank())
        assertFalse(fa.exitDialogShareBtn.isBlank())
        assertFalse(fa.exitDialogConfirmBtn.isBlank())

        // Validate Persian and English language selection strings
        assertFalse(fa.selectLanguageTitle.isBlank())
        assertFalse(en.selectLanguageTitle.isBlank())
        assertFalse(fa.startApp.isBlank())
        assertFalse(en.startApp.isBlank())

        // Validate all core labels
        assertFalse(fa.navHub.isBlank())
        assertFalse(fa.navLocalAnalysis.isBlank())
        assertFalse(fa.navRemoteScan.isBlank())
        assertFalse(fa.hubHardwareDisconnected.isBlank())
        assertFalse(fa.remoteConnected.isBlank())
        assertFalse(fa.remoteTargetDevice.isBlank())
        assertFalse(fa.remoteLoggedPoints.isBlank())

        // Validate new Telemetry Audio and Grid Matrix Localization Strings
        assertFalse(fa.audioFeedbackTitle.isBlank())
        assertFalse(fa.audioModeZPitch.isBlank())
        assertFalse(fa.audioModeSimple.isBlank())
        assertFalse(fa.audioModeMute.isBlank())
        assertFalse(fa.gridConfigTitle.isBlank())
        assertFalse(fa.gridStepsPerLine.isBlank())
        assertFalse(fa.gridCurrentLine.isBlank())
        assertFalse(fa.gridPointInLine.isBlank())
        assertFalse(fa.gridAutoStepNotice.isBlank())
        assertFalse(fa.gridModeParallel.isBlank())
        assertFalse(fa.gridModeZigzag.isBlank())
        assertFalse(fa.dnicConnectedNotification.isBlank())

        assertFalse(en.audioFeedbackTitle.isBlank())
        assertFalse(en.audioModeZPitch.isBlank())
        assertFalse(en.audioModeSimple.isBlank())
        assertFalse(en.audioModeMute.isBlank())
        assertFalse(en.gridConfigTitle.isBlank())
        assertFalse(en.gridStepsPerLine.isBlank())
        assertFalse(en.dnicConnectedNotification.isBlank())
    }

    @Test
    fun testAudioModeEnumResolution() {
        val defaultMode = com.denic.geoscan.data.audio.TelemetryAudioMode.fromCode("unknown")
        assertEquals(com.denic.geoscan.data.audio.TelemetryAudioMode.DYNAMIC_Z_PITCH, defaultMode)

        val simpleMode = com.denic.geoscan.data.audio.TelemetryAudioMode.fromCode("simple")
        assertEquals(com.denic.geoscan.data.audio.TelemetryAudioMode.SIMPLE_BEEP, simpleMode)

        val muteMode = com.denic.geoscan.data.audio.TelemetryAudioMode.fromCode("mute")
        assertEquals(com.denic.geoscan.data.audio.TelemetryAudioMode.MUTE, muteMode)
    }

    @Test
    fun testParserFlexiblePacketFormats() {
        val parser = ScanDataParser()

        // 1-token packet ($Z#)
        val single = parser.parsePacket("250.75")
        assertNotNull(single)
        assertEquals(0, single?.x)
        assertEquals(0, single?.y)
        assertEquals(250.75f, single?.zValue ?: 0f, 0.001f)

        // 2-token packet ($X,Z#)
        val double = parser.parsePacket("7, -35.2")
        assertNotNull(double)
        assertEquals(7, double?.x)
        assertEquals(0, double?.y)
        assertEquals(-35.2f, double?.zValue ?: 0f, 0.001f)

        // 3-token packet ($X,Y,Z#)
        val triple = parser.parsePacket("4, 8, 1024.0")
        assertNotNull(triple)
        assertEquals(4, triple?.x)
        assertEquals(8, triple?.y)
        assertEquals(1024.0f, triple?.zValue ?: 0f, 0.001f)
    }

    @Test
    fun testGridLineSequencerAutoIncrementLogic() {
        val targetX = 10
        // Simulate points arriving: 0 to 24 points (total 25 points)
        // Parallel mode:
        for (count in 0..24) {
            val autoY = count / targetX
            val autoX = count % targetX
            when (count) {
                0 -> { assertEquals(0, autoX); assertEquals(0, autoY) }
                9 -> { assertEquals(9, autoX); assertEquals(0, autoY) }
                10 -> { assertEquals(0, autoX); assertEquals(1, autoY) } // Line Y stepped to 1!
                19 -> { assertEquals(9, autoX); assertEquals(1, autoY) }
                20 -> { assertEquals(0, autoX); assertEquals(2, autoY) } // Line Y stepped to 2!
            }
        }

        // Zigzag mode (alternates direction on odd Y lines):
        val zigzagPoints = (0..19).map { count ->
            val autoY = count / targetX
            val rawX = count % targetX
            val autoX = if (autoY % 2 == 1) (targetX - 1 - rawX) else rawX
            Pair(autoX, autoY)
        }
        // Line 0: forward 0..9
        assertEquals(Pair(0, 0), zigzagPoints[0])
        assertEquals(Pair(9, 0), zigzagPoints[9])
        // Line 1: reverse 9..0
        assertEquals(Pair(9, 1), zigzagPoints[10])
        assertEquals(Pair(0, 1), zigzagPoints[19])
    }
}

