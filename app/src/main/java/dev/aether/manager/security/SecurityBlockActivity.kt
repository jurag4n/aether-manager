package dev.aether.manager.security

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.WindowManager
import kotlin.system.exitProcess

class SecurityBlockActivity : Activity() {
    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val reason = intent.getStringExtra(EXTRA_REASON).orEmpty().ifBlank { "security_violation" }
        showBlockedDialog(reason)
    }

    override fun onResume() {
        super.onResume()
        if (dialog?.isShowing != true) {
            showBlockedDialog(intent.getStringExtra(EXTRA_REASON).orEmpty().ifBlank { "security_violation" })
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = Unit

    private fun showBlockedDialog(reason: String) {
        dialog?.dismiss()
        dialog = AlertDialog.Builder(this)
            .setTitle("Aether Security")
            .setMessage("Aktivitas tidak wajar terdeteksi.\n\nAlasan: $reason\n\nAplikasi ditutup untuk melindungi lisensi dan data.")
            .setCancelable(false)
            .setPositiveButton("Close") { _, _ -> closeApp() }
            .create()
            .also {
                it.setCanceledOnTouchOutside(false)
                it.setOnKeyListener { _, _, _ -> true }
                it.setOnDismissListener { if (!isFinishing) showBlockedDialog(reason) }
                it.show()
            }
    }

    private fun closeApp() {
        runCatching { finishAndRemoveTask() }
        runCatching { android.os.Process.killProcess(android.os.Process.myPid()) }
        exitProcess(10)
    }

    companion object {
        const val EXTRA_REASON = "reason"
    }
}
