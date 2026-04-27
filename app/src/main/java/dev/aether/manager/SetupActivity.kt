package dev.aether.manager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.aether.manager.i18n.AppStrings
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.i18n.ProvideStrings
import dev.aether.manager.ui.AetherTheme
import dev.aether.manager.util.RootManager
import kotlinx.coroutines.launch
import androidx.compose.ui.util.lerp

class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            AetherTheme {
                ProvideStrings {
                    SetupScreen(
                        onDone = { rootWasGranted ->
                            val prefs = getSharedPreferences("aether_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("setup_done", true).apply()
                            if (rootWasGranted) RootManager.markGranted()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

private enum class PermState { IDLE, CHECKING, GRANTED, DENIED }

private data class SetupPage(
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val title: String,
    val desc: String,
    val permissionType: String? = null,
    val ctaLabel: String? = null,
    val required: Boolean = true,
    // Info chips baru untuk menjelaskan detail permission
    val infoChips: List<Pair<ImageVector, String>> = emptyList(),
    val warningNote: String? = null,
)

// ── Animated Icon Box ──────────────────────────────────────────────────────────
@Composable
private fun AnimatedIconBox(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    granted: Boolean,
    modifier: Modifier = Modifier
) {
    val glowAlpha by animateFloatAsState(
        targetValue   = if (granted) 0.60f else 0.30f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label         = "glow_alpha"
    )
    val scale by animateFloatAsState(
        targetValue   = if (granted) 1.08f else 1f,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium),
        label         = "icon_scale"
    )
    val ringScale by animateFloatAsState(
        targetValue   = if (granted) 1.18f else 1f,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
        label         = "ring_scale"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(130.dp)) {
        // Outer glow ring
        Box(
            modifier = Modifier
                .size(126.dp)
                .scale(ringScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(iconTint.copy(alpha = glowAlpha * 0.3f), Color.Transparent)
                    )
                )
        )
        // Middle ring (border pulse)
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .border(
                    width = if (granted) 1.5.dp else 1.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            iconTint.copy(alpha = glowAlpha * 0.4f),
                            iconTint.copy(alpha = 0.05f),
                            iconTint.copy(alpha = glowAlpha * 0.4f),
                        )
                    ),
                    shape = CircleShape
                )
        )
        // Icon container
        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(scale)
                .clip(RoundedCornerShape(28.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = granted,
                transitionSpec = {
                    (scaleIn(spring(Spring.DampingRatioLowBouncy)) + fadeIn(tween(250))) togetherWith
                            (scaleOut(tween(150)) + fadeOut(tween(150)))
                },
                label = "icon_swap"
            ) { isGranted ->
                Icon(
                    imageVector        = if (isGranted) Icons.Outlined.CheckCircle else icon,
                    contentDescription = null,
                    tint               = if (isGranted) Color(0xFF4CAF50) else iconTint,
                    modifier           = Modifier.size(48.dp)
                )
            }
        }
    }
}

// ── Step Progress Bar ─────────────────────────────────────────────────────────
@Composable
private fun StepProgressBar(total: Int, current: Int, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { i ->
            val filled  = i <= current
            val isCurr  = i == current
            val progress by animateFloatAsState(
                targetValue   = if (filled) 1f else 0f,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                label         = "bar_$i"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (isCurr) 4.dp else 3.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            if (isCurr) Brush.horizontalGradient(
                                listOf(primary.copy(alpha = 0.6f), primary)
                            ) else Brush.horizontalGradient(listOf(primary, primary))
                        )
                )
            }
        }
    }
}

// ── Info Chip ─────────────────────────────────────────────────────────────────
@Composable
private fun InfoChip(icon: ImageVector, label: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(0.5.dp, accent.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(13.dp))
            Text(label, color = accent.copy(alpha = 0.9f), fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Warning Note ──────────────────────────────────────────────────────────────
@Composable
private fun WarningNote(text: String) {
    val color = MaterialTheme.colorScheme.error
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.07f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.ErrorOutline, null,
                tint = color,
                modifier = Modifier.size(15.dp).padding(top = 1.dp)
            )
            Text(
                text,
                color = color.copy(alpha = 0.85f),
                fontSize = 11.5.sp,
                lineHeight = 17.sp
            )
        }
    }
}

