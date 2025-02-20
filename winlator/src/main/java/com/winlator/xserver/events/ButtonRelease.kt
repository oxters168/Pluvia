package com.winlator.xserver.events

import com.winlator.xserver.Bitmask
import com.winlator.xserver.Window

class ButtonRelease(
    detail: Byte,
    root: Window,
    event: Window,
    child: Window?,
    rootX: Short,
    rootY: Short,
    eventX: Short,
    eventY: Short,
    state: Bitmask,
) : InputDeviceEvent(5, detail, root, event, child, rootX, rootY, eventX, eventY, state)
