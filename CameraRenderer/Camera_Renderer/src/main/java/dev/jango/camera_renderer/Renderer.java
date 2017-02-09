package dev.jango.camera_renderer;

import android.hardware.Camera;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Renderer implements GLSurfaceView.Renderer {

    public enum RenderMode{
        SurfaceTextureRender,
        YUVConversionRender
    }

    public static volatile RenderMode CurrentRenderMode = RenderMode.SurfaceTextureRender;
    private CameraController mCameraController;

    private CameraSurfaceTextureRenderer cameraSurfaceTextureRenderer;
    private CameraYUVRenderer cameraYUVRenderer;
    public Renderer() {
        mCameraController = new CameraController();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

    }

    public static int mWidth;
    public static int mHeight;
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        switch (CurrentRenderMode)
        {
            case SurfaceTextureRender:
                if(cameraSurfaceTextureRenderer == null) cameraSurfaceTextureRenderer = new CameraSurfaceTextureRenderer(mCameraController, previewCallback);
                cameraSurfaceTextureRenderer.draw();
                break;
            case YUVConversionRender:
                if(cameraYUVRenderer == null) cameraYUVRenderer = new CameraYUVRenderer(mCameraController, previewCallback);
                cameraYUVRenderer.draw();
                break;
        }
    }


    public Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if(Renderer.CurrentRenderMode == RenderMode.YUVConversionRender && cameraYUVRenderer != null) {
                cameraYUVRenderer.copyCameraFrameBuffer(data);
            }
            camera.addCallbackBuffer(data);
        }
    };
}
