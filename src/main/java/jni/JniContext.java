/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
package jni;

import yjava.jni.cleaner.AbstractDeallocator;
import yjava.jni.cleaner.DeallocatingClosedBase;
import yjava.jni.cleaner.LostReferenceCounter;
import yjava.jni.cleaner.Pointer;
import yjava.jni.cleaner.PointerAccess;

public class JniContext extends DeallocatingClosedBase implements PointerAccess {

    private static final class JniContextDealloc extends AbstractDeallocator {
        protected JniContextDealloc(LostReferenceCounter counter) {
            super(counter);
        }

        /**
         * This is called from close or the deallocator to release the pointer.
         * 
         * @return index for the leak checking.
         */
        @Override
        public int free(Pointer pointer) {
            if (null == pointer || 0 == pointer.pointer) {
                return -1;
            }

            int ret = JniContextAccess.release(pointer);
            pointer.setPointer(0);
            return ret;
        }
    }

    static final LostReferenceCounter LEAK_DETECTOR;
    static {
        boolean enableLeakDetection = LostReferenceCounter.enableLeakDetection("example");

        if (enableLeakDetection) {
            LEAK_DETECTOR = new LostReferenceCounter("example", "JniContext");
        } else {
            LEAK_DETECTOR = null;
        }
    }

    private static final JniContextDealloc INTERNAL_DEALLOCATOR = new JniContextDealloc(LEAK_DETECTOR);

    static int createLeakIndex() {
        if (null == LEAK_DETECTOR) {
            return -1;
        }

        int index = LEAK_DETECTOR.open(new Throwable());
        return index;
    }

    /**
     * @throws IllegalStateException if the pointer is 0
     */
    private JniContext(final Pointer pointer) {
        super(pointer, INTERNAL_DEALLOCATOR);
        validate();
        // System.err.println("P: " + pointer);
    }

    public static JniContext create() {
        return JniContextAccess.createContext(JniContext.createLeakIndex());
    }

    public String execute() {
        return JniContextAccess.execute(this);
    }

    @Override
    public int getLeakIndex() {
        validate();

        return JniContextAccess.getLeakIndex(pointer);
    }


}
