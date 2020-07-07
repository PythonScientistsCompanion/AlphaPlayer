package com.ss.ugc.android.alpha_player.render

import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.view.Surface
import javax.sql.DataSource

interface IRender : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    fun setSurfaceListener(surfaceListener: SurfaceListener)

    fun onFirstFrame()

    fun onCompletion()

//    fun setScaleType(scaleType: DataSource.ScaleType)

    interface SurfaceListener {
        fun onSurfacePrepared(surface: Surface)

        fun onSurfaceDestroyed()
    }
}