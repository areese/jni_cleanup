/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
#include <jni.h>
#include <assert.h>
#include <stdio.h>

#include "jni_JniContextAccess.h"
#include "jni_helper_defines.h"

#include "LeakContext.h"
#include "PointerHelper.h"

/**
 * Declare a static variable for the context class we'll be using.
 */
DECLARE_CACHED_CLASS(jniContextClass, "jni/JniContext");
DECLARE_CACHED_METHOD_ID(jniContextClass, jniContextCtorID, "<init>",
		"(Lyjava/jni/cleaner/Pointer;)V");

#define RETURN_NULL_AND_THROW_IF_CONTEXT_NULL(jniContext, mesg) {\
    if (jenv->ExceptionCheck()) {\
        return 0;\
    }\
    if (0==jniContext) {\
        ThrowNullPointerException(jenv, mesg);\
        abort(); \
        return 0;\
    }\
    if (0==jniContext->getPointer()) {\
        ThrowNullPointerException(jenv, mesg);\
        abort(); \
        return 0;\
    }\
}

/**
 * This is the struct that we are wrapping.
 * In the real world, it's typically an opaque struct that belongs to some library and you call something like:
 * libAllocat(&c_context) and libRelease(&c_context);
 */
struct C_CONTEXT {
	int someData;
	char *message;
	size_t len;
};

class jniContextStruct: public LeakContext {
public:
	jniContextStruct(int leakIndex, C_CONTEXT* pointer) :
			LeakContext(leakIndex, pointer,
					&jniContextStruct::jniContextStructFree) {
	}

	C_CONTEXT *getPointer() {
		return reinterpret_cast<C_CONTEXT*>(pointer);
	}

	static void jniContextStructFree(void *pointer) {
		if (NULL == pointer) {
			return;
		}

		C_CONTEXT *context = reinterpret_cast<C_CONTEXT*>(pointer);
		if (NULL != context->message) {
			free(context->message);
			context->message = NULL;
		}

		free(context);
	}

	static jniContextStruct *allocate(JNIEnv *jenv, int leakIndex,
			C_CONTEXT *c_context) {
		RETURN_NULL_IF_EXCEPTION_OR_NULL(c_context);

		jniContextStruct *jniContext = new jniContextStruct(leakIndex,
				c_context);
		if (NULL == jniContext) {
			jniContextStruct::jniContextStructFree(c_context);
			c_context = NULL;

			ThrowException(jenv, NULL_POINTER_EXCEPTION,
					"Failed to allocate message");

			return NULL;
		}

		return jniContext;
	}
};

/*
 * Class:     jni_JniContextAccess
 * Method:    createContext
 * Signature: (I)Ljni/JniContext;
 */
JNIEXPORT jobject
JNICALL Java_jni_JniContextAccess_createContext(JNIEnv *jenv, jclass thisClass,
		jint leakIndex) {

	GET_CACHED_CLASS(jenv, jniContextClass);
	RETURN_NULL_IF_EXCEPTION_OR_NULL(jniContextClass);

	GET_CACHED_METHOD_ID(jenv, jniContextCtorID);
	RETURN_NULL_IF_EXCEPTION_OR_NULL(jniContextCtorID);

	C_CONTEXT *c_context = (C_CONTEXT *) calloc(1, sizeof(C_CONTEXT));
	RETURN_NULL_IF_EXCEPTION_OR_NULL(c_context);

	c_context->someData = 0xC001C0DE;
	c_context->len = 1024;
	c_context->message = (char*) calloc(c_context->len, sizeof(char));
	if (NULL == c_context->message) {
		jniContextStruct::jniContextStructFree(c_context);
		c_context = NULL;

		ThrowException(jenv, NULL_POINTER_EXCEPTION,
				"Failed to allocate message");

		return NULL;
	}

	snprintf(c_context->message, c_context->len, "This is some super cool jni");

	jniContextStruct *jniContext = jniContextStruct::allocate(jenv, leakIndex,
			c_context);
	if (NULL == jniContext) {
		jniContextStruct::jniContextStructFree(c_context);
		c_context = NULL;

		ThrowException(jenv, NULL_POINTER_EXCEPTION,
				"Failed to allocate message");

		return NULL;
	}

	return pointerHelper::createJavaContextObject(jenv, jniContextClass,
			jniContextCtorID, jniContext);
}

/*
 * Class:     jni_JniContextAccess
 * Method:    execute
 * Signature: (Ljni/JniContext;)Ljava/lang/String;
 */
JNIEXPORT jstring
JNICALL Java_jni_JniContextAccess_execute(JNIEnv *jenv, jclass thisClass,
		jobject contextObject) {

	// get the pointer out.
	RETURN_NULL_AND_THROW_IF_NULL(contextObject, "null context");

	jniContextStruct *jniContext = (jniContextStruct *) LeakContext::getContext(
			jenv, contextObject);
	RETURN_NULL_AND_THROW_IF_CONTEXT_NULL(jniContext, "null jniContext");

	if (NULL == jniContext->getPointer()->message) {
		return NULL;
	}

	return jenv->NewStringUTF((const char *) jniContext->getPointer()->message);
}

/*
 * Class:     jni_JniContextAccess
 * Method:    release
 * Signature: (Lcleaner/Pointer;)I
 */
JNIEXPORT jint
JNICALL Java_jni_JniContextAccess_release(JNIEnv *jenv, jclass thisClass,
		jobject pointerObject) {
	if (NULL == pointerObject) {
		return 0;
	}

	jniContextStruct *jniContext =
			(jniContextStruct *) jniContextStruct::getContextPointer(jenv,
					pointerObject);
	if (NULL == jniContext) {
		return NULL;
	}

	return jniContextStruct::release(jniContext, 52);

}

/*
 * Class:     jni_JniContextAccess
 * Method:    getLeakIndex
 * Signature: (Lcleaner/Pointer;)I
 */
JNIEXPORT jint
JNICALL Java_jni_JniContextAccess_getLeakIndex(JNIEnv *jenv, jclass thisClass,
		jobject contextObject) {
	if (NULL == contextObject) {
		return -1;
	}

	jniContextStruct *jniContext = (jniContextStruct *) LeakContext::getContext(
			jenv, contextObject);
	RETURN_NULL_AND_THROW_IF_CONTEXT_NULL(jniContext, "null jniContext");

	return jniContext->getLeakIndex();
}
