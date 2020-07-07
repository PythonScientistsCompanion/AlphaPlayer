package com.ss.ugc.android.alpha_player

import android.widget.ImageView

interface IPlayerAction {

    fun onVideoSizeChanged(videoWidth: Int, videoHeight: Int, scaleType: ImageView.ScaleType)

    fun startAction()

    fun endAction()
}