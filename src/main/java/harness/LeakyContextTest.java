/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
package harness;

import java.util.concurrent.CountDownLatch;

import jni.JniContext;

public class LeakyContextTest extends RunContextTest {

    public LeakyContextTest(CountDownLatch latch, int count) {
        super(latch, count);
    }

    @Override
    protected String execute() throws Exception {
        String decode = null;
        JniContext context = JniContext.create();
        decode = context.execute();
        return decode;
    }

    @Override
    public RunContextTest create(CountDownLatch latch, int count) {
        return new LeakyContextTest(latch, count);
    }

}
