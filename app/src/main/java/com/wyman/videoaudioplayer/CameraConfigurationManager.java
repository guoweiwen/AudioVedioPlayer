package com.wyman.videoaudioplayer;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by wyman
 * on 2018-08-19.
 */

public class CameraConfigurationManager {
    private static final String TAG = CameraConfigurationManager.class.getSimpleName();

    private Context context;
    private Point screenResolution;
    private Point cameraResolution;
    private int previewFormat;
    private String previewFormatString;
    //期望2.7倍焦距
    private static final int TEN_DESIRED_ZOOM = 27;
    private static final int DESIRED_SHARPNESS = 30;

    CameraConfigurationManager(Context context){
        this.context = context;
    }

    /**
     * 主要做了的工作：将摄像头分辨率置换
     * */
    void initCameraParameters(Camera camera){
        Camera.Parameters parameters = camera.getParameters();
        /*
        * 摄像头预览的格式
        *     摄像头格式（需要转换的格式）     返回格式（YUV420一个像素占1.5个字节）
        * PIXEL_FORMAT_YUV422SP      ImageFormat.NV16
        * PIXEL_FORMAT_YUV420SP      ImageFormat.NV21(录视频返回默认格式)
        * PIXEL_FORMAT_YUV422I       ImageFormat.YUY2
        * PIXEL_FORMAT_YUV420P       ImageFormat.YV12(录视频返回的第二种格式)
        * PIXEL_FORMAT_RGB565        ImageFormat.RGB_565（一个像素占两个字节）
        * PIXEL_FORMAT_JPEG          ImageFormat.JPEG
        * */
        previewFormat = parameters.getPreviewFormat();
        previewFormatString = parameters.get("preview-format");
        //打印摄像头格式
        Log.e(TAG, "Default preview format: " + previewFormat + '/' + previewFormatString);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        //屏幕分辨率存放在screenResolution
        display.getSize(screenResolution);
        Log.e(TAG, "Screen resolution: " + screenResolution);
        Point screenResolutionForCamera = new Point();
        screenResolutionForCamera.x = screenResolution.x;
        screenResolutionForCamera.y = screenResolution.y;
        //预览分辨率经常是横着的如：480 * 320，另外一些：320 * 480
        if(screenResolution.x < screenResolution.y){
            //将横的分辨率置换成竖的
            screenResolutionForCamera.x = screenResolution.y;
            screenResolutionForCamera.y = screenResolution.x;
        }
        //找最高分辨率那个
        cameraResolution = getCameraResolution(parameters,screenResolutionForCamera);
    }

    void setDesiredCamerParameters(Camera camer){
        Camera.Parameters parameters = camer.getParameters();
        Log.e(TAG,"Setting preview size: " + cameraResolution);
        //预览的分辨率为最大的摄像头分辨率
        parameters.setPreviewSize(cameraResolution.x,cameraResolution.y);
        //看情况设置setFlash() 和 setZoom(parameters)
        setFlash(parameters);
        setZoom(parameters);
        //设置摄像头角度
//        setDisplayOrientation(camer,90);
    }

    /**
     * 获取屏幕分辨率
     * */
    Point getScreenResolution(){
        return screenResolution;
    }

    /**
     * 获取摄像头最高分辨率
     * */
    Point getCameraResolution(){
        return cameraResolution;
    }

    /**
     * 预览画面的图像格式
     * */
    int getPreviewFormat(){
        return previewFormat;
    }

    String getPreviewFormatString(){
        return previewFormatString;
    }

    /**
     * 获取摄像头分辨率
     * */
    private Point getCameraResolution(Camera.Parameters parameters, Point screenResolutionForCamera) {
        String previewSizeValueString = parameters.get("preview-size-values");
        if(TextUtils.isEmpty(previewSizeValueString)){
            previewSizeValueString = parameters.get("preview-size-value");
        }
        Point cameraResolution = null;
        if(!TextUtils.isEmpty(previewSizeValueString)){
            Log.d(TAG, "preview-size-values parameter: " + previewSizeValueString);
            cameraResolution = findBestPreviewSizeValue(previewSizeValueString,screenResolutionForCamera);
        }
        if(cameraResolution == null){
            //找不到摄像头分辨率就用屏幕分辨率
            cameraResolution = new Point(
                    //确保相机的分辨率是8的倍数,可能有些不是
                    (screenResolution.x >> 3) << 3,
                    (screenResolution.y >> 3) << 3
                    //上面方法就是将最后的3位2进制去除掉 如
                    // 1000 8
                    // 1110 14 那么就将后面的110去掉   值变为8  1000

            );
        }
        return cameraResolution;
    }

