package com.rcremote.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcremote.bluetooth.BtState
import com.rcremote.model.ScannedDevice
import com.rcremote.ui.theme.*
import com.rcremote.viewmodel.RCViewModel

@Composable
fun DeviceScreen(vm: RCViewModel, onConnected: () -> Unit) {
    val ctx = LocalContext.current
    val btState by vm.btState.collectAsState()
    var devices by remember { mutableStateOf<List<ScannedDevice>>(emptyList()) }
    var permissionGranted by remember { mutableStateOf(false) }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionGranted = results.values.all { it }
        if (permissionGranted) devices = vm.getBondedDevices()
    }

    val btEnableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { devices = vm.getBondedDevices() }

    LaunchedEffect(Unit) { permLauncher.launch(permissions) }

    LaunchedEffect(btState) {
        if (btState is BtState.Connected) onConnected()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Text("RC Remote", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text("Select your RC car device", fontSize = 14.sp, color = TextSecondary)

        Spacer(Modifier.height(32.dp))

        // Enable BT banner
        if (!permissionGranted) {
            InfoBanner("Bluetooth permissions required. Tap to grant.") {
                permLauncher.launch(permissions)
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Paired Devices", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            TextButton(onClick = {
                btEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }) {
                Text("Enable BT", color = Indigo, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (devices.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgPanel)
                    .border(1.dp, Border, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No paired devices found.\nPair your HC-05 / HC-06 / ESP32 in Android Bluetooth settings.",
                    color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices) { dev ->
                    DeviceRow(
                        device = dev,
                        isConnecting = btState is BtState.Connecting &&
                                (btState as BtState.Connecting).device.address == dev.device.address,
                        onClick = { vm.connect(dev.device) }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (btState is BtState.Error) {
            Text((btState as BtState.Error).msg, color = RedBtn, fontSize = 12.sp)
        }

        if (btState is BtState.Connecting) {
            CircularProgressIndicator(color = Indigo, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun DeviceRow(device: ScannedDevice, isConnecting: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgPanel)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(device.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(device.device.address, color = TextMuted, fontSize = 11.sp)
        }
        if (isConnecting) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Indigo, strokeWidth = 2.dp)
        } else {
            Text("${device.rssi} dBm", color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun InfoBanner(msg: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Indigo.copy(alpha = .1f))
            .border(1.dp, Indigo.copy(alpha = .3f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(msg, color = IndigoLight, fontSize = 12.sp)
    }
    Spacer(Modifier.height(12.dp))
}
