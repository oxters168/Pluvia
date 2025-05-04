package com.OxGames.Pluvia

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.OxGames.Pluvia.enums.AppFilter
import com.OxGames.Pluvia.enums.AppTheme
import com.OxGames.Pluvia.enums.HomeDestination
import com.OxGames.Pluvia.enums.Orientation
import com.OxGames.Pluvia.service.SteamService
import com.OxGames.Pluvia.utils.application.Crypto
import com.materialkolor.PaletteStyle
import `in`.dragonbra.javasteam.enums.EPersonaState
import java.util.EnumSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * A universal Preference Manager that can be used anywhere within Pluvia.
 * Note: King of ugly though.
 */
object PrefManager {

    private val Context.datastore by preferencesDataStore(
        name = "PluviaPreferences",
        corruptionHandler = ReplaceFileCorruptionHandler {
            Timber.e("Preferences (somehow got) corrupted, resetting.")
            emptyPreferences()
        },
    )

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var dataStore: DataStore<Preferences>

    fun init(context: Context) {
        dataStore = context.datastore

        // Note: Should remove after a few release versions. we've moved to encrypted values.
        val oldPassword = stringPreferencesKey("password")
        removePref(oldPassword)

        val oldAccessToken = stringPreferencesKey("access_token")
        val oldRefreshToken = stringPreferencesKey("refresh_token")
        getPref(oldAccessToken, "").let {
            if (it.isNotEmpty()) {
                Timber.i("Converting old access token to encrypted")
                accessToken = it
                removePref(oldAccessToken)
            }
        }
        getPref(oldRefreshToken, "").let {
            if (it.isNotEmpty()) {
                Timber.i("Converting old refresh token to encrypted")
                refreshToken = it
                removePref(oldRefreshToken)
            }
        }
    }

    fun clearPreferences() {
        scope.launch {
            dataStore.edit { it.clear() }
        }
    }

    fun getInt(key: String, defaultValue: Int): Int = getPref(intPreferencesKey(key), defaultValue)

    fun putInt(key: String, defaultValue: Int) = setPref(intPreferencesKey(key), defaultValue)

    fun getBoolean(key: String, defaultValue: Boolean): Boolean = getPref(booleanPreferencesKey(key), defaultValue)

    fun putBoolean(key: String, defaultValue: Boolean) = setPref(booleanPreferencesKey(key), defaultValue)

    fun getString(key: String, defaultValue: String?): String? = runBlocking {
        dataStore.data.first()[stringPreferencesKey(key)] ?: defaultValue
    }

    fun putString(key: String, defaultValue: String) = setPref(stringPreferencesKey(key), defaultValue)

    @Suppress("SameParameterValue")
    private fun <T> getPref(key: Preferences.Key<T>, defaultValue: T): T = runBlocking {
        val value = dataStore.data.first()[key] ?: defaultValue
        Timber.d("Getting preference: $key with $value")
        value
    }

    @Suppress("SameParameterValue")
    private fun <T> setPref(key: Preferences.Key<T>, value: T) {
        Timber.d("Setting preference: $key with $value")
        scope.launch {
            dataStore.edit { pref -> pref[key] = value }
        }
    }

    private fun <T> removePref(key: Preferences.Key<T>) {
        Timber.d("Removing preference: $key")
        scope.launch {
            dataStore.edit { pref -> pref.remove(key) }
        }
    }

    // region PICS
    private val LAST_PICS_CHANGE_NUMBER = intPreferencesKey("last_pics_change_number")
    var lastPICSChangeNumber: Int
        get() = getPref(LAST_PICS_CHANGE_NUMBER, 0)
        set(value) {
            setPref(LAST_PICS_CHANGE_NUMBER, value)
        }
    // endregion

    // region Login Info
    private val APP_INSTALL_PATH = stringPreferencesKey("app_install_path")
    var appInstallPath: String
        get() = getPref(APP_INSTALL_PATH, SteamService.defaultAppInstallPath)
        set(value) {
            setPref(APP_INSTALL_PATH, value)
        }

    private val APP_STAGING_PATH = stringPreferencesKey("app_staging_path")
    var appStagingPath: String
        get() = getPref(APP_STAGING_PATH, SteamService.defaultAppStagingPath)
        set(value) {
            setPref(APP_STAGING_PATH, value)
        }

    // Special: Because null value.
    private val CLIENT_ID = longPreferencesKey("client_id")
    var clientId: Long?
        get() = runBlocking { dataStore.data.first()[CLIENT_ID] }
        set(value) {
            scope.launch {
                dataStore.edit { pref -> pref[CLIENT_ID] = value!! }
            }
        }

    private val CELL_ID = intPreferencesKey("cell_id")
    var cellId: Int
        get() = getPref(CELL_ID, 0)
        set(value) {
            setPref(CELL_ID, value)
        }

    private val USER_NAME = stringPreferencesKey("user_name")
    var username: String
        get() = getPref(USER_NAME, "")
        set(value) {
            setPref(USER_NAME, value)
        }

    private val ACCESS_TOKEN_ENC = byteArrayPreferencesKey("access_token_enc")
    var accessToken: String
        get() {
            val encryptedBytes = getPref(ACCESS_TOKEN_ENC, ByteArray(0))
            return if (encryptedBytes.isEmpty()) {
                ""
            } else {
                val bytes = Crypto.decrypt(encryptedBytes)
                String(bytes)
            }
        }
        set(value) {
            val bytes = Crypto.encrypt(value.toByteArray())
            setPref(ACCESS_TOKEN_ENC, bytes)
        }

