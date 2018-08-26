package com.wyman.playerlibrary;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatSeekBar;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by wyman
 * on 2018-08-26.
 */
public class MediaPlayerWrapperView extends FrameLayout implements SurfaceHolder.Callback{
    private static final String TAG = MediaPlayerWrapper.class.getSimpleName();

    private int mCurrentState = STATE_IDLE;
    /*播放的状态*/
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    private MediaPlayerInterface mediaPlayerInterface;
    private MediaPlayerWrapper mediaPlayerWrapper;

    private ImageButton changeorientation_button,playorstop_button,lock_button;
    private RelativeLayout control_linearlayout;
    private TextView displaystarttime_textview,displaytotaltime_textview;
    private RelativeLayout mediaplayer_linearlayout;
    private AppCompatSeekBar playerlength_seekbar;
    private SurfaceViewWrapper surfaceviewwrapper_video;
    private SurfaceHolder surfaceHolder;
    private String path;

    public MediaPlayerWrapperView(@NonNull Context context) {
        this(context,null);
    }

    public MediaPlayerWrapperView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public MediaPlayerWrapperView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        View.inflate(context,R.layout.mediaplayerwrapper_layout,this);
        initUI();
    }

    /**
     * 初始化View
     * */
    private void initUI() {
        changeorientation_button = findViewById(R.id.changeorientation_button);
        playorstop_button = findViewById(R.id.playorstop_button);
        lock_button = findViewById(R.id.lock_button);
        control_linearlayout = findViewById(R.id.control_linearlayout);
        displaystarttime_textview = findViewById(R.id.displaystarttime_textview);
        displaytotaltime_textview = findViewById(R.id.displaytotaltime_textview);
        mediaplayer_linearlayout = findViewById(R.id.mediaplayer_linearlayout);
        playerlength_seekbar = findViewById(R.id.playerlength_seekbar);
        surfaceviewwrapper_video = findViewById(R.id.surfaceviewwrapper_video);
        Log.e(TAG,"surfaceHolder:" + surfaceHolder);
        surfaceviewwrapper_video.getHolder().addCallback(this);
        surfaceviewwrapper_video.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setDataSource(String path){
        this.path = path;
        openVideo();
    }

    private void openVideo(){
        if(TextUtils.isEmpty(path))return;
        if(surfaceHolder == null)return;
        try{
            if(mediaPlayerInterface!=null){
                mediaPlayerInterface.setDataSource(path);
                if(mediaPlayerInterface instanceof MediaPlayerWrapper){
                    mediaPlayerInterface.setDisplay(surfaceHolder);
                }
                mediaPlayerInterface.prepare();
                mCurrentState = STATE_PREPARED;
            }
        } catch (Exception e){
            e.printStackTrace();
            mCurrentState = STATE_ERROR;
        }
    }

    public int getCurrentState(){
        return mCurrentState;
    }

    public boolean isPlaying() {
        return mediaPlayerInterface.isPlaying();
    }

    public void start() {
        if (mediaPlayerInterface != null) {
            if(mediaPlayerInterface instanceof MediaPlayerWrapper){
                mediaPlayerWrapper = (MediaPlayerWrapper) mediaPlayerInterface;
                mediaPlayerWrapper.getMediaPlayer().setOnPreparedListener(mPreparedListener);
            }
        }
    }

    public void pause() {
        if (mediaPlayerInterface != null) {
            if (mediaPlayerInterface.isPlaying()) {
                mediaPlayerInterface.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
    }

    public void stopPlayback() {
        if (mediaPlayerInterface != null) {
            mediaPlayerInterface.stop();
            mCurrentState = STATE_IDLE;
        }
    }

    public void release() {
        if (mediaPlayerInterface != null) {
            mediaPlayerInterface.release();
            mediaPlayerInterface = null;
            mCurrentState = STATE_IDLE;
        }
    }

    /**
     * 创建以后需要调用此方法
     * */
    public void setMediaPlayerInterface(MediaPlayerInterface mediaPlayerInterface){
        this.mediaPlayerInterface = mediaPlayerInterface;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(TAG,"surfaceCreated");
        surfaceHolder = holder;
        openVideo();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e(TAG,"surfaceDestroyed");
        surfaceHolder.removeCallback(null);
        release();
    }

    /**
     * 当MediaPlayer 调用prepare()时，这个有个过程，如果立即调用start()就会播放不了，所以需要添加个监听
     * */
    MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            mCurrentState = STATE_PREPARED;
            mediaPlayerInterface.start();
            mCurrentState = STATE_PLAYING;
        }
    };
}
