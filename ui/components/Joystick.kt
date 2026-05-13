package com.rcremote.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rcremote.ui.theme.*
import kotlin.math.*

@Composable
fun Joystick(
    size: Dp = 120.dp,
    onMove: (x: Float, y: Float) -> Unit,
    onRelease: () -> Unit = {}
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = Modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onRelease()
                        onMove(0f, 0f)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val center = Offset(this.size.width / 2f, this.size.height / 2f)
                        val raw = change.position - center
                        val radius = this.size.width / 2f * 0.55f
                        val dist = raw.getDistance()
                        knobOffset = if (dist <= radius) raw
                        else raw * (radius / dist)
                        val nx = (knobOffset.x / radius).coerceIn(-1f, 1f)
                        val ny = (-knobOffset.y / radius).coerceIn(-1f, 1f)
                        onMove(nx, ny)
                    }
                )
            }
    ) {
        val cx = size.toPx() / 2f
        val cy = size.toPx() / 2f
        val outerR = size.toPx() / 2f - 2.dp.toPx()
        val innerR = outerR * 0.65f
        val knobR = outerR * 0.28f

        // Outer ring
        drawCircle(color = BgCard, radius = outerR, center = Offset(cx, cy))
        drawCircle(color = Border, radius = outerR, center = Offset(cx, cy), style = Stroke(2.dp.toPx()))

        // Cross guides
        val guideLen = outerR * 0.25f
        val guideColor = Border
        drawLine(guideColor, Offset(cx, cy - outerR + 4.dp.toPx()), Offset(cx, cy - outerR + guideLen), 1.dp.toPx())
        drawLine(guideColor, Offset(cx, cy + outerR - guideLen), Offset(cx, cy + outerR - 4.dp.toPx()), 1.dp.toPx())
        drawLine(guideColor, Offset(cx - outerR + 4.dp.toPx(), cy), Offset(cx - outerR + guideLen, cy), 1.dp.toPx())
        drawLine(guideColor, Offset(cx + outerR - guideLen, cy), Offset(cx + outerR - 4.dp.toPx(), cy), 1.dp.toPx())

        // Inner ring
        drawCircle(color = BgPanel, radius = innerR, center = Offset(cx, cy))
        drawCircle(color = Border.copy(alpha = .5f), radius = innerR, center = Offset(cx, cy), style = Stroke(1.dp.toPx()))

        // Knob
        val kx = cx + knobOffset.x
        val ky = cy + knobOffset.y
        drawCircle(color = Indigo.copy(alpha = .9f), radius = knobR, center = Offset(kx, ky))
        drawCircle(color = Color.White.copy(alpha = .15f), radius = knobR * .45f, center = Offset(kx, ky))
        drawCircle(color = IndigoLight.copy(alpha = .6f), radius = knobR, center = Offset(kx, ky), style = Stroke(1.5.dp.toPx()))
    }
}
