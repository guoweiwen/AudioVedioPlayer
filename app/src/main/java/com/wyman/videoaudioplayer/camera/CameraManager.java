package com.wyman.videoaudioplayer.camera;

import android.hardware.Camera;
import android.opengl.GLSurfaceView;

import com.wyman.videoaudioplayer.filter.GPUFilter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
/**
 * 管理摄像头类
 */
public class CameraManager {
    private CameraWraper mCamera;
    private int mCameraId = -1;
    private GLSurfaceView glSurfaceView;
    private static CameraManager manager;
//    private boolean preViewRuning;

    private GLRender mRender;

    public static CameraManager getManager(){
        if(manager==null){
            manager = new CameraManager();
        }
        return manager;
    }

    /**
     * 初始化摄像头的信息
     * */
    public void init(){
        if(manager != null){
            //默认打开后置摄像头
            int cameraType = Camera.CameraInfo.CAMERA_FACING_BACK;
            manager.openCamera(cameraType);
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            Camera.Size preViewSize = CameraManager.getClosestSupportedSize(sizes,1280,720);
            if(parameters.getSupportedFocusModes().contains(FOCUS_MODE_CONTINUOUS_VIDEO)){
                parameters.setFocusMode(FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            parameters.setPreviewSize(preViewSize.width,preViewSize.height);

            parameters.setPreviewFrameRate(25);
            parameters.setRecordingHint(true);

            mCamera.setParameters(parameters);
            mCamera.setDisplayOrientation(90);
        }
    }

    public void CameraManger(){}

    public CameraWraper openCamera(int facingTpe){
        int cameraCount = Camera.getNumberOfCameras();
        for(int i=0;i<cameraCount;i++){
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i,cameraInfo);
            if(cameraInfo.facing == facingTpe){
                mCamera = CameraWraper.open(i);
                mCameraId = i;
                break;
            }
        }
        if(mRender!=null){
            mRender.setmCamera(mCamera);
        }
        return mCamera;
    }
    public void setGlSurfaceView(GLSurfaceView glSurfaceView) {
        //关联 GLSurfaceView
        this.glSurfaceView = glSurfaceView;
        this.glSurfaceView.setEGLContextClientVersion(2);
        mRender = new GLRender(mCamera,glSurfaceView);
        this.glSurfaceView.setRenderer(mRender);
    }

    public void onPause(){
        mCamera.stopPreview();
//        glSurfaceView.onPause();
    }

    public void onResume(){
        mCamera.startPreview();
//        glSurfaceView.onResume();

    }
    public void setFilter(GPUFilter filter){
        mRender.setmFilter(filter);
    }

    public void onDestory(){
        mCamera.startPreview();

        mRender.release();

        mRender = null;

        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    Thread.sleep(500);//休眠0.5秒
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                releaseCamera();
                glSurfaceView.onPause();
            }
        }.start();
    }
    public void releaseCamera(){
        if(mCamera == null) return;
        if(mCamera.isPreViewing){
            mCamera.stopPreview();
        }
        mCamera.release();
        mCamera = null;

    }
    public static Camera.Size getClosestSupportedSize(List<Camera.Size> supportedSizes, final int requestedWidth, final int requestedHeight) {
        return (Camera.Size) Collections.min(supportedSizes, new Comparator<Camera.Size>() {

            private int diff(final Camera.Size size) {
                return Math.abs(requestedWidth - size.width) + Math.abs(requestedHeight - size.height);
            }

            @Override
            public int compare(final Camera.Size lhs, final Camera.Size rhs) {
                return diff(lhs) - diff(rhs);
            }
        });

    }
}
