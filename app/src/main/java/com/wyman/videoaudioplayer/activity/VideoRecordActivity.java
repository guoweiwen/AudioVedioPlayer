package com.wyman.videoaudioplayer.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wyman.videoaudioplayer.BaseApplication;
import com.wyman.videoaudioplayer.R;
import com.wyman.videoaudioplayer.camera.CameraManager;
import com.wyman.videoaudioplayer.camera.CameraWraper;
import com.wyman.videoaudioplayer.filter.MovieWriter;
import com.wyman.videoaudioplayer.jni2c.FFmpegBridge;
import com.wyman.videoaudioplayer.model.MediaObject;
import com.wyman.videoaudioplayer.model.MediaRecorderBase;
import com.wyman.videoaudioplayer.model.MediaRecorderNative;
import com.wyman.videoaudioplayer.utils.FileUtils;
import com.wyman.videoaudioplayer.utils.Log;
import com.wyman.videoaudioplayer.view.FocusSurfaceView;
import com.wyman.videoaudioplayer.view.RecordedButton;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;

/**
 * 音视频录制Activity
 * */
public class VideoRecordActivity extends AppCompatActivity {
    private static final String TAG = VideoRecordActivity.class.getSimpleName();

    private AlertDialog progressDialog;
    private TextView dialogTextView;

    private static final int REQUEST_KEY = 100;
    //录制视频
    private static final int HANDLER_RECORD = 200;
    //编辑视频
    private static final int HANDLER_EDIT_VIDEO = 201;
    //拍摄照片
    private static final int HANDLER_CAMERA_PHOTO = 202;

    private long startTime;

    //视频方面的信息
    private GLSurfaceView glSurfaceView;
    private int videoDegree;
    private int videoHeight;
    private int videoWidth;
    //录制回调函数
    public MovieWriter.RecordCallBack recordCallBack;

    private RelativeLayout relativeLayout_top,rl_bottom,rl_bottom2;
    private ImageView change_flash_imageview;
    private ImageView change_camera_imageview,
            next_imageview,close_imageview;
    private TextView hint_textview;
    private RecordedButton start_recordedbutton;
    private ImageView finish_imageview,back_imageview;
    //最大录制时间
    private int maxDuration = 10;//单位秒
    //本次段落是否录制完成
    private boolean isRecordedOver;

    private MovieWriter mMovieWriter;
    private int facingType;
    private CameraWraper mCamera;
    private String musicPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //放在 setContentView前面 全屏显示
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_video_record);
        initData();
        initUI();
        initEvent();
        initCamera();

    }

    private void initData() {
        //debug 模式 打印日志
        Log.setLog(true);

        //设置视频方面信息
        videoDegree = 0;
        videoWidth = 1080;
        videoHeight = 1920;
    }

    private void initUI() {
        glSurfaceView = findViewById(R.id.videorecord_surfaceview);
        start_recordedbutton = findViewById(R.id.start_recordedbutton);
//        vv_play = (MyVideoView) findViewById(R.id.vv_play);//播放视频的view
//        iv_photo = (ImageView) findViewById(R.id.iv_photo);
//        change_flash_imageview = findViewById(R.id.iv_change_flash);
        finish_imageview =  findViewById(R.id.iv_finish);
        back_imageview =  findViewById(R.id.iv_back);
        hint_textview = findViewById(R.id.hint_textview);
        rl_bottom =  findViewById(R.id.rl_bottom);
        rl_bottom2 = findViewById(R.id.rl_bottom2);
        next_imageview =  findViewById(R.id.iv_next);
        close_imageview =  findViewById(R.id.iv_close);
        change_camera_imageview = findViewById(R.id.iv_change_camera);
        relativeLayout_top = findViewById(R.id.rltop_relativelayout);
    }

    private void initCamera() {
        CameraManager.getManager().init();
        CameraManager.getManager().setGlSurfaceView(glSurfaceView);

        mMovieWriter = new MovieWriter(getApplicationContext());
        mMovieWriter.maxDuration = maxDuration;
        mMovieWriter.setFirstLayer(true);
        CameraManager.getManager().setFilter(mMovieWriter);
        mMovieWriter.recordCallBack = new MovieWriter.RecordCallBack() {
            @Override
            public void onRecordProgress(final float progress) {
                //更新UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        Log.e(TAG,"progress:" + String.valueOf(progress));
                        //progress 为百分比
                        start_recordedbutton.setProgress(progress * maxDuration);
                        if (rl_bottom.getVisibility() == View.VISIBLE) {
                            changeButton(false);
                        }
                    }
                });
            }

            /**
             * 达到最大录制时长
             * */
            @Override
            public void onRecordTimeEnd() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //进度栏填满
                        start_recordedbutton.setProgress(maxDuration);
                    }
                });
            }

            /**
             * 调用MovieWriter.finishRecording()
             * 回调
             * */
            @Override
            public void onRecordFinish(String filePath) {
                Log.e(TAG,"录制完成回调:" + filePath);
                //录制完成回调
                mMovieWriter.outputVideoFile = null;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        Toast.makeText(getApplicationContext(),
//                                "拼接用时:"+(System.currentTimeMillis()-startTime),
//                                Toast.LENGTH_LONG).show();
                        closeProgressDialog();
                    }
                });
                //跳转到播放Activity
