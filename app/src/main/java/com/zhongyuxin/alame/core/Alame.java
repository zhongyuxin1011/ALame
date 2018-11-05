package com.zhongyuxin.alame.core;

public class Alame {

    static {
        System.loadLibrary("Alame");
    }

    private static final Alame ALAME = new Alame();

    private Alame() {

    }

    public static Alame getAlame() {
        return ALAME;
    }

    public native String version();

    /**
     * @param sampleHz
     * @param channels 1 或 2
     * @param bitrate  <= 320
     * @param mode     STEREO, JOINT_STEREO, DUAL_CHANNEL (not supported), MONO
     * @param quality  quality=0..9.  0=best (very slow).  9=worst.
     *                 recommended:  3     near-best quality, not too slow
     *                 5     good quality, fast
     *                 7     ok quality, really fast
     * @return
     */
    public native long createHandle(int sampleHz, int channels, int bitrate, int mode, int vbr,
                                    int quality);

    public native int destroyHandle(long handle);

    public native int encodeMp3(long handle, byte[] pcm, int pcmLen, byte[] mp3, int mp3Len);

    public native int flush(long handle, byte[] mp3, int mp3Len);
}