    private static Point findBestPreviewSizeValue(String previewFormatString,Point screenResolution){
        int bestX = 0;
        int bestY = 0;
        int diff = Integer.MAX_VALUE;
        for(String previewSize : Pattern.compile(",").split(previewFormatString)){
            previewSize = previewSize.trim();
            int dimPosition = previewSize.indexOf('x');
            if(dimPosition < 0){
                Log.e(TAG, "Bad preview-size: " + previewSize);
                continue;
            }
            int newX,newY;
            try{
                newX = Integer.parseInt(previewSize.substring(0,dimPosition));
                newY = Integer.parseInt(previewSize.substring(dimPosition+1));
            } catch (NumberFormatException e){
                Log.e(TAG, "Bad preview-size: " + previewSize);
                continue;
            }
            //数组或list无须已经排序 该方法和Collects.min();方法一样效果
            //假设条件是：摄像头的最大分辨率 <= 屏幕分辨率
            int newDiff = Math.abs(newX - screenResolution.x) + Math.abs(newY - screenResolution.y);
            if(newDiff == 0){//屏幕分辨率 = 摄像头分辨率  的情况
                bestX = newX;
                bestY = newY;
                break;
            } else if(newDiff < diff){//选择最接近屏幕分辨率的作为最佳分辨率
                bestX = newX;
                bestY = newY;
                diff = newDiff;
            }
        }
        if(bestX > 0 && bestY > 0){
            return new Point(bestX,bestY);
        }
        return null;
    }

    /**
     * 设置闪光灯
     * */
    private void setFlash(Camera.Parameters parameters){
        //针对三星 Galaxy 硬编码将其闪光灯关闭
        // And this is a hack-hack to work around a different value on the Behold II
        // Restrict Behold II check to Cupcake, per Samsung's advice
        //CameraManager.SDK_INT = Build.VERSION_CODES.CUPCAKE
        if(Build.MODEL.contains("Behold II") && CameraManager.SDK_INT == 3){
            parameters.set("flash-value",1);
        } else {
            parameters.set("flash-value",2);
        }
        //关闭闪光灯 针对所有设备
        parameters.set("flash-mode","off");
    }

    private void setZoom(Camera.Parameters parameters){
        String zoomSupportedString = parameters.get("zoom-supported");
        if(!TextUtils.isEmpty(zoomSupportedString) && !Boolean.parseBoolean(zoomSupportedString)){
            return;//不支持
        }
        int tenDesiredZoom = TEN_DESIRED_ZOOM;
        String maxZoomString = parameters.get("max-zoom");
        if(maxZoomString!=null){
            try {
                int tenMaxZoom = (int)(10.0 * Double.parseDouble(maxZoomString));
                if(tenDesiredZoom > tenMaxZoom){
                    //如果自定义10倍放大大于设备10倍的，取设备的
                    tenDesiredZoom = tenMaxZoom;
                }
            } catch (NumberFormatException e){
                Log.w(TAG, "Bad max-zoom: " + maxZoomString);
            }
        }

        String takingPictureZoomMaxString = parameters.get("taking-picture-zoom-max");
        if(takingPictureZoomMaxString != null){
            try{
                int tenMaxZoom = Integer.parseInt(takingPictureZoomMaxString);
                if(tenDesiredZoom > tenMaxZoom){
                    tenDesiredZoom = tenMaxZoom;
                }
            } catch (Exception e){
                e.printStackTrace();
            }
            String motZoomValuesString = parameters.get("mot-zoom-values");
            if(motZoomValuesString != null){
                tenDesiredZoom = findBestMotZoomValue(motZoomValuesString,tenDesiredZoom);
            }

            String motZoomStepString = parameters.get("mot-zoom-step");
            if (motZoomStepString != null) {
                try {
                    double motZoomStep = Double.parseDouble(motZoomStepString.trim());
                    int tenZoomStep = (int) (10.0 * motZoomStep);
                    if (tenZoomStep > 1) {
                        tenDesiredZoom -= tenDesiredZoom % tenZoomStep;
                    }
                } catch (NumberFormatException nfe) {
                    // continue
                }
            }

            // Set zoom. This helps encourage the user to pull back.
            // Some devices like the Behold have a zoom parameter
            if (maxZoomString != null || motZoomValuesString != null) {
                parameters.set("zoom", String.valueOf(tenDesiredZoom / 10.0));
            }

            // Most devices, like the Hero, appear to expose this zoom parameter.
            // It takes on values like "27" which appears to mean 2.7x zoom
            if (takingPictureZoomMaxString != null) {
                parameters.set("taking-picture-zoom", tenDesiredZoom);
            }
        }
    }

    private static int findBestMotZoomValue(String motZoomValuesString, int tenDesiredZoom) {
        int tenBestValue = 0;
        for(String stringValue : Pattern.compile(",").split(motZoomValuesString)){
            stringValue = stringValue.trim();
            double value;
            try{
                value = Double.parseDouble(stringValue);
            } catch (Exception e){
                return tenDesiredZoom;
            }
            int tenValue = (int) (10.0 * value);
            //这个要求 遍历的值是已经排了序
            if(Math.abs(tenDesiredZoom - value) < Math.abs(tenDesiredZoom - tenBestValue)){
                //取最接近 tenDesiredZoom值的那个为tenBestValue
                tenBestValue = tenValue;
            }
        }
        return tenBestValue;
    }


