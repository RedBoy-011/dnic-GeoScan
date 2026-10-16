package com.denic.geoscan.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.denic.geoscan.ui.components.ExitConfirmationDialog
import com.denic.geoscan.ui.components.LanguagePickerDialog
import com.denic.geoscan.ui.localization.AppLanguage
import com.denic.geoscan.ui.localization.LocalAppLanguage
import com.denic.geoscan.ui.localization.LocalAppStrings
import com.denic.geoscan.ui.localization.getStrings
import com.denic.geoscan.ui.screens.remote.RemoteScanViewModel
import com.denic.geoscan.R
import com.denic.geoscan.ui.screens.hub.HubScreen
import com.denic.geoscan.ui.screens.local.LocalAnalysisScreen
import com.denic.geoscan.ui.screens.remote.RemoteScanScreen
import com.denic.geoscan.ui.theme.DarkBase
import com.denic.geoscan.ui.theme.DarkSurface
import com.denic.geoscan.ui.theme.GlassBackground
import com.denic.geoscan.ui.theme.GlassBorder
import com.denic.geoscan.ui.theme.NeonCyan
import com.denic.geoscan.ui.theme.NeonGreen
import com.denic.geoscan.ui.theme.NeonRed
import com.denic.geoscan.ui.theme.TextMuted
import com.denic.geoscan.ui.theme.TextPrimary
import com.denic.geoscan.ui.theme.TextSecondary
import com.denic.geoscan.ui.theme.Typography

