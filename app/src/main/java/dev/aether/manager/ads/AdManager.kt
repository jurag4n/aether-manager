package dev.aether.manager.ads

import dev.aether.manager.NativeAether

object AdManager {

    // Fallback hardcoded — dipakai kalau libaether.so gagal load.
    // Ganti dengan Unity Game ID production kamu.
    private const val FALLBACK_GAME_ID = "YOUR_UNITY_GAME_ID"

    val GAME_ID: String
        get() = if (NativeAether.isLoaded) {
            runCatching { NativeAether.nativeGetGameId() }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: FALLBACK_GAME_ID
        } else {
            FALLBACK_GAME_ID
        }

    val isTestMode: Boolean = false
}
