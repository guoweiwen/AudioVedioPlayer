package com.wyman.playerlibrary.nicevideoplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.CountDownTimer;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.wyman.playerlibrary.R;

import org.w3c.dom.Text;

import java.util.List;

/**
 * Created by wyman
 * on 2018-09-08.
 * 视频播放控制，NiceVideoPlayerController子类
 */
public class TxVideoPlayerController extends NiceVideoPlayerController
    implements View.OnClickListener,SeekBar.OnSeekBarChangeListener{

    private Context mContext;
    private ImageView mImage;
    private ImageView mCenterStart;

    private LinearLayout mTop;
    private ImageView mBack;
    private TextView mTitle;
    private LinearLayout mBatteryTime;
    private ImageView mBattery;
    private TextView mTime;

    private LinearLayout mBottom;
    private ImageView mRestartPause;
    private TextView mPosition;
    private TextView mDuration;
    private SeekBar mSeek;
    private TextView mClarity;
    private ImageView mFullScreen;

    private TextView mLength;

    private LinearLayout mLoading;
    private TextView mLoadText;

    private LinearLayout mChangePosition;
    private TextView mChangePositionCurrent;
    private ProgressBar mChangePositionProgress;

    private LinearLayout mChangeBrightness;
    private ProgressBar mChangeBrightnessProgress;

    private LinearLayout mChangeVolume;
    private ProgressBar mChangeVolumeProgress;

    private LinearLayout mError;
    private TextView mRetry;

    private LinearLayout mCompleted;
    private TextView mReplay;
    private TextView mShare;

    private boolean topBottomVisible;
    private CountDownTimer mDismissTopBottomCountDownTimer;

    private List<Clarity> clarities;
    private int defaultClarityIndex;

    private ChangeClarityDialog mClarityDialog;

    private boolean hasRegisterBatteryReceiver;//是否已经注册了电池广播

    public TxVideoPlayerController(@NonNull Context context) {
        super(context);
        init();
    }

    private void init(){
        //后面 参数传 this，true 就是根View为FrameLayout
        LayoutInflater.from(mContext)
                .inflate(R.layout.tx_video_palyer_controller,this,true);

        mCenterStart = findViewById(R.id.center_start);
        mImage = findViewById(R.id.image);

        mTop = (LinearLayout) findViewById(R.id.top);
        mBack = (ImageView) findViewById(R.id.back);
        mTitle = (TextView) findViewById(R.id.title);
        mBatteryTime = (LinearLayout) findViewById(R.id.battery_time);
        mBattery = (ImageView) findViewById(R.id.battery);
        mTime = (TextView) findViewById(R.id.time);

        mBottom = (LinearLayout) findViewById(R.id.bottom);
        mRestartPause = (ImageView) findViewById(R.id.restart_or_pause);
        mPosition = (TextView) findViewById(R.id.position);
        mDuration = (TextView) findViewById(R.id.duration);
        mSeek = (SeekBar) findViewById(R.id.seek);
        mFullScreen = (ImageView) findViewById(R.id.full_screen);
        mClarity = (TextView) findViewById(R.id.clarity);
        mLength = (TextView) findViewById(R.id.length);

        mLoading = (LinearLayout) findViewById(R.id.loading);
        mLoadText = (TextView) findViewById(R.id.load_text);

        mChangePosition = (LinearLayout) findViewById(R.id.change_position);
        mChangePositionCurrent = (TextView) findViewById(R.id.change_position_current);
        mChangePositionProgress = (ProgressBar) findViewById(R.id.change_position_progress);

        mChangeBrightness = (LinearLayout) findViewById(R.id.change_brightness);
        mChangeBrightnessProgress = (ProgressBar) findViewById(R.id.change_brightness_progress);

        mChangeVolume = (LinearLayout) findViewById(R.id.change_volume);
        mChangeVolumeProgress = (ProgressBar) findViewById(R.id.change_volume_progress);

        mError = (LinearLayout) findViewById(R.id.error);
        mRetry = (TextView) findViewById(R.id.retry);

        mCompleted = (LinearLayout) findViewById(R.id.completed);
        mReplay = (TextView) findViewById(R.id.replay);
        mShare = (TextView) findViewById(R.id.share);

        mCenterStart.setOnClickListener(this);
        mBack.setOnClickListener(this);
        mRestartPause.setOnClickListener(this);
        mFullScreen.setOnClickListener(this);
        mClarity.setOnClickListener(this);
        mRetry.setOnClickListener(this);
        mReplay.setOnClickListener(this);
        mShare.setOnClickListener(this);
        mSeek.setOnSeekBarChangeListener(this);
        this.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void setTitle(String title) {
        mTitle.setText(title);
    }

    @Override
    public void setImage(@DrawableRes int resId) {
        mImage.setImageResource(resId);
    }

    @Override
    public ImageView imageView() {
        return mImage;
    }

    @Override
    public void setLength(long length) {
        mLength.setText(NiceUtil.formatTime(length));
    }

    @Override
    public void setNiceVideoPlayer(INiceVideoPlayer niceVideoPlayer) {
        super.setNiceVideoPlayer(niceVideoPlayer);
        // 给播放器配置视频链接地址
        if(clarities != null && clarities.size()>1){
            mNiceVideoPlayer.setUp(clarities.get(defaultClarityIndex).videoUrl,null);
        }
    }

    @Override
    protected void onPlayStateChanged(int playState) {

    }

    @Override
    protected void onPlayModeChanged(int playMode) {

    }

    @Override
    protected void reset() {

    }

    @Override
    protected void updateProgress() {

    }

    @Override
    protected void hideChangeVolume() {

    }

    @Override
    protected void hideChangeBrightness() {

    }

    @Override
    protected void hideChangePosition() {

    }

    @Override
    protected void showChangePosition(long duration, int newPositionProgress) {

    }

    @Override
    protected void showChangeVolume(int newVolumeProgress) {

    }

    @Override
    protected void showChangeBrightness(int newBrightnessProgress) {

    }

    /**
     * 电池状态即电量变化广播接收器
     */
    private BroadcastReceiver mBatterReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN);
            if(status == BatteryManager.BATTERY_STATUS_CHARGING){
                //充电中
                mBattery.setImageResource(R.drawable.battery_charging);
            } else if(status == BatteryManager.BATTERY_STATUS_FULL){
                //充电完成
                mBattery.setImageResource(R.drawable.battery_full);
            } else {
                int level = intent.getIntExtra(BatteryManager.EXTRA_STATUS,0);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE,0);
                int percentage = (int)(((float)level / scale) * 100);
                if(percentage <= 10){
                    mBattery.setImageResource(R.drawable.battery_10);
                } else if(percentage <= 20){
                    mBattery.setImageResource(R.drawable.battery_20);
                } else if(percentage <= 50){
                    mBattery.setImageResource(R.drawable.battery_50);
                } else if(percentage <= 80){
                    mBattery.setImageResource(R.drawable.battery_80);
                } else if(percentage <= 100){
                    mBattery.setImageResource(R.drawable.battery_100);
                }
            }
        }
    };

}

















