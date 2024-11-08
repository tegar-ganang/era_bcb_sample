package org.utupiu.nibbana.log;

import java.io.PrintStream;
import java.util.Calendar;
import org.utupiu.nibbana.core.GlobalKeys;
import org.utupiu.nibbana.core.NibbanaException;
import org.utupiu.nibbana.log.enums.LogDevice;
import org.utupiu.nibbana.log.enums.LogLevel;

public class TinyLog {

    private static TinyLog INSTANCE;

    private int[] LogLevelOrdinals = new int[LogComponent.values.length];

    private LogThread LogThread;

    private WriterThread WriterThread;

    private boolean[] FirstTimeInit = new boolean[LogComponent.values.length];

    private boolean[] HasSyncLog = new boolean[LogComponent.values.length];

    private boolean[] HasAsyncLog = new boolean[LogComponent.values.length];

    private TinyLog() throws NibbanaException {
        for (int i = 0; i < LogComponent.values.length; ++i) {
            FirstTimeInit[i] = true;
            HasSyncLog[i] = false;
            HasAsyncLog[i] = false;
        }
    }

    public static void init() throws NibbanaException {
        if (INSTANCE == null) {
            INSTANCE = new TinyLog();
        } else {
            throw new NibbanaException("Nibbana Log class is already initialized.");
        }
        try {
            TinyLogForwarder newErr = new TinyLogForwarder(null);
            newErr.setComponent(LogComponent.core);
            newErr.setLevel(LogLevel.severe);
            System.setErr(newErr);
        } catch (Exception e) {
        }
        INSTANCE.setupLog(LogComponent.core, LogLevel.finest, System.out, false);
        INSTANCE.setupLog(LogComponent.security, LogLevel.finest, System.out, false);
        INSTANCE.setupLog(LogComponent.waf, LogLevel.finest, System.out, false);
        INSTANCE.setupLog(LogComponent.db, LogLevel.finest, System.out, false);
        INSTANCE.setupLog(LogComponent.email, LogLevel.finest, System.out, false);
        INSTANCE.setupLog(LogComponent.app, LogLevel.finest, System.out, false);
        try {
            INSTANCE.LogThread = new LogThread();
            INSTANCE.LogThread.start();
            INSTANCE.WriterThread = new WriterThread();
        } catch (Exception e) {
            throw new NibbanaException("Could not start the log thread.", e);
        }
    }

    public static synchronized void addLog(String componentName, String deviceString, String levelName, String async) throws NibbanaException {
        LogComponent component = LogComponent.getValueForName(componentName);
        if (component == null) {
            throw new NibbanaException("The Log Component [" + componentName + "] does not match with any of Nibbana recognized components.");
        }
        LogDevice device = LogDevice.getValueForName(deviceString);
        if (device == null) {
            throw new NibbanaException("The Log Device cannot be null");
        }
        LogLevel level = LogLevel.getValueForName(levelName);
        if (level == null) {
            throw new NibbanaException("The Log Level [" + levelName + "] does not match with any of Nibbana recognized levels.");
        }
        boolean isAsync = GlobalKeys.TRUE.equals(async);
        PrintStream printStream = null;
        switch(device) {
            case sysout:
                printStream = System.out;
                break;
            case syserr:
                printStream = System.err;
                break;
            case file:
                printStream = new FilePrintStream(deviceString);
                break;
            case email:
                printStream = new EMailPrintStream(deviceString);
                break;
        }
        int componentOrdinal = component.ordinal;
        if (INSTANCE.FirstTimeInit[componentOrdinal]) {
            component.SyncWriters.clear();
            component.AsyncWriters.clear();
            INSTANCE.FirstTimeInit[componentOrdinal] = false;
        }
        INSTANCE.setupLog(component, level, printStream, isAsync);
    }

    private void setupLog(LogComponent component, LogLevel level, PrintStream printStream, boolean async) {
        int componentOrdinal = component.ordinal;
        LogWriter writer = new LogWriter(component, level, printStream, async);
        if (async) {
            component.AsyncWriters.add(writer);
            INSTANCE.HasAsyncLog[componentOrdinal] = true;
        } else {
            component.SyncWriters.add(writer);
            INSTANCE.HasSyncLog[componentOrdinal] = true;
        }
        INSTANCE.LogLevelOrdinals[componentOrdinal] = component.getMinLevel().ordinal();
    }

    public static synchronized void print(LogComponent component, LogLevel level, String message) {
        int componentOrdinal = component.ordinal;
        if (INSTANCE.HasSyncLog[componentOrdinal]) {
            for (LogWriter writer : component.SyncWriters) {
                Calendar time = Calendar.getInstance();
                INSTANCE.WriterThread.writeLog(writer, level, time, message, null);
            }
        }
        if (INSTANCE.HasAsyncLog[componentOrdinal]) {
            LogRow row = new LogRow(level, message, null);
            for (LogWriter writer : component.AsyncWriters) {
                writer.LogRows.add(row);
            }
        }
    }

    public static synchronized void print(LogComponent component, Throwable throwable) {
        int componentOrdinal = component.ordinal;
        LogLevel level = LogLevel.severe;
        if (INSTANCE.HasSyncLog[componentOrdinal]) {
            for (LogWriter writer : component.SyncWriters) {
                Calendar time = Calendar.getInstance();
                INSTANCE.WriterThread.writeLog(writer, level, time, null, throwable);
            }
        }
        if (INSTANCE.HasAsyncLog[componentOrdinal]) {
            LogRow row = new LogRow(level, null, throwable);
            for (LogWriter writer : component.AsyncWriters) {
                writer.LogRows.add(row);
            }
        }
    }

    public static synchronized void print(LogComponent component, String message, Throwable throwable) {
        int componentOrdinal = component.ordinal;
        LogLevel level = LogLevel.severe;
        if (INSTANCE.HasSyncLog[componentOrdinal]) {
            for (LogWriter writer : component.SyncWriters) {
                Calendar time = Calendar.getInstance();
                INSTANCE.WriterThread.writeLog(writer, level, time, message, throwable);
            }
        }
        if (INSTANCE.HasAsyncLog[componentOrdinal]) {
            LogRow row = new LogRow(level, message, throwable);
            for (LogWriter writer : component.AsyncWriters) {
                writer.LogRows.add(row);
            }
        }
    }

    public static void close() {
        for (LogComponent component : LogComponent.values) {
            component.close();
        }
        INSTANCE.LogThread.close();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
