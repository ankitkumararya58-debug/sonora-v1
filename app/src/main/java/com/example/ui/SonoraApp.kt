package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.components.CustomizeSection
import com.example.ui.components.NowPlayingSection
import com.example.ui.components.SettingsSection
import com.example.ui.theme.SonoraAccentCyan
import com.example.ui.theme.SonoraAccentSilver
import com.example.ui.theme.SonoraBackground
import com.example.ui.theme.SonoraBorder
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceVariant
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary

enum class SonoraTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq, "tab_home"),
    CUSTOMIZE("Customize", Icons.Filled.Palette, Icons.Outlined.Palette, "tab_customize"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
}

@Composable
fun SonoraApp(viewModel: SonoraViewModel) {
    var currentTab by rememberSaveable { mutableStateOf(SonoraTab.HOME) }
    val trackState by viewModel.trackState.collectAsState()
    val widgetSettings by viewModel.widgetSettings.collectAsState()
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Recheck permissions and active media on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // Android back handler: pop to Home before exiting
    BackHandler(enabled = currentTab != SonoraTab.HOME) {
        currentTab = SonoraTab.HOME
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(SonoraBackground),
        containerColor = SonoraBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                color = SonoraSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SonoraBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                NavigationBar(
                    containerColor = SonoraSurface,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    SonoraTab.values().forEach { tab ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    tint = if (selected) SonoraAccentCyan else SonoraTextMuted
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) SonoraAccentSilver else SonoraTextMuted
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = SonoraSurfaceVariant,
                                selectedIconColor = SonoraAccentCyan,
                                unselectedIconColor = SonoraTextMuted
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_animation"
            ) { targetTab ->
                when (targetTab) {
                    SonoraTab.HOME -> NowPlayingSection(
                        viewModel = viewModel,
                        trackState = trackState,
                        widgetSettings = widgetSettings,
                        isPermissionGranted = isPermissionGranted,
                        modifier = Modifier.fillMaxSize()
                    )
                    SonoraTab.CUSTOMIZE -> CustomizeSection(
                        viewModel = viewModel,
                        trackState = trackState,
                        widgetSettings = widgetSettings,
                        modifier = Modifier.fillMaxSize()
                    )
                    SonoraTab.SETTINGS -> SettingsSection(
                        viewModel = viewModel,
                        isPermissionGranted = isPermissionGranted,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
