/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
#ifndef __LEAK_CONTEXT_H__
#define __LEAK_CONTEXT_H__

#include <jni.h>
#include "jni_helper_defines.h"
#include "PointerHelper.h"

/** Typedef for the function that can free C_CONTEXT */
typedef void (*freeFunctionPtr)(void*);

#define HEADER_CHECK 0xFEEDBEEF
#define FOOTER_CHECK 0xBEEFC0DE
#define DEAD_HEADER 0xDEADC0DE
#define DEAD_FOOTER 0xDEADBEEF

/**
 * This is the base class that does the heavy lifting around keeping track of the leaks, and returning the C struct you are wrapping.
 */
class LeakContext {
protected:
	unsigned long header;
	int leakIndex;
	void *pointer;
	freeFunctionPtr freeFunc;
	unsigned long footer;

public:
	LeakContext(int leakIndex, freeFunctionPtr freeFunc) :
			header(HEADER_CHECK), leakIndex(leakIndex), pointer(NULL), freeFunc(
					freeFunc), footer(FOOTER_CHECK) {
		if (NULL == freeFunc) {
			abort();
		}
	}

	LeakContext(int leakIndex, void* pointer, freeFunctionPtr freeFunc) :
			header(HEADER_CHECK), leakIndex(leakIndex), pointer(pointer), freeFunc(
					freeFunc), footer(FOOTER_CHECK) {
		if (NULL == freeFunc) {
			abort();
		}
	}

	int getLeakIndex() {
		return leakIndex;
	}

	static LeakContext *getLeakContext(jlong pointer) {
		LeakContext *context = reinterpret_cast<LeakContext*>(pointer);
		assert(context->header == HEADER_CHECK);
		assert(context->footer == FOOTER_CHECK);
		return context;
	}

	static int release(jlong pointer, int r) {
		return release(getLeakContext(pointer), r);
	}

	static int release(LeakContext *context, int r) {
		if (NULL == context) {
			return -1;
		}

		assert(context->header == HEADER_CHECK);
		assert(context->footer == FOOTER_CHECK);
		if (NULL != context->pointer) {
			(*(context->freeFunc))(context->pointer);
			context->pointer = NULL;
		}
		context->header = DEAD_HEADER;
		context->footer = DEAD_FOOTER;

		delete (context);

		return 0;
	}

	/**
	 * @return false on failure, true on everything is ok
	 *
	 * jresult is any pointer.  jobject/jmethodID/jclass are all pointers
	 *
	 */
	static bool failAndFreeIfExceptionOrNull(JNIEnv *jenv, void *jresult,
			LeakContext *context) {
		if (NULL == jresult || jenv->ExceptionCheck()) {
			if (NULL != context) {
				// this will show up as a leak, not much we can do here.
				release(context, 3);
			}
			return true;
		}
		return false;
	}

	static LeakContext *getContextPointer(JNIEnv *jenv, jobject pointerObject) {
		jlong pointer = pointerHelper::getPointer(jenv, pointerObject);
		return reinterpret_cast<LeakContext *>(pointer);
	}

	static LeakContext *getContext(JNIEnv *jenv, jobject context) {
		// pointerHelper needs a PointerAccess object.
		jobject pointer = pointerHelper::getPointerFromContext(jenv, context);
		RETURN_NULL_AND_THROW_IF_NULL(pointer,
				"LeakContext::getContext null pointer");

		jlong cPointer = pointerHelper::getPointer(jenv, pointer);
		RETURN_NULL_AND_THROW_IF_NULL(cPointer,
				"LeakContext::getContext null cPointer");

		LeakContext *lc = reinterpret_cast<LeakContext*>(cPointer);
		assert(lc->header == HEADER_CHECK);
		assert(lc->footer == FOOTER_CHECK);

		return lc;
	}
};

#endif //__LEAK_CONTEXT_H__
