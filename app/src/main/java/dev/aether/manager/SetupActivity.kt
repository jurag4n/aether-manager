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
                            // FIX #4: hanya simpan setup_done dan markGranted
                            // kalau root memang sudah granted (divalidasi dari caller)
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
    // FIX #6: flag apakah permission ini wajib atau bisa di-skip
    val required: Boolean = true,
)

// ── Icon box ──────────────────────────────────────────────────────────────────
@Composable
private fun AnimatedIconBox(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    granted: Boolean,
    modifier: Modifier = Modifier
) {
    val glowAlpha by animateFloatAsState(
        targetValue   = if (granted) 0.55f else 0.28f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label         = "glow_alpha"
    )
    val scale by animateFloatAsState(
        targetValue   = if (granted) 1.07f else 1f,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium),
        label         = "icon_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(120.dp)
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(iconTint.copy(alpha = glowAlpha * 0.45f), Color.Transparent)
                    )
                )
        )
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

// ── Step progress indicator ───────────────────────────────────────────────────
@Composable
private fun StepProgressBar(total: Int, current: Int, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                    .height(3.dp)
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
                                listOf(primary.copy(alpha = 0.5f), primary)
                            ) else Brush.horizontalGradient(listOf(primary, primary))
                        )
                )
            }
        }
    }
}

