package com.swifty.glasshome

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.provider.Settings

/** Small saved settings: your favourite apps, and the auto-open pause. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("glass_home", Context.MODE_PRIVATE)

    /** The shelf to start with, in this order: matched by app name so it works whatever the package is called. */
    private val starterNames = listOf(
        listOf("netflix"),
        listOf("youtube"),
        listOf("disney+", "disney plus", "disney"),
        listOf("kayo", "kayo sports"),
    )

    fun favourites(installed: List<AppEntry>): List<String> {
        val installedSet = installed.map { it.packageName }.toSet()
        val saved = sp.getString(KEY_FAVS, null)
        if (saved != null) return saved.split(",").filter { it.isNotBlank() && it in installedSet }
        return starterNames.mapNotNull { names ->
            installed.firstOrNull { app ->
                val l = app.label.trim().lowercase()
                names.any { l == it || l.startsWith("$it ") || l.startsWith(it) }
            }?.packageName
        }.distinct()
    }

    fun toggleFavourite(current: List<String>, pkg: String): List<String> {
        val next = if (pkg in current) current - pkg else current + pkg
        sp.edit().putString(KEY_FAVS, next.joinToString(",")).apply()
        return next
    }

    /** When you pick "Amazon Home", auto-open waits this long so you can use it. */
    fun pauseAutoOpen(minutes: Int) {
        sp.edit().putLong(KEY_PAUSE_UNTIL, SystemClock.elapsedRealtime() + minutes * 60_000L).apply()
    }

    fun clearPause() = sp.edit().remove(KEY_PAUSE_UNTIL).apply()

    fun isPaused(): Boolean = SystemClock.elapsedRealtime() < sp.getLong(KEY_PAUSE_UNTIL, 0L)

    companion object {
        private const val KEY_FAVS = "favourites_v2"  // v2: reset to Netflix, YouTube, Disney+, Kayo
        private const val KEY_PAUSE_UNTIL = "pause_until"

        /** True when the auto-open helper is switched on in Fire TV settings. */
        fun isAutoOpenEnabled(context: Context): Boolean {
            val me = ComponentName(context, HomeWatcherService::class.java).flattenToString()
            val enabled = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.split(':').any { it.equals(me, ignoreCase = true) }
        }
    }
}
