package com.dnic.geoscan.data.model

/**
 * Categorization of geophysical magnetic anomaly based on Ground Zero (median) baseline.
 */
enum class AnomalyType {
    POSITIVE_METAL,    // Above Ground Zero (ferromagnetic metal / magnetic peak)
    NEUTRAL_GROUND,    // Closely aligned with Ground Zero baseline
    NEGATIVE_CAVITY    // Below Ground Zero (cavity / void / underground disruption)
}

/**
 * Represents a scan point normalized against the dataset's median ground baseline.
 *
 * @property x Grid column index.
 * @property y Grid row index.
 * @property rawZ Raw magnetic flux density reading from sensor.
 * @property normalizedZ Anomaly amplitude relative to Ground Zero (rawZ - median).
 * @property anomalyType Classified target type based on baseline offset.
 */
data class NormalizedScanPoint(
    val x: Int,
    val y: Int,
    val rawZ: Float,
    val normalizedZ: Float,
    val anomalyType: AnomalyType
)

/**
 * Summary telemetry metrics and geophysical anomaly statistics for a loaded scan.
 */
data class ScanStatistics(
    val totalPoints: Int,
    val groundZeroMedian: Float,
    val mean: Float,
    val minRaw: Float,
    val maxRaw: Float,
    val minNormalized: Float,
    val maxNormalized: Float,
    val metalCount: Int,
    val cavityCount: Int,
    val neutralCount: Int,
    val gridWidth: Int,
    val gridHeight: Int
)

/**
 * Encapsulates an entire parsed and baseline-normalized geophysical dataset.
 */
data class ScanDataset(
    val fileName: String,
    val fileSizeFormatted: String,
    val points: List<NormalizedScanPoint>,
    val statistics: ScanStatistics
)
