package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.isActive
import java.util.Random

// Represents a moving visual celebration particle (confetti/water droplets)
private class InteractiveParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val size: Float,
    val color: Color,
    val isWaterDrop: Boolean = false,
    var rotation: Float = 0f,
    val rotationSpeed: Float = 0f
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RankCelebrationDialog(
    celebration: RankCelebration,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val random = remember { Random() }
    
    // Trigger haptic feedback twice initially to build excitement
    LaunchedEffect(celebration) {
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        kotlinx.coroutines.delay(120)
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    }

    // Scale / Entrance animations
    var animateEntrance by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateEntrance = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "badgeScale")
    val badgeScaleState = infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgeScale"
    )

    val glowOpacityState = infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowOpacity"
    )

    // Setup color palettes based on rank and grandmaster status
    val primaryThemeColor = if (celebration.isGrandmaster) {
        Color(0xFFFFB300) // Beautiful Golden Accent
    } else {
        MaterialTheme.colorScheme.primary
    }

    val secondaryThemeColor = if (celebration.isGrandmaster) {
        Color(0xFFFFD54F)
    } else {
        MaterialTheme.colorScheme.secondary
    }

    val backgroundColor = if (celebration.isGrandmaster) {
        Color(0xFF0F1000) // Golden-tinted extreme dark background
    } else {
        MaterialTheme.colorScheme.background
    }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor.copy(alpha = 0.96f))
        ) {
            // HIGH-PERFORMANCE CANVAS WATER & CONFETTI PARTICLE SYSTEM
            CanvasParticleEngine(
                isGrandmaster = celebration.isGrandmaster,
                primaryColor = primaryThemeColor,
                secondaryColor = secondaryThemeColor
            )

            // Centered dialog card container
            AnimatedVisibility(
                visible = animateEntrance,
                enter = fadeIn(tween(600)) + scaleIn(tween(500, easing = OvershootInterpolator().toEasing())),
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.weight(0.2f))

                    Image(
                        painter = painterResource(id = R.drawable.dailyhydra_logo),
                        contentDescription = "dailyhydra logo",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Sparkle / Star Header row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = primaryThemeColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (celebration.isGrandmaster) "LEGENDARY RANK REACHED" else "RANK PROMOTION",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = primaryThemeColor
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = primaryThemeColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Main Level-up Title
                    Text(
                        text = if (celebration.isGrandmaster) "CELESTIAL FLOW OBTAINED!" else "Promoted to ${celebration.rankName}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            fontSize = if (celebration.isGrandmaster) 30.sp else 28.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Giant badge emblem view with pulsing aura
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(170.dp)
                            .graphicsLayer {
                                scaleX = badgeScaleState.value
                                scaleY = badgeScaleState.value
                            }
                    ) {
                        // Glowing Background Ring
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            listOf(
                                                primaryThemeColor.copy(alpha = glowOpacityState.value),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                }
                        )

                        // Base outer circle
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            primaryThemeColor.copy(alpha = 0.2f),
                                            secondaryThemeColor.copy(alpha = 0.05f)
                                        )
                                    )
                                )
                                .border(
                                    width = 3.dp,
                                    brush = Brush.radialGradient(
                                        listOf(primaryThemeColor, secondaryThemeColor)
                                    ),
                                    shape = CircleShape
                                )
                                .shadow(8.dp, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            RankBadge(
                                rankName = celebration.rankIcon,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // XP Reward Box Design
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = primaryThemeColor.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .border(1.dp, primaryThemeColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Celebration,
                                contentDescription = null,
                                tint = primaryThemeColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+${celebration.xpReward} XP PROMOTION REWARD",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = primaryThemeColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // LFTIME STATISTICS SUMMARY OR EXCLUSIVE TEXT FOR GRANDMASTER
                    if (celebration.isGrandmaster && celebration.lifetimeStats != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "You reached the highest hydration rank!",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = primaryThemeColor
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // 2x2 statistics grid displaying true on-device hydration milestones
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    StatItemCard(
                                        title = "Cumulative Intake",
                                        value = formatVolume(celebration.lifetimeStats.totalVolumeMl),
                                        icon = Icons.Default.LocalDrink,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatItemCard(
                                        title = "Active Tracking",
                                        value = "${celebration.lifetimeStats.activeDays} Days",
                                        icon = Icons.Default.EmojiEvents,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    StatItemCard(
                                        title = "Daily Goals Achieved",
                                        value = "${celebration.lifetimeStats.totalCompletedGoals} Times",
                                        icon = Icons.Default.Star,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatItemCard(
                                        title = "Ultimate Streak",
                                        value = "${celebration.lifetimeStats.bestStreak} Days",
                                        icon = Icons.Default.Celebration,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    } else {
                        // Regular rank congrats description
                        Text(
                            text = "Superb! Your consistent water logging habits are keeping you healthy, hydrated and performing at your peak.",
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(0.3f))

                    // Continue button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryThemeColor,
                            contentColor = if (celebration.isGrandmaster) Color.Black else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(54.dp)
                    ) {
                        Text(
                            text = if (celebration.isGrandmaster) "Uphold the Celestial Flow" else "Continue Journey",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StatItemCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier.border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// Particle Engine using lightweight frame loops inside a Canvas
@Composable
private fun CanvasParticleEngine(
    isGrandmaster: Boolean,
    primaryColor: Color,
    secondaryColor: Color
) {
    val random = remember { Random() }
    val particles = remember { mutableStateListOf<InteractiveParticle>() }

    // Custom frame rate clock triggers recomposition / coordinates logic continuously
    LaunchedEffect(Unit) {
        // Hydration water bubble particles (initially floating up)
        for (i in 0..14) {
            particles.add(
                InteractiveParticle(
                    x = random.nextFloat() * 1000f,
                    y = 1200f + random.nextFloat() * 400f,
                    vx = (random.nextFloat() - 0.5f) * 2f,
                    vy = -(random.nextFloat() * 3f + 2f),
                    size = random.nextFloat() * 12f + 8f,
                    color = if (isGrandmaster) Color(0xFFFFD54F) else Color(0xFF81D4FA),
                    isWaterDrop = true
                )
            )
        }

        // Falling celebratory confetti
        val confettiColors = if (isGrandmaster) {
            listOf(Color(0xFFD4AF37), Color(0xFFFFD700), Color(0xFFFFDF00), Color(0xFFF3C300), Color.White)
        } else {
            listOf(Color(0xFF81D4FA), Color(0xFF26A69A), Color(0xFFFFD54F), Color(0xFFEF5350), Color(0xFFAB47BC))
        }

        for (i in 0..25) {
            particles.add(
                InteractiveParticle(
                    x = random.nextFloat() * 1000f,
                    y = -random.nextFloat() * 800f,
                    vx = (random.nextFloat() - 0.5f) * 5f,
                    vy = random.nextFloat() * 4f + 3f,
                    size = random.nextFloat() * 10f + 6f,
                    color = confettiColors[random.nextInt(confettiColors.size)],
                    isWaterDrop = false,
                    rotation = random.nextFloat() * 360f,
                    rotationSpeed = (random.nextFloat() - 0.5f) * 10f
                )
            )
        }

        // Continual updates while visible
        while (isActive) {
            withFrameMillis { _ ->
                for (p in particles) {
                    p.x += p.vx
                    p.y += p.vy
                    p.rotation += p.rotationSpeed

                    if (p.isWaterDrop) {
                        // Reset water bubble to the bottom
                        if (p.y < -50f) {
                            p.y = 1600f
                            p.x = random.nextFloat() * 1200f
                        }
                    } else {
                        // Reset confetti to top
                        if (p.y > 2200f) {
                            p.y = -50f
                            p.x = random.nextFloat() * 1200f
                        }
                    }
                }
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        for (p in particles) {
            // Normalize coordinates base
            val drawX = (p.x / 1000f) * w
            val drawY = (p.y / 1600f) * h

            if (p.isWaterDrop) {
                // Render elegant rising water molecule bubble
                drawCircle(
                    color = p.color.copy(alpha = 0.45f),
                    radius = p.size,
                    center = androidx.compose.ui.geometry.Offset(drawX, drawY)
                )
                // Shine outline highlighting
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = p.size * 0.35f,
                    center = androidx.compose.ui.geometry.Offset(drawX - p.size * 0.3f, drawY - p.size * 0.3f)
                )
            } else {
                // Render rotating falling rectangle confetti piece
                rotate(degrees = p.rotation, pivot = androidx.compose.ui.geometry.Offset(drawX, drawY)) {
                    drawRect(
                        color = p.color,
                        topLeft = androidx.compose.ui.geometry.Offset(drawX - p.size, drawY - p.size * 0.5f),
                        size = androidx.compose.ui.geometry.Size(p.size * 2f, p.size)
                    )
                }
            }
        }
    }
}

private fun formatVolume(ml: Int): String {
    return if (ml >= 1000) {
        String.format("%.1f L", ml / 1000f)
    } else {
        "$ml ml"
    }
}

// Simple overshoot interpolator replacement to get organic bouncy enters
private class OvershootInterpolator(private val tension: Float = 1.3f) {
    fun toEasing(): Easing {
        return Easing { t ->
            val m = t - 1.0f
            m * m * ((tension + 1) * m + tension) + 1.0f
        }
    }
}
