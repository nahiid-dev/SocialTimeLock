package com.example.socialtimelock

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

/**
 * کمک‌کننده برای خواندن آمار استفاده از اپ‌ها (چند دقیقه/ساعت هر اپ امروز یا این هفته باز بوده).
 * این قابلیت به مجوز خاصی به‌اسم "دسترسی به آمار استفاده" نیاز داره که جدا از
 * Accessibility Service است و باید دستی از تنظیمات اندروید فعال بشه.
 */
object UsageStatsHelper {

    /** آیا کاربر مجوز "دسترسی به آمار استفاده" رو داده یا نه */
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** میزان استفاده هر اپ از نیمه‌شب امروز تا الان، بر حسب میلی‌ثانیه */
    fun getUsageToday(context: Context): Map<String, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return queryUsage(context, calendar.timeInMillis, System.currentTimeMillis())
    }

    /** میزان استفاده هر اپ در ۷ روز گذشته، بر حسب میلی‌ثانیه */
    fun getUsageThisWeek(context: Context): Map<String, Long> {
        val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        return queryUsage(context, sevenDaysAgo, System.currentTimeMillis())
    }

    private fun queryUsage(context: Context, start: Long, end: Long): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usm.queryAndAggregateUsageStats(start, end)
        return stats.mapValues { it.value.totalTimeInForeground }
            .filter { it.value > 0 }
    }

    /** تبدیل میلی‌ثانیه به متن فارسیِ خوانا، مثل "۲ ساعت و ۱۵ دقیقه" */
    fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 1000 / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "$hours ساعت و $minutes دقیقه"
            hours > 0 -> "$hours ساعت"
            minutes > 0 -> "$minutes دقیقه"
            else -> "کمتر از ۱ دقیقه"
        }
    }
}
