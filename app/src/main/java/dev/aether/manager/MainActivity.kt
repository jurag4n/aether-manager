package dev.aether.manager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.content.Intent
import dev.aether.manager.ads.AdBlockChecker
import dev.aether.manager.ads.AdBlockDetectedDialog
import dev.aether.manager.ads.AdScheduler
import dev.aether.manager.ads.InterstitialAdManager
import kotlinx.coroutines.delay
import dev.aether.manager.data.AppProfileViewModel
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.i18n.ProvideStrings
import androidx.compose.foundation.isSystemInDarkTheme
import dev.aether.manager.license.LicenseManager
import dev.aether.manager.notification.LicenseNotificationChecker
import dev.aether.manager.notification.UpdateNotificationHelper
import dev.aether.manager.ui.AetherTheme
import dev.aether.manager.ui.appprofile.AppProfileScreen
import dev.aether.manager.ui.components.RebootBottomSheet
import dev.aether.manager.ui.home.HomeScreen
import dev.aether.manager.ui.settings.SettingsScreen
import dev.aether.manager.ui.tweak.TweakScreen
import dev.aether.manager.update.UpdateDialogHost
import dev.aether.manager.update.UpdateViewModel
import dev.aether.manager.util.RootUtils

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private val apVm: AppProfileViewModel by viewModels()
    private val updateVm: UpdateViewModel by viewModels()

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — silently continue */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        enableEdgeToEdge()
        setContent {
            val darkModeOverride by vm.darkModeOverride.collectAsState()
            val darkMode         by vm.darkMode.collectAsState()
            val dynamicColor     by vm.dynamicColor.collectAsState()
            val effectiveDark    = if (darkModeOverride) darkMode else isSystemInDarkTheme()
            AetherTheme(darkTheme = effectiveDark, dynamicColor = dynamicColor) {
                ProvideStrings {
                    AetherApp(vm, apVm, updateVm)
                }
            }
        }
    }
}

private enum class Screen { HOME, TWEAK, APPS }

private data class NavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AetherApp(vm: MainViewModel, apVm: AppProfileViewModel, updateVm: UpdateViewModel) {
    val s        = LocalStrings.current
    val context  = LocalContext.current
    val activity = context as android.app.Activity

    var currentScreen          by remember { mutableStateOf(Screen.HOME) }
    var showReboot             by remember { mutableStateOf(false) }
    var showSettings           by remember { mutableStateOf(false) }
    var showLicense            by remember { mutableStateOf(false) }
    var licenseFromSettings    by remember { mutableStateOf(false) }

    var premiumCheckTick by remember { mutableStateOf(0) }
    val isPremium = remember(premiumCheckTick) {
        LicenseManager.isActive(context)
    }

    val updateState by updateVm.state.collectAsState()
    LaunchedEffect(updateState) {
        val state = updateState
        if (state is dev.aether.manager.update.UpdateState.Available) {
            dev.aether.manager.notification.NotificationHelper.showUpdateAvailable(
                context      = context,
                versionName  = state.info.versionName,
                releaseNotes = state.info.changelog
            )
        }
    }

    var showAdBlockDialog   by remember { mutableStateOf(false) }
    var adBlockCheckTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(adBlockCheckTrigger) {
        if (isPremium) {
            showAdBlockDialog = false
            return@LaunchedEffect
        }
        delay(1_500L)
        val detected = AdBlockChecker.isAdblockActive(context)
        if (detected) showAdBlockDialog = true
    }

