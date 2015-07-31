/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
package yjava.jni.cleaner;

class RunnableDestructor implements Runnable {

    volatile Pointer pointer;
    volatile AbstractDeallocator destructor;

    public RunnableDestructor(AbstractDeallocator destructor, Pointer pointer) {
        this.pointer = pointer;
        this.destructor = destructor;
    }

    @Override
    public void run() {
        if (null == this.destructor || null == this.pointer || 0 == this.pointer.pointer) {
            return;
        }

        // System.err.println("Finalizer run: " + swigCPtr + " " + swigCMemOwn);
        this.destructor.delete(this.pointer);
        this.pointer.pointer = 0;
        this.pointer = null;
    }
}
