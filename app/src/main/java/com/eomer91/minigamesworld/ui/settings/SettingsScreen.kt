package com.eomer91.minigamesworld.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import com.eomer91.minigamesworld.util.AppActions
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.BuildConfig
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.components.ScreenTitle
import com.eomer91.minigamesworld.ui.theme.AppColors
import java.util.Locale

/** Uygulamanin destekledigi diller. Yeni dil eklerken res/xml/locales_config.xml'i de guncelle. */
private val SUPPORTED_LANGUAGES = listOf("en", "tr", "de", "es", "fr", "it", "ja", "ru", "da", "hu")

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val sound by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val vibration by viewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val music by viewModel.musicEnabled.collectAsStateWithLifecycle()
    var showLanguageDialog by remember { mutableStateOf(false) }

    val currentTag = AppCompatDelegate.getApplicationLocales().get(0)?.language
    val systemLabel = stringResource(R.string.language_system)
    val currentLabel = currentTag?.let(::languageName) ?: systemLabel

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenTitle(stringResource(R.string.tab_settings), modifier = Modifier.padding(bottom = 6.dp))

        SettingRow(stringResource(R.string.settings_language), onClick = { showLanguageDialog = true }) {
            Text(currentLabel, color = AppColors.Primary, fontWeight = FontWeight.Bold)
        }
        SettingRow(stringResource(R.string.settings_sound), onClick = { viewModel.setSound(!sound) }) {
            Switch(checked = sound, onCheckedChange = viewModel::setSound, colors = switchColors())
        }
        SettingRow(stringResource(R.string.settings_music), onClick = { viewModel.setMusic(!music) }) {
            Switch(checked = music, onCheckedChange = viewModel::setMusic, colors = switchColors())
        }
        SettingRow(stringResource(R.string.settings_vibration), onClick = { viewModel.setVibration(!vibration) }) {
            Switch(checked = vibration, onCheckedChange = viewModel::setVibration, colors = switchColors())
        }
        val context = LocalContext.current
        val activity = LocalActivity.current
        SettingRow(stringResource(R.string.invite_friend), onClick = { AppActions.inviteFriend(context) }) {
            Icon(Icons.Rounded.Share, contentDescription = null, tint = AppColors.Primary)
        }
        SettingRow(stringResource(R.string.rate_app), onClick = { activity?.let(AppActions::rateApp) ?: AppActions.openStorePage(context) }) {
            Text("★★★★★", color = AppColors.Gold, fontWeight = FontWeight.Black)
        }

        Text(
            stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            color = AppColors.TextSecondary,
            modifier = Modifier.padding(top = 12.dp).align(Alignment.CenterHorizontally),
        )
    }

    if (showLanguageDialog) {
        val options: List<String?> = listOf(null) + SUPPORTED_LANGUAGES
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column {
                    options.forEach { tag ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLanguageDialog = false
                                    applyLanguage(tag)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = tag == currentTag, onClick = null)
                            Text(tag?.let(::languageName) ?: systemLabel, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

/** Dil adi kendi dilinde gosterilir: "Türkçe", "English", "Deutsch"... */
private fun languageName(tag: String): String {
    val locale = Locale.forLanguageTag(tag)
    return locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
}

private fun applyLanguage(tag: String?) {
    val locales = if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
    AppCompatDelegate.setApplicationLocales(locales)
}

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = AppColors.OnPrimary,
    checkedTrackColor = AppColors.Primary,
)

@Composable
private fun SettingRow(title: String, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        trailing()
    }
}
