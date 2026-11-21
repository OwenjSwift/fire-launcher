package com.swifty.glasshome

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * One app on the home screen. [banner] is the wide TV artwork, if the app has one.
 * Apps without artwork get a card in [tint], a colour taken from their icon.
 */
data class AppEntry(
    val packageName: String,
    val label: String,
    val banner: ImageBitmap?,
    val icon: ImageBitmap?,
    val tint: Color,
)

object AppRepository {

    /** Packages we never show. */
    private val hiddenPackages = setOf(
        "com.amazon.tv.launcher",
        "com.amazon.luna",
        "com.amazon.tahoe",            // Amazon Kids
        "com.amazon.minitv",
        "com.amazon.minitv.android",
    )

    /** App names we never show (exact names, ignoring capitals). */
    private val hiddenNames = setOf(
        "news", "amazon news",
        "minitv", "mini tv", "amazon minitv", "amazon mini tv",
        "luna", "amazon luna",
        "amazon kids", "amazon kids+", "amazon freetime", "freetime",
    )

    private fun isHidden(pkg: String, label: String): Boolean =
        pkg in hiddenPackages || label.trim().lowercase() in hiddenNames

    /** Loads every launchable app, sorted A to Z. Call this off the main thread. */
    fun loadApps(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val seen = LinkedHashMap<String, AppEntry>()
        val categories = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
        for (category in categories) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (info in pm.queryIntentActivities(query, 0)) {
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName || pkg in seen) continue
                val label = info.loadLabel(pm).toString()
                if (isHidden(pkg, label)) continue
                val banner = runCatching {
                    info.activityInfo.loadBanner(pm) ?: info.activityInfo.applicationInfo.loadBanner(pm)
                }.getOrNull()
                val iconBmp = runCatching { info.loadIcon(pm)?.toBitmap(160, 160) }.getOrNull()
                seen[pkg] = AppEntry(
                    packageName = pkg,
                    label = label,
                    banner = banner?.toImage(320, 180),
                    icon = iconBmp?.asImageBitmap(),
                    tint = iconBmp?.let { cardColour(it) } ?: Color(0xFF3A3A3C),
                )
            }
        }
        return seen.values.sortedBy { it.label.lowercase() }
    }

    fun launch(context: Context, packageName: String) {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)
            ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun Drawable.toImage(w: Int, h: Int): ImageBitmap? =
        runCatching { toBitmap(w, h).asImageBitmap() }.getOrNull()

    /** The icon's main colour, deepened a little so a white icon and text sit nicely on it. */
    private fun cardColour(icon: Bitmap): Color {
        var r = 0L; var g = 0L; var b = 0L; var n = 0L
        val small = Bitmap.createScaledBitmap(icon, 16, 16, true)
        for (x in 0 until 16) for (y in 0 until 16) {
            val c = small.getPixel(x, y)
            val a = (c ushr 24) and 0xFF
            if (a < 128) continue
            val cr = (c shr 16) and 0xFF; val cg = (c shr 8) and 0xFF; val cb = c and 0xFF
            // Skip near-white and near-black pixels so we find the actual brand colour.
            val max = maxOf(cr, cg, cb); val min = minOf(cr, cg, cb)
            if (max > 235 && min > 220) continue
            if (max < 25) continue
            r += cr; g += cg; b += cb; n++
        }
        if (n == 0L) return Color(0xFF3A3A3C)
        val k = 0.78f
        return Color((r / n * k).toInt(), (g / n * k).toInt(), (b / n * k).toInt())
    }
}
