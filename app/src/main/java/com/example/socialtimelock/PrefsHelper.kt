package com.example.socialtimelock

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Saves and loads settings: the list of lock groups (each group = some apps +
 * its own time windows) and also temporary access grants (when the user taps
 * "give me a few more minutes").
 */
object PrefsHelper {
    private const val PREFS_NAME = "social_time_lock_prefs"
    private const val KEY_GROUPS = "lock_groups"
    private const val KEY_TEMP_ACCESS = "temp_access"

    // ---------- Rules (groups) ----------

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

    /** Finds the group this app belongs to (if none, the app isn't restricted at all) */
    fun findGroupForPackage(context: Context, packageName: String): LockGroup? {
        return getGroups(context).find { packageName in it.packages }
    }

    /** All packages assigned to any group (used to prevent overlap between groups) */
    fun allAssignedPackages(context: Context, excludingGroupId: String? = null): Set<String> {
        return getGroups(context)
            .filter { it.id != excludingGroupId }
            .flatMap { it.packages }
            .toSet()
    }

    // ---------- Backup (Export / Import) ----------

    /** Converts all rules into a JSON string that can be saved to a file */
    fun exportGroupsAsJson(context: Context): String {
        val array = JSONArray()
        getGroups(context).forEach { array.put(it.toJson()) }
        return array.toString(2)
    }

    /**
     * Imports rules from a JSON string.
     * If replace=true, all current rules are cleared and replaced.
     * If replace=false, imported rules are merged with the current ones
     * (if an app is in both, the imported rule wins).
     * Returns: the number of rules successfully imported.
     */
    fun importGroupsFromJson(context: Context, json: String, replace: Boolean): Int {
        val array = JSONArray(json)
        val imported = mutableListOf<LockGroup>()
        for (i in 0 until array.length()) {
            imported.add(LockGroup.fromJson(array.getJSONObject(i)))
        }

        val finalGroups: List<LockGroup> = if (replace) {
            imported
        } else {
            val existing = getGroups(context)
            val importedPackages = imported.flatMap { it.packages }.toSet()
            existing.forEach { it.packages.removeAll(importedPackages) }
            existing.removeAll { it.packages.isEmpty() }
            existing + imported
        }

        saveGroups(context, finalGroups)
        return imported.size
    }

    // ---------- Temporary access ("give me a few more minutes" button) ----------

    /** Grants temporary access to this app, valid for durationMillis from now, even outside the allowed window */
    fun grantTemporaryAccess(context: Context, packageName: String, durationMillis: Long) {
        val map = getTemporaryAccessMap(context).toMutableMap()
        map[packageName] = System.currentTimeMillis() + durationMillis
        saveTemporaryAccessMap(context, map)
    }

    /** Whether this app currently has valid temporary access */
    fun hasTemporaryAccess(context: Context, packageName: String): Boolean {
        val expiry = getTemporaryAccessMap(context)[packageName] ?: return false
        return System.currentTimeMillis() < expiry
    }

    private fun getTemporaryAccessMap(context: Context): Map<String, Long> {
        val raw = prefs(context).getString(KEY_TEMP_ACCESS, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, Long>()
            obj.keys().forEach { key -> map[key] = obj.getLong(key) }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveTemporaryAccessMap(context: Context, map: Map<String, Long>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        prefs(context).edit().putString(KEY_TEMP_ACCESS, obj.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
