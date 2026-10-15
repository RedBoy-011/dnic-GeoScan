package com.denic.geoscan.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Hub : Screen(
        route = "hub",
        title = "Hub",
        icon = Icons.Outlined.Dashboard
    )

    object LocalAnalysis : Screen(
        route = "local_analysis",
        title = "Local Analysis",
        icon = Icons.Outlined.FolderOpen
    )

    object RemoteScan : Screen(
        route = "remote_scan",
        title = "Remote Scan",
        icon = Icons.Outlined.Bluetooth
    )

    companion object {
        val items = listOf(Hub, LocalAnalysis, RemoteScan)
    }
}
