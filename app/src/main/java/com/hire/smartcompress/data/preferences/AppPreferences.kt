package com.hire.smartcompress.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hire.smartcompress.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("app_preferences")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DEFAULT_IMAGE_QUALITY = intPreferencesKey("default_image_quality")
        val DEFAULT_SAVE_LOCATION = stringPreferencesKey("default_save_location")
        val AUTO_DELETE_ORIGINAL = booleanPreferencesKey("auto_delete_original")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val VIDEO_DEFAULT_RESOLUTION = stringPreferencesKey("video_default_resolution")
        val PDF_DEFAULT_LEVEL = stringPreferencesKey("pdf_default_level")
    }

    val defaultImageQuality: Flow<Int> = context.dataStore.data.map {
        it[Keys.DEFAULT_IMAGE_QUALITY] ?: 80
    }

    val defaultSaveLocation: Flow<String> = context.dataStore.data.map {
        it[Keys.DEFAULT_SAVE_LOCATION] ?: ""
    }

    val autoDeleteOriginal: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.AUTO_DELETE_ORIGINAL] ?: false
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map {
        val name = it[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        ThemeMode.valueOf(name)
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.FIRST_LAUNCH] ?: true
    }

    val videoDefaultResolution: Flow<String> = context.dataStore.data.map {
        it[Keys.VIDEO_DEFAULT_RESOLUTION] ?: "P720"
    }

    val pdfDefaultLevel: Flow<String> = context.dataStore.data.map {
        it[Keys.PDF_DEFAULT_LEVEL] ?: "MEDIUM"
    }

    suspend fun setDefaultImageQuality(quality: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_IMAGE_QUALITY] = quality }
    }

    suspend fun setDefaultSaveLocation(path: String) {
        context.dataStore.edit { it[Keys.DEFAULT_SAVE_LOCATION] = path }
    }

    suspend fun setAutoDeleteOriginal(auto: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_DELETE_ORIGINAL] = auto }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setFirstLaunchDone() {
        context.dataStore.edit { it[Keys.FIRST_LAUNCH] = false }
    }

    suspend fun setVideoDefaultResolution(resolution: String) {
        context.dataStore.edit { it[Keys.VIDEO_DEFAULT_RESOLUTION] = resolution }
    }

    suspend fun setPdfDefaultLevel(level: String) {
        context.dataStore.edit { it[Keys.PDF_DEFAULT_LEVEL] = level }
    }
}
