package com.swifty.glasshome

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Every colour and size for the look lives here, so restyling means editing just this file. */
object Glass {
    // Glass surfaces: a light white wash over the (already blurred) photo
    val glass = Color.White.copy(alpha = 0.16f)
    val glassEdge = Color.White.copy(alpha = 0.30f)

    // Text
    val text = Color.White
    val textSoft = Color.White.copy(alpha = 0.8f)

    // Sizes
    val sidePadding = 56.dp
    val shelfCardWidth = 136.dp     // the big cards on the shelf
    val gridCardWidth = 116.dp      // cards in "All apps"
    val cardGap = 18.dp
    val cardCorner = 12.dp
    val shelfCorner = 24.dp
    const val focusScale = 1.10f
    const val appsPerRow = 6
    val clockSize = 128.sp

    // Weather location (Melbourne). Change these to move it somewhere else.
    const val weatherLat = -37.81
    const val weatherLon = 144.96
    const val weatherPlace = "Melbourne"
}
