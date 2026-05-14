package com.rcremote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcremote.model.TiltState
import com.rcremote.ui.theme.*

@Composable
fun TiltVisualizer(tilt: TiltState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        // Phone icon that rotates with tilt
        Box(
            modifier = Modifier
                .size(width = 38.dp, height = 58.dp)
                .rotate(tilt.roll.coerceIn(-30f, 30f))
                .clip(RoundedCornerShape(10.dp))
                .background(BgCard)
                .border(1.5.dp, Indigo, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("📱", fontSize = 20.sp)
        }

        // Axis readings
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AxisPill(label = "Roll", value = tilt.roll, color = Indigo)
            AxisPill(label = "Pitch", value = tilt.pitch, color = IndigoLight)
        }

        Text(
            "Tilt phone to steer & throttle",
            color = TextMuted,
            fontSize = 8.sp
        )
    }
}

@Composable
private fun AxisPill(label: String, value: Float, color: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BgCard)
            .border(0.5.dp, Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${if (value >= 0) "+" else ""}${"%.1f".format(value)}°",
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(label, color = TextMuted, fontSize = 8.sp)
    }
}
