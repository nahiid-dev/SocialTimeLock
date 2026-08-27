package com.example.socialtimelock

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.socialtimelock.databinding.ActivityBlockedBinding

class BlockedScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackHome.setOnClickListener {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            finish()
        }
    }

    // با دکمه برگشت گوشی هم بره به هوم، نه به اپ قفل‌شده
    override fun onBackPressed() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}
