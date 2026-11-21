package com.swifty.glasshome

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The background photo. It's prepared once when it loads, so it costs nothing while
 * you move around.
 */
object WallpaperRepo {

    /**
     * Your own picture: wallpaper/wallpaper.jpg in the GitHub repo. Glass Home downloads it
     * the next time it opens, so changing it needs no reinstall.
     */
    private const val CUSTOM_URL =
        "https://raw.githubusercontent.com/owenjswift/fire-launcher/main/wallpaper/wallpaper.jpg"

    private fun cacheFile(ctx: Context) = File(ctx.filesDir, "wallpaper.jpg")
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("wallpaper", Context.MODE_PRIVATE)

    fun load(ctx: Context): ImageBitmap? = runCatching {
        val file = cacheFile(ctx)
        val src = (if (file.exists()) decodeFile(file) else null) ?: decodeDefault(ctx) ?: return null
        prepare(src).asImageBitmap()
    }.getOrNull()

    /** Checks GitHub for a new photo. Returns true when the wallpaper changed. */
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

    private fun decodeDefault(ctx: Context): Bitmap? =
        BitmapFactory.decodeResource(ctx.resources, R.drawable.wallpaper)

    private fun decodeFile(file: File): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, opts)
        if (opts.outWidth <= 0) return null
        var s = 1
        while (opts.outWidth / (s * 2) >= 1600) s *= 2
        opts.inSampleSize = s
        opts.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(file.path, opts)
    }

    /** Crop to 16:9 and give it a gentle blur, done once. */
    private fun prepare(src: Bitmap): Bitmap {
        val target = 16f / 9f
        val ratio = src.width.toFloat() / src.height
        val (cw, ch) = if (ratio > target) (src.height * target).toInt() to src.height
        else src.width to (src.width / target).toInt()
        val cropped = Bitmap.createBitmap(src, (src.width - cw) / 2, (src.height - ch) / 2, cw, ch)
        // Keep it sharp: only shrink photos that are bigger than the TV needs.
        return if (cw > 1920) Bitmap.createScaledBitmap(cropped, 1920, 1080, true) else cropped
    }

    private fun blur(bmp: Bitmap, radius: Int): Bitmap {
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val tmp = IntArray(w * h)
        repeat(2) {
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
