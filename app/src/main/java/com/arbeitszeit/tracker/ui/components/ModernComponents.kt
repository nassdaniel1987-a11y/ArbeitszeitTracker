package com.arbeitszeit.tracker.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.ui.theme.*

/**
 * Moderne Gradient Card mit Animations
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradient: Brush,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "gradient_card_scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    isPressed = !isPressed
                    onClick()
                }
            ),
        shape = ShapeHeroCard
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(AppSpacing.normal)
        ) {
            Column(content = content)
        }
    }
}

/**
 * Animierter Stat Counter mit Zahlen-Animation
 */
@Composable
fun AnimatedStatCounter(
    value: Int,
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    var animatedValue by remember { mutableIntStateOf(0) }

    LaunchedEffect(value) {
        animate(
            initialValue = animatedValue.toFloat(),
            targetValue = value.toFloat(),
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        ) { currentValue, _ ->
            animatedValue = currentValue.toInt()
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = ShapeHeroCard
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.normal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(AppIconSize.large)
                )
            }

            Text(
                text = animatedValue.toString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Moderne Chip mit Animation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "chip_scale"
    )

    FilterChip(
        selected = selected,
        onClick = {
            isPressed = !isPressed
            onClick()
        },
        label = { Text(label) },
        leadingIcon = if (icon != null && selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(AppIconSize.small)
                )
            }
        } else if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(AppIconSize.small)
                )
            }
        } else null,
        modifier = Modifier.scale(scale),
        shape = ShapeChip
    )
}

/**
 * Shake Animation für Fehler
 */
@Composable
fun rememberShakeController(): ShakeController {
    return remember { ShakeController() }
}

class ShakeController {
    var shouldShake by mutableStateOf(false)
        private set

    fun shake() {
        shouldShake = true
    }

    fun reset() {
        shouldShake = false
    }
}

@Composable
fun Modifier.shake(controller: ShakeController): Modifier {
    val offsetX by animateFloatAsState(
        targetValue = if (controller.shouldShake) 10f else 0f,
        animationSpec = repeatable(
            iterations = 3,
            animation = tween(durationMillis = 50),
            repeatMode = RepeatMode.Reverse
        ),
        finishedListener = { controller.reset() },
        label = "shake_offset"
    )

    return this.offset(x = offsetX.dp)
}

/**
 * Pulse Animation für Attention
 */
@Composable
fun PulseEffect(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(modifier = modifier.scale(scale)) {
        content()
    }
}

/**
 * Shimmer Loading Effect für Cards
 */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 100.dp
) {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.horizontalGradient(
        colors = shimmerColors,
        startX = translateAnim,
        endX = translateAnim + 300f
    )

    Card(
        modifier = modifier.height(height),
        shape = ShapeHeroCard
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
        )
    }
}
