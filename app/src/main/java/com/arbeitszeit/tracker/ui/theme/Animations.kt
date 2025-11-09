package com.arbeitszeit.tracker.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Fade-in Animation für neu geladene Inhalte
 */
@Composable
fun <T> AnimatedContent(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    androidx.compose.animation.AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(300, easing = EaseInOut)
            ) togetherWith fadeOut(
                animationSpec = tween(300, easing = EaseInOut)
            )
        },
        label = "content_animation"
    ) { state ->
        content(state)
    }
}

/**
 * Pulsierender Effekt (z.B. für laufende Zeit)
 */
fun Modifier.pulsing(
    enabled: Boolean = true,
    minScale: Float = 0.95f,
    maxScale: Float = 1.05f,
    duration: Int = 1000
): Modifier = composed {
    if (!enabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    this.scale(scale)
}

/**
 * Pulsierender Opacity Effekt
 */
fun Modifier.pulsingOpacity(
    enabled: Boolean = true,
    minAlpha: Float = 0.6f,
    maxAlpha: Float = 1.0f,
    duration: Int = 1000
): Modifier = composed {
    if (!enabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_opacity")
    val alpha by infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    this.graphicsLayer { this.alpha = alpha }
}

/**
 * Slide-in Animation von rechts
 */
fun slideInFromRightTransition(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(300, easing = EaseOutCubic)
    ) + fadeIn(animationSpec = tween(300))
}

/**
 * Slide-out Animation nach links
 */
fun slideOutToLeftTransition(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { -it },
        animationSpec = tween(300, easing = EaseInCubic)
    ) + fadeOut(animationSpec = tween(300))
}

/**
 * Fade-in from bottom animation
 */
fun fadeInFromBottomTransition(): EnterTransition {
    return slideInVertically(
        initialOffsetY = { it / 4 },
        animationSpec = tween(400, easing = EaseOutCubic)
    ) + fadeIn(animationSpec = tween(400))
}

/**
 * Expandable/Collapsable content animation
 */
@Composable
fun ExpandableContent(
    expanded: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(
            animationSpec = tween(300, easing = EaseOutCubic)
        ) + fadeIn(animationSpec = tween(300)),
        exit = shrinkVertically(
            animationSpec = tween(300, easing = EaseInCubic)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        content()
    }
}

/**
 * Shimmer effect for loading states
 */
fun Modifier.shimmer(enabled: Boolean = true): Modifier = composed {
    if (!enabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    this.graphicsLayer { this.alpha = alpha }
}

/**
 * Scale animation for clicks
 */
fun Modifier.bounceClick(): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounce_scale"
    )

    this
        .scale(scale)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}
