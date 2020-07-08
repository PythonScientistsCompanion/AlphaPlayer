package com.ss.ugc.android.alpha_player

import com.ss.ugc.android.alpha_player.model.ScaleType

interface IPlayerAction {

    fun onVideoSizeChanged(videoWidth: Int, videoHeight: Int, scaleType: ScaleType)

    fun startAction()

    fun endAction()
}