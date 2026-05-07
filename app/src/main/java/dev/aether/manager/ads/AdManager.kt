package dev.aether.manager.ads

import dev.aether.manager.NativeAether
import dev.aether.manager.NativeSecrets

object AdManager {

    val GAME_ID: String
        get() = NativeSecrets.gameId()

    val isTestMode: Boolean = false
}
