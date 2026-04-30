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

    // Launcher untuk request POST_NOTIFICATIONS permission (Android 13+)
    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — silently continue */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request notifikasi permission di Android 13+ jika belum diberikan
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

    // ── Premium check: hanya lisensi ──────────────────────────────────────
    var premiumCheckTick by remember { mutableStateOf(0) }
    val isPremium = remember(premiumCheckTick) {
        LicenseManager.isActive(context)
    }

    // ── Notifikasi update di status bar saat ada versi baru ───────────────
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

    // ── AdBlock Detection ─────────────────────────────────────────────────
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

    // ── Ad Scheduler lifecycle ────────────────────────────────────────────
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
                    // ── Notifikasi lisensi (expired / hampir habis) ──────
                    LicenseNotificationChecker.check(activity)
                }
                // ON_PAUSE: JANGAN stop scheduler — ini terpanggil setiap kali
                // layar redup, notifikasi masuk, atau user alt-tab sebentar.
                // Kalau di-stop di sini, timer reset terus dan iklan tidak pernah muncul.
                Lifecycle.Event.ON_PAUSE   -> { /* biarkan scheduler tetap jalan */ }
                Lifecycle.Event.ON_STOP    -> AdScheduler.stop()  // baru stop kalau benar-benar background
                Lifecycle.Event.ON_DESTROY -> AdScheduler.stop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Iklan dikelola otomatis oleh AdScheduler (interval-based).
    // Tidak perlu trigger manual saat ganti tab — mengurangi spam.

    // ── Toast ─────────────────────────────────────────────────────────────
    val snack by vm.snackMessage.collectAsState()
    LaunchedEffect(snack) {
        if (snack != null) {
            android.widget.Toast.makeText(context, snack, android.widget.Toast.LENGTH_SHORT).show()
            vm.clearSnack()
        }
    }

    data class NavItem(
        val screen: Screen,
        val label: String,
        val selectedIcon: ImageVector,
        val unselectedIcon: ImageVector,
    )
    val navItems = listOf(
        NavItem(Screen.HOME,  s.navHome,  Icons.Filled.Home,  Icons.Outlined.Home),
        NavItem(Screen.TWEAK, s.navTweak, Icons.Filled.Tune,  Icons.Outlined.Tune),
        NavItem(Screen.APPS,  s.navApps,  Icons.Filled.Apps,  Icons.Outlined.Apps),
    )

    // ── LicenseScreen overlay ─────────────────────────────────────────────
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

    // ── SettingsScreen overlay ────────────────────────────────────────────
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

    // ── Main scaffold ─────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aether Manager", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                actions = {
                    IconButton(onClick = {
                        showSettings = true
                    }) {
                        Icon(Icons.Outlined.Settings, null, modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = { showReboot = true }) {
                        Icon(Icons.Outlined.RestartAlt, null, modifier = Modifier.size(24.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor      = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.height(56.dp),
            )
        },
        bottomBar = {
            // Floating pill navbar — icons only, no labels
            Box(
                modifier        = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape  = RoundedCornerShape(50),
                    color  = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 12.dp,
                    tonalElevation  = 4.dp,
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        navItems.forEachIndexed { idx, item ->
                            val selected = currentScreen == item.screen
                            val scale by animateFloatAsState(
                                if (selected) 1.1f else 1f,
                                spring(Spring.DampingRatioMediumBouncy), label = "pill_scale_$idx"
                            )
                            val bgColor by animateColorAsState(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                                tween(200), label = "pill_bg_$idx"
                            )
                            val iconTint by animateColorAsState(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                tween(200), label = "pill_tint_$idx"
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(bgColor)
                                    .then(
                                        Modifier.clickable(
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication        = null
                                        ) { currentScreen = item.screen }
                                    )
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                                    .scale(scale),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    tint     = iconTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
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
                    Screen.HOME  -> HomeScreen(vm)
                    Screen.TWEAK -> TweakScreen(vm)
                    Screen.APPS  -> AppProfileScreen(apVm)
                }
            }
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