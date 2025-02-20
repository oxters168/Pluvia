package com.winlator.xserver.events

import com.winlator.xserver.Bitmask
import com.winlator.xserver.Window

class LeaveNotify(
    detail: Detail,
    root: Window,
    event: Window,
    child: Window?,
    rootX: Short,
    rootY: Short,
    eventX: Short,
    eventY: Short,
    state: Bitmask,
    mode: Mode,
    sameScreenAndFocus: Boolean,
) : PointerWindowEvent(8, detail, root, event, child, rootX, rootY, eventX, eventY, state, mode, sameScreenAndFocus)
