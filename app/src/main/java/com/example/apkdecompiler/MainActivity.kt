package com.example.apkdecompiler

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.apkdecompiler.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        ViewCompat.setOnApplyWindowInsetsListener(b.root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = sys.top, bottom = sys.bottom)
            insets
        }

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

        b.tvBadge.alpha = 0f; b.tvBadge.translationY = -40f
        b.tvBadge.animate().alpha(1f).translationY(0f)
            .setDuration(500).setInterpolator(spring).start()

        b.tvTitle.alpha = 0f; b.tvTitle.translationY = 40f
        b.tvTitle.animate().alpha(1f).translationY(0f)
            .setDuration(500).setStartDelay(120).setInterpolator(ease).start()

        b.tvSubtitle.alpha = 0f
        b.tvSubtitle.animate().alpha(1f)
            .setDuration(400).setStartDelay(280).start()

        b.cardUpload.alpha = 0f
        b.cardUpload.scaleX = 0.7f; b.cardUpload.scaleY = 0.7f
        b.cardUpload.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(700).setStartDelay(350).setInterpolator(spring).start()

        b.tvStatus.alpha = 0f
        b.tvStatus.animate().alpha(1f)
            .setDuration(400).setStartDelay(750).start()
    }

    private fun pickApk() {
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
            startActivityForResult(Intent.createChooser(intent, "选择 APK"), 1001)
        } catch (e: Exception) {
            val fb = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(Intent.createChooser(fb, "选择 APK"), 1001)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val name = queryName(uri)
                b.tvFileName.text = name
                b.tvStatus.text = "开始分析..."
                startDecompile(uri, name)
            }
        }
    }

    private fun startDecompile(uri: Uri, name: String) {
        b.progressBar.visibility = View.VISIBLE
        b.progressBar.progress = 0

        scope.launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    Decompiler.decompile(this@MainActivity, uri, name) { p ->
                        runOnUiThread {
                            b.progressBar.progress = p.percent
                            b.tvStatus.text = "[${p.percent}%] ${p.message}"
                        }
                    }
                }
                b.tvStatus.text = "引擎: ${res.engine.display}\n${res.engine.advice}"
                b.tvFileName.text = "输出: ${res.outputDir.absolutePath}"
            } catch (e: Exception) {
                Log.e("APKDECOMPILER", "decompile failed", e)
                b.tvStatus.text = "失败: ${e.message}"
            }
        }
    }

    private fun queryName(uri: Uri): String {
        var n = "unknown.apk"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0 && c.moveToFirst()) n = c.getString(i)
            }
        } catch (_: Exception) {}
        return n
    }
}
