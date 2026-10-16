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
        var explicitWidth: Int? = null

        // Check for metadata like Width=10, SamplesPerLine=10, Matrix, V3D
        for (line in rawLines) {
            val lower = line.trim().lowercase()
            if (lower.contains("samplesperline=") || lower.contains("width=") || lower.contains("points_x=")) {
                val valuePart = lower.substringAfter("=").trim()
                valuePart.toIntOrNull()?.let { explicitWidth = it }
            } else if (lower.contains("dim=") || lower.contains("dimension=")) {
                val dimStr = lower.substringAfter("=").trim()
                val parts = dimStr.split("x", "*", ",", " ")
                parts.firstOrNull()?.toIntOrNull()?.let { explicitWidth = it }
            }
        }

        val isExplicitMatrix = rawLines.any { line ->
            val trimmed = line.trim().lowercase()
            (trimmed.startsWith("#") || trimmed.startsWith("//") || trimmed.startsWith(";")) && trimmed.contains("matrix")
        }

        val cleanLines = rawLines.map { it.trim() }.filter { line ->
            !isCommentOrMetadata(line)
        }

        if (cleanLines.isEmpty()) return emptyList()

        if (isExplicitMatrix) {
            return parseMatrixLines(cleanLines)
        }

        val hasProtocolEnvelope = cleanLines.any { it.startsWith("$") && it.contains("#") }
        val hasTripletHeader = rawLines.any { isHeader(it) }

        // Check if file uses triplet coordinate format (X, Y, Z per line)
        val sampleLine = cleanLines.firstOrNull() ?: return emptyList()
        val tokens = tokenizeLine(sampleLine)

        val isTriplet = hasProtocolEnvelope || (hasTripletHeader && tokens.size >= 3) ||
                (tokens.size == 3 && cleanLines.all { tokenizeLine(it).size == 3 } && isProbableCoordinate(tokens[0], tokens[1]))

        if (isTriplet) {
            return parseTripletLines(cleanLines)
        }

        // Check if file is a 1D sequential stream of values (1 value per line)
        val isSingleValueStream = cleanLines.size >= 4 && cleanLines.all { tokenizeLine(it).size == 1 && it.toFloatOrNull() != null }
        if (isSingleValueStream) {
            val total = cleanLines.size
            val width = explicitWidth ?: run {
                val sqrt = kotlin.math.sqrt(total.toDouble()).toInt()
                if (sqrt * sqrt == total) sqrt else 10
            }
            val points = mutableListOf<ScanPoint>()
            cleanLines.forEachIndexed { index, line ->
                line.toFloatOrNull()?.let { z ->
                    val x = index % width
                    val y = index / width
                    points.add(ScanPoint(x = x, y = y, zValue = z))
                }
            }
            return points
        }

        // Standard 2D Gridded Matrix format (space, comma, or tab separated values across rows)
        return parseMatrixLines(cleanLines)
    }

    private fun isCommentOrMetadata(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return true
        if (trimmed.startsWith("#") || trimmed.startsWith("//") || trimmed.startsWith(";") || trimmed.startsWith("!")) return true
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) return true // INI/V3D section headers e.g. [Data], [Header]
        if (trimmed.contains("=") && !trimmed.contains(",") && !trimmed.contains("$")) return true // Metadata key=value
        return isHeader(trimmed)
    }

    private fun isProbableCoordinate(token0: String, token1: String): Boolean {
        val x = token0.toIntOrNull() ?: return false
        val y = token1.toIntOrNull() ?: return false
        return x in 0..500 && y in 0..500
    }

    private fun isHeader(line: String): Boolean {
        val lower = line.lowercase().trim()
        return (lower.contains("x") && lower.contains("y") && lower.contains("z")) ||
                lower.startsWith("date") || lower.startsWith("scan") || lower.startsWith("device") ||
                lower.startsWith("visualizer") || lower.startsWith("v3d") || lower.startsWith("version")
    }

    private fun tokenizeLine(line: String): List<String> {
        var clean = line
        if (clean.startsWith("$")) clean = clean.substring(1)
        if (clean.endsWith("#")) clean = clean.substring(0, clean.length - 1)

        // Split by comma, semicolon, tab, or whitespace sequence
        return clean.split(Regex("[,;\\t\\s]+")).map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun parseTripletLines(lines: List<String>): List<ScanPoint> {
        val points = mutableListOf<ScanPoint>()

        for (line in lines) {
            if (isCommentOrMetadata(line)) continue
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
            if (isCommentOrMetadata(line)) return@forEachIndexed
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
