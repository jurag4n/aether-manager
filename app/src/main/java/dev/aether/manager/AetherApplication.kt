package dev.aether.manager

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.topjohnwu.superuser.Shell
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import dev.aether.manager.ads.AdManager
import dev.aether.manager.ads.InterstitialAdManager
import dev.aether.manager.notification.NotificationHelper
import dev.aether.manager.notification.NotificationScheduler
import dev.aether.manager.security.AetherSecurityNative
import java.util.concurrent.atomic.AtomicBoolean

class AetherApplication : Application() {

    // Background thread for security checks — avoids ANR from l1_frida_port()
    // which opens 3 socket connections on each call.
    private val securityExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "aether-sec").also { it.isDaemon = true }
    }

    private val periodicSecurityCheck = object : Runnable {
        override fun run() {
            if (!BuildConfig.DEBUG) {
                securityExecutor.execute { runSecurityChecks() }
            }
            securityHandler.postDelayed(this, SECURITY_INTERVAL_MS)
        }
    }

    private val securityHandler = Handler(Looper.getMainLooper())
    private val securityTerminating = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()

        NativeAether.tryLoad(this)
        AetherSecurityNative.tryLoad(this)

        if (!BuildConfig.DEBUG) {
            if (!AetherSecurityNative.highCheck(this)) { securityExit(AetherSecurityNative.tamperReason(this)); return }
            // checkSignature runs on main thread (no I/O, just crypto) — fine
            checkSignature()
            // All other checks go to background: l1_frida_port() does 3 socket
            // connects per call; running on main thread risks ANR.
            securityExecutor.execute {
                runSecurityChecks()
                securityHandler.postDelayed(periodicSecurityCheck, SECURITY_INTERVAL_MS)
            }
        }

        initLibsu()
        initUnityAds()

        NotificationHelper.createChannels(this)
        NotificationScheduler.schedule(this)
    }

    private fun securityExit(reason: String) {
        if (!securityTerminating.compareAndSet(false, true)) return

        val cleanReason = reason.ifBlank { "security_violation" }
        securityHandler.post {
            Toast.makeText(
                applicationContext,
                securityMessage(cleanReason),
                Toast.LENGTH_LONG
            ).show()

            securityHandler.postDelayed({
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(0)
            }, 1700L)
        }
    }

    private fun securityMessage(reason: String): String {
        return when (reason.lowercase()) {
            "signature_mismatch" -> "Security blocked: signature mismatch"
            "signature_missing" -> "Security blocked: app signature missing"
            "native_not_loaded" -> "Security blocked: native library failed"
            "native_tamper" -> "Security blocked: native tamper detected"
            "package_repack" -> "Security blocked: package repack detected"
            "apk_repack" -> "Security blocked: APK repack detected"
            "loader_tamper" -> "Security blocked: custom loader detected"
            "debugger", "debug_detected" -> "Security blocked: debugger detected"
            "frida_port" -> "Security blocked: Frida detected"
            "hook_framework", "hook_fd", "hook_detected" -> "Security blocked: LSPosed/Xposed hook detected"
            "dump_tool", "dump_fd" -> "Security blocked: dump tool detected"
            "patcher_files", "anti_patch_failed" -> "Security blocked: Lucky Patcher detected"
            "unity_tamper" -> "Security blocked: ads component tampered"
            else -> "Security blocked: $reason"
        }
    }

    private fun checkSignature() {
        // Safety guard: if EXPECTED_SIGNATURE is still the placeholder, skip the
        // check entirely. Without this, every release build dies immediately because
        // no real cert hash can ever match the placeholder string.
        if (EXPECTED_SIGNATURE == SIGNATURE_PLACEHOLDER) return

        if (!NativeAether.isLoaded) { securityExit("native_not_loaded"); return }
        try {
            @Suppress("DEPRECATION")
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                PackageManager.GET_SIGNING_CERTIFICATES
            else
                PackageManager.GET_SIGNATURES

            val info = packageManager.getPackageInfo(packageName, flags)

            val sigBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                info.signatures?.firstOrNull()?.toByteArray()
            }

            if (sigBytes == null) { securityExit("signature_missing"); return }

            val hex = java.security.MessageDigest.getInstance("SHA-256")
                .digest(sigBytes)
                .joinToString("") { "%02x".format(it) }

            if (hex != EXPECTED_SIGNATURE) securityExit("signature_mismatch")
        } catch (_: Throwable) {
            securityExit("signature_mismatch")
        }
    }

    private fun runSecurityChecks() {
        if (!NativeAether.isLoaded) return
        try {
            if (NativeAether.nativeIsHooked()) {
                securityExit("hook_detected"); return
            }
            if (NativeAether.nativeIsDebugged()) {
                securityExit("debug_detected"); return
            }
            if (!NativeAether.nativeCheckAntiPatch(this)) {
                securityExit("anti_patch_failed"); return
            }
            if (!AetherSecurityNative.highCheck(this)) {
                securityExit(AetherSecurityNative.tamperReason(this)); return
            }
        } catch (_: Throwable) {
            if (NativeAether.isLoaded) securityExit("security_violation")
        }
    }

    private fun initLibsu() {
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        dev.aether.manager.util.RootManager.configureShell(this)

        val prefs = getSharedPreferences("aether_prefs", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean("setup_done", false)) {
            java.util.concurrent.Executors.newSingleThreadExecutor().execute {
                val ok = dev.aether.manager.util.RootManager.ensureRootShellSync(requestIfNeeded = false)
                if (ok) dev.aether.manager.util.RootManager.markGranted()
            }
        }
    }

    private fun initUnityAds() {
        val gameId = AdManager.GAME_ID
        if (gameId.isEmpty()) return

        if (UnityAds.isInitialized) {
            if (!BuildConfig.DEBUG) checkUnityIntact()
            InterstitialAdManager.preload(this)
            return
        }

        UnityAds.initialize(
            this,
            gameId,
            AdManager.isTestMode,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    if (!BuildConfig.DEBUG) checkUnityIntact()
                    InterstitialAdManager.preload(this@AetherApplication)
                }
                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String,
                ) {
                    if (!BuildConfig.DEBUG) checkUnityIntact()
                }
            }
        )
    }

    private fun checkUnityIntact() {
        if (!NativeAether.isLoaded) return
        try {
            NativeAether.nativeCheckUnityIntact()
        } catch (_: Exception) {
            if (NativeAether.isLoaded) securityExit("unity_tamper")
        }
    }

    companion object {
        private const val SECURITY_INTERVAL_MS = 45_000L

        // Replace EXPECTED_SIGNATURE with the actual SHA-256 hex of your signing cert.
        // To get it: run  `apksigner verify --print-certs your.apk`  and copy the
        // "Signer #1 certificate SHA-256 digest" value (lowercase hex, 64 chars).
        // Until it's set, checkSignature() is skipped (see SIGNATURE_PLACEHOLDER guard).
        private const val SIGNATURE_PLACEHOLDER = "GANTI_DENGAN_SHA256_SIGNING_CERT_KAMU"
        private const val EXPECTED_SIGNATURE    = SIGNATURE_PLACEHOLDER
    }
}