//                Intent intent=new Intent(getApplicationContext(),VideoPlayerActivity.class);
//                intent.putExtra("videoPath",filePath);
//                startActivity(intent);
            }
        };
    }

    /**
     * 监听事件
     * */
    private void initEvent() {
        start_recordedbutton.setMax(maxDuration);
        start_recordedbutton.setOnGestureListener(new RecordedButton.OnGestureListener() {
            @Override
            public void onLongClick() {
                //长按触发
                //录制按钮设置断点
                start_recordedbutton.setSplit();

                if(mMovieWriter.recordStatus == MovieWriter.RecordStatus.Stoped ){
                    if(mMovieWriter.outputVideoFile==null) {
                        String videoOutPutPath
                                = Environment.getExternalStorageDirectory() + "/A_Video_Wyman/" + FileUtils.getDateTimeString() + ".mp4";
                        File file = new File(videoOutPutPath);
                        if (file.exists()) {
                            file.delete();
                        }
                        mMovieWriter.outputVideoFile = videoOutPutPath;
                    }
                    mMovieWriter.startRecording(videoWidth,videoHeight,videoDegree,musicPath);
                }
            }

            @Override
            public void onClick() {
                //拍照

            }

            @Override
            public void onLift() {
                //长按手指离开时候 阶段性结束
                mMovieWriter.stopRecording();
                changeButton(true);
            }

            @Override
            public void onOver() {
                //录制完毕

                mMovieWriter.stopRecording();
                mMovieWriter.finishRecording();

                //UI 操作
                changeButton(false);
                start_recordedbutton.closeButton();
                Toast.makeText(getApplicationContext(),"已达到最大视频时长",Toast.LENGTH_LONG).show();
                //合并中等待对话框
                dialogTextView = showProgressDialog();
            }
        });

        /**
         * 回删按钮
         * */
        back_imageview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mMovieWriter.recordStatus != MovieWriter.RecordStatus.Stoped) {
//                    Toast.makeText(t"请先停止拍摄再进行次操作",Toast.LENGTH_LONG);
                    return;
                }
                if (start_recordedbutton.isDeleteMode()) {//判断是否要删除视频段落
                    mMovieWriter.fallBack();
                    //更新UI
                    int size = mMovieWriter.getUriListSize();
                    if(size > 0){
                        changeButton(true);
                    }else{
                        changeButton(false);
                    }
                    start_recordedbutton.deleteSplit();//删除断点
                    back_imageview.setImageResource(R.mipmap.video_delete);
                } else if (mMovieWriter.getUriListSize() > 0) {
                    //更新UI
                    start_recordedbutton.setDeleteMode(true);//设置删除模式
                    back_imageview.setImageResource(R.mipmap.video_delete_click);
                }
            }
        });

        /**
         * 结束录制按钮
         * */
        finish_imageview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishRecording();
            }
        });

        // 视频录制完跳转 剪辑Activity
        next_imageview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //跳转到播放Activity
