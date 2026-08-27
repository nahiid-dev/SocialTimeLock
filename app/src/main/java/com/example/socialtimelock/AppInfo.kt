package com.example.socialtimelock

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    var isSelected: Boolean = false,
    // If this app already belongs to another group, that group's name goes here (for warning display only)
    var alreadyInGroupName: String? = null
)
