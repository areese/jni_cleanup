/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
package yjava.jni.cleaner;


/**
 * This is an abstract class that aids in checking if something has already been closed, and throws an exception if it's
 * been closed.
 * 
 * @author areese
 * 
 */
public abstract class ClosedBaseChecked implements CheckedCloseable {
    protected volatile boolean closed = false;

    public ClosedBaseChecked() {

    }

    /**
     * 
     * @param closed true if this object starts off closed, false if it starts opened
     */
    public ClosedBaseChecked(boolean closed) {
        this.closed = closed;
    }

    /**
     * Check if this object is closed possibly throw an IllegalStateException if it is closed.
     * 
     * @return the closed value
     * @throws IllegalStateException if constructed to throw when you try and use a closed object.
     */
    @Override
    public synchronized boolean isClosed() throws IllegalStateException {
        return closed;
    }

    /**
     * If the object is not already closed, then call the subclasses release function.
     */
    @Override
    public synchronized void close() {
        if (!isClosed()) {
            // closed needs to be set to the return value from release so
            // reference counted closes.
            closed = release();
            // if (logCloses && logger.isTraceEnabled())
            // logger.trace("Closed " + this.getClass().getCanonicalName()
            // + " with origCptr: " + origCPtr);
        }
    }

    /**
     * The closing class should implement release and return true or false based on if the object is closed.
     * 
     * For most cases, one should just return true, and that will mark the object as closed. For reference counted and
     * shared subclasses, the subclass should check it's reference counting and decide if the object is considered
     * closed or if it is still open.
     * 
     * @return the close status, true if the object is closed when release is done, false if the object can still have
     *         closed called.
     */
    protected abstract boolean release();
}
