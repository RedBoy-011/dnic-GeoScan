package com.denic.geoscan.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Hub : Screen(
        route = "hub",
        title = "Hub",
        icon = Icons.Outlined.Dashboard
    )

    data object LocalAnalysis : Screen(
        route = "local_analysis",
        title = "Local Analysis",
        icon = Icons.Outlined.FolderOpen
    )

    data object RemoteScan : Screen(
        route = "remote_scan",
        title = "Remote Scan",
        icon = Icons.Outlined.Bluetooth
    )

    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        icon = Icons.Outlined.Settings
    )

    companion object {
        /**
         * Dynamic getter ensures subclasses are evaluated after class loading,
         * completely avoiding JVM circular dependency and null references.
         */
        val items: List<Screen>
            get() = listOfNotNull(Hub, LocalAnalysis, RemoteScan, Settings)
    }
}
