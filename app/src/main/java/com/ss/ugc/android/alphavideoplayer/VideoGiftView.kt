package com.ss.ugc.android.alphavideoplayer

import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.ss.ugc.android.alpha_player.IMonitor
import com.ss.ugc.android.alpha_player.IPlayerAction
import com.ss.ugc.android.alpha_player.controller.IPlayerController
import com.ss.ugc.android.alpha_player.controller.PlayerController
import com.ss.ugc.android.alpha_player.model.Configuration
import com.ss.ugc.android.alpha_player.model.DataSource
import com.ss.ugc.android.alpha_player.player.DefaultSystemPlayer
import com.ss.ugc.android.alphavideoplayer.utils.JsonUtil

class VideoGiftView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        val TAG = "VideoGiftView"
    }

    val mVideoContainer: RelativeLayout
    var mPlayerController: IPlayerController? = null

    init {
        LayoutInflater.from(context).inflate(getResourceLayout(), this)
        mVideoContainer = findViewById(R.id.video_view)
    }

    private fun getResourceLayout(): Int {
        return R.layout.view_video_gift
    }

    fun initPlayerController(context: Context, owner: LifecycleOwner, playerAction: IPlayerAction, monitor: IMonitor) {
        val configuration = Configuration(context, owner)
        mPlayerController = PlayerController.get(configuration, DefaultSystemPlayer())
        mPlayerController!!.setPlayerAction(playerAction)
        mPlayerController!!.setMonitor(monitor)
    }

    fun startVideoGift(filePath: String) {
        if (TextUtils.isEmpty(filePath)) {
            return
        }
        val configModel = JsonUtil.parseConfigModel(filePath)
        val dataSource = DataSource()
            .setBaseDir(filePath)
            .setPortraitPath(configModel.portraitItem!!.path!!, configModel.portraitItem!!.alignMode)
            .setLandscapePath(configModel.landscapeItem!!.path!!, configModel.landscapeItem!!.alignMode)
        startDataSource(dataSource)
    }

    fun startDataSource(dataSource: DataSource) {
        if (!dataSource.isValid()) {
            Log.e(TAG, "startDataSource: dataSource is invalid.")
        }
        mPlayerController?.start(dataSource)
    }

    fun attachView() {
        mPlayerController?.attachAlphaView(mVideoContainer)
    }

    fun detachView() {
        mPlayerController?.detachAlphaView(mVideoContainer)
    }

    fun releasePlayerController() {
        mPlayerController?.let {
            it.detachAlphaView(mVideoContainer)
            it.release()
        }
    }
}