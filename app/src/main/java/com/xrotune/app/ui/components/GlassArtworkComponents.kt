package com.xrotune.app.ui.components

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xrotune.app.ui.theme.CoralPulse
import com.xrotune.app.ui.theme.ElectricViolet
import com.xrotune.app.ui.theme.ObsidianBg
import com.xrotune.app.ui.theme.TintedSurface
import com.xrotune.app.ui.util.buildArtworkCandidates

@Composable
fun ProgressiveArtworkImage(
    videoId: String?,
    rawThumbnail: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onAspectRatioResolved: ((Float) -> Unit)? = null
) {
    val candidates = remember(videoId, rawThumbnail) {
        buildArtworkCandidates(videoId, rawThumbnail)
    }
    var candidateIndex by remember(candidates) { mutableIntStateOf(0) }
    val activeUrl = candidates.getOrNull(candidateIndex)

    if (activeUrl != null) {
        val context = LocalContext.current
        AsyncImage(
            model = remember(activeUrl) {
                ImageRequest.Builder(context)
                    .data(activeUrl)
                    .crossfade(true)
                    .build()
            },
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            onSuccess = { success ->
                val size = success.painter.intrinsicSize
                if (size.width > 1f && size.height > 1f) {
                    val ratio = (size.width / size.height).coerceIn(0.75f, 2.0f)
                    onAspectRatioResolved?.invoke(ratio)
                }
            },
            onError = {
                if (candidateIndex < candidates.lastIndex) {
                    candidateIndex++
                }
            }
        )
    }
}

@Composable
fun AdaptiveFullArtworkFrame(
    videoId: String?,
    rawThumbnail: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp
) {
    var rawAspect by remember(videoId, rawThumbnail) { mutableStateOf(16f / 9f) }
    val smoothAspect by animateFloatAsState(
        targetValue = rawAspect.coerceIn(1.0f, 1.78f),
        animationSpec = tween(durationMillis = 350),
        label = "artworkAspectRatio"
    )
    val playScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.945f,
        animationSpec = IosMotionTokens.SheetSpring,
        label = "artworkPlayScale"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Surface(
        modifier = modifier
            .aspectRatio(smoothAspect, matchHeightConstraintsFirst = false)
            .graphicsLayer {
                scaleX = playScale
                scaleY = playScale
            },
        shape = shape,
        color = TintedSurface,
        shadowElevation = if (isPlaying) 22.dp else 10.dp,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
            )

            val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier
                    .fillMaxSize()
                    .blur(28.dp)
            } else {
                Modifier.fillMaxSize()
            }
            ProgressiveArtworkImage(
                videoId = videoId,
                rawThumbnail = rawThumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = blurModifier
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ObsidianBg.copy(alpha = 0.40f),
                                ObsidianBg.copy(alpha = 0.60f)
                            )
                        )
                    )
            )

            ProgressiveArtworkImage(
                videoId = videoId,
                rawThumbnail = rawThumbnail,
                contentDescription = "Album Art",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .clip(RoundedCornerShape((cornerRadius - 4.dp).coerceAtLeast(12.dp))),
                onAspectRatioResolved = { ratio ->
                    rawAspect = ratio
                }
            )
        }
    }
}

@Composable
fun ObsidianGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    borderAccent: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = TintedSurface,
    shadowElevation: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        shadowElevation = shadowElevation,
        tonalElevation = 0.dp,
        content = content
    )
}

@Composable
fun AnimatedPlaybackAura(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "playbackAura")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraDrift"
    )
    val auraStrength by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.45f,
        animationSpec = tween(durationMillis = 650),
        label = "auraStrength"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val primaryCenter = Offset(
            x = w * (0.28f + 0.34f * drift),
            y = h * (0.24f + 0.10f * (1f - drift))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    ElectricViolet.copy(alpha = 0.32f * auraStrength),
                    ElectricViolet.copy(alpha = 0.11f * auraStrength),
                    Color.Transparent
                ),
                center = primaryCenter,
                radius = maxOf(w, h) * 0.62f
            ),
            radius = maxOf(w, h) * 0.62f,
            center = primaryCenter
        )

        val secondaryCenter = Offset(
            x = w * (0.76f - 0.30f * drift),
            y = h * (0.58f + 0.12f * drift)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    CoralPulse.copy(alpha = 0.20f * auraStrength),
                    CoralPulse.copy(alpha = 0.05f * auraStrength),
                    Color.Transparent
                ),
                center = secondaryCenter,
                radius = maxOf(w, h) * 0.55f
            ),
            radius = maxOf(w, h) * 0.55f,
            center = secondaryCenter
        )
    }
}

@Composable
fun MiniEqualizerBars(
    isPlaying: Boolean,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "eqBars")
    val bar1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(460, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val heights = if (isPlaying) listOf(bar1, bar2, bar3) else listOf(0.45f, 0.7f, 0.45f)

    Row(
        modifier = modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { frac ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((14.dp * frac).coerceAtLeast(3.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}
