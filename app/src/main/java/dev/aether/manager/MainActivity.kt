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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import dev.aether.manager.util.RootEngine

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

    var bottomNavVisible by remember { mutableStateOf(true) }
    val scrollAwareNavConnection = remember {
        object : NestedScrollConnection {
            private var scrollBuffer = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y

                when {
                    dy < -3f -> {
                        scrollBuffer = (scrollBuffer + dy).coerceAtLeast(-64f)
                        if (scrollBuffer <= -18f) {
                            bottomNavVisible = false
                            scrollBuffer = 0f
                        }
                    }
                    dy > 3f -> {
                        scrollBuffer = (scrollBuffer + dy).coerceAtMost(64f)
                        if (scrollBuffer >= 12f) {
                            bottomNavVisible = true
                            scrollBuffer = 0f
                        }
                    }
                    else -> Unit
                }

                return Offset.Zero
            }
        }
    }

    LaunchedEffect(currentScreen) {
        bottomNavVisible = true
    }

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
                .nestedScroll(scrollAwareNavConnection)
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
                    Screen.TWEAK -> TweakScreen(
                        vm = vm,
                        onOpenAppProfile = { currentScreen = Screen.APPS }
                    )
                    Screen.APPS -> AppProfileScreen(apVm)
                }
            }

            FloatingBottomCluster(
                navItems = navItems,
                currentScreen = currentScreen,
                visible = bottomNavVisible,
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
            onReboot         = { vm.reboot(RootEngine.RebootMode.NORMAL) },
            onRebootRecovery = { vm.reboot(RootEngine.RebootMode.RECOVERY) },
            onReloadUI       = { vm.refresh() }
        )
    }

    UpdateDialogHost(viewModel = updateVm)
}
// ─────────────────────────────────────────────────────────────────────────────
// Bottom nav cluster — Settings/Reboot dipisah dari tab utama
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FloatingBottomCluster(
    navItems: List<NavItem>,
    currentScreen: Screen,
    visible: Boolean,
    onScreenChange: (Screen) -> Unit,
    onSettingsClick: () -> Unit,
    onPowerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isNavSliding by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            initialOffsetY = { it + 28 }
        ) + fadeIn(tween(150, easing = FastOutSlowInEasing)),
        exit = slideOutVertically(
            animationSpec = tween(190, easing = FastOutSlowInEasing),
            targetOffsetY = { it + 28 }
        ) + fadeOut(tween(110, easing = FastOutSlowInEasing)),
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FloatingBottomBar(
                navItems = navItems,
                currentScreen = currentScreen,
                onScreenChange = onScreenChange,
                onSlidingChange = { isNavSliding = it }
            )

            AnimatedVisibility(
                visible = !isNavSliding,
                enter = fadeIn(tween(120, easing = FastOutSlowInEasing)) + expandHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    expandFrom = Alignment.Start
                ),
                exit = shrinkHorizontally(
                    animationSpec = tween(110, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Start
                ) + fadeOut(tween(90, easing = FastOutSlowInEasing))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(10.dp))

                    FloatingUtilityBar(
                        onSettingsClick = onSettingsClick,
                        onPowerClick = onPowerClick
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingUtilityBar(
    onSettingsClick: () -> Unit,
    onPowerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        shadowElevation = 16.dp,
        tonalElevation = 7.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionIcon(Icons.Outlined.Settings, onSettingsClick)
            FloatingPowerIcon(onPowerClick)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FloatingBottomBar — iOS 26-style hold-to-expand + drag-to-switch capsule
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FloatingBottomBar(
    navItems: List<NavItem>,
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    onSlidingChange: (Boolean) -> Unit = {}
) {
    // ── Expansion state ───────────────────────────────────────────────────────
    var expanded by remember { mutableStateOf(false) }
    // Drag tracking dibuat stabil: pointerInput tidak boleh restart saat expanded/currentScreen berubah.
    var dragAccum by remember { mutableStateOf(0f) }
    var dragIndex by remember { mutableIntStateOf(navItems.indexOfFirst { it.screen == currentScreen }.coerceAtLeast(0)) }
    val latestScreen by rememberUpdatedState(currentScreen)
    val latestOnScreenChange by rememberUpdatedState(onScreenChange)

    // Keep dragIndex in sync when screen changes externally
    LaunchedEffect(currentScreen) {
        if (!expanded) dragIndex = navItems.indexOfFirst { it.screen == currentScreen }.coerceAtLeast(0)
    }

    // ── Animation values ──────────────────────────────────────────────────────
    val expandSpec = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
    val collapseSpec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    val expandProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = if (expanded) expandSpec else collapseSpec,
        label = "capsule_expand"
    )

    // Outer capsule vertical scale — subtle squish on long-press start
    val capsuleScale by animateFloatAsState(
        targetValue = if (expanded) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "capsule_scale"
    )

    // ── Drag threshold per item (px → will be set after layout) ──────────────
    val itemWidthPx = remember { mutableStateOf(0f) }

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        shadowElevation = 20.dp,
        tonalElevation = 8.dp,
        modifier = modifier
            .scale(scaleX = 1f, scaleY = capsuleScale)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Nav items ─────────────────────────────────────────────────────
            navItems.forEachIndexed { idx, item ->
                val isSelected = currentScreen == item.screen
                val isHighlighted = expanded && dragIndex == idx

                // Item width: expanded shows label, collapsed is icon-only
                val itemWidth by animateDpAsState(
                    targetValue = when {
                        expanded        -> 76.dp   // icon + label
                        isSelected      -> 62.dp   // selected pill
                        else            -> 48.dp
                    },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                    label = "item_width_$idx"
                )

                val bgAlpha by animateFloatAsState(
                    targetValue = when {
                        isHighlighted -> 1f
                        !expanded && isSelected -> 1f
                        else -> 0f
                    },
                    animationSpec = tween(200),
                    label = "item_bg_$idx"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (isHighlighted || (!expanded && isSelected))
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "item_tint_$idx"
                )
                val bgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = bgAlpha)

                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .width(itemWidth)
                        .onGloballyPositioned { coords ->
                            itemWidthPx.value = coords.size.width.toFloat().coerceAtLeast(1f)
                        }
                        .clip(RoundedCornerShape(50))
                        .background(bgColor)
                        // Short tap → navigate
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!expanded) {
                                onScreenChange(item.screen)
                                dragIndex = idx
                            }
                        }
                        // Long press → expand capsule; drag → switch tabs
                        .pointerInput(navItems) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { _ ->
                                    expanded = true
                                    onSlidingChange(true)
                                    dragIndex = navItems.indexOfFirst { it.screen == latestScreen }.coerceAtLeast(0)
                                    dragAccum = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragAccum += dragAmount.x

                                    val threshold = (itemWidthPx.value * 0.72f).coerceAtLeast(42f)
                                    val steps = (dragAccum / threshold).toInt()
                                    if (steps != 0) {
                                        val newIdx = (dragIndex + steps).coerceIn(0, navItems.lastIndex)
                                        if (newIdx != dragIndex) {
                                            dragIndex = newIdx
                                            latestOnScreenChange(navItems[newIdx].screen)
                                        }
                                        dragAccum -= steps * threshold
                                    }
                                },
                                onDragEnd = {
                                    latestOnScreenChange(navItems[dragIndex].screen)
                                    expanded = false
                                    onSlidingChange(false)
                                    dragAccum = 0f
                                },
                                onDragCancel = {
                                    expanded = false
                                    onSlidingChange(false)
                                    dragAccum = 0f
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isHighlighted || (!expanded && isSelected)) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        // Label — slides in when expanded
                        AnimatedVisibility(
                            visible = expandProgress > 0.4f,
                            enter = fadeIn(tween(120)) + expandHorizontally(
                                animationSpec = tween(180),
                                expandFrom = Alignment.Start
                            ),
                            exit = shrinkHorizontally(
                                animationSpec = tween(120),
                                shrinkTowards = Alignment.Start
                            ) + fadeOut(tween(80))
                        ) {
                            Text(
                                text = item.label,
                                modifier = Modifier.padding(start = 5.dp),
                                color = iconTint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingActionIcon(icon: ImageVector, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.90f else 1f, tween(140), label = "action_icon_scale")

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
    val scale by animateFloatAsState(if (pressed) 0.90f else 1f, tween(140), label = "power_icon_scale")

    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.PowerSettingsNew, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
    }
}
