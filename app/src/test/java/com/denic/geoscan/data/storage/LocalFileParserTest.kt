package com.denic.geoscan.data.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class LocalFileParserTest {

    private lateinit var parser: LocalFileParser

    @Before
    fun setUp() {
        parser = LocalFileParser()
    }

    @Test
    fun parseLines_tripletCsvWithHeader_extractsPointsCorrectly() {
        val lines = listOf(
            "# Magnetometer Survey Data",
            "X, Y, Z",
            "0, 0, 105.2",
            "0, 1, 108.4",
            "1, 0, 120.0"
        )
        val points = parser.parseLines(lines)
        assertEquals(3, points.size)
        assertEquals(0, points[0].x)
        assertEquals(0, points[0].y)
        assertEquals(105.2f, points[0].zValue, 0.001f)
    }

    @Test
    fun parseLines_packetStrings_extractsPoints() {
        val lines = listOf(
            "$0,0,1050#",
            "$0,1,1060#",
            "$1,0,1100#"
        )
        val points = parser.parseLines(lines)
        assertEquals(3, points.size)
        assertEquals(0, points[1].x)
        assertEquals(1, points[1].y)
        assertEquals(1060f, points[1].zValue, 0.001f)
    }

    @Test
    fun parseLines_matrixGridFormat_extracts2DPoints() {
        val lines = listOf(
            "# 3x2 Matrix",
            "100, 105, 110",
            "102, 108, 120"
        )
        val points = parser.parseLines(lines)
        assertEquals(6, points.size)

        // Point at (0, 0)
        val p00 = points.first { it.x == 0 && it.y == 0 }
        assertEquals(100f, p00.zValue, 0.001f)

        // Point at (2, 1)
        val p21 = points.first { it.x == 2 && it.y == 1 }
        assertEquals(120f, p21.zValue, 0.001f)
    }

    @Test
    fun parseLines_spaceDelimitedGrid_extractsPoints() {
        val lines = listOf(
            "100.5 105.2 110.8",
            "102.1 108.7 120.3"
        )
        val points = parser.parseLines(lines)
        assertEquals(6, points.size)
        assertEquals(100.5f, points[0].zValue, 0.001f)
        assertEquals(120.3f, points[5].zValue, 0.001f)
    }

    @Test
    fun parseLines_utf8BomStrippedProperly() {
        val lines = listOf(
            "\uFEFF0, 0, 1000.5",
            "1, 0, 1002.0"
        )
        val points = parser.parseLines(lines)
        assertEquals(2, points.size)
        assertEquals(0, points[0].x)
        assertEquals(0, points[0].y)
        assertEquals(1000.5f, points[0].zValue, 0.001f)
    }

    @Test
    fun parseLines_persianAndArabicDigitsNormalized() {
        val lines = listOf(
            "۰, ۰, ۱۰۰۵.۵",
            "۱, ۰, ۱۰۱۰.۲",
            "٠, ١, ١٠٠٧.٠"
        )
        val points = parser.parseLines(lines)
        assertEquals(3, points.size)
        assertEquals(0, points[0].x)
        assertEquals(0, points[0].y)
        assertEquals(1005.5f, points[0].zValue, 0.01f)
        assertEquals(1, points[1].x)
        assertEquals(1010.2f, points[1].zValue, 0.01f)
        assertEquals(1, points[2].y)
        assertEquals(1007.0f, points[2].zValue, 0.01f)
    }

    @Test
    fun parseLines_v3dWithComplexMetadataAndCommas() {
        val lines = listOf(
            "; Visualizer 3D Geophysical Data Export",
            "[Header]",
            "Version=3.0",
            "SamplesPerLine=10",
            "ShallowFerrousTarget=X:4,Y:4,Depth:1.2m,Peak:+52uT",
            "UndergroundMineTunnel=Row Y:12,Depth:3.0m,Void:-28uT",
            "[Data]",
            "0, 0, 1000.0",
            "1, 0, 1005.5",
            "2, 0, 1010.0"
        )
        val points = parser.parseLines(lines)
        assertEquals(3, points.size)
        assertEquals(0, points[0].x)
        assertEquals(1000.0f, points[0].zValue, 0.01f)
        assertEquals(1, points[1].x)
        assertEquals(1005.5f, points[1].zValue, 0.01f)
    }

    @Test
    fun parseLines_demo10x10AnomalyFile() {
        val file = File("demo_scans/demo_10x10_anomaly.csv")
        assertTrue("demo_10x10_anomaly.csv must exist", file.exists())
        val points = parser.parseLines(file.readLines())
        assertEquals(100, points.size)
        val peak = points.maxByOrNull { it.zValue }
        assertNotNull(peak)
        assertEquals(3, peak!!.x)
        assertEquals(3, peak.y)
    }

    @Test
    fun parseLines_demo10x10V3dFile() {
        val file = File("demo_scans/demo_10x10_anomaly.v3d")
        assertTrue("demo_10x10_anomaly.v3d must exist", file.exists())
        val points = parser.parseLines(file.readLines())
        assertEquals(100, points.size)
    }

    @Test
    fun parseLines_demoMineMultidepthFile() {
        val file = File("demo_scans/demo_mine_multidepth.csv")
        assertTrue("demo_mine_multidepth.csv must exist", file.exists())
        val points = parser.parseLines(file.readLines())
        assertEquals(225, points.size)
    }

    @Test
    fun parseLines_demoMineMultidepthV3dFile() {
        val file = File("demo_scans/demo_mine_multidepth.v3d")
        assertTrue("demo_mine_multidepth.v3d must exist", file.exists())
        val points = parser.parseLines(file.readLines())
        assertEquals(225, points.size)
    }
}
