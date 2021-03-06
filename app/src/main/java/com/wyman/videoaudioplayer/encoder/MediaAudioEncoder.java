package com.wyman.videoaudioplayer.encoder;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MediaAudioEncoder.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaExtractor.SEEK_TO_CLOSEST_SYNC;

@TargetApi(18)
public class MediaAudioEncoder extends MediaEncoder {
    private static final boolean DEBUG = false;
    private static final String TAG = "MediaAudioEncoder";

    private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int SAMPLE_RATE = 44100;    // 44.1[KHz] is only setting guaranteed to be available on all devices.
    private static final int BIT_RATE = 64000;
    public static final int SAMPLES_PER_FRAME = 1024;    // AAC, bytes/frame/channel
    public static final int FRAMES_PER_BUFFER = 25;     // AAC, frame/buffer/sec
    private final String musicPath;

    private AudioThread mAudioThread = null;

    private MediaExtractor mediaExtractor;
    private MediaFormat mediaFormat;
    //解码器
    private MediaCodec mAudioDecoder;
    public long startPts;
    public MediaAudioEncoder(final MediaMuxerWrapper muxer, final String musicPath, final MediaEncoderListener listener) {
        super(muxer, listener);
        this.musicPath=musicPath;
    }

    /**
     * 准备分两种情况如果已经存在地址即 -> 选择音轨
     * 没有地址的情况 -> 只需准备 MeidaCodec
     * */
    @Override
    protected void prepare() throws IOException {
        if (DEBUG) Log.v(TAG, "prepare:");

        if(this.musicPath!=null){
            int sampleRate=0;
            int bitRate=0;
            int channelcount=0;
//            String fileName= Environment.getExternalStorageDirectory()+"/sample.mp3";
            mediaExtractor=new MediaExtractor();
            mediaExtractor.setDataSource(musicPath);

            int trackCount=mediaExtractor.getTrackCount();
            for(int i=0;i<trackCount;i++){
                mediaFormat=mediaExtractor.getTrackFormat(i);
                if(mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("audio/")){
                    //mediaExtractor 设置音轨
                    sampleRate =  mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    bitRate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE);
                    channelcount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    mediaExtractor.selectTrack(i);
                    break;
                }
            }

            mAudioDecoder = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME));
            mAudioDecoder.configure(mediaFormat, null, null, 0);
            final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, sampleRate, channelcount);
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bitRate);

            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
        }else {
            //this.musicPath == null; 的情况
            mTrackIndex = -1;
            mMuxerStarted = mIsEOS = false;
            // prepare MediaCodec for AAC encoding of audio data from inernal mic.
            final MediaCodecInfo audioCodecInfo = selectAudioCodec(MIME_TYPE);
            if (audioCodecInfo == null) {
                Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
                return;
            }
            if (DEBUG) Log.i(TAG, "selected codec: " + audioCodecInfo.getName());

            final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1);
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            //audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
            audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
