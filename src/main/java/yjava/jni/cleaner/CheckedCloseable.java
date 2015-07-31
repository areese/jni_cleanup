/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
package yjava.jni.cleaner;

import java.io.Closeable;

public interface CheckedCloseable extends Closeable {
    @Override
    public void close();

    public boolean isClosed();
}
