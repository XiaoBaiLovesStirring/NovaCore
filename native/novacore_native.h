#ifndef NOVACORE_NATIVE_H
#define NOVACORE_NATIVE_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL
Java_com_novacore_agent_NativeThreadAffinity_setThreadAffinity(
    JNIEnv *env, jclass cls, jlong threadId, jlong coreMask);

JNIEXPORT jlong JNICALL
Java_com_novacore_agent_NativeThreadAffinity_getThreadAffinity(
    JNIEnv *env, jclass cls, jlong threadId);

JNIEXPORT jint JNICALL
Java_com_novacore_agent_NativeThreadAffinity_getAvailableCores(
    JNIEnv *env, jclass cls);

JNIEXPORT jboolean JNICALL
Java_com_novacore_agent_NativeThreadAffinity_setThreadPriority(
    JNIEnv *env, jclass cls, jlong threadId, jint priority);

JNIEXPORT void JNICALL
Java_com_novacore_agent_NativeThreadAffinity_configureGC(
    JNIEnv *env, jclass cls, jint gcThreads, jlong maxPauseNanos, jint heapOccupancyPercent);

JNIEXPORT void JNICALL
Java_com_novacore_agent_NativeThreadAffinity_preallocateMemory(
    JNIEnv *env, jclass cls, jlong sizeBytes);

#ifdef __cplusplus
}
#endif

#endif // NOVACORE_NATIVE_H