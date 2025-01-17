package com.spinn3r.log5j;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AsyncLogger {

    private static final int MAX_QUEUE_SIZE = 100000;

    private static final AtomicLong __errorCounter = new AtomicLong();

    private final BlockingQueue<LogEvent> __logEventQueue = new ArrayBlockingQueue<LogEvent>(MAX_QUEUE_SIZE);

    private final WriterThread _writerThread;

    public AsyncLogger() {
        _writerThread = new WriterThread();
        _writerThread.start();
    }

    public void add(LogEvent event) {
        if (!__logEventQueue.offer(event)) {
            logFallback(event);
        }
    }

    private static void logFallback(LogEvent logEvent) {
        System.err.println("LogErr#" + __errorCounter.incrementAndGet() + ' ' + LogUtils.toString(logEvent));
    }

    public void shutdown() {
        _writerThread.shutdown();
    }

    public void flush() {
        _writerThread.flush();
    }

    class WriterThread extends Thread {

        private static final int WAIT_TIME = 2000;

        private static final boolean DAEMON = true;

        private final List<LogEvent> _loggingEvents;

        private volatile boolean _stopRequested;

        WriterThread() {
            super("AsyncLogger.WriterThread");
            setDaemon(DAEMON);
            _loggingEvents = new ArrayList<LogEvent>();
        }

        public void shutdown() {
            _stopRequested = true;
        }

        @Override
        public void run() {
            while (!_stopRequested || !__logEventQueue.isEmpty()) {
                try {
                    waitAndProcessFirstLoggingEvent();
                    processOthersIfExist();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        protected void flush() {
            processOthersIfExist();
        }

        private void waitAndProcessFirstLoggingEvent() throws InterruptedException {
            LogEvent logEvent = __logEventQueue.poll(WAIT_TIME, TimeUnit.MILLISECONDS);
            if (logEvent != null) {
                log(logEvent);
            }
        }

        private void processOthersIfExist() {
            __logEventQueue.drainTo(_loggingEvents);
            try {
                for (LogEvent logEvent : _loggingEvents) {
                    log(logEvent);
                }
            } finally {
                _loggingEvents.clear();
            }
        }

        private void log(LogEvent logEvent) {
            try {
                logEvent.logger().log(logEvent);
            } catch (Throwable e) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos));
                writer.append("Cannot log event: ");
                try {
                    writer.append(logEvent.toString());
                } catch (Exception ignore) {
                }
                writer.append('\n');
                e.printStackTrace(writer);
                writer.close();
                System.err.println(baos.toString());
            }
        }
    }
}
