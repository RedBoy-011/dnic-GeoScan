package com.denic.geoscan.ui.screens.local

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denic.geoscan.data.analysis.BicubicInterpolator
import com.denic.geoscan.data.analysis.DatasetNormalizer
import com.denic.geoscan.data.model.ScanDataset
import com.denic.geoscan.data.model.ScanPoint
import com.denic.geoscan.data.storage.LocalFileParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalAnalysisViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val fileParser = LocalFileParser()
    private val normalizer = DatasetNormalizer()
    private val bicubicInterpolator = BicubicInterpolator(subdivisionFactor = 2)
    private val gradientFilter = com.denic.geoscan.data.analysis.GeophysicalGradientFilter()
    private val sessionExporter = com.denic.geoscan.data.storage.SessionExporter(application.applicationContext)

    private val _dataset = MutableStateFlow<ScanDataset?>(null)
    val dataset: StateFlow<ScanDataset?> = _dataset.asStateFlow()

    private val _isSmoothingEnabled = MutableStateFlow(false)
    val isSmoothingEnabled: StateFlow<Boolean> = _isSmoothingEnabled.asStateFlow()

    private val _selectedFilter = MutableStateFlow(com.denic.geoscan.data.analysis.GeophysicalFilterType.RAW_NORMALIZED)
    val selectedFilter: StateFlow<com.denic.geoscan.data.analysis.GeophysicalFilterType> = _selectedFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _exportStatusMessage = MutableStateFlow<String?>(null)
    val exportStatusMessage: StateFlow<String?> = _exportStatusMessage.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    // Smooth vs raw points computed dynamically for ECharts with gradient filters
    val visualizerPoints: StateFlow<List<ScanPoint>> = combine(
        _dataset,
        _isSmoothingEnabled,
        _selectedFilter
    ) { data, smoothing, filter ->
        var points = data?.points?.map { ScanPoint(it.x, it.y, it.normalizedZ) } ?: emptyList()
        if (points.isNotEmpty() && filter != com.denic.geoscan.data.analysis.GeophysicalFilterType.RAW_NORMALIZED) {
            withContext(Dispatchers.Default) {
                points = gradientFilter.applyFilter(points, filter)
            }
        }
        if (smoothing && points.size >= 4) {
            withContext(Dispatchers.Default) {
                bicubicInterpolator.interpolate(points)
            }
        } else {
            points
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilterType(filter: com.denic.geoscan.data.analysis.GeophysicalFilterType) {
        _selectedFilter.value = filter
    }

    init {
        // Pre-load a realistic 15x15 multi-depth mining & metal survey grid on launch
        // Features deep ore vein, shallow metal dipole, and tunnel cavity
        loadMiningSimulation(15, 15)
    }

    fun toggleSmoothing() {
        _isSmoothingEnabled.value = !_isSmoothingEnabled.value
    }

    /**
     * Ingests, parses, and normalizes a magnetometer file chosen via Storage Access Framework (SAF).
     */
    fun loadFileFromUri(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val context = getApplication<Application>()
                val contentResolver = context.contentResolver

                // Extract filename and size metadata
                var fileName = "scan_data.csv"
                var fileSizeFormatted = "Unknown size"

                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex) ?: fileName
                        }
                        if (sizeIndex != -1) {
                            val sizeBytes = cursor.getLong(sizeIndex)
                            fileSizeFormatted = formatFileSize(sizeBytes)
                        }
                    }
                }

                val normalizedData = withContext(Dispatchers.IO) {
                    val stream = contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Unable to open input stream for selected file")

                    stream.use { inputStream ->
                        val rawPoints = fileParser.parse(inputStream)
                        if (rawPoints.isEmpty()) {
                            throw IllegalArgumentException("The selected file does not contain valid magnetometer coordinates or data points.")
                        }
                        normalizer.normalize(
                            rawPoints = rawPoints,
                            fileName = fileName,
                            fileSizeFormatted = fileSizeFormatted
                        )
                    }
                }

                _dataset.value = normalizedData
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to parse magnetometer file"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearDataset() {
        _dataset.value = null
        _errorMessage.value = null
    }

    /**
     * Instantly synthesizes a realistic 10x10 or 20x20 geophysical survey grid for offline testing.
     * Features background geomagnetic field, a positive ferromagnetic anomaly peak, and a negative void/cavity.
     */
    fun loadSampleGrid(width: Int = 10, height: Int = 10) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val points = mutableListOf<ScanPoint>()
                val baseField = 48.5f // Nominal microtesla (µT)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val dxMetal = x - width * 0.35f
                        val dyMetal = y - height * 0.35f
                        val distMetal = kotlin.math.sqrt(dxMetal * dxMetal + dyMetal * dyMetal)

                        val dxCavity = x - width * 0.70f
                        val dyCavity = y - height * 0.70f
                        val distCavity = kotlin.math.sqrt(dxCavity * dxCavity + dyCavity * dyCavity)

                        val metalPeak = 16.5f * kotlin.math.exp(-distMetal / 1.4f)
                        val cavityDip = -12.0f * kotlin.math.exp(-distCavity / 1.4f)
                        val noise = ((x * 13 + y * 29) % 7 - 3) * 0.12f

                        val z = baseField + metalPeak + cavityDip + noise
                        points.add(ScanPoint(x = x, y = y, zValue = z))
                    }
                }
                val normalizedData = normalizer.normalize(
                    rawPoints = points,
                    fileName = "demo_survey_${width}x${height}.csv",
                    fileSizeFormatted = "${points.size * 18} B"
                )
                _dataset.value = normalizedData
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to generate sample grid"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Synthesizes a realistic multi-depth geophysical survey representing:
     * - Deep Magnetite / Iron Ore Vein (~4.5m depth, broad diagonal anomaly)
     * - Shallow High-Grade Ferrous Metal Target (~1.2m depth, sharp dipole peak)
     * - Secondary Polymetallic Metal Node (~2.8m depth)
     * - Underground Ancient Mine Tunnel / Cavity Shaft (~3.0m depth, negative void dip)
     * - Regional geomagnetic gradient baseline (1000 - 1008 µT)
     */
    fun loadMiningSimulation(width: Int = 15, height: Int = 15) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val points = mutableListOf<ScanPoint>()
                val baseField = 1000.0f
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        // Regional natural slope gradient
                        val regionalGradient = (x * 0.35f + y * 0.25f)

                        // 1. Shallow Ferrous Target at (4, 4) - Depth ~1.2m (steep gradient)
                        val dxShallow = x - 4.0f
                        val dyShallow = y - 4.0f
                        val distShallow = kotlin.math.sqrt(dxShallow * dxShallow + dyShallow * dyShallow)
                        val shallowPeak = 52.0f * kotlin.math.exp(-distShallow / 1.2f)

                        // 2. Deep Mineralized Ore Vein along diagonal line - Depth ~4.5m (broad gradient)
                        val veinDist = kotlin.math.abs(x - y) / 1.414f
                        val veinMask = if (x in 2..12 && y in 2..12) 1.0f else 0.2f
                        val deepVeinAnomaly = 26.5f * kotlin.math.exp(-veinDist / 2.2f) * veinMask

                        // 3. Subterranean Mine Shaft / Tunnel Cavity along row y=12, x in 3..9 - Depth ~3.0m
                        val cavityDist = if (x in 3..9) kotlin.math.abs(y - 12.0f) else {
                            val edgeX = if (x < 3) 3.0f else 9.0f
                            kotlin.math.sqrt((x - edgeX) * (x - edgeX) + (y - 12.0f) * (y - 12.0f))
                        }
                        val cavityDip = -28.0f * kotlin.math.exp(-cavityDist / 1.5f)

                        // 4. Secondary Medium-Depth Metal at (11, 4) - Depth ~2.8m
                        val dxMed = x - 11.0f
                        val dyMed = y - 4.0f
                        val distMed = kotlin.math.sqrt(dxMed * dxMed + dyMed * dyMed)
                        val mediumPeak = 24.0f * kotlin.math.exp(-distMed / 1.6f)

                        // Ground sensor micro-noise
                        val noise = ((x * 17 + y * 31) % 11 - 5) * 0.08f

                        val z = baseField + regionalGradient + shallowPeak + deepVeinAnomaly + cavityDip + mediumPeak + noise
                        points.add(ScanPoint(x = x, y = y, zValue = z))
                    }
                }
                val normalizedData = normalizer.normalize(
                    rawPoints = points,
                    fileName = "demo_mine_multidepth_${width}x${height}.v3d",
                    fileSizeFormatted = "${points.size * 22} B"
                )
                _dataset.value = normalizedData
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to generate mining simulation"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportProcessedDataset() {
        val points = visualizerPoints.value
        if (points.isEmpty()) {
            _exportStatusMessage.value = "Cannot export an empty dataset."
            return
        }
        viewModelScope.launch {
            _isExporting.value = true
            val filterTag = when (_selectedFilter.value) {
                com.denic.geoscan.data.analysis.GeophysicalFilterType.RAW_NORMALIZED -> "norm"
                com.denic.geoscan.data.analysis.GeophysicalFilterType.FIRST_VERTICAL_DERIVATIVE -> "1vd"
                com.denic.geoscan.data.analysis.GeophysicalFilterType.ANALYTIC_SIGNAL -> "as"
            }
            val result = sessionExporter.exportToCsv(points, "local_${filterTag}")
            result.onSuccess { path ->
                _exportStatusMessage.value = "Exported to Documents/denic-GeoScan: $path"
            }.onFailure { err ->
                _exportStatusMessage.value = "Export failed: ${err.message}"
            }
            _isExporting.value = false
        }
    }

    fun clearExportMessage() {
        _exportStatusMessage.value = null
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024f * 1024f))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024f)
            else -> "$bytes bytes"
        }
    }
}
