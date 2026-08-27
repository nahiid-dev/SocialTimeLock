package com.example.socialtimelock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import java.util.Calendar

/**
 * این سرویس هر بار که یک اپلیکیشن جدید در جلوی صفحه (foreground) قرار می‌گیره
 * فراخوانی می‌شه. اگر اون اپ توی یکی از "قانون‌ها" باشه و زمان فعلی خارج از
 * بازه‌های مجاز اون قانون باشه، کاربر رو به صفحه "قفله" هدایت می‌کنه.
 * اپ‌هایی که توی هیچ قانونی نیستن، اصلاً دست‌نخورده می‌مونن.
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    private var lastBlockedPackage: String? = null
    private var lastBlockedTime: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // از مسدود کردن اپ خودمون جلوگیری کن
        if (packageName == this.packageName) return

        val group = PrefsHelper.findGroupForPackage(this, packageName) ?: return

        val nowMinute = currentMinuteOfDay()
        val isAllowed = group.isAllowedNow(nowMinute)

        if (!isAllowed) {
            // جلوگیری از باز کردن پشت‌سرهم صفحه قفل برای همون اپ در کسری از ثانیه
            val now = System.currentTimeMillis()
            if (packageName == lastBlockedPackage && now - lastBlockedTime < 1500) return
            lastBlockedPackage = packageName
            lastBlockedTime = now

            val intent = Intent(this, BlockedScreenActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        // چیزی لازم نیست اینجا انجام بشه
    }

    private fun currentMinuteOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }
}
