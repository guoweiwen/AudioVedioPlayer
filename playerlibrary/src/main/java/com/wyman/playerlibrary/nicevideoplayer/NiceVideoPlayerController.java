package com.wyman.playerlibrary.nicevideoplayer;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by wyman
 * on 2018-09-07.
 * 视频控制器抽象类
 * 就继承FrameLayout 其他 View由子类实现
 */
public abstract class NiceVideoPlayerController extends FrameLayout implements View.OnTouchListener{

    private Context mContext;
    protected INiceVideoPlayer mNiceVideoPlayer;
    private Timer mUpdateProgressTimer;
    private TimerTask mUpdateProgressTimerTask;
    private float mDownX;
    private float mDownY;
    private boolean mNeedChangePosition;
    private boolean mNeedChangeVolume;
    private boolean mNeedChangeBrightness;
    //滑动的临界点
    private static final int THRESHOLD = 80;
    private long mGestureDownPosition;
    private float mGestureDownBrightness;
    private int mGestureDownVolume;
    private long mNewPosition;

    public NiceVideoPlayerController(@NonNull Context context) {
        super(context);
        mContext = context;
        this.setOnTouchListener(this);
    }

    public void setNiceVideoPlayer(INiceVideoPlayer niceVideoPlayer){
        mNiceVideoPlayer = niceVideoPlayer;
    }

    /**
     * 设置播放的视频的标题
     *
     * @param title 视频标题
     */
    public abstract void setTitle(String title);

    /**
     * 视频底图
     *
     * @param resId 视频底图资源
     */
    public abstract void setImage(@DrawableRes int resId);

    /**
     * 视频底图ImageView控件，提供给外部用图片加载工具来加载网络图片
     *
     * @return 底图ImageView
     */
    public abstract ImageView imageView();

    /**
     * 设置总时长
     * */
    public abstract void setLength(long length);

    /**
     * 当播放器的播放状态发生变化，在此方法中你更新不同的播放状态的UI
     *
     * @param playState 播放状态：
     *                  <ul>
     *                  <li>{@link NiceVideoPlayer#STATE_IDLE}</li>
     *                  <li>{@link NiceVideoPlayer#STATE_PREPARING}</li>
     *                  <li>{@link NiceVideoPlayer#STATE_PREPARED}</li>
     *                  <li>{@link NiceVideoPlayer#STATE_PLAYING}</li>
     *                  <li>{@link NiceVideoPlayer#STATE_PAUSED}</li>
     *                  <li>{@link NiceVideoPlayer#STATE_BUFFERING_PLAYING}</li>
     *                  <li>{@link NiceVideoPlayer#STATE_BUFFERING_PAUSED}</li>
     *                  <li>{@link NiceVideoPlayer#STATE_ERROR}</li>
     *                  <li>{@link NiceVideoPlayer#STATE_COMPLETED}</li>
     *                  </ul>
     */
    protected abstract void onPlayStateChanged(int playState);

    /**
     * 当播放器的播放模式发生变化，在此方法中更新不同模式下的控制器界面。
     *
     * @param playMode 播放器的模式：
     *                 <ul>
     *                 <li>{@link NiceVideoPlayer#MODE_NORMAL}</li>
     *                 <li>{@link NiceVideoPlayer#MODE_FULL_SCREEN}</li>
     *                 <li>{@link NiceVideoPlayer#MODE_TINY_WINDOW}</li>
     *                 </ul>
     */
    protected abstract void onPlayModeChanged(int playMode);

    /**
     * 重置控制器，将控制器恢复到初始状态。
     * */
    protected abstract void reset();

    protected void startUpdateProgressTimer(){
        cancelUpdateProgressTimer();
        if(mUpdateProgressTimer == null){
            mUpdateProgressTimer = new Timer();
        }
        if(mUpdateProgressTimerTask == null){
            mUpdateProgressTimerTask = new TimerTask() {
                @Override
                public void run() {
                    updateProgress();
                }
            };
        }
        //相隔800毫秒执行一次run
        mUpdateProgressTimer.schedule(mUpdateProgressTimerTask,0,800);
    }

