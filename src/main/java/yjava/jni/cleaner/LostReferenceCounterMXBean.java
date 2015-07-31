/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
package yjava.jni.cleaner;

import javax.management.MXBean;


@MXBean
public interface LostReferenceCounterMXBean {
    /**
     * @return the number of open references
     */
    int getOpenCount();

    /**
     * @return the number of lost references
     */
    int getLostCount();

    /**
     * @return the number of closed references
     */
    int getClosedCount();

    /**
     * @return the stacks for open and lost references
     */
    String[] openAndLostStacks();

    /**
     * @return the stacks for open references
     */
    String[] openStacks();

    /**
     * @return the stacks for lost references
     */
    String[] lostStacks();

    /**
     * @return name of this leak detector
     */
    String getName();

    /**
     * @return true if this leak detector is enabled (max &gt; 1)
     */
    boolean isEnabled();


    /**
     * @return maximum number of stacks that can be tracked.
     */
    int getMax();
}
