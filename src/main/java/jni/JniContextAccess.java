/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
package jni;

import yjava.jni.cleaner.Pointer;

public class JniContextAccess {
    static {
        System.loadLibrary("test");
    }

    static final native JniContext createContext(int leakIndex);

    static final native String execute(JniContext context);

    static final native int release(Pointer pointer);

    static final native int getLeakIndex(Pointer pointer);
}
