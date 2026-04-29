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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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

// ─────────────────────────────────────────────────────────────────────────────
// Data / State
// ─────────────────────────────────────────────────────────────────────────────

private enum class PermState { IDLE, CHECKING, GRANTED, DENIED }

/** Feature card shown on the welcome splash page */
private data class FeatureItem(
    val icon: ImageVector,
    val title: String,
    val desc: String,
)

/** Permission card shown on the permissions page */
private data class PermItem(
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val permissionType: String,
    val required: Boolean = true,
)

// ─────────────────────────────────────────────────────────────────────────────
// Dot pager indicator — mirip foto (pill aktif, bulat kecil sisanya)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PagerDotIndicator(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { i ->
            val isActive = i == current
            val width by animateDpAsState(
                targetValue   = if (isActive) 24.dp else 6.dp,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label         = "dot_w_$i"
            )
            val alpha by animateFloatAsState(
                targetValue   = if (isActive) 1f else 0.3f,
                animationSpec = tween(200),
                label         = "dot_a_$i"
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = alpha))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Feature card — untuk halaman Welcome
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FeatureCard(item: FeatureItem) {
    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Icon container — pakai secondaryContainer supaya warna dinamis Material You
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = item.icon,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier           = Modifier.size(26.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    item.title,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    item.desc,
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Permission card — untuk halaman Permissions
// Mirip foto: card besar, status dot kanan, icon kiri bulat
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PermissionCard(
    item: PermItem,
    state: PermState,
    onClick: () -> Unit
) {
    val isGranted = state == PermState.GRANTED
    val isDenied  = state == PermState.DENIED

    // Warna dinamis Material You berdasarkan status
    val containerColor by animateColorAsState(
        targetValue = when {
            isGranted -> MaterialTheme.colorScheme.secondaryContainer
            isDenied  -> MaterialTheme.colorScheme.errorContainer
            else      -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(300),
        label = "card_color_${item.permissionType}"
    )
    val iconBg by animateColorAsState(
        targetValue = when {
            isGranted -> MaterialTheme.colorScheme.secondary
            isDenied  -> MaterialTheme.colorScheme.error
            else      -> MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = tween(300),
        label = "icon_bg_${item.permissionType}"
    )
    val iconTint by animateColorAsState(
        targetValue = when {
            isGranted -> MaterialTheme.colorScheme.onSecondary
            isDenied  -> MaterialTheme.colorScheme.onError
            else      -> MaterialTheme.colorScheme.onPrimaryContainer
        },
        animationSpec = tween(300),
        label = "icon_tint_${item.permissionType}"
    )

    // Dot status — kanan atas seperti foto
    val dotColor by animateColorAsState(
        targetValue = when {
            isGranted             -> Color(0xFF4CAF50)
            isDenied              -> MaterialTheme.colorScheme.error
            state == PermState.CHECKING -> MaterialTheme.colorScheme.tertiary
            else                  -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(300),
        label = "dot_${item.permissionType}"
    )

    val scale = remember { Animatable(1f) }
    LaunchedEffect(state) {
        if (state == PermState.GRANTED) {
            scale.animateTo(1.03f, spring(Spring.DampingRatioLowBouncy))
            scale.animateTo(1f, tween(150))
        }
    }

    Surface(
        shape    = RoundedCornerShape(20.dp),
        color    = containerColor,
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon bulat kiri
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isGranted,
                    transitionSpec = {
                        scaleIn(spring(Spring.DampingRatioLowBouncy)) + fadeIn(tween(200)) togetherWith
                                scaleOut(tween(100)) + fadeOut(tween(100))
                    },
                    label = "icon_${item.permissionType}"
                ) { granted ->
                    Icon(
                        imageVector        = if (granted) Icons.Outlined.CheckCircle else item.icon,
                        contentDescription = null,
                        tint               = iconTint,
                        modifier           = Modifier.size(26.dp)
                    )
                }
            }

            // Text block
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    item.title,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                AnimatedContent(
                    targetState = when (state) {
                        PermState.CHECKING -> "Memeriksa…"
                        PermState.GRANTED  -> "Izin diberikan"
                        PermState.DENIED   -> if (item.required) "Izin ditolak" else "Dilewati"
                        PermState.IDLE     -> item.desc
                    },
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label = "desc_${item.permissionType}"
                ) { txt ->
                    Text(
                        txt,
                        style     = MaterialTheme.typography.bodySmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            // Status dot kanan — identik dengan foto
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            ) {
                if (state == PermState.CHECKING) {
                    val inf = rememberInfiniteTransition(label = "chk_${item.permissionType}")
                    val a by inf.animateFloat(0.3f, 1f,
                        infiniteRepeatable(tween(600), RepeatMode.Reverse),
                        label = "dot_pulse"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(dotColor.copy(alpha = a))
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lifecycle resume helper
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OnLifecycleResume(onResume: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onResume()
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main SetupScreen
// ─────────────────────────────────────────────────────────────────────────────
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
            val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            else true
            if (ok) notifState = PermState.GRANTED
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

    // ── Permission items untuk page izin ────────────────────────────────────
    val permItems = remember(includeStorage) {
        buildList {
            add(PermItem(
                icon           = Icons.Outlined.AdminPanelSettings,
                title          = "Akses Root",
                desc           = "Diperlukan untuk manajemen kernel dan sistem",
                permissionType = "ROOT",
                required       = true
            ))
            add(PermItem(
                icon           = Icons.Outlined.Accessibility,
                title          = "Layanan Aksesibilitas",
                desc           = "Deteksi aplikasi aktif untuk profil per-aplikasi",
                permissionType = "ACCESSIBILITY",
                required       = false
            ))
            add(PermItem(
                icon           = Icons.Outlined.QueryStats,
                title          = "Akses Penggunaan",
                desc           = "Pantau penggunaan aplikasi dan statistik waktu layar",
                permissionType = "NOTIFICATION",
                required       = false
            ))
            if (includeStorage) {
                add(PermItem(
                    icon           = Icons.Outlined.FolderOpen,
                    title          = "Akses Penyimpanan",
                    desc           = "Baca konfigurasi dan log dari penyimpanan",
                    permissionType = "STORAGE",
                    required       = false
                ))
            }
            add(PermItem(
                icon           = Icons.Outlined.Tune,
                title          = "Ubah Pengaturan Sistem",
                desc           = "Terapkan tweak performa secara langsung",
                permissionType = "WRITE_SETTINGS",
                required       = false
            ))
        }
    }

    // ── Completion logic ─────────────────────────────────────────────────────
    val rootOk      = rootState == PermState.GRANTED
    val permsDone   = rootOk &&
                      (notifState   != PermState.IDLE && notifState   != PermState.CHECKING) &&
                      (writeState   != PermState.IDLE && writeState   != PermState.CHECKING) &&
                      (!includeStorage || (storageState != PermState.IDLE && storageState != PermState.CHECKING))

    // ── Pages definition (3 pages: Welcome, Perms, Done) ────────────────────
    // Page 0 = Welcome splash (feature list)
    // Page 1 = Izin (permission cards)
    // Page 2 = Selesai
    val totalPages = 3
    val pagerState = rememberPagerState { totalPages }
    val currentPage = pagerState.currentPage

    fun nextPage() = scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
    fun prevPage() = scope.launch { pagerState.animateScrollToPage(currentPage - 1) }

    val canProceed = when (currentPage) {
        0    -> true
        1    -> rootOk // harus root sebelum lanjut ke done
        else -> true
    }

    // Entrance animation
    val screenAlpha  = remember { Animatable(0f) }
    val screenSlideY = remember { Animatable(24f) }
    LaunchedEffect(Unit) {
        launch { screenAlpha.animateTo(1f, tween(380)) }
        launch { screenSlideY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) }
    }

    // Auto-check permissions saat landing di page perms
    LaunchedEffect(currentPage) {
        if (currentPage == 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val notifOk = ContextCompat.checkSelfPermission(
                    ctx, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (notifOk && notifState == PermState.IDLE) notifState = PermState.GRANTED
            } else {
                if (notifState == PermState.IDLE) notifState = PermState.GRANTED
            }
            if (android.provider.Settings.System.canWrite(ctx) && writeState == PermState.IDLE)
                writeState = PermState.GRANTED
            if (includeStorage) {
                val storOk = ContextCompat.checkSelfPermission(
                    ctx, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                if (storOk && storageState == PermState.IDLE) storageState = PermState.GRANTED
            }
        }
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
            // Soft background glow — Material You tonal
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 80.dp, y = (-40).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier            = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ── Header: logo + app name ──────────────────────────────
                Row(
                    modifier          = Modifier
                        .padding(horizontal = 28.dp)
                        .padding(top = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // App icon placeholder — ganti dengan Image(painterResource…) kalau ada
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "AE",
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        "AE Manager",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                }

                // ── Pager content ────────────────────────────────────────
                HorizontalPager(
                    state                  = pagerState,
                    pageSize               = PageSize.Fill,
                    beyondViewportPageCount = 1,
                    userScrollEnabled      = canProceed,
                    modifier               = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) { idx ->
                    val pageOffset = (pagerState.currentPage - idx) + pagerState.currentPageOffsetFraction
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 28.dp)
                            .padding(top = 32.dp, bottom = 8.dp)
                            .graphicsLayer {
                                translationX = pageOffset * size.width * 0.18f
                                val abs = kotlin.math.abs(pageOffset)
                                alpha  = lerp(1f, 0f, (abs - 0.5f).coerceAtLeast(0f) * 2f)
                                scaleX = lerp(1f, 0.95f, abs.coerceIn(0f, 1f))
                                scaleY = lerp(1f, 0.95f, abs.coerceIn(0f, 1f))
                            }
                    ) {
                        when (idx) {
                            // ── Page 0: Welcome ──────────────────────────
                            0 -> WelcomePage(s)
                            // ── Page 1: Permissions ──────────────────────
                            1 -> PermissionsPage(
                                s           = s,
                                permItems   = permItems,
                                rootState   = rootState,
                                notifState  = notifState,
                                writeState  = writeState,
                                storageState = storageState,
                                onAction    = { permType ->
                                    when (permType) {
                                        "ROOT" -> scope.launch {
                                            rootState = PermState.CHECKING
                                            val ok = RootManager.requestRoot()
                                            rootState = if (ok) PermState.GRANTED else PermState.DENIED
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
                                                val ok = ContextCompat.checkSelfPermission(
                                                    ctx, Manifest.permission.READ_EXTERNAL_STORAGE
                                                ) == PackageManager.PERMISSION_GRANTED
                                                if (ok) PermState.GRANTED else PermState.DENIED
                                            }
                                        }
                                        "ACCESSIBILITY" -> {
                                            // Open accessibility settings
                                            ctx.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                        }
                                    }
                                }
                            )
                            // ── Page 2: Done ─────────────────────────────
                            2 -> DonePage(s, allGranted = permsDone)
                        }
                    }
                }

                // ── Bottom: dot indicator + button ───────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier
                        .padding(horizontal = 28.dp)
                        .padding(bottom = 36.dp)
                ) {
                    PagerDotIndicator(
                        total   = totalPages,
                        current = currentPage
                    )

                    // Main CTA button — mirip foto, full-width rounded pill
                    val btnScale = remember { Animatable(1f) }
                    LaunchedEffect(canProceed) {
                        if (canProceed) {
                            btnScale.animateTo(1.03f, tween(120))
                            btnScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy))
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                btnScale.animateTo(0.97f, tween(70))
                                btnScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy))
                            }
                            if (currentPage == totalPages - 1) onDone(rootState == PermState.GRANTED)
                            else nextPage()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .scale(btnScale.value),
                        shape   = RoundedCornerShape(50), // full pill — identik foto
                        enabled = canProceed,
                        colors  = ButtonDefaults.buttonColors(
                            containerColor        = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        AnimatedContent(
                            targetState = when (currentPage) {
                                totalPages - 1 -> s.setupBtnStart
                                else           -> s.setupBtnNext
                            },
                            transitionSpec = {
                                slideInVertically { -it } + fadeIn() togetherWith
                                        slideOutVertically { it } + fadeOut()
                            },
                            label = "btn_label"
                        ) { label ->
                            Text(
                                label,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 16.sp
                            )
                        }
                    }

                    // Required-root hint
                    AnimatedVisibility(
                        visible = !canProceed && currentPage == 1,
                        enter   = fadeIn() + slideInVertically { it / 2 },
                        exit    = fadeOut() + slideOutVertically { it / 2 }
                    ) {
                        Text(
                            s.setupRootRequired,
                            color     = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            fontSize  = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Back button
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

// ─────────────────────────────────────────────────────────────────────────────
// Page 0 — Welcome splash (mirip foto: judul besar + 3 feature card)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WelcomePage(s: AppStrings) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Big headline — mirip foto "Siap untuk memulai?"
        Text(
            s.setupWelcomeTitle,
            style      = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface,
            lineHeight = 38.sp
        )
        Text(
            s.setupWelcomeDesc,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(12.dp))

        // Feature cards — identik posisi di foto
        val features = listOf(
            FeatureItem(
                icon  = Icons.Outlined.Speed,
                title = "Performa",
                desc  = "Optimalkan pengaturan CPU dan GPU untuk performa maksimal"
            ),
            FeatureItem(
                icon  = Icons.Outlined.BatteryChargingFull,
                title = "Daya Tahan Baterai",
                desc  = "Perpanjang daya tahan baterai dengan manajemen daya cerdas"
            ),
            FeatureItem(
                icon  = Icons.Outlined.SportsEsports,
                title = "Mode Gaming",
                desc  = "Tingkatkan pengalaman gaming dengan optimasi real-time"
            ),
        )
        features.forEachIndexed { i, f ->
            val enterAnim = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(i * 80L)
                enterAnim.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
            }
            Box(
                modifier = Modifier
                    .graphicsLayer(
                        alpha        = enterAnim.value,
                        translationY = (1f - enterAnim.value) * 20f
                    )
            ) {
                FeatureCard(f)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page 1 — Permissions (mirip foto: judul "Izin", deskripsi, list permission card)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PermissionsPage(
    s: AppStrings,
    permItems: List<PermItem>,
    rootState: PermState,
    notifState: PermState,
    writeState: PermState,
    storageState: PermState,
    onAction: (String) -> Unit,
) {
    fun stateFor(type: String) = when (type) {
        "ROOT"           -> rootState
        "NOTIFICATION"   -> notifState
        "WRITE_SETTINGS" -> writeState
        "STORAGE"        -> storageState
        "ACCESSIBILITY"  -> PermState.IDLE // no persistent state for now
        else             -> PermState.IDLE
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Judul halaman — mirip foto "Izin" di tengah-atas
        Text(
            "Izin",
            style      = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Berikan izin yang diperlukan untuk fungsionalitas penuh",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(8.dp))

        permItems.forEachIndexed { i, item ->
            val enterAnim = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(i * 70L)
                enterAnim.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
            }
            Box(
                modifier = Modifier.graphicsLayer(
                    alpha        = enterAnim.value,
                    translationY = (1f - enterAnim.value) * 18f
                )
            ) {
                PermissionCard(
                    item    = item,
                    state   = stateFor(item.permissionType),
                    onClick = { onAction(item.permissionType) }
                )
            }
        }

        // Catatan kecil root wajib
        AnimatedVisibility(
            visible = rootState == PermState.DENIED,
            enter   = fadeIn() + slideInVertically { it / 2 },
            exit    = fadeOut()
        ) {
            Surface(
                shape  = RoundedCornerShape(14.dp),
                color  = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier          = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Outlined.Warning, null,
                        tint     = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        s.setupRootDenied,
                        color     = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize  = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page 2 — Done
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DonePage(s: AppStrings, allGranted: Boolean) {
    val iconScale = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        iconScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(Modifier.height(32.dp))

        // Big animated icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(iconScale.value)
                .clip(CircleShape)
                .background(
                    if (allGranted)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = if (allGranted) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                contentDescription = null,
                tint               = if (allGranted) MaterialTheme.colorScheme.onPrimaryContainer
                                     else MaterialTheme.colorScheme.onErrorContainer,
                modifier           = Modifier.size(56.dp)
            )
        }

        Text(
            if (allGranted) s.setupDoneTitle else s.setupIncompleteTitle,
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            color      = MaterialTheme.colorScheme.onSurface
        )
        Text(
            if (allGranted) s.setupDoneDesc else s.setupIncompleteDesc,
            style     = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        // Badge kalau semua granted
        AnimatedVisibility(
            visible = allGranted,
            enter   = scaleIn(spring(Spring.DampingRatioLowBouncy)) + fadeIn(),
            exit    = scaleOut() + fadeOut()
        ) {
            val inf = rememberInfiniteTransition(label = "badge_glow")
            val glowA by inf.animateFloat(
                0.5f, 1f,
                infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                label = "glow"
            )
            Surface(
                shape  = RoundedCornerShape(50),
                color  = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = glowA * 0.5f))
            ) {
                Row(
                    modifier              = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle, null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        s.setupAllPermsGranted,
                        fontSize      = 13.sp,
                        fontWeight    = FontWeight.SemiBold,
                        color         = MaterialTheme.colorScheme.primary.copy(alpha = glowA),
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}
