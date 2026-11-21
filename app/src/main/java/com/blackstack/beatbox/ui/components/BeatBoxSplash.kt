package com.blackstack.beatbox.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.R
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.ui.theme.BeatBoxMotion
import com.blackstack.beatbox.ui.theme.LocalReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val SplashCyan = Color(0xFF22D3EE)
private val SplashViolet = Color(0xFFA855F7)
private val SplashPink = Color(0xFFF472B6)

/**
 * Aurora Glass açılış ekranı.
 *
 * Tek bir ritim: logo "vurur", arkasındaki aurora bloom açılır, iki halka
 * yayılır, kelime markası harf aralığını açarak oturur ve alttaki ekolayzer
 * çubukları canlanır; ilerleme çubuğu dolarken ekran kapanır.
 *
 * Reduced-motion açıksa tüm dekoratif animasyonlar atlanır; süre korunur.
 */
@Composable
fun SplashScreen(
    strings: Strings,
    onFinish: () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val logoScale = remember { Animatable(if (reducedMotion) 1f else 0.78f) }
    val contentAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val bloom = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val progress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val bars = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val ringOne = remember { Animatable(0f) }
    val ringTwo = remember { Animatable(0f) }
    val spacing = remember { Animatable(if (reducedMotion) 2.5f else 12f) }

    val rotation: State<Float> = if (reducedMotion) {
        remember { mutableStateOf(0f) }
    } else {
        val transition = rememberInfiniteTransition(label = "splash_ring")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(9_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "splash_ring_value"
        )
    }

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            launch {
                contentAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(380, easing = BeatBoxMotion.ExpressiveEasing)
                )
            }
            launch {
                logoScale.animateTo(
                    targetValue = 1.08f,
                    animationSpec = tween(240, easing = BeatBoxMotion.ExpressiveEasing)
                )
                logoScale.animateTo(0.97f, tween(140))
                logoScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                bloom.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(950, easing = BeatBoxMotion.ExpressiveEasing)
                )
            }
            launch {
                spacing.animateTo(
                    targetValue = 2.5f,
                    animationSpec = tween(820, easing = BeatBoxMotion.ExpressiveEasing)
                )
            }
            launch {
                bars.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(1_000, easing = BeatBoxMotion.ExpressiveEasing)
                )
            }
            launch {
                delay(240)
                ringOne.animateTo(1f, tween(880, easing = BeatBoxMotion.ExpressiveEasing))
            }
            launch {
                delay(440)
                ringTwo.animateTo(1f, tween(880, easing = BeatBoxMotion.ExpressiveEasing))
            }
            launch {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(1_320, easing = BeatBoxMotion.ExpressiveEasing)
                )
            }
        }
        delay(1_500)
        onFinish()
    }

    AuroraBackground(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Yumuşak aurora bloom'u; logo arkasında genişleyerek açılır.
            Box(
                modifier = Modifier
                    .size(420.dp)
                    .graphicsLayer {
                        val value = bloom.value
                        scaleX = 0.5f + value * 0.7f
                        scaleY = 0.5f + value * 0.7f
                        alpha = value
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                SplashCyan.copy(alpha = 0.34f),
                                SplashViolet.copy(alpha = 0.20f),
                                SplashPink.copy(alpha = 0.09f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )

            PulseRing(progress = ringOne.value, size = 190.dp)
            PulseRing(progress = ringTwo.value, size = 250.dp)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = contentAlpha.value
                    translationY = (1f - contentAlpha.value) * 22.dp.toPx()
                }
            ) {
                Box(
                    modifier = Modifier.size(158.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Logonun arkasında dönen ışıltı halkası.
                    Box(
                        modifier = Modifier
                            .size(158.dp)
                            .graphicsLayer {
                                rotationZ = rotation.value
                                alpha = bloom.value * 0.55f
                            }
                            .border(
                                width = 1.5.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        SplashCyan,
                                        SplashViolet,
                                        SplashPink,
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    Surface(
                        modifier = Modifier
                            .size(130.dp)
                            .graphicsLayer {
                                scaleX = logoScale.value
                                scaleY = logoScale.value
                            }
                            .shadow(30.dp, RoundedCornerShape(38.dp)),
                        shape = RoundedCornerShape(38.dp),
                        color = Color.Black.copy(alpha = 0.42f),
                        border = BorderStroke(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                listOf(SplashCyan, SplashViolet, SplashPink)
                            )
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.beatbox_logo),
                                contentDescription = "BeatBox",
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(RoundedCornerShape(26.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "BeatBox",
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.onBackground,
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.onBackground
                            )
                        ),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = spacing.value.sp
                    )
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = strings.get("splash_tagline"),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    letterSpacing = 2.4.sp
                )

                Spacer(Modifier.height(34.dp))

                // Ekolayzer: yumuşak, sıralı çubuklar.
                Row(
                    modifier = Modifier.height(26.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    repeat(9) { index ->
                        val level = abs(sin(bars.value * PI.toFloat() * 2.6f + index * 0.75f))
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((6 + 20 * level).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(SplashCyan, SplashViolet, SplashPink)
                                    )
                                )
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .width(168.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = progress.value
                                transformOrigin = TransformOrigin(0f, 0.5f)
                            }
                            .background(
                                Brush.horizontalGradient(
                                    listOf(SplashCyan, SplashViolet, SplashPink)
                                )
                            )
                    )
                }
            }
        }
    }
}

/** Logodan dışa doğru yayılan ince halka. */
@Composable
private fun PulseRing(progress: Float, size: Dp) {
    if (progress <= 0f) return
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                val scale = 0.65f + progress * 0.8f
                scaleX = scale
                scaleY = scale
                alpha = (1f - progress) * 0.45f
            }
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(SplashCyan, SplashViolet, SplashPink)
                ),
                shape = CircleShape
            )
    )
}
