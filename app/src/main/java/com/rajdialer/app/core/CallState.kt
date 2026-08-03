package com.rajdialer.app.core

enum class CallState {
    IDLE,
    DIALING,
    RINGING,
    ACTIVE,
    HOLDING,
    DISCONNECTED
}

enum class CallDirection {
    INCOMING, OUTGOING, UNKNOWN
}
