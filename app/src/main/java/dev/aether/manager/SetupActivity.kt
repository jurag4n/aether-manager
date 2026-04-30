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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.aether.manager.i18n.AppStrings
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.i18n.ProvideStrings
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
                        onDone = { rootWasGranted ->
                            getSharedPreferences("aether_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean("setup_done", true)
                                .apply()

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
    val required: Boolean = false,
)

private fun isAccessibilityEnabled(ctx: Context): Boolean {
    val enabled = Settings.Secure.getString(
        ctx.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(":").any { it.startsWith("${ctx.packageName}/") }
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
    } catch (_: Exception) {
        false
    }
}

private fun grantUsageStatsViaRoot(pkg: String): Boolean {
    return try {
        val proc = Runtime.getRuntime().exec(
            arrayOf("su", "-c", "appops set $pkg GET_USAGE_STATS allow")
        )
        proc.waitFor() == 0
    } catch (_: Exception) {
        false
    }
}

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

@Composable
private fun SetupBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.TopEnd)
                .offset(x = 110.dp, y = (-90).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-120).dp, y = 60.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )
        content()
    }
}

@Composable
private fun SetupHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher),
            contentDescription = "Aether Manager",
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
        )
        Column(Modifier.weight(1f)) {
            Text(
                "Aether Manager",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Setup awal aplikasi",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PagerDotIndicator(total: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { i ->
            val active = i == current
            val width by animateDpAsState(
                targetValue = if (active) 30.dp else 8.dp,
                animationSpec = tween(320, easing = FastOutSlowInEasing),
                label = "dot_width_$i"
            )
            val alpha by animateFloatAsState(
                targetValue = if (active) 1f else 0.30f,
                animationSpec = tween(320, easing = FastOutSlowInEasing),
                label = "dot_alpha_$i"
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun HeroFeatureSpotlight() {
    val inf = rememberInfiniteTransition(label = "hero_motion")
    val floatY by inf.animateFloat(
        initialValue = -3f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_float"
    )
    val glow by inf.animateFloat(
        initialValue = 0.32f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_glow"
    )

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(188.dp)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .align(Alignment.Center)
                    .graphicsLayer(translationY = floatY)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = glow)),
                        RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(54.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    "Smart Optimize",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text(
                    "Root Mode Ready",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(item: FeatureItem, index: Int) {
    val alpha = remember { Animatable(0f) }
    val y = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        delay(index * 70L)
        launch { alpha.animateTo(1f, tween(360, easing = FastOutSlowInEasing)) }
        launch { y.animateTo(0f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)) }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = alpha.value, translationY = y.value)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(25.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    item.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.26f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PermissionCard(
    item: PermItem,
    state: PermState,
    index: Int,
    onClick: () -> Unit
) {
    val isGranted = state == PermState.GRANTED
    val isDenied = state == PermState.DENIED
    val isChecking = state == PermState.CHECKING

    val containerColor by androidx.compose.animation.animateColorAsState(
        targetValue = when {
            isGranted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
            isDenied -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f)
        },
        animationSpec = tween(330, easing = FastOutSlowInEasing),
        label = "perm_container_${item.permissionType}"
    )
    val accentColor by androidx.compose.animation.animateColorAsState(
        targetValue = when {
            isGranted -> MaterialTheme.colorScheme.primary
            isDenied -> MaterialTheme.colorScheme.error
            isChecking -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(330, easing = FastOutSlowInEasing),
        label = "perm_accent_${item.permissionType}"
    )

    val alpha = remember { Animatable(0f) }
    val y = remember { Animatable(18f) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        delay(index * 55L)
        launch { alpha.animateTo(1f, tween(300, easing = FastOutSlowInEasing)) }
        launch { y.animateTo(0f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)) }
    }

    LaunchedEffect(state) {
        if (isGranted) {
            scale.animateTo(1.018f, tween(120, easing = FastOutSlowInEasing))
            scale.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (isGranted || isDenied) 0.34f else 0.16f)),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
            .graphicsLayer(alpha = alpha.value, translationY = y.value)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        (scaleIn(tween(180)) + fadeIn(tween(180))) togetherWith
                            (scaleOut(tween(120)) + fadeOut(tween(120)))
                    },
                    label = "perm_icon_${item.permissionType}"
                ) { target ->
                    Icon(
                        imageVector = if (target == PermState.GRANTED) Icons.Outlined.CheckCircle else item.icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.required) {
                        StatusChip("Wajib", MaterialTheme.colorScheme.error)
                    }
                }

                AnimatedContent(
                    targetState = when (state) {
                        PermState.CHECKING -> "Memeriksa izin..."
                        PermState.GRANTED -> "Aktif dan siap digunakan"
                        PermState.DENIED -> if (item.required) "Belum diberikan" else "Dilewati atau belum aktif"
                        PermState.IDLE -> item.desc
                    },
                    transitionSpec = {
                        (slideInVertically { it / 3 } + fadeIn(tween(180))) togetherWith
                            (slideOutVertically { -it / 3 } + fadeOut(tween(140)))
                    },
                    label = "perm_desc_${item.permissionType}"
                ) { text ->
                    Text(
                        text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            val pulse = rememberInfiniteTransition(label = "perm_pulse_${item.permissionType}")
            val pulseScale by pulse.animateFloat(
                initialValue = 0.92f,
                targetValue = if (isChecking) 1.35f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(760, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
            )
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (isChecking) 0.50f else 0.92f))
            )
        }
    }
}

