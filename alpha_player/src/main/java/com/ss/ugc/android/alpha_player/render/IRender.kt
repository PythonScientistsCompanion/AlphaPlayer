package com.ss.ugc.android.alpha_player.render

import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.view.Surface
import com.ss.ugc.android.alpha_player.model.ScaleType

/**
 * created by dengzhuoyao on 2020/07/07
 */
interface IRender : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    fun setSurfaceListener(surfaceListener: SurfaceListener)

    fun onFirstFrame()

    fun onCompletion()

    fun setScaleType(scaleType: ScaleType)

    fun measureInternal(viewWidth: Float, viewHeight: Float, videoWidth: Float, videoHeight: Float)

    interface SurfaceListener {
        fun onSurfacePrepared(surface: Surface)

        fun onSurfaceDestroyed()
    }
}