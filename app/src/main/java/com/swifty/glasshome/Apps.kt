package com.swifty.glasshome

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/** One app on the home screen. [banner] is the wide TV artwork, if the app has one. */
data class AppEntry(
    val packageName: String,
    val label: String,
    val banner: ImageBitmap?,
    val icon: ImageBitmap?,
)

object AppRepository {

    /** Packages we never show: ourselves and Amazon's own home screen. */
    private val hidden = setOf(
        "com.amazon.tv.launcher",
    )

    /** Loads every launchable app, sorted A to Z. Call this off the main thread. */
    fun loadApps(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val seen = LinkedHashMap<String, AppEntry>()
        val categories = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
        for (category in categories) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (info in pm.queryIntentActivities(query, 0)) {
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName || pkg in hidden || pkg in seen) continue
                val banner = runCatching {
                    info.activityInfo.loadBanner(pm) ?: info.activityInfo.applicationInfo.loadBanner(pm)
                }.getOrNull()
                val icon = runCatching { info.loadIcon(pm) }.getOrNull()
                seen[pkg] = AppEntry(
                    packageName = pkg,
                    label = info.loadLabel(pm).toString(),
                    banner = banner?.toImage(320, 180),
                    icon = icon?.toImage(160, 160),
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
}
