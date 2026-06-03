package com.hire.smartcompress.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hire.smartcompress.domain.model.ThemeMode
import com.hire.smartcompress.presentation.components.AppTopBar
import com.hire.smartcompress.presentation.components.SectionHeader

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { AppTopBar("Settings", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Compression settings
            SectionHeader("Compression Defaults")
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Default Image Quality", fontWeight = FontWeight.Medium)
                        Text("${state.defaultImageQuality}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = state.defaultImageQuality.toFloat(),
                        onValueChange = { viewModel.setDefaultImageQuality(it.toInt()) },
                        valueRange = 1f..100f
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // File handling
            SectionHeader("File Handling")
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column {
                    SettingsSwitchRow(
                        title = "Auto Delete Original",
                        subtitle = "Delete source files after compression",
                        icon = Icons.Default.Delete,
                        checked = state.autoDeleteOriginal,
                        onCheckedChange = viewModel::setAutoDeleteOriginal
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Appearance
            SectionHeader("Appearance")
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Theme", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val icon = when (mode) {
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                            }
                            FilterChip(
                                selected = state.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // About
            SectionHeader("About")
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column {
                    SettingsInfoRow("App Name", "SmartCompress — File Resizer Pro", Icons.Default.Apps)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsInfoRow("Version", "1.0.0", Icons.Default.Info)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsInfoRow("Privacy", "All processing is 100% offline", Icons.Default.Security)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsInfoRow("Data", "No data leaves your device", Icons.Default.PrivacyTip)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}

@Composable
private fun SettingsInfoRow(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
    )
}
