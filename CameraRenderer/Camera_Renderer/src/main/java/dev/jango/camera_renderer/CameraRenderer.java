package dev.jango.camera_renderer;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

public class CameraRenderer implements SurfaceTexture.OnFrameAvailableListener {
    protected static SurfaceTexture mSurfaceTexture = null;
    protected static final int[] mTextureId = new int[1];

    public CameraRenderer(CameraController controller, Camera.PreviewCallback callback)
    {
        if(controller == null) throw new IllegalArgumentException("Arguments can not be null");
        if(mSurfaceTexture == null) {
            GLES20.glGenTextures(1, mTextureId, 0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId[0]);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            mSurfaceTexture = new SurfaceTexture(mTextureId[0]);
            mSurfaceTexture.setOnFrameAvailableListener(this);
            controller.OpenCamera(mSurfaceTexture);
            controller.setPreviewCallback(callback);
            controller.startCameraPreview();
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {

    }
}
