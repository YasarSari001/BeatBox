package com.blackstack.beatbox.ui.components
import androidx.core.net.toUri
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.palette.graphics.Palette
import com.blackstack.beatbox.ui.theme.BeatBoxTheme
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import com.blackstack.beatbox.Strings

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(strings: Strings, onFinish: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            title = strings.get("onboarding_title_1"),
            description = strings.get("onboarding_desc_1"),
            icon = Icons.Rounded.GraphicEq,
            accentColor = Color(0xFF9C27B0)
        ),
        OnboardingPage(
            title = strings.get("onboarding_title_2"),
            description = strings.get("onboarding_desc_2"),
            icon = Icons.Rounded.Subtitles,
            accentColor = Color(0xFF00E5FF)
        ),
        OnboardingPage(
            title = strings.get("onboarding_title_3"),
            description = strings.get("onboarding_desc_3"),
            icon = Icons.Rounded.Palette,
            accentColor = Color(0xFFFF2E93)
        ),
        OnboardingPage(
            title = strings.get("onboarding_title_4"),
            description = strings.get("onboarding_desc_4"),
            icon = Icons.Rounded.Security,
            accentColor = Color(0xFF00E5FF)
        ),
        OnboardingPage(
            title = strings.get("onboarding_title_5"),
            description = strings.get("onboarding_desc_5"),
            icon = Icons.Rounded.MusicNote,
            accentColor = Color(0xFFFFD60A)
        )
    )

    val pagerState = rememberPagerState { pages.size }
    val coroutineScope = rememberCoroutineScope()

    // Ambient pulsing glow
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val currentPage = pages[pagerState.currentPage]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C10))
    ) {
        // Animated Ambient Background Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            currentPage.accentColor.copy(alpha = 0.35f),
                            Color(0xFF121420),
                            Color(0xFF0B0C10)
                        )
                    )
                )
        )

        // Center Pulsing Orb
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.Center)
                .scale(glowScale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            currentPage.accentColor.copy(alpha = glowAlpha),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val page = pages[pageIndex]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Glassmorphic Icon Card
                Surface(
                    shape = RoundedCornerShape(36.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    border = BorderStroke(1.5.dp, page.accentColor.copy(alpha = 0.6f)),
                    shadowElevation = 20.dp,
                    modifier = Modifier
                        .size(160.dp)
                        .cardShadow()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            page.accentColor.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = page.accentColor,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }

                Spacer(Modifier.height(48.dp))

                Text(
                    text = page.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = page.description,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // Bottom Controls Section
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Smooth Page Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                repeat(pages.size) { index ->
                    val active = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (active) 28.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "indicator_width"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (active)
                                    currentPage.accentColor
                                else
                                    Color.White.copy(alpha = 0.25f)
                            )
                    )
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip Button
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(onClick = onFinish) {
                        Text(
                            text = strings.get("skip"),
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    Spacer(Modifier.width(60.dp))
                }

                // Next / Get Started Button
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinish()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = currentPage.accentColor
                    ),
                    shape = RoundedCornerShape(18.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .animateContentSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = if (pagerState.currentPage == pages.size - 1) strings.get("start_listening") else strings.get("continue"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun UserAgreementDialog(strings: Strings, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Description,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    strings.get("user_agreement"),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        strings.get("user_agreement_full"),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(strings.get("ok"))
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
