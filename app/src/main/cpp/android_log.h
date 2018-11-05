//
// Created by Administrator on 2018/10/10.
//
#include <android/log.h>

#ifndef ANDROID_LOG_H
#define ANDROID_LOG_H

#define LOG_TOGGLE 1
#define LOG_TAG "Alame"
#if (LOG_TOGGLE == 1)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#else
#define LOGD(...) NULL
#define LOGI(...)  NULL
#define LOGE(...)  NULL
#endif

#endif //ANDROID_LOG_H
