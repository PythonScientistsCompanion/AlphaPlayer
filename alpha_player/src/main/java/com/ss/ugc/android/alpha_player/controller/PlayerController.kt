package com.ss.ugc.android.alpha_player.controller

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.Process
import android.support.annotation.WorkerThread
import android.text.TextUtils
import android.util.Log
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import com.ss.ugc.android.alpha_player.IMonitor
import com.ss.ugc.android.alpha_player.IPlayerAction
import com.ss.ugc.android.alpha_player.model.Configuration
import com.ss.ugc.android.alpha_player.model.DataSource
import com.ss.ugc.android.alpha_player.player.DefaultSystemPlayer
import com.ss.ugc.android.alpha_player.player.IMediaPlayer
import com.ss.ugc.android.alpha_player.player.PlayerState
import com.ss.ugc.android.alpha_player.render.VideoRenderer
import com.ss.ugc.android.alpha_player.widget.AlphaVideoView
import java.io.File
import java.lang.Exception

/**
 * created by dengzhuoyao on 2020/07/08
 */
class PlayerController(context: Context, owner: LifecycleOwner, mediaPlayer: IMediaPlayer): IPlayerControllerExt, LifecycleObserver, Handler.Callback {

    companion object {
        val INIT_MEDIA_PLAYER: Int = 1
        val SET_DATA_SOURCE: Int =  2
        val START: Int = 3
        val PAUSE: Int = 4
        val RESUME: Int = 5
        val STOP: Int = 6
        val DESTROY: Int = 7
        val SURFACE: Int = 8
        val RESET: Int = 9

        fun get(configuration: Configuration, mediaPlayer: IMediaPlayer? = null): PlayerController {
            return PlayerController(configuration.context, configuration.lifecycleOwner,
                mediaPlayer ?: DefaultSystemPlayer())
        }
    }

    var isPlaying : Boolean = false
    var playerState = PlayerState.NOT_PREPARED
    val context: Context
    var mMonitor: IMonitor? = null
    var mPlayerAction: IPlayerAction? = null
    val mediaPlayer: IMediaPlayer
    var alphaVideoView: AlphaVideoView? = null

    var workHandler: Handler? = null
    val mainHandler: Handler = Handler(Looper.getMainLooper())
    var playThread: HandlerThread? = null

    private val mPreparedListener = object: IMediaPlayer.OnPreparedListener {
        override fun onPrepared() {
            sendMessage(getMessage(START, null))
        }
    }

    private val mErrorListener = object : IMediaPlayer.OnErrorListener {
        override fun onError(what: Int, extra: Int, desc: String) {
            monitor(false, what, extra, "mediaPlayer error, info: $desc")
            emitEndSignal()
        }
    }

    init {
        this.context = context
        this.mediaPlayer = mediaPlayer
        init(owner)
        initAlphaView()
        initMediaPlayer()
    }

    private fun init(owner: LifecycleOwner) {
        owner.lifecycle.addObserver(this)
        playThread = HandlerThread("alpha-play-thread", Process.THREAD_PRIORITY_BACKGROUND)
        playThread!!.start()
        workHandler = Handler(playThread!!.looper, this)
    }

    private fun initAlphaView() {
        alphaVideoView = AlphaVideoView(context, null)
        alphaVideoView!!.let {
            val layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
            it.layoutParams = layoutParams
            it.setPlayerController(this)
            it.setVideoRenderer(VideoRenderer(it))
        }
    }

    private fun initMediaPlayer() {
        sendMessage(getMessage(INIT_MEDIA_PLAYER, null))
    }

    override fun setPlayerAction(playerAction: IPlayerAction) {
        this.mPlayerAction = playerAction
    }

    override fun setMonitor(monitor: IMonitor) {
        this.mMonitor = monitor
    }

    override fun setVisibility(visibility: Int) {
        alphaVideoView!!.visibility = visibility
        if (visibility == View.VISIBLE) {
            alphaVideoView!!.bringToFront()
        }
    }

