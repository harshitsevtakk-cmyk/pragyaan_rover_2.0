package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BluetoothConnectionState
import com.example.model.GasSafetyLevel
import com.example.model.LiquidGlassTheme
import com.example.ui.components.GlassBox
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassPill
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldTelem
import com.example.viewmodel.RoverViewModel

enum class RoverScreenTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DRIVE("Drive", Icons.Filled.TwoWheeler, Icons.Outlined.TwoWheeler),
    ARM("6-Axis Arm", Icons.Filled.PrecisionManufacturing, Icons.Outlined.PrecisionManufacturing),
    TELEMETRY("Telemetry", Icons.Filled.Sensors, Icons.Outlined.Sensors),
    TERMINAL("Terminal", Icons.Filled.Terminal, Icons.Outlined.Terminal)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoverApp(
    viewModel: RoverViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showBluetoothSheet by remember { mutableStateOf(false) }

    val connectionState by viewModel.connectionState.collectAsState()
    val isSimulationMode by viewModel.isSimulationMode.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val liquidTheme by viewModel.liquidTheme.collectAsState()
    val themeAccent = Color(liquidTheme.accentColor)

    // Wrap the entire app in iOS Liquid Glass Mesh Background
    LiquidGlassBackground(theme = liquidTheme) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent, // Transparent so liquid glass mesh shines through
            topBar = {
                GlassTopAppBar(
                    connectionState = connectionState,
                    isSimulationMode = isSimulationMode,
                    themeAccent = themeAccent,
                    onConnectionClick = { showBluetoothSheet = true },
                    onThemeCycleClick = {
                        val next = when (liquidTheme) {
                            LiquidGlassTheme.AURORA_CYAN -> LiquidGlassTheme.MIDNIGHT_NEBULA
                            LiquidGlassTheme.MIDNIGHT_NEBULA -> LiquidGlassTheme.CYBER_EMERALD
                            LiquidGlassTheme.CYBER_EMERALD -> LiquidGlassTheme.SOLAR_FLARE
                            LiquidGlassTheme.SOLAR_FLARE -> LiquidGlassTheme.AURORA_CYAN
                        }
                        viewModel.setLiquidTheme(next)
                    },
                    onEmergencyStop = { viewModel.emergencyStop() }
                )
            },
            bottomBar = {
                FloatingGlassNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    gasAlert = uiState.latestTelemetry.gasStatus == GasSafetyLevel.DANGER,
                    themeAccent = themeAccent
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (RoverScreenTab.entries[selectedTab]) {
                    RoverScreenTab.DRIVE -> DriveControlScreen(viewModel = viewModel)
                    RoverScreenTab.ARM -> RoboticArmScreen(viewModel = viewModel)
                    RoverScreenTab.TELEMETRY -> TelemetryScreen(viewModel = viewModel)
                    RoverScreenTab.TERMINAL -> TerminalScreen(viewModel = viewModel)
                }
            }
        }

        if (showBluetoothSheet) {
            BluetoothConnectionSheet(
                viewModel = viewModel,
                onDismiss = { showBluetoothSheet = false }
            )
        }
    }
}

@Composable
fun GlassTopAppBar(
    connectionState: BluetoothConnectionState,
    isSimulationMode: Boolean,
    themeAccent: Color,
    onConnectionClick: () -> Unit,
    onThemeCycleClick: () -> Unit,
    onEmergencyStop: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        GlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = Color.White.copy(alpha = 0.08f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Connection Status Glass Pill
                ConnectionGlassPill(
                    connectionState = connectionState,
                    themeAccent = themeAccent,
                    onClick = onConnectionClick
                )

                // Center Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ROVER",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "CONTROL",
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.sp,
                        fontSize = 15.sp,
                        color = themeAccent
                    )
                    if (isSimulationMode) {
                        Spacer(modifier = Modifier.width(6.dp))
                        GlassPill(accentColor = themeAccent, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "SIM",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeAccent,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Action icons: Theme cycle + Emergency STOP
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onThemeCycleClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Switch Atmosphere Theme",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    GlassButton(
                        onClick = onEmergencyStop,
                        shape = CircleShape,
                        backgroundColor = CrimsonAlert.copy(alpha = 0.25f),
                        activeAccent = CrimsonAlert,
                        modifier = Modifier.size(36.dp).testTag("appbar_emergency_stop")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Emergency Stop",
                            tint = CrimsonAlert,
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionGlassPill(
    connectionState: BluetoothConnectionState,
    themeAccent: Color,
    onClick: () -> Unit
) {
    val (dotColor, text) = when (connectionState) {
        is BluetoothConnectionState.Connected -> Pair(EmeraldTelem, connectionState.deviceName.take(9))
        is BluetoothConnectionState.Connecting -> Pair(AmberWarning, "Connecting")
        is BluetoothConnectionState.Error -> Pair(CrimsonAlert, "BT Error")
        BluetoothConnectionState.Disconnected -> Pair(Color.White.copy(alpha = 0.5f), "Offline")
    }

    GlassButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        backgroundColor = dotColor.copy(alpha = 0.15f),
        activeAccent = dotColor,
        modifier = Modifier.testTag("connection_status_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingGlassNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    gasAlert: Boolean,
    themeAccent: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("bottom_nav_bar")
    ) {
        GlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = Color(0xFF0F172A).copy(alpha = 0.75f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoverScreenTab.entries.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index

                    GlassButton(
                        onClick = { onTabSelected(index) },
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = if (isSelected) themeAccent.copy(alpha = 0.22f) else Color.Transparent,
                        activeAccent = themeAccent,
                        isActive = isSelected,
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (tab == RoverScreenTab.TELEMETRY && gasAlert) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = CrimsonAlert) {
                                            Text("!", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) themeAccent else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) themeAccent else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
