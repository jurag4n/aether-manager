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
import android.widget.Toast
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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.i18n.ProvideStrings
import dev.aether.manager.shizuku.ShizukuShell
import dev.aether.manager.ui.AetherTheme
import dev.aether.manager.util.RootManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent {
            AetherTheme {
                ProvideStrings {
                    SetupScreen { mode, rootGranted, shizukuGranted ->
                        getSharedPreferences("aether_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("setup_done", true)
                            .putString("setup_mode", mode.name.lowercase())
                            .putBoolean("root_mode", mode == SetupMode.ROOT)
                            .putBoolean("shizuku_mode", mode == SetupMode.SHIZUKU)
                            .putBoolean("no_root_mode", mode == SetupMode.SHIZUKU)
                            .putBoolean("shizuku_granted", shizukuGranted)
                            .apply()

                        if (rootGranted) RootManager.markGranted()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}

enum class SetupMode { ROOT, SHIZUKU }

private enum class PermState { IDLE, CHECKING, GRANTED, DENIED }

private data class SetupPermission(
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val key: String,
    val required: Boolean = false,
)

private fun isBatteryOptimizationIgnored(ctx: Context): Boolean {
    val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(ctx.packageName)
}

private fun isUsageStatsGranted(ctx: Context): Boolean {
    return try {
        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), ctx.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), ctx.packageName)
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (_: Throwable) {
        false
    }
}

private fun grantUsageStatsViaRoot(pkg: String): Boolean = runCatching {
    com.topjohnwu.superuser.Shell.cmd("appops set $pkg GET_USAGE_STATS allow").exec().isSuccess
}.getOrDefault(false)

private fun openShizuku(ctx: Context) {
    val launch = ctx.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
    if (launch != null) {
        ctx.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } else {
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

@Composable
private fun OnLifecycleResume(onResume: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) onResume() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
}

@Composable
fun SetupScreen(onDone: (mode: SetupMode, rootGranted: Boolean, shizukuGranted: Boolean) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(SetupMode.ROOT) }
    var rootState by remember { mutableStateOf(PermState.IDLE) }
    var shizukuState by remember { mutableStateOf(PermState.IDLE) }
    var notifState by remember { mutableStateOf(PermState.IDLE) }
    var writeState by remember { mutableStateOf(PermState.IDLE) }
    var storageState by remember { mutableStateOf(PermState.IDLE) }
    var batteryState by remember { mutableStateOf(PermState.IDLE) }
    var usageState by remember { mutableStateOf(PermState.IDLE) }
    var busy by remember { mutableStateOf(false) }
    var shizukuRunning by remember { mutableStateOf(false) }
    var shizukuGrantedLive by remember { mutableStateOf(false) }

    val includeStorage = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        notifState = if (ok) PermState.GRANTED else PermState.DENIED
    }
    val storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        storageState = if (ok) PermState.GRANTED else PermState.DENIED
    }
    val writeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        writeState = if (Settings.System.canWrite(ctx)) PermState.GRANTED else PermState.DENIED
    }
    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        batteryState = if (isBatteryOptimizationIgnored(ctx)) PermState.GRANTED else PermState.DENIED
    }
    val usageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        usageState = if (isUsageStatsGranted(ctx)) PermState.GRANTED else PermState.DENIED
    }

    fun refresh() {
        val notifOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (notifOk) notifState = PermState.GRANTED
        if (Settings.System.canWrite(ctx)) writeState = PermState.GRANTED
        if (isBatteryOptimizationIgnored(ctx)) batteryState = PermState.GRANTED
        if (isUsageStatsGranted(ctx)) usageState = PermState.GRANTED
        if (includeStorage && ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            storageState = PermState.GRANTED
        }
        shizukuRunning = ShizukuShell.isAvailable()
        shizukuGrantedLive = ShizukuShell.hasPermission()
        shizukuState = when {
            shizukuGrantedLive -> PermState.GRANTED
            shizukuRunning -> if (shizukuState == PermState.CHECKING) PermState.CHECKING else PermState.IDLE
            else -> PermState.DENIED
        }
    }

    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(mode) {
        while (true) {
            shizukuRunning = ShizukuShell.isAvailable()
            shizukuGrantedLive = ShizukuShell.hasPermission()
            if (mode == SetupMode.SHIZUKU) {
                shizukuState = when {
                    shizukuGrantedLive -> PermState.GRANTED
                    shizukuRunning -> if (shizukuState == PermState.CHECKING) PermState.CHECKING else PermState.IDLE
                    else -> PermState.DENIED
                }
            }
            delay(1200L)
        }
    }
    OnLifecycleResume { refresh() }

    val modeReady = when (mode) {
        SetupMode.ROOT -> rootState == PermState.GRANTED
        SetupMode.SHIZUKU -> shizukuState == PermState.GRANTED
    }
    val optionalReady = notifState == PermState.GRANTED &&
        batteryState == PermState.GRANTED &&
        usageState == PermState.GRANTED &&
        writeState == PermState.GRANTED &&
        (!includeStorage || storageState == PermState.GRANTED)

    val permissions = buildList {
        if (mode == SetupMode.ROOT) {
            add(SetupPermission(Icons.Outlined.AdminPanelSettings, "Root Access", "Full kernel manager mode: CPU, GPU, scheduler, thermal, governor, and root tweak engine.", "ROOT", true))
        } else {
            add(SetupPermission(Icons.Outlined.Security, "Shizuku Access", "No-root mode for limited tweaks: Private DNS, animation scale, doze basic, cache cleaner, and network lite.", "SHIZUKU", true))
        }
        add(SetupPermission(Icons.Outlined.NotificationsActive, "Notifications", "Show license, update, tweak, and background status notifications.", "NOTIFICATION"))
        if (includeStorage) add(SetupPermission(Icons.Outlined.FolderOpen, "Storage Access", "Read and save backup/config files on older Android versions.", "STORAGE"))
        add(SetupPermission(Icons.Outlined.BatteryChargingFull, "Battery Optimization", "Keep background monitor and tweak service more stable.", "BATTERY"))
        add(SetupPermission(Icons.Outlined.QueryStats, "Usage Access", "Improve app profile detection and per-app monitoring.", "USAGE"))
        add(SetupPermission(Icons.Outlined.Tune, "Write Settings", "Needed for safe no-root settings such as animation and DNS toggles.", "WRITE"))
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SetupHeader()
                Text(
                    text = "Choose Setup Mode",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Use Root Mode for full control, or No Root / Shizuku Mode for limited tweaks without root.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ModeCard(
                        title = "Root Mode",
                        desc = "Full control",
                        icon = Icons.Outlined.AdminPanelSettings,
                        selected = mode == SetupMode.ROOT,
                        modifier = Modifier.weight(1f),
                        onClick = { mode = SetupMode.ROOT }
                    )
                    ModeCard(
                        title = "No Root",
                        desc = "Limited safe tweaks",
                        icon = Icons.Outlined.Security,
                        selected = mode == SetupMode.SHIZUKU,
                        modifier = Modifier.weight(1f),
                        onClick = { mode = SetupMode.SHIZUKU }
                    )
                }

                ModeInfoCard(mode = mode)

                if (mode == SetupMode.SHIZUKU) {
                    ShizukuLiveStatusCard(
                        running = shizukuRunning,
                        granted = shizukuGrantedLive,
                        onOpen = { openShizuku(ctx) },
                        onGrant = { ShizukuShell.requestPermissionIfNeeded() }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    permissions.forEach { item ->
                        PermissionRow(
                            item = item,
                            state = when (item.key) {
                                "ROOT" -> rootState
                                "SHIZUKU" -> shizukuState
                                "NOTIFICATION" -> notifState
                                "STORAGE" -> storageState
                                "BATTERY" -> batteryState
                                "USAGE" -> usageState
                                "WRITE" -> writeState
                                else -> PermState.IDLE
                            },
                            onClick = {
                                when (item.key) {
                                    "ROOT" -> scope.launch {
                                        rootState = PermState.CHECKING
                                        val ok = withContext(Dispatchers.IO) { RootManager.requestRoot() }
                                        rootState = if (ok) {
                                            RootManager.markGranted()
                                            PermState.GRANTED
                                        } else PermState.DENIED
                                    }
                                    "SHIZUKU" -> {
                                        if (!ShizukuShell.isAvailable()) {
                                            Toast.makeText(ctx, "Open Shizuku and start service first", Toast.LENGTH_LONG).show()
                                            openShizuku(ctx)
                                            shizukuState = PermState.DENIED
                                        } else if (ShizukuShell.hasPermission()) {
                                            shizukuState = PermState.GRANTED
                                        } else {
                                            shizukuState = PermState.CHECKING
                                            ShizukuShell.requestPermissionIfNeeded()
                                            Toast.makeText(ctx, "Grant Shizuku permission, then return to Aether", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    "NOTIFICATION" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else notifState = PermState.GRANTED
                                    "STORAGE" -> if (includeStorage) storageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE) else storageState = PermState.GRANTED
                                    "BATTERY" -> batteryLauncher.launch(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")))
                                    "USAGE" -> {
                                        if (isUsageStatsGranted(ctx)) usageState = PermState.GRANTED
                                        else if (mode == SetupMode.ROOT && rootState == PermState.GRANTED) {
                                            scope.launch {
                                                usageState = PermState.CHECKING
                                                val ok = withContext(Dispatchers.IO) { grantUsageStatsViaRoot(ctx.packageName) }
                                                usageState = if (ok || isUsageStatsGranted(ctx)) PermState.GRANTED else PermState.IDLE
                                                if (usageState == PermState.IDLE) usageLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                            }
                                        } else usageLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                    }
                                    "WRITE" -> if (Settings.System.canWrite(ctx)) writeState = PermState.GRANTED else writeLauncher.launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}")))
                                }
                            }
                        )
                    }
                }

                SetupSummary(modeReady = modeReady, optionalReady = optionalReady, mode = mode)

                FilledTonalButton(
                    onClick = {
                        if (!modeReady) {
                            Toast.makeText(
                                ctx,
                                if (mode == SetupMode.ROOT) "Grant root access first" else "Grant Shizuku access first",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@FilledTonalButton
                        }
                        scope.launch {
                            busy = true
                            delay(160)
                            onDone(mode, rootState == PermState.GRANTED, shizukuState == PermState.GRANTED)
                        }
                    },
                    enabled = !busy && modeReady,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    AnimatedContent(targetState = busy, label = "setup_button") { running ->
                        if (running) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Start Aether Manager", fontWeight = FontWeight.Bold)
                    }
                }
Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SetupHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher),
            contentDescription = "Aether Manager",
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(15.dp))
        )
        Column(Modifier.weight(1f)) {
            Text("Aether Manager", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            Text("Setup access mode", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    desc: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        tween(220, easing = FastOutSlowInEasing),
        label = "mode_border"
    )
    Surface(
        modifier = modifier
            .animateContentSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f) else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        border = BorderStroke(1.4.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            if (selected) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Selected", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ModeInfoCard(mode: SetupMode) {
    val isRoot = mode == SetupMode.ROOT
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isRoot) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.48f) else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (isRoot) "Root setup is for full kernel control" else "No Root setup is limited by Android/Shizuku permission",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (isRoot) "CPU, GPU, governor, scheduler, thermal profile, root apply engine, and boot persistence can be enabled."
                else "Only safe no-root tweaks are enabled. CPU/GPU/kernel tweaks stay disabled until Root Mode is granted.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


@Composable
private fun ShizukuLiveStatusCard(
    running: Boolean,
    granted: Boolean,
    onOpen: () -> Unit,
    onGrant: () -> Unit
) {
    val statusColor = when {
        running && granted -> MaterialTheme.colorScheme.primary
        running -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(statusColor.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (running && granted) Icons.Outlined.CheckCircle else Icons.Outlined.Security,
                        contentDescription = null,
                        tint = statusColor
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Shizuku Status", fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            running && granted -> "Running · permission granted"
                            running -> "Running · grant permission"
                            else -> "Not running · pair/start again"
                        },
                        color = statusColor,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                "Status updates in realtime. If Shizuku is not running, pair/start Shizuku again, then return here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Text(if (running) "Open Shizuku" else "Pair Again")
                }
                FilledTonalButton(onClick = onGrant, enabled = running && !granted, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Text("Grant")
                }
            }
        }
    }
}


