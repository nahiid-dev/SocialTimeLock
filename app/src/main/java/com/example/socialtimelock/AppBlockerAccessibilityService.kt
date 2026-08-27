package com.example.socialtimelock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import java.util.Calendar

/**
 * This service is called every time a new app comes to the foreground.
 * If that app belongs to one of the "rules", the current time is outside its
 * allowed window, and it doesn't have temporary access, the user is sent to
 * the "locked" screen. Apps that aren't part of any rule are left untouched.
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
    }

    private var lastBlockedPackage: String? = null
    private var lastBlockedTime: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Don't block our own app
        if (packageName == this.packageName) return

        val group = PrefsHelper.findGroupForPackage(this, packageName) ?: return

        // If the user already tapped "give me a few more minutes" and it's still valid, do nothing
        if (PrefsHelper.hasTemporaryAccess(this, packageName)) return

        val nowMinute = currentMinuteOfDay()
        val isAllowed = group.isAllowedNow(nowMinute)

        if (!isAllowed) {
            // Prevent the lock screen from reopening repeatedly for the same app within a split second
            val now = System.currentTimeMillis()
            if (packageName == lastBlockedPackage && now - lastBlockedTime < 1500) return
            lastBlockedPackage = packageName
            lastBlockedTime = now

            val intent = Intent(this, BlockedScreenActivity::class.java)
            intent.putExtra(EXTRA_BLOCKED_PACKAGE, packageName)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        // Nothing needed here
    }

    private fun currentMinuteOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }
}
