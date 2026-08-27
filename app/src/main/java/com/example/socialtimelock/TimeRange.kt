package com.example.socialtimelock

/**
 * An allowed time window, in minutes since midnight (0..1439).
 */
data class TimeRange(val startMinute: Int, val endMinute: Int) {

    fun contains(nowMinute: Int): Boolean {
        return if (startMinute <= endMinute) {
            nowMinute in startMinute..endMinute
        } else {
            // Range crosses midnight, e.g. 23:00 to 02:00
            nowMinute >= startMinute || nowMinute <= endMinute
        }
    }

    fun toStorageString(): String = "$startMinute-$endMinute"

    fun toDisplayString(): String =
        "${formatMinute(startMinute)} to ${formatMinute(endMinute)}"

    companion object {
        fun fromStorageString(value: String): TimeRange? {
            val parts = value.split("-")
            if (parts.size != 2) return null
            val start = parts[0].toIntOrNull() ?: return null
            val end = parts[1].toIntOrNull() ?: return null
            return TimeRange(start, end)
        }

        fun formatMinute(minute: Int): String {
            val h = minute / 60
            val m = minute % 60
            return "%02d:%02d".format(h, m)
        }
    }
}
