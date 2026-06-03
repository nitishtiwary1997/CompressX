package com.hire.smartcompress.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hire.smartcompress.data.preferences.AppPreferences
import com.hire.smartcompress.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val defaultImageQuality: Int = 80,
    val autoDeleteOriginal: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val videoDefaultResolution: String = "P720",
    val pdfDefaultLevel: String = "MEDIUM"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        prefs.defaultImageQuality,
        prefs.autoDeleteOriginal,
        prefs.themeMode,
        prefs.videoDefaultResolution,
        prefs.pdfDefaultLevel
    ) { quality, autoDelete, theme, videoRes, pdfLevel ->
        SettingsUiState(quality, autoDelete, theme, videoRes, pdfLevel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setDefaultImageQuality(q: Int) { viewModelScope.launch { prefs.setDefaultImageQuality(q) } }
    fun setAutoDeleteOriginal(v: Boolean) { viewModelScope.launch { prefs.setAutoDeleteOriginal(v) } }
    fun setThemeMode(mode: ThemeMode) { viewModelScope.launch { prefs.setThemeMode(mode) } }
    fun setVideoResolution(res: String) { viewModelScope.launch { prefs.setVideoDefaultResolution(res) } }
    fun setPdfLevel(level: String) { viewModelScope.launch { prefs.setPdfDefaultLevel(level) } }
}
