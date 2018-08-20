package com.wyman.videoaudioplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by wyman
 * on 2018-08-20.
 */
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback{
    private static final String TAG = CameraSurfaceView.class.getSimpleName();
    private SurfaceHolder surfaceHolder;
    private CameraManager cameraManager;

    public CameraSurfaceView(Context context) {
        this(context,null);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        cameraManager = CameraManager.getCameraInstance(getContext());
        try {
            cameraManager.openCamera(surfaceHolder);
            cameraManager.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    /**
     * Activity生命周期onPause()就会被调用此surfaceDestroyed()
     * */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceHolder.removeCallback(null);
        cameraManager.stopPreview();
    }
}
