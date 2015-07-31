/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
package harness;

import java.util.concurrent.CountDownLatch;

public class TestContextLeaks {
    public static void main(String[] args) throws Exception {
        if (args.length <= 0) {
            args = new String[] {"leak"};
        }

        if (args.length <= 1) {
            args = new String[] {args[0], "100"};
        }

        if (args.length <= 2) {
            args = new String[] {args[0], args[1], "10000"};
        }

        int threadCount = Integer.parseInt(args[1]);

        RunContextTest r = null;
        switch (args[0]) {
            case "leak":
                r = new LeakyContextTest(null, 0);
                break;

            case "close":
                r = new ClosingContextTest(null, 0);
                break;

            case "dbl":
                r = new DoubleCloseContextTest(null, 0);
                break;
        }

        jni.JniContext.create();

        Thread[] threads = new Thread[threadCount];

        int loops = Integer.parseInt(args[2]);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(r.create(latch, loops), "t[" + i + "]");
            threads[i].setDaemon(false);
        }

        for (Thread t : threads) {
            t.start();
        }

        latch.await();
        
        for (int i=0;i<50;i++) {
            System.gc();
        }
    }

}
