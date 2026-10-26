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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Container implementing standard AOSP predictive back gesture with live gesture-following
 * scale down preview, dynamic rounded card morphing, and reveal of the underlying page.
 *
 * Visual spec:
 * - Overlay scales down from 1.00 to 0.90 following swipe progress
 * - Dynamic rounded corners morph from 0.dp to 28.dp (strictly non-negative)
 * - Floating card shadow elevation (up to 12.dp)
 * - Underlying background page reveals beneath with AOSP parallax scale (0.94 -> 1.00) and fading scrim
 * - Smooth transition on cancellation (non-overshooting to prevent negative corner radius)
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

    // Active animation job holder to manage gesture completion/cancellation transitions safely
    var animJob by remember { mutableStateOf<Job?>(null) }

    // Predictive back gesture state
    var isPredicting by remember { mutableStateOf(false) }
    var predictiveProgress by remember { mutableFloatStateOf(0f) }
    var predictiveSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    var commitExitFraction by remember { mutableFloatStateOf(0f) }

    // Handle isOverlayOpen property changes from parent
    LaunchedEffect(isOverlayOpen) {
        if (isOverlayOpen) {
            animJob?.cancel()
            internalOverlayOpen = true
            isPredicting = false
            predictiveProgress = 0f
            commitExitFraction = 0f
            enterExitAnim.snapTo(1f)
            enterExitAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
        } else if (commitExitFraction == 0f && enterExitAnim.value == 0f) {
            internalOverlayOpen = false
            enterExitAnim.snapTo(1f)
            isPredicting = false
            predictiveProgress = 0f
            commitExitFraction = 0f
        }
    }

    // Function to close overlay with smooth slide-out animation (used by top bar back button)
    val requestClose: () -> Unit = {
        if (internalOverlayOpen && !isPredicting && commitExitFraction == 0f) {
            animJob?.cancel()
            animJob = coroutineScope.launch {
                enterExitAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                )
                onCloseOverlay()
                internalOverlayOpen = false
                predictiveProgress = 0f
                commitExitFraction = 0f
            }
        }
    }

    // Predictive back gesture handler
    // Note: Do NOT add transient closing flags to enabled; enabled remains true while overlay is open
    // so gesture coroutines are never cancelled prematurely mid-animation.
    PredictiveBackHandler(enabled = isOverlayOpen && enterExitAnim.value == 0f) { progressFlow ->
        animJob?.cancel()
        isPredicting = true
        commitExitFraction = 0f
        try {
            progressFlow.collect { backEvent ->
                predictiveProgress = backEvent.progress.coerceIn(0f, 1f)
                predictiveSwipeEdge = backEvent.swipeEdge
            }
            // --- GESTURE COMMITTED (user released finger to finish back navigation) ---
            animJob = coroutineScope.launch {
                animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)
                ) { value, _ ->
                    commitExitFraction = value.coerceIn(0f, 1f)
                }
                onCloseOverlay()
                internalOverlayOpen = false
                isPredicting = false
                predictiveProgress = 0f
                commitExitFraction = 0f
            }
        } catch (e: CancellationException) {
            // --- GESTURE CANCELLED (user slid back to edge without committing) ---
            val startProgress = predictiveProgress.coerceIn(0f, 1f)
            animJob = coroutineScope.launch {
                animate(
                    initialValue = startProgress,
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                ) { value, _ ->
                    predictiveProgress = value.coerceIn(0f, 1f)
                }
                predictiveProgress = 0f
                isPredicting = false
            }
        }
    }

    // Calculate transformation values
    val isGestureMode = isPredicting || commitExitFraction > 0f

    val eased = FastOutSlowInEasing.transform(predictiveProgress.coerceIn(0f, 1f)).coerceIn(0f, 1f)
    val exitFraction = commitExitFraction.coerceIn(0f, 1f)

    val currentScale: Float
    val currentTranslationX: Float
    val currentAlpha: Float
    val currentCornerRadius: Dp
    val currentElevation: Dp

    val bgScale: Float
    val currentScrimAlpha: Float

    if (isGestureMode) {
        // Card scales down to 0.90 during drag, slightly further to 0.82 during commit exit
        currentScale = ((1f - 0.10f * eased) * (1f - 0.08f * exitFraction)).coerceIn(0.80f, 1f)
        // Corner radius strictly clamped in [0.dp, 28.dp] - NEVER negative
        currentCornerRadius = (28f * eased).coerceIn(0f, 28f).dp
        currentElevation = (12f * eased).coerceIn(0f, 12f).dp

        val edgeMultiplier = if (predictiveSwipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
        val dragOffset = edgeMultiplier * (density.density * 36f) * eased
        val commitOffset = (if (predictiveSwipeEdge == BackEventCompat.EDGE_RIGHT) -screenWidthPx else screenWidthPx) * exitFraction
        currentTranslationX = dragOffset + commitOffset
        currentAlpha = ((1f - 0.05f * eased) * (1f - exitFraction)).coerceIn(0f, 1f)

        // Parallax zoom on background content (0.94 -> 1.00)
        val totalProgress = (eased + (1f - eased) * exitFraction).coerceIn(0f, 1f)
        bgScale = (0.94f + 0.06f * totalProgress).coerceIn(0.90f, 1f)
        currentScrimAlpha = (0.25f * (1f - totalProgress)).coerceIn(0f, 0.25f)
    } else {
        val slideVal = enterExitAnim.value.coerceIn(0f, 1f)
        val openFraction = (1f - slideVal).coerceIn(0f, 1f)

        currentScale = 1f
        currentCornerRadius = 0.dp
        currentElevation = 0.dp
        currentTranslationX = screenWidthPx * slideVal
        currentAlpha = (1f - 0.3f * slideVal).coerceIn(0f, 1f)

        bgScale = (1f - 0.06f * openFraction).coerceIn(0.90f, 1f)
        currentScrimAlpha = (0.25f * openFraction).coerceIn(0f, 0.25f)
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
            val safeCornerRadius = currentCornerRadius.coerceAtLeast(0.dp)
            val safeElevation = currentElevation.coerceAtLeast(0.dp)
            val cardShape = RoundedCornerShape(safeCornerRadius)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = currentScale
                        scaleY = currentScale
                        translationX = currentTranslationX
                        alpha = currentAlpha
                        if (safeElevation > 0.dp) {
                            shadowElevation = safeElevation.toPx()
                        }
                        shape = cardShape
                        clip = safeCornerRadius > 0.dp
                    }
                    .shadow(
                        elevation = safeElevation,
                        shape = cardShape,
                        clip = false
                    )
                    .clip(cardShape)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                overlayContent(requestClose)
            }
        }
    }
}