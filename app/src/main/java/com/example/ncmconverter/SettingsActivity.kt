package com.example.ncmconverter

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.content.ContextCompat
import com.example.ncmconverter.ui.SettingsScreen
import com.example.ncmconverter.ui.theme.NcmConverterTheme
import com.example.ncmconverter.util.AppPrefs

class SettingsActivity : AppCompatActivity() {

    private lateinit var openDocumentTreeLauncher: androidx.activity.result.ActivityResultLauncher<Uri?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register the document-tree picker launcher
        openDocumentTreeLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri != null) {
                // Persist the permission so the URI remains valid after app restart
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                AppPrefs.customOutputUri = uri.toString()
            }
        }

        enableEdgeToEdge()
        window.setBackgroundDrawable(ColorDrawable(
            ContextCompat.getColor(this, R.color.window_bg)
        ))

        setContent {
            NcmConverterTheme {
                var backProgress by remember { mutableFloatStateOf(0f) }

                PredictiveBackHandler { progress ->
                    backProgress = 0f
                    progress.collect { event ->
                        backProgress = event.progress
                    }
                    finish()
                }

                Box(
                    modifier = Modifier.graphicsLayer {
                        val scale = 1f - backProgress * 0.1f
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - backProgress * 0.5f
                        translationX = size.width * backProgress * 0.3f
                    }
                ) {
                    SettingsScreen(
                        onBack = { finish() },
                        onPickOutputFolder = {
                            openDocumentTreeLauncher.launch(null)
                        }
                    )
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= 34) {
            @Suppress("WrongConstant")
            overrideActivityTransition(1, R.anim.slide_in_left, R.anim.slide_out_right)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}
