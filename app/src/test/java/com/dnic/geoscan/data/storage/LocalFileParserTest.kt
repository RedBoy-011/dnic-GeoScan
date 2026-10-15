package com.dnic.geoscan.data.storage

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

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
}
