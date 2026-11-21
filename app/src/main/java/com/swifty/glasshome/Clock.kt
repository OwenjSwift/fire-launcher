package com.swifty.glasshome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val softShadow = Shadow(Color.Black.copy(alpha = 0.30f), Offset(0f, 2f), 12f)

/**
 * The big clock: the date, then the time in large, light, white numbers with a soft glow
 * behind them. Plain text with the system font, so it shows up on every Fire TV.
 */
@Composable
fun GlassClock(now: Date, modifier: Modifier = Modifier) {
    val time = SimpleDateFormat("h:mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(now)

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(
            date,
            style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, shadow = softShadow),
        )
        BasicText(
            time,
            style = TextStyle(
                color = Color.White.copy(alpha = 0.94f),
                fontSize = Glass.clockSize,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp,
                shadow = Shadow(Color.Black.copy(alpha = 0.28f), Offset(0f, 4f), 24f),
            ),
        )
    }
}

/** Small glass pill for the top-right corner. */
@Composable
fun WeatherPill(w: Weather, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .clip(shape)
            .background(Glass.glass)
            .border(1.dp, Glass.glassEdge, shape)
            .padding(horizontal = 14.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText(w.symbol, style = TextStyle(fontSize = 14.sp))
        BasicText("${w.tempC}°", style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
        BasicText("H ${w.highC}°  L ${w.lowC}°", style = TextStyle(color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp))
    }
}