@Composable
private fun PermissionSummaryCard(granted: Int, total: Int, rootOk: Boolean) {
    val progress = if (total == 0) 0f else granted.toFloat() / total.toFloat()

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$granted/$total",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 15.sp
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Permission Setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (rootOk) "Root sudah aktif. Selesaikan izin tambahan agar monitoring, profil, dan optimasi berjalan stabil."
                        else "Root wajib aktif untuk membuka mode performa, tweak sistem, dan kontrol penuh Aether Manager.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
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
private fun DetailCard(item: FeatureItem, index: Int) {
    FeatureCard(item = item, index = index)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AetherNextButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val inf = rememberInfiniteTransition(label = "next_button_motion")
    val arrowX by inf.animateFloat(
        initialValue = 0f,
        targetValue = if (enabled) 5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "next_arrow_x"
    )
    val glow by animateFloatAsState(
        targetValue = if (enabled) 0.26f else 0.08f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "next_glow"
    )

    Box(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = glow))
        )
        FilledTonalButton(
            onClick = {
                scope.launch {
                    scale.animateTo(0.985f, tween(95, easing = FastOutSlowInEasing))
                    scale.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
                }
                onClick()
            },
            enabled = enabled,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .scale(scale.value)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = text,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInHorizontally { it / 5 }) togetherWith
                            (fadeOut(tween(160)) + slideOutHorizontally { -it / 5 })
                    },
                    label = "next_text"
                ) { label ->
                    Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer(translationX = arrowX)
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SetupScreen(onDone: (rootWasGranted: Boolean) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val density = LocalDensity.current

    var rootState by remember { mutableStateOf(PermState.IDLE) }
    var notifState by remember { mutableStateOf(PermState.IDLE) }
    var writeState by remember { mutableStateOf(PermState.IDLE) }
    var storState by remember { mutableStateOf(PermState.IDLE) }
    var batteryState by remember { mutableStateOf(PermState.IDLE) }
    var usageState by remember { mutableStateOf(PermState.IDLE) }
    var accessState by remember { mutableStateOf(PermState.IDLE) }

    val includeStorage = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifState = if (granted) PermState.GRANTED else PermState.DENIED }

    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> storState = if (granted) PermState.GRANTED else PermState.DENIED }

    val writeSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { writeState = if (Settings.System.canWrite(ctx)) PermState.GRANTED else PermState.DENIED }

    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { batteryState = if (isBatteryOptimizationIgnored(ctx)) PermState.GRANTED else PermState.DENIED }

    val usageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { usageState = if (isUsageStatsGranted(ctx)) PermState.GRANTED else PermState.DENIED }

    val accessibilityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { accessState = if (isAccessibilityEnabled(ctx)) PermState.GRANTED else PermState.DENIED }

    fun refreshPermissionStates() {
        val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true

        if (notifState != PermState.IDLE || notifOk) notifState = if (notifOk) PermState.GRANTED else notifState
        if (writeState != PermState.IDLE && Settings.System.canWrite(ctx)) writeState = PermState.GRANTED
        if (batteryState != PermState.IDLE && isBatteryOptimizationIgnored(ctx)) batteryState = PermState.GRANTED
        if (usageState != PermState.IDLE && isUsageStatsGranted(ctx)) usageState = PermState.GRANTED
        if (accessState != PermState.IDLE) accessState = if (isAccessibilityEnabled(ctx)) PermState.GRANTED else PermState.DENIED

        if (includeStorage && storState != PermState.IDLE) {
            val ok = ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (ok) storState = PermState.GRANTED
        }
    }

    OnLifecycleResume { refreshPermissionStates() }

    val permItems = remember(includeStorage) {
        buildList {
            add(
                PermItem(
                    Icons.Outlined.AdminPanelSettings,
                    "Akses Root",
                    "Aktifkan kontrol performa tingkat sistem.",
                    "ROOT",
                    required = true
                )
            )
            add(
                PermItem(
                    Icons.Outlined.NotificationsActive,
                    "Notifikasi",
                    "Info status optimasi, peringatan, dan proses background.",
                    "NOTIFICATION"
                )
            )
            if (includeStorage) {
                add(
                    PermItem(
                        Icons.Outlined.FolderOpen,
                        "Penyimpanan",
                        "Simpan konfigurasi, profil, dan log aplikasi.",
                        "STORAGE"
                    )
                )
            }
            add(
                PermItem(
                    Icons.Outlined.BatteryChargingFull,
                    "Optimasi Baterai",
                    "Cegah sistem membatasi proses Aether Manager.",
                    "BATTERY"
                )
            )
            add(
                PermItem(
                    Icons.Outlined.QueryStats,
                    "Akses Penggunaan",
                    "Baca statistik aplikasi untuk mode per-aplikasi.",
                    "USAGE_STATS"
                )
            )
            add(
                PermItem(
                    Icons.Outlined.Tune,
                    "Pengaturan Sistem",
                    "Terapkan tweak sistem, layar, dan performa.",
                    "WRITE_SETTINGS"
                )
            )
        }
    }

    fun PermState.decided() = this == PermState.GRANTED || this == PermState.DENIED
    val rootOk = rootState == PermState.GRANTED
    val allDecided = rootOk &&
        notifState.decided() &&
        batteryState.decided() &&
        usageState.decided() &&
        writeState.decided() &&
        (!includeStorage || storState.decided())

    val totalPermissions = permItems.size
    val grantedPermissions = permItems.count { item ->
        when (item.permissionType) {
            "ROOT" -> rootState == PermState.GRANTED
            "NOTIFICATION" -> notifState == PermState.GRANTED
            "WRITE_SETTINGS" -> writeState == PermState.GRANTED
            "STORAGE" -> storState == PermState.GRANTED
            "BATTERY" -> batteryState == PermState.GRANTED
            "USAGE_STATS" -> usageState == PermState.GRANTED
            else -> false
        }
    }

    val totalPages = 3
    val pagerState = rememberPagerState { totalPages }
    val currentPage = pagerState.currentPage
    val canProceed = currentPage != 1 || allDecided

    fun nextPage() = scope.launch {
        pagerState.animateScrollToPage(
            page = currentPage + 1,
            animationSpec = tween(620, easing = FastOutSlowInEasing)
        )
    }

    fun prevPage() = scope.launch {
        pagerState.animateScrollToPage(
            page = currentPage - 1,
            animationSpec = tween(520, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(currentPage) {
        if (currentPage == 1) {
            refreshPermissionStates()
            if (writeState == PermState.IDLE && Settings.System.canWrite(ctx)) writeState = PermState.GRANTED
            if (batteryState == PermState.IDLE && isBatteryOptimizationIgnored(ctx)) batteryState = PermState.GRANTED
            if (usageState == PermState.IDLE && isUsageStatsGranted(ctx)) usageState = PermState.GRANTED
            if (accessState == PermState.IDLE && isAccessibilityEnabled(ctx)) accessState = PermState.GRANTED
            if (includeStorage && storState == PermState.IDLE) {
                val ok = ContextCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                if (ok) storState = PermState.GRANTED
            }
        }
    }

    val screenAlpha = remember { Animatable(0f) }
    val screenY = remember { Animatable(24f) }
    LaunchedEffect(Unit) {
        launch { screenAlpha.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        launch { screenY.animateTo(0f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)) }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        SetupBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .graphicsLayer(
                        alpha = screenAlpha.value,
                        translationY = with(density) { screenY.value.dp.toPx() }
                    ),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                SetupHeader()

                HorizontalPager(
                    state = pagerState,
                    pageSize = PageSize.Fill,
                    beyondViewportPageCount = 1,
                    userScrollEnabled = canProceed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { idx ->
                    val pageOffset = (pagerState.currentPage - idx) + pagerState.currentPageOffsetFraction
                    val absOffset = kotlin.math.abs(pageOffset).coerceIn(0f, 1f)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                            .padding(top = 4.dp, bottom = 18.dp)
                            .graphicsLayer {
                                translationX = pageOffset * size.width * 0.08f
                                alpha = lerp(1f, 0.50f, absOffset)
                                scaleX = lerp(1f, 0.97f, absOffset)
                                scaleY = lerp(1f, 0.97f, absOffset)
                            }
                    ) {
                        when (idx) {
                            0 -> WelcomePage(s)
                            1 -> PermissionsPage(
                                s = s,
                                permItems = permItems,
                                granted = grantedPermissions,
                                total = totalPermissions,
                                rootState = rootState,
                                notifState = notifState,
                                writeState = writeState,
                                storageState = storState,
                                batteryState = batteryState,
                                usageState = usageState,
                                accessState = accessState,
                                onAction = { permType ->
                                    when (permType) {
                                        "ROOT" -> scope.launch {
                                            rootState = PermState.CHECKING
                                            val ok = RootManager.requestRoot()
                                            rootState = if (ok) PermState.GRANTED else PermState.DENIED
                                        }

                                        "NOTIFICATION" -> {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                notifState = PermState.GRANTED
                                            }
                                        }

                                        "WRITE_SETTINGS" -> {
                                            if (Settings.System.canWrite(ctx)) {
                                                writeState = PermState.GRANTED
                                            } else {
                                                writeSettingsLauncher.launch(
                                                    Intent(
                                                        Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                                        Uri.parse("package:${ctx.packageName}")
                                                    )
                                                )
                                            }
                                        }

                                        "STORAGE" -> {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                storState = PermState.GRANTED
                                            } else {
                                                val ok = ContextCompat.checkSelfPermission(
                                                    ctx,
                                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                                ) == PackageManager.PERMISSION_GRANTED

                                                if (ok) storState = PermState.GRANTED
                                                else storageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                            }
                                        }

                                        "BATTERY" -> {
                                            if (isBatteryOptimizationIgnored(ctx)) {
                                                batteryState = PermState.GRANTED
                                            } else {
                                                batteryLauncher.launch(
                                                    Intent(
                                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                        Uri.parse("package:${ctx.packageName}")
                                                    )
                                                )
                                            }
                                        }

                                        "USAGE_STATS" -> {
                                            if (isUsageStatsGranted(ctx)) {
                                                usageState = PermState.GRANTED
                                            } else {
                                                usageState = PermState.CHECKING
                                                scope.launch(Dispatchers.IO) {
                                                    val granted = grantUsageStatsViaRoot(ctx.packageName)
                                                    withContext(Dispatchers.Main) {
                                                        if (granted && isUsageStatsGranted(ctx)) {
                                                            usageState = PermState.GRANTED
                                                        } else {
                                                            usageState = PermState.IDLE
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
                                                }
                                            }
                                        }

                                        "ACCESSIBILITY" -> {
                                            accessState = PermState.CHECKING
                                            accessibilityLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                        }
                                    }
                                }
                            )
                            2 -> DonePage(s, allGranted = allDecided)
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 28.dp)
                ) {
                    PagerDotIndicator(total = totalPages, current = currentPage)

                    AetherNextButton(
                        text = if (currentPage == totalPages - 1) s.setupBtnStart else s.setupBtnNext,
                        enabled = canProceed,
                        onClick = {
                            if (currentPage == totalPages - 1) onDone(rootState == PermState.GRANTED)
                            else nextPage()
                        }
                    )

                    AnimatedVisibility(
                        visible = !canProceed && currentPage == 1,
                        enter = fadeIn(tween(220)) + slideInVertically { it / 2 },
                        exit = fadeOut(tween(160)) + slideOutVertically { it / 2 }
                    ) {
                        val pendingPerms = buildList {
                            if (!rootOk) add("Akses Root")
                            if (notifState == PermState.IDLE) add("Notifikasi")
                            if (batteryState == PermState.IDLE) add("Optimasi Baterai")
                            if (usageState == PermState.IDLE) add("Akses Penggunaan")
                            if (writeState == PermState.IDLE) add("Pengaturan Sistem")
                            if (includeStorage && storState == PermState.IDLE) add("Penyimpanan")
                        }
                        Text(
                            if (!rootOk) s.setupRootRequired
                            else "Ketuk izin yang belum diputuskan: ${pendingPerms.joinToString(", ")}",
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.88f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }

                    AnimatedVisibility(
                        visible = currentPage > 0,
                        enter = fadeIn(tween(220)) + slideInVertically { it / 2 },
                        exit = fadeOut(tween(160)) + slideOutVertically { it / 2 }
                    ) {
                        TextButton(onClick = { prevPage() }) {
                            Icon(
                                imageVector = Icons.Outlined.ChevronLeft,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                s.setupBtnBack,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePage(s: AppStrings) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            s.setupWelcomeTitle,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 40.sp
        )
        Text(
            "Setup cepat untuk mengaktifkan root mode, monitoring perangkat, profil performa, dan optimasi baterai dengan tampilan yang lebih rapi.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        HeroFeatureSpotlight()

        Text(
            "Fitur Unggulan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )

        listOf(
            FeatureItem(
                Icons.Outlined.Speed,
                "Smart Performance",
                "Profil performa untuk menjaga CPU, GPU, dan scheduler tetap responsif saat digunakan."
            ),
            FeatureItem(
                Icons.Outlined.BatteryChargingFull,
                "Battery Control",
                "Mengurangi limit background agar proses penting Aether Manager tetap berjalan stabil."
            ),
            FeatureItem(
                Icons.Outlined.SportsEsports,
                "Game Mode",
                "Prioritaskan resource untuk game dan kurangi gangguan proses latar belakang."
            )
        ).forEachIndexed { index, item ->
            FeatureCard(item = item, index = index)
        }
    }
}

@Composable
private fun PermissionsPage(
    s: AppStrings,
    permItems: List<PermItem>,
    granted: Int,
    total: Int,
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
        "ROOT" -> rootState
        "NOTIFICATION" -> notifState
        "WRITE_SETTINGS" -> writeState
        "STORAGE" -> storageState
        "BATTERY" -> batteryState
        "USAGE_STATS" -> usageState
        "ACCESSIBILITY" -> accessState
        else -> PermState.IDLE
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Izin Aplikasi",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Aktifkan izin inti secara bertahap. Root wajib untuk mode performa, sedangkan izin tambahan membantu monitoring dan optimasi berjalan lebih stabil.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        PermissionSummaryCard(granted = granted, total = total, rootOk = rootState == PermState.GRANTED)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusChip("Root wajib", MaterialTheme.colorScheme.error)
            StatusChip("Ketuk kartu izin", MaterialTheme.colorScheme.primary)
            StatusChip("Swipe untuk pindah", MaterialTheme.colorScheme.tertiary)
        }

        permItems.forEachIndexed { index, item ->
            PermissionCard(
                item = item,
                state = stateFor(item.permissionType),
                index = index,
                onClick = { onAction(item.permissionType) }
            )
        }

        AnimatedVisibility(
            visible = rootState == PermState.DENIED,
            enter = fadeIn(tween(220)) + slideInVertically { it / 2 },
            exit = fadeOut(tween(160))
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.28f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        s.setupRootDenied,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DonePage(s: AppStrings, allGranted: Boolean) {
    val iconScale = remember { Animatable(0.72f) }
    val iconAlpha = remember { Animatable(0f) }
    val inf = rememberInfiniteTransition(label = "done_glow")
    val glow by inf.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.62f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "done_glow_alpha"
    )

    LaunchedEffect(Unit) {
        launch { iconScale.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
        launch { iconAlpha.animateTo(1f, tween(360, easing = FastOutSlowInEasing)) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .size(128.dp)
                .scale(iconScale.value)
                .graphicsLayer(alpha = iconAlpha.value)
                .clip(CircleShape)
                .background(
                    if (allGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f)
                )
                .border(
                    BorderStroke(
                        1.dp,
                        if (allGranted) MaterialTheme.colorScheme.primary.copy(alpha = glow)
                        else MaterialTheme.colorScheme.error.copy(alpha = glow)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (allGranted) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                contentDescription = null,
                tint = if (allGranted) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(60.dp)
            )
        }

        Text(
            if (allGranted) "Setup Complete" else s.setupIncompleteTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 32.sp
        )
        Text(
            if (allGranted) "Aether Manager sudah siap digunakan. Root mode aktif, izin utama selesai, dan fitur monitoring siap berjalan di background."
            else "Beberapa izin belum selesai. Kembali ke halaman izin lalu aktifkan kartu yang masih belum aktif.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
            modifier = Modifier.widthIn(max = 340.dp)
        )

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = if (allGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f),
            border = BorderStroke(
                1.dp,
                if (allGranted) MaterialTheme.colorScheme.primary.copy(alpha = glow)
                else MaterialTheme.colorScheme.error.copy(alpha = glow)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (allGranted) "Semua sistem siap" else "Setup belum lengkap",
                    fontWeight = FontWeight.Bold,
                    color = if (allGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
                Text(
                    if (allGranted) "Tekan tombol Mulai untuk masuk ke dashboard utama."
                    else "Tombol Mulai baru aktif setelah root dan izin wajib selesai.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        Text(
            "Yang sudah disiapkan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        listOf(
            FeatureItem(
                Icons.Outlined.AdminPanelSettings,
                "Root Mode",
                "Kontrol sistem dan tuning performa sudah bisa dipakai dari dashboard."
            ),
            FeatureItem(
                Icons.Outlined.Tune,
                "Optimasi Stabil",
                "Profil performa, baterai, dan monitoring siap berjalan lebih konsisten."
            ),
            FeatureItem(
                Icons.Outlined.QueryStats,
                "Monitoring Siap",
                "Status perangkat, aplikasi, dan proses background bisa dipantau lebih rapi."
            )
        ).forEachIndexed { index, item ->
            DetailCard(item = item, index = index)
        }
    }
}
