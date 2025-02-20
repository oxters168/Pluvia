package com.winlator.xserver.events

import com.winlator.xserver.Bitmask
import com.winlator.xserver.Window

class MotionNotify(
    detail: Boolean,
    root: Window,
    event: Window,
    child: Window?,
    rootX: Short,
    rootY: Short,
    eventX: Short,
    eventY: Short,
    state: Bitmask,
) : InputDeviceEvent(6, (if (detail) 1 else 0).toByte(), root, event, child, rootX, rootY, eventX, eventY, state)
