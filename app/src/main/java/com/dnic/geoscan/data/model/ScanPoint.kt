package com.dnic.geoscan.data.model

/**
 * Represents a single geophysical scan data point recorded at coordinates (X, Y)
 * with magnetic field flux density or anomaly value Z.
 *
 * @property x Column or horizontal step index on the scan grid.
 * @property y Row or longitudinal line index on the scan grid.
 * @property zValue Magnetic flux density or anomaly reading in microtesla (µT) / arbitrary units.
 * @property timestamp Unix timestamp in milliseconds when the packet was captured.
 */
data class ScanPoint(
    val x: Int,
    val y: Int,
    val zValue: Float,
    val timestamp: Long = System.currentTimeMillis()
)
