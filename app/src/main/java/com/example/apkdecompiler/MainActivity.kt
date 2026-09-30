package com.example.apkdecompiler

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
    private var selectedUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        ViewCompat.setOnApplyWindowInsetsListener(b.root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = sys.top, bottom = sys.bottom)
            insets
        }

        // 动画延迟 50ms 执行, 保证视图已布局
        b.root.postDelayed({ runEntranceAnimation() }, 50)

        b.cardUpload.setOnClickListener { pickApk() }

        if (savedInstanceState == null) {
            b.root.postDelayed({
                try { AnnouncementDialog().show(supportFragmentManager, "ann") }
                catch (e: Exception) { Log.e("APKDECOMPILER", "dialog", e) }
            }, 300)
        }
    }

    private fun runEntranceAnimation() {
        val ease = DecelerateInterpolator()
        val spring = OvershootInterpolator(1.2f)

        // 徽章 - 从上往下
        b.tvBadge.alpha = 0f
        b.tvBadge.translationY = -40f
        b.tvBadge.animate().alpha(1f).translationY(0f)
            .setDuration(500).setInterpolator(spring).start()

        // 标题 - 从下往上
        b.tvTitle.alpha = 0f
        b.tvTitle.translationY = 40f
        b.tvTitle.animate().alpha(1f).translationY(0f)
            .setDuration(500).setStartDelay(120).setInterpolator(ease).start()

        // 副标题
        b.tvSubtitle.alpha = 0f
        b.tvSubtitle.animate().alpha(1f)
            .setDuration(400).setStartDelay(280).start()

        // 上传卡片 - 弹跳放大
        b.cardUpload.alpha = 0f
        b.cardUpload.scaleX = 0.7f
        b.cardUpload.scaleY = 0.7f
        b.cardUpload.animate().alpha(1f)
            .scaleX(1f).scaleY(1f)
            .setDuration(700).setStartDelay(350)
            .setInterpolator(spring).start()

        // 状态
        b.tvStatus.alpha = 0f
        b.tvStatus.animate().alpha(1f)
            .setDuration(400).setStartDelay(750).start()
    }

    private fun pickApk() {
        // 按压缩放反馈
        b.cardUpload.animate().scaleX(0.95f).scaleY(0.95f).setDuration(90)
            .withEndAction {
                b.cardUpload.animate().scaleX(1f).scaleY(1f)
                    .setDuration(150).setInterpolator(OvershootInterpolator(2f)).start()
            }.start()

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/vnd.android.package-archive"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            startActivityForResult(Intent.createChooser(intent, "选择 APK 文件"), 1001)
        } catch (e: Exception) {
            // 有些设备不接受 apk mime, 退回到 */*
            val fallback = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(Intent.createChooser(fallback, "选择 APK 文件"), 1001)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedUri = uri
                val name = queryName(uri)
                val size = querySize(uri)
                b.tvFileName.text = "$name (${formatSize(size)})"
                b.tvStatus.text = "已选择, 准备上传..."
            }
        }
    }

    private fun queryName(uri: Uri): String {
        var name = "unknown.apk"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) name = c.getString(idx)
            }
        } catch (_: Exception) {}
        return name
    }

    private fun querySize(uri: Uri): Long {
        var size = 0L
        try {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0 && c.moveToFirst()) size = c.getLong(idx)
            }
        } catch (_: Exception) {}
        return size
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "未知大小"
        val mb = bytes / 1024.0 / 1024.0
        return String.format("%.2f MB", mb)
    }
}
