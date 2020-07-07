package com.ss.ugc.android.alpha_player

interface IMonitor {
    fun monitor(state: Boolean, playType: String, what: Int, extra: Int, errorInfo: String)
}