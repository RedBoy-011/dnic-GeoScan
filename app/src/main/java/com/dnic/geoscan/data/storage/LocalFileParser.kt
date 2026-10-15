package com.dnic.geoscan.data.storage

import com.dnic.geoscan.data.model.ScanPoint
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Parser for local CSV and TXT magnetometer data files loaded from device storage via SAF.
 *
 * Automatically detects and handles:
 * 1. Coordinate triplet format: `X,Y,Z` or `$X,Y,Z#` (comma, semicolon, or tab delimited).
 * 2. 2D Surface Matrix format: Each row represents Y, each column represents X, with values representing Z.
 * 3. Comments, headers, and metadata lines (e.g. starting with `#`, `//`, `Date`, `X,Y,Z`).
 */
class LocalFileParser {

    /**
     * Parses an input stream (from ContentResolver or file) into a list of [ScanPoint] items.
     */
    fun parse(inputStream: InputStream): List<ScanPoint> {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val lines = reader.readLines()
        return parseLines(lines)
    }

    /**
     * Parses a list of lines extracted from CSV or TXT.
     */
    fun parseLines(rawLines: List<String>): List<ScanPoint> {
        val cleanLines = rawLines.map { it.trim() }.filter { line ->
            line.isNotEmpty() && !line.startsWith("#") && !line.startsWith("//")
        }

        if (cleanLines.isEmpty()) return emptyList()

        // Check if file uses triplet coordinate format (X, Y, Z per line)
        val sampleLine = cleanLines.firstOrNull { !isHeader(it) } ?: return emptyList()
        val tokens = tokenizeLine(sampleLine)

        return if (tokens.size == 3 && cleanLines.all { isHeader(it) || tokenizeLine(it).size == 3 }) {
            parseTripletLines(cleanLines)
        } else {
            // Treat as 2D Matrix format or fallback
            parseMatrixLines(cleanLines)
        }
    }

    private fun isHeader(line: String): Boolean {
        val lower = line.lowercase()
        return lower.contains("x") && lower.contains("y") && lower.contains("z") ||
                lower.startsWith("date") || lower.startsWith("scan") || lower.startsWith("device")
    }

    private fun tokenizeLine(line: String): List<String> {
        var clean = line
        if (clean.startsWith("$")) clean = clean.substring(1)
        if (clean.endsWith("#")) clean = clean.substring(0, clean.length - 1)

        // Split by comma, semicolon, tab, or whitespace sequence
        return clean.split(Regex("[,;\\t]+")).map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun parseTripletLines(lines: List<String>): List<ScanPoint> {
        val points = mutableListOf<ScanPoint>()

        for (line in lines) {
            if (isHeader(line)) continue
            val tokens = tokenizeLine(line)
            if (tokens.size >= 3) {
                try {
                    val x = tokens[0].toInt()
                    val y = tokens[1].toInt()
                    val z = tokens[2].toFloat()
                    points.add(ScanPoint(x = x, y = y, zValue = z))
                } catch (_: NumberFormatException) {
                    // Ignore corrupted line
                }
            }
        }

        return points
    }

    private fun parseMatrixLines(lines: List<String>): List<ScanPoint> {
        val points = mutableListOf<ScanPoint>()
        var rowIndex = 0

        for (line in lines) {
            if (isHeader(line)) continue
            val tokens = tokenizeLine(line)
            if (tokens.isEmpty()) continue

            tokens.forEachIndexed { colIndex, token ->
                try {
                    val z = token.toFloat()
                    points.add(ScanPoint(x = colIndex, y = rowIndex, zValue = z))
                } catch (_: NumberFormatException) {
                    // Ignore non-numeric cell
                }
            }
            rowIndex++
        }

        return points
    }
}
