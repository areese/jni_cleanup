/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
package yjava.jni.cleaner;



/**
 * This class takes advantage of the Cleaner API inside the GC to do native memory collection without a finalizer. The
 * Cleaner is a special type of PhatomReference and when the GC see's it during reference processing, it will call the
 * runnable method. This allows us to call a special JNI Free function that is implemented in Destructor, which only
 * takes a long which is actually a C pointer.
 * 
 * This also deals with ensuring that we don't double free if the resource has been closed.
 * 
 * @author areese
 * 
 */
public abstract class DeallocatingClosedBase extends ClosedBaseChecked implements PointerAccess {
    protected volatile Pointer pointer;
    protected volatile AbstractDeallocator destructor;
    protected volatile RunnableDestructor runnableDestructor;
    @SuppressWarnings("restriction")
    protected volatile sun.misc.Cleaner cleaner;

    @SuppressWarnings("restriction")
    protected DeallocatingClosedBase(Pointer pointer, AbstractDeallocator destructor) {
        this.pointer = pointer;
        this.destructor = destructor;
        this.runnableDestructor = new RunnableDestructor(destructor, this.pointer);

        if (null != this.runnableDestructor) {
            this.cleaner = sun.misc.Cleaner.create(this, this.runnableDestructor);
        } else {
            this.cleaner = null;
        }
    }

    /**
     * This is only ever called from close(), so it's not leaked.
     */
    @Override
    protected final boolean release() {
        if (null != pointer) {
            int leakIndex = release(pointer);

            // pointer is free set it to 0.
            erasePointer(leakIndex);
        }

        return true;
    }

    /**
     * Throw if the pointer has been released.
     * 
     * @throws IllegalStateException if the pointer has already been released.
     */
    public void validate() throws IllegalStateException {
        if (null == pointer || 0 == pointer.pointer) {
            throw new IllegalStateException("Context has been released");
        }
    }


    protected synchronized int release(final Pointer pointer) {
        // call validate here because this should never be called with a pointer
        // that is 0.
        validate();
        int leakIndex = destructor.free(pointer);
        erasePointer(leakIndex);
        return leakIndex;
    }


    @Override
    public abstract int getLeakIndex();

    @Override
    public Pointer getPointer() {
        return pointer;
    }

    @Override
    public void erasePointer(int leakIndex) {
        if (null == this.pointer) {
            this.closed = true;
            return;
        }

        this.pointer.pointer = 0;
        this.pointer = null;
        if (null != this.destructor) {

            // note this wasn't leaked.
            if (null != this.destructor.counter) {
                this.destructor.counter.close(leakIndex);
            }

            // ensure the deallocator doesn't think it owns the pointer.
            this.runnableDestructor.pointer.pointer = 0L;
            this.runnableDestructor.pointer = null;
            this.runnableDestructor.destructor = null;

            // releases the deallocator
            this.destructor = null;
        }

        // release the cleaner
        this.cleaner = null;
        this.closed = true;
    }

}
