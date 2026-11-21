package com.swifty.glasshome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val clockFont = FontFamily(
    androidx.compose.ui.text.font.Typeface(
        android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
    )
)

val softShadow = Shadow(Color.Black.copy(alpha = 0.28f), Offset(0f, 2f), 12f)

/**
 * The big lock-screen clock: tall, light numbers in translucent white, brightest at the top
 * like glass catching the light, with a fine bright edge and a soft shadow. Only two layers of
 * text, drawn once a minute, so it costs nothing.
 */
@Composable
fun GlassClock(now: Date, modifier: Modifier = Modifier) {
    val time = SimpleDateFormat("h:mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEE d MMMM", Locale.getDefault()).format(now)

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(
            date,
            style = TextStyle(color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, shadow = softShadow),
        )
        val base = TextStyle(fontFamily = clockFont, fontSize = Glass.clockSize, letterSpacing = (-4).sp)
        Box(
            Modifier.graphicsLayer {
                scaleY = 1.18f
                transformOrigin = TransformOrigin(0.5f, 0f)
            },
        ) {
            // Glass body
            BasicText(
                time,
                style = base.copy(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.95f),
                        1f to Color.White.copy(alpha = 0.62f),
                    ),
                    shadow = Shadow(Color.Black.copy(alpha = 0.18f), Offset(0f, 6f), 28f),
                ),
            )
            // Fine bright edge
            BasicText(
                time,
                style = base.copy(
                    brush = Brush.verticalGradient(
                        0f to Color.White,
                        1f to Color.White.copy(alpha = 0.35f),
                    ),
                    drawStyle = Stroke(width = 1.5f),
                ),
            )
        }
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
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText(w.symbol, style = TextStyle(fontSize = 15.sp))
        BasicText("${w.tempC}°", style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
        BasicText("H ${w.highC}°  L ${w.lowC}°", style = TextStyle(color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp))
    }
}
