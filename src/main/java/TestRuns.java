/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
import jni.JniContext;

public class TestRuns {
    public static void main(String[] args) {
        System.loadLibrary("test");

        JniContext context = JniContext.create();
        System.err.println(context.execute());
    }
}
