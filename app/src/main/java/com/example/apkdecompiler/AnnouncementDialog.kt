package com.example.apkdecompiler

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.DialogFragment
import com.example.apkdecompiler.databinding.DialogAnnouncementBinding

class AnnouncementDialog : DialogFragment() {
    private var _b: DialogAnnouncementBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View? {
        return try {
            _b = DialogAnnouncementBinding.inflate(i, c, false)
            b.root
        } catch (e: Exception) {
            Log.e("APKDECOMPILER", "inflate", e); null
        }
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        try {
            dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog?.setCanceledOnTouchOutside(false)

            val card = b.cardAnnouncement
            card.scaleX = 0.7f
            card.scaleY = 0.7f
            card.alpha = 0f
            card.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(450)
                .setInterpolator(OvershootInterpolator(1.3f))
                .start()

            b.btnAgree.setOnClickListener {
                try {
                    card.animate()
                        .scaleX(0.85f).scaleY(0.85f).alpha(0f)
                        .setDuration(200)
                        .setInterpolator(DecelerateInterpolator())
                        .withEndAction { dismissAllowingStateLoss() }
                        .start()
                } catch (e: Exception) { dismissAllowingStateLoss() }
            }
        } catch (e: Exception) {
            Log.e("APKDECOMPILER", "setup", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
