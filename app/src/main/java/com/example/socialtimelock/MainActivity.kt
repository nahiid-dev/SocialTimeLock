package com.example.socialtimelock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.socialtimelock.databinding.ActivityMainBinding
import com.example.socialtimelock.databinding.ItemRuleBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home screen: a list of all the "rules" created so far (each with one or
 * more apps + its own allowed time windows). From here you can create a new
 * rule, edit/delete existing ones, or back up / restore your rules.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { exportRulesTo(it) }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { promptImportMode(it) }
    }

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
        binding.btnExportRules.setOnClickListener { startExport() }
        binding.btnImportRules.setOnClickListener {
            importLauncher.launch(arrayOf("application/json"))
        }
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        refreshRulesList()
    }

    // ---------- Backup ----------

    private fun startExport() {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
        exportLauncher.launch("social-time-lock-backup-$timestamp.json")
    }

    private fun exportRulesTo(uri: Uri) {
        try {
            val json = PrefsHelper.exportGroupsAsJson(this)
            contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray())
            }
            Toast.makeText(this, getString(R.string.export_success), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.export_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun promptImportMode(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle(R.string.import_confirm_title)
            .setMessage(R.string.import_confirm_message)
            .setPositiveButton(R.string.btn_import_merge) { _, _ -> importRulesFrom(uri, replace = false) }
            .setNegativeButton(R.string.btn_import_replace) { _, _ -> importRulesFrom(uri, replace = true) }
            .show()
    }

    private fun importRulesFrom(uri: Uri, replace: Boolean) {
        try {
            val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalStateException("empty file")
            val count = PrefsHelper.importGroupsFromJson(this, json, replace)
            Toast.makeText(
                this, getString(R.string.import_success_format, count), Toast.LENGTH_SHORT
            ).show()
            refreshRulesList()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.import_failed), Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- Rules list ----------

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

            rowBinding.ruleName.text = group.name.ifEmpty { getString(R.string.default_name_group) }
            rowBinding.ruleApps.text = if (appLabels.isEmpty())
                getString(R.string.no_apps_label) else appLabels.joinToString(", ")
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
            .setTitle(R.string.delete_group_title)
            .setMessage(R.string.delete_group_message)
            .setPositiveButton(R.string.btn_confirm_delete) { _, _ ->
                val groups = PrefsHelper.getGroups(this)
                groups.removeAll { it.id == groupId }
                PrefsHelper.saveGroups(this, groups)
                refreshRulesList()
            }
            .setNegativeButton(R.string.btn_cancel, null)
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
