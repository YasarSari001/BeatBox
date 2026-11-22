package com.blackstack.beatbox.ui.components
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.blackstack.beatbox.ui.theme.rememberPerformanceMode
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun EnhancedRealTimeVisualizer(
    isPlaying: Boolean,
    level: Float,
    waveform: FloatArray,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("BeatBoxSettings", Context.MODE_PRIVATE) }
    val tempoVizEnabled = remember { prefs.getBoolean("tempo_visualization", true) }
    // Düşük donanım veya performans modunda sürekli animasyon çalıştırılmaz;
    // duraklatılmışken de hiçbir sonsuz animasyon kalmaz.
    val performanceMode = rememberPerformanceMode()
    val animated = tempoVizEnabled && !performanceMode && isPlaying

    if (!animated) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            repeat(12) { index ->
                val staticHeight = when {
                    index < 3 -> 24.dp
                    index < 6 -> 20.dp
                    index < 9 -> 16.dp
                    else -> 12.dp
                }
                Box(
                    Modifier
                        .padding(horizontal = 1.5.dp)
                        .width(4.dp)
                        .height(if (isPlaying) staticHeight else 8.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.primary
                                )
                            ),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        return
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "tempo_fallback")
    val fallbackPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tempo_phase"
    )

    // Bu noktadan sonra çalma her zaman etkindir (bkz. yukarıdaki erken çıkış).
    val barCount = 14
    val hasRealData = waveform.any { it > 0.01f }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(barCount) { index ->
            val targetAmplitude = if (hasRealData && index < waveform.size) {
                (waveform[index] * 2.8f).coerceIn(0f, 1f)
            } else {
                val sineVal = sin(fallbackPhase + index * 0.45f)
                val baseAmp = (sineVal + 1f) / 2f
                (0.25f + baseAmp * 0.7f).coerceIn(0.15f, 1f)
            }
            
            val amplitude by animateFloatAsState(
                targetValue = targetAmplitude,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "bar_amplitude_$index"
            )
            
            val centerDistance = abs(index - barCount / 2f) / (barCount / 2f)
            val baseHeight = 6f
            val maxHeight = (38f - centerDistance * 14f).coerceAtLeast(20f)
            
            val height = (baseHeight + amplitude * (maxHeight - baseHeight)).dp
            val alpha = (0.65f + amplitude * 0.35f).coerceIn(0.65f, 1f)
            
            Box(
                Modifier
                    .padding(horizontal = 1.5.dp)
                    .width(4.5.dp)
                    .height(height)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.secondary.copy(alpha = alpha),
                                MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                                MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.5f)
                            )
                        ),
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

/** MediaPlaybackService'in her turda ürettiği visualizer örnek sayısı. */
const val VISUALIZER_BUCKETS = 15

/**
 * Visualizer verisinin kararlı kapsayıcısı.
 *
 * Veri daha önce ekranlar arasında `Float`/`FloatArray` parametresi olarak
 * taşınıyordu. `FloatArray` kararsız sayıldığı için çalma sırasında saniyede
 * sekiz kez tüm uygulama ağacı (kitaplık listesi, sekmeler, overlay'ler)
 * yeniden compose ediliyordu. Bu nesnenin kimliği sabittir; güncellemeler
 * yalnızca [level] ve [waveform] okuyan bileşenleri geçersiz kılar.
 */
@Stable
class VisualizerState {
    var level by mutableFloatStateOf(0f)
        private set
    var waveform by mutableStateOf(FloatArray(VISUALIZER_BUCKETS))
        private set

    fun update(level: Float, waveform: FloatArray) {
        this.level = level
        this.waveform = waveform
    }

    fun reset() {
        level = 0f
        waveform = FloatArray(VISUALIZER_BUCKETS)
    }
}

@Composable
fun MusicVisualizer(
    isPlaying: Boolean,
    state: VisualizerState,
    modifier: Modifier = Modifier
) {
    // Değerler burada okunur: yalnızca bu küçük bileşen yeniden compose olur.
    EnhancedRealTimeVisualizer(isPlaying, state.level, state.waveform, modifier)
}
