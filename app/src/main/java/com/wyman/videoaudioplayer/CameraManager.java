package com.wyman.videoaudioplayer;

import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.Objects;

/**
 * Created by wyman
 * on 2018-08-19.
 *
 */
public class CameraManager implements Camera.PreviewCallback{
    private static final String TAG = CameraManager.class.getSimpleName();
    private final Context context;
    private Camera camera;
    private final CameraConfigurationManager configManager;

    //单例
    private static CameraManager CAMERA_INSTANCE;
    private boolean previewing;

    public static CameraManager getCameraInstance(Context context){
        if(CAMERA_INSTANCE == null){
            synchronized (CameraManager.class){
                if(CAMERA_INSTANCE == null){
                    CAMERA_INSTANCE = new CameraManager(context);
                }
            }
        }
        return CAMERA_INSTANCE;
    }

    private CameraManager(Context context){
        this.context = context;
        this.configManager = new CameraConfigurationManager(context);
    }

    static final int SDK_INT;
    static {
        int sdk_int;
        sdk_int = Integer.parseInt(Build.VERSION.SDK);//获取当前设备Android版本
        SDK_INT = sdk_int;
    }

    /**
     * 打开摄像头
     * 如果摄像头空就会抛出异常
     * */
    public void openCamera(SurfaceHolder surfaceHolder) throws IOException {
        if(camera == null){
            camera = Camera.open();
            Objects.requireNonNull(camera,"Camera is null");
        }
        camera.setPreviewDisplay(surfaceHolder);
        //设置 Camera.Parameters
        configManager.initCameraParameters(camera);
        configManager.setDesiredCamerParameters(camera);
        camera.setPreviewCallback(this);
    }

    /**
     * 释放摄像头
     * */
    public void closeCamera(){
        if(camera != null){
            camera.release();
            camera = null;
        }
    }

    /**
     * 关闭预览摄像头
     * */
    public void stopPreview(){
        if(camera!=null && previewing){
            camera.stopPreview();
            previewing = false;
        }
    }


    public void startPreview(){
        if(camera!=null && !previewing){
            previewing = true;
            camera.startPreview();
        }
    }

    /**
     * 摄像头预览帧数返回接口
     * */
    private PreviewFrameCallback callback;
    public void setPreviewCallback(PreviewFrameCallback callback){
        this.callback = callback;
    }
    interface PreviewFrameCallback{
        void onPreviewFrame(byte[] data, Camera camera);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if(callback != null){
            callback.onPreviewFrame(data,camera);
        }
    }
}















