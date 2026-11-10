package com.example.ncmconverter.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var showBlueprintDialog by remember { mutableStateOf(false) }
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

    val activeFontLabel = remember(systemFontId) {
        SystemFontScanner.getDisplayName(systemFontId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.home_title),
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (isProcessing) {
                                Text(
                                    stringResource(R.string.home_decrypting),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.home_m3e_badge),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = { showBlueprintDialog = true },
                        modifier = Modifier.testTag("open_flutter_blueprint_button"),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Icon(
                            Icons.Filled.Devices,
                            contentDescription = stringResource(R.string.pref_flutter_blueprint)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("clear_list_button"),
                            shape = RoundedCornerShape(16.dp),
                            enabled = files.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.home_clear_list))
                        }

                        Button(
                            onClick = { viewModel.decryptAll() },
                            modifier = Modifier
                                .weight(1.25f)
                                .height(48.dp)
                                .testTag("convert_all_button"),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isProcessing && files.any {
                                it.state == DecryptState.IDLE || it.state == DecryptState.FAILED
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.home_convert_all))
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
            // FlClash-inspired M3E Expressive Hero Dashboard Card
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        M3EStatPill(
                            label = stringResource(R.string.home_stats_total),
                            value = totalCount.toString(),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        M3EStatPill(
                            label = stringResource(R.string.home_stats_done),
                            value = doneCount.toString(),
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        M3EStatPill(
                            label = stringResource(R.string.home_stats_pending),
                            value = pendingCount.toString(),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPickFiles,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pick_files_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = !isProcessing
                        ) {
                            Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.home_pick_files),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Surface(
                            onClick = onOpenSettings,
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
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
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
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

    if (showBlueprintDialog) {
        FlutterBlueprintDialog(onDismiss = { showBlueprintDialog = false })
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                fontSize = 18.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun FlutterBlueprintDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val blueprintText = remember {
        try {
            context.resources.openRawResource(R.raw.flutter_migration_blueprint)
                .bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            "docs/FLUTTER_MIGRATION_BLUEPRINT.md"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Devices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.pref_flutter_blueprint),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 440.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = blueprintText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Flutter Migration Plan", blueprintText))
                    Toast.makeText(
                        context,
                        context.getString(R.string.blueprint_copied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.log_action_copy))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.instructions_close))
            }
        }
    )
}
