package com.wyman.videoaudioplayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class VideoAudioPlayerActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = VideoAudioPlayerActivity.class.getSimpleName();
    private CameraSurfaceView previewForCamera_CameraSurfaceView;
    private Button playStop_button;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_audio_player);
        initUI();
    }

    private void initUI() {
        previewForCamera_CameraSurfaceView = findViewById(R.id.previewForCamera_CameraSurfaceView);
        playStop_button = findViewById(R.id.playStop_button);
        playStop_button.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.playStop_button:
                buttonEvent();
                break;
        }
    }

    private boolean playing = true;
    /**
     * 拍视频按钮
     * */
    private void buttonEvent() {
        if(playing){//开始拍摄
            startRecording();
            playing = false;
        } else {//停止拍摄
            stopRecording();
            playing = true;
        }
    }

    /**
     * 初始化工作最好在工作线程处理
     * */
    private void startRecording() {
        playStop_button.setText("停止");


    }

    private void stopRecording() {
        playStop_button.setText("拍视频");
    }
}
