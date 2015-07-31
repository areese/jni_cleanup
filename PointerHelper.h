/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
#ifndef __POINTER_HELPER_H__
#define __POINTER_HELPER_H__

#include <jni.h>

class LeakContext;

/**
 * This is just an easy place to namespace functions for use with the Pointer and PointerAccess java classes.
 */
class pointerHelper {
public:
	static jlong getPointer(JNIEnv *jenv, jobject pointer);
	static jobject getPointerFromContext(JNIEnv *jenv, jobject context);
	static jobject createPointer(JNIEnv *jenv, void *cPointer);
	static jobject createJavaContextObject(JNIEnv *jenv, jclass contextClass,
			jmethodID constructorId, LeakContext *c_context);
};

#endif //__POINTER_HELPER_H__
