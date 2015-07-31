/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
package harness;

import java.util.concurrent.CountDownLatch;

public abstract class RunContextTest implements Runnable {

    private CountDownLatch latch;
    private int loops;

    public RunContextTest(CountDownLatch latch, int loops) {
        this.latch = latch;
        this.loops = loops;
    }

    @Override
    public void run() {
        for (int i = 0; i < loops; i++) {
            try {
                String d = execute();
                if (!Utils.data.equals(d)) {
                    System.err.println("FAIL: " + d + " != " + Utils.data);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        latch.countDown();
    }

    protected abstract String execute() throws Exception;

    public abstract RunContextTest create(CountDownLatch latch, int loops);
}
