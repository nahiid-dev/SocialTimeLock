package com.example.socialtimelock

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    var isSelected: Boolean = false,
    // اگه این اپ قبلاً توی یه گروه دیگه باشه، اسم اون گروه اینجا میاد (فقط برای نمایش هشدار)
    var alreadyInGroupName: String? = null
)
