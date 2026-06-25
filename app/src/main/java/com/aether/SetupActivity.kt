package com.aether

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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aether.i18n.ProvideStrings
import com.aether.shizuku.ShizukuShell
import com.aether.ui.AetherTheme
import com.aether.util.RootManager
import com.aether.util.SettingsPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            AetherTheme {
                ProvideStrings {
                    SetupScreen(
                        onDone = { selectedMode, rootGranted ->
                            val finalMode = if (selectedMode == SetupMode.ROOT && rootGranted) {
                                AccessModeValue.ROOT
                            } else {
                                AccessModeValue.NO_ROOT
                            }
                            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean(KEY_SETUP_DONE, true)
                                .apply()
                            SettingsPrefs.setAccessMode(this, finalMode.value)
                            if (rootGranted) RootManager.markGranted() else RootManager.markDenied()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }

    private companion object {
        private const val PREFS_NAME = "aether_prefs"
        private const val KEY_SETUP_DONE = "setup_done"
    }
}

private enum class SetupMode { ROOT, NO_ROOT }
private enum class AccessModeValue(val value: String) { ROOT("root"), NO_ROOT("no_root") }
private enum class CheckState { IDLE, CHECKING, GRANTED, DENIED }

private data class PermissionAction(
    val key: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val required: Boolean = false,
)

private fun isBatteryOptimizationIgnored(ctx: Context): Boolean {
    val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(ctx.packageName)
}

private fun isUsageStatsGranted(ctx: Context): Boolean {
    return runCatching {
        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                ctx.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                ctx.packageName
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)
}

private fun isNotificationGranted(ctx: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

private fun isStorageGranted(ctx: Context): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
}

private fun grantUsageStatsViaRoot(pkg: String): Boolean {
    return runCatching {
        com.topjohnwu.superuser.Shell.cmd("appops set $pkg GET_USAGE_STATS allow").exec().isSuccess
    }.getOrDefault(false)
}

@Composable
private fun OnResumeEffect(onResume: () -> Unit) {
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onResume()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun SetupScreen(onDone: (selectedMode: SetupMode, rootGranted: Boolean) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val includeStorage = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

    var selectedMode by remember { mutableStateOf(SetupMode.NO_ROOT) }
    var rootState by remember { mutableStateOf(CheckState.IDLE) }
    var shizukuState by remember { mutableStateOf(CheckState.IDLE) }
    var notificationState by remember { mutableStateOf(CheckState.IDLE) }
    var storageState by remember { mutableStateOf(CheckState.IDLE) }
    var batteryState by remember { mutableStateOf(CheckState.IDLE) }
    var usageState by remember { mutableStateOf(CheckState.IDLE) }
    var writeState by remember { mutableStateOf(CheckState.IDLE) }
    var message by remember { mutableStateOf("Pilih mode awal yang paling cocok untuk perangkat kamu.") }
    var startRunning by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        notificationState = if (ok) CheckState.GRANTED else CheckState.DENIED
    }
    val storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        storageState = if (ok) CheckState.GRANTED else CheckState.DENIED
    }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        writeState = if (Settings.System.canWrite(ctx)) CheckState.GRANTED else CheckState.DENIED
    }
    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        batteryState = if (isBatteryOptimizationIgnored(ctx)) CheckState.GRANTED else CheckState.DENIED
    }
    val usageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        usageState = if (isUsageStatsGranted(ctx)) CheckState.GRANTED else CheckState.DENIED
    }

    fun refreshStates() {
        shizukuState = when (ShizukuShell.state()) {
            ShizukuShell.State.READY -> CheckState.GRANTED
            ShizukuShell.State.DENIED -> if (shizukuState == CheckState.CHECKING) CheckState.DENIED else shizukuState
            ShizukuShell.State.NOT_RUNNING, ShizukuShell.State.ERROR -> CheckState.DENIED
        }
        notificationState = if (isNotificationGranted(ctx)) CheckState.GRANTED else notificationState
        storageState = if (isStorageGranted(ctx)) CheckState.GRANTED else storageState
        batteryState = if (isBatteryOptimizationIgnored(ctx)) CheckState.GRANTED else batteryState
        usageState = if (isUsageStatsGranted(ctx)) CheckState.GRANTED else usageState
        writeState = if (Settings.System.canWrite(ctx)) CheckState.GRANTED else writeState
    }

    fun requestRoot() {
        scope.launch {
            rootState = CheckState.CHECKING
            message = "Memeriksa akses Superuser…"
            val ok = withContext(Dispatchers.IO) { RootManager.requestRoot() }
            if (ok) {
                RootManager.markGranted()
                rootState = CheckState.GRANTED
                selectedMode = SetupMode.ROOT
                message = "Root aktif. Fitur kernel penuh bisa digunakan."
            } else {
                RootManager.markDenied()
                rootState = CheckState.DENIED
                selectedMode = SetupMode.NO_ROOT
                message = "Root belum tersedia. Mode Root diblok dan app aman masuk No Root."
            }
        }
    }

    fun selectMode(mode: SetupMode) {
        if (mode == SetupMode.ROOT) {
            if (rootState == CheckState.GRANTED) selectedMode = SetupMode.ROOT else requestRoot()
        } else {
            selectedMode = SetupMode.NO_ROOT
            message = "Mode No Root aktif. Izinkan Shizuku agar fitur ringan bisa berjalan normal tanpa SU."
        }
    }

    fun openAction(key: String) {
        when (key) {
            "ROOT" -> requestRoot()
            "SHIZUKU" -> {
                shizukuState = CheckState.CHECKING
                val ok = ShizukuShell.requestPermissionIfNeeded()
                shizukuState = if (ok || ShizukuShell.hasPermission()) CheckState.GRANTED else CheckState.IDLE
                message = when (ShizukuShell.state()) {
                    ShizukuShell.State.READY -> "Shizuku aktif. Mode No Root siap dipakai."
                    ShizukuShell.State.DENIED -> "Buka popup Shizuku lalu pilih Allow untuk Aether."
                    ShizukuShell.State.NOT_RUNNING -> "Shizuku belum berjalan. Aktifkan Shizuku dulu, lalu kembali ke Aether."
                    ShizukuShell.State.ERROR -> "Status Shizuku belum bisa dibaca. Coba buka Shizuku lalu ulangi."
                }
            }
            "NOTIFICATION" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else notificationState = CheckState.GRANTED
            }
            "STORAGE" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) storageState = CheckState.GRANTED
                else storageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            "BATTERY" -> {
                if (isBatteryOptimizationIgnored(ctx)) batteryState = CheckState.GRANTED
                else batteryLauncher.launch(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                )
            }
            "USAGE" -> {
                if (isUsageStatsGranted(ctx)) {
                    usageState = CheckState.GRANTED
                } else {
                    usageState = CheckState.CHECKING
                    scope.launch {
                        val shellGrant = withContext(Dispatchers.IO) {
                            when (selectedMode) {
                                SetupMode.ROOT -> grantUsageStatsViaRoot(ctx.packageName)
                                SetupMode.NO_ROOT -> ShizukuShell.hasPermission() &&
                                    ShizukuShell.sh("appops set ${ctx.packageName} GET_USAGE_STATS allow").exitCode == 0
                            }
                        }
                        if (shellGrant && isUsageStatsGranted(ctx)) {
                            usageState = CheckState.GRANTED
                        } else {
                            usageState = CheckState.IDLE
                            usageLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        }
                    }
                }
            }
            "WRITE" -> {
                if (Settings.System.canWrite(ctx)) writeState = CheckState.GRANTED
                else settingsLauncher.launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}")))
            }
        }
    }

    fun finishSetup() {
        if (startRunning) return
        val requiredOk =
            (if (selectedMode == SetupMode.ROOT) rootState == CheckState.GRANTED else shizukuState == CheckState.GRANTED) &&
                notificationState == CheckState.GRANTED &&
                (!includeStorage || storageState == CheckState.GRANTED) &&
                batteryState == CheckState.GRANTED &&
                usageState == CheckState.GRANTED &&
                writeState == CheckState.GRANTED
        if (!requiredOk) {
            message = "Lengkapi semua izin yang tampil dulu sebelum masuk aplikasi."
            return
        }
        scope.launch {
            startRunning = true
            val rootOk = selectedMode == SetupMode.ROOT && (rootState == CheckState.GRANTED || withContext(Dispatchers.IO) { RootManager.requestRoot() })
            if (selectedMode == SetupMode.ROOT && !rootOk) {
                startRunning = false
                RootManager.markDenied()
                rootState = CheckState.DENIED
                message = "Root belum diizinkan. Mode Root tidak bisa dimulai sebelum SU aktif."
                return@launch
            }
            onDone(selectedMode, rootOk)
        }
    }

    OnResumeEffect { refreshStates() }

    LaunchedEffect(Unit) {
        refreshStates()
        val alreadyRoot = withContext(Dispatchers.IO) { RootManager.ensureRootShellSync(requestIfNeeded = false) }
        if (alreadyRoot) {
            rootState = CheckState.GRANTED
            RootManager.markGranted()
        }
    }

    val permissionItems = remember(includeStorage, selectedMode) {
        buildList {
            if (selectedMode == SetupMode.ROOT) {
                add(PermissionAction("ROOT", Icons.Outlined.AdminPanelSettings, "Izinkan Root", "Wajib untuk mode Root dan tweak kernel penuh.", required = true))
            } else {
                add(PermissionAction("SHIZUKU", Icons.Outlined.PhoneAndroid, "Izinkan Shizuku", "Wajib untuk mode No Root agar shell sistem berjalan tanpa SU.", required = true))
            }
            add(PermissionAction("NOTIFICATION", Icons.Outlined.NotificationsActive, "Notifikasi", "Wajib untuk status apply, peringatan, dan proses background.", required = true))
            if (includeStorage) add(PermissionAction("STORAGE", Icons.Outlined.FolderOpen, "Penyimpanan", "Wajib untuk backup, import/export, dan log aplikasi.", required = true))
            add(PermissionAction("BATTERY", Icons.Outlined.BatteryChargingFull, "Optimasi Baterai", "Wajib agar service dan monitor tidak mudah dibatasi sistem.", required = true))
            add(PermissionAction("USAGE", Icons.Outlined.QueryStats, "Usage Access", "Wajib untuk profil per-aplikasi dan statistik aplikasi.", required = true))
            add(PermissionAction("WRITE", Icons.Outlined.Tune, "Write Settings", "Wajib untuk animasi, DNS, dan setting sistem aman.", required = true))
        }
    }

    val grantedCount = permissionItems.count { item ->
        when (item.key) {
            "ROOT" -> rootState == CheckState.GRANTED
            "SHIZUKU" -> shizukuState == CheckState.GRANTED
            "NOTIFICATION" -> notificationState == CheckState.GRANTED
            "STORAGE" -> storageState == CheckState.GRANTED
            "BATTERY" -> batteryState == CheckState.GRANTED
            "USAGE" -> usageState == CheckState.GRANTED
            "WRITE" -> writeState == CheckState.GRANTED
            else -> false
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.74f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .size(330.dp)
                    .align(Alignment.TopEnd)
                    .graphicsLayer(translationX = 90f, translationY = -100f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            )
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.BottomStart)
                    .graphicsLayer(translationX = -110f, translationY = 70f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(top = 12.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SetupHero(selectedMode = selectedMode, rootState = rootState)

                ModePicker(
                    selected = selectedMode,
                    rootState = rootState,
                    onSelect = ::selectMode
                )

                StatusMessageCard(
                    message = message,
                    state = if (selectedMode == SetupMode.ROOT && rootState == CheckState.GRANTED) CheckState.GRANTED else rootState
                )

                PermissionSummary(granted = grantedCount, total = permissionItems.size)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    permissionItems.forEachIndexed { index, item ->
                        val state = when (item.key) {
                            "ROOT" -> rootState
                            "SHIZUKU" -> shizukuState
                            "NOTIFICATION" -> notificationState
                            "STORAGE" -> storageState
                            "BATTERY" -> batteryState
                            "USAGE" -> usageState
                            "WRITE" -> writeState
                            else -> CheckState.IDLE
                        }
                        PermissionRow(
                            item = item,
                            state = state,
                            index = index,
                            onClick = { openAction(item.key) }
                        )
                    }
                }

                val canStart = permissionItems.all { item ->
                    when (item.key) {
                        "ROOT" -> rootState == CheckState.GRANTED
                        "SHIZUKU" -> shizukuState == CheckState.GRANTED
                        "NOTIFICATION" -> notificationState == CheckState.GRANTED
                        "STORAGE" -> storageState == CheckState.GRANTED
                        "BATTERY" -> batteryState == CheckState.GRANTED
                        "USAGE" -> usageState == CheckState.GRANTED
                        "WRITE" -> writeState == CheckState.GRANTED
                        else -> true
                    }
                }

                FilledTonalButton(
                    onClick = ::finishSetup,
                    enabled = !startRunning && canStart,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    AnimatedContent(
                        targetState = startRunning,
                        transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                        label = "setup_start_state"
                    ) { running ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (running) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Menyiapkan…", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Mulai Pakai Aether", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Text(
                    text = if (selectedMode == SetupMode.ROOT) "Mode Root wajib izin SU. Jika pilih No Root, opsi izinkan Root disembunyikan dan Shizuku yang dipakai." else "Mode No Root tidak meminta SU. Semua fitur ringan berjalan lewat Shizuku dan izin Android standar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SetupHero(selectedMode: SetupMode, rootState: CheckState) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Setup Aether",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Root atau No Root, tanpa stuck dan tanpa crash.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TinyPill(
                    text = if (selectedMode == SetupMode.ROOT && rootState == CheckState.GRANTED) "ROOT ACTIVE" else "NO ROOT READY",
                    color = if (selectedMode == SetupMode.ROOT && rootState == CheckState.GRANTED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
                TinyPill(text = "M3 UI", color = MaterialTheme.colorScheme.tertiary)
                TinyPill(text = "SAFE", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ModePicker(
    selected: SetupMode,
    rootState: CheckState,
    onSelect: (SetupMode) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Mode Akses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeCard(
                    title = "Root",
                    subtitle = when (rootState) {
                        CheckState.GRANTED -> "Superuser aktif"
                        CheckState.CHECKING -> "Memeriksa…"
                        CheckState.DENIED -> "Diblok sampai SU aktif"
                        CheckState.IDLE -> "Butuh Superuser"
                    },
                    icon = Icons.Outlined.AdminPanelSettings,
                    selected = selected == SetupMode.ROOT,
                    locked = rootState == CheckState.DENIED,
                    loading = rootState == CheckState.CHECKING,
                    onClick = { onSelect(SetupMode.ROOT) },
                    modifier = Modifier.weight(1f)
                )
                ModeCard(
                    title = "No Root",
                    subtitle = "Aman tanpa SU",
                    icon = Icons.Outlined.PhoneAndroid,
                    selected = selected == SetupMode.NO_ROOT,
                    locked = false,
                    loading = false,
                    onClick = { onSelect(SetupMode.NO_ROOT) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    locked: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
            locked -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.34f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "setup_mode_bg_$title"
    )
    val fg by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.onPrimaryContainer
            locked -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "setup_mode_fg_$title"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.015f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
        label = "setup_mode_scale_$title"
    )

    Surface(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = bg,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.30f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(fg.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = fg)
                else Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(23.dp))
            }
            Text(title, color = fg, fontWeight = FontWeight.Black, maxLines = 1)
            Text(
                subtitle,
                color = fg.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatusMessageCard(message: String, state: CheckState) {
    val color = when (state) {
        CheckState.GRANTED -> MaterialTheme.colorScheme.primary
        CheckState.DENIED -> MaterialTheme.colorScheme.error
        CheckState.CHECKING -> MaterialTheme.colorScheme.tertiary
        CheckState.IDLE -> MaterialTheme.colorScheme.secondary
    }
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.20f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = when (state) {
                    CheckState.GRANTED -> Icons.Outlined.CheckCircle
                    CheckState.DENIED -> Icons.Outlined.Warning
                    CheckState.CHECKING -> Icons.Outlined.Info
                    CheckState.IDLE -> Icons.Outlined.Info
                },
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun PermissionSummary(granted: Int, total: Int) {
    val progress = if (total <= 0) 0f else granted.toFloat() / total.toFloat()
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Izin & Stabilitas", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    Text("$granted dari $total aktif", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                TinyPill(text = "Wajib", color = MaterialTheme.colorScheme.primary)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    item: PermissionAction,
    state: CheckState,
    index: Int,
    onClick: () -> Unit,
) {
    val color = when (state) {
        CheckState.GRANTED -> MaterialTheme.colorScheme.primary
        CheckState.DENIED -> MaterialTheme.colorScheme.error
        CheckState.CHECKING -> MaterialTheme.colorScheme.tertiary
        CheckState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val bg by animateColorAsState(
        targetValue = when (state) {
            CheckState.GRANTED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f)
            CheckState.DENIED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.40f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
        },
        animationSpec = tween(210, easing = FastOutSlowInEasing),
        label = "permission_row_bg_${item.key}"
    )
    val rowAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(180 + index * 22, easing = FastOutSlowInEasing),
        label = "permission_row_alpha_${item.key}"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = rowAlpha)
            .clip(RoundedCornerShape(22.dp))
            .clickable(enabled = state != CheckState.CHECKING) { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = bg,
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (state == CheckState.CHECKING) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = color)
                else Icon(item.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
            }
            StatusBadge(state)
        }
    }
}

@Composable
private fun StatusBadge(state: CheckState) {
    val (label, color) = when (state) {
        CheckState.GRANTED -> "ON" to MaterialTheme.colorScheme.primary
        CheckState.DENIED -> "OFF" to MaterialTheme.colorScheme.error
        CheckState.CHECKING -> "…" to MaterialTheme.colorScheme.tertiary
        CheckState.IDLE -> "SET" to MaterialTheme.colorScheme.secondary
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.20f))
    ) {
        Text(
            text = label,
            color = color,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TinyPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.20f))
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            letterSpacing = 0.4.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
