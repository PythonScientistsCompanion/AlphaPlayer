package com.ss.ugc.android.alphavideoplayer

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import com.ss.ugc.android.alpha_player.IMonitor
import com.ss.ugc.android.alpha_player.IPlayerAction
import com.ss.ugc.android.alpha_player.model.ScaleType
import com.ss.ugc.android.alphavideoplayer.utils.PermissionUtils
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    val basePath = Environment.getExternalStorageDirectory().absolutePath

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PermissionUtils.verifyStoragePermissions(this)
        initVideoGiftView()
    }

    fun initVideoGiftView() {
        video_gift_view.initPlayerController(this, this, playerAction, monitor)
    }

    val playerAction = object : IPlayerAction {
        override fun onVideoSizeChanged(videoWidth: Int, videoHeight: Int, scaleType: ScaleType) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun startAction() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun endAction() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    val monitor = object : IMonitor {
        override fun monitor(state: Boolean, playType: String, what: Int, extra: Int, errorInfo: String) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    fun attachView(v: View) {
        video_gift_view.attachView()
    }

    fun detachView(v: View) {
        video_gift_view.detachView()
    }

    fun playGift(v: View) {
        val testPath = getResourcePath()
        Log.i("dzy", "play gift file path : $testPath")
        if ("".equals(testPath)) {
            Toast.makeText(this, "请先运行 gift_install.sh gift/hotsoonNew 导入视频礼物", Toast.LENGTH_SHORT)
                .show()
        }
        video_gift_view.startVideoGift(testPath)
    }

    private fun getResourcePath(): String {
        val dirPath = basePath + File.separator + "videoGift" + File.separator
        val dirFile = File(dirPath)
        if (dirFile.exists() && dirFile.listFiles() != null && dirFile.listFiles().isNotEmpty()) {
            return dirFile.listFiles()[0].absolutePath
        }
        return ""
    }

    override fun onDestroy() {
        super.onDestroy()
        video_gift_view.releasePlayerController()
    }
}
