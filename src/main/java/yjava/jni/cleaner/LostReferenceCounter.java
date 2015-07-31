/* Copyright 2015 Yahoo Inc. */
/* Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details. */
package yjava.jni.cleaner;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

public class LostReferenceCounter implements LostReferenceCounterMXBean {
    // if you have log4j, change this.
    // private static final Logger LOG = Logger.getLogger(LostReferenceCounter.class);

    private final boolean enabled;
    private final int max;
    private final AtomicInteger[] openCount;
    private final AtomicInteger[] closedCount;
    private final AtomicInteger[] lostCount;

    private final Map<String, Integer> stackToIndex;
    private final AtomicInteger last;
    private final boolean logStacks;
    private final boolean failIfStackIsEmpty;
    private final String name;

    public static enum StackTypes {
        Open, //
        Lost, //
        Closed;
    }

    public LostReferenceCounter(String packageName, String name) {
        this(name, getMax(packageName), enableLeakStackLogging(packageName), failIfStackIsEmpty(packageName));
    }

    LostReferenceCounter(String name, int inMax, boolean logStacks) {
        this(name, inMax, logStacks, logStacks);
    }

    LostReferenceCounter(String name, int inMax, boolean logStacks, boolean failIfStackIsEmpty) {
        if (null == name) {
            throw new NullPointerException();
        }

        name = name.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null");
        }

        this.max = inMax;
        this.name = name;

