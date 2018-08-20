package com.wyman.videoaudioplayer;

import android.media.MediaCodec;
import android.media.MediaMuxer;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by wyman
 * on 2018-08-20.
 * MediaMuxer 包装类
 * 整个MediaMuxerWrapper控制着MeidaCodec的周期
 */
public class MediaMuxerWrapper {
    private static final String TAG = MediaMuxerWrapper.class.getSimpleName();
    private MediaMuxer mediaMuxer;
    //编码器数量
    private int encodecCount;
    //添加编码器数量
    private int addEncodecCount;
    //标记MediaMuxer是否可以start
    private AtomicBoolean isStart;
    //编码器
    private MediaEncoder audioMediaCodec,videoMediaCodec;

    public MediaMuxerWrapper(String filePath){
        Objects.requireNonNull(filePath,"FilePath is null");
        try {
            mediaMuxer = new MediaMuxer(filePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            encodecCount = addEncodecCount = 0;
            isStart = new AtomicBoolean(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void prepare(){
        if(videoMediaCodec!=null){
            videoMediaCodec.prepare();
        }
        if(audioMediaCodec!=null){
            audioMediaCodec.prepare();
        }
    }

    public void startRecording(){
        if(videoMediaCodec!=null){
            videoMediaCodec.startRecording();
        }
        if(audioMediaCodec!=null){
            audioMediaCodec.startRecording();
        }
    }

    public synchronized boolean isStarted(){
        return isStart.get();
    }
}




















