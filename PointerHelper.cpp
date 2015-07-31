/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
#include <jni.h>
#include <assert.h>

#include "PointerHelper.h"
#include "LeakContext.h"
#include "jni_helper_defines.h"

/**
 * Declare a static variable for the Pointer class that lies between the Java and us.
 */
DECLARE_CACHED_CLASS(pointerClass, "yjava/jni/cleaner/Pointer");
DECLARE_CACHED_METHOD_ID(pointerClass, pointerCtorId, "<init>", "(J)V");
DECLARE_CACHED_METHOD_ID(pointerClass, pointerGetPointerId, "getCPointer",
		"()J");

/**
 * Declare a static variable for the PointerAccess interface that lies between the Java and us.
 * This is used to ask a context for the pointer.
 */
DECLARE_CACHED_CLASS(pointerAccessClass, "yjava/jni/cleaner/PointerAccess");
DECLARE_CACHED_METHOD_ID(pointerAccessClass, pointerAccessGetPointerId,
		"getPointer", "()Lyjava/jni/cleaner/Pointer;");

DECLARE_CACHED_CLASS(classClass, "java/lang/Class");
DECLARE_CACHED_METHOD_ID(classClass, classGetNameId, "getName", "()Ljava/lang/String;");

bool throwWithClassInformation(JNIEnv *jenv, jobject obj, const char *className,
		jclass klass, const char *mesg) {
	if (JNI_TRUE == jenv->IsInstanceOf(obj, klass)) {
		return true;
	}

	char exceptionMesg[1024] = { 0, };
	snprintf(exceptionMesg, sizeof(exceptionMesg) - 1,
			"%s, expected %s could not find actual classname", mesg, className);

	jclass cls = jenv->GetObjectClass(obj);
	if (NULL == cls || jenv->ExceptionCheck()) {
		ThrowException(jenv, CLASS_CAST_EXCEPTION, exceptionMesg);
		return false;
	}

	jmethodID mid = jenv->GetMethodID(cls, "getClass", "()Ljava/lang/Class;");
	if (NULL == mid || jenv->ExceptionCheck()) {
		ThrowException(jenv, CLASS_CAST_EXCEPTION, exceptionMesg);
		return false;
	}

	jobject clsObj = jenv->CallObjectMethod(obj, mid);
	if (NULL == clsObj || jenv->ExceptionCheck()) {
		ThrowException(jenv, CLASS_CAST_EXCEPTION, exceptionMesg);
		return false;
	}

	GET_CACHED_CLASS(jenv, classClass);
	if (NULL == classClass || jenv->ExceptionCheck()) {
		ThrowException(jenv, CLASS_CAST_EXCEPTION, exceptionMesg);
		return false;
	}

	GET_CACHED_METHOD_ID(jenv, classGetNameId);
	if (NULL == classGetNameId || jenv->ExceptionCheck()) {
		ThrowException(jenv, CLASS_CAST_EXCEPTION, exceptionMesg);
		return false;
	}

	// almost there, we need to call getName.
	jstring name = (jstring) jenv->CallObjectMethod(clsObj, classGetNameId);
	if (NULL == name || jenv->ExceptionCheck()) {
		ThrowException(jenv, CLASS_CAST_EXCEPTION, exceptionMesg);
		return false;
	}

	// we finally have a name
	ScopedStringUTFChars nameString(jenv, name);
	memset(exceptionMesg, 0, sizeof(exceptionMesg));
	snprintf(exceptionMesg, sizeof(exceptionMesg) - 1,
			"%s, expected %s but was %s", mesg, className, nameString.get());
	ThrowException(jenv, CLASS_CAST_EXCEPTION, exceptionMesg);

	return false;

}

jobject pointerHelper::createJavaContextObject(JNIEnv *jenv,
		jclass contextClass, jmethodID constructorId, LeakContext *c_context) {

	if (LeakContext::failAndFreeIfExceptionOrNull(jenv, contextClass,
			c_context)) {
		return NULL;
	}

	if (LeakContext::failAndFreeIfExceptionOrNull(jenv, constructorId,
			c_context)) {
		return NULL;
	}

	/* A Java Context Object is:  Context -> Pointer -> c_pointer */
	jobject newPointer = pointerHelper::createPointer(jenv, c_context);
	if (LeakContext::failAndFreeIfExceptionOrNull(jenv, newPointer,
			c_context)) {
		return NULL;
	}

	jobject jresult = jenv->NewObject(contextClass, constructorId, newPointer);
	if (LeakContext::failAndFreeIfExceptionOrNull(jenv, jresult, c_context)) {
		return NULL;
	}

	return jresult;
}

jlong pointerHelper::getPointer(JNIEnv *jenv, jobject pointer) {
	if (NULL == pointer) {
		abort();
		return 0;
	}

	GET_CACHED_CLASS(jenv, pointerClass);
	RETURN_NULL_IF_EXCEPTION_OR_NULL (pointerClass);

	GET_CACHED_METHOD_ID(jenv, pointerGetPointerId);
	RETURN_NULL_IF_EXCEPTION_OR_NULL (pointerGetPointerId);

	if (!throwWithClassInformation(jenv, pointer, "yjava/jni/cleaner/Pointer",
			pointerClass, "oops not a ")) {
		return NULL;
	}

	return jenv->CallLongMethod(pointer, pointerGetPointerId);
}

jobject pointerHelper::getPointerFromContext(JNIEnv *jenv, jobject context) {
	RETURN_NULL_IF_EXCEPTION_OR_NULL(context);

	GET_CACHED_CLASS(jenv, pointerAccessClass);
	RETURN_NULL_IF_EXCEPTION_OR_NULL (pointerClass);

	GET_CACHED_METHOD_ID(jenv, pointerAccessGetPointerId);
	RETURN_NULL_IF_EXCEPTION_OR_NULL (pointerAccessGetPointerId);

	if (!throwWithClassInformation(jenv, context,
			"yjava/jni/cleaner/PointerAccess", pointerAccessClass,
			"oops not a ")) {
		return NULL;
	}

	return jenv->CallObjectMethod(context, pointerAccessGetPointerId);
}

jobject pointerHelper::createPointer(JNIEnv *jenv, void *cPointer) {
	GET_CACHED_CLASS(jenv, pointerClass);
	RETURN_NULL_IF_EXCEPTION_OR_NULL (pointerClass);

	GET_CACHED_METHOD_ID(jenv, pointerCtorId);
	RETURN_NULL_IF_EXCEPTION_OR_NULL (pointerCtorId);

//	fprintf(stderr, "%d: context: %p\n", __LINE__, cPointer);

	return jenv->NewObject(pointerClass, pointerCtorId,
			reinterpret_cast<jlong>(cPointer));
}
