/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
package harness;

import java.util.concurrent.CountDownLatch;

import jni.JniContext;

public class ClosingContextTest extends RunContextTest {

    public ClosingContextTest(CountDownLatch latch, int loops) {
        super(latch, loops);
    }

    @Override
    protected String execute() throws Exception {
        try (JniContext context = JniContext.create();) {
            return context.execute();
        }
    }

    @Override
    public RunContextTest create(CountDownLatch latch, int loops) {
        return new ClosingContextTest(latch, loops);
    }

}
