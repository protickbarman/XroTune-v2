package com.xrotune.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.xrotune.app.ui.theme.CoralPulse
import com.xrotune.app.ui.theme.ElectricViolet
import com.xrotune.app.ui.theme.ObsidianBg
import com.xrotune.app.ui.theme.TintedSurface
import com.xrotune.app.ui.theme.TintedSurfaceHigh
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Global zero-ripple indication provider so Material components never emit ink ripples.
 */
object NoRippleIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return object : Modifier.Node(), DrawModifierNode {
            override fun ContentDrawScope.draw() {
                drawContent()
            }
        }
    }

    override fun hashCode(): Int = 42
    override fun equals(other: Any?): Boolean = other === this
}

object IosMotionTokens {
    val PressSpring = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = 560f
    )
    val SheetSpring = spring<Float>(
        dampingRatio = 0.80f,
        stiffness = 380f
    )
    val RevealSpring = spring<Float>(
        dampingRatio = 0.82f,
        stiffness = 340f
    )
    val LaunchSpring = spring<Float>(
        dampingRatio = 0.78f,
        stiffness = 260f
    )
}

/**
 * Reusable iOS-style tactile clickable modifier with zero ripple, spring scale (0.965f),
 * subtle opacity compression (0.84f) on press, and rememberUpdatedState so recycled
 * LazyColumn/LazyRow items never fire a stale click lambda.
 */
fun Modifier.iosClickable(
    enabled: Boolean = true,
    scaleDown: Float = 0.965f,
    pressedAlpha: Float = 0.84f,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentEnabled by rememberUpdatedState(enabled)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && currentEnabled) scaleDown else 1f,
        animationSpec = IosMotionTokens.PressSpring,
        label = "iosPressScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (!currentEnabled) 0.45f else if (isPressed) pressedAlpha else 1f,
        animationSpec = IosMotionTokens.PressSpring,
        label = "iosPressAlpha"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = currentEnabled,
            role = role,
            onClick = { currentOnClick() }
        )
}

