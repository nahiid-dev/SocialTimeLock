package com.example.socialtimelock

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

/**
 * Helper for reading app usage stats (how many minutes/hours each app has
 * been open today or this week). This relies on a special permission called
 * "Usage Access", separate from the Accessibility Service, which must be
 * enabled manually in Android settings.
 */
object UsageStatsHelper {

    /** Whether the user has granted "Usage Access" permission */
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Usage time for each app from midnight today until now, in milliseconds */
    fun getUsageToday(context: Context): Map<String, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return queryUsage(context, calendar.timeInMillis, System.currentTimeMillis())
    }

    /** Usage time for each app over the past 7 days, in milliseconds */
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

    /** Converts milliseconds into a readable string, e.g. "2h 15m" */
    fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 1000 / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "Less than 1m"
        }
    }
}
