package com.example.socialtimelock

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialtimelock.databinding.ActivityUsageStatsBinding

/**
 * صفحه آمار استفاده: نشون می‌ده هر اپ امروز یا این هفته چقدر استفاده شده.
 * اپ‌هایی که توی یک قانون فعال هستن، یه نشان کوچیک قفل کنارشون دارن.
 */
class UsageStatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsageStatsBinding
    private var showingWeek = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsageStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGrantAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        binding.toggleToday.setOnClickListener {
            showingWeek = false
            updateToggleStyle()
            loadUsageData()
        }
        binding.toggleWeek.setOnClickListener {
            showingWeek = true
            updateToggleStyle()
            loadUsageData()
        }

        updateToggleStyle()
    }

    override fun onResume() {
        super.onResume()
        val hasAccess = UsageStatsHelper.hasUsageAccess(this)
        binding.permissionCard.visibility = if (hasAccess) android.view.View.GONE else android.view.View.VISIBLE
        binding.contentGroup.visibility = if (hasAccess) android.view.View.VISIBLE else android.view.View.GONE

        if (hasAccess) {
            loadUsageData()
        }
    }

    private fun updateToggleStyle() {
        binding.toggleToday.isChecked = !showingWeek
        binding.toggleWeek.isChecked = showingWeek
    }

    private fun loadUsageData() {
        val pm = packageManager
        val usageMap = if (showingWeek)
            UsageStatsHelper.getUsageThisWeek(this) else UsageStatsHelper.getUsageToday(this)

        val lockedPackages = PrefsHelper.getGroups(this).flatMap { it.packages }.toSet()

        val items = usageMap.entries
            .mapNotNull { (pkg, millis) ->
                try {
                    val appInfo: ApplicationInfo = pm.getApplicationInfo(pkg, 0)
                    // فقط اپ‌هایی که آیکون قابل باز شدن دارن (نه سرویس‌های سیستمی مخفی)
                    if (pm.getLaunchIntentForPackage(pkg) == null) return@mapNotNull null
                    AppUsageInfo(
                        packageName = pkg,
                        label = pm.getApplicationLabel(appInfo).toString(),
                        icon = pm.getApplicationIcon(appInfo),
                        usageMillis = millis,
                        hasActiveRule = pkg in lockedPackages
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
            .sortedByDescending { it.usageMillis }
            .take(30) // فقط ۳۰ تای پراستفاده‌ترین، تا لیست خیلی طولانی نشه

        binding.emptyUsageText.visibility =
            if (items.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

        binding.usageRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.usageRecyclerView.adapter = UsageStatsAdapter(items)
    }
}
