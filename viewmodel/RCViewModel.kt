package com.rcremote.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rcremote.bluetooth.BtState
import com.rcremote.bluetooth.RCBluetoothManager
import com.rcremote.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class RCViewModel(app: Application) : AndroidViewModel(app), SensorEventListener {

    val btManager = RCBluetoothManager(app)
    val btState = btManager.state

    // --- Control state ---
    private val _controlMode = MutableStateFlow(ControlMode.JOYSTICK)
    val controlMode: StateFlow<ControlMode> = _controlMode.asStateFlow()

    private val _joystick = MutableStateFlow(JoystickState())
    val joystick: StateFlow<JoystickState> = _joystick.asStateFlow()

    private val _dpad = MutableStateFlow(DPadDirection.NONE)
    val dpad: StateFlow<DPadDirection> = _dpad.asStateFlow()

    private val _tilt = MutableStateFlow(TiltState())
    val tilt: StateFlow<TiltState> = _tilt.asStateFlow()

    private val _turbo = MutableStateFlow(false)
    val turbo: StateFlow<Boolean> = _turbo.asStateFlow()

    private val _brake = MutableStateFlow(false)
    val brake: StateFlow<Boolean> = _brake.asStateFlow()

    private val _horn = MutableStateFlow(false)
    val horn: StateFlow<Boolean> = _horn.asStateFlow()

    private val _lights = MutableStateFlow(false)
    val lights: StateFlow<Boolean> = _lights.asStateFlow()

    private val _speedLimit = MutableStateFlow(80) // 0-100%
    val speedLimit: StateFlow<Int> = _speedLimit.asStateFlow()

    // --- Car telemetry (parsed from BT incoming) ---
    private val _carState = MutableStateFlow(CarState())
    val carState: StateFlow<CarState> = _carState.asStateFlow()

    private val _phoneBattery = MutableStateFlow(100)
    val phoneBattery: StateFlow<Int> = _phoneBattery.asStateFlow()

    // --- Sensor ---
    private val sensorManager = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    init {
        // Parse incoming telemetry from car
        viewModelScope.launch {
            btManager.incoming.collect { line -> parseTelemetry(line) }
        }
        // Send commands reactively whenever any control state changes
        viewModelScope.launch {
            combine(
                _joystick, _dpad, _tilt, _turbo, _brake, _horn, _lights, _controlMode, _speedLimit
            ) { arr ->
                buildCommand(
                    joystick = arr[0] as JoystickState,
                    dpad = arr[1] as DPadDirection,
                    tilt = arr[2] as TiltState,
                    turbo = arr[3] as Boolean,
                    brake = arr[4] as Boolean,
                    horn = arr[5] as Boolean,
                    lights = arr[6] as Boolean,
                    mode = arr[7] as ControlMode,
                    limit = arr[8] as Int
                )
            }.collect { cmd -> btManager.sendCommand(cmd) }
        }
    }

    // --- Public actions ---
    fun setMode(mode: ControlMode) {
        _controlMode.value = mode
        if (mode == ControlMode.TILT) registerTilt() else unregisterTilt()
    }

    fun connect(device: BluetoothDevice) = btManager.connect(device)
    fun disconnect() = btManager.disconnect()
    fun getBondedDevices() = btManager.getBondedDevices()

    fun onJoystickMoved(x: Float, y: Float) { _joystick.value = JoystickState(x, y) }
    fun onDPad(dir: DPadDirection) { _dpad.value = dir }
    fun setTurbo(v: Boolean) { _turbo.value = v }
    fun setBrake(v: Boolean) { _brake.value = v }
    fun setHorn(v: Boolean) { _horn.value = v }
    fun toggleLights() { _lights.value = !_lights.value }
    fun setSpeedLimit(v: Int) { _speedLimit.value = v }

    // --- Command builder ---
    private fun buildCommand(
        joystick: JoystickState, dpad: DPadDirection, tilt: TiltState,
        turbo: Boolean, brake: Boolean, horn: Boolean, lights: Boolean,
        mode: ControlMode, limit: Int
    ): RCCommand {
        val scale = limit / 100f
        val (throttle, steering) = when (mode) {
            ControlMode.JOYSTICK -> {
                val t = (joystick.y * 100 * scale).roundToInt()
                val s = (joystick.x * 100).roundToInt()
                t to s
            }
            ControlMode.DPAD -> {
                val t = when (dpad) {
                    DPadDirection.UP, DPadDirection.UP_LEFT, DPadDirection.UP_RIGHT ->
                        (80 * scale).roundToInt()
                    DPadDirection.DOWN, DPadDirection.DOWN_LEFT, DPadDirection.DOWN_RIGHT ->
                        -(60 * scale).roundToInt()
                    else -> 0
                }
                val s = when (dpad) {
                    DPadDirection.LEFT, DPadDirection.UP_LEFT, DPadDirection.DOWN_LEFT -> -70
                    DPadDirection.RIGHT, DPadDirection.UP_RIGHT, DPadDirection.DOWN_RIGHT -> 70
                    else -> 0
                }
                t to s
            }
            ControlMode.TILT -> {
                // pitch → throttle, roll → steering
                val t = (tilt.pitch.coerceIn(-45f, 45f) / 45f * 100 * scale).roundToInt()
                val s = (tilt.roll.coerceIn(-45f, 45f) / 45f * 100).roundToInt()
                t to s
            }
        }
        return RCCommand(
            throttle = if (brake) 0 else throttle,
            steering = steering,
            turbo = turbo,
            brake = brake,
            horn = horn,
            lights = lights
        )
    }

    // --- Telemetry parser ---
    // Expected format from car: "SPD:42,BAT:85,GEAR:DRIVE"
    private fun parseTelemetry(line: String) {
        val map = line.split(",").mapNotNull {
            val parts = it.split(":")
            if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
        }.toMap()
        _carState.value = _carState.value.copy(
            speedKmh = map["SPD"]?.toIntOrNull() ?: _carState.value.speedKmh,
            rcBatteryPct = map["BAT"]?.toIntOrNull() ?: _carState.value.rcBatteryPct,
            gear = map["GEAR"] ?: _carState.value.gear,
            isConnected = true
        )
    }

    // --- Tilt sensor ---
    private fun registerTilt() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun unregisterTilt() = sensorManager.unregisterListener(this)

    override fun onSensorChanged(e: SensorEvent) {
        if (e.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotMat = FloatArray(9)
            val orientation = FloatArray(3)
            SensorManager.getRotationMatrixFromVector(rotMat, e.values)
            SensorManager.getOrientation(rotMat, orientation)
            val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
            val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
            _tilt.value = TiltState(roll = roll, pitch = pitch)
        }
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    override fun onCleared() {
        super.onCleared()
        unregisterTilt()
        btManager.destroy()
    }
}
