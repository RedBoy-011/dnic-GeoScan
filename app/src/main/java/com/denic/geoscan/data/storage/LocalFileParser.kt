package com.denic.geoscan.data.storage

import com.denic.geoscan.data.model.ScanPoint
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Ultra-robust parser for magnetometer survey datasets loaded from offline storage.
 * Supports comma/semicolon/tab/space delimited CSV, TXT, and V3D files in three formats:
 * 1. Triplet format: X, Y, Z coordinates per line (e.g. "0, 1, 1045.5" or "$0,1,1045.5#")
 * 2. 2D Matrix format: Gridded matrix of magnetic field values across columns and rows.
 * 3. 1D Sequential stream: Single field reading per line with grid dimension metadata.
 *
 * Fully supports Persian/Arabic numerals, UTF-8 BOM stripping, and Visualizer 3D tags.
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
            val sanitized = sanitize(line).trim().lowercase()
            if (sanitized.contains("samplesperline=") || sanitized.contains("width=") || sanitized.contains("points_x=")) {
                val valuePart = sanitized.substringAfter("=").trim()
                valuePart.toIntOrNull()?.let { explicitWidth = it }
            } else if (sanitized.contains("dim=") || sanitized.contains("dimension=")) {
                val dimStr = sanitized.substringAfter("=").trim()
                val parts = dimStr.split("x", "*", ",", " ")
                parts.firstOrNull()?.toIntOrNull()?.let { explicitWidth = it }
            }
        }

        val isExplicitMatrix = rawLines.any { line ->
            val trimmed = sanitize(line).trim().lowercase()
            (trimmed.startsWith("#") || trimmed.startsWith("//") || trimmed.startsWith(";")) && trimmed.contains("matrix")
        }

        val cleanLines = rawLines
            .map { sanitize(it).trim() }
            .filter { line -> !isCommentOrMetadata(line) }

        com.denic.geoscan.util.GeoLogger.i("LocalFileParser", "Parsing ${rawLines.size} raw lines (${cleanLines.size} clean lines, explicitWidth=$explicitWidth, isMatrix=$isExplicitMatrix)")

        if (cleanLines.isEmpty()) return emptyList()

        if (isExplicitMatrix) {
            val points = parseMatrixLines(cleanLines)
            com.denic.geoscan.util.GeoLogger.i("LocalFileParser", "Parsed explicit matrix: ${points.size} points")
            return points
        }

        val hasProtocolEnvelope = cleanLines.any { it.startsWith("$") && it.contains("#") }
        val hasTripletHeader = rawLines.any { isHeader(it) }

        // Determine if file uses triplet coordinate format (X, Y, Z)
        // Statistical majority sampling: checks up to first 25 non-comment lines
        val testLines = cleanLines.take(25)
        var tripletMatches = 0
        for (tLine in testLines) {
            val tokens = tokenizeLine(tLine)
            if (tokens.size >= 3) {
                val x = tokens[0].toIntOrNull()
                val y = tokens[1].toIntOrNull()
                val z = tokens[2].toFloatOrNull()
                if (x != null && y != null && z != null && x in 0..5000 && y in 0..5000) {
                    tripletMatches++
                }
            }
        }

        val isTriplet = hasProtocolEnvelope ||
                (hasTripletHeader && cleanLines.firstOrNull()?.let { tokenizeLine(it).size in 3..6 } == true) ||
                (tripletMatches >= (testLines.size + 1) / 2 && tripletMatches > 0)

        if (isTriplet) {
            val points = parseTripletLines(cleanLines)
            if (points.isNotEmpty()) {
                com.denic.geoscan.util.GeoLogger.i("LocalFileParser", "Parsed triplet coordinates: ${points.size} points")
                return points
            }
        }

        // Check if file is a 1D sequential stream of values (1 value per line)
        val isSingleValueStream = cleanLines.size >= 4 && cleanLines.all { line ->
            val tok = tokenizeLine(line)
            tok.size == 1 && tok[0].toFloatOrNull() != null
        }
        if (isSingleValueStream) {
            val total = cleanLines.size
            val width = explicitWidth ?: run {
                val sqrt = kotlin.math.sqrt(total.toDouble()).toInt()
                if (sqrt * sqrt == total) sqrt else 10
            }
            val points = mutableListOf<ScanPoint>()
            cleanLines.forEachIndexed { index, line ->
                val tok = tokenizeLine(line)
                tok.firstOrNull()?.toFloatOrNull()?.let { z ->
                    val x = index % width
                    val y = index / width
                    points.add(ScanPoint(x = x, y = y, zValue = z))
                }
            }
            com.denic.geoscan.util.GeoLogger.i("LocalFileParser", "Parsed 1D stream: ${points.size} points (width=$width)")
            return points
        }

        // Standard 2D Gridded Matrix format (space, comma, or tab separated values across rows)
        val points = parseMatrixLines(cleanLines)
        com.denic.geoscan.util.GeoLogger.i("LocalFileParser", "Parsed standard matrix format: ${points.size} points")
        return points
    }

    private fun isCommentOrMetadata(line: String): Boolean {
        val trimmed = sanitize(line).trim().lowercase()
        if (trimmed.isEmpty()) return true
        if (trimmed.startsWith("#") || trimmed.startsWith("//") || trimmed.startsWith(";") ||
            trimmed.startsWith("!") || trimmed.startsWith("%") || trimmed.startsWith("*")) return true
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) return true // INI/V3D section headers e.g. [Data], [Header]

        if (trimmed.contains("=")) {
            // Check if it's metadata key=value (e.g. Version=3.0, Width=10, ShallowTarget=X:4,Y:4...)
            val tokens = tokenizeLine(trimmed)
            val numericCount = tokens.count { it.toFloatOrNull() != null }
            if (numericCount < 3) return true
        }

        return isHeader(trimmed)
    }

    private fun isHeader(line: String): Boolean {
        val lower = sanitize(line).lowercase().trim()
        return (lower.contains("x") && lower.contains("y") && lower.contains("z")) ||
                lower.startsWith("date") || lower.startsWith("scan") || lower.startsWith("device") ||
                lower.startsWith("visualizer") || lower.startsWith("v3d") || lower.startsWith("version") ||
                lower.startsWith("survey") || lower.startsWith("unit") || lower.startsWith("lines") ||
                lower.startsWith("samples") || lower.startsWith("points") || lower.startsWith("width") ||
                lower.startsWith("height") || lower.startsWith("header") || lower.startsWith("data")
    }

    private fun sanitize(text: String): String {
        return text
            .replace("\uFEFF", "") // UTF-8 BOM
            .replace("\u200B", "") // Zero-width space
            .replace("\u200C", "") // Zero-width non-joiner
            .replace("\u00A0", " ") // Non-breaking space
            .map { ch ->
                when (ch) {
                    '۰', '٠' -> '0'
                    '۱', '١' -> '1'
                    '۲', '٢' -> '2'
                    '۳', '٣' -> '3'
                    '۴', '٤' -> '4'
                    '۵', '٥' -> '5'
                    '۶', '٦' -> '6'
                    '۷', '٧' -> '7'
                    '۸', '٨' -> '8'
                    '۹', '٩' -> '9'
                    '٫' -> '.'
                    else -> ch
                }
            }.joinToString("")
    }

    private fun tokenizeLine(line: String): List<String> {
        var clean = sanitize(line).trim()
        if (clean.startsWith("$")) clean = clean.substring(1)
        if (clean.endsWith("#")) clean = clean.substring(0, clean.length - 1)
        clean = clean.replace("\"", "").replace("'", "")

        // Split by comma, semicolon, tab, pipe, or whitespace sequence
        return clean.split(Regex("[,;\\t|\\s]+")).map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun parseTripletLines(lines: List<String>): List<ScanPoint> {
        val points = mutableListOf<ScanPoint>()

        for (line in lines) {
            if (isCommentOrMetadata(line)) continue
            val tokens = tokenizeLine(line)
            if (tokens.size >= 3) {
                val x = tokens[0].toIntOrNull()
                val y = tokens[1].toIntOrNull()
                val z = tokens[2].toFloatOrNull()
                if (x != null && y != null && z != null) {
                    points.add(ScanPoint(x = x, y = y, zValue = z))
                }
            }
        }
        return points
    }

    private fun parseMatrixLines(lines: List<String>): List<ScanPoint> {
        val nonCommentLines = lines.filter { !isCommentOrMetadata(it) }
        val rowsCount = nonCommentLines.size
        val points = mutableListOf<ScanPoint>()

        nonCommentLines.forEachIndexed { rowIndex, line ->
            val tokens = tokenizeLine(line)

            var colIndex = 0
            for (token in tokens) {
                token.toFloatOrNull()?.let { zValue ->
                    // R surface3d Cartesian calibration: (0,0) is bottom-left
                    // Invert Y axis so top row in text maps to highest Y in Cartesian space
                    val cartesianY = if (rowsCount > 1) (rowsCount - 1 - rowIndex) else rowIndex
                    points.add(
                        ScanPoint(
                            x = colIndex,
                            y = cartesianY,
                            zValue = zValue
                        )
                    )
                    colIndex++
                }
            }
        }
        return points
    }
}