    if (showAdBlockDialog && !isPremium) {
        AdBlockDetectedDialog(
            onDisableAds = {
                showAdBlockDialog = false
                try {
                    val intent = Intent(android.provider.Settings.ACTION_VPN_SETTINGS)
                    activity.startActivity(intent)
                } catch (_: Exception) {
                    val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                    activity.startActivity(intent)
                }
            }
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val licensed = LicenseManager.isActive(activity)
                    if (!licensed) {
                        InterstitialAdManager.preload(activity)
                        AdScheduler.start {
                            activity.takeUnless { it.isFinishing || it.isDestroyed }
                        }
                    } else {
                        AdScheduler.stop()
                    }
                    adBlockCheckTrigger++
                    premiumCheckTick++
                    LicenseNotificationChecker.check(activity)
                }
                Lifecycle.Event.ON_PAUSE   -> { /* biarkan scheduler tetap jalan */ }
                Lifecycle.Event.ON_STOP    -> AdScheduler.stop()
                Lifecycle.Event.ON_DESTROY -> AdScheduler.stop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    val snack by vm.snackMessage.collectAsState()
    LaunchedEffect(snack) {
        if (snack != null) {
            android.widget.Toast.makeText(context, snack, android.widget.Toast.LENGTH_SHORT).show()
            vm.clearSnack()
        }
    }

    val navItems = listOf(
        NavItem(Screen.HOME,  s.navHome,  Icons.Filled.Home,  Icons.Outlined.Home),
        NavItem(Screen.TWEAK, s.navTweak, Icons.Filled.Tune,  Icons.Outlined.Tune),
        NavItem(Screen.APPS,  s.navApps,  Icons.Filled.Apps,  Icons.Outlined.Apps),
    )

    if (showLicense) {
        dev.aether.manager.ui.license.LicenseScreen(onBack = {
            showLicense = false
            if (licenseFromSettings) {
                licenseFromSettings = false
                showSettings = true
            }
        })
        return
    }

    if (showSettings) {
        key(showSettings) {
            SettingsScreen(
                vm              = vm,
                apVm            = apVm,
                onBack          = { showSettings = false },
                onResetProfiles = {
                    apVm.resetAllProfiles()
                    vm.emitBackupSuccess("resetProfiles")
                },
                onResetMonitor  = {
                    apVm.resetMonitor()
                    vm.emitBackupSuccess("resetMonitor")
                },
                onOpenLicense   = { showSettings = false; licenseFromSettings = true; showLicense = true }
            )
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.systemBars
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally { it * dir / 4 } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally { -it * dir / 4 } + fadeOut(tween(150)))
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.HOME -> HomeScreen(vm)
                    Screen.TWEAK -> TweakScreen(vm)
                    Screen.APPS -> AppProfileScreen(apVm)
                }
            }

            FloatingBottomBar(
                navItems = navItems,
                currentScreen = currentScreen,
                onScreenChange = { currentScreen = it },
                onSettingsClick = { showSettings = true },
                onPowerClick = { showReboot = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
    if (showReboot) {
        RebootBottomSheet(
            onDismiss        = { showReboot = false },
            onReboot         = { vm.reboot(RootUtils.RebootMode.NORMAL) },
            onRebootRecovery = { vm.reboot(RootUtils.RebootMode.RECOVERY) },
            onReloadUI       = { vm.refresh() }
        )
    }

    UpdateDialogHost(viewModel = updateVm)
}
@Composable
private fun FloatingBottomBar(
    navItems: List<NavItem>,
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    onSettingsClick: () -> Unit,
    onPowerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        shadowElevation = 20.dp,
        tonalElevation = 8.dp,
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEachIndexed { idx, item ->
                val selected = currentScreen == item.screen
                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                val expanded = selected || pressed
                val width by animateDpAsState(
                    targetValue = if (expanded) 64.dp else 48.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "float_nav_width_$idx"
                )
                val scale by animateFloatAsState(
                    targetValue = if (pressed) 0.94f else 1f,
                    animationSpec = tween(140),
                    label = "float_nav_scale_$idx"
                )
                val bgColor by animateColorAsState(
                    targetValue = if (expanded) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    animationSpec = tween(220),
                    label = "float_nav_bg_$idx"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(220),
                    label = "float_nav_tint_$idx"
                )

                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .width(width)
                        .scale(scale)
                        .clip(RoundedCornerShape(50))
                        .background(bgColor)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onScreenChange(item.screen) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (expanded) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = iconTint,
                            modifier = Modifier.size(23.dp)
                        )
                        AnimatedVisibility(
                            visible = pressed,
                            enter = fadeIn(tween(120)) + expandHorizontally(tween(160)),
                            exit = fadeOut(tween(90)) + shrinkHorizontally(tween(120))
                        ) {
                            Text(
                                text = item.label.take(1),
                                modifier = Modifier.padding(start = 5.dp),
                                color = iconTint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            FloatingActionIcon(Icons.Outlined.Settings, onSettingsClick)
            FloatingPowerIcon(onPowerClick)
        }
    }
}

@Composable
private fun FloatingActionIcon(icon: ImageVector, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(140), label = "action_icon_scale")

    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(23.dp))
    }
}

@Composable
private fun FloatingPowerIcon(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(140), label = "power_icon_scale")

    Box(
        modifier = Modifier
            .size(54.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.PowerSettingsNew, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
    }
}
