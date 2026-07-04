package com.example.ncmconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.ncmconverter.ui.SettingsScreen
import com.example.ncmconverter.ui.theme.NcmConverterTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NcmConverterTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}
