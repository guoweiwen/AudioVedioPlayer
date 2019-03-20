package com.wyman.videoaudioplayer.filter;


import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.os.Environment;
import android.util.Log;

import com.wyman.videoaudioplayer.camera.OpenGLUtils;
import com.wyman.videoaudioplayer.encoder.EglCore;
import com.wyman.videoaudioplayer.encoder.MediaAudioEncoder;
import com.wyman.videoaudioplayer.encoder.MediaEncoder;
import com.wyman.videoaudioplayer.encoder.MediaMuxerWrapper;
import com.wyman.videoaudioplayer.encoder.MediaVideoEncoder;
import com.wyman.videoaudioplayer.encoder.VideoCombiner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;


/**
 * Created by chenchao on 2017/12/7.
 */
public class MovieWriter extends GPUFilter implements VideoCombiner.VideoCombineListener {
    private static final String TAG = MovieWriter.class.getSimpleName();

    private ArrayList<String> uriList;
    private ArrayList<Long> times;
    private ArrayList<Long> audioPts;

    public String outputVideoFile;

    private int videoFileIndex = -1;
    private String tmpVideoFilePath;

    public int maxDuration = 0;//录制时间 秒单位
    private long currentMillis=0;
    private long lastAudioPts;
    private Timer timer;
    private Context mContext;
    private int mVideoHeight;
    private int mVideoWidth;

    long combineStartTime = 0;//记录合并所需时间
    private boolean isDeleteTemp = false;//默认不删除分段的mp4

    //-----------------------合并视频回调接口------------------------------
    @Override
    public void onCombineStart() {
        combineStartTime = System.currentTimeMillis();
    }

    @Override
    public void onCombineProcessing(int current, int sum) {

    }

    @Override
    public void onCombineFinished(boolean success) {
        long combineLastTime = System.currentTimeMillis() - combineStartTime;
        Log.e(TAG,"合并视频所需时间：" + combineLastTime);
        if(recordCallBack != null){
            recordCallBack.onRecordFinish(outputVideoFile);
        }
        //是否删除多段的Mp4
        if(isDeleteTemp){
            for(int i=0;i<uriList.size();i++){
                String path = uriList.get(i);
                File file = new File(path);
                if(file.isFile()){
                    file.delete();
                }
            }
        }
        uriList = new ArrayList<>();
    }
//-----------------------合并视频回调接口------------------------------

    public enum RecordStatus {
        Stoped,Paused,Capturing
    }
    public RecordStatus recordStatus= RecordStatus.Stoped;


    private EGLSurface mEGLScreenSurface;
    private EGL10 mEGL;
    private EGLDisplay mEGLDisplay;
    private EGLContext mEGLContext;
    private EglCore mEGLCore;

    private MediaMuxerWrapper mMuxer;
    private MediaVideoEncoder mVideoEncoder;
    private MediaAudioEncoder mAudioEncoder;
    private WindowSurface mCodecInput;

    public interface RecordCallBack{
        void onRecordProgress(float progress);
        void onRecordTimeEnd();
        void onRecordFinish(String filePath);
    }
    public RecordCallBack recordCallBack;

    public MovieWriter(Context context)
    {
        super();
        mContext = context;
    }

    /**
     * 返回当前毫秒
     * */
    public long getCurrentMillis() {
        return currentMillis;
    }

    /**
     * 返回分段视频数量
     * */
    public int getUriListSize() {
        int size;
        if(uriList != null && uriList.size() > 0){
            size = uriList.size();
        } else {
            size = 0;
        }
        return size;
    }

    @Override
    public void init(){
        super.init();

        resetGL();
        uriList = new ArrayList<>();
        times = new ArrayList<Long>();
        audioPts = new ArrayList<Long>();
    }
    private void resetGL(){

        mEGL = (EGL10) EGLContext.getEGL();
        mEGLDisplay = mEGL.eglGetCurrentDisplay();
        mEGLContext = mEGL.eglGetCurrentContext();
        mEGLScreenSurface = mEGL.eglGetCurrentSurface(EGL10.EGL_DRAW);

    }
    @Override
    public void onDrawFrame(int textureId, SurfaceTexture st, int mViewWidth, int mViewHeight){
        OpenGLUtils.checkGlError("MovWriter1");
        super.onDrawFrame(textureId,st, mViewWidth, mViewHeight);

        if (recordStatus== RecordStatus.Capturing) {
            // create encoder surface
            if (mCodecInput == null) {
                mEGLCore = new EglCore(EGL14.eglGetCurrentContext(), EglCore.FLAG_RECORDABLE);
                mCodecInput = new WindowSurface(mEGLCore, mVideoEncoder.getSurface(), false);
            }
            // Draw on encoder surface
            mCodecInput.makeCurrent();
            GLES20.glViewport(0,0,mVideoWidth,mVideoHeight);
            super.onDrawFrame(textureId, st, mViewWidth, mViewHeight);

            if(mCodecInput!=null) {
                mCodecInput.swapBuffers();
                mVideoEncoder.frameAvailableSoon();
            }

        }
        // Make screen surface be current surface
        mEGL.eglMakeCurrent(mEGLDisplay, mEGLScreenSurface, mEGLScreenSurface, mEGLContext);
//        if(OpenGLUtils.checkGlError("makeCurrent") != 1){
//            Log.e("mEGL.eglMakeCurrent","error");
//        }
        GLES20.glViewport(0,0,mViewWidth,mViewHeight);
    }
    public void startRecording(final int width, final int height, final int degree, final String musicPath) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                resetGL();
                if (recordStatus!= RecordStatus.Stoped) {
                    return;
                }
                recordStatus = RecordStatus.Capturing;
                videoFileIndex ++;
                mVideoWidth = width;
                mVideoHeight = height;
                times.add(new Long(currentMillis));
                audioPts.add(new Long(lastAudioPts));
                File dic = new File(Environment.getExternalStorageDirectory(),"A_Video_Wyman");
                if(!dic.exists()){
                    dic.mkdir();
                }