    override fun attachAlphaView(parentView: ViewGroup) {
        if (parentView.indexOfChild(alphaVideoView) == -1) {
            alphaVideoView!!.parent?.let {
                (it as ViewGroup).removeView(alphaVideoView)
            }
            parentView.addView(alphaVideoView)
        }
    }

    override fun detachAlphaView(parentView: ViewGroup) {
        parentView.removeView(alphaVideoView)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        pause()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        resume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        stop()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        release()
    }

    private fun sendMessage(msg: Message) {
        playThread?.let {
            if (it.isAlive && !it.isInterrupted) {
                when (workHandler) {
                    null -> workHandler = Handler(it.looper, this)
                }
                workHandler!!.sendMessageDelayed(msg, 0)
            }
        }
    }

    private fun getMessage(what: Int, obj: Any?): Message {
        val message = Message.obtain()
        message.what = what
        message.obj = obj
        return message
    }

    override fun setSurface(surface: Surface) {
        sendMessage(getMessage(SURFACE, surface))
    }

    override fun start(dataSource: DataSource) {
        if (dataSource.isValid()) {
            setVisibility(View.VISIBLE)
            sendMessage(getMessage(SET_DATA_SOURCE, dataSource))
        } else {
            emitEndSignal()
            monitor(false, errorInfo = "dataSource is invalid!")
        }
    }

    override fun pause() {
        sendMessage(getMessage(PAUSE, null))
    }

    override fun resume() {
        sendMessage(getMessage(RESUME, null))
    }

    override fun stop() {
        sendMessage(getMessage(STOP, null))
    }

    override fun reset() {
        sendMessage(getMessage(RESET, null))
    }

    override fun release() {
        sendMessage(getMessage(DESTROY, null))
    }

    override fun getView(): View {
        return alphaVideoView!!
    }

    override fun getPlayerType(): String {
        return mediaPlayer.getPlayerType()
    }

    @WorkerThread
    private fun initPlayer() {
        mediaPlayer.initMediaPlayer()
        mediaPlayer.setScreenOnWhilePlaying(true)
        mediaPlayer.setLooping(false)

        mediaPlayer.setOnFirstFrameListener(object : IMediaPlayer.OnFirstFrameListener {
            override fun onFirstFrame() {
                alphaVideoView!!.onFirstFrame()
            }
        })
        mediaPlayer.setOnCompletionListener(object : IMediaPlayer.OnCompletionListener {
            override fun onCompletion() {
                alphaVideoView!!.onCompletion()
                playerState = PlayerState.PAUSED
                monitor(true, errorInfo = "")
                emitEndSignal()
            }
        })
    }

    @WorkerThread
    private fun setDataSource(dataSource: DataSource) {
        try {
            setVideoFromFile(dataSource)
        } catch (e: Exception) {
            e.printStackTrace()
            monitor(false, errorInfo = "alphaVideoView set dataSource failure: " + Log.getStackTraceString(e))
            emitEndSignal()
        }
    }

    @WorkerThread
    private fun setVideoFromFile(dataSource: DataSource) {
        mediaPlayer.reset()
        playerState = PlayerState.NOT_PREPARED
        val orientation = context.resources.configuration.orientation

        val dataPath = dataSource.getPath(orientation)
        val scaleType = dataSource.getScaleType(orientation)
        if (TextUtils.isEmpty(dataPath) || !File(dataPath).exists()) {
            monitor(false, errorInfo = "dataPath is empty or File is not exists. path = $dataPath")
            emitEndSignal()
            return
        }
        scaleType?.let {
            alphaVideoView!!.setScaleType(it)
        }
        mediaPlayer.setDataSource(dataPath)
        if (alphaVideoView!!.isSurfaceCreated) {
            prepareAsync()
        }
    }