    /**
     * 设置摄像头角度
     * 适配到1.6
     * */
    protected void setDisplayOrientation(Camera camera,int angle){
        //Polymorghic 聚合
        try {
            //反射：通过调用的类获取类对象 --> 获取Method对象  --> 参数 方法名和方法参数类型 new Class[]{参数类型}
            //        method.invoke(调用对象，实际参数);
            Method downPolymorghic = camera.getClass().getMethod("setDisplayOrientation", new Class[]{int.class});
            if(downPolymorghic != null){
                downPolymorghic.invoke(camera,new Object[]{angle});
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void setCameraOtherParameters(Camera camera){
        Camera.Parameters parameters = camera.getParameters();
        List<String> focusModes = parameters.getSupportedFocusModes();
        if(focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)){
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        } else {
            Log.e(TAG, "Camera does not support autofocus");
        }
        // let's try fastest frame rate. You will get near 60fps, but your device become hot.
        final List<int[]> supportedFpsRange = parameters.getSupportedPreviewFpsRange();
        /*final int supportFpsNum = supportedFpsRange != null ? supportedFpsRange.size() : 0;
        int[] range;
        for(int i=0;i<supportFpsNum;i++){
            range = supportedFpsRange.get(i);
            Log.e(TAG, String.format("supportedFpsRange(%d)=(%d,%d)", i, range[0], range[1]));
        }*/
        final int[] max_fps = supportedFpsRange.get(supportedFpsRange.size() - 1);
        Log.e(TAG, String.format("fps:%d-%d", max_fps[0], max_fps[1]));
        parameters.setPreviewFpsRange(max_fps[0],max_fps[1]);
        //告诉摄像头该应用是录像的
        parameters.setRecordingHint(true);

    }

    public void setPreviewSize(Camera camera,int width,int height){
        Camera.Parameters parameters = camera.getParameters();
        final Camera.Size closestSize = getClosestSupportedSize(parameters.getSupportedPreviewSizes(),width,height);
        parameters.setPreviewSize(closestSize.width,closestSize.height);
    }

    public void setPictureSize(Camera camera,int width,int height){
        Camera.Parameters parameters = camera.getParameters();
        final Camera.Size closestSize = getClosestSupportedSize(parameters.getSupportedPictureSizes(),width,height);
        parameters.setPictureSize(closestSize.width,closestSize.height);
    }

    /**
     * 设置摄像头角度
     * rotate preview screen according to the device orientation
     * 根据设备屏幕的旋转改变摄像头的角度
     * */
    public void setCameraRotation(Camera camera,Context context){
        Camera.Parameters parameters = camera.getParameters();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        final int rotation = windowManager.getDefaultDisplay().getRotation();
        int degree = 0;//设备的角度
        switch (rotation){
            case 0 : degree = 0;break;
            case 90 : degree = 90;break;
            case 180 : degree = 180;break;
            case 270 : degree = 270;break;
        }
        Log.e(TAG,"设备旋转的度数：" + degree);
        //判断摄像头是前置还是后置摄像头
        final Camera.CameraInfo info = new Camera.CameraInfo();
        //0 为首个摄像头  如果有两个摄像头需要设置0这个参数  通常 0 为后置，1为前置
        Camera.getCameraInfo(0,info);
        boolean isFrontFace = info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
        if(isFrontFace){//前置摄像头
            degree = (info.orientation + degree) % 360;
            degree = (360 - degree) % 360;// 反转
        } else {//后置摄像头
            degree = (info.orientation - degree + 360) % 360;
        }
        Log.e(TAG,"摄像头旋转的度数：" + info.orientation);
        Log.e(TAG,"摄像头最终应该旋转的度数：" + degree);
        try {
            Method method = camera.getClass().getMethod("setDisplayOrientation", new Class[]{int.class});
            if(method != null){
                method.invoke(camera,new Object[]{degree});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 该方法设置完所有方法后再调用
     * 最终将参数设置在Camera上
     * */
    public void setFinalCameraParameters(Camera camera){
        Camera.Parameters parameters = camera.getParameters();
        camera.setParameters(parameters);
    }

    /**
     * 获取最接近提供的分辨率
     * */
    private Camera.Size getClosestSupportedSize(List<Camera.Size> supportedPreviewSizes, final int requestWidth, final int requestHeight) {
        return Collections.min(supportedPreviewSizes, new Comparator<Camera.Size>() {

            private int diff(final Camera.Size size){
                return Math.abs(requestWidth - size.width) + Math.abs(requestHeight - size.height);
            }

            @Override
            public int compare(Camera.Size o1, Camera.Size o2) {
                return diff(o1) - diff(o2);
            }
        });
    }
}

















