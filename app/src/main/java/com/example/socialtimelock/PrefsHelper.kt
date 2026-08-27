package com.example.socialtimelock

import android.content.Context
import org.json.JSONArray

/**
 * ذخیره و بازیابی تنظیمات: لیست گروه‌های قفل (هر گروه = چند اپ + بازه‌های زمانی خودش).
 */
object PrefsHelper {
    private const val PREFS_NAME = "social_time_lock_prefs"
    private const val KEY_GROUPS = "lock_groups"

    fun saveGroups(context: Context, groups: List<LockGroup>) {
        val array = JSONArray()
        groups.forEach { array.put(it.toJson()) }
        prefs(context).edit().putString(KEY_GROUPS, array.toString()).apply()
    }

    fun getGroups(context: Context): MutableList<LockGroup> {
        val raw = prefs(context).getString(KEY_GROUPS, null) ?: return mutableListOf()
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<LockGroup>()
            for (i in 0 until array.length()) {
                result.add(LockGroup.fromJson(array.getJSONObject(i)))
            }
            result
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    /** پیدا کردن گروهی که این اپ توش هست (اگر هیچ گروهی نداشتش، یعنی اصلاً محدود نیست) */
    fun findGroupForPackage(context: Context, packageName: String): LockGroup? {
        return getGroups(context).find { packageName in it.packages }
    }

    /** مجموع همه پکیج‌هایی که در هر گروهی قرار دارند (برای جلوگیری از تداخل بین گروه‌ها) */
    fun allAssignedPackages(context: Context, excludingGroupId: String? = null): Set<String> {
        return getGroups(context)
            .filter { it.id != excludingGroupId }
            .flatMap { it.packages }
            .toSet()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
