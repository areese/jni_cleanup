/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
package yjava.jni.cleaner;

public interface PointerAccess {
    int getLeakIndex();

    Pointer getPointer();

    void erasePointer(int leakIndex);

}
