package com.winlator.xserver.errors

class BadFence(id: Int) : XRequestError(Byte.MIN_VALUE + 2, id)
