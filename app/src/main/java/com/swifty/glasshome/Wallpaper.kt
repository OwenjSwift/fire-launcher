package com.swifty.glasshome

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The background photo in three versions:
 * [soft]  slightly blurred, for the full-screen background
 * [frost] very blurred and lightened, for the frosted-glass shelf and panels
 * [glass] blurred and brightened, used to fill the clock's glass numbers
 */
class WallpaperSet(val soft: ImageBitmap, val frost: ImageBitmap, val glass: Bitmap)

object WallpaperRepo {

    /**
     * Your own picture: put any landscape photo at wallpaper/wallpaper.jpg in the GitHub repo
     * and Glass Home picks it up the next time it opens. No reinstall needed.
     * Delete that file to go back to the built-in beach.
     */
    private const val CUSTOM_URL =
        "https://raw.githubusercontent.com/owenjswift/fire-launcher/main/wallpaper/wallpaper.jpg"

    private fun cacheFile(ctx: Context) = File(ctx.filesDir, "wallpaper.jpg")
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("wallpaper", Context.MODE_PRIVATE)

    /** Builds the wallpaper from your custom photo if downloaded, else the built-in beach. */
    fun load(ctx: Context): WallpaperSet? = runCatching {
        val file = cacheFile(ctx)
        val src = (if (file.exists()) decodeFile(file) else null) ?: decodeDefault(ctx) ?: return null
        build(src)
    }.getOrNull()

    /** Checks GitHub for a new custom photo. Returns true when the wallpaper changed. */
    fun refresh(ctx: Context): Boolean = runCatching {
        val p = prefs(ctx)
        val file = cacheFile(ctx)
        val conn = (URL(CUSTOM_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 20000
            p.getString("etag", null)?.let { if (file.exists()) setRequestProperty("If-None-Match", it) }
        }
        try {
            when (conn.responseCode) {
                200 -> {
                    val tmp = File(ctx.filesDir, "wallpaper.tmp")
                    conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
                    if (decodeFile(tmp) == null) { tmp.delete(); return false }
                    tmp.renameTo(file)
                    p.edit().putString("etag", conn.getHeaderField("ETag")).apply()
                    true
                }
                404 -> {
                    // No custom photo in the repo: go back to the built-in beach.
                    val had = file.exists()
                    file.delete()
                    p.edit().remove("etag").apply()
                    had
                }
                else -> false
            }
        } finally {
            conn.disconnect()
        }
    }.getOrDefault(false)

    private fun decodeDefault(ctx: Context): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(ctx.resources, R.drawable.wallpaper, opts)
        opts.inSampleSize = sampleSize(opts.outWidth)
        opts.inJustDecodeBounds = false
        return BitmapFactory.decodeResource(ctx.resources, R.drawable.wallpaper, opts)
    }

    private fun decodeFile(file: File): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, opts)
        if (opts.outWidth <= 0) return null
        opts.inSampleSize = sampleSize(opts.outWidth)
        opts.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(file.path, opts)
    }

    private fun sampleSize(width: Int): Int {
        var s = 1
        while (width / (s * 2) >= 1600) s *= 2
        return s
    }

    private fun build(src: Bitmap): WallpaperSet {
        val full = centerCrop(src, 1600, 900)
        val soft = blur(Bitmap.createScaledBitmap(full, 800, 450, true), radius = 2)
        val frost = lighten(blur(Bitmap.createScaledBitmap(full, 200, 113, true), radius = 5), 0.30f)
        val glass = lighten(blur(Bitmap.createScaledBitmap(full, 320, 180, true), radius = 4), 0.45f)
        return WallpaperSet(soft.asImageBitmap(), frost.asImageBitmap(), glass)
    }

    /** Crops to the TV's 16:9 shape from the middle, then resizes. */
    private fun centerCrop(src: Bitmap, w: Int, h: Int): Bitmap {
        val target = w.toFloat() / h
        val srcRatio = src.width.toFloat() / src.height
        val (cw, ch) = if (srcRatio > target) (src.height * target).toInt() to src.height
        else src.width to (src.width / target).toInt()
        val x = (src.width - cw) / 2
        val y = (src.height - ch) / 2
        return Bitmap.createScaledBitmap(Bitmap.createBitmap(src, x, y, cw, ch), w, h, true)
    }

    /** Washes the picture towards white, like light coming through frosted glass. */
    private fun lighten(bmp: Bitmap, amount: Float): Bitmap {
        val out = bmp.copy(Bitmap.Config.ARGB_8888, true)
        Canvas(out).drawColor(android.graphics.Color.argb((amount * 255).toInt(), 255, 255, 255))
        return out
    }

    /** A fast blur: three passes of a box blur look almost the same as a proper Gaussian blur. */
    private fun blur(bmp: Bitmap, radius: Int): Bitmap {
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val tmp = IntArray(w * h)
        repeat(3) {
            boxPass(px, tmp, w, h, radius, horizontal = true)
            boxPass(tmp, px, w, h, radius, horizontal = false)
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun boxPass(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        val div = 2 * r + 1
        for (line in 0 until lines) {
            fun idx(i: Int): Int {
                val c = i.coerceIn(0, len - 1)
                return if (horizontal) line * w + c else c * w + line
            }
            var sr = 0; var sg = 0; var sb = 0
            for (i in -r..r) {
                val c = src[idx(i)]
                sr += (c shr 16) and 0xFF; sg += (c shr 8) and 0xFF; sb += c and 0xFF
            }
            for (i in 0 until len) {
                dst[idx(i)] = (0xFF shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
                val add = src[idx(i + r + 1)]
                val sub = src[idx(i - r)]
                sr += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
                sg += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
                sb += (add and 0xFF) - (sub and 0xFF)
            }
        }
    }
}
