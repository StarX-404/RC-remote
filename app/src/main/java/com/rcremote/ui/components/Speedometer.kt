package com.rcremote.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcremote.ui.theme.*

@Composable
fun Speedometer(
    speedKmh: Int,
    gear: String,
    size: Dp = 100.dp
) {
    val maxSpeed = 120f
    val fraction = (speedKmh / maxSpeed).coerceIn(0f, 1f)

    val arcColor = when {
        fraction > 0.75f -> RedBtn
        fraction > 0.45f -> AmberBtn
        else -> Indigo
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.toPx() * 0.07f
            val inset = stroke / 2f + 4.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val startAngle = 135f
            val sweepTotal = 270f

            // Background arc
            drawArc(
                color = BgCard,
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )

            // Filled arc
            if (fraction > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = startAngle,
                    sweepAngle = sweepTotal * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$speedKmh",
                color = TextPrimary,
                fontSize = (size.value * 0.22f).sp,
                fontWeight = FontWeight.Medium,
                lineHeight = (size.value * 0.22f).sp
            )
            Text(
                text = "km/h",
                color = TextMuted,
                fontSize = (size.value * 0.09f).sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = gear,
                color = arcColor,
                fontSize = (size.value * 0.09f).sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}
