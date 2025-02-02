package com.OxGames.Pluvia.service

import androidx.room.withTransaction
import com.OxGames.Pluvia.data.FriendMessage
import com.OxGames.Pluvia.data.OwnedGames
import `in`.dragonbra.javasteam.enums.EAccountType
import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.enums.EUniverse
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient
import `in`.dragonbra.javasteam.rpc.service.Chat
import `in`.dragonbra.javasteam.rpc.service.FriendMessages
import `in`.dragonbra.javasteam.rpc.service.FriendMessagesClient
import `in`.dragonbra.javasteam.rpc.service.Player
import `in`.dragonbra.javasteam.rpc.service.PlayerClient
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.SteamUnifiedMessages
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.callback.ServiceMethodNotification
import `in`.dragonbra.javasteam.types.SteamID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

// References:
// https://github.com/marwaniaaj/RichLinksJetpackCompose/tree/main
// https://blog.stackademic.com/rick-link-representation-in-jetpack-compose-d33956e8719e
// https://github.com/lukasroberts/AndroidLinkView
// https://github.com/android/compose-samples/tree/main/Jetchat
// https://github.com/LossyDragon/Vapulla

// TODO
//  Chat slash commands.
//  EmoticonListCallback ?
//  Favorite / UnFavorite friends
//  FriendMsgEchoCallback ?
//  Implement Reactions
//  Message echos (aka bypass notifications/indications if using another client)
//  Notifications of a chat message
//  Observing that a friend is typing...
//  OfflineMessageNotificationCallback ?
//  Per friend notification settings.
//  Recent message sessions (automatically list recent chats on top of the list)
//  Unread message indications
//  View blocked (but still) friends.

typealias AckMessageNotification = SteammessagesFriendmessagesSteamclient.CFriendMessages_AckMessage_Notification
typealias AckMessageNotificationBuilder = SteammessagesFriendmessagesSteamclient.CFriendMessages_AckMessage_Notification.Builder
typealias FriendNicknameChangedBuilder = SteammessagesPlayerSteamclient.CPlayer_FriendNicknameChanged_Notification.Builder
typealias FriendPersonaStatesRequest = SteammessagesChatSteamclient.CChat_RequestFriendPersonaStates_Request
typealias GetActiveMessageSessionsRequest = SteammessagesFriendmessagesSteamclient.CFriendsMessages_GetActiveMessageSessions_Request
typealias GetOwnedGamesRequest = SteammessagesPlayerSteamclient.CPlayer_GetOwnedGames_Request
typealias GetRecentMessagesRequest = SteammessagesFriendmessagesSteamclient.CFriendMessages_GetRecentMessages_Request
typealias IncomingMessageNotification = SteammessagesFriendmessagesSteamclient.CFriendMessages_IncomingMessage_Notification.Builder
typealias SendMessageRequest = SteammessagesFriendmessagesSteamclient.CFriendMessages_SendMessage_Request
typealias UpdateMessageReactionRequest = SteammessagesFriendmessagesSteamclient.CFriendMessages_UpdateMessageReaction_Request

class SteamUnifiedFriends(private val service: SteamService) : AutoCloseable {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val typingTimeouts = mutableMapOf<Long, Job>()

    private var unifiedMessages: SteamUnifiedMessages? = service.steamClient!!.getHandler<SteamUnifiedMessages>()
    private var chat: Chat? = unifiedMessages!!.createService(Chat::class.java)
    private var friendMessages: FriendMessages? = unifiedMessages!!.createService(FriendMessages::class.java)
    private var player: Player? = unifiedMessages!!.createService(Player::class.java)

    init {
        with(service.callbackManager!!) {
            with(service.callbackSubscriptions) {
                add(subscribeServiceNotification<FriendMessagesClient, IncomingMessageNotification>(::onIncomingMessage))
                add(subscribeServiceNotification<FriendMessages, AckMessageNotificationBuilder>(::onAckMessage))
                add(subscribeServiceNotification<PlayerClient, FriendNicknameChangedBuilder>(::onNickNameChanged))
            }
        }
    }

    override fun close() {
        unifiedMessages = null
        chat = null
        player = null
        friendMessages = null
        typingTimeouts.forEach { (_, job) -> job.cancel() }
        typingTimeouts.clear()
    }

    /**
     * Request a fresh state of Friend's PersonaStates
     */
    fun refreshPersonaStates() {
        val request = FriendPersonaStatesRequest.newBuilder().build()
        // Does not return anything.
        chat?.requestFriendPersonaStates(request)

        // Clear any stale typing values.
        scope.launch {
            service.db.withTransaction {
                service.friendDao.clearAllTypingStatus()
            }
        }
    }

