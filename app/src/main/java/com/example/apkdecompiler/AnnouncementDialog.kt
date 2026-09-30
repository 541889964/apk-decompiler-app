package com.example.apkdecompiler

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.DialogFragment
import com.example.apkdecompiler.databinding.DialogAnnouncementBinding

class AnnouncementDialog : DialogFragment() {
    private var _b: DialogAnnouncementBinding? = null
    private val b get() = _b!!

    override fun onCreateDialog(s: Bundle?): Dialog {
        return super.onCreateDialog(s).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCanceledOnTouchOutside(false)
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = DialogAnnouncementBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        b.cardAnnouncement.scaleX = 0.8f; b.cardAnnouncement.scaleY = 0.8f; b.cardAnnouncement.alpha = 0f
        b.cardAnnouncement.animate().scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(400).setInterpolator(OvershootInterpolator(1.2f)).start()
        b.btnAgree.setOnClickListener {
            b.cardAnnouncement.animate().scaleX(0.9f).scaleY(0.9f).alpha(0f)
                .setDuration(200).withEndAction { dismiss() }.start()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