@Composable
private fun PermissionRow(item: SetupPermission, state: PermState, onClick: () -> Unit) {
    val color = when (state) {
        PermState.GRANTED -> MaterialTheme.colorScheme.primary
        PermState.DENIED -> MaterialTheme.colorScheme.error
        PermState.CHECKING -> MaterialTheme.colorScheme.tertiary
        PermState.IDLE -> MaterialTheme.colorScheme.outline
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(17.dp)).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    PermState.CHECKING -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = color)
                    PermState.GRANTED -> Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = color)
                    PermState.DENIED -> Icon(Icons.Outlined.Warning, contentDescription = null, tint = color)
                    else -> Icon(item.icon, contentDescription = null, tint = color)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    if (item.required) RequiredChip()
                }
                Text(
                    when (state) {
                        PermState.GRANTED -> when (item.key) {
                            "ROOT" -> "Root shell granted · full kernel tweaks unlocked"
                            "SHIZUKU" -> "Shizuku permission granted · no-root tweaks unlocked"
                            "NOTIFICATION" -> "Notifications enabled for license, update, and service status"
                            "STORAGE" -> "Storage access granted for backups and config files"
                            "BATTERY" -> "Battery optimization ignored · background service more stable"
                            "USAGE" -> "Usage access granted · app/profile monitoring ready"
                            "WRITE" -> "Write Settings granted · DNS and animation tweaks ready"
                            else -> "Permission granted"
                        }
                        PermState.DENIED -> when (item.key) {
                            "SHIZUKU" -> "Shizuku is not running or permission is missing"
                            "ROOT" -> "Root is not granted yet"
                            else -> "Not granted yet"
                        }
                        PermState.CHECKING -> "Checking permission…"
                        else -> item.desc
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun RequiredChip() {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)) {
        Text(
            "Required",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SetupSummary(modeReady: Boolean, optionalReady: Boolean, mode: SetupMode) {
    val color = if (modeReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.24f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.Start) {
            Text(
                if (modeReady) "Setup mode ready" else "Setup mode not ready",
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                when {
                    !modeReady && mode == SetupMode.ROOT -> "Grant root access to continue with full tweak mode."
                    !modeReady && mode == SetupMode.SHIZUKU -> "Start Shizuku service and grant permission to continue with no-root mode."
                    optionalReady -> "All optional permissions are ready."
                    else -> "Optional permissions can be completed now or later from Settings."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp
            )
        }
    }
}
