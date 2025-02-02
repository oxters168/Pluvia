package com.OxGames.Pluvia.ui.screen.friends

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Badge
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.OxGames.Pluvia.data.SteamFriend
import com.OxGames.Pluvia.ui.component.text.StatusIconText
import com.OxGames.Pluvia.ui.component.text.TypingIndicator
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.OxGames.Pluvia.ui.util.ListItemImage
import com.OxGames.Pluvia.utils.getAvatarURL
import com.materialkolor.ktx.isLight
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag

// https://m3.material.io/components/lists/specs#d156b3f2-6763-4fde-ba6f-0f088ce5a4e4

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FriendItem(
    modifier: Modifier = Modifier,
    friend: SteamFriend,
    onClick: (SteamFriend) -> Unit,
    onLongClick: (SteamFriend) -> Unit,
) {
    // Can't use CompositionLocal for colors. Instead we can use ListItemDefault.colors()

    val isLight = MaterialTheme.colorScheme.background.isLight()

    ListItem(
        modifier = modifier.combinedClickable(
            onClick = { onClick(friend) },
            onLongClick = { onLongClick(friend) },
        ),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = if (isLight) MaterialTheme.colorScheme.onSurface else friend.statusColor,
            supportingColor = if (isLight) MaterialTheme.colorScheme.onSurfaceVariant else friend.statusColor,
        ),
        headlineContent = {
            StatusIconText(friend = friend)
        },
        supportingContent = {
            // TODO get game names
            if (friend.isTyping) {
                TypingIndicator()
            } else {
                Text(text = friend.isPlayingGameName)
            }
        },
        leadingContent = {
            ListItemImage(
                modifier = Modifier.clickable { onLongClick(friend) },
                image = { friend.avatarHash.getAvatarURL() },
            )
        },
        trailingContent = {
            friend.unreadMessageCount.takeIf { it > 0 }?.let { count ->
                Badge(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    content = { Text(text = if (count <= 99) "$count" else "99+") },
                )
            }
        },
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_FriendItem() {
    val friendData = mapOf(
        "Friend Online" to EPersonaState.Online,
        "Friend Typing" to EPersonaState.Online,
        "Friend Away" to EPersonaState.Away,
        "Friend Offline" to EPersonaState.Offline,
        "Friend In Game" to EPersonaState.Online,
        "Friend Away In Game" to EPersonaState.Away,
    )

    PluviaTheme {
        Surface {
            Column {
                friendData.onEachIndexed { index, entry ->
                    FriendItem(
                        friend = SteamFriend(
                            id = index.toLong(),
                            gameAppID = if (index < 3) 0 else index,
                            gameName = if (index < 3) "" else "Team Fortress 2",
                            name = entry.key,
                            nickname = if (index < 3) "" else entry.key,
                            relation = EFriendRelationship.Friend,
                            state = entry.value,
                            stateFlags = EPersonaStateFlag.from(256.times(index + 1)),
                            isTyping = index == 1,
                            unreadMessageCount = if (index == 1) 999 else 0,
                        ),
                        onClick = { },
                        onLongClick = { },
                    )
                }
            }
        }
    }
}
