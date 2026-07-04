package com.example.ncmconverter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.example.ncmconverter.decrypt.model.DecryptState
import com.example.ncmconverter.metadata.FlacMetadataWriter
import com.example.ncmconverter.metadata.Mp3MetadataWriter
import com.example.ncmconverter.ui.ConvertViewModel
import com.example.ncmconverter.ui.FileItem
import com.example.ncmconverter.ui.HomeScreen
import com.example.ncmconverter.ui.theme.NcmConverterTheme
import com.example.ncmconverter.util.AppPrefs
import com.example.ncmconverter.util.FileUtils
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: ConvertViewModel

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val ncmFiles = uris.filter { uri ->
                val name = FileUtils.queryFileName(uri, this)
                name.endsWith(".ncm", ignoreCase = true)
            }
            if (ncmFiles.isEmpty()) {
                Toast.makeText(this, "未选择任何 .ncm 文件", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            if (ncmFiles.size < uris.size) {
                Toast.makeText(this, "已过滤非 .ncm 文件", Toast.LENGTH_SHORT).show()
            }
            val capped = if (ncmFiles.size > 50) {
                Toast.makeText(this, "最多选择 50 个文件，已截取前 50 个", Toast.LENGTH_LONG).show()
                ncmFiles.take(50)
            } else ncmFiles

            val fileList = capped.map { uri ->
                val name = FileUtils.queryFileName(uri, this) ?: uri.lastPathSegment ?: "unknown"
                uri to name
            }
            viewModel.addFiles(fileList) { uri -> FileUtils.queryFileSize(uri, this) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPrefs.init(this)

        viewModel = ConvertViewModel()

        setContent {
            // Observe completed items for auto-save
            LaunchedEffect(Unit) {
                viewModel.completedItem.collect { item ->
                    if (!AppPrefs.manualSave) {
                        saveResult(item)
                    }
                }
            }

            NcmConverterTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onPickFiles = { filePickerLauncher.launch(arrayOf("*/*")) },
                    onSaveFile = { item -> saveResult(item) },
                    readBytes = { uri -> FileUtils.readBytes(uri, this) },
                    openInputStream = { uri -> contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException("无法打开文件") },
                    onOpenSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                )
            }
        }
    }

    private fun saveResult(item: FileItem) {
        val result = item.result ?: return
        if (item.state != DecryptState.COMPLETED && item.state != DecryptState.COMPLETED_NO_LYRIC) return

        val mp3Writer = Mp3MetadataWriter()
        val flacWriter = FlacMetadataWriter()

        kotlinx.coroutines.MainScope().launch {
            try {
                val writer: suspend (ByteArray) -> ByteArray = when (result.extension) {
                    "flac" -> { bytes -> flacWriter.write(bytes, result.metadata, result.lyric) }
                    else -> { bytes -> mp3Writer.write(bytes, result.metadata, result.lyric) }
                }

                val uri = FileUtils.saveToCustomPath(
                    this@MainActivity, result, AppPrefs.customPath, writer
                )

                if (uri != null) {
                    Toast.makeText(this@MainActivity, "已保存到 ${AppPrefs.customPath}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "保存失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
