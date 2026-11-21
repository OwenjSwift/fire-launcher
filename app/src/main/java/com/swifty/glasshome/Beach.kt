package com.swifty.glasshome

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

/** Colours for one time of day. */
private class BeachPalette(
    val skyTop: Color, val skyHorizon: Color,
    val sun: Color, val sunGlow: Color, val sunHigh: Float,
    val seaFar: Color, val seaNear: Color,
    val sandWet: Color, val sand: Color,
    val foam: Color,
)

private val dawn = BeachPalette(
    Color(0xFF5B79C9), Color(0xFFFFCFA3), Color(0xFFFFE7C2), Color(0xFFFFB27A), 0.10f,
    Color(0xFF6E86C4), Color(0xFF4FA6C0), Color(0xFFC7A98A), Color(0xFFEBCFA8), Color.White,
)
private val day = BeachPalette(
    Color(0xFF2F7FD8), Color(0xFFBFE8F6), Color(0xFFFFFBEA), Color(0xFFFFF3B0), 0.30f,
    Color(0xFF1C7DB8), Color(0xFF3CC6CF), Color(0xFFD9BC8C), Color(0xFFF4DDB0), Color.White,
)
private val sunset = BeachPalette(
    Color(0xFF2C2F78), Color(0xFFFF9A62), Color(0xFFFFD08A), Color(0xFFFF7A4A), 0.06f,
    Color(0xFF5A4C8C), Color(0xFF2F6E9E), Color(0xFFB08670), Color(0xFFDDB38E), Color(0xFFFFE6D0),
)
private val night = BeachPalette(
    Color(0xFF040824), Color(0xFF1E2D63), Color(0xFFEFF3FF), Color(0xFF8FA3E8), 0.28f,
    Color(0xFF0E1D45), Color(0xFF163A66), Color(0xFF3B3E5C), Color(0xFF55536E), Color(0xFFBFD0FF),
)

private fun paletteFor(hour: Int): BeachPalette = when (hour) {
    in 5..7 -> dawn
    in 8..16 -> day
    in 17..19 -> sunset
    else -> night
}

/**
 * A beach drawn in code: sky, sun (or moon at night), sea with gently rolling swell, and waves
 * washing up the sand. The colours follow the time of day.
 */
@Composable
fun BeachWallpaper(hour: Int) {
    val p = remember(hour) { paletteFor(hour) }
    val anim = rememberInfiniteTransition(label = "beach")
    val swell by anim.animateFloat(
        0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(9_000, easing = LinearEasing), RepeatMode.Restart), label = "swell",
    )
    val wash by anim.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(6_500, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wash",
    )

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val horizon = h * 0.60f
        val shore = h * (0.80f + 0.03f * wash)

        // Sky
        drawRect(Brush.verticalGradient(listOf(p.skyTop, p.skyHorizon), endY = horizon))

        // Sun or moon, with a big soft glow
        val sunC = Offset(w * 0.74f, horizon - h * p.sunHigh)
        drawCircle(Brush.radialGradient(listOf(p.sunGlow.copy(alpha = 0.55f), Color.Transparent), sunC, h * 0.45f), h * 0.45f, sunC)
        drawCircle(p.sun, h * 0.055f, sunC)

        // Sea
        drawRect(
            Brush.verticalGradient(listOf(p.seaFar, p.seaNear), startY = horizon, endY = h),
            topLeft = Offset(0f, horizon), size = androidx.compose.ui.geometry.Size(w, h - horizon),
        )
        // Light shimmering on the water under the sun
        drawRect(
            Brush.radialGradient(listOf(p.sunGlow.copy(alpha = 0.35f), Color.Transparent), Offset(sunC.x, horizon + h * 0.04f), w * 0.22f),
            topLeft = Offset(0f, horizon), size = androidx.compose.ui.geometry.Size(w, h * 0.2f),
        )
        // Rolling swell lines, fainter further away
        for (i in 0 until 5) {
            val y = horizon + (shore - horizon) * (0.12f + i * 0.19f)
            swellLine(y, w, amp = 1.5f + i * 1.6f, wavelength = w / (6f - i * 0.6f), phase = swell + i * 1.3f,
                color = p.foam.copy(alpha = 0.10f + i * 0.05f), stroke = 1.5f + i * 0.7f)
        }

        // Sand, with the wet edge and a line of foam where the waves wash in
        val sandPath = shorePath(shore, w, h, swell, h * 0.012f)
        drawPath(sandPath, Brush.verticalGradient(listOf(p.sandWet, p.sand), startY = shore, endY = h))
        drawPath(shorePath(shore, w, h, swell, h * 0.012f, onlyEdge = true), p.foam.copy(alpha = 0.75f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f))

        // Darken the lower part a little so the app tiles and labels stay easy to read
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f)), startY = h * 0.35f, endY = h))
        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent), endY = h * 0.3f))
    }
}

private fun DrawScope.swellLine(y: Float, w: Float, amp: Float, wavelength: Float, phase: Float, color: Color, stroke: Float) {
    val path = Path()
    var x = 0f
    path.moveTo(0f, y + amp * sin(phase))
    while (x <= w) {
        x += 16f
        path.lineTo(x, y + amp * sin(x / wavelength * 2f * PI.toFloat() + phase))
    }
    drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
}

private fun shorePath(shore: Float, w: Float, h: Float, phase: Float, amp: Float, onlyEdge: Boolean = false): Path {
    val path = Path()
    fun yAt(x: Float) = shore + amp * sin(x / w * 5f * PI.toFloat() + phase * 0.5f) + amp * 0.5f * sin(x / w * 13f * PI.toFloat() - phase)
    path.moveTo(0f, yAt(0f))
    var x = 0f
    while (x <= w) {
        x += 12f
        path.lineTo(x, yAt(x))
    }
    if (!onlyEdge) {
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
    }
    return path
}
