package com.swifty.glasshome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Tries to open Glass Home as soon as the Fire TV finishes starting up. Newer Fire OS versions
 * may block this, in which case [HomeWatcherService] catches Amazon's home screen instead.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Prefs(context).clearPause()
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(open) }
    }
}
