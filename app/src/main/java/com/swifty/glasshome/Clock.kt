package com.swifty.glasshome

import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val condensed = FontFamily(
    androidx.compose.ui.text.font.Typeface(
        android.graphics.Typeface.create("sans-serif-condensed-light", android.graphics.Typeface.NORMAL)
    )
)

val softShadow = Shadow(Color.Black.copy(alpha = 0.30f), Offset(0f, 2f), 10f)

/**
 * The tall lock-screen clock. Each number is "liquid glass": it's filled with a blurred,
 * brightened copy of the photo behind it (so the beach shows through), with a white sheen
 * on top, a bright glass rim, and a faint shadow so it floats.
 */
@Composable
fun GlassClock(now: Date, modifier: Modifier = Modifier) {
    val time = SimpleDateFormat("h:mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEE d MMMM", Locale.getDefault()).format(now)
    val backdrop = LocalBackdrop.current
    var pos by remember { mutableStateOf(Offset.Zero) }

    val glassFill: Brush = remember(backdrop.glass, backdrop.screen, pos) {
        val bmp = backdrop.glass
        if (bmp == null) SolidColor(Color.White.copy(alpha = 0.75f))
        else {
            val shader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(Matrix().apply {
                    setScale(backdrop.screen.width / bmp.width.toFloat(), backdrop.screen.height / bmp.height.toFloat())
                    postTranslate(-pos.x, -pos.y)
                })
            }
            object : ShaderBrush() {
                override fun createShader(size: Size): Shader = shader
            }
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(
            date,
            style = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, shadow = softShadow),
        )
        val base = TextStyle(fontFamily = condensed, fontSize = Glass.clockSize, letterSpacing = (-2).sp)
        Box(
            Modifier
                .onGloballyPositioned { pos = it.positionInRoot() }
                .graphicsLayer {
                    scaleY = 1.22f
                    transformOrigin = TransformOrigin(0.5f, 0f)
                },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(time, Modifier.offset(y = 3.dp), style = base.copy(color = Color.Black.copy(alpha = 0.10f)))
            BasicText(time, style = base.copy(brush = glassFill))
            BasicText(
                time,
                style = base.copy(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.45f),
                        0.45f to Color.White.copy(alpha = 0.08f),
                        1f to Color.White.copy(alpha = 0.22f),
                    )
                ),
            )
            BasicText(
                time,
                style = base.copy(
                    brush = Brush.linearGradient(
                        0f to Color.White,
                        0.5f to Color.White.copy(alpha = 0.35f),
                        1f to Color.White.copy(alpha = 0.85f),
                    ),
                    drawStyle = Stroke(width = 2f),
                ),
            )
        }
    }
}

/** Small glass pill for the top-right corner, like the Apple TV buttons. */
@Composable
fun WeatherPill(w: Weather, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .frosted(shape, Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.5f), shape)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val soft = TextStyle(color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp, shadow = softShadow)
        BasicText(w.symbol, style = TextStyle(fontSize = 16.sp))
        BasicText("${w.tempC}°", style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, shadow = softShadow))
        BasicText("H ${w.highC}°  L ${w.lowC}°", style = soft)
    }
}