                tmpVideoFilePath = dic.toString()+"/"+videoFileIndex+".mp4";
                File file = new File(tmpVideoFilePath);
                if(file.exists()){
                    file.delete();
                }


                try {
                    //通过Muxer合并
                    mMuxer = new MediaMuxerWrapper(tmpVideoFilePath,degree);

                    // for video capturing
                    mVideoEncoder = new MediaVideoEncoder(mMuxer, mMediaEncoderListener, width, height);
                    // for audio capturing
                    mAudioEncoder = new MediaAudioEncoder(mMuxer,musicPath, mMediaEncoderListener);

                    mAudioEncoder.startPts = lastAudioPts;
                    mMuxer.prepare();
                    mMuxer.startRecording();
                    if(timer==null){
                        timer=new Timer(true);
                    }
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if(recordStatus== RecordStatus.Capturing) {
                                currentMillis += 200;
                                if (currentMillis >= maxDuration * 1000) {
                                    if (recordCallBack != null) {
                                        recordCallBack.onRecordTimeEnd();
                                    }
                                } else {
                                    if (recordCallBack != null) {
                                        float progress = (float) currentMillis / (maxDuration * 1000);
                                        recordCallBack.onRecordProgress(progress);
                                    }
                                }
                            }

                        }
                    },0,200);

                } catch (IOException e) {
                    recordStatus = RecordStatus.Stoped;
//                    e.printStackTrace();
                    throw new Error(e);

                }
            }
        });
    }
    protected void resumeRecording() {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                if (recordStatus!= RecordStatus.Paused) {
                    return;
                }

                mMuxer.resumeRecording();
                recordStatus= RecordStatus.Capturing;

            }
        });
    }
    protected void pauseRecording() {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                if (recordStatus!= RecordStatus.Capturing) {
                    return;
                }

                mMuxer.pauseRecording();
                recordStatus= RecordStatus.Paused;


            }
        });
    }

    public void stopRecording() {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                if (recordStatus== RecordStatus.Stoped) {
                    return;
                }
                recordStatus= RecordStatus.Stoped;
                timer.cancel();
                timer=null;
                lastAudioPts = mMuxer.getSampleTime();

                mMuxer.stopRecording();

                releaseEncodeSurface();
                uriList.add(tmpVideoFilePath);//Uri.parse(tmpVideoFilePath)
            }
        });
    }
    //回删
    public void fallBack() {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                if(videoFileIndex>-1) {
                    videoFileIndex--;
                }
                if(times.size()>0) {
                    Long time = times.remove(times.size() - 1);
                    currentMillis = time.longValue();
                    if (recordCallBack != null) {
                        recordCallBack.onRecordProgress((float) currentMillis / (maxDuration * 1000));
                    }
                }
                if(audioPts.size()>0){
                    Long pts = audioPts.remove(audioPts.size()-1);
                    lastAudioPts = pts.longValue();
                }
                if(uriList.size()>0) {
                    String uri = uriList.remove(uriList.size() - 1);
                    File file = new File(uri);
                    if(file.exists()){
                        file.delete();
                    }
                }
            }
        });
    }

    public void finishRecording() {
        currentMillis=0;
        videoFileIndex=-1;
        lastAudioPts = 0;
        times = new ArrayList<Long>();
        audioPts = new ArrayList<Long>();
        File file = new File(outputVideoFile);
        if(file.exists()){
            file.delete();
        }
        if(uriList.size() == 1){//当前只有一段视频的时候
            if(recordCallBack!=null){
                recordCallBack.onRecordFinish(uriList.get(0));
            }
            uriList = new ArrayList<>();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG,"combiner.combineVideo");
                //合并多段MP4
                VideoCombiner combiner
                        = new VideoCombiner(uriList,outputVideoFile,MovieWriter.this);
                combiner.combineVideo();
            }
        }).start();
    }

    private void releaseEncodeSurface() {
        if (mEGLCore != null) {
            mEGLCore.makeNothingCurrent();
            mEGLCore.release();
            mEGLCore = null;
        }
        if (mCodecInput != null) {
            mCodecInput.release();
            mCodecInput = null;
        }

    }

    /**
     * callback methods from encoder
     */
    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
        }

        @Override
        public void onMuxerStopped() {
        }
    };
}
