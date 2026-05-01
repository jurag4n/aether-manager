package dev.aether.manager

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aether.manager.i18n.ProvideStrings
import dev.aether.manager.ui.AetherTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("aether_prefs", Context.MODE_PRIVATE)
        val setupDone = prefs.getBoolean("setup_done", false)

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
                    SplashScreen {
                        val next = if (setupDone) MainActivity::class.java else SetupActivity::class.java
                        startActivity(Intent(this, next))
                        finish()
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current

    val backgroundAlpha = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.55f) }
    val logoRotate = remember { Animatable(-8f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(28f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val subtitleOffset = remember { Animatable(18f) }
    val chipAlpha = remember { Animatable(0f) }
    val progressAlpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }
    val versionAlpha = remember { Animatable(0f) }

    val infinite = rememberInfiniteTransition(label = "splash_motion")
    val pulse by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_pulse"
    )
    val aura by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aura_rotate"
    )
    val zigzagFlow by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "zigzag_flow"
    )

    LaunchedEffect(Unit) {
        launch { backgroundAlpha.animateTo(1f, tween(300)) }

        delay(120)
        launch { logoAlpha.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        launch {
            logoScale.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch { logoRotate.animateTo(0f, tween(680, easing = FastOutSlowInEasing)) }

        delay(430)
        launch { titleAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
        launch {
            titleOffset.animateTo(
                0f,
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        delay(240)
        launch { subtitleAlpha.animateTo(1f, tween(360, easing = FastOutSlowInEasing)) }
        launch {
            subtitleOffset.animateTo(
                0f,
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        delay(230)
        launch { chipAlpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing)) }
        launch { progressAlpha.animateTo(1f, tween(260)) }
        launch { versionAlpha.animateTo(1f, tween(350)) }
        launch { progress.animateTo(1f, tween(2300, easing = FastOutSlowInEasing)) }

        delay(2480)
        onFinished()
    }

    val context = LocalContext.current
    val versionName = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager
                    .getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                    .versionName ?: "dev"
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "dev"
            }
        } catch (_: Exception) {
            "dev"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surface.copy(alpha = backgroundAlpha.value)),
        contentAlignment = Alignment.Center
    ) {
        SplashBackground(
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            alpha = backgroundAlpha.value,
            rotation = aura
        )

        Column(
            modifier = Modifier.padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(
                    modifier = Modifier
                        .size(156.dp)
                        .graphicsLayer {
                            alpha = logoAlpha.value
                            scaleX = logoScale.value * pulse
                            scaleY = logoScale.value * pulse
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                renderEffect = BlurEffect(36f, 36f, TileMode.Decal)
                            }
                        }
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(primary.copy(alpha = 0.46f), Color.Transparent),
                            center = center,
                            radius = size.minDimension * 0.48f
                        ),
                        radius = size.minDimension * 0.46f,
                        center = center
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(secondary.copy(alpha = 0.26f), Color.Transparent),
                            center = center,
                            radius = size.minDimension * 0.34f
                        ),
                        radius = size.minDimension * 0.34f,
                        center = center
                    )
                }

                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .graphicsLayer {
                            alpha = logoAlpha.value
                            scaleX = logoScale.value * pulse
                            scaleY = logoScale.value * pulse
                            rotationZ = logoRotate.value
                        }
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    primary.copy(alpha = 0.20f),
                                    secondary.copy(alpha = 0.11f),
                                    onSurface.copy(alpha = 0.05f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher_round),
                        contentDescription = null,
                        modifier = Modifier
                            .size(82.dp)
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            Text(
                text = "Aether Manager",
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha.value
                    translationY = with(density) { titleOffset.value.dp.toPx() }
                },
                style = TextStyle(
                    brush = Brush.linearGradient(
                        listOf(onSurface, primary, secondary)
                    ),
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Smart Control • Clean Boost • Premium Tools",
                modifier = Modifier.graphicsLayer {
                    alpha = subtitleAlpha.value
                    translationY = with(density) { subtitleOffset.value.dp.toPx() }
                },
                color = onSurface.copy(alpha = 0.58f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .graphicsLayer(alpha = chipAlpha.value)
                    .clip(RoundedCornerShape(100.dp))
                    .background(primary.copy(alpha = 0.10f))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Preparing your workspace",
                    color = primary.copy(alpha = 0.92f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.35.sp
                )
            }

            Spacer(Modifier.height(42.dp))

            ZigZagProgress(
                progress = progress.value,
                flow = zigzagFlow,
                alpha = progressAlpha.value,
                primary = primary,
                secondary = secondary,
                onSurface = onSurface,
                modifier = Modifier
                    .width(236.dp)
                    .height(44.dp)
            )
        }

        Text(
            text = "v$versionName",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .graphicsLayer(alpha = versionAlpha.value),
            color = onSurface.copy(alpha = 0.28f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp
        )
    }
}

