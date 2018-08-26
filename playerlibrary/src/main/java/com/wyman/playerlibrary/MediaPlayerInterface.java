package com.wyman.playerlibrary;

/**
 * Created by wyman
 * on 2018-08-26.
 * 媒体播放器接口
 * 接软编码或MediaPlayer硬编码
 */
public interface MediaPlayerInterface {

    /*准备阶段*/
    void prepare();

    /*开始播放*/
    void start();

    /* 暂停播放 */
    void pause();

    /*停止播放*/
    void stop();

    /*重置*/
    void reset();

    /*释放播放器*/
    void release();

    /**
     * 从哪里开始播放
     * @param msec 毫秒
     * */
    void seekTo(long msec);
}
