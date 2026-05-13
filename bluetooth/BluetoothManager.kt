package com.rcremote.model

import android.bluetooth.BluetoothDevice

enum class ControlMode { JOYSTICK, DPAD, TILT }

enum class DPadDirection { NONE, UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT }

data class JoystickState(
    val x: Float = 0f,  // -1f (left) to 1f (right)
    val y: Float = 0f   // -1f (back) to 1f (forward)
)

data class TiltState(
    val roll: Float = 0f,   // degrees, -90 to 90
    val pitch: Float = 0f   // degrees, -90 to 90
)

data class CarState(
    val speedKmh: Int = 0,
    val gear: String = "NEUTRAL",
    val rcBatteryPct: Int = 0,
    val isConnected: Boolean = false
)

data class RCCommand(
    val throttle: Int,   // -100 to 100 (negative = reverse)
    val steering: Int,   // -100 (full left) to 100 (full right)
    val turbo: Boolean = false,
    val brake: Boolean = false,
    val horn: Boolean = false,
    val lights: Boolean = false
) {
    // Serialises to a compact string the Arduino/ESP can parse
    // Format: T:{throttle},S:{steering},F:{flags}\n
    fun toCommand(): String {
        val flags = buildString {
            if (turbo) append("T")
            if (brake) append("B")
            if (horn) append("H")
            if (lights) append("L")
            if (isEmpty()) append("0")
        }
        return "T:$throttle,S:$steering,F:$flags\n"
    }
}

data class ScannedDevice(
    val device: BluetoothDevice,
    val name: String,
    val rssi: Int
)
