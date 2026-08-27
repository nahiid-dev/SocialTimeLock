package com.example.socialtimelock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UsageStatsAdapter(
    private val items: List<AppUsageInfo>
) : RecyclerView.Adapter<UsageStatsAdapter.UsageViewHolder>() {

    inner class UsageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.usageAppIcon)
        val label: TextView = view.findViewById(R.id.usageAppLabel)
        val duration: TextView = view.findViewById(R.id.usageDuration)
        val bar: ProgressBar = view.findViewById(R.id.usageBar)
        val ruleBadge: TextView = view.findViewById(R.id.usageRuleBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_usage_app, parent, false)
        return UsageViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsageViewHolder, position: Int) {
        val item = items[position]
        val maxUsage = items.maxOfOrNull { it.usageMillis } ?: 1L

        holder.icon.setImageDrawable(item.icon)
        holder.label.text = item.label
        holder.duration.text = UsageStatsHelper.formatDuration(item.usageMillis)
        holder.bar.max = 100
        holder.bar.progress = if (maxUsage > 0) ((item.usageMillis * 100) / maxUsage).toInt() else 0
        holder.ruleBadge.visibility = if (item.hasActiveRule) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int = items.size
}