@Composable
private fun SplashBackground(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    alpha: Float,
    rotation: Float
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    renderEffect = BlurEffect(82f, 82f, TileMode.Decal)
                }
            }
    ) {
        val min = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f
        val angle = rotation * (PI / 180f).toFloat()

        drawCircle(
            brush = Brush.radialGradient(
                listOf(primary.copy(alpha = 0.26f), Color.Transparent),
                center = Offset(cx + cos(angle) * min * 0.18f, cy + sin(angle) * min * 0.10f),
                radius = min * 0.42f
            ),
            radius = min * 0.42f,
            center = Offset(cx + cos(angle) * min * 0.18f, cy + sin(angle) * min * 0.10f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(secondary.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(cx - sin(angle) * min * 0.16f, cy + cos(angle) * min * 0.13f),
                radius = min * 0.34f
            ),
            radius = min * 0.34f,
            center = Offset(cx - sin(angle) * min * 0.16f, cy + cos(angle) * min * 0.13f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(tertiary.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(cx, cy),
                radius = min * 0.26f
            ),
            radius = min * 0.26f,
            center = Offset(cx, cy)
        )
    }
}

@Composable
private fun ZigZagProgress(
    progress: Float,
    flow: Float,
    alpha: Float,
    primary: Color,
    secondary: Color,
    onSurface: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.graphicsLayer(alpha = alpha)) {
        val startX = 8.dp.toPx()
        val endX = size.width - 8.dp.toPx()
        val centerY = size.height / 2f
        val amplitude = 5.5.dp.toPx()
        val strokeWidth = 4.5.dp.toPx()
        val segments = 10
        val step = (endX - startX) / segments
        val points = ArrayList<Offset>(segments + 1)

        for (i in 0..segments) {
            val x = startX + step * i
            val y = centerY + if (i % 2 == 0) -amplitude else amplitude
            points.add(Offset(x, y))
        }

        fun softZigZagPath(pathPoints: List<Offset>): Path {
            return Path().apply {
                if (pathPoints.isEmpty()) return@apply
                moveTo(pathPoints.first().x, pathPoints.first().y)
                if (pathPoints.size == 1) return@apply

                for (i in 1 until pathPoints.size) {
                    val control = pathPoints[i - 1]
                    val target = pathPoints[i]
                    val mid = Offset(
                        x = (control.x + target.x) / 2f,
                        y = (control.y + target.y) / 2f
                    )
                    quadraticBezierTo(control.x, control.y, mid.x, mid.y)

                    if (i == pathPoints.lastIndex) {
                        quadraticBezierTo(target.x, target.y, target.x, target.y)
                    }
                }
            }
        }

        val track = softZigZagPath(points)

        drawPath(
            path = track,
            color = onSurface.copy(alpha = 0.10f),
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        val totalSegments = segments * progress.coerceIn(0f, 1f)
        val fullSegments = totalSegments.toInt().coerceIn(0, segments)
        val remainder = totalSegments - fullSegments

        val activePoints = ArrayList<Offset>().apply {
            add(points.first())
            for (i in 1..fullSegments) {
                add(points[i])
            }
            if (fullSegments < segments) {
                val a = points[fullSegments]
                val b = points[fullSegments + 1]
                add(
                    Offset(
                        x = a.x + (b.x - a.x) * remainder,
                        y = a.y + (b.y - a.y) * remainder
                    )
                )
            }
        }

        val active = softZigZagPath(activePoints)

        drawPath(
            path = active,
            brush = Brush.linearGradient(
                colors = listOf(primary, secondary, primary),
                start = Offset(size.width * (flow - 1f), 0f),
                end = Offset(size.width * flow, size.height)
            ),
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        val currentIndex = fullSegments.coerceIn(0, segments)
        val currentPoint = if (currentIndex < segments) {
            val a = points[currentIndex]
            val b = points[currentIndex + 1]
            Offset(
                x = a.x + (b.x - a.x) * remainder,
                y = a.y + (b.y - a.y) * remainder
            )
        } else {
            points.last()
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.42f), Color.Transparent),
                center = currentPoint,
                radius = 15.dp.toPx()
            ),
            radius = 15.dp.toPx(),
            center = currentPoint
        )
        drawCircle(
            color = secondary,
            radius = 3.8.dp.toPx(),
            center = currentPoint
        )
    }
}