    private val REFRESH_TOKEN_ENC = byteArrayPreferencesKey("refresh_token_enc")
    var refreshToken: String
        get() {
            val encryptedBytes = getPref(REFRESH_TOKEN_ENC, ByteArray(0))
            return if (encryptedBytes.isEmpty()) {
                ""
            } else {
                val bytes = Crypto.decrypt(encryptedBytes)
                String(bytes)
            }
        }
        set(value) {
            val bytes = Crypto.encrypt(value.toByteArray())
            setPref(REFRESH_TOKEN_ENC, bytes)
        }

    private val PERSONA_STATE = intPreferencesKey("persona_state")
    var personaState: EPersonaState
        get() {
            val value = getPref(PERSONA_STATE, EPersonaState.Online.code())
            return EPersonaState.from(value)
        }
        set(value) {
            setPref(PERSONA_STATE, value.code())
        }
    // endregion

    // region App
    private val RECENTLY_CRASHED = booleanPreferencesKey("recently_crashed")
    var recentlyCrashed: Boolean
        get() = getPref(RECENTLY_CRASHED, false)
        set(value) {
            setPref(RECENTLY_CRASHED, value)
        }

    private val ALLOWED_ORIENTATION = intPreferencesKey("allowed_orientation")
    var allowedOrientation: EnumSet<Orientation>
        get() {
            val defaultValue = Orientation.toInt(
                EnumSet.of(Orientation.LANDSCAPE, Orientation.REVERSE_LANDSCAPE),
            )
            val value = getPref(ALLOWED_ORIENTATION, defaultValue)
            return Orientation.fromInt(value)
        }
        set(value) {
            setPref(ALLOWED_ORIENTATION, Orientation.toInt(value))
        }

    private val TIPPED = booleanPreferencesKey("tipped")
    var tipped: Boolean
        get() {
            val value = getPref(TIPPED, false)
            return value
        }
        set(value) {
            setPref(TIPPED, value)
        }

    private val APP_THEME = intPreferencesKey("app_theme")
    var appTheme: AppTheme
        get() {
            val value = getPref(APP_THEME, AppTheme.AUTO.ordinal)
            return AppTheme.entries.getOrNull(value) ?: AppTheme.AUTO
        }
        set(value) {
            setPref(APP_THEME, value.ordinal)
        }

    private val APP_THEME_PALETTE = intPreferencesKey("app_theme_palette")
    var appThemePalette: PaletteStyle
        get() {
            val value = getPref(APP_THEME_PALETTE, PaletteStyle.TonalSpot.ordinal)
            return PaletteStyle.entries.getOrNull(value) ?: PaletteStyle.TonalSpot
        }
        set(value) {
            setPref(APP_THEME_PALETTE, value.ordinal)
        }

    private val START_SCREEN = intPreferencesKey("start screen")
    var startScreen: HomeDestination
        get() {
            val value = getPref(START_SCREEN, HomeDestination.Library.ordinal)
            return HomeDestination.entries.getOrNull(value) ?: HomeDestination.Library
        }
        set(value) {
            setPref(START_SCREEN, value.ordinal)
        }

    // Whether to open links internally with a webview or open externally with a user's browser.
    private val OPEN_WEB_LINKS_EXTERNALLY = booleanPreferencesKey("open_web_links_externally")
    var openWebLinksExternally: Boolean
        get() = getPref(OPEN_WEB_LINKS_EXTERNALLY, true)
        set(value) {
            setPref(OPEN_WEB_LINKS_EXTERNALLY, value)
        }

    private val BROADCAST_PLAYING_GAME = booleanPreferencesKey("broadcast_playing_game")
    var broadcastPlayingGame: Boolean
        get() = getPref(BROADCAST_PLAYING_GAME, true)
        set(value) {
            setPref(BROADCAST_PLAYING_GAME, value)
        }
    // endregion

    // region Screens
    private val LIBRARY_FILTER = intPreferencesKey("library_filter")
    var libraryFilter: EnumSet<AppFilter>
        get() {
            val value = getPref(LIBRARY_FILTER, AppFilter.toFlags(EnumSet.of(AppFilter.GAME)))
            return AppFilter.fromFlags(value)
        }
        set(value) {
            setPref(LIBRARY_FILTER, AppFilter.toFlags(value))
        }

    private val FRIENDS_LIST_HEADER = stringPreferencesKey("friends_list_header")
    var friendsListHeader: Set<String>
        get() {
            val value = getPref(FRIENDS_LIST_HEADER, "[]")
            return Json.decodeFromString<Set<String>>(value)
        }
        set(value) {
            setPref(FRIENDS_LIST_HEADER, Json.encodeToString(value))
        }

    // NOTE: This should be removed once chat is considered stable.
    private val ACK_CHAT_PREVIEW = booleanPreferencesKey("ack_chat_preview")
    var ackChatPreview: Boolean
        get() = getPref(ACK_CHAT_PREVIEW, false)
        set(value) {
            setPref(ACK_CHAT_PREVIEW, value)
        }
    // endregion
}
