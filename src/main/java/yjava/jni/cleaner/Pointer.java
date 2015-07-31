/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
package yjava.jni.cleaner;

public class Pointer implements PointerAccess {
    @Override
    public String toString() {
        return "Pointer [pointer=" + Long.toHexString(pointer) + "]";
    }

    public long pointer;

    public Pointer(long pointer) {
        this.pointer = pointer;
    }

    public long getCPointer() {
        return pointer;
    }

    public void setPointer(long p) {
        this.pointer = p;
    }

    @Override
    public int getLeakIndex() {
        throw new IllegalAccessError();
    }

    @Override
    public Pointer getPointer() {
        return this;
    }

    @Override
    public void erasePointer(int leakIndex) {
        throw new IllegalAccessError();
    }
}