// ── Lifecycle resume observer ─────────────────────────────────────────────────
// FIX #7: recheck permission saat app di-resume dari Settings
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
// FIX #4: onDone sekarang menerima rootWasGranted: Boolean
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

    // FIX #7: recheck permission on resume (user balik dari Settings)
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

    // "Selesai" = root wajib granted + permission opsional sudah ada keputusan
    // (GRANTED atau DENIED/skip — yang penting bukan IDLE/CHECKING)
    val rootOk    = rootState    == PermState.GRANTED
    val notifOk   = notifState   == PermState.GRANTED || notifState   == PermState.DENIED
    val writeOk   = writeState   == PermState.GRANTED || writeState   == PermState.DENIED
    val storageOk = !includeStorage ||
                    storageState == PermState.GRANTED || storageState == PermState.DENIED
    val allPermsGranted = rootOk && notifOk && writeOk && storageOk

    val primaryContainer   = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val errContainer       = MaterialTheme.colorScheme.errorContainer
    val onErrContainer     = MaterialTheme.colorScheme.onErrorContainer
    val secContainer       = MaterialTheme.colorScheme.secondaryContainer
    val onSecContainer     = MaterialTheme.colorScheme.onSecondaryContainer
    val terContainer       = MaterialTheme.colorScheme.tertiaryContainer
    val onTerContainer     = MaterialTheme.colorScheme.onTertiaryContainer

    // FIX #3: Storage page di-skip di API >= 33 karena READ_EXTERNAL_STORAGE sudah dicabut
    val includeStorage = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

    val pages = buildList {
        add(SetupPage(
            icon = Icons.Outlined.Rocket, iconBg = primaryContainer, iconTint = onPrimaryContainer,
            title = s.setupWelcomeTitle, desc = s.setupWelcomeDesc
        ))
        add(SetupPage(
            icon = Icons.Outlined.AdminPanelSettings, iconBg = errContainer, iconTint = onErrContainer,
            title = s.setupRootTitle, desc = s.setupRootDesc,
            permissionType = "ROOT", ctaLabel = s.setupRootCta,
            required = true
        ))
        add(SetupPage(
            icon = Icons.Outlined.Notifications, iconBg = secContainer, iconTint = onSecContainer,
            title = s.setupNotifTitle, desc = s.setupNotifDesc,
            permissionType = "NOTIFICATION", ctaLabel = s.setupNotifCta,
            required = false
        ))
        add(SetupPage(
            icon = Icons.Outlined.Tune, iconBg = terContainer, iconTint = onTerContainer,
            title = s.setupWriteTitle, desc = s.setupWriteDesc,
            permissionType = "WRITE_SETTINGS", ctaLabel = s.setupWriteCta,
            required = false
        ))
        if (includeStorage) {
            add(SetupPage(
                icon = Icons.Outlined.FolderOpen, iconBg = secContainer, iconTint = onSecContainer,
                title = s.setupStorageTitle, desc = s.setupStorageDesc,
                permissionType = "STORAGE", ctaLabel = s.setupStorageCta,
                required = false
            ))
        }
        // FIX Bug 2: last page pakai placeholder — icon/title/desc-nya ditentukan
        // secara langsung di dalam pager berdasarkan allPermsGranted yang reaktif,
        // bukan dari pages list yang di-build sekali saat composable pertama jalan
        add(SetupPage(
            icon     = Icons.Outlined.CheckCircle, // placeholder, override di pager
            iconBg   = Color.Transparent,
            iconTint = Color.Transparent,
            title    = "",
            desc     = ""
        ))
    }

    val pagerState  = rememberPagerState { pages.size }
    val currentPage = pagerState.currentPage
    val isLast      = currentPage == pages.size - 1

    // Auto-check saat landing di halaman permission
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

    // FIX #2: canProceed mempertimbangkan required vs opsional
    // Required: harus GRANTED. Opsional: GRANTED atau DENIED (sudah keputusan user) bisa lanjut
    val canProceed = when {
        isLast -> rootState == PermState.GRANTED // last page hanya butuh root
        currentPageData.permissionType == null -> true
        currentPageData.required -> permState == PermState.GRANTED
        else -> permState == PermState.GRANTED || permState == PermState.DENIED
    }

    // FIX #2: swipe hanya diizinkan kalau canProceed atau tidak ada permission di page ini
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
                    alpha = screenAlpha.value,
                    // FIX #1: convert dp → px via density, bukan .dp.value double-convert
                    translationY = with(density) { screenSlideY.value.dp.toPx() }
                )
        ) {
            // Background glow
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 60.dp, y = (-20).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
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
                // ── Top: step progress ─────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .padding(top = 36.dp)
                        .padding(bottom = 4.dp)
                ) {
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
                            text          = "Setup  ${page + 1} / ${pages.size}",
                            style         = MaterialTheme.typography.labelSmall,
                            color         = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            letterSpacing = 1.sp
                        )
                    }
                    StepProgressBar(total = pages.size, current = currentPage)
                }

                // ── Pager ──────────────────────────────────────────────
                HorizontalPager(
                    state = pagerState,
                    pageSize = PageSize.Fill,
                    beyondViewportPageCount = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    // FIX #2: swipe dikunci kalau permission required belum granted
                    userScrollEnabled = swipeEnabled,
                    pageSpacing = 0.dp,
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

                    // FIX Bug 2: last page icon/title/desc dihitung langsung
                    // dari allPermsGranted yang reaktif, bukan dari pages[] yang stale
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
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 28.dp)
                            .padding(top = 16.dp, bottom = 8.dp)
                            .graphicsLayer {
                                translationX = pageOffset * size.width * 0.22f
                                val absOffset = kotlin.math.abs(pageOffset)
                                alpha  = lerp(1f, 0f, (absOffset - 0.5f).coerceAtLeast(0f) * 2f)
                                scaleX = lerp(1f, 0.93f, absOffset.coerceIn(0f, 1f))
                                scaleY = lerp(1f, 0.93f, absOffset.coerceIn(0f, 1f))
                            }
                    ) {
                        AnimatedIconBox(
                            icon     = effectivePg.icon,
                            iconBg   = effectivePg.iconBg,
                            iconTint = effectivePg.iconTint,
                            granted  = isGrantedPage || (isLastPage && allPermsGranted)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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

                        AnimatedVisibility(
                            visible = isLastPage && allPermsGranted,
                            enter   = scaleIn(spring(Spring.DampingRatioLowBouncy)) + fadeIn(),
                            exit    = scaleOut() + fadeOut()
                        ) {
                            AllGrantedBadge()
                        }

                        AnimatedVisibility(
                            visible = isLastPage && !allPermsGranted,
                            enter   = slideInVertically { it / 2 } + fadeIn(),
                            exit    = slideOutVertically { it / 2 } + fadeOut()
                        ) {
                            MissingPermsSummary(
                                // Hanya tampil sebagai "missing" kalau benar-benar belum diputuskan
                                // DENIED = user skip = bukan missing, hanya root yang wajib
                                rootMissing    = rootState != PermState.GRANTED,
                                notifMissing   = notifState == PermState.IDLE || notifState == PermState.CHECKING,
                                writeMissing   = writeState == PermState.IDLE || writeState == PermState.CHECKING,
                                storageMissing = includeStorage &&
                                                 (storageState == PermState.IDLE || storageState == PermState.CHECKING),
                                strings        = s,
                                onGoToPage     = { targetIdx -> goToPage(targetIdx) }
                            )
                        }

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

                // ── Bottom controls ────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .padding(horizontal = 28.dp)
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
                            // FIX #4: pass root state ke onDone
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
                            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }

                    // Pesan error — hanya tampil kalau permission required dan belum granted
                    AnimatedVisibility(
                        visible = !canProceed &&
                                currentPageData.permissionType != null &&
                                currentPageData.required
                    ) {
                        Text(
                            s.setupRootRequired,
                            color     = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            fontSize  = 12.sp,
                            textAlign = TextAlign.Center
                        )
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

// ── All granted badge ─────────────────────────────────────────────────────────
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
        color  = Color(0xFF4CAF50).copy(alpha = 0.15f),
        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = glowAlpha * 0.4f))
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
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

