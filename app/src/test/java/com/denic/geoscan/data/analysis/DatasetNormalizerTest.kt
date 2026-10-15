package com.denic.geoscan.data.analysis

import com.denic.geoscan.data.model.AnomalyType
import com.denic.geoscan.data.model.ScanPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DatasetNormalizerTest {

    private lateinit var normalizer: DatasetNormalizer

    @Before
    fun setUp() {
        normalizer = DatasetNormalizer(neutralDeadbandThreshold = 0.1f)
    }

    @Test
    fun calculateMedian_oddCount_returnsMiddleValue() {
        val values = listOf(10f, 20f, 30f, 40f, 50f)
        val median = normalizer.calculateMedian(values)
        assertEquals(30f, median, 0.001f)
    }

    @Test
    fun calculateMedian_evenCount_returnsAverageOfTwoMiddleValues() {
        val values = listOf(10f, 20f, 30f, 40f)
        val median = normalizer.calculateMedian(values)
        assertEquals(25f, median, 0.001f)
    }

    @Test
    fun normalize_emptyDataset_returnsEmptyStatsGracefully() {
        val dataset = normalizer.normalize(emptyList())
        assertEquals(0, dataset.points.size)
        assertEquals(0f, dataset.statistics.groundZeroMedian, 0.001f)
    }

    @Test
    fun normalize_classifiesMetalsAndCavitiesAccurately() {
        val points = listOf(
            ScanPoint(0, 0, 100f), // median
            ScanPoint(0, 1, 80f),  // cavity (-20)
            ScanPoint(0, 2, 100f), // ground zero
            ScanPoint(1, 0, 150f), // metal (+50)
            ScanPoint(1, 1, 100f)  // ground zero
        )

        val dataset = normalizer.normalize(points)
        val stats = dataset.statistics

        // Median should be 100
        assertEquals(100f, stats.groundZeroMedian, 0.001f)
        assertEquals(1, stats.metalCount)
        assertEquals(1, stats.cavityCount)
        assertEquals(3, stats.neutralCount)

        val metalPoint = dataset.points.first { it.anomalyType == AnomalyType.POSITIVE_METAL }
        assertEquals(50f, metalPoint.normalizedZ, 0.001f)

        val cavityPoint = dataset.points.first { it.anomalyType == AnomalyType.NEGATIVE_CAVITY }
        assertEquals(-20f, cavityPoint.normalizedZ, 0.001f)
    }
}
