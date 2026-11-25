package com.blackstack.beatbox.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Sistem "animasyonları kaldır" ayarı. `true` iken sonsuz animasyonlar ve
 * dekoratif hareketler durdurulur; içerik yine de okunabilir kalır.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Android'in animatör süre ölçeğini okuyarak reduced-motion tercihini döndürür.
 *
 * Değer uygulama açılışında okunur; ayar değişirse uygulamanın yeniden
 * başlatılması beklenir. Bu, Faz 1 kapsamındaki hafif bir temeldir.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * BeatBox hareket dili.
 *
 * Tüm yeni bileşenler süre ve yay değerlerini buradan almalı. Amaç, uygulamanın
 * her yerinde aynı "yumuşak ama çevik" hissi korumak.
 */
object BeatBoxMotion {
    /** Dokunma geri bildirimi, ripple ve anlık renk değişimleri. */
    const val Instant = 120

    /** İkon morph, çip ve küçük durum geçişleri. */
    const val Quick = 180

    /** Sayfa geçişi, alt sayfa, standart görünürlük. */
    const val Standard = 280

    /** Hero morph, splash ve sinematik açılışlar. */
    const val Cinematic = 460

    /** Liste öğeleri arasında önerilen gecikme. */
    const val Stagger = 40

    val Soft = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 380f
    )

    val Snappy = spring<Float>(
        dampingRatio = 0.70f,
        stiffness = 900f
    )

    val Bouncy = spring<Float>(
        dampingRatio = 0.55f,
        stiffness = 500f
    )

    /**
     * Kayan göstergeler (gezinme pill'i, seçim çerçevesi) için hafif taşmalı
     * yay. Hedefe hızla varır, birkaç piksel taşıp yerine oturur.
     */
    val Glide = spring<Float>(
        dampingRatio = 0.62f,
        stiffness = 420f
    )

    val StandardEasing = FastOutSlowInEasing

    /** Hızlı başlayıp yumuşak biten, "premium" hissi veren eğri. */
    val ExpressiveEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    fun <T> soft(durationMillis: Int = Standard): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = StandardEasing)

    fun <T> expressive(durationMillis: Int = Cinematic): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = ExpressiveEasing)

    fun slideSpec(durationMillis: Int = Standard): FiniteAnimationSpec<IntOffset> =
        tween(durationMillis = durationMillis, easing = StandardEasing)
}

/**
 * Liste, kart ve içerik öğeleri için yumuşak giriş hareketi.
 *
 * Öğe ilk kez göründüğünde hafifçe yukarı kayarak belirir. [index] sıralı
 * gecikme verir; böylece liste bir anda değil dalga hâlinde açılır. Gecikme
 * sekiz öğede sınırlanır, uzun listelerde kaydırma yavaşlamaz. Kaydırıp geri
 * dönüldüğünde hareket yeniden oynamaz. Sistemde hareket kapalıysa atlanır.
 */
@Composable
fun Modifier.entranceAnimation(
    index: Int = 0,
    distance: Dp = 14.dp
): Modifier {
    if (LocalReducedMotion.current) return this
    var started by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val steps = index.coerceIn(0, 8)
        if (steps > 0) delay(steps * BeatBoxMotion.Stagger.toLong())
        started = true
    }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(
            durationMillis = BeatBoxMotion.Standard,
            easing = BeatBoxMotion.ExpressiveEasing
        ),
        label = "entrance_progress"
    )
    return graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * distance.toPx()
    }
}
