package com.wyman.videoaudioplayer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

/**
 * Created by wyman
 * on 2018-08-20.
 * 编码器父类
 */
public abstract class MediaEncodec implements Runnable{
    private static final String TAG = MediaEncodec.class.getSimpleName();

    protected static final int TIMEOUT_USEC = 10_000;//10毫秒
    protected static final int MSG_FRAME_AVAILABLE = 1;
    protected static final int MSG_STOP_RECORDING = 9;//停止拍摄

    //该对象的同步锁
    protected static final Object lock = new Object();
    //标记正在拍摄      用于子类采集数据标记
    protected boolean isCaptureing;
    //要求停止         用于当前类控制是否Muxer.writeSimpleData();
    protected boolean requestStop;
    //标记muxer 是否running
    protected boolean muxerStarted;
    //标记是否结尾
    protected boolean isEOS;
    //muxer 添加的轨道数量
    protected int trackIndex;
    //存放MediaMuxer的弱引用
    protected WeakReference<MediaMuxerWrapper> weakMuxer;
    protected MediaCodec mediaCodec;
    protected MediaCodec.BufferInfo bufferInfo;
    //记录帧数
    protected int requestDrain;
    //对上一次的时间戳
    private long prevOutputPTSUs = 0;


    protected MediaEncodec(MediaMuxerWrapper muxer){
        weakMuxer = new WeakReference<>(muxer);
        //添加MediaCodec
        muxer.addEncoder(this);
        synchronized (lock){
            //在这里创建BufferInfo 为了高效
            bufferInfo = new MediaCodec.BufferInfo();
            //实现Runnable outputbuffer
            new Thread(this,getClass().getName()).start();
            try {
                //释放锁  等待唤醒
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 该方法通知帧数据可以使用或者已经可用
     * */
    public boolean frameAvailableSoon(){
        synchronized (lock){
            if(!isCaptureing || requestStop){
                return false;
            }
            requestDrain++;
            lock.notifyAll();
        }
        return true;
    }


    //针对子类 初始化MeidaCodec
    //开一个子线程 用于OutputBuffer
    abstract void prepare() throws IOException;

    protected void startRecording(){
        synchronized (lock){
            isCaptureing = true;
            requestStop = false;
            lock.notifyAll();
        }
    }

    protected void stopRecording(){
        synchronized (lock){
            if(!isCaptureing || requestStop){
                //
                return;
            }
            //不在接收帧数据
            requestStop = true;
            //不知道编码和mp4合成是否完成，所以立即唤醒所有线程避免延迟
            lock.notifyAll();
        }
    }

    /**
     * 释放所有对象
     * */
    protected void release(){
        isCaptureing = false;
        if(mediaCodec != null){
            try {
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        if(muxerStarted){
            MediaMuxerWrapper muxerWrapper = weakMuxer.get();
            if(muxerWrapper != null){
                try {
                    muxerWrapper.stop();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        bufferInfo = null;
    }

    protected void signalEndOfInputStream(){
        // signalEndOfInputStream is only avairable for video encoding with surface
        // and equivalent sending a empty buffer with BUFFER_FLAG_END_OF_STREAM flag.
//		mMediaCodec.signalEndOfInputStream();	// API >= 18
        //MP4尾
        encode(null,0,getPTSUs());
    }

    /*
    * 用于编码
    * */
    @Override
    public void run() {
        synchronized (lock){
            requestStop = false;
            requestDrain = 0;
            lock.notify();
        }
        final boolean isRunning = true;
        boolean localRequestStop;
        boolean localRequestDrain;
        while(isRunning){
            synchronized (lock){
                localRequestStop = requestStop;
                localRequestDrain = (requestDrain > 0);
                if(localRequestDrain){
                    //处理帧数据
                    requestDrain--;
                }
            }
            if(localRequestStop){//要求停止处理最后一帧数据
                drain();
                signalEndOfInputStream();
                drain();
                release();
                break;
            }
            //处理帧数据
            if(localRequestDrain){
                drain();
            } else {
                //当没有帧数据同时没有要求停止的时候当前线程等待状态释放锁
                synchronized (lock){
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        synchronized (lock){
            requestStop = true;
            isCaptureing = false;
        }
    }

    /**
     * 设置数组去MediaCodec encoder
     * 处理InputBuffer
     * */
    protected void encode(final ByteBuffer buffer,final int length,final long presentationTimeUs){
        if(!isCaptureing)return;
        final ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        while (isCaptureing){
            final int inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            if(inputBufferIndex >= 0){
                final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                //重置inputBuffer
                inputBuffer.clear();
                if(buffer != null){
                    inputBuffer.put(buffer);
                }
                if(length <= 0){
                    //EOS 没有数据
                    mediaCodec.queueInputBuffer(inputBufferIndex,0,0,
                            presentationTimeUs,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    break;
                } else {
                    mediaCodec.queueInputBuffer(inputBufferIndex,0,length,
                            presentationTimeUs,0);
                }
                break;
            } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait for MediaCodec encoder is ready to encode
                // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
                // will wait for maximum TIMEOUT_USEC(10msec) on each call
            }
        }
    }

    /**
     * 处理OutputBuffer和Muxer write数据
     * */
    protected void drain(){
        if(mediaCodec == null)return;
        ByteBuffer[] encoderOutputBuffer = mediaCodec.getOutputBuffers();
        int encoderStatus,count = 0;
        final MediaMuxerWrapper muxerWrapper = weakMuxer.get();
        if(muxerWrapper == null){
            Log.e(TAG,"MediaMuxerWrapper is unexpectedly null");
            return;
        }
        LOOP: while(isCaptureing){
            //bufferInfo是自己new 出来的
            encoderStatus = mediaCodec.dequeueOutputBuffer(bufferInfo,TIMEOUT_USEC);
            if(encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER){
                //如果5次（TIMEOUT_USEC x 5 = 50毫秒）都是此状态且没有结束即结束LOOP循环
                if(!isEOS){
                    if(++count > 5){
                        break LOOP;//跳出LOOP这个循环走外面的大循环
                    }
                }
            } else if(encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                //如果已经进入编码阶段该标记应该不会来
                encoderOutputBuffer = mediaCodec.getOutputBuffers();
            } else if(encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                //该标记状态是输出格式发生改变，在实际编码中应出现一次。
                // but this status never come on Android4.3 or less
                // and in that case, you should treat when MediaCodec.BUFFER_FLAG_CODEC_CONFIG come.
                if(muxerStarted){
                    //出现第二次的话，报错
                    throw new RuntimeException("format changed twice");
                }
                // get output format from codec and pass them to muxer
                // getOutputFormat should be called after INFO_OUTPUT_FORMAT_CHANGED otherwise crash.
                MediaFormat mediaFormat = mediaCodec.getOutputFormat();
                trackIndex = muxerWrapper.addTrack(mediaFormat);
                muxerStarted = true;
                if(!muxerWrapper.start()){
                    synchronized (muxerWrapper){
                        //等待另外一个线程添加轨道
                        while (!muxerWrapper.isStarted()){
                            try {
                                muxerWrapper.wait(100);
                            } catch (Exception e){
                                break LOOP;
                            }
                        }
                    }
                }
            } else if(encoderStatus < 0){
                Log.e(TAG, "drain:unexpected result from encoder#dequeueOutputBuffer: " + encoderStatus);
            } else {
                ByteBuffer encodedData = encoderOutputBuffer[encoderStatus];
                if(encodedData == null){
                    // this never should come...may be a MediaCodec internal error
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }
                if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0){
                    // You shoud set output format to muxer here when you target Android4.3 or less
                    // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                    // therefor we should expand and prepare output format from buffer data.
                    // This sample is for API>=18(>=Android 4.3), just ignore this flag here
                    Log.e(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG");
                    bufferInfo.size = 0;
                }
                if(bufferInfo.size != 0){
                    // encoded data is ready, clear waiting counter
                    count = 0;
                    if(!muxerStarted){
                        // muxer is not ready...this will prrograming failure.
                        throw new RuntimeException("drain:muxer hasn't started");
                    }
                    //write encoded data to muxer(need to adjust presentationTimeUs
                    bufferInfo.presentationTimeUs = getPTSUs();
                    muxerWrapper.writeSampleData(trackIndex,encodedData,bufferInfo);
                    prevOutputPTSUs = bufferInfo.presentationTimeUs;
                    //return buffer to encoder
                    mediaCodec.releaseOutputBuffer(encoderStatus,false);
                    if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                        //EOS come
                        isCaptureing = false;
                        break;//跳出循环
                    }
                }
            }
        }
    }

    /**
     * 获取下一个时间戳
     * */
    protected long getPTSUs(){
        long result = System.nanoTime() / 1000l;
        if(result < prevOutputPTSUs){
            result = (prevOutputPTSUs - result) + result;
        }
        return result;
    }
}




















