package com.swifty.glasshome

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

/**
 * Amazon doesn't let other apps replace the home screen any more, so this does the next best
 * thing: the moment Amazon's home screen appears (after boot, or when you press Home), it
 * opens Glass Home on top. You may see Amazon's screen flash for a split second.
 *
 * It's set (in res/xml/home_watcher.xml) to only hear about Amazon's launcher, nothing else.
 */
class HomeWatcherService : AccessibilityService() {

    private var lastJump = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (event.packageName?.toString() != AMAZON_HOME) return

        // Little pop-ups that belong to Amazon's launcher are not the home screen itself.
        val cls = event.className?.toString().orEmpty()
        if (cls.contains("Dialog", ignoreCase = true) || cls.contains("Toast", ignoreCase = true)) return

        if (Prefs(this).isPaused()) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastJump < 700) return
        lastJump = now

        val open = Intent(this, MainActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_NO_ANIMATION
        )
        runCatching { startActivity(open) }
    }

    override fun onInterrupt() = Unit

    companion object {
        const val AMAZON_HOME = "com.amazon.tv.launcher"
    }
}
