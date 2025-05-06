package com.OxGames.Pluvia.events

import com.OxGames.Pluvia.data.SteamFriend
import com.OxGames.Pluvia.enums.LoginResult
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.ProfileInfoCallback

sealed interface SteamEvent<T> : Event<T> {
    data class Connected(val isAutoLoggingIn: Boolean) : SteamEvent<Unit>
    data class LoggedOut(val username: String?) : SteamEvent<Unit>
    data class LogonEnded(val username: String?, val loginResult: LoginResult, val message: String? = null) : SteamEvent<Unit>
    data class LogonStarted(val username: String?) : SteamEvent<Unit>
    data class PersonaStateReceived(val persona: SteamFriend) : SteamEvent<Unit>
    data class QrAuthEnded(val success: Boolean, val message: String? = null) : SteamEvent<Unit>
    data class QrChallengeReceived(val challengeUrl: String) : SteamEvent<Unit>
    data class PersonaStateChange(val state: EPersonaState) : SteamEvent<Unit>

    // data object AppInfoReceived : SteamEvent<Unit>
    data object Disconnected : SteamEvent<Unit>
    data object ForceCloseApp : SteamEvent<Unit>
    data object RemotelyDisconnected : SteamEvent<Unit>

    // This isn't a SteamEvent, but since its the only one now, it can stay
    data class OnAliasHistory(val names: List<String>) : SteamEvent<Unit>
    data class OnProfileInfo(val info: ProfileInfoCallback) : SteamEvent<Unit>
}
