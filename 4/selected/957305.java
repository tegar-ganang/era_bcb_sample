package newsatort.log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import newsatort.exception.GeneralException;
import newsatort.manager.AbstractManager;
import org.apache.commons.logging.LogFactory;

public class LogManager extends AbstractManager implements ILogManager {

    private final Map<String, ILogger> loggersMap;

    private final Map<LogLevel, List<ILogger>> loggersByLevelsMap;

    private final Queue<LogEntry> queueLogs;

    private final RunnableLoop runnableLoop;

    private final Thread writerThread;

    private boolean stopLoop = false;

    private boolean isStarted = false;

    public LogManager() {
        loggersMap = new HashMap<String, ILogger>();
        loggersByLevelsMap = new HashMap<LogLevel, List<ILogger>>();
        for (LogLevel logLevel : LogLevel.values()) {
            loggersByLevelsMap.put(logLevel, new ArrayList<ILogger>());
        }
        queueLogs = new ArrayDeque<LogEntry>();
        runnableLoop = new RunnableLoop();
        writerThread = new Thread(runnableLoop);
        writerThread.setName("LogManager Writer Thread");
    }

    @Override
    public void start() throws GeneralException {
        isStarted = true;
        writerThread.start();
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", CommonsLogImpl.class.getName());
        addLog(LogLevel.VERBOSE, "D�marrage");
    }

    @Override
    public void stop() throws GeneralException {
        addLog(LogLevel.VERBOSE, "Arr�t");
        stopLoop = true;
        notifyWriterThread();
        for (ILogger logger : loggersMap.values()) logger.stop();
        isStarted = false;
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    @Override
    public void addLogger(ILogger logger) throws GeneralException {
        loggersMap.put(logger.getName(), logger);
        for (LogLevel logLevel : logger.getAcceptedLogLevelList()) loggersByLevelsMap.get(logLevel).add(logger);
        logger.start();
    }

    @Override
    public void removeLogger(String name) throws GeneralException {
        final ILogger logger = loggersMap.get(name);
        for (LogLevel logLevel : logger.getAcceptedLogLevelList()) loggersByLevelsMap.get(logLevel).remove(logger);
        logger.stop();
        loggersMap.remove(name);
    }

    @Override
    public Map<String, ILogger> getLoggers() {
        return loggersMap;
    }

    @Override
    public void addLog(LogLevel level, Throwable throwable, Object... objects) {
        addLog(3, level, throwable, objects);
    }

    @Override
    public void addLog(LogLevel level, Object... objects) {
        addLog(3, level, null, objects);
    }

    @Override
    public void addLogUp(LogLevel level, Object... objects) {
        addLog(4, level, null, objects);
    }

    @Override
    public void addLogUp(LogLevel level, Throwable throwable, Object... objects) {
        addLog(4, level, throwable, objects);
    }

    private void addLog(int stackLevel, LogLevel level, Throwable throwable, Object... objects) {
        final LogEntry logEntry = getNewLog();
        final Thread currentThread = Thread.currentThread();
        logEntry.setLevel(level);
        logEntry.setStackTraceArray(currentThread.getStackTrace());
        logEntry.setObjects(objects);
        logEntry.setThreadName(currentThread.getName());
        logEntry.setThrowable(throwable);
        logEntry.setWritable(true);
        notifyWriterThread();
    }

    private void notifyWriterThread() {
        if (runnableLoop != null) {
            synchronized (runnableLoop) {
                runnableLoop.notify();
            }
        }
    }

    private synchronized LogEntry getNewLog() {
        final LogEntry log = new LogEntry();
        log.setDate(new Date());
        queueLogs.add(log);
        return log;
    }

    private class RunnableLoop implements Runnable {

        @Override
        public void run() {
            try {
                while (!stopLoop) {
                    LogEntry logEntry = null;
                    while ((logEntry = queueLogs.peek()) != null) {
                        if (logEntry.isWritable()) {
                            for (ILogger logger : loggersByLevelsMap.get(logEntry.getLevel())) logger.addLog(logEntry);
                            queueLogs.poll();
                        } else {
                            break;
                        }
                    }
                    synchronized (this) {
                        wait();
                    }
                }
            } catch (InterruptedException exception) {
                System.err.println("!!! ERREUR LOG !!!");
                exception.printStackTrace();
                System.err.println("!!! ERREUR LOG !!!");
            }
        }
    }
}
