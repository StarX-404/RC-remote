package com.rcremote.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.rcremote.model.RCCommand
import com.rcremote.model.ScannedDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.util.UUID

// Standard SPP UUID for Classic Bluetooth (HC-05, HC-06, ESP32 BT Classic)
private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

sealed class BtState {
    object Disconnected : BtState()
    object Scanning : BtState()
    data class Connecting(val device: BluetoothDevice) : BtState()
    data class Connected(val device: BluetoothDevice) : BtState()
    data class Error(val msg: String) : BtState()
}

@SuppressLint("MissingPermission")
class RCBluetoothManager(private val ctx: Context) {

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var writeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _state = MutableStateFlow<BtState>(BtState.Disconnected)
    val state: StateFlow<BtState> = _state.asStateFlow()

    private val _scanned = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val scanned: StateFlow<List<ScannedDevice>> = _scanned.asStateFlow()

    // Incoming data from car (speed, battery, etc.)
    private val _incoming = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val incoming: SharedFlow<String> = _incoming.asSharedFlow()

    // Command queue — only keep latest command to avoid lag
    private val _cmdFlow = MutableStateFlow<RCCommand?>(null)

    fun getBondedDevices(): List<ScannedDevice> =
        adapter?.bondedDevices?.map { d ->
            ScannedDevice(d, d.name ?: "Unknown", -70)
        } ?: emptyList()

    fun connect(device: BluetoothDevice) {
        scope.launch {
            _state.value = BtState.Connecting(device)
            try {
                adapter?.cancelDiscovery()
                val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
                s.connect()
                socket = s
                _state.value = BtState.Connected(device)
                launchReadLoop(s)
                launchWriteLoop(s)
            } catch (e: IOException) {
                _state.value = BtState.Error("Connection failed: ${e.message}")
            }
        }
    }

    fun disconnect() {
        writeJob?.cancel()
        try { socket?.close() } catch (_: IOException) {}
        socket = null
        _state.value = BtState.Disconnected
    }

    fun sendCommand(cmd: RCCommand) {
        _cmdFlow.value = cmd
    }

    private fun launchWriteLoop(s: BluetoothSocket) {
        writeJob = scope.launch {
            // Throttle commands to ~50Hz to avoid flooding the serial buffer
            _cmdFlow
                .filterNotNull()
                .sample(20L)
                .collect { cmd ->
                    try {
                        s.outputStream.write(cmd.toCommand().toByteArray())
                    } catch (e: IOException) {
                        _state.value = BtState.Error("Write error: ${e.message}")
                        cancel()
                    }
                }
        }
    }

    private fun launchReadLoop(s: BluetoothSocket) {
        scope.launch {
            val buf = ByteArray(256)
            val sb = StringBuilder()
            while (isActive) {
                try {
                    val n = s.inputStream.read(buf)
                    sb.append(String(buf, 0, n))
                    // Emit complete lines
                    var idx: Int
                    while (sb.indexOf("\n").also { idx = it } != -1) {
                        val line = sb.substring(0, idx).trim()
                        if (line.isNotEmpty()) _incoming.emit(line)
                        sb.delete(0, idx + 1)
                    }
                } catch (e: IOException) {
                    if (isActive) _state.value = BtState.Error("Read error: ${e.message}")
                    break
                }
            }
        }
    }

    fun destroy() {
        disconnect()
        scope.cancel()
    }
}
