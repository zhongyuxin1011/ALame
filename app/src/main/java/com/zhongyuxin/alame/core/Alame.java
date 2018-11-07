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

    /**
     * 获取版本号
     *
     * @return 版本号
     */
    public native String version();

    /**
     * 创建句柄
     *
     * @param sampleHz 采样率
     * @param channels 声道数：1 或 2
     * @param bitrate  比特率：<= 320
     * @param mode     模式：STEREO, JOINT_STEREO, DUAL_CHANNEL (not supported), MONO
     * @param quality  质量：quality=0..9.  0=best (very slow).  9=worst.
     *                 recommended:  3     near-best quality, not too slow
     *                 5     good quality, fast
     *                 7     ok quality, really fast
     * @return 句柄值，-1失败
     */
    public native long createHandle(int sampleHz, int channels, int bitrate, int mode, int vbr,
                                    int quality);

    /**
     * 销毁句柄
     *
     * @param handle 句柄值
     * @return 结果：0成功   1失败
     */
    public native int destroyHandle(long handle);

    /**
     * 编码MP3
     *
     * @param handle 句柄值
     * @param pcm    PCM数据
     * @param pcmLen PCM数据长度
     * @param mp3    存放MP3数据
     * @param mp3Len 存放MP3数据长度
     * @return 编码后MP3数据长度：number of bytes output in mp3buf. Can be 0
     * -1:  mp3buf was too small
     * -2:  malloc() problem
     * -3:  lame_init_params() not called
     * -4:  psycho acoustic problems
     */
    public native int encodeMp3(long handle, byte[] pcm, int pcmLen, byte[] mp3, int mp3Len);

    /**
     * 刷新MP3编码缓冲数据
     *
     * @param handle 句柄值
     * @param mp3    存放MP3数据
     * @param mp3Len 存放MP3数据长度
     * @return MP3编码缓冲数据长度：return code = number of bytes output to mp3buf. Can be 0
     */
    public native int flush(long handle, byte[] mp3, int mp3Len);
}
