package com.OxGames.Pluvia

import com.OxGames.Pluvia.data.SteamFriend
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import junit.framework.TestCase.assertEquals
import org.junit.Test

class SteamFriendTest {

    @Test
    fun `test isPlayingGameName`() {
        val friend = SteamFriend(
            id = 123456789,
            relation = EFriendRelationship.Friend,
            state = EPersonaState.Online,
            gameAppID = 12345,
        )

        assertEquals(friend.isPlayingGameName, "Playing game id: ${friend.gameAppID}")

        val blockedFriend = friend.copy(relation = EFriendRelationship.Blocked)
        assertEquals(blockedFriend.isPlayingGameName, EFriendRelationship.Blocked.name)
    }
}
