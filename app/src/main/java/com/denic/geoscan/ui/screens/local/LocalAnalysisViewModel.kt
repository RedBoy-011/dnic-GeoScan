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
        // Pre-load a baseline 10x10 geophysical reference grid on launch
        // Ensures Local Analysis is immediately functional, interactive and stable
        loadSampleGrid(10, 10)
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
