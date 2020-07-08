package com.ss.ugc.android.alpha_player.player

import android.view.Surface
import com.ss.ugc.android.alpha_player.model.VideoInfo
import java.io.IOException
import java.lang.Exception

/**
 * created by dengzhuoyao on 2020/07/07
 */
interface IMediaPlayer {

    fun setOnCompletionListener(completionListener: OnCompletionListener)

    fun setOnPreparedListener(preparedListener: OnPreparedListener)

    fun setOnErrorListener(errorListener: OnErrorListener)

    fun setOnFirstFrameListener(firstFrameListener: OnFirstFrameListener)

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


    interface OnCompletionListener {
        fun onCompletion()
    }

    interface OnPreparedListener {
        fun onPrepared()
    }

    interface OnErrorListener {
        fun onError(what: Int, extra: Int, desc: String)
    }

    interface OnFirstFrameListener {
        fun onFirstFrame()
    }
}
