package dev.aether.manager.ads

import dev.aether.manager.NativeAether

object AdManager {
    val GAME_ID: String
        get() = if (NativeAether.isLoaded) NativeAether.nativeGetGameId() else ""

    val isTestMode: Boolean = false
}
