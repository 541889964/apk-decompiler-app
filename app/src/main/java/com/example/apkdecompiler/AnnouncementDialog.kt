package com.example.apkdecompiler

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.DialogFragment
import com.example.apkdecompiler.databinding.DialogAnnouncementBinding

class AnnouncementDialog : DialogFragment() {
    private var _b: DialogAnnouncementBinding? = null
    private val b get() = _b!!

    override fun onCreateDialog(s: Bundle?): Dialog {
        return super.onCreateDialog(s).apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.92).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setCanceledOnTouchOutside(false)
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View? {
        _b = DialogAnnouncementBinding.inflate(i, c, false)
        return b.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)

        val card = b.cardAnnouncement
        card.scaleX = 0.5f
        card.scaleY = 0.5f
        card.alpha = 0f
        card.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(500)
            .setInterpolator(OvershootInterpolator(1.4f))
            .start()

        b.btnAgree.setOnClickListener {
            card.animate()
                .scaleX(0.7f).scaleY(0.7f).alpha(0f)
                .setDuration(220)
                .withEndAction { dismissAllowingStateLoss() }
                .start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
