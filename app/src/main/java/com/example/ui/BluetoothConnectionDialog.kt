package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.BluetoothConnectionState
import com.example.model.BluetoothDeviceInfo
import com.example.model.LiquidGlassTheme
import com.example.ui.components.GlassBox
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldTelem
import com.example.viewmodel.RoverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothConnectionSheet(
    viewModel: RoverViewModel,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val isSimulationMode by viewModel.isSimulationMode.collectAsState()
    val liquidTheme by viewModel.liquidTheme.collectAsState()
    val themeAccent = Color(liquidTheme.accentColor)

    var hasBluetoothPermissions by remember {
        mutableStateOf(checkBluetoothPermissions(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasBluetoothPermissions = results.values.all { it }
        if (hasBluetoothPermissions) {
            viewModel.refreshPaired()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasBluetoothPermissions && !isSimulationMode) {
            requestAppropriatePermissions(permissionLauncher)
        } else {
            viewModel.refreshPaired()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .testTag("bluetooth_connection_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassPill(accentColor = themeAccent, shape = CircleShape, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Bluetooth Icon",
                            tint = themeAccent,
                            modifier = Modifier
                                .size(22.dp)
                                .align(Alignment.Center)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Connection & Atmosphere",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "HC-05 SPP & Liquid Glass Styling",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_sheet_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Background Theme Selector
            GlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color.White.copy(alpha = 0.08f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = themeAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Liquid Glass Atmosphere Theme",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiquidGlassTheme.entries.forEach { theme ->
                            val isSelected = liquidTheme == theme
                            val accent = Color(theme.accentColor)
                            GlassButton(
                                onClick = { viewModel.setLiquidTheme(theme) },
                                shape = RoundedCornerShape(12.dp),
                                backgroundColor = if (isSelected) accent.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f),
                                activeAccent = accent,
                                isActive = isSelected,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = theme.title.split(" ").first(),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) accent else Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current Connection Status Glass Card
            ConnectionStatusGlassBanner(
                connectionState = connectionState,
                themeAccent = themeAccent,
                onDisconnect = { viewModel.disconnectDevice() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Simulation Mode Switcher
            GlassBox(
                backgroundColor = if (isSimulationMode) themeAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Simulation Mode",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            GlassPill(
                                accentColor = if (isSimulationMode) themeAccent else Color.Gray,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isSimulationMode) "ACTIVE" else "OFF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSimulationMode) themeAccent else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Test rover controls without physical HC-05 module",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = isSimulationMode,
                        onCheckedChange = { viewModel.toggleSimulation(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = themeAccent,
                            checkedTrackColor = themeAccent.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.testTag("simulation_mode_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Row: Refresh & Scan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Paired Devices (${pairedDevices.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row {
                    GlassButton(
                        onClick = { viewModel.refreshPaired() },
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.testTag("refresh_paired_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh", fontSize = 11.sp, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassButton(
                        onClick = {
                            if (isScanning) viewModel.stopScan() else viewModel.startScan()
                        },
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = if (isScanning) CrimsonAlert.copy(alpha = 0.3f) else themeAccent.copy(alpha = 0.3f),
                        activeAccent = themeAccent,
                        modifier = Modifier.testTag("scan_bluetooth_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop", fontSize = 11.sp, color = Color.White)
                            } else {
                                Icon(imageVector = Icons.Default.BluetoothSearching, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scan", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Devices List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .height(240.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pairedDevices.isEmpty() && discoveredDevices.isEmpty()) {
                    item {
                        GlassBox(
                            backgroundColor = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(imageVector = Icons.Default.Devices, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("No devices found", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            }
                        }
                    }
                }

                items(pairedDevices) { device ->
                    DeviceGlassRowItem(
                        device = device,
                        connectionState = connectionState,
                        themeAccent = themeAccent,
                        onConnect = { viewModel.connectDevice(device) }
                    )
                }

                if (discoveredDevices.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Discovered Devices (${discoveredDevices.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = themeAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(discoveredDevices) { device ->
                        DeviceGlassRowItem(
                            device = device,
                            connectionState = connectionState,
                            themeAccent = themeAccent,
                            onConnect = { viewModel.connectDevice(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusGlassBanner(
    connectionState: BluetoothConnectionState,
    themeAccent: Color,
    onDisconnect: () -> Unit
) {
    val (statusColor, statusText, subText) = when (connectionState) {
        is BluetoothConnectionState.Connected -> Triple(
            EmeraldTelem,
            "Connected to ${connectionState.deviceName}",
            "SPP: 00001101-0000-1000-8000-00805F9B34FB"
        )
        is BluetoothConnectionState.Connecting -> Triple(
            AmberWarning,
            "Connecting to ${connectionState.deviceName}...",
            "Negotiating RFCOMM"
        )
        is BluetoothConnectionState.Error -> Triple(
            CrimsonAlert,
            "Connection Error",
            connectionState.message
        )
        BluetoothConnectionState.Disconnected -> Triple(
            Color.White.copy(alpha = 0.6f),
            "Disconnected",
            "Select an HC-05 device below to connect"
        )
    }

    GlassBox(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = statusColor.copy(alpha = 0.15f),
        borderColor = statusColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            if (connectionState is BluetoothConnectionState.Connected) {
                GlassButton(
                    onClick = onDisconnect,
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = CrimsonAlert.copy(alpha = 0.25f),
                    modifier = Modifier.testTag("disconnect_rover_button")
                ) {
                    Text(
                        text = "Disconnect",
                        fontSize = 11.sp,
                        color = CrimsonAlert,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceGlassRowItem(
    device: BluetoothDeviceInfo,
    connectionState: BluetoothConnectionState,
    themeAccent: Color,
    onConnect: () -> Unit
) {
    val isConnected = connectionState is BluetoothConnectionState.Connected &&
            connectionState.address == device.address
    val isConnecting = connectionState is BluetoothConnectionState.Connecting &&
            connectionState.address == device.address

    GlassBox(
        shape = RoundedCornerShape(12.dp),
        backgroundColor = if (isConnected) EmeraldTelem.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
        borderColor = if (isConnected) EmeraldTelem else Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnected && !isConnecting) { onConnect() }
            .testTag("device_row_${device.address.replace(":", "_")}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                GlassPill(
                    accentColor = if (device.isHc05Likely) themeAccent else Color.Gray,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (device.isHc05Likely) Icons.Default.Sensors else Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (device.isHc05Likely) themeAccent else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.Center)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (device.isHc05Likely) {
                            Spacer(modifier = Modifier.width(6.dp))
                            GlassPill(accentColor = themeAccent, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "HC-05",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeAccent,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            when {
                isConnected -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldTelem, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Linked", color = EmeraldTelem, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                isConnecting -> {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = themeAccent)
                }
                else -> {
                    GlassButton(
                        onClick = onConnect,
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = themeAccent.copy(alpha = 0.35f),
                        activeAccent = themeAccent,
                        modifier = Modifier.testTag("connect_btn_${device.address.replace(":", "_")}")
                    ) {
                        Text(
                            text = "Connect",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun checkBluetoothPermissions(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}

private fun requestAppropriatePermissions(launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        launcher.launch(arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        ))
    } else {
        launcher.launch(arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        ))
    }
}
