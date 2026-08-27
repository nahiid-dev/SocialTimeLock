package com.example.socialtimelock

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * A "rule": a set of apps that all follow the same list of allowed time windows.
 * A rule can contain just one app (an individual restriction) or several apps
 * together (a group restriction).
 */
data class LockGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val packages: MutableSet<String> = mutableSetOf(),
    val ranges: MutableList<TimeRange> = mutableListOf()
) {

    fun isAllowedNow(nowMinute: Int): Boolean {
        if (ranges.isEmpty()) return false // no ranges means always locked
        return ranges.any { it.contains(nowMinute) }
    }

    fun rangesDisplayString(): String {
        if (ranges.isEmpty()) return "Always locked"
        return ranges.sortedBy { it.startMinute }.joinToString(", ") { it.toDisplayString() }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("name", name)
        obj.put("packages", JSONArray(packages.toList()))
        obj.put("ranges", JSONArray(ranges.map { it.toStorageString() }))
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): LockGroup {
            val id = obj.optString("id", UUID.randomUUID().toString())
            val name = obj.optString("name", "")
            val packagesArray = obj.optJSONArray("packages") ?: JSONArray()
            val packages = mutableSetOf<String>()
            for (i in 0 until packagesArray.length()) {
                packages.add(packagesArray.getString(i))
            }
            val rangesArray = obj.optJSONArray("ranges") ?: JSONArray()
            val ranges = mutableListOf<TimeRange>()
            for (i in 0 until rangesArray.length()) {
                TimeRange.fromStorageString(rangesArray.getString(i))?.let { ranges.add(it) }
            }
            return LockGroup(id, name, packages, ranges)
        }
    }
}
