package com.zhongyuxin.alame;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.util.Log;

public class AudioCapture {

    private static final String TAG = AudioCapture.class.getSimpleName();
    private static final int DEFAULT_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public static int DEFAULT_SAMPLE_RATE = 8000;

    private AudioRecord mAudioRecord;
    private int mMinBufferSize;
    private boolean mIsCaptureStarted = false;
    private boolean mRecordFlag = false;
    private AudioCaptureRunnable mCaptureRunnable;
    private OnAudioFrameCapturedListener mAudioFrameCapturedListener;
    private NoiseSuppressor mNoiseSuppressor;

    public boolean isCaptureStarted() {
        return mIsCaptureStarted;
    }

    public void setOnAudioFrameCapturedListener(OnAudioFrameCapturedListener listener) {
        mAudioFrameCapturedListener = listener;
    }

    public boolean startCapture() {
        return startCapture(DEFAULT_SOURCE, DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_CONFIG,
                DEFAULT_AUDIO_FORMAT);
    }

    public boolean startCapture(int sample) {
        return startCapture(DEFAULT_SOURCE, sample, DEFAULT_CHANNEL_CONFIG,
                DEFAULT_AUDIO_FORMAT);
    }

    public boolean startCapture(int sample, int channelConfig) {
        return startCapture(DEFAULT_SOURCE, sample, channelConfig, DEFAULT_AUDIO_FORMAT);

    }

    public boolean startCapture(int audioSource, int sampleRateInHz, int channelConfig, int
            audioFormat) {
        if (mIsCaptureStarted) {
            return false;
        }
        mMinBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        if (mMinBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            return false;
        }

        if (mCaptureRunnable == null) {
            mCaptureRunnable = new AudioCaptureRunnable();
            new Thread(mCaptureRunnable).start();
        }

        mAudioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat,
                mMinBufferSize * 2);
        if (mAudioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            return false;
        }

        mAudioRecord.startRecording();

        if (NoiseSuppressor.isAvailable()) {
            mNoiseSuppressor = NoiseSuppressor.create(mAudioRecord.getAudioSessionId());
            mNoiseSuppressor.setEnabled(true);
        }

        mIsCaptureStarted = true;
        mRecordFlag = true;
        Log.e(TAG, "min buffer size: " + mMinBufferSize);

        return true;
    }

    public void stopCapture() {
        if (!mIsCaptureStarted) {
            return;
        }

        if (mCaptureRunnable != null) {
            mCaptureRunnable.exit();
            mCaptureRunnable = null;
        }

        if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            mRecordFlag = false;
            mAudioRecord.release();
        }

        if (mNoiseSuppressor != null) {
            mNoiseSuppressor.setEnabled(false);
            mNoiseSuppressor.release();
        }

        mAudioRecord = null;
        mNoiseSuppressor = null;
        mIsCaptureStarted = false;
        mAudioFrameCapturedListener = null;
    }

    private class AudioCaptureRunnable implements Runnable {

        private boolean mIsLoopExit = false;
        private int mRet;
        private byte[] mData = new byte[mMinBufferSize > 4096 ? 4096 : mMinBufferSize];

        @Override
        public void run() {
            while (!mIsLoopExit) {
                if (null != mAudioRecord && mRecordFlag) {
                    mRet = mAudioRecord.read(mData, 0, mData.length);
                    if (mRet == AudioRecord.ERROR_INVALID_OPERATION || mRet == AudioRecord
                            .ERROR_BAD_VALUE || mRet == AudioRecord.ERROR) {
                        continue;
                    }
                    if (mAudioFrameCapturedListener != null) {
                        mAudioFrameCapturedListener.onAudioFrameCaptured(mMinBufferSize, mData,
                                DEFAULT_SAMPLE_RATE);
                    }
                }
            }
        }

        private void exit() {
            mIsLoopExit = true;
        }
    }

    public interface OnAudioFrameCapturedListener {

        void onAudioFrameCaptured(int minBuffSize, byte[] data, int sample);

    }
}