@Composable
fun AppNavigation(
    startDestination: String = Screen.Hub.route,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val prefs = remember { context.getSharedPreferences("dnic_prefs", android.content.Context.MODE_PRIVATE) }
    var preferredStartScreen by remember {
        mutableStateOf(prefs.getString("start_screen", startDestination) ?: startDestination)
    }

    // Language state & first launch detection
    val savedLangCode = prefs.getString("app_language", "fa") ?: "fa"
    var currentLanguage by remember { mutableStateOf(AppLanguage.fromCode(savedLangCode)) }
    val isLanguageSelected = prefs.getBoolean("language_selected", false)
    var showLanguageDialog by remember { mutableStateOf(!isLanguageSelected) }

    val currentStrings = remember(currentLanguage) { getStrings(currentLanguage) }
    val layoutDirection = if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    val remoteViewModel: RemoteScanViewModel = viewModel()
    val remotePoints by remoteViewModel.scannedPoints.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }

    val initialRoute = when (startDestination) {
        Screen.LocalAnalysis.route, "local", "local_analysis" -> Screen.LocalAnalysis.route
        else -> Screen.Hub.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Hub.route

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalAppLanguage provides currentLanguage,
        LocalAppStrings provides currentStrings
    ) {
        // Handle system back navigation (press Back on Hub triggers Exit confirmation, elsewhere returns to Hub)
        BackHandler(enabled = true) {
            if (currentRoute != Screen.Hub.route) {
                navController.navigate(Screen.Hub.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            } else {
                showExitDialog = true
            }
        }

        // First launch language picker or manual switcher
        if (showLanguageDialog) {
            LanguagePickerDialog(
                currentLanguage = currentLanguage,
                onLanguageSelected = { lang ->
                    currentLanguage = lang
                    prefs.edit()
                        .putString("app_language", lang.code)
                        .putBoolean("language_selected", true)
                        .apply()
                    showLanguageDialog = false
                },
                onDismissRequest = if (isLanguageSelected) { { showLanguageDialog = false } } else null
            )
        }

        if (showExitDialog) {
            ExitConfirmationDialog(
                hasUnsavedBluetoothData = remotePoints.isNotEmpty(),
                unsavedPointsCount = remotePoints.size,
                onDismiss = { showExitDialog = false },
                onConfirmExit = {
                    if (remotePoints.isNotEmpty()) {
                        remoteViewModel.autoSaveCurrentSession()
                    }
                    showExitDialog = false
                    activity?.finish()
                },
                onShareBluetoothData = if (remotePoints.isNotEmpty()) {
                    { remoteViewModel.shareCurrentSession(context) }
                } else null
            )
        }

        Scaffold(
            containerColor = DarkBase,
            topBar = {
                TopBrandingBar(
                    currentRoute = currentRoute,
                    currentLanguage = currentLanguage,
                    onOpenLanguagePicker = { showLanguageDialog = true },
                    onLogoClick = {
                        if (currentRoute != Screen.Hub.route) {
                            navController.navigate(Screen.Hub.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                onNavigate = { screen ->
                    val targetRoute = screen?.route ?: return@TopBrandingBar
                    if (currentRoute != targetRoute) {
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        },
        bottomBar = {
            GlassmorphicBottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { screen ->
                    val targetRoute = screen?.route ?: return@GlassmorphicBottomNavBar
                    if (currentRoute != targetRoute) {
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = initialRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Hub.route) {
                HubScreen(
                    onNavigateTo = { targetScreen ->
                        navController.navigate(targetScreen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToGridDemo = { size ->
                        navController.navigate("local_analysis?grid=$size") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    preferredStartScreen = preferredStartScreen,
                    onToggleDefaultStartScreen = { nextScreen ->
                        preferredStartScreen = nextScreen
                        prefs.edit().putString("start_screen", nextScreen).apply()
                    }
                )
            }
            composable(Screen.LocalAnalysis.route) {
                LocalAnalysisScreen()
            }
            composable(
                route = "local_analysis?grid={grid}",
                arguments = listOf(
                    androidx.navigation.navArgument("grid") {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val gridArg = backStackEntry.arguments?.getString("grid")?.toIntOrNull()
                LocalAnalysisScreen(initialGridSize = gridArg)
            }
            composable(Screen.RemoteScan.route) {
                RemoteScanScreen(
                    viewModel = remoteViewModel,
                    onNavigateToLocal = {
                        navController.navigate(Screen.LocalAnalysis.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TopBrandingBar(
    currentRoute: String,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onOpenLanguagePicker: (() -> Unit)? = null,
    onLogoClick: (() -> Unit)? = null,
    onNavigate: ((Screen) -> Unit)? = null
) {
    val strings = LocalAppStrings.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(DarkBase),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 1100.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val clickModifier = if (onLogoClick != null) Modifier.clickable(onClick = onLogoClick) else Modifier

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = clickModifier
            ) {
                DenicBrandLogo(modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Dnic-GeoScan",
                        style = Typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "DIGITAL ELECTRONIC • 3D VISUALIZER",
                        style = Typography.labelSmall,
                        color = NeonGreen,
                        letterSpacing = 1.1.sp
                    )
                }
            }

            // Quick Actions: Language Selector Pill + Mode Switcher Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language Switcher Pill (1-tap opens language dialog)
                if (onOpenLanguagePicker != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { onOpenLanguagePicker() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.PERSIAN) "🇮🇷 فا" else "🇬🇧 EN",
                            style = Typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                if (onNavigate != null) {
                    val isLocalActive = currentRoute.startsWith(Screen.LocalAnalysis.route)
                    val isRemoteActive = currentRoute.startsWith(Screen.RemoteScan.route)

                    // Local Switch Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLocalActive) NeonGreen.copy(alpha = 0.2f) else DarkSurface)
                            .border(
                                1.dp,
                                if (isLocalActive) NeonGreen else GlassBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onNavigate(Screen.LocalAnalysis) }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = strings.pillLocal,
                            style = Typography.labelSmall,
                            color = if (isLocalActive) NeonGreen else TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Remote Switch Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isRemoteActive) NeonCyan.copy(alpha = 0.2f) else DarkSurface)
                            .border(
                                1.dp,
                                if (isRemoteActive) NeonCyan else GlassBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onNavigate(Screen.RemoteScan) }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = strings.pillRemote,
                            style = Typography.labelSmall,
                            color = if (isRemoteActive) NeonCyan else TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassmorphicBottomNavBar(
    currentRoute: String,
    onNavigate: (Screen) -> Unit
) {
    val strings = LocalAppStrings.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        val shape = RoundedCornerShape(24.dp)
        Row(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .clip(shape)
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            NeonGreen.copy(alpha = 0.4f),
                            GlassBorder,
                            NeonCyan.copy(alpha = 0.4f)
                        )
                    ),
                    shape = shape
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GlassBackground.copy(alpha = 0.85f),
                            DarkSurface.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navScreens = try {
                Screen.items.filterNotNull().ifEmpty {
                    listOf(Screen.Hub, Screen.LocalAnalysis, Screen.RemoteScan)
                }
            } catch (_: Throwable) {
                listOf(Screen.Hub, Screen.LocalAnalysis, Screen.RemoteScan)
            }

            navScreens.forEach { screen ->
                val route = screen.route
                val isSelected = currentRoute == route || currentRoute.startsWith("$route?")
                val activeColor = when (screen) {
                    Screen.Hub -> NeonCyan
                    Screen.LocalAnalysis -> NeonGreen
                    Screen.RemoteScan -> NeonCyan
                    else -> NeonCyan
                }

                val title = when (screen) {
                    Screen.Hub -> strings.navHub
                    Screen.LocalAnalysis -> strings.navLocalAnalysis
                    Screen.RemoteScan -> strings.navRemoteScan
                    else -> screen.title
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onNavigate(screen) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = title,
                            tint = if (isSelected) activeColor else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = title,
                            style = Typography.labelLarge,
                            color = if (isSelected) TextPrimary else TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DenicBrandLogo(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            lineTo(w * 0.85f, h * 0.27f)
            lineTo(w * 0.85f, h * 0.73f)
            lineTo(w * 0.5f, h * 0.92f)
            lineTo(w * 0.15f, h * 0.73f)
            lineTo(w * 0.15f, h * 0.27f)
            close()
        }
        drawPath(
            path = path,
            color = NeonCyan.copy(alpha = 0.35f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
        )

        drawLine(
            color = NeonRed,
            start = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.42f),
            end = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.42f),
            strokeWidth = 1.8.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = NeonCyan,
            start = androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.54f),
            end = androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.54f),
            strokeWidth = 2.2.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = NeonGreen,
            start = androidx.compose.ui.geometry.Offset(w * 0.20f, h * 0.66f),
            end = androidx.compose.ui.geometry.Offset(w * 0.80f, h * 0.66f),
            strokeWidth = 2.6.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        drawCircle(
            color = androidx.compose.ui.graphics.Color.White,
            radius = 2.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.54f)
        )
    }
}

