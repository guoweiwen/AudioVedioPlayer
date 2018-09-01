package com.wyman.videoaudioplayer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by wyman
 * on 2018-08-21.
 */

public class MediaAudioEncoder extends MediaEncodec{
    private static final String TAG = MediaAudioEncoder.class.getSimpleName();
    private static final String MIME_TYPE = "audio/mp4a-latm";
    public static final int SAMPLES_PER_FRAME = 1024;	// AAC, bytes/frame/channel
    public static final int FRAMES_PER_BUFFER = 25; 	// AAC, frame/buffer/sec
    private static final int[] AUDIO_SOURCES = new int[] {
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.CAMCORDER,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
    };
    private AudioThread audioThread;


    protected MediaAudioEncoder(MediaMuxerWrapper muxer) {
        super(muxer);
    }

    /**
     * prepare()初始化MeidaCodec
     * */
    @Override
    void prepare() throws IOException{
        trackIndex = -1;
        muxerStarted = isEOS = false;
        final MediaCodecInfo audioCodecInfo = selectAudioCodec(MIME_TYPE);
        if(audioCodecInfo == null){
            Log.e(TAG,"Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
        final MediaFormat audioFormat =
                MediaFormat.createAudioFormat(MIME_TYPE,44100,1);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC);;
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT,1);
        mediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
        mediaCodec.configure(audioFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
    }

    @Override
    protected void startRecording() {
        super.startRecording();
        if(audioThread == null){
            audioThread = new AudioThread();
            audioThread.start();
        }
    }

    /**
     * 该线程主要用于读取MIC数据
     * */
    private class AudioThread extends Thread{
        @Override
        public void run() {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            try{
                final int min_buffer_size = AudioRecord.getMinBufferSize(
                        44100,AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                );
                int buffer_size = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
                if(buffer_size < min_buffer_size){
                    buffer_size = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;
                }
                AudioRecord audioRecord = null;
                for(final int source : AUDIO_SOURCES){
                    try{
                        audioRecord = new AudioRecord(
                                source,44100,
                                AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,buffer_size
                        );
                        if(audioRecord.getState() != AudioRecord.STATE_INITIALIZED){
                            audioRecord = null;
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                        audioRecord = null;
                    }
                    if(audioRecord != null){
                        break;
                    }
                }
                if(audioRecord != null){
                    try{
                        if(isCaptureing){
                            final ByteBuffer buf = ByteBuffer.allocate(SAMPLES_PER_FRAME);
                            int readBytes;
                            audioRecord.startRecording();
                            try{
                                for (;isCaptureing && !requestStop && !isEOS;){
                                    buf.clear();
                                    readBytes = audioRecord.read(buf,SAMPLES_PER_FRAME);
                                    if(readBytes > 0){
                                        buf.position(readBytes);
                                        //buf.flip(); 重置limit = position，position = 0，让读取时，读取到limit的值
                                        //写模式看position，读模式看limit
                                        buf.flip();
                                        encode(buf,readBytes,getPTSUs());
                                        //要求OutputBuffer处理数据
                                        frameAvailableSoon();
                                    }
                                }
                                frameAvailableSoon();
                            } finally{
                                audioRecord.stop();
                            }
                        }
                    } finally {
                        audioRecord.release();
                    }
                } else {
                    Log.e(TAG, "failed to initialize AudioRecord");
                }
            } catch (Exception e){
                Log.e(TAG, "AudioThread#run", e);
            }
        }
    }

    /**
     * select the first codec that match a specific MIME type
     * @param mimeType
     * @return
     */
    private static final MediaCodecInfo selectAudioCodec(final String mimeType){
        MediaCodecInfo result = null;
        int numCodec = MediaCodecList.getCodecCount();
LOOP:   for(int i=0;i<numCodec;i++){
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if(!codecInfo.isEncoder()){
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for(String type : types){
                if(type.equalsIgnoreCase(mimeType)){
                    if(result == null){
                        result = codecInfo;
                        break LOOP;
                    }
                }
            }
        }
        return result;
    }
}

























