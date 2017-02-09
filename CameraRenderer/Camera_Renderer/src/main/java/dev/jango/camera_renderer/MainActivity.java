package dev.jango.camera_renderer;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.ViewGroup;

public class MainActivity extends Activity {

    private Renderer renderer;
    private GLSurfaceView mCameraView = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        renderer = new Renderer();
        mCameraView = new GLSurfaceView(getApplicationContext());
        mCameraView.setEGLContextClientVersion(2);
        mCameraView.setPreserveEGLContextOnPause(true);
        mCameraView.setRenderer(renderer);
        addContentView(mCameraView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if(mCameraView !=null) mCameraView.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(mCameraView !=null) mCameraView.onResume();
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        if(event.getAction() == KeyEvent.ACTION_UP) {
            int keyCode = event.getKeyCode();

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_VOLUME_UP:
                    Renderer.CurrentRenderMode = Renderer.RenderMode.SurfaceTextureRender;
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    Renderer.CurrentRenderMode = Renderer.RenderMode.YUVConversionRender;
                    break;
                default:
            }
        }

        return super.dispatchKeyEvent(event);
    }
}
