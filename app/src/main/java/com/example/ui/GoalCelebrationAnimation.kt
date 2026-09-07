package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance data structure for celebration particles:
 * - Water splash droplets (arch upward, fall with gravity)
 * - Ascending buoyant bubbles (wobble with sine wave)
 * - Shimmering confetti diamonds / stars (rotate and flutter)
 * - Expanding liquid ripple shockwaves
 */
private class CelebrationParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    val color: Color,
    val type: ParticleType,
    var rotation: Float = 0f,
    val rotationSpeed: Float = 0f,
    var alpha: Float = 1f,
    var life: Float = 1f,
    val maxLife: Float = 1f,
    val wobblePhase: Float = 0f
)

private enum class ParticleType {
    WATER_DROPLET,
    BUOYANT_BUBBLE,
    GOLDEN_STAR,
    CONFETTI_DIAMOND,
    RIPPLE_RING
}

/**
 * High-performance hardware-accelerated Canvas Particle Engine for Daily Hydration Celebrations.
 * Uses a smooth 60fps coroutine physics loop.
 */
@Composable
fun GoalCelebrationParticleEngine(
    modifier: Modifier = Modifier,
    burstTrigger: Int = 0,
    isDarkTheme: Boolean = false
) {
    val random = remember { Random() }
    val particles = remember { mutableStateListOf<CelebrationParticle>() }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Vibrant water-inspired celebratory color palette
    val palette = remember(isDarkTheme) {
        listOf(
            Color(0xFF00E5FF), // Neon Aqua
            Color(0xFF00B0FF), // Pure Sky Blue
            Color(0xFF0091EA), // Deep Water Blue
            Color(0xFF80D8FF), // Crystal Ice Blue
            Color(0xFF64FFDA), // Mint Marine Aqua
            Color(0xFFFFD700), // Amber Gold
            Color(0xFFFFC107), // Golden Sunlight
            Color(0xFFFFF9C4), // Champagne Shimmer
            Color(0xFFFFFFFF)  // Pure Droplet Sparkle
        )
    }

    // Function to spawn an explosive water burst
    fun spawnWaterCelebrationBurst(width: Float, height: Float, count: Int = 85) {
        if (width <= 0 || height <= 0) return
        val originX = width * 0.5f
        val originY = height * 0.45f

        // 1. Concentric Ripple Rings
        for (i in 0..2) {
            particles.add(
                CelebrationParticle(
                    x = originX,
                    y = originY,
                    vx = 0f,
                    vy = 0f,
                    size = 10f + i * 20f,
                    color = Color(0xFF00E5FF),
                    type = ParticleType.RIPPLE_RING,
                    alpha = 0.8f - (i * 0.15f),
                    life = 1f,
                    maxLife = 1f + (i * 0.2f)
                )
            )
        }

        // 2. Upward Water Splash Droplets & Fountain
        val dropletCount = (count * 0.45f).toInt()
        for (i in 0 until dropletCount) {
            val angle = -Math.PI / 2.0 + (random.nextDouble() - 0.5) * (Math.PI * 0.85)
            val speed = 9f + random.nextFloat() * 18f
            val vx = (cos(angle) * speed).toFloat()
            val vy = (sin(angle) * speed).toFloat()
            val size = 5f + random.nextFloat() * 8f
            val color = palette[random.nextInt(5)] // Aqua palette

            particles.add(
                CelebrationParticle(
                    x = originX + (random.nextFloat() - 0.5f) * 40f,
                    y = originY + (random.nextFloat() - 0.5f) * 20f,
                    vx = vx,
                    vy = vy,
                    size = size,
                    color = color,
                    type = ParticleType.WATER_DROPLET,
                    rotation = random.nextFloat() * 360f,
                    life = 1f,
                    maxLife = 1f + random.nextFloat() * 0.5f
                )
            )
        }

        // 3. Ascending Buoyant Bubbles with shimmer
        val bubbleCount = (count * 0.25f).toInt()
        for (i in 0 until bubbleCount) {
            val vx = (random.nextFloat() - 0.5f) * 4f
            val vy = -2.5f - random.nextFloat() * 4.5f
            val size = 8f + random.nextFloat() * 14f
            val color = if (random.nextBoolean()) Color(0xFF80D8FF) else Color(0xFF64FFDA)

            particles.add(
                CelebrationParticle(
                    x = originX + (random.nextFloat() - 0.5f) * (width * 0.7f),
                    y = originY + random.nextFloat() * (height * 0.35f),
                    vx = vx,
                    vy = vy,
                    size = size,
                    color = color,
                    type = ParticleType.BUOYANT_BUBBLE,
                    alpha = 0.85f,
                    wobblePhase = random.nextFloat() * (2 * Math.PI.toFloat()),
                    life = 1f,
                    maxLife = 1.2f + random.nextFloat() * 0.8f
                )
            )
        }

        // 4. Golden Stars & Confetti Diamonds
        val confettiCount = (count * 0.3f).toInt()
        for (i in 0 until confettiCount) {
            val angle = random.nextDouble() * 2 * Math.PI
            val speed = 4f + random.nextFloat() * 12f
            val vx = (cos(angle) * speed).toFloat()
            val vy = (sin(angle) * speed).toFloat() - 4f
            val size = 6f + random.nextFloat() * 8f
            val isStar = random.nextBoolean()
            val color = if (isStar) palette[5 + random.nextInt(3)] else palette[random.nextInt(palette.size)]

            particles.add(
                CelebrationParticle(
                    x = originX,
                    y = originY,
                    vx = vx,
                    vy = vy,
                    size = size,
                    color = color,
                    type = if (isStar) ParticleType.GOLDEN_STAR else ParticleType.CONFETTI_DIAMOND,
                    rotation = random.nextFloat() * 360f,
                    rotationSpeed = (random.nextFloat() - 0.5f) * 12f,
                    life = 1f,
                    maxLife = 1f + random.nextFloat() * 0.6f
                )
            )
        }
    }

    // Trigger burst on canvas resize or trigger increment
    LaunchedEffect(canvasSize, burstTrigger) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            spawnWaterCelebrationBurst(canvasSize.width, canvasSize.height)
        }
    }

    // 60FPS Physics Simulation Loop
    var frameClock by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { frameNanos ->
                frameClock = frameNanos

                val iter = particles.iterator()
                while (iter.hasNext()) {
                    val p = iter.next()

                    when (p.type) {
                        ParticleType.RIPPLE_RING -> {
                            // Expand ripple radius and fade alpha
                            p.size += 5.5f
                            p.alpha -= 0.016f
                            if (p.alpha <= 0f) {
                                iter.remove()
                                continue
                            }
                        }
                        ParticleType.WATER_DROPLET -> {
                            p.x += p.vx
                            p.y += p.vy
                            p.vy += 0.42f // Gravity
                            p.vx *= 0.985f // Drag
                            p.alpha = (p.alpha - 0.012f).coerceAtLeast(0f)
                            if (p.alpha <= 0f || p.y > canvasSize.height + 50f) {
                                iter.remove()
                                continue
                            }
                        }
                        ParticleType.BUOYANT_BUBBLE -> {
                            p.x += p.vx + sin(frameNanos * 0.000000004f + p.wobblePhase) * 0.8f
                            p.y += p.vy
                            p.alpha = (p.alpha - 0.008f).coerceAtLeast(0f)
                            if (p.alpha <= 0f || p.y < -50f) {
                                iter.remove()
                                continue
                            }
                        }
                        ParticleType.GOLDEN_STAR, ParticleType.CONFETTI_DIAMOND -> {
                            p.x += p.vx
                            p.y += p.vy
                            p.vy += 0.22f // Gentle gravity
                            p.vx *= 0.98f // Air resistance
                            p.rotation += p.rotationSpeed
                            p.alpha = (p.alpha - 0.011f).coerceAtLeast(0f)
                            if (p.alpha <= 0f || p.y > canvasSize.height + 50f) {
                                iter.remove()
                                continue
                            }
                        }
                    }
                }
            }
        }
    }

    // Reusable Path instances to avoid allocations during 120fps particle rendering
    val dropPath = remember { Path() }
    val starPath = remember { Path() }
    val diamondPath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { }
    ) {
        if (canvasSize != size) {
            canvasSize = size
        }

        // Draw all active particles
        particles.forEach { p ->
            when (p.type) {
                ParticleType.RIPPLE_RING -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                p.color.copy(alpha = 0f),
                                p.color.copy(alpha = p.alpha * 0.4f),
                                p.color.copy(alpha = p.alpha * 0.8f),
                                Color.Transparent
                            ),
                            center = Offset(p.x, p.y),
                            radius = p.size
                        ),
                        radius = p.size,
                        center = Offset(p.x, p.y),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
                ParticleType.WATER_DROPLET -> {
                    // Draw teardrop or rounded water droplet
                    val r = p.size
                    dropPath.reset()
                    dropPath.moveTo(p.x, p.y - r * 1.4f)
                    dropPath.quadraticTo(p.x + r, p.y, p.x + r * 0.8f, p.y + r * 0.8f)
                    dropPath.quadraticTo(p.x, p.y + r * 1.5f, p.x - r * 0.8f, p.y + r * 0.8f)
                    dropPath.quadraticTo(p.x - r, p.y, p.x, p.y - r * 1.4f)
                    dropPath.close()

                    drawPath(
                        path = dropPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = p.alpha * 0.9f),
                                p.color.copy(alpha = p.alpha * 0.85f)
                            )
                        ),
                        style = Fill
                    )
                }
                ParticleType.BUOYANT_BUBBLE -> {
                    // Outer translucent bubble ring
                    drawCircle(
                        color = p.color.copy(alpha = p.alpha * 0.6f),
                        radius = p.size,
                        center = Offset(p.x, p.y),
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                    // Inner glowing tint
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                p.color.copy(alpha = p.alpha * 0.25f),
                                Color.Transparent
                            ),
                            center = Offset(p.x, p.y),
                            radius = p.size
                        ),
                        radius = p.size,
                        center = Offset(p.x, p.y)
                    )
                    // Specular glare spot
                    drawCircle(
                        color = Color.White.copy(alpha = p.alpha * 0.85f),
                        radius = p.size * 0.28f,
                        center = Offset(p.x - p.size * 0.35f, p.y - p.size * 0.35f)
                    )
                }
                ParticleType.GOLDEN_STAR -> {
                    rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
                        val r = p.size
                        starPath.reset()
                        starPath.moveTo(p.x, p.y - r * 1.3f)
                        starPath.quadraticTo(p.x, p.y, p.x + r * 1.3f, p.y)
                        starPath.quadraticTo(p.x, p.y, p.x, p.y + r * 1.3f)
                        starPath.quadraticTo(p.x, p.y, p.x - r * 1.3f, p.y)
                        starPath.quadraticTo(p.x, p.y, p.x, p.y - r * 1.3f)
                        starPath.close()

                        drawPath(
                            path = starPath,
                            color = p.color.copy(alpha = p.alpha),
                            style = Fill
                        )
                    }
                }
                ParticleType.CONFETTI_DIAMOND -> {
                    rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
                        val w = p.size * 0.9f
                        val h = p.size * 1.4f
                        diamondPath.reset()
                        diamondPath.moveTo(p.x, p.y - h)
                        diamondPath.lineTo(p.x + w, p.y)
                        diamondPath.lineTo(p.x, p.y + h)
                        diamondPath.lineTo(p.x - w, p.y)
                        diamondPath.close()

                        drawPath(
                            path = diamondPath,
                            color = p.color.copy(alpha = p.alpha),
                            style = Fill
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full-screen fluid celebration overlay dialog that activates when the user hits 100% daily goal.
 * Features:
 * - Liquid glassmorphic card with dynamic lighting
 * - Animated celebratory chalice/trophy with glowing radial aura
 * - Particle physics simulation with water droplets and buoyant bubbles
 * - Milestone statistics (Total volume, Streak multiplier, XP reward)
 * - Interactive "Celebrate Again" action with burst feedback
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HydrationGoalCelebrationOverlay(
    totalIntakeMl: Int,
    dailyGoalMl: Int,
    currentStreak: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var burstCount by remember { mutableIntStateOf(1) }
    var isVisible by remember { mutableStateOf(false) }

    // Trigger celebratory rhythmic haptic pulses
    LaunchedEffect(Unit) {
        isVisible = true
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        delay(140)
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        delay(180)
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "celebrationAura")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF031A2F).copy(alpha = 0.92f),
                            Color(0xFF000B16).copy(alpha = 0.98f)
                        )
                    )
                )
                .testTag("celebration_overlay"),
            contentAlignment = Alignment.Center
        ) {
            // Background Canvas Particle System
            GoalCelebrationParticleEngine(
                burstTrigger = burstCount,
                isDarkTheme = true,
                modifier = Modifier.fillMaxSize()
            )

            // Animated Celebration Center Card
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(400)) + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)),
                exit = fadeOut(tween(300)) + scaleOut(tween(300))
            ) {
                Card(
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0D2538).copy(alpha = 0.94f)
                    ),
                    border = BorderStroke(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFF00E5FF),
                                Color(0xFFFFD700),
                                Color(0xFF00B0FF),
                                Color(0xFF64FFDA),
                                Color(0xFF00E5FF)
                            )
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 440.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(32.dp),
                            spotColor = Color(0xFF00E5FF).copy(alpha = 0.6f)
                        )
                        .testTag("celebration_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // 1. Water Trophy Crest with Radiant Halo
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .scale(pulseScale),
                            contentAlignment = Alignment.Center
                        ) {
                            // Rotating radiant rays
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                rotate(rotationAngle) {
                                    val rayCount = 12
                                    for (i in 0 until rayCount) {
                                        val angleRad = (i * (360f / rayCount) * Math.PI / 180.0)
                                        val length = size.width * 0.48f
                                        val endX = center.x + (cos(angleRad) * length).toFloat()
                                        val endY = center.y + (sin(angleRad) * length).toFloat()

                                        drawLine(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF00E5FF).copy(alpha = glowAlpha * 0.7f),
                                                    Color(0xFFFFD700).copy(alpha = 0f)
                                                ),
                                                start = center,
                                                end = Offset(endX, endY)
                                            ),
                                            start = center,
                                            end = Offset(endX, endY),
                                            strokeWidth = 3.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                            }

                            // Glowing Water Sphere Container
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF00E5FF),
                                                Color(0xFF0072B2)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 2.5.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color.White, Color(0xFFFFD700))
                                        ),
                                        shape = CircleShape
                                    )
                                    .drawBehind {
                                        // Water drop surface highlight arc
                                        drawArc(
                                            color = Color.White.copy(alpha = 0.8f),
                                            startAngle = 190f,
                                            sweepAngle = 80f,
                                            useCenter = false,
                                            topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                                            size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
                                            style = Stroke(width = 3.dp.toPx())
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = 4.dp.toPx(),
                                            center = Offset(size.width * 0.28f, size.height * 0.28f)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Victory Trophy",
                                    tint = Color(0xFFFFF9C4),
                                    modifier = Modifier.size(46.dp)
                                )
                            }
                        }

                        // 2. Headings & Copy
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "GOAL REACHED",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = Color(0xFF00E5FF),
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "100% Daily Target Met",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "You have met your daily hydration goal. Great job maintaining a consistent habit today.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        // 3. Stats & Milestone Badges Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF061421).copy(alpha = 0.8f))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Volume Stat
                            CelebrationStatPill(
                                icon = Icons.Default.WaterDrop,
                                label = "Total Drank",
                                value = "${totalIntakeMl} ml",
                                iconColor = Color(0xFF00E5FF)
                            )

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(Color.White.copy(alpha = 0.15f))
                            )

                            // Streak Stat
                            CelebrationStatPill(
                                icon = Icons.Default.Whatshot,
                                label = "Daily Streak",
                                value = "$currentStreak Days",
                                iconColor = Color(0xFFFF7043)
                            )

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(Color.White.copy(alpha = 0.15f))
                            )

                            // XP Reward Stat
                            CelebrationStatPill(
                                icon = Icons.Default.Star,
                                label = "aqora XP",
                                value = "+50 XP",
                                iconColor = Color(0xFFFFD700)
                            )
                        }

                        // 4. Interactive Action Buttons
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Primary "Celebrate Again" Interactive Particle Burst Button
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    burstCount++
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00E5FF),
                                    contentColor = Color(0xFF001B2E)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("btn_celebrate_again")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Celebration,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Celebrate Again 🎆",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            // Secondary "Keep Flowing" Dismiss Button
                            OutlinedButton(
                                onClick = { onDismiss() },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_dismiss_celebration")
                            ) {
                                Text(
                                    text = "Keep Flowing 🌊",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CelebrationStatPill(
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

/**
 * Glowing celebration banner embedded right in the Home Tab
 * Displays when the daily goal has been conquered, allowing the user to view celebratory flair or replay it.
 */
@Composable
fun HomeGoalCelebrationBanner(
    modifier: Modifier = Modifier,
    onReplayCelebration: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bannerGlow")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF00223D).copy(alpha = 0.75f),
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF00E5FF).copy(alpha = shimmerAlpha),
                    Color(0xFFFFD700).copy(alpha = shimmerAlpha * 0.8f),
                    Color(0xFF00E5FF).copy(alpha = shimmerAlpha)
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onReplayCelebration != null) Modifier.clickable { onReplayCelebration() } else Modifier
            )
            .testTag("banner_goal_celebration")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF00E5FF).copy(alpha = 0.35f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Daily Goal Accomplished",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Daily Goal Completed! 🎉",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF00E5FF)
                    )
                }
                Text(
                    text = "Daily target reached! Keep up the great hydration.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = Icons.Default.Celebration,
                contentDescription = "Celebrate",
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
