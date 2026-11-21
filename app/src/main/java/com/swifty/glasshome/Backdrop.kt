package com.swifty.glasshome

import android.graphics.Bitmap
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/** What frosted surfaces need to know: the blurred photo, and how big the screen is. */
class Backdrop(val frost: ImageBitmap?, val glass: Bitmap?, val screen: IntSize)

val LocalBackdrop = compositionLocalOf { Backdrop(null, null, IntSize(1920, 1080)) }

/**
 * Real frosted glass: draws the very blurred version of the photo behind this element, lined
 * up exactly with the photo behind it, so it looks like you're seeing the background through
 * misted glass. Then a white [tint] on top.
 */
fun Modifier.frosted(shape: Shape, tint: Color): Modifier = composed {
    val backdrop = LocalBackdrop.current
    var pos by remember { mutableStateOf(Offset.Zero) }
    this
        .onGloballyPositioned { pos = it.positionInRoot() }
        .clip(shape)
        .drawBehind {
            backdrop.frost?.let {
                drawImage(
                    image = it,
                    dstOffset = IntOffset(-pos.x.roundToInt(), -pos.y.roundToInt()),
                    dstSize = backdrop.screen,
                    filterQuality = FilterQuality.Low,
                )
            }
            drawRect(tint)
        }
}
