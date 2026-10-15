package com.denic.geoscan.data.storage

import com.denic.geoscan.data.model.ScanPoint
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Robust parser for magnetometer survey datasets loaded from offline storage.
 * Supports comma/semicolon/tab delimited CSV and TXT files in two primary formats:
 * 1. Triplet format: X, Y, Z coordinates per line (e.g. "0, 1, 1045.5" or "$0,1,1045.5#")
 * 2. 2D Matrix format: Gridded matrix of magnetic field values across columns and rows.
 */
class LocalFileParser {

    /**
     * Parses an [InputStream] into a list of [ScanPoint] items.
     */
    fun parse(inputStream: InputStream): List<ScanPoint> {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val lines = reader.readLines()
        return parseLines(lines)
    }

    /**
     * Parses a list of raw text lines into [ScanPoint] models.
     */
    fun parseLines(rawLines: List<String>): List<ScanPoint> {
        val isExplicitMatrix = rawLines.any { line ->
            val trimmed = line.trim().lowercase()
            (trimmed.startsWith("#") || trimmed.startsWith("//")) && trimmed.contains("matrix")
        }

        val cleanLines = rawLines.map { it.trim() }.filter { line ->
            line.isNotEmpty() && !line.startsWith("#") && !line.startsWith("//")
        }

        if (cleanLines.isEmpty()) return emptyList()

        if (isExplicitMatrix) {
            return parseMatrixLines(cleanLines)
        }

        val hasProtocolEnvelope = cleanLines.any { it.startsWith("$") && it.contains("#") }
        val hasTripletHeader = cleanLines.any { isHeader(it) }

        // Check if file uses triplet coordinate format (X, Y, Z per line)
        val sampleLine = cleanLines.firstOrNull { !isHeader(it) } ?: return emptyList()
        val tokens = tokenizeLine(sampleLine)

        val isTriplet = hasProtocolEnvelope || hasTripletHeader || (tokens.size == 3 && cleanLines.all { isHeader(it) || tokenizeLine(it).size == 3 } && isProbableCoordinate(tokens[0], tokens[1]))

        return if (isTriplet) {
            parseTripletLines(cleanLines)
        } else {
            // Treat as 2D Matrix format or fallback
            parseMatrixLines(cleanLines)
        }
    }

    private fun isProbableCoordinate(token0: String, token1: String): Boolean {
        val x = token0.toIntOrNull() ?: return false
        val y = token1.toIntOrNull() ?: return false
        return x in 0..100 && y in 0..100
    }

    private fun isHeader(line: String): Boolean {
        val lower = line.lowercase()
        return (lower.contains("x") && lower.contains("y") && lower.contains("z")) ||
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
                    // Skip malformed line
                }
            }
        }
        return points
    }

    private fun parseMatrixLines(lines: List<String>): List<ScanPoint> {
        val points = mutableListOf<ScanPoint>()

        lines.forEachIndexed { rowIndex, line ->
            if (isHeader(line)) return@forEachIndexed
            val tokens = tokenizeLine(line)

            tokens.forEachIndexed { colIndex, token ->
                token.toFloatOrNull()?.let { zValue ->
                    points.add(
                        ScanPoint(
                            x = colIndex,
                            y = rowIndex,
                            zValue = zValue
                        )
                    )
                }
            }
        }
        return points
    }
}
