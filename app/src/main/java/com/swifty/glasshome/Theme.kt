package com.swifty.glasshome

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Every colour and size for the look lives here, so restyling means editing just this file.
 */
object Glass {
    // Old glow wallpaper colours (the beach colours live in Beach.kt).
    val sky = Color(0xFF070B1E)
    val glowA = Color(0xFF3446E0)   // blue
    val glowB = Color(0xFF8A3FD1)   // violet
    val glowC = Color(0xFF0FA3B1)   // teal
    val glowD = Color(0xFFE0457B)   // rose

    // Frosted tiles
    val tileFill = Color.White.copy(alpha = 0.10f)
    val tileFillFocused = Color.White.copy(alpha = 0.24f)
    val tileEdge = Color.White.copy(alpha = 0.16f)
    val tileEdgeFocused = Color.White.copy(alpha = 0.85f)

    // Text
    val text = Color.White
    val textSoft = Color.White.copy(alpha = 0.65f)

    // Sizes
    val sidePadding = 64.dp
    val tileWidth = 150.dp
    val tileGap = 22.dp
    val corner = 16.dp
    const val focusScale = 1.13f
    const val appsPerRow = 5
    val clockSize = 124.sp

    // Weather location (Melbourne). Change these to move it somewhere else.
    const val weatherLat = -37.81
    const val weatherLon = 144.96
    const val weatherPlace = "Melbourne"
}
