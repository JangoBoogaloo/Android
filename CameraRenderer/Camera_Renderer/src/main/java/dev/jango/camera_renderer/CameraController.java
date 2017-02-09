package dev.jango.camera_renderer;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

public class CameraController {
    private static final String TAG = CameraController.class.getName();

    private Camera mCamera;

    public CameraController() {}

    public void OpenCamera(SurfaceTexture texture) {
        mCamera = this.openCamera();
        if(mCamera == null) {
            Log.e(TAG, "Camera NULL");
        }

        try {
            mCamera.setPreviewTexture(texture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startCameraPreview()
    {
        mCamera.startPreview();
    }

    private Camera openCamera() {
        int cameraId = -1;
        Vector<Camera.CameraInfo> cameraVector = getCameraVector();

        if (cameraVector.isEmpty()) {
            Log.e(TAG, "No camera found.");
            return null;
        }

        for (int i = 0; i < cameraVector.size(); i++) {
            if (Camera.CameraInfo.CAMERA_FACING_BACK == cameraVector.get(i).facing) {
                return Camera.open(i);
            }
        }
        return null;
    }

    private static Vector<Camera.CameraInfo> getCameraVector() {
        int numberOfCameras = Camera.getNumberOfCameras();

        Vector<Camera.CameraInfo> cameraVector = new Vector<Camera.CameraInfo>();

        for (int i = 0; i < numberOfCameras; i++)
        {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);
            cameraVector.add(cameraInfo);
        }
        return cameraVector;
    }

    public int previewWidth = 0;
    public int previewHeight = 0;
    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        Camera.Parameters parameters = mCamera.getParameters();
        Camera.Size optimalPreviewSize = getOptimalPreviewSize(parameters.getSupportedPreviewSizes(), Renderer.mWidth, Renderer.mHeight);
        previewWidth = optimalPreviewSize.width;
        previewHeight = optimalPreviewSize.height;
        parameters.setPreviewSize(previewWidth, previewHeight);
        parameters.setPreviewSize(previewWidth, previewHeight);
        switch (parameters.getPreviewFormat())
        {
            case ImageFormat.NV21:
                Log.d(TAG, "Image Format is NV21");
                break;
            default:
                Log.e(TAG, "Image Format is not NV21");
                break;
        }
        int frameSize = (int) ((previewWidth * previewHeight * (ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()))) / 8.0);
        mCamera.addCallbackBuffer(new byte[frameSize]);
        mCamera.setPreviewCallbackWithBuffer(previewCallback);
        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

    private static Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int targetWidth, int targetHeight)
    {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) targetWidth / targetHeight;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
}
