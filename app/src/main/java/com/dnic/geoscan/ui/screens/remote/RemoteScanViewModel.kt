package com.dnic.geoscan.ui.screens.remote

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dnic.geoscan.data.bluetooth.BluetoothConnectionState
import com.dnic.geoscan.data.bluetooth.BluetoothDeviceModel
import com.dnic.geoscan.data.bluetooth.BluetoothService
import com.dnic.geoscan.data.model.ScanPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemoteScanViewModel(
    application: Application
) : AndroidViewModel(application) {

    val bluetoothService = BluetoothService(application.applicationContext)

    val connectionState: StateFlow<BluetoothConnectionState> = bluetoothService.connectionState

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceModel>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceModel>> = _pairedDevices.asStateFlow()

    private val _isDevicePickerVisible = MutableStateFlow(false)
    val isDevicePickerVisible: StateFlow<Boolean> = _isDevicePickerVisible.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedPoints = MutableStateFlow<List<ScanPoint>>(emptyList())
    val scannedPoints: StateFlow<List<ScanPoint>> = _scannedPoints.asStateFlow()

    private val _latestPoint = MutableStateFlow<ScanPoint?>(null)
    val latestPoint: StateFlow<ScanPoint?> = _latestPoint.asStateFlow()

    private val _rawPacketPreview = MutableStateFlow("Awaiting packet stream...")
    val rawPacketPreview: StateFlow<String> = _rawPacketPreview.asStateFlow()

    init {
        // Collect raw packet strings for live monitor
        viewModelScope.launch {
            bluetoothService.rawPackets.collect { rawChunk ->
                _rawPacketPreview.value = rawChunk.trim()
            }
        }

        // Collect parsed ScanPoints when scanning/recording is active
        viewModelScope.launch {
            bluetoothService.scanPoints.collect { point ->
                _latestPoint.value = point
                if (_isScanning.value) {
                    _scannedPoints.value = _scannedPoints.value + point
                }
            }
        }
    }

    fun openDevicePicker() {
        refreshPairedDevices()
        _isDevicePickerVisible.value = true
    }

    fun closeDevicePicker() {
        _isDevicePickerVisible.value = false
    }

    fun refreshPairedDevices() {
        _pairedDevices.value = bluetoothService.getPairedDevices()
    }

    fun connectDevice(address: String) {
        _isDevicePickerVisible.value = false
        viewModelScope.launch {
            bluetoothService.connect(address, viewModelScope)
        }
    }

    fun disconnectDevice() {
        if (_isScanning.value) {
            stopScan()
        }
        bluetoothService.disconnect()
    }

    fun toggleScan() {
        if (_isScanning.value) {
            stopScan()
        } else {
            startScan()
        }
    }

    private fun startScan() {
        _isScanning.value = true
        viewModelScope.launch {
            bluetoothService.sendCommand("START\n")
        }
    }

    private fun stopScan() {
        _isScanning.value = false
        viewModelScope.launch {
            bluetoothService.sendCommand("STOP\n")
        }
    }

    fun clearData() {
        _scannedPoints.value = emptyList()
        _latestPoint.value = null
        _rawPacketPreview.value = "Buffer cleared. Ready."
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothService.disconnect()
    }
}
