package com.OxGames.Pluvia.service.callback

import com.OxGames.Pluvia.data.Emoticon
import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.base.IPacketMsg
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientEmoticonList
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackMsg

class EmoticonListCallback(packetMsg: IPacketMsg) : CallbackMsg() {

    val emoteList: List<Emoticon>

    init {
        val resp = ClientMsgProtobuf<CMsgClientEmoticonList.Builder>(
            CMsgClientEmoticonList::class.java,
            packetMsg,
        )
        jobID = resp.targetJobID

        emoteList = buildList {
            resp.body.emoticonsList.mapTo(this) {
                Emoticon(name = it.name.substring(1, it.name.length - 1), appID = it.appid, isSticker = false)
            }
            resp.body.stickersList.mapTo(this) {
                Emoticon(name = it.name, appID = it.appid, isSticker = true)
            }
        }
    }
}
