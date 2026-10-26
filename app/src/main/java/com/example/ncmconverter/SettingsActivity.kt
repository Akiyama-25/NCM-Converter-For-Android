package com.example.ncmconverter

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
                SettingsScreen(
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onPickOutputFolder = {
                        openDocumentTreeLauncher.launch(null)
                    }
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT < 34) {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}
