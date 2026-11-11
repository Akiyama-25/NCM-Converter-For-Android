package com.example.ncmconverter.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ncmconverter.R
import com.example.ncmconverter.decrypt.model.DecryptState
import com.example.ncmconverter.ui.theme.SystemFontScanner
import com.example.ncmconverter.util.AppPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun HomeScreen(
    viewModel: ConvertViewModel,
    onPickFiles: () -> Unit,
    onSaveFile: (FileItem) -> Unit,
    onOpenSettings: () -> Unit,
    isBackEnabled: Boolean = true
) {
    val files by viewModel.files.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val gridColumns by AppPrefs.gridColumnsFlow.collectAsState()
    val portraitGridEnabled by AppPrefs.portraitGridEnabledFlow.collectAsState()
    val systemFontId by AppPrefs.systemFontIdFlow.collectAsState()
    val fontWeight by AppPrefs.fontWeightFlow.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity

    val effectiveColumns = if (gridColumns <= 1) 1
    else when (calculateWindowSizeClass(activity).widthSizeClass) {
        WindowWidthSizeClass.Compact -> if (portraitGridEnabled) gridColumns else 1
        else -> gridColumns
    }

    var showClearDialog by remember { mutableStateOf(false) }
    var newFileIds by remember { mutableStateOf(emptySet<Long>()) }
    var animatingOutItems by remember { mutableStateOf(emptyMap<Long, FileItem>()) }
    var lastBackPress by remember { mutableLongStateOf(0L) }

    // Double-tap back key or gesture to exit app
    BackHandler(enabled = isBackEnabled) {
        val now = System.currentTimeMillis()
        if (now - lastBackPress < 2000) {
            activity.finish()
        } else {
            lastBackPress = now
            Toast.makeText(activity, activity.getString(R.string.press_back_again), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(files.map { it.id }) {
        val currentIds = files.map { it.id }.toSet()
        val knownIds = newFileIds + animatingOutItems.keys
        val fresh = currentIds.filter { it !in knownIds }
        if (fresh.isNotEmpty()) {
            newFileIds = newFileIds + fresh.toSet()
            delay(1000)
            newFileIds = newFileIds - fresh.toSet()
        }
    }
    val displayFiles = files + animatingOutItems.values

    val totalCount = files.size
    val doneCount = files.count {
        it.state == DecryptState.COMPLETED || it.state == DecryptState.COMPLETED_NO_LYRIC
    }
    val pendingCount = files.count {
        it.state == DecryptState.IDLE || it.state == DecryptState.FAILED
    }

    val defaultFontLabel = stringResource(R.string.font_system)
    val activeFontLabel = remember(systemFontId, defaultFontLabel) {
        if (systemFontId == SystemFontScanner.ID_SYSTEM_DEFAULT || systemFontId.isBlank()) {
            defaultFontLabel
        } else {
            SystemFontScanner.getDisplayName(systemFontId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AdaptiveTopBarTitle(
                        title = stringResource(R.string.home_title),
                        subtitle = if (isProcessing) stringResource(R.string.home_decrypting) else null,
                        badge = stringResource(R.string.home_m3e_badge)
                    )
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("open_settings_button"),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = files.isNotEmpty(),
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                ) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    AdaptiveControlsRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalSpacing = 10.dp,
                        verticalSpacing = 8.dp
                    ) {
                        OutlinedButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("clear_list_button"),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            enabled = files.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.home_clear_list),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = { viewModel.decryptAll() },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("convert_all_button"),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            enabled = !isProcessing && files.any {
                                it.state == DecryptState.IDLE || it.state == DecryptState.FAILED
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.home_convert_all),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // M3E Expressive Hero Dashboard Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    AdaptiveControlsRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalSpacing = 8.dp,
                        verticalSpacing = 8.dp
                    ) {
                        M3EStatPill(
                            label = stringResource(R.string.home_stats_total),
                            value = totalCount.toString(),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                        M3EStatPill(
                            label = stringResource(R.string.home_stats_done),
                            value = doneCount.toString(),
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        M3EStatPill(
                            label = stringResource(R.string.home_stats_pending),
                            value = pendingCount.toString(),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AdaptiveControlsRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalSpacing = 8.dp,
                        verticalSpacing = 8.dp
                    ) {
                        Button(
                            onClick = onPickFiles,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("pick_files_button"),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = !isProcessing
                        ) {
                            Icon(
                                Icons.Filled.AddCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.home_pick_files),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            onClick = onOpenSettings,
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.FontDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$activeFontLabel · W$fontWeight",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = files.isEmpty(),
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith fadeOut(tween(200))
                },
                label = "emptyTransition"
            ) { isEmpty ->
                if (isEmpty) {
                    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.96f,
                        targetValue = 1.08f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier
                                    .size(104.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    },
                                shape = RoundedCornerShape(32.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.LibraryMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                stringResource(R.string.home_empty_hint),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.home_empty_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(effectiveColumns),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(displayFiles, key = { it.id }) { item ->
                            val isNew = item.id in newFileIds
                            val isExiting = item.id in animatingOutItems
                            var animPhase by remember { mutableStateOf(if (isNew) "waiting" else "idle") }

                            if (isExiting && animPhase != "exit") animPhase = "exit"

                            val targetAlpha = when (animPhase) {
                                "waiting" -> 0f
                                "exit" -> 0f
                                else -> 1f
                            }
                            val animatedAlpha by animateFloatAsState(
                                targetValue = targetAlpha,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "alpha"
                            )
                            val animatedOffsetX by animateFloatAsState(
                                targetValue = when (animPhase) {
                                    "waiting" -> -0.25f
                                    "exit" -> 0.8f
                                    else -> 0f
                                },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "offsetX"
                            )

                            LaunchedEffect(animPhase) {
                                when (animPhase) {
                                    "waiting" -> {
                                        delay(80)
                                        animPhase = "enter"
                                    }
                                    "exit" -> {
                                        snapshotFlow { animatedAlpha < 0.05f }.first { it }
                                        animatingOutItems = animatingOutItems - item.id
                                    }
                                }
                            }

                            FileItemView(
                                item = item,
                                onDecrypt = { viewModel.decryptSingle(item.id) },
                                onRemove = {
                                    animatingOutItems = animatingOutItems + (item.id to item)
                                    viewModel.removeFile(item.id)
                                },
                                onSave = { onSaveFile(item) },
                                autoSave = AppPrefs.autoSave,
                                enableCover = AppPrefs.enableCover,
                                modifier = Modifier.graphicsLayer {
                                    alpha = animatedAlpha
                                    translationX = size.width * animatedOffsetX
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.home_clear_dialog_title)) },
            text = { Text(stringResource(R.string.home_clear_dialog_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.home_clear_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.home_cancel)) }
            }
        )
    }
}

@Composable
private fun AdaptiveTopBarTitle(
    title: String,
    subtitle: String?,
    badge: String
) {
    SubcomposeLayout(modifier = Modifier.fillMaxWidth()) { constraints ->
        val maxWidth = constraints.maxWidth
        val gap = 8.dp.roundToPx()

        val titleMeasurable = subcompose("title") {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }.first()

        val badgeMeasurable = subcompose("badge") {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = badge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }.first()

        val titleIntrinsicW = titleMeasurable.maxIntrinsicWidth(Constraints.Infinity)
        val badgeIntrinsicW = badgeMeasurable.maxIntrinsicWidth(Constraints.Infinity)

        if (titleIntrinsicW + gap + badgeIntrinsicW <= maxWidth) {
            val badgePlaceable = badgeMeasurable.measure(Constraints(maxWidth = maxWidth))
            val titlePlaceable = titleMeasurable.measure(
                Constraints(maxWidth = (maxWidth - badgePlaceable.width - gap).coerceAtLeast(0))
            )
            val height = maxOf(titlePlaceable.height, badgePlaceable.height)
            layout(maxWidth, height) {
                titlePlaceable.placeRelative(0, (height - titlePlaceable.height) / 2)
                badgePlaceable.placeRelative(
                    titlePlaceable.width + gap,
                    (height - badgePlaceable.height) / 2
                )
            }
        } else {
            val compactTitleMeasurable = subcompose("compactTitle") {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }.first()
            val compactTitleW = compactTitleMeasurable.maxIntrinsicWidth(Constraints.Infinity)
            if (compactTitleW + gap + badgeIntrinsicW <= maxWidth) {
                val badgePlaceable = badgeMeasurable.measure(Constraints(maxWidth = maxWidth))
                val titlePlaceable = compactTitleMeasurable.measure(
                    Constraints(maxWidth = (maxWidth - badgePlaceable.width - gap).coerceAtLeast(0))
                )
                val height = maxOf(titlePlaceable.height, badgePlaceable.height)
                layout(maxWidth, height) {
                    titlePlaceable.placeRelative(0, (height - titlePlaceable.height) / 2)
                    badgePlaceable.placeRelative(
                        titlePlaceable.width + gap,
                        (height - badgePlaceable.height) / 2
                    )
                }
            } else {
                val titlePlaceable = compactTitleMeasurable.measure(Constraints(maxWidth = maxWidth))
                val badgePlaceable = badgeMeasurable.measure(Constraints(maxWidth = maxWidth))
                val vGap = 2.dp.roundToPx()
                val height = titlePlaceable.height + vGap + badgePlaceable.height
                layout(maxWidth, height) {
                    titlePlaceable.placeRelative(0, 0)
                    badgePlaceable.placeRelative(0, titlePlaceable.height + vGap)
                }
            }
        }
    }
}

/**
 * Width-adaptive row container:
 * Measures each child's natural single-line intrinsic width.
 * - If all children fit on one row within the available width, allocates each child its
 *   intrinsic width plus a proportional share of remaining space so no control ever wraps or clips.
 * - If combined intrinsic widths exceed available width, wraps controls into multiple rows
 *   and expands items on each row to fill 100% of the available width.
 */
@Composable
fun AdaptiveControlsRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    equalMinWeight: Boolean = false,
    content: @Composable () -> Unit
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val measurables = subcompose("items", content)
        if (measurables.isEmpty()) {
            return@SubcomposeLayout layout(0, 0) {}
        }
        val hGap = horizontalSpacing.roundToPx()
        val vGap = verticalSpacing.roundToPx()
        val maxWidth = constraints.maxWidth

        val intrinsicWidths = measurables.map {
            it.maxIntrinsicWidth(Constraints.Infinity).coerceAtLeast(1)
        }
        val totalGap = hGap * (measurables.size - 1)
        val totalIntrinsic = intrinsicWidths.sum() + totalGap

        if (totalIntrinsic <= maxWidth) {
            val extraWidth = (maxWidth - totalIntrinsic).coerceAtLeast(0)
            val sumIntrinsic = intrinsicWidths.sum().toFloat().coerceAtLeast(1f)
            var allocatedSum = 0
            val allocatedWidths = IntArray(measurables.size) { i ->
                if (i == measurables.lastIndex) {
                    (maxWidth - totalGap - allocatedSum).coerceAtLeast(intrinsicWidths[i])
                } else {
                    val share = if (equalMinWeight) {
                        extraWidth / measurables.size
                    } else {
                        (extraWidth * (intrinsicWidths[i] / sumIntrinsic)).toInt()
                    }
                    val w = intrinsicWidths[i] + share
                    allocatedSum += w
                    w
                }
            }

            val prelimHeights = measurables.mapIndexed { i, m ->
                m.maxIntrinsicHeight(allocatedWidths[i])
            }
            val targetHeight = prelimHeights.maxOrNull() ?: 0

            val placeables = measurables.mapIndexed { i, m ->
                m.measure(
                    Constraints(
                        minWidth = allocatedWidths[i],
                        maxWidth = allocatedWidths[i],
                        minHeight = targetHeight,
                        maxHeight = Constraints.Infinity
                    )
                )
            }
            val finalHeight = placeables.maxOfOrNull { it.height } ?: targetHeight

            layout(maxWidth, finalHeight) {
                var x = 0
                placeables.forEach { p ->
                    p.placeRelative(x, (finalHeight - p.height) / 2)
                    x += p.width + hGap
                }
            }
        } else {
            val rows = mutableListOf<MutableList<Int>>()
            var currentRow = mutableListOf<Int>()
            var currentRowWidth = 0

            for (i in measurables.indices) {
                val w = intrinsicWidths[i]
                val needed = if (currentRow.isEmpty()) w else currentRowWidth + hGap + w
                if (currentRow.isNotEmpty() && needed > maxWidth) {
                    rows.add(currentRow)
                    currentRow = mutableListOf(i)
                    currentRowWidth = w
                } else {
                    currentRow.add(i)
                    currentRowWidth = needed
                }
            }
            if (currentRow.isNotEmpty()) {
                rows.add(currentRow)
            }

            val measuredRows = rows.map { rowIndices ->
                val rowGap = hGap * (rowIndices.size - 1)
                val rowIntrinsicSum = rowIndices.sumOf { intrinsicWidths[it] }
                val extra = (maxWidth - rowGap - rowIntrinsicSum).coerceAtLeast(0)
                val sumF = rowIntrinsicSum.toFloat().coerceAtLeast(1f)
                var used = 0
                val widths = IntArray(rowIndices.size) { pos ->
                    val idx = rowIndices[pos]
                    if (pos == rowIndices.lastIndex) {
                        (maxWidth - rowGap - used).coerceAtLeast(1)
                    } else {
                        val w = (intrinsicWidths[idx] + (extra * (intrinsicWidths[idx] / sumF)).toInt())
                            .coerceAtMost(maxWidth - rowGap - used)
                        used += w
                        w
                    }
                }
                val prelimRowHeights = rowIndices.mapIndexed { pos, idx ->
                    measurables[idx].maxIntrinsicHeight(widths[pos].coerceAtLeast(1))
                }
                val rowTargetH = prelimRowHeights.maxOrNull() ?: 0
                rowIndices.mapIndexed { pos, idx ->
                    val w = widths[pos].coerceAtLeast(1)
                    measurables[idx].measure(
                        Constraints(
                            minWidth = w,
                            maxWidth = w,
                            minHeight = rowTargetH,
                            maxHeight = Constraints.Infinity
                        )
                    )
                }
            }

            val rowHeights = measuredRows.map { r -> r.maxOfOrNull { it.height } ?: 0 }
            val totalHeight = rowHeights.sum() + vGap * (measuredRows.size - 1).coerceAtLeast(0)

            layout(maxWidth, totalHeight) {
                var y = 0
                measuredRows.forEachIndexed { rIdx, rPlaceables ->
                    var x = 0
                    val rH = rowHeights[rIdx]
                    rPlaceables.forEach { p ->
                        p.placeRelative(x, y + (rH - p.height) / 2)
                        x += p.width + hGap
                    }
                    y += rH + vGap
                }
            }
        }
    }
}

@Composable
private fun M3EStatPill(
    label: String,
    value: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
