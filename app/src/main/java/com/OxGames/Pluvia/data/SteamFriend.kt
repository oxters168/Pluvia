package com.OxGames.Pluvia.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Web
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.OxGames.Pluvia.ui.component.icons.VR
import com.OxGames.Pluvia.ui.theme.friendAwayOrSnooze
import com.OxGames.Pluvia.ui.theme.friendInGame
import com.OxGames.Pluvia.ui.theme.friendInGameAwayOrSnooze
import com.OxGames.Pluvia.ui.theme.friendOffline
import com.OxGames.Pluvia.ui.theme.friendOnline
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag

@Entity("steam_friend")
data class SteamFriend(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "relation") val relation: Int = 0,
    @ColumnInfo("status_flags") val statusFlags: Int = 0,
    @ColumnInfo("state") val state: Int = 0,
    @ColumnInfo("state_flags") val stateFlags: Int = 0,
    @ColumnInfo("game_app_id") val gameAppID: Int = 0,
    @ColumnInfo("game_id") val gameID: Long = 0L,
    @ColumnInfo("game_name") val gameName: String = "",
    @ColumnInfo("game_server_ip") val gameServerIP: Int = 0,
    @ColumnInfo("game_server_port") val gameServerPort: Int = 0,
    @ColumnInfo("query_port") val queryPort: Int = 0,
    @ColumnInfo("source_steam_id") val sourceSteamID: Long = 0L,
    @ColumnInfo("game_data_blob") val gameDataBlob: String = "",
    @ColumnInfo("name") val name: String = "",
    @ColumnInfo("nickname") val nickname: String = "",
    @ColumnInfo("avatar_hash") val avatarHash: String = "",
    @ColumnInfo("last_log_off") val lastLogOff: Long = 0L,
    @ColumnInfo("last_log_on") val lastLogOn: Long = 0L,
    @ColumnInfo("clan_rank") val clanRank: Int = 0,
    @ColumnInfo("clan_tag") val clanTag: String = "",
    @ColumnInfo("online_session_instances") val onlineSessionInstances: Int = 0,
) {
    val isOnline: Boolean
        get() = (state in 1..6)

    val isOffline: Boolean
        get() = EPersonaState.from(state) == EPersonaState.Offline

    val nameOrNickname: String
        get() = nickname.ifEmpty { name.ifEmpty { "<unknown>" } }

    val isPlayingGame: Boolean
        get() = if (isOnline) gameAppID > 0 || gameName.isEmpty().not() else false

    val isAwayOrSnooze: Boolean
        get() = EPersonaState.from(state).let {
            it == EPersonaState.Away || it == EPersonaState.Snooze || it == EPersonaState.Busy
        }

    val isInGameAwayOrSnooze: Boolean
        get() = isPlayingGame && isAwayOrSnooze

    val isRequestRecipient: Boolean
        get() = EFriendRelationship.from(relation) == EFriendRelationship.RequestRecipient

    val isBlocked: Boolean
        get() = EFriendRelationship.from(relation) == EFriendRelationship.Blocked ||
                EFriendRelationship.from(relation) == EFriendRelationship.Ignored ||
                EFriendRelationship.from(relation) == EFriendRelationship.IgnoredFriend

    val isFriend: Boolean
        get() = EFriendRelationship.from(relation) == EFriendRelationship.Friend

    val statusColor: Color
        get() = when {
            isOffline -> friendOffline
            isInGameAwayOrSnooze -> friendInGameAwayOrSnooze
            isAwayOrSnooze -> friendAwayOrSnooze
            isPlayingGame -> friendInGame
            isOnline -> friendOnline
            else -> friendOffline
        }

    val statusIcon: ImageVector?
        get() {
            val flags = EPersonaStateFlag.from(stateFlags)
            return when {
                isRequestRecipient -> Icons.Default.PersonAddAlt1
                isAwayOrSnooze -> Icons.Default.Bedtime
                flags.contains(EPersonaStateFlag.ClientTypeVR) -> Icons.Default.VR
                flags.contains(EPersonaStateFlag.ClientTypeTenfoot) -> Icons.Default.SportsEsports
                flags.contains(EPersonaStateFlag.ClientTypeMobile) -> Icons.Default.Smartphone
                flags.contains(EPersonaStateFlag.ClientTypeWeb) -> Icons.Default.Web
                else -> null
            }
        }
}