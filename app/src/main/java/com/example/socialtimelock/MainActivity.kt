package com.example.socialtimelock

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.socialtimelock.databinding.ActivityMainBinding
import com.example.socialtimelock.databinding.ItemRuleBinding

/**
 * صفحه اصلی: لیست تمام "قانون"های ساخته‌شده (هرکدوم شامل یک یا چند اپ + بازه‌های مجاز خودشون).
 * از اینجا می‌تونی قانون جدید بسازی، یا قانون‌های قبلی رو ویرایش/حذف کنی.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnNewRule.setOnClickListener {
            startActivity(Intent(this, GroupEditActivity::class.java))
        }
        binding.btnUsageStats.setOnClickListener {
            startActivity(Intent(this, UsageStatsActivity::class.java))
        }
        binding.btnEnableService.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        refreshRulesList()
    }

    private fun refreshRulesList() {
        binding.rulesContainer.removeAllViews()
        val groups = PrefsHelper.getGroups(this)

        if (groups.isEmpty()) {
            binding.emptyRulesText.visibility = android.view.View.VISIBLE
            return
        }
        binding.emptyRulesText.visibility = android.view.View.GONE

        val pm = packageManager
        for (group in groups.sortedBy { it.name }) {
            val rowBinding = ItemRuleBinding.inflate(
                LayoutInflater.from(this), binding.rulesContainer, false
            )

            val appLabels = group.packages.mapNotNull { pkg ->
                try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (e: Exception) {
                    null
                }
            }

            rowBinding.ruleName.text = group.name.ifEmpty { "قانون بدون‌نام" }
            rowBinding.ruleApps.text = if (appLabels.isEmpty())
                "بدون اپ" else appLabels.joinToString("، ")
            rowBinding.ruleRanges.text = group.rangesDisplayString()

            rowBinding.root.setOnClickListener { openEditor(group.id) }
            rowBinding.btnEditRule.setOnClickListener { openEditor(group.id) }
            rowBinding.btnDeleteRule.setOnClickListener { confirmDelete(group.id) }

            binding.rulesContainer.addView(rowBinding.root)
        }
    }

    private fun openEditor(groupId: String) {
        val intent = Intent(this, GroupEditActivity::class.java)
        intent.putExtra(GroupEditActivity.EXTRA_GROUP_ID, groupId)
        startActivity(intent)
    }

    private fun confirmDelete(groupId: String) {
        AlertDialog.Builder(this)
            .setTitle("حذف این قانون؟")
            .setMessage("اپ‌های این قانون دیگه محدودیتی نخواهند داشت.")
            .setPositiveButton("حذف کن") { _, _ ->
                val groups = PrefsHelper.getGroups(this)
                groups.removeAll { it.id == groupId }
                PrefsHelper.saveGroups(this, groups)
                refreshRulesList()
            }
            .setNegativeButton("انصراف", null)
            .show()
    }

    private fun updateServiceStatus() {
        val enabled = isAccessibilityServiceEnabled()
        binding.statusText.text = if (enabled)
            getString(R.string.status_service_on) else getString(R.string.status_service_off)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "$packageName/${AppBlockerAccessibilityService::class.java.canonicalName}"
        val enabledServicesSetting = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
