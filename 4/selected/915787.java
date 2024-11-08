package org.moonshot.log;

import java.io.PrintWriter;
import java.util.TreeMap;

public class LogFactory {

    private static LogFactory anonymous = null;

    private final TreeMap<String, boolean[]> logBits = new TreeMap<String, boolean[]>();

    public static LogFactory getInstance() {
        if (anonymous == null) anonymous = new LogFactory();
        return anonymous;
    }

    private LogFactory() {
    }

    public static Logger create(Class<?> aSource) {
        return new Logger(getInstance(), getChannelFor(aSource, null), aSource);
    }

    public static Logger create(Class<?> aSource, Object aRuntime) {
        return new Logger(getInstance(), getChannelFor(aSource, aRuntime), aSource, aRuntime);
    }

    private static PrintWriter getChannelFor(Class<?> aSource, Object object) {
        return new PrintWriter(System.err);
    }

    protected void setLogBit(Class<?> aClass, LogLevel aLevel, boolean aState) {
        final String aKey = aClass.getCanonicalName();
        boolean[] bits = logBits.get(aKey);
        if (bits == null) {
            synchronized (logBits) {
                final int len = LogLevel.values().length;
                bits = new boolean[len];
                for (int i = 0; i < len; i++) bits[i] = false;
                logBits.put(aKey, bits);
            }
        }
        bits[aLevel.ordinal()] = aState;
    }

    protected boolean getLogBit(Class<?> aClass, LogLevel aLevel) {
        final boolean[] bits = logBits.get(aClass.getCanonicalName());
        if (bits == null) return false;
        return bits[aLevel.ordinal()];
    }
}
