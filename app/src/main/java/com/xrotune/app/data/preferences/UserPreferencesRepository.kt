package com.xrotune.app.data.preferences

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xrotune.app.data.local.XroTuneDocument
import com.xrotune.app.data.local.XroTuneFileStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "xrotune_prefs")

data class UserSettings(
    val preferredContainer: String = "any",          // any, mp4, webm
    val preferredDownloadQuality: String = "high",   // high, medium, low
    val askDownloadQuality: Boolean = true,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = 0                          // 0 = off, 1 = all, 2 = one
)

class UserPreferencesRepository(private val context: Context) {

    private val fileStorage = XroTuneFileStorage(context)

    private val _xroTuneDocument = MutableStateFlow(fileStorage.loadOrCreate())
    val xroTuneDocument: StateFlow<XroTuneDocument> = _xroTuneDocument.asStateFlow()

    private object Keys {
        val PREFERRED_CONTAINER = stringPreferencesKey("preferred_container")
        val PREFERRED_DOWNLOAD_QUALITY = stringPreferencesKey("preferred_download_quality")
        val ASK_DOWNLOAD_QUALITY = booleanPreferencesKey("ask_download_quality")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = intPreferencesKey("repeat_mode")
    }

    val userSettings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val fileSettings = _xroTuneDocument.value.settings
        UserSettings(
            preferredContainer = prefs[Keys.PREFERRED_CONTAINER] ?: fileSettings.preferredContainer,
            preferredDownloadQuality = prefs[Keys.PREFERRED_DOWNLOAD_QUALITY] ?: fileSettings.preferredDownloadQuality,
            askDownloadQuality = prefs[Keys.ASK_DOWNLOAD_QUALITY] ?: fileSettings.askDownloadQuality,
            shuffleEnabled = prefs[Keys.SHUFFLE_ENABLED] ?: fileSettings.shuffleEnabled,
            repeatMode = prefs[Keys.REPEAT_MODE] ?: fileSettings.repeatMode
        )
    }

    private fun updateXroTuneFile(transform: (XroTuneDocument) -> XroTuneDocument) {
        val updated = fileStorage.saveDocument(transform(_xroTuneDocument.value))
        _xroTuneDocument.value = updated
    }

    suspend fun setPreferredContainer(container: String) {
        context.dataStore.edit { it[Keys.PREFERRED_CONTAINER] = container }
        updateXroTuneFile { doc ->
            doc.copy(settings = doc.settings.copy(preferredContainer = container))
        }
    }

    suspend fun setPreferredDownloadQuality(quality: String) {
        context.dataStore.edit { it[Keys.PREFERRED_DOWNLOAD_QUALITY] = quality }
        updateXroTuneFile { doc ->
            doc.copy(settings = doc.settings.copy(preferredDownloadQuality = quality))
        }
    }

    suspend fun setAskDownloadQuality(ask: Boolean) {
        context.dataStore.edit { it[Keys.ASK_DOWNLOAD_QUALITY] = ask }
        updateXroTuneFile { doc ->
            doc.copy(settings = doc.settings.copy(askDownloadQuality = ask))
        }
    }

    suspend fun setShuffleEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHUFFLE_ENABLED] = enabled }
        updateXroTuneFile { doc ->
            doc.copy(settings = doc.settings.copy(shuffleEnabled = enabled))
        }
    }

    suspend fun setRepeatMode(mode: Int) {
        context.dataStore.edit { it[Keys.REPEAT_MODE] = mode }
        updateXroTuneFile { doc ->
            doc.copy(settings = doc.settings.copy(repeatMode = mode))
        }
    }

    fun updateUserProfile(
        displayName: String,
        bio: String,
        favoriteGenre: String,
        avatarColorIndex: Int
    ) {
        updateXroTuneFile { doc ->
            doc.copy(
                profile = doc.profile.copy(
                    displayName = displayName.trim().ifBlank { "XroTune Listener" },
                    bio = bio.trim().ifBlank { "Streaming & local playlists" },
                    favoriteGenre = favoriteGenre.trim().ifBlank { "All Genres" },
                    avatarColorIndex = avatarColorIndex
                )
            )
        }
    }

    fun incrementTracksPlayed() {
        updateXroTuneFile { doc ->
            doc.copy(
                profile = doc.profile.copy(
                    totalTracksPlayed = doc.profile.totalTracksPlayed + 1
                )
            )
        }
    }

    fun getXroTuneFileSizeBytes(): Long = fileStorage.getFileSizeBytes()

    fun getXroTuneFileName(): String = XroTuneFileStorage.FILE_NAME

    fun exportXroTuneToUri(uri: Uri): Result<Unit> {
        return fileStorage.exportToUri(uri, _xroTuneDocument.value)
    }

    suspend fun importXroTuneFromUri(uri: Uri): Result<XroTuneDocument> {
        val result = fileStorage.importFromUri(uri)
        result.onSuccess { doc ->
            _xroTuneDocument.value = doc
            context.dataStore.edit { prefs ->
                prefs[Keys.PREFERRED_CONTAINER] = doc.settings.preferredContainer
                prefs[Keys.PREFERRED_DOWNLOAD_QUALITY] = doc.settings.preferredDownloadQuality
                prefs[Keys.ASK_DOWNLOAD_QUALITY] = doc.settings.askDownloadQuality
                prefs[Keys.SHUFFLE_ENABLED] = doc.settings.shuffleEnabled
                prefs[Keys.REPEAT_MODE] = doc.settings.repeatMode
            }
        }
        return result
    }

    companion object {
        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
