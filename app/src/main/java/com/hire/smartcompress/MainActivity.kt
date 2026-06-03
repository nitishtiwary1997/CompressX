package com.hire.smartcompress

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.hire.smartcompress.data.preferences.AppPreferences
import com.hire.smartcompress.domain.model.ThemeMode
import com.hire.smartcompress.presentation.navigation.AppNavigation
import com.hire.smartcompress.ui.theme.SmartCompressTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by appPreferences.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            SmartCompressTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}
