package com.swifty.glasshome

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Every colour and size for the look lives here, so restyling means editing just this file. */
object Glass {
    // Frosted glass (white tint over the blurred photo)
    val shelfTint = Color.White.copy(alpha = 0.22f)
    val shelfEdge = Color.White.copy(alpha = 0.45f)
    val panelTint = Color.White.copy(alpha = 0.28f)
    val pillTint = Color.White.copy(alpha = 0.20f)

    // App cards
    val cardPlain = Color.White.copy(alpha = 0.92f)   // cards for apps with no TV artwork
    val cardText = Color(0xFF1C1C1E)
    val focusRing = Color.White

    // Text
    val text = Color.White
    val textSoft = Color.White.copy(alpha = 0.85f)

    // Sizes
    val sidePadding = 56.dp
    val shelfCardWidth = 176.dp     // the big cards on the bottom shelf
    val gridCardWidth = 152.dp      // cards in "All apps"
    val cardGap = 20.dp
    val cardCorner = 14.dp
    val shelfCorner = 28.dp
    const val focusScale = 1.09f
    const val appsPerRow = 5
    val clockSize = 150.sp

    // Weather location (Melbourne). Change these to move it somewhere else.
    const val weatherLat = -37.81
    const val weatherLon = 144.96
    const val weatherPlace = "Melbourne"
}
