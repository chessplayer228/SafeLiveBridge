package com.safelivebridge.app.receiver

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.safelivebridge.app.R

class OtpCopyReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_OTP_CODE = "extra_otp_code"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val code = intent?.getStringExtra(EXTRA_OTP_CODE) ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OTP Code", code)
        clipboard.setPrimaryClip(clip)

        val message = context.getString(R.string.otp_copied_toast, code)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
