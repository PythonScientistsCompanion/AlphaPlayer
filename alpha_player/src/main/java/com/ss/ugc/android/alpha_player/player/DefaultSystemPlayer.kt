package com.ss.ugc.android.alpha_player.player

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.text.TextUtils
import android.view.Surface
import com.ss.ugc.android.alpha_player.model.VideoInfo
import java.lang.Exception

/**
 * created by dengzhuoyao on 2020/07/07
 */
class DefaultSystemPlayer : AbsPlayer() {

    var mediaPlayer : MediaPlayer? = null
    val retriever: MediaMetadataRetriever = MediaMetadataRetriever()
    lateinit var dataPath : String

    init {
        mediaPlayer?.setOnCompletionListener(MediaPlayer.OnCompletionListener { mediaPlayer ->
            if (completionListener != null) {
                completionListener.onCompletion()
            }
        })

        mediaPlayer?.setOnPreparedListener(MediaPlayer.OnPreparedListener { mediaPlayer ->
            if (preparedListener != null) {
                preparedListener.onPrepared()
            }
        })

        mediaPlayer?.setOnErrorListener(MediaPlayer.OnErrorListener { mp, what, extra ->
            if (errorListener != null) {
                errorListener.onError(what, extra, "")
            }
            false
        })

        mediaPlayer?.setOnInfoListener { mp, what, extra ->
            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                if (firstFrameListener != null) {
                    firstFrameListener.onFirstFrame()
                }
            }
            false
        }
    }

    override fun initMediaPlayer() {
        mediaPlayer = MediaPlayer()
    }

    override fun setSurface(surface: Surface) {
        mediaPlayer?.setSurface(surface)
    }

    override fun setDataSource(dataPath: String) {
        this.dataPath = dataPath
        mediaPlayer?.setDataSource(dataPath)
    }

    override fun prepareAsync() {
        mediaPlayer?.prepareAsync()
    }

    override fun start() {
        mediaPlayer?.start()
    }

    override fun pause() {
        mediaPlayer?.pause()
    }

    override fun stop() {
        mediaPlayer?.stop()
    }

    override fun reset() {
        mediaPlayer?.reset()
        this.dataPath = ""
    }

    override fun release() {
        mediaPlayer?.release()
        this.dataPath = ""
    }

    override fun setLooping(looping: Boolean) {
        mediaPlayer?.isLooping = looping
    }

    override fun setScreenOnWhilePlaying(onWhilePlaying: Boolean) {
        mediaPlayer?.setScreenOnWhilePlaying(onWhilePlaying)
    }

    override fun getVideoInfo(): VideoInfo {
        if (TextUtils.isEmpty(dataPath)) {
            throw Exception("dataPath is null, please set setDataSource firstly!")
        }

        retriever.setDataSource(dataPath)
        val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        if (TextUtils.isEmpty(widthStr) || TextUtils.isEmpty(heightStr)) {
            throw Exception("DefaultSystemPlayer get metadata failure!")
        }

        val videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
        val videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()

        return VideoInfo(videoWidth, videoHeight)
    }
}