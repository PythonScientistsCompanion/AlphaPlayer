package com.ss.ugc.android.alpha_player.player

import android.view.Surface
import com.ss.ugc.android.alpha_player.model.VideoInfo
import java.io.IOException
import java.lang.Exception

interface IMediaPlayer<T : IMediaPlayer<T>> {

    fun setOnCompletionListener(completionListener: OnCompletionListener<T>)

    fun setOnPreparedListener(preparedListener: OnPreparedListener<T>)

    fun setOnErrorListener(errorListener: OnErrorListener<T>)

    fun setOnFirstFrameListener(firstFrameListener: OnFirstFrameListener<T>)

    fun setSurface(surface: Surface)

    @Throws(IOException::class)
    fun setDataSource(dataPath: String)

    fun prepareAsync()

    fun start()

    fun pause()

    fun stop()

    fun reset()

    fun release()

    fun setLooping(looping: Boolean)

    fun setScreenOnWhilePlaying(onWhilePlaying: Boolean)

    @Throws(Exception::class)
    fun getVideoInfo(): VideoInfo


    interface OnCompletionListener<T> {
        fun onCompletion(t: T)
    }

    interface OnPreparedListener<T> {
        fun onPrepared(t: T)
    }

    interface OnErrorListener<T> {
        fun onError(t: T, what: Int, extra: Int, desc: String)
    }

    interface OnFirstFrameListener<T> {
        fun onFirstFrame(t: T)
    }
}
