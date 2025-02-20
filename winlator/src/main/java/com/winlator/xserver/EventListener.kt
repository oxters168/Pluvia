package com.winlator.xserver

import com.winlator.xserver.events.Event
import java.io.IOException
import timber.log.Timber

class EventListener(val client: XClient, val eventMask: Bitmask) {

    fun isInterestedIn(eventId: Int): Boolean = eventMask.isSet(eventId)

    fun isInterestedIn(mask: Bitmask): Boolean = this.eventMask.intersects(mask)

    fun sendEvent(event: Event) {
        try {
            event.send(client.sequenceNumber, client.outputStream)
        } catch (e: IOException) {
            Timber.e(e)
        }
    }
}
