package com.ss.ugc.android.alpha_player.player

import android.content.Context

/**
 * created by dengzhuoyao on 2020/07/07
 */
abstract class AbsPlayer<T : AbsPlayer<T>>(context: Context) : IMediaPlayer<AbsPlayer<T>>{

    lateinit var completionListener: IMediaPlayer.OnCompletionListener<AbsPlayer<T>>
    lateinit var preparedListener: IMediaPlayer.OnPreparedListener<AbsPlayer<T>>
    lateinit var errorListener: IMediaPlayer.OnErrorListener<AbsPlayer<T>>
    lateinit var firstFrameListener: IMediaPlayer.OnFirstFrameListener<AbsPlayer<T>>

    override fun setOnCompletionListener(completionListener: IMediaPlayer.OnCompletionListener<AbsPlayer<T>>) {
        this.completionListener = completionListener
    }

    override fun setOnPreparedListener(preparedListener: IMediaPlayer.OnPreparedListener<AbsPlayer<T>>) {
        this.preparedListener = preparedListener
    }

    override fun setOnErrorListener(errorListener: IMediaPlayer.OnErrorListener<AbsPlayer<T>>) {
        this.errorListener = errorListener
    }

    override fun setOnFirstFrameListener(firstFrameListener: IMediaPlayer.OnFirstFrameListener<AbsPlayer<T>>) {
        this.firstFrameListener = firstFrameListener
    }
}