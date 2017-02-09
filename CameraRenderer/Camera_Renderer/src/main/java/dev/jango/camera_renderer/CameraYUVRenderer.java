package dev.jango.camera_renderer;

import android.hardware.Camera;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class CameraYUVRenderer extends CameraRenderer {
    private ByteBuffer yBuffer;
    private ByteBuffer uvBuffer;
    private volatile byte[] cameraByteFrame;
    private final Object frameBufferLock = new Object();

    private static final String TAG = CameraYUVRenderer.class.getName();

    private final int mTextureId[] = new int[2];

    private FloatBuffer mVertices;
    private ShortBuffer mIndices;

    private final int vertexDataBufferID[] = new int[1];
    private final float[] mVerticesData = {
            -1.f, 1.f, 0.0f, // Position 0
            0.0f, 0.0f, // TexCoord 0
            -1.f, -1.f, 0.0f, // Position 1
            0.0f, 1.0f, // TexCoord 1
            1.f, -1.f, 0.0f, // Position 2
            1.0f, 1.0f, // TexCoord 2
            1.f, 1.f, 0.0f, // Position 3
            1.0f, 0.0f // TexCoord 3
    };

    private final short[] mIndicesData = {0, 1, 2, 0, 2, 3};

    private int mCameraFrameWidth = 0;
    private int mCameraFrameHeight = 0;

    public CameraYUVRenderer(CameraController controller, Camera.PreviewCallback callback) {
        super(controller,callback);
        Log.d(CameraYUVRenderer.class.getName(), "Preview Size: "+controller.previewWidth+", "+controller.previewHeight);
        setImageSize(controller.previewWidth, controller.previewHeight);

        GLES20.glGenBuffers(1, vertexDataBufferID, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexDataBufferID[0]);
        mVertices = ByteBuffer.allocateDirect(mVerticesData.length * Float.SIZE / Byte.SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(mVerticesData).position(0);

        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVerticesData.length * Float.SIZE / Byte.SIZE, mVertices, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        mIndices = ByteBuffer.allocateDirect(mIndicesData.length * Short.SIZE / Byte.SIZE).order(ByteOrder.nativeOrder()).asShortBuffer();
        mIndices.put(mIndicesData).position(0);

        loadShaders();
        GLES20.glGenTextures(mTextureId.length, mTextureId, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[1]);
        setTexture2DProperty();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[0]);
        setTexture2DProperty();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    private void setTexture2DProperty() {
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
    }

    private volatile boolean cameraFrameStarted = false;

    public void copyCameraFrameBuffer(byte[] frame) {
        synchronized (frameBufferLock) {
            if (cameraByteFrame == null) {
                cameraByteFrame = new byte[frame.length];
            }
            System.arraycopy(frame, 0, cameraByteFrame, 0, frame.length);
        }
        cameraFrameStarted = true;
    }

    /**
     * Removes self as a frame listener and releases OpenGL shader handle.
     * <p>Must be called from a thread holding a valid OpenGL context.</p>
     */
    public void release() {
        mSurfaceTexture.setOnFrameAvailableListener(null);
        GLES20.glDeleteProgram(m_ProgramHandle);
        m_ProgramHandle = 0;
    }

    /**
     * Draws a frame from the camera's onPreviewFrame.
     * <p>Must be called from a thread holding a valid OpenGL context.</p>
     * <p>Render the frame received from PreviewCallback.</p>
     */
    public void draw() {
        if (!cameraFrameStarted) return;
        long startTime = System.currentTimeMillis();
        if (mCameraFrameWidth == 0 && mCameraFrameHeight == 0) return;
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_ALWAYS);
        GLES20.glDepthMask(false);

        synchronized (frameBufferLock) {
            if (cameraByteFrame != null) {
                yBuffer.put(cameraByteFrame, 0, mCameraFrameWidth * mCameraFrameHeight);
                yBuffer.position(0);
                uvBuffer.put(cameraByteFrame, mCameraFrameWidth * mCameraFrameHeight, mCameraFrameWidth * mCameraFrameHeight / 2);
                uvBuffer.position(0);
            }
        }

        Log.d(TAG, "Camera CPU Renderer Draw");
        GLES20.glUseProgram(m_ProgramHandle);
        mVertices.position(0);
        GLES20.glVertexAttribPointer(mShaderPosition, 3, GLES20.GL_FLOAT, false, 5 * 4, mVertices);
        mVertices.position(3);
        GLES20.glVertexAttribPointer(mShaderTexCoordinate, 2, GLES20.GL_FLOAT, false, 5 * 4, mVertices);

        GLES20.glEnableVertexAttribArray(mShaderPosition);
        GLES20.glEnableVertexAttribArray(mShaderTexCoordinate);

        //Handle UV Buffer
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[1]);
        //UV texture is (width/2*height/2) in size (downsampled by 2 in
        //both dimensions, each pixel corresponds to 4 pixels of the Y channel)
        //and each pixel is two bytes. By setting GL_LUMINANCE_ALPHA, OpenGL
        //puts first byte (V) into R,G and B components and of the texture
        //and the second byte (U) into the A component of the texture. That's
        //why we find U and V at A and R respectively in the fragment shader code.
        //Note that we could have also found V at G or B as well.
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA,
                mCameraFrameWidth / 2, mCameraFrameHeight / 2, 0, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, uvBuffer);
        GLES20.glUniform1i(mShaderSampler_uv, 1);
        //Handle Y Buffer
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                mCameraFrameWidth, mCameraFrameHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yBuffer);
        GLES20.glUniform1i(mShaderSampler_y, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mIndicesData.length, GLES20.GL_UNSIGNED_SHORT, mIndices);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDisableVertexAttribArray(mShaderPosition);
        GLES20.glDisableVertexAttribArray(mShaderTexCoordinate);
        GLES20.glUseProgram(0);
        GLES20.glDepthFunc(GLES20.GL_LESS);
        long endTime = System.currentTimeMillis();
        Log.d(TAG, "Camera Render Time: " + (endTime - startTime));
    }

    private int m_ProgramHandle;
    private int mShaderPosition;
    private int mShaderTexCoordinate;
    private int mShaderSampler_y;
    private int mShaderSampler_uv;

    private void loadShaders() {
        final String srcVertex =
                "#version 100\n" +
                        "precision lowp float;" +
                        "attribute vec4 position;" +
                        "attribute vec2 texCoord;" +
                        "varying   vec2 vtexCoord;" +
                        "void main(){" +
                        "	vtexCoord = texCoord;" +
                        "	gl_Position = position;" +
                        "}";

        final String srcFragment =
                "#version 100\n" +
                        "precision lowp float;" +
                        "uniform sampler2D y_texture;" +
                        "uniform sampler2D uv_texture;" +
                        "varying vec2 vtexCoord;" +
                        "void main(){" +
                        "   float r,g,b, y,u,v;" +
                        //We had put the Y values of each pixel to the R,G,B components by
                        //GL_LUMINANCE, that's why we're pulling it from the R component,
                        //we could also use G or B
                        "	y = texture2D(y_texture, vtexCoord).r;" +

                        //We had put the U and V values of each pixel to the A and R,G,B
                        //components of the texture respectively using GL_LUMINANCE_ALPHA.
                        //Since U,V bytes are interspread in the texture, this is probably
                        //the fastest way to use them in the shader
                        "   u = texture2D(uv_texture, vtexCoord).a - 0.5;" +
                        "   v = texture2D(uv_texture, vtexCoord).r - 0.5;" +

                        //The numbers are just YUV to RGB conversion constants
                        //Formula reference link: https://en.wikipedia.org/wiki/YUV#Y.E2.80.B2UV420sp_.28NV21.29_to_RGB_conversion_.28Android.29
                        "   r = y + 1.370705*v;" +
                        "   g = y - 0.337633*u - 0.698001*v;" +
                        "   b = y + 1.732446*u;" +
                        "   gl_FragColor = vec4(r, g, b, 1.0);" +
                        "}";
        m_ProgramHandle = Shader.load(srcVertex, srcFragment);
        mShaderPosition = GLES20.glGetAttribLocation(m_ProgramHandle, "position");
        mShaderTexCoordinate = GLES20.glGetAttribLocation(m_ProgramHandle, "texCoord");
        mShaderSampler_y = GLES20.glGetUniformLocation(m_ProgramHandle, "y_texture");
        mShaderSampler_uv = GLES20.glGetUniformLocation(m_ProgramHandle, "uv_texture");
    }

    private void setImageSize(int width, int height) {
        mCameraFrameWidth = width;
        mCameraFrameHeight = height;
        //Allocate yBuffer
        yBuffer = ByteBuffer.allocateDirect(mCameraFrameWidth * mCameraFrameHeight);
        yBuffer.order(ByteOrder.nativeOrder());
        //Allocate uvBuffer
        uvBuffer = ByteBuffer.allocate(mCameraFrameWidth * mCameraFrameHeight / 2);
        uvBuffer.order(ByteOrder.nativeOrder());
    }
}
