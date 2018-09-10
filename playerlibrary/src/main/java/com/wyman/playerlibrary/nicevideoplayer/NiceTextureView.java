package com.wyman.playerlibrary.nicevideoplayer;

import android.content.Context;
import android.view.TextureView;

/**
 * Created by wyman
 * on 2018-09-07.
 * * 重写TextureView，适配视频的宽高和旋转.
 * （参考自节操播放器 https://github.com/lipangit/JieCaoVideoPlayer）
 * 问题：重新布局时，视频会重置吗？
 * 屏幕旋转后，宽高本身是不会置换的
 */
public class NiceTextureView extends TextureView{

    //视频的高度
    private int videoHeight;
    //视频的宽度
    private int videoWidth;

    public NiceTextureView(Context context) {
        super(context);
    }

    /**
     * 适应视频宽高
     * */
    public void adaptVideoSize(int videoWidth,int videoHeight){
        if(this.videoWidth != videoWidth && this.videoHeight != videoHeight){
            //设置视频宽高
            this.videoWidth = videoWidth;
            this.videoHeight = videoHeight;
            //要求重新布局
            requestLayout();
        }
    }

    /**
     * 屏幕旋转，View 的宽高是不会变化的
     * */
    @Override
    public void setRotation(float rotation) {
        //如果当前角度非需要设置的角度
        if(rotation != getRotation()){
            super.setRotation(rotation);
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float viewRotation = getRotation();
        //角度颠倒了，宽高置换
        if(viewRotation == 90f || viewRotation == 270){
            int temp = widthMeasureSpec;
            widthMeasureSpec = heightMeasureSpec;
            heightMeasureSpec = temp;
        }
        int width = getDefaultSize(videoWidth,widthMeasureSpec);
        int height = getDefaultSize(videoHeight,widthMeasureSpec);
        if(videoWidth > 0 && videoHeight > 0){
            int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
            int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
            //目的都是根据视频的 宽高比 调整View的 宽高比 等于视频的 宽高比 即 width/height = videoWidth / videoHeight
            if(widthSpecMode == MeasureSpec.EXACTLY
                    && heightSpecMode == MeasureSpec.EXACTLY){
                width = widthSpecSize;
                height = heightSpecSize;
                if(videoWidth * height < width * videoHeight){
                    /*
                    * 即：width/height > videoWidth/videoHeight
                    * 说明 View的宽高比大于视频宽高比；View的宽度过大
                    * 下面将 宽度 调整为 视频宽高比例，在高不变的情况下
                    * */
                    width = height * videoWidth / videoHeight;
                } else if(videoWidth * height > width * videoHeight){
                    /*
                    * 即 width/height < videoWidth/videoHeight
                    *   说明 View的宽高比小于于视频宽高比；View的高度过大
                    *   下面将 高度 调整为 视频宽高比例，在宽度不变的情况下
                    * */
                    height = width * videoHeight / videoWidth;
                    //其实等同于 height = width / videoWidth / videoHeight  除以一个数等于乘以它的倒数
                }
            } else if (widthSpecMode == MeasureSpec.EXACTLY) {
                // only the width is fixed, adjust the height to match aspect ratio if possible
                //只有宽度固定，尽可能调整高度适应 视频宽高比
                width = widthSpecSize;
                height = width * videoHeight / videoWidth;
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    //如果调整后高度大于 系统给定的高度 就调整宽度 使View的宽高比与 视频宽高比一致
                    // couldn't match aspect ratio within the constraints
                    height = heightSpecSize;
                    width = height * videoWidth / videoHeight;
                }
            } else if (heightSpecMode == MeasureSpec.EXACTLY) {
                // only the height is fixed, adjust the width to match aspect ratio if possible
                height = heightSpecSize;
                width = height * videoWidth / videoHeight;
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    width = widthSpecSize;
                    height = width * videoHeight / videoWidth;
                }
            } else {
                // neither the width nor the height are fixed, try to use actual video size
                width = videoWidth;
                height = videoHeight;
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // too tall, decrease both width and height
                    height = heightSpecSize;
                    width = height * videoWidth / videoHeight;
                }
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // too wide, decrease both width and height
                    width = widthSpecSize;
                    height = width * videoHeight / videoWidth;
                }
            }
        } else {
            // no size yet, just adopt the given spec sizes
        }
        setMeasuredDimension(width,height);
    }
}
























