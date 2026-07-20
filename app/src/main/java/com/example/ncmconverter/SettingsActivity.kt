package com.example.ncmconverter

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.content.ContextCompat
import com.example.ncmconverter.ui.SettingsScreen
import com.example.ncmconverter.ui.theme.NcmConverterTheme

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    SettingsScreen(onBack = { finish() })
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
