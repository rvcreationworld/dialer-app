package com.rajdialer.app.core

sealed class CallAction {
    data class Dial(val number: String) : CallAction()
    object Answer : CallAction()
    object Reject : CallAction()
    object Disconnect : CallAction()
    object Hold : CallAction()
    object Unhold : CallAction()
    object Mute : CallAction()
    object Unmute : CallAction()
}
