package com.denic.geoscan.data.bluetooth

/**
 * Lightweight data model representing an SPP Bluetooth device (such as HC-05).
 */
data class BluetoothDeviceModel(
    val name: String,
    val address: String
)
