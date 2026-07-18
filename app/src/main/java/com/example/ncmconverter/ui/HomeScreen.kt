package com.example.ncmconverter.ui

import android.app.Activity
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ncmconverter.R
import com.example.ncmconverter.decrypt.model.DecryptState
import com.example.ncmconverter.util.AppPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun HomeScreen(
    viewModel: ConvertViewModel,
    onPickFiles: () -> Unit,
    onSaveFile: (FileItem) -> Unit,
    readBytes: (Uri) -> ByteArray,
    openInputStream: (Uri) -> InputStream,
    onOpenSettings: () -> Unit
) {
    val files by viewModel.files.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val gridColumns by AppPrefs.gridColumnsFlow.collectAsState()
    val activity = LocalContext.current as Activity

    val effectiveColumns = if (gridColumns <= 1) 1
        else when (calculateWindowSizeClass(activity).widthSizeClass) {
            WindowWidthSizeClass.Compact -> 1
            else -> gridColumns
        }

    var showClearDialog by remember { mutableStateOf(false) }
    var newFileIds by remember { mutableStateOf(emptySet<Long>()) }
    var animatingOutItems by remember { mutableStateOf(emptyMap<Long, FileItem>()) }
    LaunchedEffect(files.map { it.id }) {
        val currentIds = files.map { it.id }.toSet()
        val previousIds = currentIds + animatingOutItems.keys
        val added = currentIds - (previousIds - animatingOutItems.keys - currentIds)
        // Track truly new files (not already in previous list, not animating out)
        val knownIds = newFileIds + animatingOutItems.keys
        val fresh = currentIds.filter { it !in knownIds && it !in (newFileIds) }
        if (fresh.isNotEmpty()) {
            newFileIds = newFileIds + fresh.toSet()
            delay(1000)
            newFileIds = newFileIds - fresh.toSet()
        }
    }
    val displayFiles = files + animatingOutItems.values

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.home_title), fontWeight = FontWeight.Bold)
                        if (isProcessing) {
                            Text(stringResource(R.string.home_decrypting), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Button(
                onClick = onPickFiles,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = !isProcessing
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.home_pick_files))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            AnimatedContent(
                targetState = files.isEmpty(),
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                },
                label = "emptyTransition"
            ) { isEmpty ->
            if (isEmpty) {
                val infiniteTransition = rememberInfiniteTransition(label = "breathe")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f, targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ), label = "scale"
                )
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ), label = "alpha"
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .size(72.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.home_empty_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.home_empty_desc),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(effectiveColumns),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                            animationSpec = tween(300),
                            label = "alpha"
                        )
                        val animatedOffsetX by animateFloatAsState(
                            targetValue = when (animPhase) {
                                "waiting" -> -0.3f
                                "exit" -> 1f
                                else -> 0f
                            },
                            animationSpec = tween(300),
                            label = "offsetX"
                        )

                        LaunchedEffect(animPhase) {
                            when (animPhase) {
                                "waiting" -> { delay(1000); animPhase = "enter" }
                                "exit" -> {
                                    snapshotFlow { animatedAlpha == 0f }.first { it }
                                    animatingOutItems = animatingOutItems - item.id
                                }
                            }
                        }

                        FileItemView(
                            item = item,
                            onDecrypt = { viewModel.decryptSingle(item.id, readBytes, openInputStream) },
                            onRemove = {
                                animatingOutItems = animatingOutItems + (item.id to item)
                                viewModel.removeFile(item.id)
                            },
                            onSave = { onSaveFile(item) },
                            manualSave = AppPrefs.manualSave,
                            enableCover = AppPrefs.enableCover,
                            modifier = Modifier.graphicsLayer {
                                alpha = animatedAlpha
                                translationX = size.width * animatedOffsetX
                            }
                        )
                    }
                }
            }
            } // AnimatedContent

            AnimatedVisibility(
                visible = files.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled = files.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.home_clear_list))
                        }

                        Button(
                            onClick = { viewModel.decryptAll(readBytes, openInputStream) },
                            modifier = Modifier.weight(1f),
                            enabled = !isProcessing && files.any {
                                it.state == DecryptState.IDLE || it.state == DecryptState.FAILED
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.home_convert_all))
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
