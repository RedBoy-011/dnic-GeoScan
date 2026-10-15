package com.dnic.geoscan.data.analysis

import com.dnic.geoscan.data.model.ScanPoint
import kotlin.math.max
import kotlin.math.min

/**
 * Mathematical smoothing filter applying Bicubic Convolution Interpolation (Catmull-Rom spline, a = -0.5)
 * to 2D geophysical discrete scan points.
 *
 * Transforms discrete steps into a continuous high-resolution topographical surface mesh for ECharts 3D.
 */
class BicubicInterpolator(
    private val subdivisionFactor: Int = 2
) {

    /**
     * Interpolates raw discrete scan points into a smoothed, higher-density surface grid.
     *
     * @param points List of discrete [ScanPoint] items collected from sensor or file.
     * @return List of smoothed [ScanPoint] items with continuous Z-value curvature.
     */
    fun interpolate(points: List<ScanPoint>): List<ScanPoint> {
        if (points.size < 4 || subdivisionFactor <= 1) {
            return points
        }

        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }

        val width = maxX - minX + 1
        val height = maxY - minY + 1

        // Need at least a 2x2 grid to interpolate
        if (width < 2 || height < 2) {
            return points
        }

        // 1. Build discrete 2D grid matrix
        val grid = Array(height) { FloatArray(width) }
        val populated = Array(height) { BooleanArray(width) }

        for (point in points) {
            val gx = point.x - minX
            val gy = point.y - minY
            if (gy in 0 until height && gx in 0 until width) {
                grid[gy][gx] = point.zValue
                populated[gy][gx] = true
            }
        }

        // Fill any sparsely missing cells with nearest neighbors
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (!populated[y][x]) {
                    grid[y][x] = findNearestZ(grid, populated, x, y, width, height)
                }
            }
        }

        // 2. Perform Bicubic Subdivision
        val smoothedPoints = mutableListOf<ScanPoint>()
        val newWidth = (width - 1) * subdivisionFactor + 1
        val newHeight = (height - 1) * subdivisionFactor + 1

        for (ny in 0 until newHeight) {
            val sourceY = ny.toFloat() / subdivisionFactor
            val y0 = sourceY.toInt().coerceIn(0, height - 2)
            val dy = sourceY - y0

            for (nx in 0 until newWidth) {
                val sourceX = nx.toFloat() / subdivisionFactor
                val x0 = sourceX.toInt().coerceIn(0, width - 2)
                val dx = sourceX - x0

                val interpolatedZ = evaluateBicubic(grid, x0, y0, dx, dy, width, height)

                smoothedPoints.add(
                    ScanPoint(
                        x = nx,
                        y = ny,
                        zValue = interpolatedZ
                    )
                )
            }
        }

        return smoothedPoints
    }

    /**
     * Evaluates bicubic convolution at relative fractional offset (dx, dy) from cell (x0, y0).
     */
    private fun evaluateBicubic(
        grid: Array<FloatArray>,
        x0: Int,
        y0: Int,
        dx: Float,
        dy: Float,
        width: Int,
        height: Int
    ): Float {
        // Horizontal interpolation across 4 rows
        val rowValues = FloatArray(4)
        for (j in 0 until 4) {
            val yIndex = (y0 - 1 + j).coerceIn(0, height - 1)
            val p0 = grid[yIndex][(x0 - 1).coerceIn(0, width - 1)]
            val p1 = grid[yIndex][x0.coerceIn(0, width - 1)]
            val p2 = grid[yIndex][(x0 + 1).coerceIn(0, width - 1)]
            val p3 = grid[yIndex][(x0 + 2).coerceIn(0, width - 1)]

            rowValues[j] = cubicKernel(p0, p1, p2, p3, dx)
        }

        // Vertical interpolation down the columns
        return cubicKernel(rowValues[0], rowValues[1], rowValues[2], rowValues[3], dy)
    }

    /**
     * Standard 1D cubic convolution interpolation formula (Catmull-Rom spline, a = -0.5).
     */
    private fun cubicKernel(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
        val a0 = -0.5f * p0 + 1.5f * p1 - 1.5f * p2 + 0.5f * p3
        val a1 = p0 - 2.5f * p1 + 2.0f * p2 - 0.5f * p3
        val a2 = -0.5f * p0 + 0.5f * p2
        val a3 = p1

        return a0 * t * t * t + a1 * t * t + a2 * t + a3
    }

    private fun findNearestZ(
        grid: Array<FloatArray>,
        populated: Array<BooleanArray>,
        targetX: Int,
        targetY: Int,
        width: Int,
        height: Int
    ): Float {
        var minDistance = Float.MAX_VALUE
        var nearestVal = 0f

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (populated[y][x]) {
                    val dist = (x - targetX) * (x - targetX) + (y - targetY) * (y - targetY)
                    if (dist < minDistance) {
                        minDistance = dist.toFloat()
                        nearestVal = grid[y][x]
                    }
                }
            }
        }
        return nearestVal
    }
}
