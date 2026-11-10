package com.example.ncmconverter.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ncmconverter.R
import com.example.ncmconverter.decrypt.model.DecryptState
import com.example.ncmconverter.ui.theme.Error
import com.example.ncmconverter.ui.theme.Success

@Composable
fun FileItemView(
    item: FileItem,
    onDecrypt: () -> Unit,
    onRemove: () -> Unit,
    onSave: () -> Unit,
    autoSave: Boolean,
    enableCover: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val onSurface = colorScheme.onSurface
    val onSurfaceVariant = colorScheme.onSurfaceVariant

    val isDone = item.state == DecryptState.COMPLETED || item.state == DecryptState.COMPLETED_NO_LYRIC
    val isFailed = item.state == DecryptState.FAILED

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("file_item_card_${item.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isDone -> Success.copy(alpha = 0.35f)
                isFailed -> Error.copy(alpha = 0.4f)
                else -> colorScheme.outlineVariant.copy(alpha = 0.45f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Cover or M3E Expressive Tonal Icon Container
                val albumPic = item.result?.metadata?.albumPic
                if (enableCover && !albumPic.isNullOrBlank()) {
                    AsyncImage(
                        model = albumPic,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isDone -> Success.copy(alpha = 0.14f)
                            isFailed -> Error.copy(alpha = 0.14f)
                            else -> colorScheme.primaryContainer.copy(alpha = 0.65f)
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AudioFile,
                                contentDescription = null,
                                tint = when {
                                    isDone -> Success
                                    isFailed -> Error
                                    else -> primaryColor
                                },
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        color = onSurface,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = formatFileSize(item.size),
                                color = onSurfaceVariant,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        val ext = item.result?.extension?.uppercase()
                        if (!ext.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = primaryColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = ext,
                                    color = primaryColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (item.result?.lyric != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Success.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Lyrics,
                                        contentDescription = null,
                                        tint = Success,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "LRC",
                                        color = Success,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // M3E Action Pill Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (item.state) {
                        DecryptState.IDLE, DecryptState.FAILED -> {
                            FilledTonalIconButton(
                                onClick = onDecrypt,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("decrypt_item_button_${item.id}"),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = colorScheme.primaryContainer,
                                    contentColor = colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = stringResource(R.string.file_action_decrypt),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        DecryptState.COMPLETED, DecryptState.COMPLETED_NO_LYRIC -> {
                            if (!autoSave) {
                                FilledTonalIconButton(
                                    onClick = onSave,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("save_item_button_${item.id}"),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = Success.copy(alpha = 0.18f),
                                        contentColor = Success
                                    )
                                ) {
                                    Icon(
                                        Icons.Filled.SaveAlt,
                                        contentDescription = stringResource(R.string.file_action_save),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        else -> {}
                    }

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("remove_item_button_${item.id}")
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.file_action_remove),
                            tint = onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expressive Status Row & Progress Indicator
            AnimatedContent(
                targetState = item.state,
                transitionSpec = {
                    fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                        scaleIn(initialScale = 0.94f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) togetherWith
                        fadeOut(tween(150))
                },
                label = "stateTransition"
            ) { state ->
                when (state) {
                    DecryptState.IDLE -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(onSurfaceVariant.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.file_wait),
                                color = onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                    DecryptState.PARSING, DecryptState.DECRYPTING, DecryptState.WRITING_META -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = primaryColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (state) {
                                        DecryptState.PARSING -> stringResource(R.string.file_parsing)
                                        DecryptState.DECRYPTING -> stringResource(
                                            R.string.file_decrypting,
                                            (item.progress * 100).toInt()
                                        )
                                        DecryptState.WRITING_META -> stringResource(R.string.file_writing_meta)
                                        else -> ""
                                    },
                                    color = primaryColor,
                                    fontSize = 12.sp
                                )
                            }
                            if (state == DecryptState.DECRYPTING) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val animatedProgress by animateFloatAsState(
                                    targetValue = item.progress,
                                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                                    label = "progress"
                                )
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(CircleShape),
                                    color = primaryColor,
                                    trackColor = colorScheme.surfaceContainerHighest
                                )
                            }
                        }
                    }
                    DecryptState.SEARCHING_LYRIC -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = primaryColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.file_searching_lyric),
                                color = primaryColor,
                                fontSize = 12.sp
                            )
                        }
                    }
                    DecryptState.COMPLETED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Success,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                stringResource(
                                    R.string.file_completed,
                                    item.result?.extension?.uppercase() ?: ""
                                ),
                                color = Success,
                                fontSize = 12.sp
                            )
                        }
                    }
                    DecryptState.COMPLETED_NO_LYRIC -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Success,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                stringResource(
                                    R.string.file_completed,
                                    item.result?.extension?.uppercase() ?: ""
                                ),
                                color = Success,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "· " + stringResource(R.string.file_no_lyric),
                                color = onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                    DecryptState.FAILED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = Error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                item.error ?: stringResource(R.string.file_failed),
                                color = Error,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}
