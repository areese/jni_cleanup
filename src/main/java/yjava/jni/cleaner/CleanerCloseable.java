/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
package yjava.jni.cleaner;


public interface CleanerCloseable extends CheckedCloseable {
    /**
     * You MUST close this or it WILL LEAK memory.
     */
    @Override
    public void close();

    /**
     * Throw if the certificate has been released.
     * 
     * @throws IllegalStateException if the certificate has been released.
     */
    public void validate();

    public int getLeakIndex();

    @Override
    public boolean isClosed();
}
