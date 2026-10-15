package com.denic.geoscan.data.analysis

import com.denic.geoscan.data.model.ScanPoint
import kotlin.math.abs

/**
 * Result of depth estimation using potential-field geophysical rules.
 */
data class DepthEstimationResult(
    val peakValue: Float,
    val peakIndex: Int,
    val halfWidthStations: Float,
    val estimatedDepthMeters: Float,
    val isCavity: Boolean,
    val confidence: String
)

/**
 * Specialized Geophysical Depth Estimation Engine.
 * Implements the classic Half-Width Rule (Telford et al., 1990; Breiner, 1999)
 * for isolated magnetic dipoles, pipes, spheres, and subterranean voids.
 */
class GeophysicalDepthEstimator {

    /**
     * Estimates source burial depth from a 2D transect magnetic profile.
     *
     * @param slicePoints Ordered list of sample stations along the transect line.
     * @param stationSpacingMeters Physical distance in meters between consecutive stations (e.g. 0.5m, 1.0m, 2.0m).
     * @return DepthEstimationResult or null if profile does not contain a discernible peak.
     */
    fun estimateDepth(
        slicePoints: List<ScanPoint>,
        stationSpacingMeters: Float = 1.0f
    ): DepthEstimationResult? {
        if (slicePoints.size < 3) return null

        var maxAbsIndex = 0
        var maxAbsVal = 0f
        for (i in slicePoints.indices) {
            val a = abs(slicePoints[i].zValue)
            if (a > maxAbsVal) {
                maxAbsVal = a
                maxAbsIndex = i
            }
        }

        if (maxAbsVal < 0.1f) return null // Background noise / no anomaly

        val peakZ = slicePoints[maxAbsIndex].zValue
        val halfMax = maxAbsVal * 0.5f

        // Search left of peak for half-amplitude crossing
        var leftIndex = -1f
        for (i in maxAbsIndex downTo 1) {
            val v1 = abs(slicePoints[i].zValue)
            val v0 = abs(slicePoints[i - 1].zValue)
            if (v0 <= halfMax && v1 >= halfMax) {
                val frac = if (v1 != v0) (halfMax - v0) / (v1 - v0) else 0.5f
                leftIndex = (i - 1) + frac
                break
            }
        }
        if (leftIndex < 0f) {
            leftIndex = 0f
        }

        // Search right of peak for half-amplitude crossing
        var rightIndex = -1f
        for (i in maxAbsIndex until slicePoints.size - 1) {
            val v0 = abs(slicePoints[i].zValue)
            val v1 = abs(slicePoints[i + 1].zValue)
            if (v0 >= halfMax && v1 <= halfMax) {
                val frac = if (v0 != v1) (halfMax - v0) / (v1 - v0) else 0.5f
                rightIndex = i + frac
                break
            }
        }
        if (rightIndex < 0f) {
            rightIndex = (slicePoints.size - 1).toFloat()
        }

        val fwhmStations = (rightIndex - leftIndex).coerceAtLeast(0.5f)
        val depthMeters = fwhmStations * stationSpacingMeters * 0.5f

        val confidence = when {
            fwhmStations >= 1.5f && maxAbsIndex > 0 && maxAbsIndex < slicePoints.size - 1 -> "High"
            fwhmStations >= 1.0f -> "Moderate"
            else -> "Low (Sub-station peak)"
        }

        return DepthEstimationResult(
            peakValue = peakZ,
            peakIndex = maxAbsIndex,
            halfWidthStations = fwhmStations,
            estimatedDepthMeters = depthMeters,
            isCavity = peakZ < 0f,
            confidence = confidence
        )
    }
}
