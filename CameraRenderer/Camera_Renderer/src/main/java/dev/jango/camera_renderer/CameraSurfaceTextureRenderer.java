package dev.jango.camera_renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

public class CameraSurfaceTextureRenderer extends CameraRenderer
{
    private static final String TAG = CameraSurfaceTextureRenderer.class.getName();

    private final FloatBuffer    mPositionBuffer;
    private       FloatBuffer    mUVBuffer = null;

    private int mShaderHandle;
    private int mShaderPosition;
    private int mShaderUV;
    private int mShaderSampler;

    private volatile int mSurfaceDirtyCounter = 0;
    private float[] mTransform = new float[16];
    float[][] m_preTransform = { {0, 1, 0, 1}, {1, 1, 0, 1}, {0, 0, 0, 1}, {1, 0, 0, 1} };
    float[][] mTransformR = new float[4][4];
    float[] uvs = new float[8];

    public CameraSurfaceTextureRenderer(CameraController controller, Camera.PreviewCallback callback)
    {
        super(controller, callback);
        loadShaders();

        final float d = 1;
        final float[] position = { 1, 1, d, -1, 1, d, 1, -1, d, -1, -1, d };
        mPositionBuffer = ByteBuffer.allocateDirect(position.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mPositionBuffer.put(position).position(0);
        mUVBuffer = ByteBuffer.allocateDirect(position.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    /**
     * Removes self as a frame listener and releases OpenGL shader handle.
     * <p>Must be called from a thread holding a valid OpenGL context.</p>
     */
    public void release()
    {
        mSurfaceTexture.setOnFrameAvailableListener(null);

        GLES20.glDeleteProgram(mShaderHandle);
        mShaderHandle = 0;
    }

    /**
     * Draws a frame from the camera's stream.
     * <p>Must be called from a thread holding a valid OpenGL context.</p>
     * <p>Updates the internal texture to the latest video frame if a new one is available.</p>
     */
    public void draw()
    {
        long startTime = System.currentTimeMillis();
        // Note:
        //     This function is called from GLSurfaceView.Renderer.onDrawFrame() callback
        //     Below variables can also be accessed from other threads, for example, SurfaceTexture.onFrameAvailable() callback
        //     We need to make them thread-safe to avoid freezing camera image rendering
        synchronized (this) {
            if (mSurfaceDirtyCounter > 0) {
                mSurfaceTexture.updateTexImage();
                mSurfaceTexture.getTransformMatrix(mTransform);

                for (int i = 0; i < 4; i++)
                    Matrix.multiplyMV(mTransformR[i], 0, mTransform, 0, m_preTransform[i], 0);

                uvs[0] = mTransformR[1][0];
                uvs[1] = mTransformR[1][1];
                uvs[2] = mTransformR[0][0];
                uvs[3] = mTransformR[0][1];
                uvs[4] = mTransformR[3][0];
                uvs[5] = mTransformR[3][1];
                uvs[6] = mTransformR[2][0];
                uvs[7] = mTransformR[2][1];

                mUVBuffer.put(uvs).position(0);

                mSurfaceDirtyCounter--;
            }
        }

        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_ALWAYS);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        Log.d(TAG, "Camera GPU Renderer Draw");
        GLES20.glUseProgram(mShaderHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId[0]);

        GLES20.glUniform1i(mShaderSampler, 0);

        GLES20.glEnableVertexAttribArray(mShaderPosition);
        GLES20.glEnableVertexAttribArray(mShaderUV);

        GLES20.glVertexAttribPointer(mShaderPosition, 3, GLES20.GL_FLOAT, false, 0, mPositionBuffer);
        GLES20.glVertexAttribPointer(mShaderUV, 2, GLES20.GL_FLOAT, false, 0, mUVBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(mShaderPosition);
        GLES20.glDisableVertexAttribArray(mShaderUV);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);
        GLES20.glDepthFunc(GLES20.GL_LESS);
        long endTime = System.currentTimeMillis();
        Log.d(TAG, "Camera Render Time: "+(endTime-startTime));
    }

    /**
     * Loads the shaders used for drawing the stream
     * <p>Fragment shader uses require directive for <code>GL_OES_EGL_image_external</code>.</p>
     * <p>Fragment shader uses <code>samplerExternalOES</code> as its sampler object.</p>
     */
    private void loadShaders()
    {
        final String srcVertex =
                "precision lowp float;"      +
                        "attribute vec4 position;"   +
                        "attribute vec2 uv;"         +
                        "varying   vec2 vUV;"        +
                        "void main(){"               +
                        "	vUV = uv;"               +
                        "	gl_Position = position;" +
                        "}";

        final String srcFragment =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision lowp float;"                            +
                        "uniform samplerExternalOES texture;"              +
                        "varying vec2 vUV;"                                +
                        "void main(){"                                     +
                        "	gl_FragColor = texture2D(texture, vUV);"       +
                        "}";

        mShaderHandle   = Shader.load(srcVertex, srcFragment);

        mShaderPosition = GLES20.glGetAttribLocation(mShaderHandle, "position");
        mShaderUV       = GLES20.glGetAttribLocation(mShaderHandle, "uv");
        mShaderSampler  = GLES20.glGetUniformLocation(mShaderHandle, "texture");
    }

    /**
     * Signals that a new video frame is available.
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture)
    {
        synchronized (this) {
            mSurfaceDirtyCounter++;
        }
    }
}