@Composable
fun IosIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Color.Transparent,
    size: Dp = 44.dp,
    shape: Shape = CircleShape,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .clip(shape)
            .background(containerColor)
            .iosClickable(enabled = enabled, scaleDown = 0.93f, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun IosPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = TintedSurfaceHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shadowElevation: Dp = 4.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .iosClickable(enabled = enabled, scaleDown = 0.965f, role = Role.Button, onClick = onClick),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = shadowElevation,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun IosSurfaceChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        TintedSurfaceHigh
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .height(36.dp)
            .iosClickable(scaleDown = 0.96f, role = Role.Tab, onClick = onClick),
        shape = CircleShape,
        color = bgColor,
        contentColor = textColor,
        shadowElevation = if (selected) 8.dp else 2.dp,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun IosTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    shape: Shape = RoundedCornerShape(18.dp),
    containerColor: Color = TintedSurfaceHigh
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(text = placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        label = if (label != null) {
            { Text(text = label) }
        } else null,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = shape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor.copy(alpha = 0.6f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

/**
 * Interactive floating-sheet swipe-to-minimize container using NestedScrollConnection + handle drag
 * so internal lists/lyrics scroll naturally while pulling downward at top collapses smoothly.
 */
@Composable
fun SwipeToMinimizeContainer(
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier,
    baseCornerRadius: Dp = 28.dp,
    content: @Composable (handleDragModifier: Modifier) -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentOnMinimize by rememberUpdatedState(onMinimize)
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 120.dp.toPx() }
    val maxSlidePx = with(density) { 460.dp.toPx() }
    val offsetY = remember { Animatable(0f) }

    val progress = (offsetY.value / maxSlidePx).coerceIn(0f, 1f)
    val dynamicScale = 1f - (progress * 0.06f)
    val dynamicCorner by animateDpAsState(
        targetValue = baseCornerRadius + (progress * 12f).dp,
        label = "swipeCornerMorph"
    )

    fun settleSheet(velocityY: Float) {
        if (offsetY.value > dismissThresholdPx || (offsetY.value > 24f && velocityY > 950f)) {
            scope.launch {
                offsetY.animateTo(
                    targetValue = maxSlidePx,
                    initialVelocity = velocityY,
                    animationSpec = IosMotionTokens.SheetSpring
                )
                currentOnMinimize()
            }
        } else if (offsetY.value > 0f) {
            scope.launch {
                offsetY.animateTo(
                    targetValue = 0f,
                    initialVelocity = velocityY,
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = 440f
                    )
                )
            }
        }
    }

    val nestedScrollConnection = remember(maxSlidePx, dismissThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                return if (delta < 0f && offsetY.value > 0f) {
                    val consumed = maxOf(delta, -offsetY.value)
                    scope.launch {
                        offsetY.snapTo((offsetY.value + consumed).coerceAtLeast(0f))
                    }
                    Offset(0f, consumed)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    scope.launch {
                        offsetY.snapTo((offsetY.value + available.y).coerceIn(0f, maxSlidePx))
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offsetY.value > 0f) {
                    settleSheet(available.y)
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (offsetY.value > 0f) {
                    settleSheet(available.y)
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    val handleDraggableState = rememberDraggableState { delta ->
        scope.launch {
            offsetY.snapTo((offsetY.value + delta).coerceIn(0f, maxSlidePx))
        }
    }
    val handleDragModifier = Modifier.draggable(
        state = handleDraggableState,
        orientation = Orientation.Vertical,
        onDragStopped = { velocity -> settleSheet(velocity) }
    )

    Box(
        modifier = modifier
            .nestedScroll(nestedScrollConnection)
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .graphicsLayer {
                scaleX = dynamicScale
                scaleY = dynamicScale
                alpha = 1f - (progress * 0.28f)
            }
            .clip(RoundedCornerShape(topStart = dynamicCorner, topEnd = dynamicCorner))
    ) {
        content(handleDragModifier)
    }
}

@Composable
fun EdgeSwipeBackContainer(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val currentOnBack by rememberUpdatedState(onBack)
    val density = LocalDensity.current
    val edgeWidthPx = with(density) { 36.dp.toPx() }
    val triggerThresholdPx = with(density) { 84.dp.toPx() }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    val animatedShift by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = IosMotionTokens.PressSpring,
        label = "edgeSwipeShift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset((animatedShift * 0.45f).roundToInt(), 0) }
            .pointerInput(edgeWidthPx, triggerThresholdPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.position.x > edgeWidthPx) {
                        return@awaitEachGesture
                    }
                    var totalDx = 0f
                    val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                        if (over > 0f) {
                            change.consume()
                            totalDx = over
                            dragOffset = totalDx.coerceAtLeast(0f)
                        }
                    }
                    if (drag != null) {
                        horizontalDrag(drag.id) { change ->
                            val dx = change.positionChange().x
                            totalDx = (totalDx + dx).coerceAtLeast(0f)
                            dragOffset = totalDx
                            change.consume()
                        }
                        if (totalDx >= triggerThresholdPx) {
                            currentOnBack()
                        }
                    }
                    dragOffset = 0f
                }
            }
    ) {
        content()
    }
}

@Composable
fun CinematicLaunchContainer(
    content: @Composable () -> Unit
) {
    var launchDismissed by remember { mutableStateOf(false) }
    val contentScale = remember { Animatable(0.91f) }
    val contentAlpha = remember { Animatable(0f) }
    val emblemScale = remember { Animatable(0.76f) }
    val overlayAlpha = remember { Animatable(1f) }
    val overlayScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch {
            emblemScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.68f, stiffness = 290f)
            )
        }
        delay(280L)
        val revealJob = launch {
            contentScale.animateTo(1f, animationSpec = IosMotionTokens.LaunchSpring)
        }
        val fadeInJob = launch {
            contentAlpha.animateTo(1f, animationSpec = tween(420, easing = FastOutSlowInEasing))
        }
        val fadeOutJob = launch {
            overlayScale.animateTo(1.09f, animationSpec = tween(420, easing = FastOutSlowInEasing))
        }
        val overlayFadeJob = launch {
            overlayAlpha.animateTo(0f, animationSpec = tween(380, easing = FastOutSlowInEasing))
        }
        revealJob.join()
        fadeInJob.join()
        fadeOutJob.join()
        overlayFadeJob.join()
        launchDismissed = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = contentScale.value
                    scaleY = contentScale.value
                    alpha = contentAlpha.value
                }
        ) {
            content()
        }

        if (!launchDismissed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(50f)
                    .graphicsLayer {
                        scaleX = overlayScale.value
                        scaleY = overlayScale.value
                        alpha = overlayAlpha.value
                    }
                    .background(ObsidianBg),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    ElectricViolet.copy(alpha = 0.34f),
                                    CoralPulse.copy(alpha = 0.14f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.graphicsLayer {
                        scaleX = emblemScale.value
                        scaleY = emblemScale.value
                    }
                ) {
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = Color.Transparent,
                        shadowElevation = 24.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(Brush.linearGradient(listOf(ElectricViolet, CoralPulse))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "XroTune",
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                    Text(
                        text = "XroTune",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun StaggeredEnterItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(index > 5) }
    LaunchedEffect(Unit) {
        if (!visible) {
            delay((index.coerceIn(0, 5) * 18L))
            visible = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
        label = "staggerAlpha"
    )
    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else 14f,
        animationSpec = IosMotionTokens.RevealSpring,
        label = "staggerSlide"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translationY
        }
    ) {
        content()
    }
}

@Composable
fun SoftPulseLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val transition = rememberInfiniteTransition(label = "softPulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
                alpha = pulseAlpha
            },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            strokeWidth = 2.6.dp,
            color = color,
            trackColor = color.copy(alpha = 0.16f)
        )
    }
}

@Composable
fun SoftTrackLoadingSkeleton(
    count: Int = 5,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "skeletonPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(count) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .alpha(alpha),
                shape = RoundedCornerShape(18.dp),
                color = TintedSurface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(76.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TintedSurfaceHigh)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .height(13.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(TintedSurfaceHigh)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.42f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(TintedSurfaceHigh)
                        )
                    }
                }
            }
        }
    }
}
