package com.winlator.xserver.events

import com.winlator.xserver.Bitmask
import com.winlator.xserver.Window

class KeyPress(
    keycode: Byte,
    root: Window,
    event: Window,
    child: Window?,
    rootX: Short,
    rootY: Short,
    eventX: Short,
    eventY: Short,
    state: Bitmask,
) : InputDeviceEvent(2, keycode, root, event, child, rootX, rootY, eventX, eventY, state)
