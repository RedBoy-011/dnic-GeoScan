package com.denic.geoscan.data.analysis

import com.denic.geoscan.data.model.ScanPoint
import kotlin.math.sqrt

/**
 * Advanced geophysical mathematical filters for magnetic anomaly enhancement:
 * - RAW_NORMALIZED: Ground Zero median baseline
 * - FIRST_VERTICAL_DERIVATIVE (1VD): High-pass Laplacian filter enhancing boundaries and shallow bodies
 * - ANALYTIC_SIGNAL (AS): Total gradient amplitude removing magnetic dipole inclination shift
 */
enum class GeophysicalFilterType(val title: String) {
    RAW_NORMALIZED("Normal (Ground Zero)"),
    FIRST_VERTICAL_DERIVATIVE("1st Vertical Derivative (1VD)"),
    ANALYTIC_SIGNAL("Analytic Signal (AS)")
}

class GeophysicalGradientFilter {

    fun applyFilter(points: List<ScanPoint>, filterType: GeophysicalFilterType): List<ScanPoint> {
        if (points.size < 4 || filterType == GeophysicalFilterType.RAW_NORMALIZED) {
            return points
        }

        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }

        val width = maxX - minX + 1
        val height = maxY - minY + 1

        if (width < 2 || height < 2) return points

        // Construct 2D grid matrix
        val grid = Array(height) { FloatArray(width) }
        for (p in points) {
            val gx = p.x - minX
            val gy = p.y - minY
            if (gy in 0 until height && gx in 0 until width) {
                grid[gy][gx] = p.zValue
            }
        }

        val result = mutableListOf<ScanPoint>()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val zVal = when (filterType) {
                    GeophysicalFilterType.FIRST_VERTICAL_DERIVATIVE -> {
                        // Laplacian 2D finite-difference: 4*Z - (left + right + up + down)
                        val left = if (x > 0) grid[y][x - 1] else grid[y][x]
                        val right = if (x < width - 1) grid[y][x + 1] else grid[y][x]
                        val up = if (y > 0) grid[y - 1][x] else grid[y][x]
                        val down = if (y < height - 1) grid[y + 1][x] else grid[y][x]
                        (4f * grid[y][x] - (left + right + up + down))
                    }
                    GeophysicalFilterType.ANALYTIC_SIGNAL -> {
                        // Total spatial gradient magnitude: sqrt(dx^2 + dy^2)
                        val dx = ((if (x < width - 1) grid[y][x + 1] else grid[y][x]) - (if (x > 0) grid[y][x - 1] else grid[y][x])) / 2f
                        val dy = ((if (y < height - 1) grid[y + 1][x] else grid[y][x]) - (if (y > 0) grid[y - 1][x] else grid[y][x])) / 2f
                        sqrt(dx * dx + dy * dy)
                    }
                    GeophysicalFilterType.RAW_NORMALIZED -> grid[y][x]
                }

                result.add(
                    ScanPoint(
                        x = minX + x,
                        y = minY + y,
                        zValue = zVal
                    )
                )
            }
        }
        return result
    }
}
