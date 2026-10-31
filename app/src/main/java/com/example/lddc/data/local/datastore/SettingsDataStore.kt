package com.example.lddc.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.lddc.common.models.enums.LyricsFormat
import com.example.lddc.common.models.enums.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val DEFAULT_SAVE_PATH = stringPreferencesKey("default_save_path")
        val LYRICS_FORMAT = stringPreferencesKey("lyrics_format")
        val SEARCH_SOURCE = stringPreferencesKey("search_source")
        val ENABLED_SOURCES = stringPreferencesKey("enabled_sources")
        val AUTO_TRANSLATE = booleanPreferencesKey("auto_translate")
        val TRANSLATE_ENGINE = stringPreferencesKey("translate_engine")
        val CACHE_DURATION = intPreferencesKey("cache_duration_days")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_ROMAJI = booleanPreferencesKey("show_romaji")
        val EMBED_LYRICS = booleanPreferencesKey("embed_lyrics")
        val SAVE_LYRICS_FILE = booleanPreferencesKey("save_lyrics_file")
        val DOWNLOAD_TREE_URI = stringPreferencesKey("download_tree_uri")
        val AUTO_HIDE_NAV_BAR = booleanPreferencesKey("auto_hide_nav_bar")
        val LAST_SCAN_TIME = longPreferencesKey("last_scan_time")
        val LAST_SCAN_SONG_COUNT = intPreferencesKey("last_scan_song_count")
        val INCREMENTAL_SCAN_ENABLED = booleanPreferencesKey("incremental_scan_enabled")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            defaultSavePath = preferences[DEFAULT_SAVE_PATH],
            lyricsFormat = preferences[LYRICS_FORMAT]?.let {
                try { LyricsFormat.valueOf(it) } catch (e: Exception) { LyricsFormat.LINEBYLINELRC }
            } ?: LyricsFormat.LINEBYLINELRC,
            searchSource = preferences[SEARCH_SOURCE]?.let {
                try { Source.valueOf(it) } catch (e: Exception) { Source.MULTI }
            } ?: Source.MULTI,
            enabledSources = preferences[ENABLED_SOURCES]?.let {
                try { it.split(",").map { sourceName -> Source.valueOf(sourceName) } }
                catch (e: Exception) { listOf(Source.MULTI, Source.NE, Source.QM, Source.KG) }
            } ?: listOf(Source.MULTI, Source.NE, Source.QM, Source.KG),
            autoTranslate = preferences[AUTO_TRANSLATE] ?: false,
            translateEngine = preferences[TRANSLATE_ENGINE] ?: "bing",
            cacheDurationDays = preferences[CACHE_DURATION] ?: 7,
            themeMode = preferences[THEME_MODE] ?: "system",
            showRomaji = preferences[SHOW_ROMAJI] ?: false,
            embedLyrics = preferences[EMBED_LYRICS] ?: true,
            saveLyricsFile = preferences[SAVE_LYRICS_FILE] ?: false,
            downloadTreeUri = preferences[DOWNLOAD_TREE_URI],
            autoHideNavBar = preferences[AUTO_HIDE_NAV_BAR] ?: true,
            lastScanTime = preferences[LAST_SCAN_TIME] ?: 0L,
            lastScanSongCount = preferences[LAST_SCAN_SONG_COUNT] ?: 0,
            incrementalScanEnabled = preferences[INCREMENTAL_SCAN_ENABLED] ?: true
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            settings.defaultSavePath?.let { preferences[DEFAULT_SAVE_PATH] = it }
            preferences[LYRICS_FORMAT] = settings.lyricsFormat.name
            preferences[SEARCH_SOURCE] = settings.searchSource.name
            preferences[ENABLED_SOURCES] = settings.enabledSources.joinToString(",")
            preferences[AUTO_TRANSLATE] = settings.autoTranslate
            preferences[TRANSLATE_ENGINE] = settings.translateEngine
            preferences[CACHE_DURATION] = settings.cacheDurationDays
            preferences[THEME_MODE] = settings.themeMode
            preferences[SHOW_ROMAJI] = settings.showRomaji
            preferences[EMBED_LYRICS] = settings.embedLyrics
            preferences[SAVE_LYRICS_FILE] = settings.saveLyricsFile
            preferences[AUTO_HIDE_NAV_BAR] = settings.autoHideNavBar
            if (settings.downloadTreeUri != null) {
                preferences[DOWNLOAD_TREE_URI] = settings.downloadTreeUri
            } else {
                preferences.remove(DOWNLOAD_TREE_URI)
            }
        }
    }

    suspend fun updateDefaultSavePath(path: String) { context.dataStore.edit { it[DEFAULT_SAVE_PATH] = path } }
    suspend fun updateLyricsFormat(format: LyricsFormat) { context.dataStore.edit { it[LYRICS_FORMAT] = format.name } }
    suspend fun updateSearchSource(source: Source) { context.dataStore.edit { it[SEARCH_SOURCE] = source.name } }
    suspend fun clearSettings() { context.dataStore.edit { it.clear() } }
}

data class AppSettings(
    val defaultSavePath: String? = null,
    val lyricsFormat: LyricsFormat = LyricsFormat.LINEBYLINELRC,
    val searchSource: Source = Source.MULTI,
    val enabledSources: List<Source> = listOf(Source.NE, Source.QM, Source.KG),
    val autoTranslate: Boolean = false,
    val translateEngine: String = "bing",
    val cacheDurationDays: Int = 7,
    val themeMode: String = "system",
    val showRomaji: Boolean = false,
    val embedLyrics: Boolean = true,
    val saveLyricsFile: Boolean = false,
    val downloadTreeUri: String? = null,
    val autoHideNavBar: Boolean = true,
    val lastScanTime: Long = 0L,
    val lastScanSongCount: Int = 0,
    val incrementalScanEnabled: Boolean = true
)

data class ScanSettings(
    val lastScanTime: Long = 0L,
    val lastScanSongCount: Int = 0,
    val incrementalScanEnabled: Boolean = true
)