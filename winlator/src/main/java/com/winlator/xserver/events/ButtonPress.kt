package com.winlator.xserver.events

import com.winlator.xserver.Bitmask
import com.winlator.xserver.Window

class ButtonPress(
    detail: Byte,
    root: Window,
    event: Window,
    child: Window?,
    rootX: Short,
    rootY: Short,
    eventX: Short,
    eventY: Short,
    state: Bitmask,
) : InputDeviceEvent(4, detail, root, event, child, rootX, rootY, eventX, eventY, state)