//        audioFormat.setLong(MediaFormat.KEY_MAX_INPUT_SIZE, inputFile.length());
//      audioFormat.setLong(MediaFormat.KEY_DURATION, (long)durationInMs );
            if (DEBUG) Log.i(TAG, "format: " + audioFormat);
            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
            if (DEBUG) Log.i(TAG, "prepare finishing");
            if (mListener != null) {
                try {
                    mListener.onPrepared(this);
                } catch (final Exception e) {
                    Log.e(TAG, "prepare:", e);
                }
            }
        }
    }

    @Override
    protected void startRecording() {
        super.startRecording();
        // create and execute audio capturing thread using internal mic
        if (mAudioThread == null) {
            mAudioThread = new AudioThread();
            mAudioThread.start();
        }
    }

    @Override
    protected void release() {
        mAudioThread = null;
        super.release();
    }

    private static final int[] AUDIO_SOURCES = new int[] {
            MediaRecorder.AudioSource.MIC,
            //MediaRecorder.AudioSource.DEFAULT,
            //MediaRecorder.AudioSource.CAMCORDER,
            //MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            //MediaRecorder.AudioSource.VOICE_RECOGNITION,
    };

    /**
     * Thread to capture audio data from internal mic as uncompressed 16bit PCM data
     * and write them to the MediaCodec encoder
     */
    private class AudioThread extends Thread {
        @Override
        public void run() {
            if (MediaAudioEncoder.this.musicPath!=null) {//增加音乐特效情况
                Log.e(this.getClass().getSimpleName(),
                        Thread.currentThread().getName()+":MediaAudioEncoder->run方法->musicPath not null");

                long pts = 0;

                mAudioDecoder.start();

                int channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

                int mAudioSampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                final int min_buf_size = AudioTrack.getMinBufferSize(mAudioSampleRate,
                        (channelCount == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                        AudioFormat.ENCODING_PCM_16BIT);
                final int max_input_size = mAudioDecoder.getInputBuffers().length;
                int mAudioInputBufSize = min_buf_size > 0 ? min_buf_size * 4 : max_input_size;

                AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        mAudioSampleRate,
                        channelCount == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        mAudioInputBufSize,
                        AudioTrack.MODE_STREAM);
                try {
                    audioTrack.play();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    audioTrack.release();
                }

                ByteBuffer[] inputBuffers = mAudioDecoder.getInputBuffers();
                ByteBuffer[] outputBuffers = mAudioDecoder.getOutputBuffers();
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//                    boolean isEOS = false;
                long startMs = System.currentTimeMillis();
                mediaExtractor.seekTo(startPts,SEEK_TO_CLOSEST_SYNC);

                while (mIsCapturing && !mRequestStop && !mIsEOS) {
                    if(mBlockCapturing) {
                        synchronized (mSync) {
                            try {
                                mSync.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    System.out.println("index===>" + "acde");
                    if (!mIsEOS) {

                        int inIndex = mAudioDecoder.dequeueInputBuffer(0);
                        if (inIndex >= 0) {

                            ByteBuffer buffer = inputBuffers[inIndex];
                            int trackIndex = mediaExtractor.getSampleTrackIndex();

                            System.out.println("index A===>" + mediaExtractor.getSampleTrackIndex());
                            //读取数据
                            int sampleSize = mediaExtractor.readSampleData(buffer, 0);
                            if (sampleSize < 0) {
                                Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                                mAudioDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                mIsEOS = true;
                            } else {
                                mAudioDecoder.queueInputBuffer(inIndex, 0, sampleSize, mediaExtractor.getSampleTime(), 0);
                                // 移动到下一帧
                                mediaExtractor.advance();
                            }
                        }
                    }

                    int outIndex = mAudioDecoder.dequeueOutputBuffer(info, 10000);
                    switch (outIndex) {
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                            outputBuffers = mAudioDecoder.getOutputBuffers();
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            Log.d("DecodeActivity", "New format " + mAudioDecoder.getOutputFormat());
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                            break;
                        default:

                            ByteBuffer buffer = outputBuffers[outIndex];
                            Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + buffer);
                            // We use a very simple clock to keep the video FPS, or the video
                            // playback will be too fast
                            int realBufferSize = info.size;
                            final byte[] chunk = new byte[realBufferSize];
                            int position = buffer.position();
                            buffer.get(chunk);

                            //播放解码后的PCM音频
                            audioTrack.write(chunk, 0, realBufferSize);
                            buffer.position(position);
                            buffer.position();
                            encode(buffer, realBufferSize, getPTSUs());
                            frameAvailableSoon();
                            buffer.clear();
                            mAudioDecoder.releaseOutputBuffer(outIndex, false);
                            break;
                    }

                    // All decoded frames have been rendered, we can stop playing now
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                        break;
                    }
                }
                mAudioDecoder.stop();
                mAudioDecoder.release();
                mediaExtractor.release();
                Log.e("TAG","--->mediaExtractor.release()");

            } else {
                //MediaAudioEncoder.this.musicPath == null 的情况即没有增加音乐特效
                Log.e(this.getClass().getSimpleName(),
                        Thread.currentThread().getName()+":MediaAudioEncoder->run方法->musicPath null");

                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                try {
                    final int min_buffer_size = AudioRecord.getMinBufferSize(
                            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);
                    int buffer_size = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
                    if (buffer_size < min_buffer_size)
                        buffer_size = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;

                    AudioRecord audioRecord = null;
                    for (final int source : AUDIO_SOURCES) {
                        try {
                            audioRecord = new AudioRecord(
                                    source, SAMPLE_RATE,
                                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size);
                            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
                                audioRecord = null;
                        } catch (final Exception e) {
                            audioRecord = null;
                        }
                        if (audioRecord != null) break;
                    }
                    if (audioRecord != null) {
                        try {
                            if (mIsCapturing) {
                                if (DEBUG) Log.v(TAG, "AudioThread:start audio recording");
                                final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
                                int readBytes;
                                int errorCount = 0;
                                audioRecord.startRecording();
                                try {
                                    for (; mIsCapturing && !mRequestStop && !mIsEOS; ) {
                                        // read audio data from internal mic
                                        buf.clear();
                                        //通过 AudioRecord 录音
                                        readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME);
                                        if (readBytes > 0) {
                                            // set audio data to encoder
                                            buf.position(readBytes);
                                            buf.flip();
                                            encode(buf, readBytes, getPTSUs());
                                            frameAvailableSoon();
                                        } else {
                                            errorCount++;
                                            if (errorCount >= 3) {
                                                Log.e(TAG, "audio recorder error..");
                                                mInputError = true;
                                                break;
                                            }
                                        }
                                    }
                                    frameAvailableSoon();
                                } finally {
                                    audioRecord.stop();
                                }
                            }
                        } finally {
                            audioRecord.release();
                        }
                    } else {
                        Log.e(TAG, "failed to initialize AudioRecord");
                    }
                } catch (final Exception e) {
                    Log.e(TAG, "AudioThread#run", e);
                }
                if (DEBUG) Log.v(TAG, "AudioThread:finished");
            }
        }
    }

    /**
     * select the first codec that match a specific MIME type
     * @param mimeType
     * @return
     */
    private static final MediaCodecInfo selectAudioCodec(final String mimeType) {
        if (DEBUG) Log.v(TAG, "selectAudioCodec:");

        MediaCodecInfo result = null;
        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        LOOP:    for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {    // skipp decoder
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (DEBUG) Log.i(TAG, "supportedType:" + codecInfo.getName() + ",MIME=" + types[j]);
                if (types[j].equalsIgnoreCase(mimeType)) {
                    if (result == null) {
                        result = codecInfo;
                        break LOOP;
                    }
                }
            }
        }
        return result;
    }
    public long getSampleTime() {
        if(mediaExtractor!=null){
            return mediaExtractor.getSampleTime();
        }
        return 0;
    }

}
