package com.example.ncmconverter.ui

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Container implementing standard AOSP predictive back gesture with live gesture-following
 * scale down preview, dynamic rounded card morphing, and reveal of the underlying page.
 *
 * Visual spec:
 * - Overlay scales down from 1.00 to 0.90 following swipe progress
 * - Dynamic rounded corners morph from 0.dp to 28.dp
 * - Floating card shadow elevation (up to 12.dp)
 * - Underlying background page reveals beneath with AOSP parallax scale (0.94 -> 1.00) and fading scrim
 * - Smooth physics spring on gesture cancellation
 * - Smooth slide-out on gesture commit or manual back button press
 */
@Composable
fun PredictiveBackContainer(
    isOverlayOpen: Boolean,
    onCloseOverlay: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundContent: @Composable () -> Unit,
    overlayContent: @Composable (requestClose: () -> Unit) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

    // Internal composition state of overlay
    var internalOverlayOpen by remember { mutableStateOf(isOverlayOpen) }

    // Regular slide enter/exit animator (0f = settled on-screen, 1f = offscreen to the right)
    val enterExitAnim = remember { Animatable(if (isOverlayOpen) 0f else 1f) }

    // Predictive back gesture state
    var isPredicting by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }
    var predictiveProgress by remember { mutableFloatStateOf(0f) }
    var predictiveSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    var commitExitFraction by remember { mutableFloatStateOf(0f) }

    // Handle isOverlayOpen property changes from parent
    LaunchedEffect(isOverlayOpen) {
        if (isOverlayOpen) {
            internalOverlayOpen = true
            isClosing = false
            isPredicting = false
            predictiveProgress = 0f
            commitExitFraction = 0f
            enterExitAnim.snapTo(1f)
            enterExitAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
        } else if (!isClosing) {
            internalOverlayOpen = false
            enterExitAnim.snapTo(1f)
            predictiveProgress = 0f
            commitExitFraction = 0f
        }
    }

    // Function to close overlay with smooth slide-out animation (used by top bar back button)
    val requestClose: () -> Unit = {
        if (!isClosing && !isPredicting && internalOverlayOpen) {
            coroutineScope.launch {
                isClosing = true
                enterExitAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                )
                onCloseOverlay()
                internalOverlayOpen = false
                isClosing = false
            }
        }
    }

    // Predictive back gesture handler
    PredictiveBackHandler(enabled = isOverlayOpen && enterExitAnim.value == 0f && !isClosing) { progressFlow ->
        isPredicting = true
        try {
            progressFlow.collect { backEvent ->
                predictiveProgress = backEvent.progress
                predictiveSwipeEdge = backEvent.swipeEdge
            }
            // --- GESTURE COMMITTED ---
            isClosing = true
            val commitAnim = Animatable(0f)
            commitAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            ) {
                commitExitFraction = this.value
            }
            onCloseOverlay()
            internalOverlayOpen = false
            isClosing = false
            isPredicting = false
            predictiveProgress = 0f
            commitExitFraction = 0f
        } catch (e: CancellationException) {
            // --- GESTURE CANCELLED (user slid back to edge) ---
            withContext(NonCancellable) {
                val cancelAnim = Animatable(predictiveProgress)
                cancelAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) {
                    predictiveProgress = this.value
                }
                predictiveProgress = 0f
                isPredicting = false
            }
            throw e
        }
    }

    // Calculate transformation values
    val isGestureMode = isPredicting || commitExitFraction > 0f

    val currentScale: Float
    val currentTranslationX: Float
    val currentAlpha: Float
    val currentCornerRadius: Dp
    val currentElevation: Dp

    val bgScale: Float
    val currentScrimAlpha: Float

    if (isGestureMode) {
        val eased = FastOutSlowInEasing.transform(predictiveProgress)
        // Card scales down to 0.90 during drag, slightly further to 0.85 during commit exit
        currentScale = (1f - 0.10f * eased) * (1f - 0.05f * commitExitFraction)
        currentCornerRadius = (28f * eased).dp
        currentElevation = (12f * eased).dp

        val edgeMultiplier = if (predictiveSwipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
        val dragOffset = edgeMultiplier * (density.density * 36f) * eased
        val commitOffset = (if (predictiveSwipeEdge == BackEventCompat.EDGE_RIGHT) -screenWidthPx else screenWidthPx) * commitExitFraction
        currentTranslationX = dragOffset + commitOffset
        currentAlpha = (1f - 0.08f * eased) * (1f - commitExitFraction)

        // Parallax zoom on background content (0.94 -> 1.00)
        val totalProgress = (eased + (1f - eased) * commitExitFraction).coerceIn(0f, 1f)
        bgScale = 0.94f + 0.06f * totalProgress
        currentScrimAlpha = 0.25f * (1f - totalProgress)
    } else {
        val openFraction = (1f - enterExitAnim.value).coerceIn(0f, 1f)
        currentScale = 1f
        currentCornerRadius = 0.dp
        currentElevation = 0.dp
        currentTranslationX = screenWidthPx * enterExitAnim.value
        currentAlpha = 1f - (0.3f * enterExitAnim.value)

        bgScale = 1f - (0.06f * openFraction)
        currentScrimAlpha = 0.25f * openFraction
    }

    Box(modifier = modifier.fillMaxSize()) {
        // --- LAYER 1: Background Screen (HomeScreen) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = bgScale
                    scaleY = bgScale
                    alpha = 1f
                }
        ) {
            backgroundContent()
        }

        // --- LAYER 2: Dim Scrim over Background Screen ---
        if (internalOverlayOpen && currentScrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = currentScrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            // Consume clicks so underlying home buttons cannot be tapped while overlay is open
                        }
                    )
            )
        }

        // --- LAYER 3: Overlay Screen (SettingsScreen) ---
        if (internalOverlayOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = currentScale
                        scaleY = currentScale
                        translationX = currentTranslationX
                        alpha = currentAlpha
                        shadowElevation = currentElevation.toPx()
                        shape = RoundedCornerShape(currentCornerRadius)
                        clip = currentCornerRadius > 0.dp
                    }
                    .shadow(
                        elevation = currentElevation,
                        shape = RoundedCornerShape(currentCornerRadius),
                        clip = false
                    )
                    .clip(RoundedCornerShape(currentCornerRadius))
                    .background(MaterialTheme.colorScheme.background)
            ) {
                overlayContent(requestClose)
            }
        }
    }
}