    @WorkerThread
    private fun prepareAsync() {
        mediaPlayer?.let {
            if (playerState == PlayerState.NOT_PREPARED || playerState == PlayerState.STOPPED) {
                it.setOnPreparedListener(mPreparedListener)
                it.setOnErrorListener(mErrorListener)
                it.prepareAsync()
            }
        }
    }

    @WorkerThread
    private fun startPlay() {
        when (playerState) {
            PlayerState.PREPARED -> {
                mediaPlayer.start()
                isPlaying = true
                playerState = PlayerState.STARTED
                mainHandler.post {
                    mPlayerAction?.startAction()
                }
            }
            PlayerState.PAUSED -> {
                mediaPlayer.start()
                playerState = PlayerState.STARTED
            }
            PlayerState.NOT_PREPARED, PlayerState.STOPPED -> {
                try {
                    prepareAsync()
                } catch (e: Exception) {
                    e.printStackTrace()
                    monitor(false, errorInfo = "prepare and start MediaPlayer failure!")
                    emitEndSignal()
                }
            }
        }
    }

    @WorkerThread
    private fun parseVideoSize() {
        val videoInfo = mediaPlayer.getVideoInfo()
        alphaVideoView!!.measureInternal((videoInfo.videoWidth / 2).toFloat(), videoInfo.videoHeight.toFloat())

        val scaleType = alphaVideoView!!.mScaleType
        mainHandler.post {
            mPlayerAction?.onVideoSizeChanged(videoInfo.videoWidth / 2, videoInfo.videoHeight, scaleType)
        }
    }

    override fun handleMessage(msg: Message?): Boolean {
        msg?.let {
            when(msg.what) {
                INIT_MEDIA_PLAYER -> {
                    initPlayer()
                }
                SURFACE -> {
                    val surface = msg.obj as Surface
                    mediaPlayer.setSurface(surface)
                }
                SET_DATA_SOURCE -> {
                    val dataSource = msg.obj as DataSource
                    setDataSource(dataSource)
                }
                START -> {
                    try {
                        parseVideoSize()
                        playerState = PlayerState.PREPARED
                        startPlay()
                    } catch (e: Exception) {
                        monitor(false, errorInfo = "start video failure: " + Log.getStackTraceString(e))
                        emitEndSignal()
                    }
                }
                PAUSE -> {
                    when (playerState) {
                        PlayerState.STARTED -> {
                            mediaPlayer.pause()
                            playerState = PlayerState.PAUSED
                        }
                        else -> {}
                    }
                }
                RESUME -> {
                    if (isPlaying) {
                        startPlay()
                    } else {
                    }
                }
                STOP -> {
                    when (playerState) {
                        PlayerState.STARTED, PlayerState.PAUSED -> {
                            mediaPlayer.stop()
                            playerState = PlayerState.STOPPED
                        }
                        else -> {}
                    }
                }
                DESTROY -> {
                    alphaVideoView!!.onPause()
                    if (playerState == PlayerState.STARTED) {
                        mediaPlayer.pause()
                        playerState = PlayerState.PAUSED
                    }
                    if (playerState == PlayerState.PAUSED) {
                        mediaPlayer.stop()
                        playerState = PlayerState.STOPPED
                    }
                    mediaPlayer.release()
                    alphaVideoView!!.release()
                    playerState = PlayerState.RELEASE

                    playThread?.let {
                        it.quit()
                        it.interrupt()
                    }
                }
                RESET -> {
                    mediaPlayer.reset()
                    playerState = PlayerState.NOT_PREPARED
                    isPlaying = false
                }
                else -> {}
            }
        }
        return true
    }

    private fun emitEndSignal() {
        isPlaying = false
        mainHandler.post {
            mPlayerAction?.endAction()
        }
    }

    private fun monitor(state: Boolean, what: Int = 0, extra: Int = 0, errorInfo: String) {
        mMonitor?.monitor(state, getPlayerType(), what, extra, errorInfo)
    }
}