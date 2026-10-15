package com.dnic.geoscan.data.analysis

import com.dnic.geoscan.data.model.AnomalyType
import com.dnic.geoscan.data.model.NormalizedScanPoint
import com.dnic.geoscan.data.model.ScanDataset
import com.dnic.geoscan.data.model.ScanPoint
import com.dnic.geoscan.data.model.ScanStatistics

/**
 * Performs geophysical baseline normalization on magnetometer datasets.
 *
 * Uses the dataset Median to establish "Ground Zero" (nominal background magnetic field).
 * Points above the median represent positive anomalies (ferromagnetic metals),
 * while points below represent negative anomalies (cavities, voids, or low-density anomalies).
 */
class DatasetNormalizer(
    private val neutralDeadbandThreshold: Float = 0.05f
) {

    /**
     * Normalizes raw scan points by computing the median ground zero baseline and
     * categorizing anomalies.
     *
     * @param rawPoints List of raw [ScanPoint] items parsed from file or stream.
     * @param fileName Optional file name for dataset container.
     * @param fileSizeFormatted Human readable file size string.
     * @return [ScanDataset] with normalized points and comprehensive summary metrics.
     */
    fun normalize(
        rawPoints: List<ScanPoint>,
        fileName: String = "scan_data.csv",
        fileSizeFormatted: String = "0 KB"
    ): ScanDataset {
        if (rawPoints.isEmpty()) {
            return ScanDataset(
                fileName = fileName,
                fileSizeFormatted = fileSizeFormatted,
                points = emptyList(),
                statistics = ScanStatistics(
                    totalPoints = 0,
                    groundZeroMedian = 0f,
                    mean = 0f,
                    minRaw = 0f,
                    maxRaw = 0f,
                    minNormalized = 0f,
                    maxNormalized = 0f,
                    metalCount = 0,
                    cavityCount = 0,
                    neutralCount = 0,
                    gridWidth = 0,
                    gridHeight = 0
                )
            )
        }

        // 1. Calculate Ground Zero Median
        val sortedZValues = rawPoints.map { it.zValue }.sorted()
        val median = calculateMedian(sortedZValues)

        // 2. Normalize and classify each scan point
        var metalCount = 0
        var cavityCount = 0
        var neutralCount = 0

        val normalizedPoints = rawPoints.map { point ->
            val deltaZ = point.zValue - median
            val anomalyType = when {
                deltaZ > neutralDeadbandThreshold -> {
                    metalCount++
                    AnomalyType.POSITIVE_METAL
                }
                deltaZ < -neutralDeadbandThreshold -> {
                    cavityCount++
                    AnomalyType.NEGATIVE_CAVITY
                }
                else -> {
                    neutralCount++
                    AnomalyType.NEUTRAL_GROUND
                }
            }

            NormalizedScanPoint(
                x = point.x,
                y = point.y,
                rawZ = point.zValue,
                normalizedZ = deltaZ,
                anomalyType = anomalyType
            )
        }

        // 3. Compute spatial and mathematical statistics
        val minX = rawPoints.minOf { it.x }
        val maxX = rawPoints.maxOf { it.x }
        val minY = rawPoints.minOf { it.y }
        val maxY = rawPoints.maxOf { it.y }

        val gridWidth = (maxX - minX + 1).coerceAtLeast(1)
        val gridHeight = (maxY - minY + 1).coerceAtLeast(1)

        val minRaw = sortedZValues.first()
        val maxRaw = sortedZValues.last()
        val mean = sortedZValues.average().toFloat()

        val minNormalized = normalizedPoints.minOf { it.normalizedZ }
        val maxNormalized = normalizedPoints.maxOf { it.normalizedZ }

        val statistics = ScanStatistics(
            totalPoints = rawPoints.size,
            groundZeroMedian = median,
            mean = mean,
            minRaw = minRaw,
            maxRaw = maxRaw,
            minNormalized = minNormalized,
            maxNormalized = maxNormalized,
            metalCount = metalCount,
            cavityCount = cavityCount,
            neutralCount = neutralCount,
            gridWidth = gridWidth,
            gridHeight = gridHeight
        )

        return ScanDataset(
            fileName = fileName,
            fileSizeFormatted = fileSizeFormatted,
            points = normalizedPoints,
            statistics = statistics
        )
    }

    /**
     * Calculates the statistical median of a sorted list of float values.
     */
    fun calculateMedian(sortedValues: List<Float>): Float {
        if (sortedValues.isEmpty()) return 0f
        val size = sortedValues.size
        return if (size % 2 == 1) {
            sortedValues[size / 2]
        } else {
            (sortedValues[size / 2 - 1] + sortedValues[size / 2]) / 2.0f
        }
    }
}
