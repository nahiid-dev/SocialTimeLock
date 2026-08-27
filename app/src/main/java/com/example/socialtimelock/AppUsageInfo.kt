package com.example.socialtimelock

import android.graphics.drawable.Drawable

data class AppUsageInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val usageMillis: Long,
    val hasActiveRule: Boolean
)
