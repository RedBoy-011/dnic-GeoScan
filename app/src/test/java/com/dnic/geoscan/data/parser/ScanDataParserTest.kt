package com.dnic.geoscan.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ScanDataParserTest {

    private lateinit var parser: ScanDataParser

    @Before
    fun setUp() {
        parser = ScanDataParser()
    }

    @Test
    fun parsePacket_validPacket_returnsScanPoint() {
        val point = parser.parsePacket("1,2,1045")
        assertNotNull(point)
        assertEquals(1, point?.x)
        assertEquals(2, point?.y)
        assertEquals(1045f, point?.zValue)
    }

    @Test
    fun parsePacket_floatingZValue_returnsScanPoint() {
        val point = parser.parsePacket("10,20,-48.75")
        assertNotNull(point)
        assertEquals(10, point?.x)
        assertEquals(20, point?.y)
        assertEquals(-48.75f, point?.zValue)
    }

    @Test
    fun parsePacket_malformedPacket_returnsNull() {
        assertNull(parser.parsePacket("1,2"))
        assertNull(parser.parsePacket("1,2,abc"))
        assertNull(parser.parsePacket("1,2,3,4"))
    }

    @Test
    fun parseChunk_singleCompletePacket_returnsListWithOnePoint() {
        val points = parser.parseChunk("$1,2,1045#\n")
        assertEquals(1, points.size)
        assertEquals(1, points[0].x)
        assertEquals(2, points[0].y)
        assertEquals(1045f, points[0].zValue)
    }

    @Test
    fun parseChunk_fragmentedAcrossReads_reassemblesCorrectly() {
        val chunk1Points = parser.parseChunk("noise$5,10,")
        assertEquals(0, chunk1Points.size)

        val chunk2Points = parser.parseChunk("320.5#\r\n")
        assertEquals(1, chunk2Points.size)
        assertEquals(5, chunk2Points[0].x)
        assertEquals(10, chunk2Points[0].y)
        assertEquals(320.5f, chunk2Points[0].zValue)
    }

    @Test
    fun parseChunk_multiplePacketsInSingleChunk_extractsAll() {
        val chunk = "$0,0,100#\n$0,1,110#\n$0,2,120#\n"
        val points = parser.parseChunk(chunk)
        assertEquals(3, points.size)
        assertEquals(0, points[0].y)
        assertEquals(1, points[1].y)
        assertEquals(2, points[2].y)
    }

    @Test
    fun parseChunk_noisyAndMalformedMixed_dropsCorruptedOnly() {
        val chunk = "garbage$corrupt#extra$3,4,500#\n"
        val points = parser.parseChunk(chunk)
        assertEquals(1, points.size)
        assertEquals(3, points[0].x)
        assertEquals(4, points[0].y)
        assertEquals(500f, points[0].zValue)
    }
}
