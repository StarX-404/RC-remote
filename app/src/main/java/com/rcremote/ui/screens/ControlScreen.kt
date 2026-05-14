package com.rcremote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcremote.bluetooth.BtState
import com.rcremote.model.ControlMode
import com.rcremote.model.DPadDirection
import com.rcremote.model.TiltState
import com.rcremote.ui.components.*
import com.rcremote.ui.theme.*
import com.rcremote.viewmodel.RCViewModel

@Composable
fun ControlScreen(vm: RCViewModel, onDisconnect: () -> Unit) {
    val btState by vm.btState.collectAsState()
    val mode by vm.controlMode.collectAsState()
    val carState by vm.carState.collectAsState()
    val phoneBattery by vm.phoneBattery.collectAsState()
    val turbo by vm.turbo.collectAsState()
    val brake by vm.brake.collectAsState()
    val lights by vm.lights.collectAsState()
    val tilt by vm.tilt.collectAsState()

    LaunchedEffect(btState) {
        if (btState is BtState.Disconnected || btState is BtState.Error) onDisconnect()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── Top trigger bar ──────────────────────────────────────────────
            TopTriggerBar(
                turbo = turbo,
                brake = brake,
                rcBattery = carState.rcBatteryPct,
                phoneBattery = phoneBattery,
                connected = btState is BtState.Connected,
                deviceName = (btState as? BtState.Connected)?.device?.name ?: "—",
                onDisconnect = { vm.disconnect() }
            )

            // ── Main body ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left zone
                LeftZone(
                    mode = mode,
                    tilt = tilt,
                    onJoystickMove = { x, y -> vm.onJoystickMoved(x, y) },
                    onDPad = { vm.onDPad(it) },
                    onTurboDown = { vm.setTurbo(true) },
                    onTurboUp = { vm.setTurbo(false) },
                    onBrakeDown = { vm.setBrake(true) },
                    onBrakeUp = { vm.setBrake(false) }
                )

                // Center HUD
                CenterHud(
                    speedKmh = carState.speedKmh,
                    gear = carState.gear,
                    mode = mode,
                    onModeChange = { vm.setMode(it) },
                    onEStop = { vm.onJoystickMoved(0f, 0f); vm.onDPad(DPadDirection.NONE) },
                    lights = lights,
                    onToggleLights = { vm.toggleLights() }
                )

                // Right zone
                RightZone(
                    onHorn = { vm.setHorn(it) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top Trigger Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TopTriggerBar(
    turbo: Boolean, brake: Boolean,
    rcBattery: Int, phoneBattery: Int,
    connected: Boolean, deviceName: String,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(BgStrip)
            .border(0.5.dp, Border, RoundedCornerShape(0.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // L2 Trigger
        TriggerButton(
            label = "L2", sub = if (turbo) "TURBO ON" else "Turbo",
            active = turbo, activeColor = IndigoLight,
            modifier = Modifier.width(100.dp).fillMaxHeight()
        )

        // Center info
        Row(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(6.dp).clip(CircleShape)
                    .background(if (connected) GreenOn else RedBtn)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                if (connected) deviceName else "Disconnected",
                color = if (connected) GreenOn else RedBtn,
                fontSize = 10.sp, fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(12.dp))
            BatteryIndicator("RC", rcBattery)
            Spacer(Modifier.width(8.dp))
            BatteryIndicator("📱", phoneBattery)
        }

        // R2 Throttle
        TriggerButton(
            label = "R2", sub = if (brake) "BRAKE" else "Throttle",
            active = brake, activeColor = RedBtn,
            modifier = Modifier.width(100.dp).fillMaxHeight()
        )
    }
}

@Composable
private fun TriggerButton(
    label: String, sub: String, active: Boolean,
    activeColor: Color, modifier: Modifier
) {
    Box(
        modifier = modifier
            .background(if (active) activeColor.copy(alpha = .15f) else BgPanel)
            .border(0.5.dp, if (active) activeColor.copy(alpha = .4f) else Border),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = if (active) activeColor else TextSecondary,
                fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = if (active) activeColor.copy(.7f) else TextMuted, fontSize = 8.sp)
        }
    }
}

@Composable
private fun BatteryIndicator(icon: String, pct: Int) {
    val color = when {
        pct > 60 -> GreenOn
        pct > 30 -> AmberBtn
        else -> RedBtn
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(icon, fontSize = 9.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.CenterVertically) {
            val filled = (pct / 20).coerceIn(0, 5)
            repeat(5) { i ->
                Box(
                    Modifier.width(4.dp).height(7.dp).clip(RoundedCornerShape(1.dp))
                        .background(if (i < filled) color else Border)
                )
            }
        }
        Text("$pct%", color = color, fontSize = 9.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Left Zone
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LeftZone(
    mode: ControlMode, tilt: TiltState,
    onJoystickMove: (Float, Float) -> Unit,
    onDPad: (DPadDirection) -> Unit,
    onTurboDown: () -> Unit, onTurboUp: () -> Unit,
    onBrakeDown: () -> Unit, onBrakeUp: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .fillMaxHeight()
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // L1 strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(BgCard)
                .border(0.5.dp, Border, RoundedCornerShape(7.dp))
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        onTurboDown()
                        tryAwaitRelease()
                        onTurboUp()
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            Text("L1  ·  Turbo", color = TextMuted, fontSize = 8.sp)
        }

        // Main control
        when (mode) {
            ControlMode.JOYSTICK -> Joystick(size = 110.dp, onMove = onJoystickMove)
            ControlMode.DPAD -> DPadControl(size = 110.dp, onDirection = onDPad)
            ControlMode.TILT -> TiltVisualizer(tilt = tilt)
        }

        // L2 hint (actual trigger is top bar)
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(BgCard)
                .border(0.5.dp, Border, RoundedCornerShape(7.dp))
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        onBrakeDown()
                        tryAwaitRelease()
                        onBrakeUp()
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            Text("L2  ·  Brake", color = TextMuted, fontSize = 8.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Center HUD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CenterHud(
    speedKmh: Int, gear: String, mode: ControlMode,
    onModeChange: (ControlMode) -> Unit,
    onEStop: () -> Unit,
    lights: Boolean, onToggleLights: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Speedometer(speedKmh = speedKmh, gear = gear, size = 100.dp)

        // Mode switcher
        ModeSegment(mode = mode, onModeChange = onModeChange)

        // Controls row
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lights
            SmallActionButton(
                label = "LIGHTS",
                active = lights,
                activeColor = AmberBtn,
                onClick = onToggleLights
            )
            // E-Stop
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(EStopBg)
                    .border(1.5.dp, EStopRed, CircleShape)
                    .pointerInput(Unit) { detectTapGestures { onEStop() } },
                contentAlignment = Alignment.Center
            ) {
                Text("■", color = RedBtn, fontSize = 14.sp)
            }
            // Select / Start placeholders
            SmallActionButton(label = "MENU", active = false, activeColor = IndigoLight) {}
        }
    }
}

@Composable
private fun ModeSegment(mode: ControlMode, onModeChange: (ControlMode) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(BgStrip)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ControlMode.values().forEach { m ->
            val selected = m == mode
            val label = when (m) {
                ControlMode.JOYSTICK -> "Stick"
                ControlMode.DPAD -> "D-Pad"
                ControlMode.TILT -> "Tilt"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (selected) Indigo else Color.Transparent)
                    .pointerInput(Unit) { detectTapGestures { onModeChange(m) } }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (selected) Color.White else TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SmallActionButton(
    label: String, active: Boolean,
    activeColor: Color, onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) activeColor.copy(.15f) else BgCard)
            .border(0.5.dp, if (active) activeColor.copy(.5f) else Border, RoundedCornerShape(8.dp))
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (active) activeColor else TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Right Zone — Face buttons
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RightZone(onHorn: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .fillMaxHeight()
            .padding(end = 12.dp, top = 8.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(BgCard)
                .border(0.5.dp, Border, RoundedCornerShape(7.dp))
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        onHorn(true); tryAwaitRelease(); onHorn(false)
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            Text("R1  ·  Horn", color = TextMuted, fontSize = 8.sp)
        }

        FaceButtons()

        // LED indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Indicator("LED", GreenOn)
            Indicator("CAM", TextMuted)
            Indicator("BT", Indigo)
        }
    }
}

@Composable
private fun FaceButtons() {
    val btns = listOf(
        Triple("Y", GreenOn, Color(0xFF0F1F0F)),
        Triple("X", Indigo, Color(0xFF0F0F1F)),
        Triple("B", OrangeBtn, Color(0xFF1F120A)),
        Triple("A", RedBtn, Color(0xFF1F0A0A))
    )
    Box(Modifier.size(90.dp)) {
        // Y top
        FaceBtn(btns[0], Modifier.align(Alignment.TopCenter))
        // X left
        FaceBtn(btns[1], Modifier.align(Alignment.CenterStart))
        // B right
        FaceBtn(btns[2], Modifier.align(Alignment.CenterEnd))
        // A bottom
        FaceBtn(btns[3], Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun FaceBtn(data: Triple<String, Color, Color>, modifier: Modifier) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(data.third)
            .border(1.5.dp, data.second.copy(.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(data.first, color = data.second, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Indicator(label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(label, color = TextMuted, fontSize = 7.sp)
    }
}

