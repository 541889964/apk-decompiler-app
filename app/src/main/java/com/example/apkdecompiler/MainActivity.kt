package com.example.apkdecompiler

import android.os.Bundle
import android.util.Log
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.apkdecompiler.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            b = ActivityMainBinding.inflate(layoutInflater)
            setContentView(b.root)
        } catch (e: Exception) {
            Log.e("APKDECOMPILER", "inflate failed", e)
            finish(); return
        }

        try {
            ViewCompat.setOnApplyWindowInsetsListener(b.root) { v, insets ->
                val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.updatePadding(top = sys.top, bottom = sys.bottom)
                insets
            }
        } catch (e: Exception) { Log.e("APKDECOMPILER", "insets", e) }

        setupAnimations()
        setupClicks()

        if (savedInstanceState == null) {
            try {
                AnnouncementDialog().show(supportFragmentManager, "ann")
            } catch (e: Exception) { Log.e("APKDECOMPILER", "dialog", e) }
        }
    }

    private fun setupAnimations() {
        val ease = DecelerateInterpolator()
        val spring = OvershootInterpolator(1.1f)

        // 顶部徽章
        b.tvBadge.alpha = 0f
        b.tvBadge.translationY = -20f
        b.tvBadge.animate().alpha(1f).translationY(0f)
            .setDuration(500).setInterpolator(ease).start()

        // 主标题
        b.tvTitle.alpha = 0f
        b.tvTitle.translationY = -30f
        b.tvTitle.animate().alpha(1f).translationY(0f)
            .setDuration(600).setStartDelay(100).setInterpolator(ease).start()

        // 副标题
        b.tvSubtitle.alpha = 0f
        b.tvSubtitle.animate().alpha(1f)
            .setDuration(600).setStartDelay(250).setInterpolator(ease).start()

        // 上传卡片
        b.cardUpload.alpha = 0f
        b.cardUpload.translationY = 100f
        b.cardUpload.scaleX = 0.85f
        b.cardUpload.scaleY = 0.85f
        b.cardUpload.animate().alpha(1f).translationY(0f)
            .scaleX(1f).scaleY(1f)
            .setDuration(800).setStartDelay(350)
            .setInterpolator(spring).start()

        // 状态行
        b.statusRow.alpha = 0f
        b.statusRow.animate().alpha(1f)
            .setDuration(600).setStartDelay(700).setInterpolator(ease).start()
    }

    private fun setupClicks() {
        b.cardUpload.setOnClickListener {
            it.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f)
                        .setDuration(180).setInterpolator(OvershootInterpolator(2f))
                        .start()
                }.start()
            b.tvStatus.text = "请选择 APK 文件..."
        }
    }
}
