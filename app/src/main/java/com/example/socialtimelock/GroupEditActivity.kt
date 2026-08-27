package com.example.socialtimelock

import android.app.TimePickerDialog
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialtimelock.databinding.ActivityGroupEditBinding
import com.example.socialtimelock.databinding.ItemTimeRangeBinding
import java.util.Calendar

/**
 * صفحه ساخت/ویرایش یک گروه قفل. یک گروه می‌تونه یک اپ تنها داشته باشه
 * (محدودیت تکی) یا چند اپ با هم (محدودیت گروهی) — هر دو حالت با همین صفحه پوشش داده می‌شن.
 */
class GroupEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
    }

    private lateinit var binding: ActivityGroupEditBinding
    private lateinit var appAdapter: AppListAdapter
    private val ranges = mutableListOf<TimeRange>()
    private var editingGroupId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editingGroupId = intent.getStringExtra(EXTRA_GROUP_ID)
        val existingGroup = editingGroupId?.let { id ->
            PrefsHelper.getGroups(this).find { it.id == id }
        }

        if (existingGroup != null) {
            binding.groupNameInput.setText(existingGroup.name)
            ranges.addAll(existingGroup.ranges)
            binding.btnDeleteGroup.visibility = android.view.View.VISIBLE
        }

        setupAppList(existingGroup)
        refreshRangesUi()

        binding.btnAddRange.setOnClickListener { showAddRangeDialog() }
        binding.btnSaveGroup.setOnClickListener { saveGroup() }
        binding.btnDeleteGroup.setOnClickListener { deleteGroup() }
    }

    private fun setupAppList(existingGroup: LockGroup?) {
        val pm = packageManager
        val currentGroupPackages = existingGroup?.packages ?: emptySet()
        val allGroups = PrefsHelper.getGroups(this)

        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { appInfo ->
                pm.getLaunchIntentForPackage(appInfo.packageName) != null &&
                        appInfo.packageName != packageName
            }
            .map { appInfo: ApplicationInfo ->
                val otherGroup = allGroups.find {
                    it.id != editingGroupId && appInfo.packageName in it.packages
                }
                AppInfo(
                    packageName = appInfo.packageName,
                    label = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo),
                    isSelected = currentGroupPackages.contains(appInfo.packageName),
                    alreadyInGroupName = otherGroup?.name
                )
            }
            .sortedBy { it.label.lowercase() }

        appAdapter = AppListAdapter(installedApps)
        binding.appRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.appRecyclerView.adapter = appAdapter
    }

    private fun showAddRangeDialog() {
        val now = Calendar.getInstance()
        TimePickerDialog(this, { _, startHour, startMinute ->
            TimePickerDialog(this, { _, endHour, endMinute ->
                val start = startHour * 60 + startMinute
                val end = endHour * 60 + endMinute
                ranges.add(TimeRange(start, end))
                ranges.sortBy { it.startMinute }
                refreshRangesUi()
            }, startHour, startMinute, true).apply {
                setTitle("ساعت پایان بازه")
            }.show()
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).apply {
            setTitle("ساعت شروع بازه")
        }.show()
    }

    private fun refreshRangesUi() {
        binding.rangesContainer.removeAllViews()

        if (ranges.isEmpty()) {
            val empty = TextView(this).apply {
                text = "هنوز بازه‌ای اضافه نکردی — یعنی این گروه همیشه قفله."
                setTextColor(getColor(R.color.text_secondary))
                textSize = 14f
            }
            binding.rangesContainer.addView(empty)
            return
        }

        for (range in ranges) {
            val rowBinding = ItemTimeRangeBinding.inflate(
                LayoutInflater.from(this), binding.rangesContainer, false
            )
            rowBinding.rangeText.text = range.toDisplayString()
            rowBinding.btnRemoveRange.setOnClickListener {
                ranges.remove(range)
                refreshRangesUi()
            }
            binding.rangesContainer.addView(rowBinding.root)
        }
    }

    private fun saveGroup() {
        val selectedPackages = appAdapter.getSelectedPackages()
        if (selectedPackages.isEmpty()) {
            Toast.makeText(this, "حداقل یک اپ باید انتخاب کنی", Toast.LENGTH_SHORT).show()
            return
        }

        var name = binding.groupNameInput.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            name = if (selectedPackages.size == 1) "محدودیت تکی" else "گروه بدون‌نام"
        }

        val allGroups = PrefsHelper.getGroups(this)

        // این پکیج‌ها رو از هر گروه دیگه‌ای که توش بودن حذف کن (هر اپ فقط توی یک گروه)
        allGroups.forEach { group ->
            if (group.id != editingGroupId) {
                group.packages.removeAll(selectedPackages)
            }
        }
        // گروه‌هایی که با حذف پکیج‌ها خالی موندن رو هم حذف کن
        allGroups.removeAll { it.id != editingGroupId && it.packages.isEmpty() }

        val existingIndex = allGroups.indexOfFirst { it.id == editingGroupId }
        if (existingIndex >= 0) {
            allGroups[existingIndex] = LockGroup(
                id = editingGroupId!!,
                name = name,
                packages = selectedPackages.toMutableSet(),
                ranges = ranges.toMutableList()
            )
        } else {
            allGroups.add(
                LockGroup(
                    name = name,
                    packages = selectedPackages.toMutableSet(),
                    ranges = ranges.toMutableList()
                )
            )
        }

        PrefsHelper.saveGroups(this, allGroups)
        Toast.makeText(this, "ذخیره شد ✅", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun deleteGroup() {
        AlertDialog.Builder(this)
            .setTitle("حذف این گروه؟")
            .setMessage("اپ‌های این گروه دیگه محدودیتی نخواهند داشت.")
            .setPositiveButton("حذف کن") { _, _ ->
                val allGroups = PrefsHelper.getGroups(this)
                allGroups.removeAll { it.id == editingGroupId }
                PrefsHelper.saveGroups(this, allGroups)
                finish()
            }
            .setNegativeButton("انصراف", null)
            .show()
    }
}