//                Intent intent=new Intent(getApplicationContext(),VideoPlayerActivity.class);
//                intent.putExtra("videoPath",filePath);
//                startActivity(intent);
            }
        });


        close_imageview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initMediaRecorderState();
            }
        });

        /*
        * 转换摄像头监听
        * */
        change_camera_imageview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(facingType == Camera.CameraInfo.CAMERA_FACING_BACK){
                    facingType = Camera.CameraInfo.CAMERA_FACING_FRONT;
                }else {
                    facingType = Camera.CameraInfo.CAMERA_FACING_BACK;
                }
                CameraManager.getManager().onPause();
                CameraManager.getManager().releaseCamera();

                mCamera = CameraManager.getManager().openCamera( facingType);
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


                CameraManager.getManager().setFilter(mMovieWriter);
                CameraManager.getManager().onResume();
            }
        });
    }

    /**
     * 录制结束
     * */
    private void finishRecording() {
        changeButton(false);
        start_recordedbutton.setVisibility(View.GONE);
        dialogTextView = showProgressDialog();

        mMovieWriter.finishRecording();
        startTime = System.currentTimeMillis();
    }

    /**
     * 初始化视频拍摄状态
     */
    private void initMediaRecorderState(){
        //播放器View 和 照片显示的View
//        vv_play.setVisibility(View.GONE);
//        vv_play.pause();
//        iv_photo.setVisibility(View.GONE);

        //闪光灯
//        rl_top.setVisibility(View.VISIBLE);
        start_recordedbutton.setVisibility(View.VISIBLE);
        rl_bottom2.setVisibility(View.GONE);
        changeButton(false);
        hint_textview.setVisibility(View.VISIBLE);

        start_recordedbutton.setProgress(mMovieWriter.getCurrentMillis());
        start_recordedbutton.cleanSplit();
    }

    @SuppressLint("HandlerLeak")
    private Handler myHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case HANDLER_RECORD: {
                    //拍摄视频的handler

                }
                break;
                case HANDLER_EDIT_VIDEO: {
                    //合成视频的handler
//                    int progress = UtilityAdapter.FilterParserAction("", UtilityAdapter.PARSERACTION_PROGRESS);
                }
                break;
                case HANDLER_CAMERA_PHOTO: {
                    //拍照

                }
                break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        CameraManager.getManager().onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        CameraManager.getManager().onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * 回退键
     * */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraManager.getManager().onDestory();
    }

    /**
     * 显示回删按钮和录制完成按钮
     */
    private void changeButton(boolean flag){
        if(flag){
            hint_textview.setVisibility(View.VISIBLE);
            rl_bottom.setVisibility(View.VISIBLE);
        }else{
            hint_textview.setVisibility(View.GONE);
            rl_bottom.setVisibility(View.GONE);
        }
    }

    public TextView showProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        View view = View.inflate(this, R.layout.dialog_loading, null);
        builder.setView(view);
        ProgressBar pb_loading = (ProgressBar) view.findViewById(R.id.pb_loading);
        TextView tv_hint = (TextView) view.findViewById(R.id.tv_hint);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            pb_loading.setIndeterminateTintList(ContextCompat.getColorStateList(this, R.color.dialog_pro_color));
        }
        tv_hint.setText("视频编译中");
        progressDialog = builder.create();
        Log.e(TAG,"progressDialog create");
        progressDialog.show();

        return tv_hint;
    }

    public void closeProgressDialog() {
        Log.e(TAG,"progressDialog close");
        try {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}






















