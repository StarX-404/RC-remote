package com.rcremote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Indigo = Color(0xFF6C63FF)
val IndigoLight = Color(0xFFA78BFA)
val IndigoDark = Color(0xFF4B44CC)
val BgDeep = Color(0xFF0B0B10)
val BgPanel = Color(0xFF111118)
val BgCard = Color(0xFF16162A)
val BgStrip = Color(0xFF13131E)
val Border = Color(0xFF1E1E2A)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF888888)
val TextMuted = Color(0xFF444444)
val GreenOn = Color(0xFF4ADE80)
val OrangeBtn = Color(0xFFF97316)
val RedBtn = Color(0xFFEF4444)
val AmberBtn = Color(0xFFFBBF24)
val EStopRed = Color(0xFF7F1D1D)
val EStopBg = Color(0xFF1E0808)

private val RCColorScheme = darkColorScheme(
    primary = Indigo,
    secondary = IndigoLight,
    background = BgDeep,
    surface = BgPanel,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun RCRemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RCColorScheme,
        content = content
    )
}
