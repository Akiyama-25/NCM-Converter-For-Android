package com.example.ncmconverter.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = item.name,
                color = onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Top) {
                val albumPic = item.result?.metadata?.albumPic
                if (enableCover && !albumPic.isNullOrBlank()) {
                    AsyncImage(
                        model = albumPic,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.AudioFile,
                        contentDescription = null,
                        tint = when (item.state) {
                            DecryptState.COMPLETED, DecryptState.COMPLETED_NO_LYRIC -> Success
                            DecryptState.FAILED -> Error
                            else -> primaryColor
                        },
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatFileSize(item.size),
                        color = onSurfaceVariant,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    AnimatedContent(
                        targetState = item.state,
                        transitionSpec = {
                            fadeIn(tween(250)) + scaleIn(
                                initialScale = 0.92f,
                                animationSpec = tween(250)
                            ) togetherWith fadeOut(tween(150))
                        },
                        label = "stateTransition"
                    ) { state ->
                    when (state) {
                        DecryptState.IDLE -> {
                            Text(stringResource(R.string.file_wait), color = onSurfaceVariant, fontSize = 12.sp)
                        }
                        DecryptState.PARSING, DecryptState.DECRYPTING, DecryptState.WRITING_META -> {
                            Column {
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
                                            DecryptState.DECRYPTING -> stringResource(R.string.file_decrypting, (item.progress * 100).toInt())
                                            DecryptState.WRITING_META -> stringResource(R.string.file_writing_meta)
                                            else -> ""
                                        },
                                        color = primaryColor,
                                        fontSize = 12.sp
                                    )
                                }
                                if (state == DecryptState.DECRYPTING) {
                                    val animatedProgress by animateFloatAsState(
                                        targetValue = item.progress,
                                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                                        label = "progress"
                                    )
                                    LinearProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .height(3.dp),
                                        color = primaryColor,
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
                                Text(stringResource(R.string.file_searching_lyric), color = primaryColor, fontSize = 12.sp)
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
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    stringResource(R.string.file_completed, item.result?.extension?.uppercase() ?: ""),
                                    color = Success,
                                    fontSize = 12.sp
                                )
                                if (item.result?.lyric != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Filled.Lyrics,
                                        contentDescription = null,
                                        tint = Success,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        DecryptState.COMPLETED_NO_LYRIC -> {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Success,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        stringResource(R.string.file_completed, item.result?.extension?.uppercase() ?: ""),
                                        color = Success,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    stringResource(R.string.file_no_lyric),
                                    color = onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        DecryptState.FAILED -> {
                            Text(
                                item.error ?: stringResource(R.string.file_failed),
                                color = Error,
                                fontSize = 12.sp
                            )
                        }
                    }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (item.state) {
                    DecryptState.IDLE, DecryptState.FAILED -> {
                        IconButton(onClick = onDecrypt) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.file_action_decrypt),
                                tint = primaryColor
                            )
                        }
                    }
                    DecryptState.COMPLETED, DecryptState.COMPLETED_NO_LYRIC -> {
                        if (!autoSave) {
                            IconButton(onClick = onSave) {
                                Icon(
                                    Icons.Filled.SaveAlt,
                                    contentDescription = stringResource(R.string.file_action_save),
                                    tint = Success
                                )
                            }
                        }
                    }
                    else -> {}
                }

                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.file_action_remove),
                        tint = onSurfaceVariant
                    )
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