    /**
     * Gets the last 50 messages from the specified friend. Steam may not provide all 50.
     */
    suspend fun getRecentMessages(friendID: Long) {
        Timber.i("Getting Recent messages for: $friendID")

        val userSteamID = SteamService.userSteamId

        if (userSteamID == null) {
            Timber.w("Unable to get recent messages, userSteamID is null")
            return
        }

        val request = GetRecentMessagesRequest.newBuilder().apply {
            steamid1 = userSteamID.convertToUInt64() // You
            steamid2 = friendID // Friend
            // The rest here and below is what steam has looking at NHA2
            count = 50
            rtime32StartTime = 0
            bbcodeFormat = true
            startOrdinal = 0
            timeLast = Int.MAX_VALUE
            ordinalLast = 0
        }.build()

        val response = friendMessages?.getRecentMessages(request)?.await()

        if (response == null || response.result != EResult.OK) {
            Timber.w("Failed to get message history for friend: $friendID, result: ${response?.result}")
            return
        }

        val regex = "\\[U:\\d+:(\\d+)]".toRegex()
        val userSteamId3 = regex.find(userSteamID.render())!!.groupValues[1].toInt()
        val messages = response.body.messagesList.map { message ->
            FriendMessage(
                steamIDFriend = friendID,
                fromLocal = userSteamId3 == message.accountid,
                message = message.message,
                timestamp = message.timestamp,
            )
        }

        service.db.withTransaction {
            service.messagesDao.insertMessagesIfNotExist(messages)
        }

        Timber.i("More available: ${response.body.moreAvailable}")
    }

    /**
     * Sends a 'is typing' message to the specified friend.
     */
    suspend fun setIsTyping(friendID: Long) {
        Timber.i("Sending 'is typing' to $friendID")
        val request = SendMessageRequest.newBuilder().apply {
            steamid = friendID
            chatEntryType = EChatEntryType.Typing.code()
        }.build()

        val response = friendMessages?.sendMessage(request)?.await()

        if (response == null || response.result != EResult.OK) {
            Timber.w("Failed to send typing message to friend: $friendID. Result: ${response?.result}")
            return
        }

        // TODO: This, I believe returns a result with supplemental data to append to the database.
        // response.body.serverTimestamp
    }

    /**
     * Sends a chat message to the specified friend.
     */
    suspend fun sendMessage(friendID: Long, chatMessage: String) {
        Timber.i("Sending chat message to $friendID")
        val trimmedMessage = chatMessage.trim().ifEmpty {
            Timber.w("Trying to send an empty message.")
            return
        }

        val request = SendMessageRequest.newBuilder().apply {
            chatEntryType = EChatEntryType.ChatMsg.code()
            message = trimmedMessage
            steamid = friendID
            containsBbcode = true
            echoToSender = false
            lowPriority = false
        }.build()

        val response = friendMessages?.sendMessage(request)?.await()

        if (response == null || response.result != EResult.OK) {
            Timber.w("Failed to send chat message to friend: $friendID. Result: ${response?.result}")
            return
        }

        service.db.withTransaction {
            service.messagesDao.insertMessageIfNotExists(
                FriendMessage(
                    steamIDFriend = friendID,
                    fromLocal = true,
                    message = response.body.modifiedMessage.ifEmpty { trimmedMessage },
                    timestamp = response.body.serverTimestamp,
                ),
            )
        }

        // Once chat notifications are implemented, we should clear it here as well.
    }

    /**
     * Acknowledge the message, this will mark other clients that we have read the message.
     */
    fun ackMessage(friendID: Long) {
        Timber.d("Ack-ing message for friend: $friendID")
        val request = AckMessageNotification.newBuilder().apply {
            steamidPartner = friendID
            timestamp = System.currentTimeMillis().div(1000).toInt() // TODO verify arg.
        }.build()

        // This does not return anything.
        friendMessages?.ackMessage(request) ?: Timber.w("Unable to ack message")

        scope.launch {
            service.db.withTransaction {
                service.friendDao.findFriend(friendID)?.let { friend ->
                    service.friendDao.update(friend.copy(unreadMessageCount = 0))
                }
            }
        }
    }

    /**
     * TODO
     */
    suspend fun getActiveMessageSessions() {
        Timber.i("Get Active message sessions")

        val request = GetActiveMessageSessionsRequest.newBuilder().apply {
            lastmessageSince = 0
            onlySessionsWithMessages = true
        }.build()

        val response = friendMessages?.getActiveMessageSessions(request)?.await()

        if (response == null || response.result != EResult.OK) {
            Timber.w("Failed to get active message sessions. Result: ${response?.result}")
            return
        }

        // response.body.timestamp

        response.body.messageSessionsList.forEach { session ->
            // session.accountidFriend
            // session.lastMessage
            // session.lastView
            // session.unreadMessageCount
        }
    }

    /**
     * TODO
     */
    // suspend fun getPerFriendPreferences()

    /**
     * TODO
     */
    suspend fun updateMessageReaction(
        friendID: Long,
        serverTimestamp: Int,
        reactionType: SteammessagesFriendmessagesSteamclient.EMessageReactionType,
        reaction: String,
        isAdd: Boolean,
    ) {
        Timber.d("Reaction: $friendID, timestamp: $serverTimestamp, type: $reactionType, reaction: $reaction, isAdd: $isAdd")

        val request = UpdateMessageReactionRequest.newBuilder().apply {
            this.steamid = friendID
            this.serverTimestamp = serverTimestamp
            this.ordinal = 0
            this.reactionType = reactionType
            this.reaction = reaction
            this.isAdd = isAdd
        }.build()

        val response = friendMessages?.updateMessageReaction(request)?.await()

        if (response == null || response.result != EResult.OK) {
            Timber.w("Failed to get message reaction. Result: ${response?.result}")
            return
        }

        response.body.reactorsList.forEach { reactor ->
            // Last part of steamID3
        }
    }

