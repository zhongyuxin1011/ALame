#include <jni.h>
#include <string>

#include "lame_global_flags.h"
#include "android_log.h"
#include "lame.h"

extern "C"
{

void convert_short_arr_to_char_arr(short *s_arr, int len, char *c_arr);
void convert_char_arr_to_short_arr(char *c_arr, int len, short *s_arr);

JNIEXPORT jstring JNICALL
Java_com_zhongyuxin_alame_core_Alame_version(JNIEnv *env, jobject instance) {
    return env->NewStringUTF(get_lame_very_short_version());
}

JNIEXPORT jlong JNICALL
Java_com_zhongyuxin_alame_core_Alame_createHandle(JNIEnv *env, jobject instance, jint sampleHz,
                                                  jint channels, jint bitrate, jint mode, jint vbr,
                                                  jint quality, jobjectArray information) {
    lame_global_flags *glf = lame_init();
    if (glf == NULL) {
        return -1;
    }
    lame_set_num_channels(glf, channels);
    lame_set_in_samplerate(glf, sampleHz);
//    lame_set_out_samplerate(glf, sampleHz);
    if (information != NULL) {
        jsize size = env->GetArrayLength(information);
        if (size > 0) {
            jstring title = (jstring)env->GetObjectArrayElement(information, 0);
            const char* vtitle = env->GetStringUTFChars(title, NULL);
            id3tag_set_title(glf, vtitle);
            env->ReleaseStringUTFChars(title, vtitle);
        }
        if (size > 1) {
            jstring artist = (jstring)env->GetObjectArrayElement(information, 1);
            const char* vartist = env->GetStringUTFChars(artist, NULL);
            id3tag_set_artist(glf, vartist);
            env->ReleaseStringUTFChars(artist, vartist);
        }
        if (size > 2) {
            jstring album = (jstring)env->GetObjectArrayElement(information, 2);
            const char* valbum = env->GetStringUTFChars(album, NULL);
            id3tag_set_album(glf, valbum);
            env->ReleaseStringUTFChars(album, valbum);
        }
        if (size > 3) {
            jstring year = (jstring)env->GetObjectArrayElement(information, 3);
            const char* vyear = env->GetStringUTFChars(year, NULL);
            id3tag_set_year(glf, vyear);
            env->ReleaseStringUTFChars(year, vyear);
        }
        if (size > 4) {
            jstring comment = (jstring)env->GetObjectArrayElement(information, 4);
            const char* vcomment = env->GetStringUTFChars(comment, NULL);
            id3tag_set_comment(glf, vcomment);
            env->ReleaseStringUTFChars(comment, vcomment);
        }
        env->DeleteLocalRef(information);
    }
    MPEG_mode mpeg_mode = STEREO;
    switch (mode) {
        case 1:
            mpeg_mode = JOINT_STEREO;
            break;
        case 2:
            mpeg_mode = DUAL_CHANNEL;
            break;
        case 3:
            mpeg_mode = MONO;
            break;
        case 4:
            mpeg_mode = NOT_SET;
            break;
        case 5:
            mpeg_mode = MAX_INDICATOR;
            break;
        default:
            break;
    }
    lame_set_mode(glf, mpeg_mode);
    lame_set_quality(glf, quality);
    vbr_mode v_mode = vbr_default;
    switch (vbr) {
        case 0:
            v_mode = vbr_off;
            break;
        case 1:
            v_mode = vbr_mt;
            break;
        case 2:
            v_mode = vbr_rh;
            break;
        case 3:
            v_mode = vbr_abr;
            break;
        case 4:
            v_mode = vbr_mtrh;
            break;
        case 5:
            v_mode = vbr_max_indicator;
            break;
        default:
            break;
    }
    lame_set_VBR(glf, v_mode);
    if (vbr == 0) {
        lame_set_brate(glf, bitrate);
    }
    int ret = lame_init_params(glf);
    if (ret == -1) {
        lame_close(glf);
        return -1;
    }
//    int delay = lame_get_encoder_delay(glf);
//    LOGD("%d delay: %d", __LINE__, delay);
//    int padding = lame_get_encoder_padding(glf);
//    LOGD("%d padding: %d", __LINE__, padding);
//    float compression_ratio = lame_get_compression_ratio(glf);
//    LOGD("%d compression_ratio: %f", __LINE__, compression_ratio);
//    int frameNum = lame_get_frameNum(glf);
//    LOGD("%d frameNum: %d", __LINE__, frameNum);
//    int size_mp3buffer = lame_get_size_mp3buffer(glf);
//    LOGD("%d size_mp3buffer: %d", __LINE__, size_mp3buffer);
//    int framesize = lame_get_framesize(glf);
//    LOGD("%d framesize: %d", __LINE__, framesize);
    return (long long) glf;
}

JNIEXPORT jint JNICALL
Java_com_zhongyuxin_alame_core_Alame_destroyHandle(JNIEnv *env, jobject instance, jlong handle) {
    if (handle == -1) {
        return -1;
    }
    lame_global_flags *glf = (lame_global_flags *) handle;
    if (glf == NULL) {
        return -1;
    }
    lame_close(glf);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_zhongyuxin_alame_core_Alame_encodeMp3(JNIEnv *env, jobject instance, jlong handle,
                                               jbyteArray pcm_, jint pcmLen, jbyteArray mp3_,
                                               jint mp3Len) {
    if (handle == -1) {
        return -5;
    }
    lame_global_flags *glf = (lame_global_flags *) handle;
    if (glf == NULL) {
        return -6;
    }
    jbyte *pcm = env->GetByteArrayElements(pcm_, NULL);
    jbyte *mp3 = env->GetByteArrayElements(mp3_, NULL);
    int16_t pcm_s[pcmLen / 2];
    convert_char_arr_to_short_arr((char *) pcm, pcmLen, pcm_s);

    int ret;
    if (glf->mode == MONO) {
        ret = lame_encode_buffer(glf, pcm_s, NULL, pcmLen / 2, (u_char *) mp3, mp3Len);
    } else {
        ret = lame_encode_buffer_interleaved(glf, pcm_s, pcmLen / 4, (u_char *) mp3, mp3Len);
    }
    env->ReleaseByteArrayElements(pcm_, pcm, 0);
    env->ReleaseByteArrayElements(mp3_, mp3, 0);
    return ret;
}

JNIEXPORT jint JNICALL
Java_com_zhongyuxin_alame_core_Alame_flush(JNIEnv *env, jobject instance, jlong handle,
                                           jbyteArray mp3_, jint mp3Len) {
    if (handle == -1) {
        return -1;
    }
    lame_global_flags *glf = (lame_global_flags *) handle;
    if (glf == NULL) {
        return -1;
    }
    jbyte *mp3 = env->GetByteArrayElements(mp3_, NULL);
    int ret = lame_encode_flush(glf, (u_char *) mp3, mp3Len);
    env->ReleaseByteArrayElements(mp3_, mp3, 0);
    return ret;
}

/*
 * Convert
 */
void convert_char_arr_to_short_arr(char *c_arr, int len, short *s_arr) {
    int i = 0;
    for (; i < len / 2; i++) {
        s_arr[i] = (short) (c_arr[i * 2 + 1] << 8 | (c_arr[2 * i] & 0xff));
    }
}

/*
 * Convert
 */
void convert_short_arr_to_char_arr(short *s_arr, int len, char *c_arr) {
    int i = 0;
    for (; i < len; i++) {
        int j = 0;
        for (; j < 2; j++) {
            c_arr[2 * i + j] = (char) (s_arr[i] >> (j == 0 ? 0 : 8));
        }
    }
}
}