package com.blackstack.beatbox.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
    // Sekme sınırları: kayan pill bu ölçüler arasında ilerler.
    var itemBounds by remember { mutableStateOf<Map<Int, Rect>>(emptyMap()) }
    val targetBounds = itemBounds[selectedTab]

    // Pill değerleri yalnızca çizim aşamasında okunur; sekme geçişinde alt
    // navigasyon yeniden compose edilmez, sadece pill yeniden çizilir.
    val pillLeft = animateFloatAsState(
        targetValue = targetBounds?.left ?: 0f,
        animationSpec = BeatBoxMotion.Soft,
        label = "nav_pill_left"
    )
    val pillTop = animateFloatAsState(
        targetValue = targetBounds?.top ?: 0f,
        animationSpec = BeatBoxMotion.Soft,
        label = "nav_pill_top"
    )
    val pillWidth = animateFloatAsState(
        targetValue = targetBounds?.width ?: 0f,
        animationSpec = BeatBoxMotion.Soft,
        label = "nav_pill_width"
    )
    val pillHeight = animateFloatAsState(
        targetValue = targetBounds?.height ?: 0f,
        animationSpec = BeatBoxMotion.Soft,
        label = "nav_pill_height"
    )
    val pillAlpha = animateFloatAsState(
        targetValue = if (targetBounds != null) 1f else 0f,
        animationSpec = tween(durationMillis = BeatBoxMotion.Quick),
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
                    drawRoundRect(
                        color = primary.copy(alpha = 0.17f * alpha),
                        topLeft = Offset(pillLeft.value + insetX, pillTop.value + insetY),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(21.dp.toPx())
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
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = BeatBoxMotion.Snappy,
        label = "nav_icon_scale"
    )
    val primary = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .onGloballyPositioned { onBoundsChange(it.boundsInParent()) }
            .clip(RoundedCornerShape(24.dp))
            .graphicsLayer {
                scaleX = pressScale * iconScale
                scaleY = pressScale * iconScale
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
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    (scaleIn(initialScale = 0.72f) + fadeIn(tween(140))) togetherWith
                        (scaleOut(targetScale = 0.72f) + fadeOut(tween(100)))
                },
                label = "nav_icon_morph"
            ) { isSelected ->
                Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.icon,
                    contentDescription = null,
                    tint = if (isSelected) primary else idle,
                    modifier = Modifier
                        .size(23.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            }

            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(180)) + expandHorizontally(tween(220)),
                exit = fadeOut(tween(120)) + shrinkHorizontally(tween(160))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = item.label,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primary,
                        maxLines = 1,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                }
            }
        }
    }
}
