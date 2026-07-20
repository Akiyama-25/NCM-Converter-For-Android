package com.example.ncmconverter

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.ncmconverter.decrypt.model.DecryptState
import com.example.ncmconverter.metadata.FlacMetadataWriter
import com.example.ncmconverter.metadata.Mp3MetadataWriter
import com.example.ncmconverter.service.DecryptService
import com.example.ncmconverter.ui.ConvertViewModel
import com.example.ncmconverter.ui.FileItem
import com.example.ncmconverter.ui.HomeScreen
import com.example.ncmconverter.ui.theme.NcmConverterTheme
import com.example.ncmconverter.util.AppPrefs
import com.example.ncmconverter.util.FileUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val KEY_FILE_URIS = "file_uris"
        private const val KEY_FILE_NAMES = "file_names"
    }

    private lateinit var viewModel: ConvertViewModel
    private var lastAppliedLanguage: String? = null

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

            // Take persistable URI permission so files survive process death
            for (uri in ncmFiles) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
            }

            val fileList = ncmFiles.map { uri ->
                val name = FileUtils.queryFileName(uri, this) ?: uri.lastPathSegment ?: "unknown"
                uri to name
            }
            viewModel.addFiles(fileList) { uri -> FileUtils.queryFileSize(uri, this) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPrefs.init(this)

        // Apply saved language
        try {
            val lang = AppPrefs.appLanguage
            lastAppliedLanguage = lang
            val locales = if (lang == "system") LocaleListCompat.getEmptyLocaleList()
                          else LocaleListCompat.forLanguageTags(lang)
            AppCompatDelegate.setApplicationLocales(locales)
        } catch (_: Exception) {
        }

        viewModel = ViewModelProvider(this)[ConvertViewModel::class.java]

        // Restore file list after process death
        if (savedInstanceState != null && viewModel.files.value.isEmpty()) {
            val uriStrings = savedInstanceState.getStringArray(KEY_FILE_URIS)
            val names = savedInstanceState.getStringArray(KEY_FILE_NAMES)
            if (uriStrings != null && names != null && uriStrings.size == names.size) {
                val fileList = uriStrings.mapIndexed { i, s ->
                    android.net.Uri.parse(s) to names[i]
                }
                viewModel.addFiles(fileList) { uri -> FileUtils.queryFileSize(uri, this) }
            }
        }

        // Start DecryptService for background conversion (only when autoSave is enabled)
        if (AppPrefs.autoSave) {
            startForegroundService(Intent(this, DecryptService::class.java))
        }

        enableEdgeToEdge()
        window.setBackgroundDrawable(ColorDrawable(
            ContextCompat.getColor(this, R.color.window_bg)
        ))

        setContent {
            // Observe completed items for auto-save (only when autoSave is enabled)
            LaunchedEffect(Unit) {
                viewModel.completedItem.collect { item ->
                    if (AppPrefs.autoSave) {
                        saveResult(item)
                    }
                }
            }

            NcmConverterTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onPickFiles = { filePickerLauncher.launch(arrayOf("*/*")) },
                    onSaveFile = { item -> saveResult(item) },
                    onOpenSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                        if (Build.VERSION.SDK_INT >= 34) {
                            @Suppress("WrongConstant")
                            overrideActivityTransition(0, R.anim.slide_in_right, R.anim.slide_out_left)
                        } else {
                            @Suppress("DEPRECATION")
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val lang = AppPrefs.appLanguage
        if (lang != lastAppliedLanguage) {
            lastAppliedLanguage = lang
            try {
                val locales = if (lang == "system") LocaleListCompat.getEmptyLocaleList()
                              else LocaleListCompat.forLanguageTags(lang)
                AppCompatDelegate.setApplicationLocales(locales)
            } catch (_: Exception) {}
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val files = viewModel.files.value
        if (files.isNotEmpty()) {
            outState.putStringArray(KEY_FILE_URIS, files.map { it.uri.toString() }.toTypedArray())
            outState.putStringArray(KEY_FILE_NAMES, files.map { it.name }.toTypedArray())
        }
    }

    private fun saveResult(item: FileItem) {
        val result = item.result ?: return
        if (item.state != DecryptState.COMPLETED && item.state != DecryptState.COMPLETED_NO_LYRIC) return

        val mp3Writer = Mp3MetadataWriter()
        val flacWriter = FlacMetadataWriter()

        lifecycleScope.launch {
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
            } finally {
                // Release decrypted audio data from memory
                viewModel.clearResult(item.id)
            }
        }
    }
}
