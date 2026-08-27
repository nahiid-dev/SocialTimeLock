package com.example.socialtimelock

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.socialtimelock.databinding.ActivityBlockedBinding

class BlockedScreenActivity : AppCompatActivity() {

    companion object {
        private const val WAIT_SECONDS = 30L          // deliberate delay before granting access
        private const val GRANT_MINUTES = 5L           // length of temporary access granted afterward
    }

    private lateinit var binding: ActivityBlockedBinding
    private var blockedPackage: String? = null
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        blockedPackage = intent.getStringExtra(AppBlockerAccessibilityService.EXTRA_BLOCKED_PACKAGE)
        setupAppName()

        binding.btnBackHome.setOnClickListener { goHome() }
        binding.btnRequestDelay.setOnClickListener { startDelayCountdown() }
    }

    private fun setupAppName() {
        val pkg = blockedPackage ?: return
        try {
            val label = packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
            binding.blockedAppName.text = label
        } catch (e: PackageManager.NameNotFoundException) {
            // If the app name isn't found, the default text stays as-is
        }
    }

    /**
     * Instead of unlocking immediately, we wait 30 seconds. This delay is
     * intentional — its purpose is to break the impulsive, unconscious urge
     * to open the app.
     */
    private fun startDelayCountdown() {
        binding.btnRequestDelay.isEnabled = false

        countDownTimer = object : CountDownTimer(WAIT_SECONDS * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000) + 1
                binding.btnRequestDelay.text = getString(R.string.btn_waiting_format, secondsLeft.toInt())
            }

            override fun onFinish() {
                val pkg = blockedPackage
                if (pkg != null) {
                    PrefsHelper.grantTemporaryAccess(
                        this@BlockedScreenActivity, pkg, GRANT_MINUTES * 60 * 1000
                    )
                    Toast.makeText(
                        this@BlockedScreenActivity,
                        getString(R.string.temp_access_granted_format, GRANT_MINUTES.toInt()),
                        Toast.LENGTH_SHORT
                    ).show()
                    openApp(pkg)
                }
                finish()
            }
        }.start()
    }

    private fun openApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        }
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    // Back button also goes home, not back to the locked app
    override fun onBackPressed() {
        goHome()
    }
}