    /**
     * Gets a list of games that the user owns. If the library is private, it will be empty.
     */
    suspend fun getOwnedGames(steamID: Long): List<OwnedGames> {
        val request = GetOwnedGamesRequest.newBuilder().apply {
            steamid = steamID
            includePlayedFreeGames = true
            includeFreeSub = true
            includeAppinfo = true
            includeExtendedAppinfo = true
        }.build()

        val result = player?.getOwnedGames(request)?.await()

        if (result == null || result.result != EResult.OK) {
            Timber.w("Unable to get owned games! Result: ${result?.result}")
            return emptyList()
        }

        val list = result.body.gamesList.map { game ->
            OwnedGames(
                appId = game.appid,
                name = game.name,
                playtimeTwoWeeks = game.playtime2Weeks,
                playtimeForever = game.playtimeForever,
                imgIconUrl = game.imgIconUrl,
                sortAs = game.sortAs,
            )
        }

        if (list.size != result.body.gamesCount) {
            Timber.w("List was not the same as given")
        }

        return list
    }

    /**
     * Another steam client (logged into the same account) has opened up chat to acknowledge the message(s).
     */
    private fun onAckMessage(notification: ServiceMethodNotification<AckMessageNotificationBuilder>) {
        scope.launch {
            val friendID = notification.body.steamidPartner
            Timber.i("Ack-ing Message for $friendID")
            with(service) {
                db.withTransaction {
                    friendDao.findFriend(friendID)?.let { friend ->
                        friendDao.update(friend.copy(unreadMessageCount = 0))
                    }
                }
            }
        }
    }

    /**
     * Someone has changed their nickname.
     */
    private fun onNickNameChanged(notification: ServiceMethodNotification<FriendNicknameChangedBuilder>) {
        scope.launch {
            Timber.i("Nickname Changed for ${notification.body.accountid} -> ${notification.body.nickname}")

            // Convert from a steamID3 number
            val friendID = SteamID(notification.body.accountid.toLong(), EUniverse.Public, EAccountType.Individual).convertToUInt64()

            with(service) {
                db.withTransaction {
                    friendDao.findFriend(friendID)?.let { friend ->
                        friendDao.update(friend.copy(nickname = notification.body.nickname))
                    }
                }
            }
        }
    }

    // The coolest 'is typing' timeout you've ever seen.
    private fun setTypingTimeout(steamIDFriend: Long) {
        typingTimeouts[steamIDFriend] = scope.launch {
            try {
                delay(12.seconds)
            } finally {
                Timber.d("Cancelling $steamIDFriend typing timeout")
                with(service) {
                    db.withTransaction {
                        friendDao.findFriend(steamIDFriend)?.let { friend ->
                            friendDao.update(friend.copy(isTyping = false))
                        }
                    }
                }
                typingTimeouts.remove(steamIDFriend)
            }
        }
    }

    /**
     * We're receiving information that someone is either typing a message or sent a message.
     */
    private fun onIncomingMessage(notification: ServiceMethodNotification<IncomingMessageNotification>) {
        val steamIDFriend = notification.body.steamidFriend
        Timber.i("Incoming Message form $steamIDFriend")

        // Cancel any jobs if active for the friend.
        typingTimeouts[steamIDFriend]?.cancel()

        when (notification.body.chatEntryType) {
            EChatEntryType.Typing.code() -> scope.launch {
                setTypingTimeout(steamIDFriend)

                with(service) {
                    db.withTransaction {
                        friendDao.findFriend(steamIDFriend)?.let { friend ->
                            friendDao.update(friend.copy(isTyping = true))
                        } ?: run {
                            Timber.w("Unable to find friend $steamIDFriend")
                            return@withTransaction
                        }
                    }
                }
            }

            EChatEntryType.ChatMsg.code() -> scope.launch {
                with(service) {
                    db.withTransaction {
                        friendDao.findFriend(steamIDFriend)?.let { friend ->
                            messagesDao.insertMessage(
                                FriendMessage(
                                    steamIDFriend = steamIDFriend,
                                    fromLocal = false,
                                    message = notification.body.message,
                                    timestamp = notification.body.rtime32ServerTimestamp,
                                ),
                            )

                            // We cannot have two `updates` in a transaction.
                            if (SteamService.currentChat != steamIDFriend) {
                                friendDao.update(friend.copy(unreadMessageCount = friend.unreadMessageCount + 1, isTyping = false))
                            } else {
                                friendDao.update(friend.copy(isTyping = false))
                            }
                        } ?: Timber.w("Unable to find friend $steamIDFriend")
                    }
                }
            }

            else -> Timber.w("Unknown incoming message: ${EChatEntryType.from(notification.body.chatEntryType)}")
        }
    }
}
