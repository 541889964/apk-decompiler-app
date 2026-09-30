package com.example.apkdecompiler

import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.apkdecompiler.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = sys.top, bottom = sys.bottom)
            insets
        }

        setupAnimations()
        setupClicks()

        if (savedInstanceState == null) AnnouncementDialog().show(supportFragmentManager, "ann")
    }

    private fun setupAnimations() {
        binding.tvTitle.alpha = 0f; binding.tvTitle.translationY = -30f
        binding.tvTitle.animate().alpha(1f).translationY(0f)
            .setDuration(600).setInterpolator(DecelerateInterpolator()).start()

        binding.tvSubtitle.alpha = 0f
        binding.tvSubtitle.animate().alpha(1f).setDuration(600).setStartDelay(200).start()

        binding.cardUpload.alpha = 0f; binding.cardUpload.translationY = 80f
        binding.cardUpload.scaleX = 0.9f; binding.cardUpload.scaleY = 0.9f
        binding.cardUpload.animate().alpha(1f).translationY(0f)
            .scaleX(1f).scaleY(1f).setDuration(700).setStartDelay(300)
            .setInterpolator(DecelerateInterpolator()).start()
    }

    private fun setupClicks() {
        binding.cardUpload.setOnClickListener {
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()
            binding.tvStatus.text = "请选择 APK 文件..."
        }
    }
}
