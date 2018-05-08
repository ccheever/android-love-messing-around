//
// Created by Charles Cheever on 5/7/18.
//


#include <jni.h>
#include <string>

#include <stdlib.h>                             /* For function exit() */
#include <stdio.h>                              /* For input/output */

extern "C" {
JNIEXPORT jstring

JNICALL Java_org_love2d_android_GameActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello Love from C++";
    return env->NewStringUTF(hello.c_str());
}
}
