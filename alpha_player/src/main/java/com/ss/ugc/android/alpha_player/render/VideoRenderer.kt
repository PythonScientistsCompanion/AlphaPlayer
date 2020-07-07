package com.ss.ugc.android.alpha_player.render

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Build
import android.util.Log
import android.view.Surface
import com.ss.ugc.android.alpha_player.model.ScaleType
import com.ss.ugc.android.alpha_player.utils.ShaderUtil
import com.ss.ugc.android.alpha_player.utils.TextureCropUtil
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VideoRenderer: IRender {

    companion object {
        val TAG = "VideoRenderer"
        val FLOAT_SIZE_BYTES = 4
        val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES
        val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
        val TRIANGLE_VERTICES_DATA_UV_OFFSET = 3

        val GL_TEXTURE_EXTERNAL_OES: Int = 0x8D65;
    }

    var halfRightVerticeData = floatArrayOf(
        // X, Y, Z, U, V
        -1.0f, -1.0f, 0f, 0.5f, 0f,
        1.0f, -1.0f, 0f, 1f, 0f,
        -1.0f, 1.0f, 0f, 0.5f, 1f,
        1.0f, 1.0f, 0f, 1f, 1f
    )
    var triangleVertices: FloatBuffer? = null
    var mVPMatrix: FloatArray = FloatArray(16, {0f})
    var sTMatrix: FloatArray = FloatArray(16, {0f})

    var programId: Int = 0
    var textureId: Int = 0
    var uMVPMatrixHandle: Int = 0
    var uSTMatrixHandle: Int = 0
    var aPositionHandle: Int = 0
    var aTextureHandle: Int = 0

    var surfaceTexture: SurfaceTexture? = null
    var updateSurface: AtomicBoolean = AtomicBoolean(false)
    var canDraw: AtomicBoolean = AtomicBoolean(false)

    var glSurfaceView: GLSurfaceView? = null
    var mSurfaceListener: IRender.SurfaceListener? = null
    var mScaleType = ScaleType.ScaleAspectFill

    constructor(glSurfaceView: GLSurfaceView) {
        this.glSurfaceView = glSurfaceView
        triangleVertices = ByteBuffer.allocateDirect(halfRightVerticeData.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        triangleVertices?.put(halfRightVerticeData)?.position(0)
        Matrix.setIdentityM(sTMatrix, 0)
    }

    override fun setSurfaceListener(surfaceListener: IRender.SurfaceListener) {
        this.mSurfaceListener = surfaceListener
    }

    override fun onFirstFrame() {
        canDraw.compareAndSet(false, true)
        glSurfaceView!!.requestRender()
    }

    override fun onCompletion() {
        canDraw.compareAndSet(true, false)
        glSurfaceView!!.requestRender()
    }

    override fun setScaleType(scaleType: ScaleType) {
        this.mScaleType = scaleType
    }

    override fun measureInternal(
        viewWidth: Float, viewHeight: Float,
        videoWidth: Float, videoHeight: Float) {
        if (viewWidth <= 0 || viewHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) {
            return
        }
        halfRightVerticeData = TextureCropUtil.calculateHalfRightVerticeData(mScaleType, viewWidth, viewHeight, videoWidth, videoHeight)
        triangleVertices = ByteBuffer.allocateDirect(halfRightVerticeData.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        triangleVertices?.put(halfRightVerticeData)?.position(0)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (updateSurface.compareAndSet(true, false)) {
            try {
                surfaceTexture?.updateTexImage()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            surfaceTexture?.getTransformMatrix(sTMatrix)
        }

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT and GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        if (!canDraw.get()) {
            GLES20.glFinish()
            return
        }
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        GLES20.glUseProgram(programId)
        checkGlError("glUseProgram")

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId)

        triangleVertices?.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
        GLES20.glVertexAttribPointer(
            aPositionHandle, 3, GLES20.GL_FLOAT, false,
            TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices
        )
        checkGlError("glVertexAttribPointer maPosition")
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        checkGlError("glEnableVertexAttribArray aPositionHandle")

        triangleVertices?.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
        GLES20.glVertexAttribPointer(
            aTextureHandle, 3, GLES20.GL_FLOAT, false,
            TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices
        )
        checkGlError("glVertexAttribPointer aTextureHandle")
        GLES20.glEnableVertexAttribArray(aTextureHandle)
        checkGlError("glEnableVertexAttribArray aTextureHandle")

        Matrix.setIdentityM(mVPMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mVPMatrix, 0)
        GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, sTMatrix, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        checkGlError("glDrawArrays")

        GLES20.glFinish()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val vertexShader: String = ShaderUtil.loadFromAssetsFile("vertex.sh", glSurfaceView?.resources)
        val fragShader: String = ShaderUtil.loadFromAssetsFile("frag.sh", glSurfaceView?.resources)
        programId = createProgram(vertexShader, fragShader)
        if (programId == 0) {
            return
        }
        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition")
        checkGlError("glGetAttribLocation aPosition")
        if (aPositionHandle == -1) {
            throw RuntimeException("Could not get attrib location for aPosition")
        }
        aTextureHandle = GLES20.glGetAttribLocation(programId, "aTextureCoord")
        checkGlError("glGetAttribLocation aTextureCoord")
        if (aTextureHandle == -1) {
            throw RuntimeException("Could not get attrib location for aTextureCoord")
        }
        uMVPMatrixHandle = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        checkGlError("glGetUniformLocation uMVPMatrix")
        if (uMVPMatrixHandle == -1) {
            throw RuntimeException("Could not get attrib location for uMVPMatrix")
        }
        uSTMatrixHandle = GLES20.glGetUniformLocation(programId, "uSTMatrix")
        checkGlError("glGetUniformLocation uSTMatrix")
        if (uSTMatrixHandle == -1) {
            throw RuntimeException("Could not get attrib location for uSTMatrix")
        }
        prepareSurface()
    }

    private fun prepareSurface() {
        val textures = IntArray(1, {0})
        GLES20.glGenTextures(1, textures, 0)

        textureId = textures[0]
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId)
        checkGlError("glBindTexture textureID")

        GLES20.glTexParameterf(
            GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST.toFloat()
        )
        GLES20.glTexParameterf(
            GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )

        surfaceTexture = SurfaceTexture(textureId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            surfaceTexture!!.setDefaultBufferSize(
                glSurfaceView!!.getMeasuredWidth(),
                glSurfaceView!!.getMeasuredHeight()
            )
        }
        surfaceTexture!!.setOnFrameAvailableListener(this)

        val surface = Surface(this.surfaceTexture)
        mSurfaceListener?.onSurfacePrepared(surface)
        updateSurface.compareAndSet(true, false)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        updateSurface.compareAndSet(false, true)
        glSurfaceView!!.requestRender()
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) {
            return 0
        }
        val fragShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (fragShader == 0) {
            return 0
        }

        var program = GLES20.glCreateProgram()
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader)
            checkGlError("glAttachShader")
            GLES20.glAttachShader(program, fragShader)
            checkGlError("glAttachShader")
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1, {0})
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ")
                Log.e(TAG, GLES20.glGetProgramInfoLog(program))
                GLES20.glDeleteProgram(program)
                program = 0
            }
        }
        return program
    }

    private fun loadShader(shaderType: Int, source: String): Int {
        var shader = GLES20.glCreateShader(shaderType)
        if (shader != 0) {
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1, {0})
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader $shaderType:")
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader))
                GLES20.glDeleteShader(shader)
                shader = 0
            }
        }
        return shader
    }

    private fun checkGlError(op: String) {
        val error: Int = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "$op: glError $error")
        }
    }
}