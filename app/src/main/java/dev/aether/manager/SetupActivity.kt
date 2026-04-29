package dev.aether.manager

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.ui.res.painterResource
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

private data class FeatureItem(
    val icon: ImageVector,
    val title: String,
    val desc: String,
)

private data class PermItem(
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val permissionType: String,
    val required: Boolean = true,
)

// ─────────────────────────────────────────────────────────────────────────────
// FIX: Helper cek Accessibility Service aktif untuk package ini
// Bug sebelumnya: ACCESSIBILITY selalu PermState.IDLE karena tidak pernah di-check
// ─────────────────────────────────────────────────────────────────────────────
private fun isAccessibilityEnabled(ctx: Context): Boolean {
    val enabled = Settings.Secure.getString(
        ctx.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val pkg = ctx.packageName
    return enabled.split(":").any { it.startsWith("$pkg/") }
}

private fun isBatteryOptimizationIgnored(ctx: Context): Boolean {
    val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(ctx.packageName)
}

private fun isUsageStatsGranted(ctx: Context): Boolean {
    return try {
        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), ctx.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), ctx.packageName
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) { false }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dot indicator — pill aktif + bouncy spring
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PagerDotIndicator(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        repeat(total) { i ->
            val isActive = i == current
            val width by animateDpAsState(
                targetValue   = if (isActive) 28.dp else 7.dp,
                animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
                label         = "dot_w_$i"
            )
            val alpha by animateFloatAsState(
                targetValue   = if (isActive) 1f else 0.28f,
                animationSpec = tween(240, easing = FastOutSlowInEasing),
                label         = "dot_a_$i"
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = alpha))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Feature card — stagger entrance via index
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FeatureCard(item: FeatureItem, index: Int) {
    val enterAlpha = remember { Animatable(0f) }
    val enterY     = remember { Animatable(30f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 90L)
        launch { enterAlpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing)) }
        launch { enterY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) }
    }

    Surface(
        shape    = RoundedCornerShape(20.dp),
        color    = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = enterAlpha.value, translationY = enterY.value)
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
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
                    style      = MaterialTheme.typography.bodySmall,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Permission card — animasi lebih smooth + dot pulse
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PermissionCard(
    item: PermItem,
    state: PermState,
    index: Int,
    onClick: () -> Unit
) {
    val isGranted  = state == PermState.GRANTED
    val isDenied   = state == PermState.DENIED
    val isChecking = state == PermState.CHECKING

    val containerColor by animateColorAsState(
        targetValue   = when {
            isGranted -> MaterialTheme.colorScheme.secondaryContainer
            isDenied  -> MaterialTheme.colorScheme.errorContainer
            else      -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label         = "cc_${item.permissionType}"
    )
    val iconBg by animateColorAsState(
        targetValue   = when {
            isGranted -> MaterialTheme.colorScheme.secondary
            isDenied  -> MaterialTheme.colorScheme.error
            else      -> MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label         = "ib_${item.permissionType}"
    )
    val iconTint by animateColorAsState(
        targetValue   = when {
            isGranted -> MaterialTheme.colorScheme.onSecondary
            isDenied  -> MaterialTheme.colorScheme.onError
            else      -> MaterialTheme.colorScheme.onPrimaryContainer
        },
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label         = "it_${item.permissionType}"
    )
    val dotColor by animateColorAsState(
        targetValue   = when {
            isGranted  -> Color(0xFF4CAF50)
            isDenied   -> MaterialTheme.colorScheme.error
            isChecking -> MaterialTheme.colorScheme.tertiary
            else       -> MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label         = "dc_${item.permissionType}"
    )

    // Bounce saat granted
    val cardScale = remember { Animatable(1f) }
    LaunchedEffect(state) {
        if (state == PermState.GRANTED) {
            cardScale.animateTo(1.045f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium))
            cardScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
        }
    }

    // Stagger entrance
    val enterAlpha = remember { Animatable(0f) }
    val enterY     = remember { Animatable(22f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 80L)
        launch { enterAlpha.animateTo(1f, tween(280, easing = FastOutSlowInEasing)) }
        launch { enterY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) }
    }

    Surface(
        shape    = RoundedCornerShape(20.dp),
        color    = containerColor,
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale.value)
            .graphicsLayer(alpha = enterAlpha.value, translationY = enterY.value)
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier         = Modifier.size(52.dp).clip(CircleShape).background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isGranted,
                    transitionSpec = {
                        (scaleIn(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium)) +
                                fadeIn(tween(200))) togetherWith
                                (scaleOut(tween(120)) + fadeOut(tween(120)))
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

            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        item.title,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        modifier   = Modifier.weight(1f, fill = false)
                    )
                    if (item.required) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                        ) {
                            Text(
                                "Wajib",
                                fontSize      = 9.sp,
                                fontWeight    = FontWeight.Bold,
                                color         = MaterialTheme.colorScheme.onErrorContainer,
                                modifier      = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }
                AnimatedContent(
                    targetState = when (state) {
                        PermState.CHECKING -> "Memeriksa…"
                        PermState.GRANTED  -> "Izin diberikan"
                        PermState.DENIED   -> if (item.required) "Izin ditolak" else "Dilewati"
                        PermState.IDLE     -> item.desc
                    },
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "desc_${item.permissionType}"
                ) { txt ->
                    Text(
                        txt,
                        style      = MaterialTheme.typography.bodySmall,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            // Dot pulse saat checking
            Box(
                modifier         = Modifier.size(12.dp).clip(CircleShape).background(dotColor),
                contentAlignment = Alignment.Center
            ) {
                if (isChecking) {
                    val inf = rememberInfiniteTransition(label = "pulse_${item.permissionType}")
                    val ps by inf.animateFloat(
                        0.5f, 1.5f,
                        infiniteRepeatable(
                            tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse
                        ),
                        label = "ps"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(ps)
                            .clip(CircleShape)
                            .background(dotColor.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lifecycle resume
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
// SetupScreen
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
    var storState    by remember { mutableStateOf(PermState.IDLE) }
    var batteryState by remember { mutableStateOf(PermState.IDLE) }
    var usageState   by remember { mutableStateOf(PermState.IDLE) }
    // FIX: accessState punya state sendiri — tidak lagi IDLE permanen
    var accessState  by remember { mutableStateOf(PermState.IDLE) }

    val includeStorage = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

    // ── Launchers ────────────────────────────────────────────────────────────
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifState = if (granted) PermState.GRANTED else PermState.DENIED }

    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> storState = if (granted) PermState.GRANTED else PermState.DENIED }

    val writeSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        writeState = if (Settings.System.canWrite(ctx)) PermState.GRANTED else PermState.DENIED
    }

    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        batteryState = if (isBatteryOptimizationIgnored(ctx)) PermState.GRANTED else PermState.DENIED
    }

    val usageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        usageState = if (isUsageStatsGranted(ctx)) PermState.GRANTED else PermState.DENIED
    }

    // FIX: Launcher aksesibilitas — cek hasilnya saat kembali dari Settings
    val accessibilityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        accessState = if (isAccessibilityEnabled(ctx)) PermState.GRANTED else PermState.DENIED
    }

    // ── Re-check semua saat ON_RESUME ────────────────────────────────────────
    OnLifecycleResume {
        if (notifState != PermState.IDLE) {
            val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            else true
            if (ok) notifState = PermState.GRANTED
        }
        if (writeState != PermState.IDLE && Settings.System.canWrite(ctx))
            writeState = PermState.GRANTED
        if (includeStorage && storState != PermState.IDLE) {
            val ok = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (ok) storState = PermState.GRANTED
        }
        if (batteryState != PermState.IDLE && isBatteryOptimizationIgnored(ctx))
            batteryState = PermState.GRANTED
        if (usageState != PermState.IDLE && isUsageStatsGranted(ctx))
            usageState = PermState.GRANTED
        // FIX: re-check aksesibilitas setiap kali balik dari Settings
        if (accessState != PermState.IDLE) {
            accessState = if (isAccessibilityEnabled(ctx)) PermState.GRANTED else PermState.DENIED
        }
    }

    // ── Permission items ──────────────────────────────────────────────────────
    val permItems = remember(includeStorage) {
        buildList {
            add(PermItem(
                Icons.Outlined.AdminPanelSettings,
                "Akses Root",
                "Diperlukan untuk manajemen kernel dan pengaturan sistem tingkat rendah",
                "ROOT", required = true
            ))
            add(PermItem(
                Icons.Outlined.NotificationsActive,
                "Notifikasi",
                "Tampilkan pemberitahuan status optimasi dan peringatan sistem",
                "NOTIFICATION", required = false
            ))
            if (includeStorage) {
                add(PermItem(
                    Icons.Outlined.FolderOpen,
                    "Penyimpanan",
                    "Baca dan simpan konfigurasi profil serta log sistem",
                    "STORAGE", required = false
                ))
            }
            add(PermItem(
                Icons.Outlined.BatteryChargingFull,
                "Jangan Batasi Baterai",
                "Jalankan optimasi latar belakang tanpa dibatasi penghemat daya",
                "BATTERY", required = false
            ))
            add(PermItem(
                Icons.Outlined.QueryStats,
                "Akses Penggunaan",
                "Pantau statistik pemakaian aplikasi untuk profil per-aplikasi",
                "USAGE_STATS", required = false
            ))
            add(PermItem(
                Icons.Outlined.Tune,
                "Ubah Pengaturan Sistem",
                "Terapkan tweak performa dan layar secara langsung",
                "WRITE_SETTINGS", required = false
            ))
        }
    }

    // ── Completion ────────────────────────────────────────────────────────────
    val rootOk = rootState == PermState.GRANTED
    fun PermState.decided() = this == PermState.GRANTED || this == PermState.DENIED

    // FIX: semua permission harus sudah "decided" (bukan IDLE/CHECKING)
    // sebelum bisa lanjut ke halaman Done
    val allDecided = rootOk &&
            notifState.decided() &&
            batteryState.decided() &&
            usageState.decided() &&
            writeState.decided() &&
            (!includeStorage || storState.decided())

    // ── Pager ─────────────────────────────────────────────────────────────────
    val totalPages  = 3
    val pagerState  = rememberPagerState { totalPages }
    val currentPage = pagerState.currentPage

    fun nextPage() = scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
    fun prevPage() = scope.launch { pagerState.animateScrollToPage(currentPage - 1) }

    // FIX: page 1 (izin) tidak bisa lanjut sampai root granted + semua perm diputuskan
    val canProceed = when (currentPage) {
        1    -> allDecided
        else -> true
    }

    // ── Auto-check saat landing di page perms ────────────────────────────────
    LaunchedEffect(currentPage) {
        if (currentPage == 1) {
            val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            else true
            if (notifState == PermState.IDLE && notifOk) notifState = PermState.GRANTED
            if (writeState == PermState.IDLE && Settings.System.canWrite(ctx))
                writeState = PermState.GRANTED
            if (includeStorage && storState == PermState.IDLE) {
                val ok = ContextCompat.checkSelfPermission(
                    ctx, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                if (ok) storState = PermState.GRANTED
            }
            if (batteryState == PermState.IDLE && isBatteryOptimizationIgnored(ctx))
                batteryState = PermState.GRANTED
            if (usageState == PermState.IDLE && isUsageStatsGranted(ctx))
                usageState = PermState.GRANTED
            // FIX: cek aksesibilitas saat halaman izin dibuka
            if (accessState == PermState.IDLE && isAccessibilityEnabled(ctx))
                accessState = PermState.GRANTED
        }
    }

    // ── Entrance animation ────────────────────────────────────────────────────
    val screenAlpha  = remember { Animatable(0f) }
    val screenSlideY = remember { Animatable(30f) }
    LaunchedEffect(Unit) {
        launch { screenAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
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
            // Ambient glow
            Box(
                modifier = Modifier
                    .size(340.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 90.dp, y = (-50).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier            = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ── Header ───────────────────────────────────────────────
                Row(
                    modifier              = Modifier
                        .padding(horizontal = 28.dp)
                        .padding(top = 28.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // FIX: pakai ic_launcher bukan placeholder Box teks "AE"
                    Image(
                        painter            = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "Aether Manager",
                        modifier           = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    // FIX: nama benar "Aether Manager" bukan "AE Manager"
                    Text(
                        "Aether Manager",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                }

                // ── Pager ────────────────────────────────────────────────
                HorizontalPager(
                    state                   = pagerState,
                    pageSize                = PageSize.Fill,
                    beyondViewportPageCount = 1,
                    // FIX: swipe diblokir sampai semua permission diputuskan
                    userScrollEnabled       = canProceed,
                    modifier                = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) { idx ->
                    val pageOffset =
                        (pagerState.currentPage - idx) + pagerState.currentPageOffsetFraction
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 28.dp)
                            .padding(top = 28.dp, bottom = 8.dp)
                            .graphicsLayer {
                                translationX = pageOffset * size.width * 0.14f
                                val abs = kotlin.math.abs(pageOffset)
                                alpha  = lerp(1f, 0f, (abs - 0.45f).coerceAtLeast(0f) * 2.2f)
                                scaleX = lerp(1f, 0.94f, abs.coerceIn(0f, 1f))
                                scaleY = lerp(1f, 0.94f, abs.coerceIn(0f, 1f))
                            }
                    ) {
                        when (idx) {
                            0 -> WelcomePage(s)
                            1 -> PermissionsPage(
                                s            = s,
                                permItems    = permItems,
                                rootState    = rootState,
                                notifState   = notifState,
                                writeState   = writeState,
                                storageState = storState,
                                batteryState = batteryState,
                                usageState   = usageState,
                                accessState  = accessState,
                                onAction     = { permType ->
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
                                            if (Settings.System.canWrite(ctx)) {
                                                writeState = PermState.GRANTED
                                            } else {
                                                writeSettingsLauncher.launch(Intent(
                                                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                                    Uri.parse("package:${ctx.packageName}")
                                                ))
                                            }
                                        }
                                        "STORAGE" -> {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                storState = PermState.GRANTED
                                            } else {
                                                val ok = ContextCompat.checkSelfPermission(
                                                    ctx, Manifest.permission.READ_EXTERNAL_STORAGE
                                                ) == PackageManager.PERMISSION_GRANTED
                                                if (ok) {
                                                    storState = PermState.GRANTED
                                                } else {
                                                    storageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                                }
                                            }
                                        }
                                        "BATTERY" -> {
                                            if (isBatteryOptimizationIgnored(ctx)) {
                                                batteryState = PermState.GRANTED
                                            } else {
                                                batteryLauncher.launch(Intent(
                                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                    Uri.parse("package:${ctx.packageName}")
                                                ))
                                            }
                                        }
                                        "USAGE_STATS" -> {
                                            if (isUsageStatsGranted(ctx)) {
                                                usageState = PermState.GRANTED
                                            } else {
                                                usageLauncher.launch(
                                                    Intent(
                                                        Settings.ACTION_USAGE_ACCESS_SETTINGS,
                                                        Uri.parse("package:${ctx.packageName}")
                                                    ).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                )
                                            }
                                        }
                                        // FIX: buka Accessibility Settings via launcher
                                        "ACCESSIBILITY" -> {
                                            accessState = PermState.CHECKING
                                            accessibilityLauncher.launch(
                                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                            )
                                        }
                                    }
                                }
                            )
                            2 -> DonePage(s, allGranted = allDecided)
                        }
                    }
                }

                // ── Bottom controls ──────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier            = Modifier
                        .padding(horizontal = 28.dp)
                        .padding(bottom = 36.dp)
                ) {
                    PagerDotIndicator(total = totalPages, current = currentPage)

                    val btnScale = remember { Animatable(1f) }
                    LaunchedEffect(canProceed) {
                        if (canProceed) {
                            btnScale.animateTo(1.04f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium))
                            btnScale.animateTo(1f, tween(130))
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                btnScale.animateTo(0.96f, tween(55))
                                btnScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium))
                            }
                            if (currentPage == totalPages - 1) onDone(rootState == PermState.GRANTED)
                            else nextPage()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .scale(btnScale.value),
                        shape   = RoundedCornerShape(50),
                        enabled = canProceed,
                        colors  = ButtonDefaults.buttonColors(
                            containerColor         = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            disabledContentColor   = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        AnimatedContent(
                            targetState = when (currentPage) {
                                totalPages - 1 -> s.setupBtnStart
                                else           -> s.setupBtnNext
                            },
                            transitionSpec = {
                                (slideInVertically { -it } + fadeIn(tween(180))) togetherWith
                                        (slideOutVertically { it } + fadeOut(tween(140)))
                            },
                            label = "btn_label"
                        ) { label ->
                            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }

                    // FIX: hint lebih informatif — beda pesan kalau root belum vs perm lain
                    AnimatedVisibility(
                        visible = !canProceed && currentPage == 1,
                        enter   = fadeIn(tween(200)) + slideInVertically { it / 2 },
                        exit    = fadeOut(tween(160)) + slideOutVertically { it / 2 }
                    ) {
                        val pendingPerms = buildList {
                            if (!rootOk) add("Akses Root (wajib)")
                            if (notifState == PermState.IDLE) add("Notifikasi")
                            if (batteryState == PermState.IDLE) add("Jangan Batasi Baterai")
                            if (usageState == PermState.IDLE) add("Akses Penggunaan")
                            if (writeState == PermState.IDLE) add("Ubah Pengaturan Sistem")
                            if (includeStorage && storState == PermState.IDLE) add("Penyimpanan")
                        }
                        Text(
                            if (!rootOk) s.setupRootRequired
                            else "Ketuk untuk memutuskan: ${pendingPerms.joinToString(", ")}",
                            color     = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            fontSize  = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }

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
// Page 0 — Welcome
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WelcomePage(s: AppStrings) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            s.setupWelcomeTitle,
            style      = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface,
            lineHeight = 40.sp
        )
        Text(
            s.setupWelcomeDesc,
            style      = MaterialTheme.typography.bodyMedium,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(8.dp))

        val features = listOf(
            FeatureItem(Icons.Outlined.Speed, "Performa Maksimal",
                "Optimalkan CPU, GPU, dan kernel untuk responsivitas terbaik"),
            FeatureItem(Icons.Outlined.BatteryChargingFull, "Manajemen Daya",
                "Perpanjang masa pakai baterai dengan scheduler daya yang cerdas"),
            FeatureItem(Icons.Outlined.SportsEsports, "Mode Gaming",
                "Prioritaskan resource untuk gaming tanpa gangguan latar belakang"),
        )
        features.forEachIndexed { i, f -> FeatureCard(item = f, index = i) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page 1 — Permissions
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PermissionsPage(
    s: AppStrings,
    permItems: List<PermItem>,
    rootState: PermState,
    notifState: PermState,
    writeState: PermState,
    storageState: PermState,
    batteryState: PermState,
    usageState: PermState,
    accessState: PermState,
    onAction: (String) -> Unit,
) {
    fun stateFor(type: String) = when (type) {
        "ROOT"          -> rootState
        "NOTIFICATION"  -> notifState
        "WRITE_SETTINGS"-> writeState
        "STORAGE"       -> storageState
        "BATTERY"       -> batteryState
        "USAGE_STATS"   -> usageState
        "ACCESSIBILITY" -> accessState
        else            -> PermState.IDLE
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Izin Aplikasi",
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Ketuk setiap izin untuk mengaktifkan. Izin wajib harus diberikan untuk melanjutkan.",
            style      = MaterialTheme.typography.bodyMedium,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(2.dp))

        permItems.forEachIndexed { i, item ->
            PermissionCard(
                item    = item,
                state   = stateFor(item.permissionType),
                index   = i,
                onClick = { onAction(item.permissionType) }
            )
        }

        AnimatedVisibility(
            visible = rootState == PermState.DENIED,
            enter   = fadeIn(tween(200)) + slideInVertically { it / 2 },
            exit    = fadeOut(tween(150))
        ) {
            Surface(
                shape    = RoundedCornerShape(14.dp),
                color    = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier.padding(14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.Warning, null,
                        tint     = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp))
                    Text(
                        s.setupRootDenied,
                        color      = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize   = 12.sp,
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
    val iconScale = remember { Animatable(0.4f) }
    val iconAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { iconScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)) }
        launch { iconAlpha.animateTo(1f, tween(280)) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier            = Modifier.fillMaxWidth()
    ) {
        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(iconScale.value)
                .graphicsLayer(alpha = iconAlpha.value)
                .clip(CircleShape)
                .background(
                    if (allGranted) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
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
            style      = MaterialTheme.typography.bodyMedium,
            textAlign  = TextAlign.Center,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        AnimatedVisibility(
            visible = allGranted,
            enter   = scaleIn(spring(Spring.DampingRatioLowBouncy)) + fadeIn(tween(250)),
            exit    = scaleOut() + fadeOut()
        ) {
            val inf = rememberInfiniteTransition(label = "badge")
            val glow by inf.animateFloat(
                0.5f, 1f,
                infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "glow"
            )
            Surface(
                shape  = RoundedCornerShape(50),
                color  = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = glow * 0.5f))
            ) {
                Row(
                    modifier              = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.CheckCircle, null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp))
                    Text(
                        s.setupAllPermsGranted,
                        fontSize      = 13.sp,
                        fontWeight    = FontWeight.SemiBold,
                        color         = MaterialTheme.colorScheme.primary.copy(alpha = glow),
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}