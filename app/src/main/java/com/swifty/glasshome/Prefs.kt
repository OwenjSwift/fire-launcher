package com.swifty.glasshome

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.provider.Settings

/** Small saved settings: your favourite apps, and the auto-open pause. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("glass_home", Context.MODE_PRIVATE)

    /** Apps we put in Favourites the first time, if you have them installed. */
    private val starterFavourites = listOf(
        "com.netflix.ninja",
        "com.amazon.firetv.youtube",
        "com.google.android.youtube.tv",
        "com.amazon.avod.thirdpartyclient",
        "com.disney.disneyplus",
        "com.spotify.tv.android",
        "com.plexapp.android",
    )

    fun favourites(installed: List<AppEntry>): List<String> {
        val installedSet = installed.map { it.packageName }.toSet()
        val saved = sp.getString(KEY_FAVS, null)
        val list = saved?.split(",")?.filter { it.isNotBlank() }
            ?: starterFavourites
        return list.filter { it in installedSet }
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
        private const val KEY_FAVS = "favourites"
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
