package com.scottandjoe.texasholdem.resources;

import com.scottandjoe.texasholdem.networking.Task;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.UIManager;

/**
 *
 * @author Scott DellaTorre
 * @author Joe Stein
 * safe
 */
public abstract class Utilities {

    public static final String AUTORUN = "autorun";

    public static final String DEFAULT_PLAYER_IMAGE = "default_player_image.jpg";

    public static final String FACE_DOWN_CARD = "cards/b1fv.png";

    public static final String LOG_ERROR = "error.txt";

    public static final String LOG_MESSAGES_IN = "messages/in.txt";

    public static final String LOG_MESSAGES_OUT = "messages/out.txt";

    public static final String LOG_OUTPUT = "output.txt";

    public static final String LOG_STATS = "stats.txt";

    public static final String LOG_THREADS = "threads.txt";

    public static final int LOGGING_BASIC = 0;

    public static final int LOGGING_FILES_ONLY = 1;

    public static final int LOGGING_FULL = 2;

    public static final int LOGGING_OFF = 3;

    private static LoggerThread loggerThread;

    static int loggingMode = LOGGING_BASIC;

    public static final String sessionID = String.valueOf(System.currentTimeMillis());

    private static final BlockingQueue<Task> taskQueue = new LinkedBlockingQueue<Task>();

    private static void finishLogging() throws InterruptedException {
        loggerThread.setDelay(0);
        Task task = new Task((Object) null);
        taskQueue.add(task);
        task.waitForCompletion();
    }

    public static String getCurrentTimeStamp() {
        Calendar cal = Calendar.getInstance();
        String minute = String.valueOf(cal.get(Calendar.MINUTE));
        if (minute.length() < 2) {
            minute = "0" + minute;
        }
        String second = String.valueOf(cal.get(Calendar.SECOND));
        if (second.length() < 2) {
            second = "0" + second;
        }
        return cal.get(Calendar.HOUR_OF_DAY) + ":" + minute + ":" + second;
    }

    public static String getCurrentDateTimeStamp() {
        Calendar cal = Calendar.getInstance();
        String millisecond = String.valueOf(cal.get(Calendar.MILLISECOND));
        while (millisecond.length() < 3) {
            millisecond = "0" + millisecond;
        }
        return cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US) + " " + String.valueOf(cal.get(Calendar.DAY_OF_MONTH)) + ", " + String.valueOf(cal.get(Calendar.YEAR)) + " " + getCurrentTimeStamp() + "." + millisecond;
    }

    public static URL getResource(String resource) {
        return Utilities.class.getResource(resource);
    }

    static BlockingQueue<Task> getTaskQueue() {
        return taskQueue;
    }

    public static void log(String logPath, Object output) {
        log(Thread.currentThread(), logPath, output);
    }

    public static void log(Thread thread, String logPath, Object output) {
        if (loggingMode != LOGGING_OFF) {
            taskQueue.add(new Task(LoggerThread.LOG, thread, getCurrentDateTimeStamp(), logPath, output));
            prepareLoggerThread();
        }
    }

    public static void logParcial(String logPath, Object output) {
        logParcial(Thread.currentThread(), logPath, output);
    }

    public static synchronized void logParcial(Thread thread, String logPath, Object output) {
        if (loggingMode != LOGGING_OFF) {
            taskQueue.add(new Task(LoggerThread.LOG_PARCIAL, thread, getCurrentDateTimeStamp(), logPath, output));
            prepareLoggerThread();
        }
    }

    private static synchronized void prepareLoggerThread() {
        if (loggerThread == null) {
            loggerThread = new LoggerThread();
            loggerThread.start();
            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    try {
                        finishLogging();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            });
        }
    }

    public static void setLoggingMode(int mode) {
        loggingMode = mode;
    }

    public static void setUIManager() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class LoggerThread extends Thread {

    static final int LOG = 0;

    static final int LOG_PARCIAL = 1;

    private int delayTime = 100;

    private HashMap<String, Object> parcialMessages = new HashMap<String, Object>();

    private HashMap<String, OutputStreamWriter> writers = new HashMap<String, OutputStreamWriter>();

    LoggerThread() {
        super("Logger Thread");
        setDaemon(true);
    }

    void addWriter(String logPath) {
        String separator = System.getProperty("file.separator");
        String beginning = "log" + separator + Utilities.sessionID + separator;
        String filePath;
        if (logPath.contains("/")) {
            filePath = beginning + logPath.substring(0, logPath.lastIndexOf("/")).replace("/", separator);
        } else {
            filePath = beginning;
        }
        new File(filePath).mkdirs();
        try {
            FileOutputStream outputStream = new FileOutputStream(beginning + logPath.replace("/", separator));
            outputStream.getChannel().lock();
            writers.put(logPath, new OutputStreamWriter(outputStream));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    void loggingModeChanged() {
        if (Utilities.loggingMode == Utilities.LOGGING_OFF || Utilities.loggingMode == Utilities.LOGGING_BASIC) {
            if (writers.size() > 0) {
                for (OutputStreamWriter writer : writers.values()) {
                    try {
                        writer.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
                writers.clear();
            }
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Task task = Utilities.getTaskQueue().take();
                if (task.info[0] != null) {
                    if (delayTime > 0) {
                        sleep(delayTime);
                    }
                    int taskType = (Integer) task.info[0];
                    Thread thread = (Thread) task.info[1];
                    String dateTimeStamp = (String) task.info[2];
                    String logPath = (String) task.info[3];
                    Object output = task.info[4];
                    if (taskType == LOG) {
                        if (Utilities.loggingMode == Utilities.LOGGING_BASIC || Utilities.loggingMode == Utilities.LOGGING_FULL) {
                            if (logPath.equals(Utilities.LOG_OUTPUT)) {
                                System.out.println(output);
                            } else if (logPath.equals(Utilities.LOG_ERROR)) {
                                System.err.println(output);
                            }
                        }
                        if (Utilities.loggingMode == Utilities.LOGGING_FILES_ONLY || Utilities.loggingMode == Utilities.LOGGING_FULL) {
                            try {
                                if (parcialMessages.containsKey(thread.getId() + logPath)) {
                                    output = (String) parcialMessages.get(thread.getId() + logPath) + output;
                                    parcialMessages.remove(thread.getId() + logPath);
                                }
                                if (!writers.containsKey(logPath)) {
                                    addWriter(logPath);
                                }
                                OutputStreamWriter writer = writers.get(logPath);
                                writer.write(dateTimeStamp + ": " + output + " (" + thread.getName() + ")" + System.getProperty("line.separator"));
                                writer.flush();
                            } catch (IOException ioe) {
                                ioe.printStackTrace();
                            }
                        }
                    } else if (taskType == LOG_PARCIAL) {
                        if (Utilities.loggingMode == Utilities.LOGGING_BASIC || Utilities.loggingMode == Utilities.LOGGING_FULL) {
                            if (logPath.equals(Utilities.LOG_OUTPUT)) {
                                System.out.print(output);
                            } else if (logPath.equals(Utilities.LOG_ERROR)) {
                                System.err.print(output);
                            }
                        }
                        if (Utilities.loggingMode == Utilities.LOGGING_FILES_ONLY || Utilities.loggingMode == Utilities.LOGGING_FULL) {
                            if (parcialMessages.containsKey(thread.getId() + logPath)) {
                                output = (String) parcialMessages.get(thread.getId() + logPath) + output;
                            }
                            parcialMessages.put(thread.getId() + logPath, output);
                        }
                    }
                }
                task.complete();
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    void setDelay(int delay) {
        delayTime = delay;
    }
}
