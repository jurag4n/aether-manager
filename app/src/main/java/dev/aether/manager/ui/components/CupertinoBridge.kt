package dev.aether.manager.ui.components

import androidx.compose.runtime.Composable
import dev.aether.manager.ui.AetherThemePreset
import dev.aether.manager.ui.LocalAetherThemeStyle

/**
 * Small bridge flag for iOS/Cupertino themed widgets.
 * Actual Cupertino composition is installed in AetherTheme when preset == IOS.
 */
@Composable
fun isCupertinoThemeActive(): Boolean = LocalAetherThemeStyle.current.preset == AetherThemePreset.IOS
