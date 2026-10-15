package com.denic.geoscan.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object RemoteScan : Screen(
        route = "remote_scan",
        title = "Remote Scan",
        icon = Icons.Outlined.Bluetooth
    )

    object LocalAnalysis : Screen(
        route = "local_analysis",
        title = "Local Analysis",
        icon = Icons.Outlined.FolderOpen
    )

    companion object {
        val items = listOf(RemoteScan, LocalAnalysis)
    }
}