// ── Missing perms summary ─────────────────────────────────────────────────────
@Composable
private fun MissingPermsSummary(
    rootMissing: Boolean,
    notifMissing: Boolean,
    writeMissing: Boolean,
    storageMissing: Boolean,
    strings: AppStrings,
    onGoToPage: (Int) -> Unit
) {
    // FIX #8: hitung page index dari list yang aktual bukan hardcoded
    // Page 0 = welcome, 1 = root, 2 = notif, 3 = write, 4 = storage (opsional), last = done
    var pageIdx = 1 // mulai dari root page
    val rootPageIdx    = pageIdx++
    val notifPageIdx   = pageIdx++
    val writePageIdx   = pageIdx++
    val storagePageIdx = if (storageMissing) pageIdx else -1

    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Warning, null,
                    tint     = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp)
                )
                // FIX #8: gunakan string yang lebih generik untuk judul summary
                Text(
                    strings.setupIncompleteTitle,
                    color      = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp
                )
            }
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

// ── Permission block ──────────────────────────────────────────────────────────
@Composable
private fun PermissionBlock(
    permType: String,
    ctaLabel: String,
    state: PermState,
    strings: AppStrings,
    canSkip: Boolean,      // FIX #6
    onSkip: () -> Unit,    // FIX #6
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
                // Animasi glow infinite untuk tombol CTA
                val btnGlow = remember { Animatable(0.5f) }
                LaunchedEffect(Unit) {
                    while (true) {
                        btnGlow.animateTo(1f, tween(1000))
                        btnGlow.animateTo(0.5f, tween(1000))
                    }
                }
                val primary = MaterialTheme.colorScheme.primary
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // FIX #5: hapus Modifier.blur() pada glow layer button,
                    // ganti ke alpha-only background — lebih ringan, tidak per-frame costly
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(primary.copy(alpha = btnGlow.value * 0.12f))
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

                    // FIX #6: skip button untuk permission opsional
                    if (canSkip) {
                        TextButton(
                            onClick  = onSkip,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                strings.setupBtnSkip,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            PermState.CHECKING -> {
                val infiniteTransition = rememberInfiniteTransition(label = "checking")
                val dotAlpha1 by infiniteTransition.animateFloat(0f, 1f,
                    infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "d1")
                val dotAlpha2 by infiniteTransition.animateFloat(0f, 1f,
                    infiniteRepeatable(tween(400, delayMillis = 133), RepeatMode.Reverse), label = "d2")
                val dotAlpha3 by infiniteTransition.animateFloat(0f, 1f,
                    infiniteRepeatable(tween(400, delayMillis = 266), RepeatMode.Reverse), label = "d3")

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
                        Spacer(Modifier.width(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            listOf(dotAlpha1, dotAlpha2, dotAlpha3).forEach { alpha ->
                                Text(
                                    "•",
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                                    fontSize = 16.sp
                                )
                            }
                        }
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
                    color    = Color(0xFF1B5E20).copy(alpha = 0.12f),
                    border   = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.35f)),
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
                    }
                }
            }

            PermState.DENIED -> {
                Surface(
                    shape    = RoundedCornerShape(18.dp),
                    color    = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
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
                                    color      = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
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
                                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.4f)
                                    )
                                ) {
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
