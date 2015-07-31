/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
package harness;

import java.util.concurrent.CountDownLatch;

import jni.JniContext;

public class DoubleCloseContextTest extends RunContextTest {

    public DoubleCloseContextTest(CountDownLatch latch, int loops) {
        super(latch, loops);
    }

    @Override
    protected String execute() throws Exception {
        try (JniContext context = JniContext.create();) {
            String s = context.execute();
            context.close();
            context.close();
            return s;
        }
    }

    @Override
    public RunContextTest create(CountDownLatch latch, int loops) {
        return new DoubleCloseContextTest(latch, loops);
    }

}
