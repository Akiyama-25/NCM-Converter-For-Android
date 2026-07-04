package com.example.ncmconverter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ncmconverter.decrypt.model.DecryptState
import com.example.ncmconverter.ui.theme.Error
import com.example.ncmconverter.ui.theme.Success

@Composable
fun FileItemView(
    item: FileItem,
    onDecrypt: () -> Unit,
    onRemove: () -> Unit,
    onSave: () -> Unit,
    manualSave: Boolean,
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
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    text = item.name,
                    color = onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatFileSize(item.size),
                    color = onSurfaceVariant,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                when (item.state) {
                    DecryptState.IDLE -> {
                        Text("等待转换", color = onSurfaceVariant, fontSize = 12.sp)
                    }
                    DecryptState.PARSING, DecryptState.DECRYPTING, DecryptState.WRITING_META -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = primaryColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (item.state) {
                                    DecryptState.PARSING -> "解析中..."
                                    DecryptState.DECRYPTING -> "解密中... ${(item.progress * 100).toInt()}%"
                                    DecryptState.WRITING_META -> "写入标签..."
                                    else -> ""
                                },
                                color = primaryColor,
                                fontSize = 12.sp
                            )
                        }
                        if (item.state == DecryptState.DECRYPTING) {
                            LinearProgressIndicator(
                                progress = { item.progress },
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(3.dp),
                                color = primaryColor,
                            )
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
                            Text("搜索歌词中...", color = primaryColor, fontSize = 12.sp)
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
                                "转换完成 · ${item.result?.extension?.uppercase() ?: ""}",
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
                                    "转换完成 · ${item.result?.extension?.uppercase() ?: ""}",
                                    color = Success,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                "未找到歌词",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                    DecryptState.FAILED -> {
                        Text(
                            item.error ?: "转换失败",
                            color = Error,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Actions
            when (item.state) {
                DecryptState.IDLE, DecryptState.FAILED -> {
                    IconButton(onClick = onDecrypt) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "解密",
                            tint = primaryColor
                        )
                    }
                }
                DecryptState.COMPLETED, DecryptState.COMPLETED_NO_LYRIC -> {
                    if (manualSave) {
                        IconButton(onClick = onSave) {
                            Icon(
                                Icons.Filled.SaveAlt,
                                contentDescription = "保存",
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
                    contentDescription = "移除",
                    tint = onSurfaceVariant
                )
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
