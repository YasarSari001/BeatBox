package com.blackstack.beatbox.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.ui.theme.BeatBoxMotion
import com.blackstack.beatbox.ui.theme.LocalReducedMotion

/** Alt navigasyondaki tek bir sekme. */
data class BeatBoxNavItem(
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String
)

/**
 * Aurora Glass alt navigasyonu.
 *
 * Seçili sekmede bir pill belirir, ikon dolu varyantına morph yapar ve
 * etiket yumuşakça açılır. Seçim değişiminde hafif bir dokunsal geri bildirim
 * verilir. Yüzey, üst çubukla aynı cam dilini kullanır.
 */
@Composable
fun BeatBoxBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    items: List<BeatBoxNavItem>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    // Sistem hareketi kapalıyken pill kaymaz, hedef konuma anında atlar.
    val reducedMotion = LocalReducedMotion.current
    // Sekme sınırları: kayan pill bu ölçüler arasında ilerler.
    var itemBounds by remember { mutableStateOf<Map<Int, Rect>>(emptyMap()) }
    val targetBounds = itemBounds[selectedTab]

    // Pill değerleri yalnızca çizim aşamasında okunur; sekme geçişinde alt
    // navigasyon yeniden compose edilmez, sadece pill yeniden çizilir.
    val pillSpec = if (reducedMotion) snap<Float>() else BeatBoxMotion.Glide
    val pillLeft = animateFloatAsState(
        targetValue = targetBounds?.left ?: 0f,
        animationSpec = pillSpec,
        label = "nav_pill_left"
    )
    val pillTop = animateFloatAsState(
        targetValue = targetBounds?.top ?: 0f,
        animationSpec = pillSpec,
        label = "nav_pill_top"
    )
    val pillWidth = animateFloatAsState(
        targetValue = targetBounds?.width ?: 0f,
        animationSpec = pillSpec,
        label = "nav_pill_width"
    )
    val pillHeight = animateFloatAsState(
        targetValue = targetBounds?.height ?: 0f,
        animationSpec = pillSpec,
        label = "nav_pill_height"
    )
    val pillAlpha = animateFloatAsState(
        targetValue = if (targetBounds != null) 1f else 0f,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            tween(durationMillis = BeatBoxMotion.Quick)
        },
        label = "nav_pill_alpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = RoundedCornerShape(30.dp)
            ),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f),
        shadowElevation = 18.dp,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(5.dp)
                // Tek bir pill tüm sekmeler arasında kayar; her sekmede ayrı
                // beliren soluk bir arka plan yerine tek sürekli gösterge.
                // Çizim aşamasında çizildiği için kare başına recomposition yok.
                .drawBehind {
                    val alpha = pillAlpha.value
                    if (alpha <= 0.01f) return@drawBehind
                    val insetX = 2.dp.toPx()
                    val insetY = 5.dp.toPx()
                    val width = (pillWidth.value - insetX * 2f).coerceAtLeast(0f)
                    val height = (pillHeight.value - insetY * 2f).coerceAtLeast(0f)
                    if (width <= 0f || height <= 0f) return@drawBehind
                    val left = pillLeft.value + insetX
                    val top = pillTop.value + insetY
                    // Yumuşak dış parıltı: blur maliyeti olmadan iki katman.
                    val glow = 5.dp.toPx()
                    val radius = CornerRadius(21.dp.toPx())
                    drawRoundRect(
                        color = primary.copy(alpha = 0.08f * alpha),
                        topLeft = Offset(left - glow, top - glow),
                        size = Size(width + glow * 2f, height + glow * 2f),
                        cornerRadius = CornerRadius(radius.x + glow)
                    )
                    // Gösterge düz renk yerine soldan sağa incelen cam geçişiyle
                    // çizilir; seçili sekme "ışıklandırılmış" görünür.
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.30f * alpha),
                                secondary.copy(alpha = 0.16f * alpha)
                            ),
                            start = Offset(left, top),
                            end = Offset(left + width, top + height)
                        ),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = radius
                    )
                    // Üst kenardaki ince ışık çizgisi cam hissini tamamlar.
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.18f * alpha),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = radius,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    BeatBoxNavItemView(
                        item = item,
                        selected = selectedTab == index,
                        onSelect = { onTabSelected(index) },
                        onBoundsChange = { rect ->
                            // Aynı ölçü her layout'ta yeni map üretmesin.
                            if (itemBounds[index] != rect) itemBounds = itemBounds + (index to rect)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BeatBoxNavItemView(
    item: BeatBoxNavItem,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    onBoundsChange: (Rect) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    // Sistem hareketi kapaliyken ikon pop'u ve etiket acilisi animasyonsuz
    // uygulanir; durum degisimi aninda gorunur olur.
    val reducedMotion = LocalReducedMotion.current
    // Secim hem dokunmayla hem de TalkBack eylemiyle ayni yoldan gecer.
    val select = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!selected) onSelect()
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "nav_press_scale"
    )
    // Seçili ikonun arkasındaki rozet: 0 → 1 arasında yumuşakça açılır.
    // Değer yalnızca çizim aşamasında okunur, sekme geçişinde recomposition yok.
    val badgeProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else BeatBoxMotion.Soft,
        label = "nav_icon_badge"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = when {
            reducedMotion -> snap()
            // Seçilen ikon kısa bir "pop" yapar: hızla büyür, yumuşakça oturur.
            selected -> keyframes {
                durationMillis = 420
                0.92f at 0
                1.09f at 150 using FastOutLinearInEasing
                1f at 420 using BeatBoxMotion.ExpressiveEasing
            }

            else -> BeatBoxMotion.Soft
        },
        label = "nav_icon_scale"
    )
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val idle = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .onGloballyPositioned { onBoundsChange(it.boundsInParent()) }
            .clip(RoundedCornerShape(24.dp))
            // Yalnızca dokunma geri bildirimi tüm öğeyi ölçekler; seçim
            // "pop"u ikonda kalır, böylece etiket kırpılmaz.
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            // Etiket, secili durum ve tiklama tek bir erisilebilirlik dugumunde
            // toplanir; aksi halde TalkBack sekme adini veya "secili" bilgisini
            // ayri dugumlerde gorur.
            .clearAndSetSemantics {
                contentDescription = item.label
                this.selected = selected
                role = Role.Tab
                onClick {
                    select()
                    true
                }
            }
            .selectable(
                selected = selected,
                onClick = select,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            // İkon rozeti: hale, renk geçişli daire ve ince halka. Hepsi
            // drawBehind içinde çizilir; animasyon sırasında yalnızca yeniden
            // çizim olur, alt navigasyon yeniden compose edilmez.
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .drawBehind {
                        val progress = badgeProgress
                        if (progress <= 0.01f) return@drawBehind
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primary.copy(alpha = 0.22f * progress),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = radius
                            ),
                            radius = radius,
                            center = center
                        )
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    primary.copy(alpha = 0.34f * progress),
                                    secondary.copy(alpha = 0.16f * progress)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height)
                            ),
                            radius = radius * 0.82f,
                            center = center
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.20f * progress),
                            radius = radius * 0.82f,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = selected,
                    transitionSpec = {
                        if (reducedMotion) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            (scaleIn(initialScale = 0.72f) + fadeIn(tween(140))) togetherWith
                                (scaleOut(targetScale = 0.72f) + fadeOut(tween(100)))
                        }
                    },
                    label = "nav_icon_morph"
                ) { isSelected ->
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.icon,
                        contentDescription = null,
                        tint = if (isSelected) primary else idle,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                    )
                }
            }

            AnimatedVisibility(
                visible = selected,
                enter = if (reducedMotion) {
                    EnterTransition.None
                } else {
                    fadeIn(tween(180)) + expandHorizontally(
                        animationSpec = tween(240, easing = BeatBoxMotion.ExpressiveEasing)
                    )
                },
                exit = if (reducedMotion) {
                    ExitTransition.None
                } else {
                    fadeOut(tween(120)) + shrinkHorizontally(
                        animationSpec = tween(170, easing = BeatBoxMotion.StandardEasing)
                    )
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = item.label,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp,
                        color = primary,
                        maxLines = 1,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                }
            }
        }
    }
}
