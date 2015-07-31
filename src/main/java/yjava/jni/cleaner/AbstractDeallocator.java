/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
package yjava.jni.cleaner;

public abstract class AbstractDeallocator {
    protected final LostReferenceCounter counter;

    protected AbstractDeallocator(LostReferenceCounter counter) {
        this.counter = counter;
    }

    /**
     * This is only called from the deallocator to free memory, so it's a leak.
     * 
     * @param p pointer to delete.
     */
    protected void delete(Pointer p) {
        if (0 == p.pointer) {
            return;
        }

        int leakIndex = free(p);
        p.setPointer(0);
        if (null != counter) {
            counter.lost(leakIndex);
        }
    }

    /**
     * This is called to free a pointer
     * 
     * @param pointer to free.
     * @return the leak index for the LostReferenceCounter
     */
    protected abstract int free(Pointer p);

    public LostReferenceCounter getLostReferenceCounter() {
        return counter;
    }

}