    protected void cancelUpdateProgressTimer(){
        if(mUpdateProgressTimer != null){
            mUpdateProgressTimer.cancel();
            mUpdateProgressTimer = null;
        }
        if(mUpdateProgressTimerTask != null){
            mUpdateProgressTimerTask.cancel();
            mUpdateProgressTimerTask = null;
        }
    }

    /**
     * 更新进度，包括进度条进度，展示的当前播放位置时长，总时长等
     * */
    protected abstract void updateProgress();

    /**
     * onTouch 与 onTouchEvent 关系
     * */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // 只有全屏的时候才能拖动位置、亮度、声音
        if(!mNiceVideoPlayer.isFullScreen()){
            return false;
        }

        if(mNiceVideoPlayer.isIdle()
                || mNiceVideoPlayer.isError()
                || mNiceVideoPlayer.isPreparing()
                || mNiceVideoPlayer.isPrepared()
                || mNiceVideoPlayer.isCompleted()){
            hideChangePosition();
            hideChangeBrightness();
            hideChangeVolume();
            return false;
        }
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                mDownX = x;
                mDownY = y;
                mNeedChangePosition = false;
                mNeedChangeVolume = false;
                mNeedChangeBrightness = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = x - mDownX;
                float deltaY = y - mDownY;
                float absDeltaX = Math.abs(deltaX);
                float absDeltaY = Math.abs(deltaY);
                /*
                * if(absDeltaX > THRESHOLD){//左右滑动
                *
                * } else if(absDeltaY >= THRESHOLD){//上下滑动
                *
                *  }
                *  这段代码，即使是斜滑，只要是 absDeltaX > THRESHOLD 就确定是左右滑动，忽略上下滑动
                *  只有当 absDeltaX <= THRESHOLD 且 absDeltaY >= THRESHOLD 就确定上下滑动
                *  不用将斜滑的情况细分,斜滑统一看成 左右滑动
                * */
                if(!mNeedChangePosition && !mNeedChangeVolume && !mNeedChangeBrightness){
                    // 只有在播放、暂停、缓冲的时候能够拖动改变位置、亮度和声音
                    if(absDeltaX > THRESHOLD){//左右滑动
                        cancelUpdateProgressTimer();
                        mNeedChangePosition = true;
                        mGestureDownPosition = mNiceVideoPlayer.getCurrentPosition();
                        changeVedioPosition(mNeedChangePosition,mGestureDownPosition,deltaX);
                    } else if(absDeltaY >= THRESHOLD){//上下滑动
                        if(mDownX < getWidth() * 0.5f){
                            //左侧改变亮度
                            mNeedChangeBrightness = true;
                            mGestureDownBrightness = ((Activity)mContext).getWindow().getAttributes().screenBrightness;
                            changeBrightness(mNeedChangeBrightness,mGestureDownBrightness,deltaY);
                        } else {
                            // 右侧改变声音
                            mNeedChangeVolume = true;
                            mGestureDownVolume = mNiceVideoPlayer.getVolume();
                            changeVolume(mNeedChangeVolume,mGestureDownVolume,deltaY);
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if(mNeedChangePosition){
                    mNiceVideoPlayer.seekTo(mNewPosition);
                    hideChangePosition();
                    startUpdateProgressTimer();
                    return true;
                }
                if(mNeedChangeBrightness){
                    hideChangeBrightness();
                    return true;
                }
                if(mNeedChangeVolume){
                    hideChangeVolume();
                    return true;
                }
                break;
        }
        return false;
    }

    /**
     * 手势在左侧上下滑动改变音量后，手势up或者cancel时，隐藏控制器中间的音量变化视图，
     * 在手势ACTION_UP或ACTION_CANCEL时调用。
     */
    protected abstract void hideChangeVolume();

    /**
     * 手势在左侧上下滑动改变亮度后，手势up或者cancel时，隐藏控制器中间的亮度变化视图，
     * 在手势ACTION_UP或ACTION_CANCEL时调用。
     */
    protected abstract void hideChangeBrightness();

    /**
     * 手势左右滑动改变播放位置后，手势up或者cancel时，隐藏控制器中间的播放位置变化视图，
     * 在手势ACTION_UP或ACTION_CANCEL时调用。
     */
    protected abstract void hideChangePosition();

    /**
     * @param mGestureDownPosition 为当前视频时间
     * */
    private void changeVedioPosition(boolean mNeedChangePosition, long mGestureDownPosition, float deltaX) {
        if(mNeedChangePosition){
            long duration = mNiceVideoPlayer.getDuration();//总时间
            //deltaX 不用添加负号因为方向与视频轨道一致 正数为快进，负数为后退
            //duration * deltaX / getWidth() 为快进或者后退总时间的百分比
            long toPosition = (long) (mGestureDownPosition + duration * deltaX / getWidth());
            mNewPosition = Math.max(0,Math.min(toPosition,duration));
            int newPositionProgress = (int)(100f * mNewPosition / duration);
            //这里只是更改滑动条UI；更改播放视频在手指抬起时，即 ACTION_UP
            showChangePosition(duration,newPositionProgress);
        }
    }

    /**
     * 手势左右滑动改变播放位置时，显示控制器中间的播放位置变化视图，
     * 在手势滑动ACTION_MOVE的过程中，会不断调用此方法。
     *
     * @param duration            视频总时长ms
     * @param newPositionProgress 新的位置进度，取值0到100。
     */
    protected abstract void showChangePosition(long duration, int newPositionProgress);

    private void changeVolume(boolean mNeedChangeVolume, int mGestureDownVolume,float deltaY) {
        if(mNeedChangeVolume){
            int maxVolume = mNiceVideoPlayer.getMaxVolume();
            int deltaVolume = (int)(maxVolume + -deltaY * 3 / getHeight());
            int newVolume = mGestureDownVolume + deltaVolume;
            // 0 < newVolume < maxVolume
            newVolume = Math.max(0,Math.min(maxVolume,newVolume));
            mNiceVideoPlayer.setVolumn(newVolume);
            int newVolumeProgress = (int)(100f * newVolume / maxVolume);
            showChangeVolume(newVolumeProgress);
        }
    }

    /**
     * 手势在右侧上下滑动改变音量时，显示控制器中间的音量变化视图，
     * 在手势滑动ACTION_MOVE的过程中，会不断调用此方法。
     *
     * @param newVolumeProgress 新的音量进度，取值1到100。
     */
    protected abstract void showChangeVolume(int newVolumeProgress);

    private void changeBrightness(boolean mNeedChangeBrightness, float mGestureDownBrightness,float deltaY) {
        if(mNeedChangeBrightness){
            //增加音量，向上滑动，值为负，所以前面加负号，使其变为正数；减少音量同理，3分之一高度因为 Brightness是0-1之间 除以3能精细敏感度
            float deltaBrightness = -deltaY * 3 / getHeight();
            float newBrightness = mGestureDownBrightness + deltaBrightness;
            //这样意思是 0 < newBrightness < 1 不用if语句
            newBrightness = Math.max(0,Math.min(newBrightness,1));
            float newBrightnessPercentage = newBrightness;
            WindowManager.LayoutParams params = ((Activity)mContext).getWindow().getAttributes();
            params.screenBrightness = newBrightnessPercentage;
            ((Activity)mContext).getWindow().setAttributes(params);
            int newBrightnessProgress = (int)(100f * newBrightnessPercentage);
            showChangeBrightness(newBrightnessProgress);
        }
    }

    /**
     * 手势在左侧上下滑动改变亮度时，显示控制器中间的亮度变化视图，
     * 在手势滑动ACTION_MOVE的过程中，会不断调用此方法。
     *
     * @param newBrightnessProgress 新的亮度进度，取值1到100。
     */
    protected abstract void showChangeBrightness(int newBrightnessProgress);
}





















