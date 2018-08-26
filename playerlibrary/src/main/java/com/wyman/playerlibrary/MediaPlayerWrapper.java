package com.wyman.playerlibrary;

import android.media.MediaPlayer;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.Objects;

/**
 * Created by wyman
 * on 2018-08-26.
 * MediaPlayer包装类
 */
public class MediaPlayerWrapper implements MediaPlayerInterface{
    private static final String TAG = MediaPlayerWrapper.class.getSimpleName();

    private MediaPlayer mMediaPlayer;

    public MediaPlayerWrapper(){
        mMediaPlayer = new MediaPlayer();
    }

    @Override
    public void prepare() {
        Objects.requireNonNull(mMediaPlayer,"MediaPlayer is null");
        try {
            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        Objects.requireNonNull(mMediaPlayer,"MediaPlayer is null");
        mMediaPlayer.start();
    }

    @Override
    public void pause() {
        Objects.requireNonNull(mMediaPlayer,"MediaPlayer is null");
        mMediaPlayer.pause();
    }

    @Override
    public void stop() {
        Objects.requireNonNull(mMediaPlayer,"MediaPlayer is null");
        mMediaPlayer.stop();
    }

    @Override
    public void reset() {
        Objects.requireNonNull(mMediaPlayer,"MediaPlayer is null");
        mMediaPlayer.reset();
    }

    @Override
    public void release() {
        Objects.requireNonNull(mMediaPlayer,"MediaPlayer is null");
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    @Override
    public void seekTo(int msec) {
        Objects.requireNonNull(mMediaPlayer,"MediaPlayer is null");
        mMediaPlayer.seekTo(msec);
    }

    @Override
    public void setDataSource(String path) {
        Objects.requireNonNull(mMediaPlayer,"MediaPlayer is null");
        try {
            mMediaPlayer.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setDisplay(SurfaceHolder surfaceHolder) {
        Objects.requireNonNull(mMediaPlayer,"MediaPlayer is null");
        mMediaPlayer.setDisplay(surfaceHolder);
    }

    @Override
    public boolean isPlaying() {
        Objects.requireNonNull(mMediaPlayer,"MediaPlayer is null");
        return mMediaPlayer.isPlaying();
    }

    public MediaPlayer getMediaPlayer(){
        if(mMediaPlayer != null){
            return mMediaPlayer;
        }
        return null;
    }

}
