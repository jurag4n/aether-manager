package dev.aether.manager.ads

import dev.aether.manager.NativeAether

object AdManager {
    val GAME_ID: String
        get() = if (NativeAether.tryLoad()) NativeAether.nativeGetGameId() else ""

    // Ganti ke false untuk production build
    val isTestMode: Boolean = false
}
