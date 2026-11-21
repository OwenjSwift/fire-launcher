package com.swifty.glasshome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The big lock-screen style clock. The "liquid glass" look is three layers of the same text:
 * a soft shadow underneath, a see-through body that's brighter at the top, and a thin bright
 * rim around the edge of each number, like light catching on glass.
 */
@Composable
fun GlassClock(now: Date, weather: Weather?, modifier: Modifier = Modifier) {
    val time = SimpleDateFormat("h:mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(now)

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(
            date,
            style = TextStyle(
                color = Color.White.copy(alpha = 0.9f), fontSize = 20.sp, fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                shadow = Shadow(Color.Black.copy(alpha = 0.25f), Offset(0f, 2f), 8f),
            ),
        )

        val base = TextStyle(
            fontSize = Glass.clockSize,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-3).sp,
        )
        Box(contentAlignment = Alignment.Center) {
            // 1. Shadow, so the glass seems to float above the beach.
            BasicText(
                time,
                modifier = Modifier.offset(y = 6.dp),
                style = base.copy(color = Color.Black.copy(alpha = 0.16f)),
            )
            // 2. The glass body: clear-ish, brighter at the top.
            BasicText(
                time,
                style = base.copy(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.92f),
                        0.5f to Color.White.copy(alpha = 0.55f),
                        1f to Color.White.copy(alpha = 0.75f),
                    )
                ),
            )
            // 3. The bright rim that makes it read as glass.
            BasicText(
                time,
                style = base.copy(
                    brush = Brush.linearGradient(
                        0f to Color.White,
                        0.5f to Color.White.copy(alpha = 0.25f),
                        1f to Color.White.copy(alpha = 0.8f),
                    ),
                    drawStyle = Stroke(width = 2.5f),
                ),
            )
        }

        if (weather != null) WeatherPill(weather)
    }
}

@Composable
private fun WeatherPill(w: Weather) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
            .padding(horizontal = 18.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val soft = TextStyle(color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
        BasicText(w.symbol, style = TextStyle(fontSize = 18.sp))
        BasicText("${w.tempC}°", style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold))
        BasicText(w.words, style = soft)
        BasicText("H ${w.highC}°  L ${w.lowC}°", style = soft)
        BasicText(Glass.weatherPlace, style = soft)
    }
}