// ── Lifecycle Resume Observer ─────────────────────────────────────────────────
@Composable
private fun OnLifecycleResume(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

// ── Main Setup Screen ─────────────────────────────────────────────────────────
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SetupScreen(onDone: (rootWasGranted: Boolean) -> Unit) {
    val ctx     = LocalContext.current
    val scope   = rememberCoroutineScope()
    val s       = LocalStrings.current
    val density = LocalDensity.current

    var rootState    by remember { mutableStateOf(PermState.IDLE) }
    var notifState   by remember { mutableStateOf(PermState.IDLE) }
    var writeState   by remember { mutableStateOf(PermState.IDLE) }
    var storageState by remember { mutableStateOf(PermState.IDLE) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifState = if (granted) PermState.GRANTED else PermState.DENIED }

    val writeSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        writeState = if (android.provider.Settings.System.canWrite(ctx))
            PermState.GRANTED else PermState.DENIED
    }

    OnLifecycleResume {
        if (notifState != PermState.IDLE) {
            val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            else true
            if (granted) notifState = PermState.GRANTED
        }
        if (writeState != PermState.IDLE) {
            if (android.provider.Settings.System.canWrite(ctx)) writeState = PermState.GRANTED
        }
        if (storageState != PermState.IDLE) {
            val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) true
            else ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (ok) storageState = PermState.GRANTED
        }
    }

    val includeStorage = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

    val rootOk        = rootState    == PermState.GRANTED
    val notifOk       = notifState   == PermState.GRANTED || notifState   == PermState.DENIED
    val writeOk       = writeState   == PermState.GRANTED || writeState   == PermState.DENIED
    val storageOk     = !includeStorage || storageState == PermState.GRANTED || storageState == PermState.DENIED
    val allPermsGranted = rootOk && notifOk && writeOk && storageOk

    val primaryContainer   = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val errContainer       = MaterialTheme.colorScheme.errorContainer
    val onErrContainer     = MaterialTheme.colorScheme.onErrorContainer
    val secContainer       = MaterialTheme.colorScheme.secondaryContainer
    val onSecContainer     = MaterialTheme.colorScheme.onSecondaryContainer
    val terContainer       = MaterialTheme.colorScheme.tertiaryContainer
    val onTerContainer     = MaterialTheme.colorScheme.onTertiaryContainer

    val pages = buildList {
        // ── Welcome ──
        add(SetupPage(
            icon     = Icons.Outlined.Rocket,
            iconBg   = primaryContainer,
            iconTint = onPrimaryContainer,
            title    = s.setupWelcomeTitle,
            desc     = s.setupWelcomeDesc,
            infoChips = listOf(
                Icons.Outlined.Shield      to s.setupChipWelcomeMgr,
                Icons.Outlined.Speed       to s.setupChipWelcomePerf,
                Icons.Outlined.BugReport   to s.setupChipWelcomeRoot,
            )
        ))
        // ── Root ──
        add(SetupPage(
            icon           = Icons.Outlined.AdminPanelSettings,
            iconBg         = errContainer,
            iconTint       = onErrContainer,
            title          = s.setupRootTitle,
            desc           = s.setupRootDesc,
            permissionType = "ROOT",
            ctaLabel       = s.setupRootCta,
            required       = true,
            infoChips      = listOf(
                Icons.Outlined.Lock       to s.setupChipRootFull,
                Icons.Outlined.Terminal   to s.setupChipRootShell,
                Icons.Outlined.Security   to s.setupChipRootRequired,
            ),
            warningNote = s.setupWarnRoot
        ))
        // ── Notifikasi ──
        add(SetupPage(
            icon           = Icons.Outlined.Notifications,
            iconBg         = secContainer,
            iconTint       = onSecContainer,
            title          = s.setupNotifTitle,
            desc           = s.setupNotifDesc,
            permissionType = "NOTIFICATION",
            ctaLabel       = s.setupNotifCta,
            required       = false,
            infoChips      = listOf(
                Icons.Outlined.NotificationsActive to s.setupChipNotifStatus,
                Icons.Outlined.CheckCircle         to s.setupChipNotifConfirm,
                Icons.Outlined.Info                to s.setupChipOptional,
            )
        ))
        // ── Write Settings ──
        add(SetupPage(
            icon           = Icons.Outlined.Tune,
            iconBg         = terContainer,
            iconTint       = onTerContainer,
            title          = s.setupWriteTitle,
            desc           = s.setupWriteDesc,
            permissionType = "WRITE_SETTINGS",
            ctaLabel       = s.setupWriteCta,
            required       = false,
            infoChips      = listOf(
                Icons.Outlined.SettingsSuggest to s.setupChipWriteModify,
                Icons.Outlined.Bolt            to s.setupChipWriteFeatures,
                Icons.Outlined.Info            to s.setupChipOptional,
            ),
            warningNote = s.setupWarnWrite
        ))
        // ── Storage ── (API < 33 only)
        if (includeStorage) {
            add(SetupPage(
                icon           = Icons.Outlined.FolderOpen,
                iconBg         = secContainer,
                iconTint       = onSecContainer,
                title          = s.setupStorageTitle,
                desc           = s.setupStorageDesc,
                permissionType = "STORAGE",
                ctaLabel       = s.setupStorageCta,
                required       = false,
                infoChips      = listOf(
                    Icons.Outlined.Storage    to s.setupChipStorageRead,
                    Icons.Outlined.Download   to s.setupChipStorageExport,
                    Icons.Outlined.Info       to s.setupChipOptional,
                )
            ))
        }
        // ── Done placeholder ──
        add(SetupPage(
            icon     = Icons.Outlined.CheckCircle,
            iconBg   = Color.Transparent,
            iconTint = Color.Transparent,
            title    = "",
            desc     = ""
        ))
    }

    val pagerState  = rememberPagerState { pages.size }
    val currentPage = pagerState.currentPage
    val isLast      = currentPage == pages.size - 1

    LaunchedEffect(currentPage) {
        val pg = pages[currentPage]
        when (pg.permissionType) {
            "NOTIFICATION" -> {
                val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                            PackageManager.PERMISSION_GRANTED
                else true
                if (granted) notifState = PermState.GRANTED
            }
            "WRITE_SETTINGS" -> {
                if (android.provider.Settings.System.canWrite(ctx)) writeState = PermState.GRANTED
            }
            "STORAGE" -> {
                val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) true
                else ContextCompat.checkSelfPermission(
                    ctx, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                if (ok) storageState = PermState.GRANTED
            }
        }
    }

    val currentPageData = pages[currentPage]
    val permState = when (currentPageData.permissionType) {
        "ROOT"           -> rootState
        "NOTIFICATION"   -> notifState
        "WRITE_SETTINGS" -> writeState
        "STORAGE"        -> storageState
        else             -> PermState.IDLE
    }

    val canProceed = when {
        isLast -> rootState == PermState.GRANTED
        currentPageData.permissionType == null -> true
        currentPageData.required -> permState == PermState.GRANTED
        else -> permState == PermState.GRANTED || permState == PermState.DENIED
    }
    val swipeEnabled = canProceed || currentPageData.permissionType == null

    fun nextPage() { scope.launch { pagerState.animateScrollToPage(currentPage + 1) } }
    fun prevPage() { scope.launch { pagerState.animateScrollToPage(currentPage - 1) } }
    fun goToPage(idx: Int) { scope.launch { pagerState.animateScrollToPage(idx) } }

    val screenAlpha  = remember { Animatable(0f) }
    val screenSlideY = remember { Animatable(30f) }
    LaunchedEffect(Unit) {
        launch { screenAlpha.animateTo(1f, tween(420)) }
        launch { screenSlideY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .graphicsLayer(
                    alpha        = screenAlpha.value,
                    translationY = with(density) { screenSlideY.value.dp.toPx() }
                )
        ) {
            // Background gradient orb
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 80.dp, y = (-40).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-60).dp, y = 60.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Top: App brand + step progress ────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .padding(top = 36.dp)
                        .padding(bottom = 4.dp)
                ) {
                    // App label kecil
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Android, null,
                            tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            s.setupBrandLabel,
                            style         = MaterialTheme.typography.labelSmall,
                            color         = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            letterSpacing = 1.5.sp,
                            fontWeight    = FontWeight.SemiBold
                        )
                    }
                    AnimatedContent(
                        targetState = currentPage,
                        transitionSpec = {
                            if (targetState > initialState)
                                slideInVertically { -it } + fadeIn(tween(200)) togetherWith
                                        slideOutVertically { it } + fadeOut(tween(150))
                            else
                                slideInVertically { it } + fadeIn(tween(200)) togetherWith
                                        slideOutVertically { -it } + fadeOut(tween(150))
                        },
                        label = "step_counter"
                    ) { page ->
                        Text(
                            text          = s.setupStepOf.format(page + 1, pages.size),
                            style         = MaterialTheme.typography.labelSmall,
                            color         = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            letterSpacing = 0.8.sp
                        )
                    }
                    StepProgressBar(total = pages.size, current = currentPage)
                }

                // ── Pager ──────────────────────────────────────────────
                HorizontalPager(
                    state                   = pagerState,
                    pageSize                = PageSize.Fill,
                    beyondViewportPageCount = 1,
                    modifier                = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    userScrollEnabled        = swipeEnabled,
                    pageSpacing              = 0.dp,
                ) { idx ->
                    val pg = pages[idx]
                    val pagePermState = when (pg.permissionType) {
                        "ROOT"           -> rootState
                        "NOTIFICATION"   -> notifState
                        "WRITE_SETTINGS" -> writeState
                        "STORAGE"        -> storageState
                        else             -> PermState.IDLE
                    }
                    val isGrantedPage = pagePermState == PermState.GRANTED
                    val isLastPage    = idx == pages.size - 1

                    val effectivePg = if (isLastPage) pg.copy(
                        icon     = if (allPermsGranted) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                        iconBg   = if (allPermsGranted) Color(0xFF1B5E20).copy(alpha = 0.2f) else errContainer,
                        iconTint = if (allPermsGranted) Color(0xFF4CAF50) else onErrContainer,
                        title    = if (allPermsGranted) s.setupDoneTitle else s.setupIncompleteTitle,
                        desc     = if (allPermsGranted) s.setupDoneDesc  else s.setupIncompleteDesc
                    ) else pg

                    val pageOffset = (pagerState.currentPage - idx) + pagerState.currentPageOffsetFraction

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 26.dp)
                            .padding(top = 12.dp, bottom = 8.dp)
                            .graphicsLayer {
                                translationX = pageOffset * size.width * 0.22f
                                val absOffset = kotlin.math.abs(pageOffset)
                                alpha  = lerp(1f, 0f, (absOffset - 0.5f).coerceAtLeast(0f) * 2f)
                                scaleX = lerp(1f, 0.93f, absOffset.coerceIn(0f, 1f))
                                scaleY = lerp(1f, 0.93f, absOffset.coerceIn(0f, 1f))
                            }
                    ) {
                        // Icon
                        AnimatedIconBox(
                            icon     = effectivePg.icon,
                            iconBg   = effectivePg.iconBg,
                            iconTint = effectivePg.iconTint,
                            granted  = isGrantedPage || (isLastPage && allPermsGranted)
                        )

                        // Title + Desc
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnimatedContent(
                                targetState = effectivePg.title,
                                transitionSpec = {
                                    fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                                },
                                label = "title_$idx"
                            ) { title ->
                                Text(
                                    title,
                                    style      = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign  = TextAlign.Center,
                                    color      = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                effectivePg.desc,
                                style      = MaterialTheme.typography.bodyMedium,
                                textAlign  = TextAlign.Center,
                                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )
                        }

                        // Info Chips
                        if (effectivePg.infoChips.isNotEmpty() && !isLastPage) {
                            val chipColor = when (effectivePg.permissionType) {
                                "ROOT"           -> onErrContainer
                                "NOTIFICATION"   -> onSecContainer
                                "WRITE_SETTINGS" -> onTerContainer
                                "STORAGE"        -> onSecContainer
                                else             -> onPrimaryContainer
                            }
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                    verticalArrangement   = Arrangement.spacedBy(6.dp),
                                    modifier              = Modifier.fillMaxWidth()
                                ) {
                                    effectivePg.infoChips.forEach { (icon, label) ->
                                        InfoChip(icon = icon, label = label, accent = chipColor)
                                    }
                                }
                            }
                        }

                        // Warning note (jika ada)
                        if (effectivePg.warningNote != null && pagePermState != PermState.GRANTED) {
                            AnimatedVisibility(
                                visible = true,
                                enter   = fadeIn(tween(400)) + slideInVertically { it / 3 }
                            ) {
                                WarningNote(effectivePg.warningNote)
                            }
                        }

                        // All Granted Badge (last page)
                        AnimatedVisibility(
                            visible = isLastPage && allPermsGranted,
                            enter   = scaleIn(spring(Spring.DampingRatioLowBouncy)) + fadeIn(),
                            exit    = scaleOut() + fadeOut()
                        ) {
                            AllGrantedBadge()
                        }

                        // Missing Perms Summary (last page)
                        AnimatedVisibility(
                            visible = isLastPage && !allPermsGranted,
                            enter   = slideInVertically { it / 2 } + fadeIn(),
                            exit    = slideOutVertically { it / 2 } + fadeOut()
                        ) {
                            MissingPermsSummary(
                                rootMissing    = rootState != PermState.GRANTED,
                                notifMissing   = notifState == PermState.IDLE || notifState == PermState.CHECKING,
                                writeMissing   = writeState == PermState.IDLE || writeState == PermState.CHECKING,
                                storageMissing = includeStorage &&
                                                 (storageState == PermState.IDLE || storageState == PermState.CHECKING),
                                strings        = s,
                                onGoToPage     = { targetIdx -> goToPage(targetIdx) }
                            )
                        }

                        // Permission Action Block
                        if (effectivePg.permissionType != null) {
                            PermissionBlock(
                                permType = effectivePg.permissionType,
                                ctaLabel = effectivePg.ctaLabel ?: s.setupBtnRetry,
                                state    = pagePermState,
                                strings  = s,
                                canSkip  = !effectivePg.required,
                                onSkip   = {
                                    when (effectivePg.permissionType) {
                                        "NOTIFICATION"   -> notifState   = PermState.DENIED
                                        "WRITE_SETTINGS" -> writeState   = PermState.DENIED
                                        "STORAGE"        -> storageState = PermState.DENIED
                                        else             -> Unit
                                    }
                                    nextPage()
                                },
                                onAction = {
                                    when (effectivePg.permissionType) {
                                        "ROOT" -> scope.launch {
                                            rootState = PermState.CHECKING
                                            val granted = RootManager.requestRoot()
                                            rootState = if (granted) PermState.GRANTED else PermState.DENIED
                                        }
                                        "NOTIFICATION" -> {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            else notifState = PermState.GRANTED
                                        }
                                        "WRITE_SETTINGS" -> {
                                            if (android.provider.Settings.System.canWrite(ctx)) {
                                                writeState = PermState.GRANTED
                                            } else {
                                                writeSettingsLauncher.launch(Intent(
                                                    android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                                    android.net.Uri.parse("package:${ctx.packageName}")
                                                ))
                                            }
                                        }
                                        "STORAGE" -> {
                                            storageState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                PermState.GRANTED
                                            } else {
                                                if (ContextCompat.checkSelfPermission(
                                                        ctx, Manifest.permission.READ_EXTERNAL_STORAGE
                                                    ) == PackageManager.PERMISSION_GRANTED
                                                ) PermState.GRANTED else PermState.DENIED
                                            }
                                        }
                                    }
                                },
                                onRetry = {
                                    when (effectivePg.permissionType) {
                                        "ROOT"           -> { RootManager.clearCache(); rootState = PermState.IDLE }
                                        "WRITE_SETTINGS" -> writeState   = PermState.IDLE
                                        "NOTIFICATION"   -> notifState   = PermState.IDLE
                                        "STORAGE"        -> storageState = PermState.IDLE
                                    }
                                }
                            )
                        }
                    }
                }

                // ── Bottom Controls ────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(horizontal = 26.dp)
                        .padding(bottom = 32.dp)
                ) {
                    val btnScale = remember { Animatable(1f) }
                    LaunchedEffect(canProceed) {
                        if (canProceed) {
                            btnScale.animateTo(1.04f, tween(150))
                            btnScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy))
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                btnScale.animateTo(0.96f, tween(80))
                                btnScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy))
                            }
                            if (isLast) onDone(rootState == PermState.GRANTED)
                            else nextPage()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .scale(btnScale.value),
                        shape   = RoundedCornerShape(18.dp),
                        enabled = canProceed,
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        )
                    ) {
                        AnimatedContent(
                            targetState = if (isLast) s.setupBtnStart else s.setupBtnNext,
                            transitionSpec = {
                                slideInVertically { -it } + fadeIn() togetherWith
                                        slideOutVertically { it } + fadeOut()
                            },
                            label = "btn_label"
                        ) { label ->
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                if (!isLast) {
                                    Icon(
                                        Icons.Outlined.ChevronRight, null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Outlined.RocketLaunch, null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Error hint kalau required belum granted
                    AnimatedVisibility(
                        visible = !canProceed &&
                                currentPageData.permissionType != null &&
                                currentPageData.required
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Lock, null,
                                tint     = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                s.setupRootRequired,
                                color     = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                fontSize  = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        AnimatedVisibility(
                            visible = currentPage > 0,
                            enter   = fadeIn() + slideInHorizontally { -it / 2 },
                            exit    = fadeOut() + slideOutHorizontally { -it / 2 }
                        ) {
                            TextButton(onClick = { prevPage() }) {
                                Icon(Icons.Outlined.ChevronLeft, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(s.setupBtnBack, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── All Granted Badge ─────────────────────────────────────────────────────────
@Composable
private fun AllGrantedBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "badge_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.6f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label         = "badge_alpha"
    )
    Surface(
        shape  = RoundedCornerShape(50),
        color  = Color(0xFF4CAF50).copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = glowAlpha * 0.45f))
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.CheckCircle, null,
                tint     = Color(0xFF4CAF50),
                modifier = Modifier.size(17.dp)
            )
            Text(
                text          = LocalStrings.current.setupAllPermsGranted,
                fontSize      = 13.sp,
                color         = Color(0xFF4CAF50).copy(alpha = glowAlpha),
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// ── Missing Perms Summary ─────────────────────────────────────────────────────
@Composable
private fun MissingPermsSummary(
    rootMissing: Boolean,
    notifMissing: Boolean,
    writeMissing: Boolean,
    storageMissing: Boolean,
    strings: AppStrings,
    onGoToPage: (Int) -> Unit
) {
    var pageIdx      = 1
    val rootPageIdx    = pageIdx++
    val notifPageIdx   = pageIdx++
    val writePageIdx   = pageIdx++
    val storagePageIdx = if (storageMissing) pageIdx else -1

    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    Icons.Outlined.Warning, null,
                    tint     = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    strings.setupIncompleteTitle,
                    color      = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp
                )
            }
            Text(
                strings.setupMissingTapHint,
                color    = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
            Spacer(Modifier.height(2.dp))
            if (rootMissing)    MissingPermRow(strings.setupRootTitle,    rootPageIdx,    onGoToPage)
            if (notifMissing)   MissingPermRow(strings.setupNotifTitle,   notifPageIdx,   onGoToPage)
            if (writeMissing)   MissingPermRow(strings.setupWriteTitle,   writePageIdx,   onGoToPage)
            if (storageMissing && storagePageIdx >= 0)
                                MissingPermRow(strings.setupStorageTitle, storagePageIdx, onGoToPage)
        }
    }
}

@Composable
private fun MissingPermRow(label: String, pageIdx: Int, onGoToPage: (Int) -> Unit) {
    TextButton(
        onClick        = { onGoToPage(pageIdx) },
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(
            Icons.Outlined.ChevronRight, null,
            tint     = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
    }
}

// ── Permission Block ──────────────────────────────────────────────────────────
@Composable
private fun PermissionBlock(
    permType: String,
    ctaLabel: String,
    state: PermState,
    strings: AppStrings,
    canSkip: Boolean,
    onSkip: () -> Unit,
    onAction: () -> Unit,
    onRetry: () -> Unit
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            (slideInVertically { it / 2 } + fadeIn(tween(300))) togetherWith
                    (slideOutVertically { -it / 2 } + fadeOut(tween(200)))
        },
        label = "perm_$permType"
    ) { s ->
        when (s) {
            PermState.IDLE -> {
                val btnGlow = remember { Animatable(0.5f) }
                LaunchedEffect(Unit) {
                    while (true) {
                        btnGlow.animateTo(1f, tween(1000))
                        btnGlow.animateTo(0.5f, tween(1000))
                    }
                }
                val primary = MaterialTheme.colorScheme.primary
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(primary.copy(alpha = btnGlow.value * 0.10f))
                        )
                        FilledTonalButton(
                            onClick   = onAction,
                            modifier  = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape     = RoundedCornerShape(18.dp),
                            elevation = ButtonDefaults.filledTonalButtonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(
                                when (permType) {
                                    "ROOT"           -> Icons.Outlined.AdminPanelSettings
                                    "NOTIFICATION"   -> Icons.Outlined.Notifications
                                    "WRITE_SETTINGS" -> Icons.Outlined.Tune
                                    else             -> Icons.Outlined.FolderOpen
                                }, null, modifier = Modifier.size(19.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(ctaLabel, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                    if (canSkip) {
                        TextButton(
                            onClick  = onSkip,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                strings.setupBtnSkip,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            PermState.CHECKING -> {
                Surface(
                    shape    = RoundedCornerShape(18.dp),
                    color    = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            strings.setupRootChecking,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            PermState.GRANTED -> {
                val scaleAnim = remember { Animatable(0.7f) }
                LaunchedEffect(Unit) {
                    scaleAnim.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium))
                }
                Surface(
                    shape    = RoundedCornerShape(18.dp),
                    color    = Color(0xFF1B5E20).copy(alpha = 0.10f),
                    border   = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.30f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(scaleAnim.value)
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle, null,
                            tint     = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                when (permType) {
                                    "ROOT"           -> strings.setupRootGranted
                                    "NOTIFICATION"   -> strings.setupNotifGranted
                                    "WRITE_SETTINGS" -> strings.setupWriteGranted
                                    else             -> strings.setupStorageGranted
                                },
                                color      = Color(0xFF4CAF50),
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 15.sp
                            )
                            Text(
                                strings.setupGrantedSub,
                                color    = Color(0xFF4CAF50).copy(alpha = 0.6f),
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }

            PermState.DENIED -> {
                Surface(
                    shape    = RoundedCornerShape(18.dp),
                    color    = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f),
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Warning, null,
                            tint     = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                when (permType) {
                                    "ROOT"           -> strings.setupRootDenied
                                    "NOTIFICATION"   -> strings.setupNotifDenied
                                    "WRITE_SETTINGS" -> strings.setupWriteDenied
                                    else             -> strings.setupStorageDenied
                                },
                                color      = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 14.sp
                            )
                            val sub = when (permType) {
                                "ROOT"           -> strings.setupRootDeniedSub
                                "NOTIFICATION"   -> strings.setupWriteDeniedSub
                                "WRITE_SETTINGS" -> strings.setupWriteDeniedSub
                                "STORAGE"        -> strings.setupWriteDeniedSub
                                else             -> null
                            }
                            if (sub != null) {
                                Text(
                                    sub,
                                    color      = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.65f),
                                    fontSize   = 11.5.sp,
                                    lineHeight = 16.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick        = onRetry,
                                    modifier       = Modifier.height(36.dp),
                                    shape          = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    border         = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.35f)
                                    )
                                ) {
                                    Icon(
                                        Icons.Outlined.Refresh, null,
                                        modifier = Modifier.size(13.dp),
                                        tint     = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        strings.setupBtnRetry,
                                        fontSize = 12.sp,
                                        color    = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
