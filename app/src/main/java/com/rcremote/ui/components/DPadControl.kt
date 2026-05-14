package com.rcremote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcremote.model.DPadDirection
import com.rcremote.ui.theme.*
import kotlin.math.abs

@Composable
fun DPadControl(
    size: Dp = 110.dp,
    onDirection: (DPadDirection) -> Unit
) {
    var active by remember { mutableStateOf(DPadDirection.NONE) }

    fun resolve(offset: Offset): DPadDirection {
        val threshold = 12f
        val x = offset.x
        val y = offset.y
        if (abs(x) < threshold && abs(y) < threshold) return DPadDirection.NONE
        return when {
            abs(y) > abs(x) * 1.5f -> if (y < 0) DPadDirection.UP else DPadDirection.DOWN
            abs(x) > abs(y) * 1.5f -> if (x < 0) DPadDirection.LEFT else DPadDirection.RIGHT
            x < 0 && y < 0 -> DPadDirection.UP_LEFT
            x > 0 && y < 0 -> DPadDirection.UP_RIGHT
            x < 0 && y > 0 -> DPadDirection.DOWN_LEFT
            else -> DPadDirection.DOWN_RIGHT
        }
    }

    val btnSize = size / 3.2f
    val centerSize = btnSize * 0.9f
    val r = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .size(size)
            .pointerInput(Unit) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                detectDragGestures(
                    onDragEnd = { active = DPadDirection.NONE; onDirection(DPadDirection.NONE) },
                    onDrag = { change, _ ->
                        change.consume()
                        val dir = resolve(change.position - center)
                        if (dir != active) { active = dir; onDirection(dir) }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    val center = Offset(this.size.width / 2f, this.size.height / 2f)
                    val dir = resolve(it - center)
                    active = dir; onDirection(dir)
                    tryAwaitRelease()
                    active = DPadDirection.NONE; onDirection(DPadDirection.NONE)
                })
            },
        contentAlignment = Alignment.Center
    ) {
        // Up
        DPadBtn("▲", active == DPadDirection.UP || active == DPadDirection.UP_LEFT || active == DPadDirection.UP_RIGHT,
            Modifier.size(btnSize).align(Alignment.TopCenter))
        // Down
        DPadBtn("▼", active == DPadDirection.DOWN || active == DPadDirection.DOWN_LEFT || active == DPadDirection.DOWN_RIGHT,
            Modifier.size(btnSize).align(Alignment.BottomCenter))
        // Left
        DPadBtn("◀", active == DPadDirection.LEFT || active == DPadDirection.UP_LEFT || active == DPadDirection.DOWN_LEFT,
            Modifier.size(btnSize).align(Alignment.CenterStart))
        // Right
        DPadBtn("▶", active == DPadDirection.RIGHT || active == DPadDirection.UP_RIGHT || active == DPadDirection.DOWN_RIGHT,
            Modifier.size(btnSize).align(Alignment.CenterEnd))
        // Center
        Box(
            Modifier.size(centerSize).clip(RoundedCornerShape(6.dp))
                .background(BgDeep).border(0.5.dp, Border, RoundedCornerShape(6.dp))
        )
    }
}

@Composable
private fun DPadBtn(symbol: String, active: Boolean, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) Indigo else BgCard)
            .border(0.5.dp, if (active) IndigoLight.copy(.5f) else Border, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = if (active) TextPrimary else TextMuted,
            fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
