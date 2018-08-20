package com.wyman.videoaudioplayer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by wyman
 * on 2018-08-20.
 * MediaMuxer 包装类
 * 整个MediaMuxerWrapper控制着MeidaCodec的周期
 * MediaMuxer生命周期：
 *          创建：       new Muxer()
 *          添加音轨视轨： mediaMuxer.addTrack()
 *          旋转角度     mediaMuxer.setOrientationHint(angle)
 *          start       mediaMuxer.start()
 *          writeData   mediaMuxer.write(trackIndex,bytebuffer,bufferInfo)
 */
public class MediaMuxerWrapper {
    private static final String TAG = MediaMuxerWrapper.class.getSimpleName();
    private MediaMuxer mediaMuxer;// API >= 18
    //编码器数量
    private int encodecCount;
    //添加编码器数量
    private int addEncodecCount;
    //标记MediaMuxer是否可以start
    private AtomicBoolean isStart;
    //编码器
    private MediaEncodec audioMediaCodec,videoMediaCodec;

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

    /**
     * prepare()实际上是MediaCodec的prepare()
     * */
    public void prepare(){
        if(videoMediaCodec!=null){
            videoMediaCodec.prepare();
        }
        if(audioMediaCodec!=null){
            audioMediaCodec.prepare();
        }
    }

    /**
     * startRecording()实际上是MediaCodec的startstartRecording()
     * */
    public void startRecording(){
        if(videoMediaCodec!=null){
            videoMediaCodec.startRecording();
        }
        if(audioMediaCodec!=null){
            audioMediaCodec.startRecording();
        }
    }

    /**
     * 方法添加synchronized 表示锁是该方法的对象
     * */
    public synchronized boolean isStarted(){
        return isStart.get();
    }

    void addEncoder(MediaEncodec encodec){
        if(encodec instanceof MediaVideoEncoder){
            if(MediaVideoEncoder!=null)throw new IllegalArgumentException("MediaVideoEncoder already added");
            videoMediaCodec = encodec;
        } else if(encodec instanceof MediaAudioEncoder){
            if(MediaAudioEncoder != null)throw new IllegalArgumentException("MediaAudioEncoder already added");
            audioMediaCodec = encodec;
        } else {
            throw new IllegalArgumentException("unsupported encoder");
        }
        encodecCount = (videoMediaCodec != null ? 1 : 0) + (audioMediaCodec!=null ? 1 : 0);
    }

    /**
     * MediaMuxer.start()
     * */
    synchronized boolean start(){
        addEncodecCount++;
        if(encodecCount>0 && encodecCount == addEncodecCount){
            mediaMuxer.start();
            isStart.set(true);
            notifyAll();//使用该对象即MediaMuxerWrapper进行wait()会被唤醒
        }
        return isStart.get();
    }

    synchronized void stop(){
        addEncodecCount--;
        if(encodecCount > 0 && addEncodecCount <= 0){
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
            isStart.set(false);
        }
    }

    /**
     * 添加音轨，视轨
     * */
    synchronized int addTrack(final MediaFormat mediaFormat){
        if(isStart.get()){
            throw new IllegalStateException("MeidaMuxer already started");
        }
        return mediaMuxer.addTrack(mediaFormat);
    }

    /**
     * 合成MP4文件
     * */
    synchronized void writeSampleData(int trackIndex, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo){
        if(addEncodecCount > 0){
            mediaMuxer.writeSampleData(trackIndex,byteBuffer,bufferInfo);
        }
    }
}




