        if (this.max > 0) {
            this.enabled = true;
            this.logStacks = logStacks;
            this.failIfStackIsEmpty = failIfStackIsEmpty;
            this.openCount = new AtomicInteger[max];
            this.closedCount = new AtomicInteger[max];
            this.lostCount = new AtomicInteger[max];
            this.stackToIndex = new ConcurrentHashMap<String, Integer>();
            // start at -1 so we are zero-based for incrementAndGet
            this.last = new AtomicInteger(-1);

            if (!logStacks) {
                this.stackToIndex.put("", Integer.valueOf(0));
            }

            registerMbean();
        } else {
            this.enabled = false;
            this.openCount = null;
            this.closedCount = null;
            this.lostCount = null;
            this.stackToIndex = null;
            this.last = null;
            this.logStacks = false;
            this.failIfStackIsEmpty = false;
        }
    }

    LostReferenceCounter(String name, int inMax, boolean logStacks, AtomicInteger[] openCount,
                    AtomicInteger[] closedCount, AtomicInteger[] lostCount) {
        this.max = inMax;
        this.name = name;
        this.enabled = true;
        this.failIfStackIsEmpty = false;
        this.logStacks = logStacks;
        this.openCount = openCount;
        this.closedCount = closedCount;
        this.lostCount = lostCount;
        this.stackToIndex = new ConcurrentHashMap<String, Integer>();
        // start at -1 so we are zero-based for incrementAndGet
        this.last = new AtomicInteger(-1);

        if (!logStacks) {
            this.stackToIndex.put("", Integer.valueOf(0));
        }
    }

    void registerMbean() {
        MBeanServer mbs = getPlatformMBeanServer();

        // Construct the ObjectName for the Hello MBean we will register
        try {
            ObjectName mbeanName = new ObjectName("yjava.security.ysecure." + name + ":type=LostReferenceCounter");
            mbs.registerMBean(this, mbeanName);
        } catch (MalformedObjectNameException | MBeanRegistrationException | NotCompliantMBeanException e) {
            // LOG.error("Error registering mbean for " + name, e);
            // if you have log4j, change this.
            //e.printStackTrace();
        } catch (InstanceAlreadyExistsException e) {
            // ignore;
        }
    }

    MBeanServer getPlatformMBeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }

    /**
     * Given a stack return the index to store for the deallocation
     * 
     * @param at Throwable of where open was called from.
     * @return index the stack can be retrieved from.
     */
    public int open(Throwable at) {
        if (!enabled || null == at) {
            return -1;
        }

        synchronized (this) {
            Integer index = Integer.valueOf(0);

            if (logStacks) {
                String stack = stackToString(at);
                if (failIfStackIsEmpty && stack.isEmpty()) {
                    throw new IllegalStateException("Unable to store empty stack");
                }

                index = stackToIndex.get(stack);

                if (null == index) {
                    int newInt = last.incrementAndGet();
                    index = Integer.valueOf(newInt);
                }

                if (badIndex(index.intValue())) {
                    return -2;
                }

                stackToIndex.put(stack, index);
            }

            if (badIndex(index.intValue())) {
                return -3;
            }

            final int ofs = index.intValue();
            AtomicInteger counter = openCount[ofs];
            if (null == counter) {
                counter = new AtomicInteger(0);
                openCount[ofs] = counter;

                closedCount[ofs] = new AtomicInteger(0);
                lostCount[ofs] = new AtomicInteger(0);
            }

            // count an open
            counter.incrementAndGet();

            // and cache the losses.
            return ofs;
        }
    }

    /**
     * Given an index close the value
     * 
     * @param atIndex the index to close
     */
    public void close(int atIndex) {
        if (!enabled) {
            return;
        }

        if (badIndex(atIndex)) {
            return;
        }

        // count a close
        closeClose(atIndex);
    }

    /**
     * Given an index leak the value
     * 
     * @param atIndex the index to close
     */
    public void lost(int atIndex) {
        if (!enabled) {
            return;
        }

        if (badIndex(atIndex)) {
            return;
        }

        // count a close
        closeLost(atIndex);
    }

    @Override
    public int getOpenCount() {
        if (!enabled) {
            return -1;
        }

        int ret = 0;
        for (int i = 0; i < max; i++) {
            AtomicInteger ai = openCount[i];
            if (null != ai) {
                ret += ai.intValue();
            }
        }

        return ret;
    }

    @Override
    public int getLostCount() {
        if (!enabled) {
            return -1;
        }

        int ret = 0;
        for (int i = 0; i < max; i++) {
            AtomicInteger ai = lostCount[i];
            if (null != ai) {
                ret += ai.intValue();
            }
        }

        return ret;
    }

    @Override
    public int getClosedCount() {
        if (!enabled) {
            return -1;
        }

        int ret = 0;
        for (int i = 0; i < max; i++) {
            AtomicInteger ai = closedCount[i];
            if (null != ai) {
                ret += ai.intValue();
            }
        }

        return ret;
    }


    boolean badIndex(int index) {
        if (index < 0) {
            return true;
        }

        if (index >= max) {
            return true;
        }

        return false;
    }

    boolean valid(final int i, final AtomicInteger[] from) {
        if (!badIndex(i) && null != from && i < from.length && null != from[i] && from[i].intValue() > 0) {
            return true;
        }

        return false;
    }

    int addAmount(final int i, final AtomicInteger[] from) {
        int v = 0;
        if (valid(i, from)) {
            v += from[i].intValue();
        }

        return v;
    }

    int addAmount(final int i, final String type, List<String> results, final AtomicInteger[] from, final String key) {
        if (valid(i, from)) {
            results.add("REFCOUNT: " + type + ": " + from[i].intValue() + " references at i=" + i + " key=" + key);
            return from[i].intValue();
        }

        return 0;
    }

    int addClosedCount(int i) {
        return addAmount(i, closedCount);
    }

    int addLostCount(int i, List<String> ret, String key) {
        return addAmount(i, "Lost", ret, lostCount, key);
    }

    int addOpenCount(int i, List<String> ret, String key) {
        return addAmount(i, "Open", ret, openCount, key);
    }

    int addClosedCount(int i, List<String> ret, String key) {
        return addAmount(i, "Closed", ret, closedCount, key);
    }

    String[] getCounts(EnumSet<StackTypes> types) {
        if (!enabled) {
            return new String[] {"disabled"};
        }

        List<String> ret = new ArrayList<String>(max + 1);
        synchronized (this) {
            int totalOpen = 0;
            int totalLost = 0;
            int totalClosed = 0;
            for (Entry<String, Integer> e : stackToIndex.entrySet()) {
                Integer v = e.getValue();
                if (null != v) {
                    int o = v.intValue();
                    String key = e.getKey();

                    if (types.contains(StackTypes.Lost)) {
                        totalLost += addLostCount(o, ret, key);
                    }

                    if (types.contains(StackTypes.Open)) {
                        totalOpen += addOpenCount(o, ret, key);
                    }

                    if (types.contains(StackTypes.Closed)) {
                        addClosedCount(o, ret, key);
                    }

                    totalClosed += addClosedCount(o);
                }
            }
            ret.add("REFCOUNT: Open: " + totalOpen + "\n");
            ret.add("REFCOUNT: Lost: " + totalLost + "\n");
            ret.add("REFCOUNT: Closed: " + totalClosed + "\n");
        }

        return ret.toArray(new String[] {});
    }

    @Override
    public String[] openAndLostStacks() {
        return getCounts(EnumSet.of(StackTypes.Lost, StackTypes.Open));
    }

    @Override
    public String[] lostStacks() {
        return getCounts(EnumSet.of(StackTypes.Lost));
    }

    @Override
    public String[] openStacks() {
        return getCounts(EnumSet.of(StackTypes.Open));
    }


    public String[] allStacks() {
        return getCounts(EnumSet.of(StackTypes.Open, StackTypes.Lost, StackTypes.Closed));
    }

    void closeOpen(int atIndex) {
        if (badIndex(atIndex)) {
            return;
        }

        AtomicInteger openCounter = openCount[atIndex];
        if (null != openCounter) {
            openCounter.decrementAndGet();
        }
    }

    void closeClose(int atIndex) {
        if (badIndex(atIndex)) {
            return;
        }

        closeOpen(atIndex);
        AtomicInteger counter = closedCount[atIndex];
        if (null != counter) {
            counter.incrementAndGet();
        }
    }

    void closeLost(int atIndex) {
        if (badIndex(atIndex)) {
            return;
        }

        closeOpen(atIndex);
        AtomicInteger counter = lostCount[atIndex];
        if (null != counter) {
            counter.incrementAndGet();
        }
    }


    String stackToString(Throwable at) {
        return stackToString(at, this.logStacks);
    }

    static String stackToString(Throwable t, boolean logStacks) {
        if (!logStacks) {
            return "";
        }

        StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            return sw.getBuffer().toString();
        } finally {
            if (null != pw) {
                pw.close();
            }
            if (null != sw) {
                try {
                    sw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public int getMax() {
        return max;
    }

    static String getProperty(String packageName, String appendant, String dv) {
        if (null == packageName) {
            throw new NullPointerException();
        }

        packageName = packageName.trim();
        if (packageName.isEmpty()) {
            throw new IllegalArgumentException("packageName cannot be empty");
        }

        String key = "yjava." + packageName + appendant;
        String v = System.getProperty(key, dv);

        return v;
    }

    public static boolean enableLeakDetection(String packageName) {
        // look at the things that could enable it.
        String v = getProperty(packageName, ".enableLeakDetection", "false");
        return Boolean.parseBoolean(v);
    }

    public static boolean enableLeakStackLogging(String packageName) {
        // look at the things that could enable it.
        String v = getProperty(packageName, ".enableLeakLogs", "false");
        return Boolean.parseBoolean(v);
    }

    public static boolean failIfStackIsEmpty(String packageName) {
        // look at the things that could enable it.
        String v = getProperty(packageName, ".failEmptyStacks", "false");
        return Boolean.parseBoolean(v);
    }

    public static int getMax(String packageName) {
        int max = 0;
        String v = getProperty(packageName, ".leakLogMax", "100");
        max = Integer.parseInt(v);
        return max;
    }

}
