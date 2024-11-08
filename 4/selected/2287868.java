package com.art.anette.common.logging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;
import java.util.logging.Logger;
import com.art.anette.client.database.ClientDBControl;
import com.art.anette.client.main.Global;
import com.art.anette.common.SharedGlobal;
import com.art.anette.common.VersionUtils;

/**
 * @author alex
 */
public class LogController {

    private static String dir;

    public static boolean IS_CLIENT;

    private static final Formatter singleLineFormatter = new CompactFormatter();

    private LogController() {
    }

    public static void setUpClientLogging(String dir) {
        setUpLogging(dir, "client.log", true);
        getLogger(LogController.class).info("Client starting up " + VersionUtils.getVersionString() + " appHome=" + SharedGlobal.APP_HOME_DIR);
    }

    public static void setUpServerLogging(String dir) {
        setUpLogging(dir, "server.log", false);
        getLogger(LogController.class).info("Server starting up " + VersionUtils.getVersionString());
    }

    @SuppressWarnings({ "UseOfSystemOutOrSystemErr", "CallToPrintStackTrace" })
    private static void setUpLogging(String dir, String fname, boolean isClient) {
        IS_CLIENT = isClient;
        LogController.dir = dir.endsWith(File.separator) ? dir : dir + File.separator;
        Logger l = Logger.getLogger("");
        for (Handler h : Logger.getLogger("").getHandlers()) {
            l.removeHandler(h);
        }
        l = Logger.getLogger("com.art.anette");
        final ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(singleLineFormatter);
        consoleHandler.setLevel(Level.INFO);
        l.addHandler(consoleHandler);
        if (isClient) {
            final File file = new File(Global.CAUGHT_EXCEPTIONS_FILE);
            final SerializingReportHandler handler = SerializingReportHandler.install(file);
            handler.setFormatter(singleLineFormatter);
            handler.setLevel(Level.WARNING);
            l.addHandler(handler);
        }
        try {
            Handler fh = new MyFileHandler(dir + fname);
            fh.setLevel(Level.INFO);
            fh.setFormatter(singleLineFormatter);
            l.addHandler(fh);
        } catch (IOException e) {
            System.err.println("Error while creating log file");
            e.printStackTrace();
        } catch (SecurityException e) {
            System.err.println("Error while creating log file");
            e.printStackTrace();
        }
    }

    public static Handler getClientFileHandler(long client) throws IOException {
        Handler fh = new MyFileHandler(dir + "client-" + Long.toString(client) + ".log");
        fh.setFormatter(singleLineFormatter);
        fh.setLevel(Level.INFO);
        return fh;
    }

    public static void switchToClientSpecificLogging(ClientDBControl cdbc) {
    }

    /**
     * Like FileHandler but bails out if the file can't be locked.
     */
    private static class MyFileHandler extends StreamHandler {

        private String pattern;

        private String lockFileName;

        private FileOutputStream lockStream;

        private File[] files;

        private static final int MAX_LOCKS = 1;

        private static Map<String, String> locks = new HashMap<String, String>();

        private void open(File fname, boolean append) throws IOException {
            FileOutputStream fout = new FileOutputStream(fname.toString(), append);
            BufferedOutputStream bout = new BufferedOutputStream(fout);
            setOutputStream(bout);
        }

        private MyFileHandler(String pattern) throws IOException, SecurityException {
            if (pattern.length() < 1) {
                throw new IllegalArgumentException();
            }
            this.pattern = pattern;
            openFiles();
        }

        private void openFiles() throws IOException {
            LogManager manager = LogManager.getLogManager();
            manager.checkAccess();
            InitializationErrorManager em = new InitializationErrorManager();
            setErrorManager(em);
            int unique = -1;
            while (true) {
                unique++;
                if (unique > MAX_LOCKS) {
                    throw new IOException("Couldn't get lock for " + pattern);
                }
                lockFileName = generate(pattern, 0, unique).toString() + ".lck";
                synchronized (locks) {
                    if (locks.get(lockFileName) != null) {
                        continue;
                    }
                    FileChannel fc;
                    try {
                        lockStream = new FileOutputStream(lockFileName);
                        fc = lockStream.getChannel();
                    } catch (IOException ix) {
                        continue;
                    }
                    try {
                        FileLock fl = fc.tryLock();
                        if (fl == null) {
                            continue;
                        }
                    } catch (IOException ix) {
                    }
                    locks.put(lockFileName, lockFileName);
                    break;
                }
            }
            files = new File[] { generate(pattern, 0, unique) };
            open(files[0], true);
            Exception ex = em.lastException;
            if (ex != null) {
                if (ex instanceof IOException) {
                    throw (IOException) ex;
                } else if (ex instanceof SecurityException) {
                    throw (SecurityException) ex;
                } else {
                    throw new IOException("Exception: " + ex);
                }
            }
            setErrorManager(new ErrorManager());
        }

        @SuppressWarnings({ "HardcodedFileSeparator" })
        private static File generate(String pattern, int generation, int unique) {
            File file = null;
            String word = "";
            int ix = 0;
            boolean sawu = false;
            while (ix < pattern.length()) {
                char ch = pattern.charAt(ix);
                ix++;
                char ch2 = 0;
                if (ix < pattern.length()) {
                    ch2 = Character.toLowerCase(pattern.charAt(ix));
                }
                if (ch == '/') {
                    if (file == null) {
                        file = new File(word);
                    } else {
                        file = new File(file, word);
                    }
                    word = "";
                    continue;
                } else if (ch == '%') {
                    if (ch2 == 't') {
                        String tmpDir = System.getProperty("java.io.tmpdir");
                        if (tmpDir == null) {
                            tmpDir = System.getProperty("user.home");
                        }
                        file = new File(tmpDir);
                        ix++;
                        word = "";
                        continue;
                    } else if (ch2 == 'h') {
                        file = new File(System.getProperty("user.home"));
                        ix++;
                        word = "";
                        continue;
                    } else if (ch2 == 'g') {
                        word += generation;
                        ix++;
                        continue;
                    } else if (ch2 == 'u') {
                        word += unique;
                        sawu = true;
                        ix++;
                        continue;
                    } else if (ch2 == '%') {
                        word += '%';
                        ix++;
                        continue;
                    }
                }
                word += ch;
            }
            if (unique > 0 && !sawu) {
                word = word + '.' + unique;
            }
            if (word.length() > 0) {
                if (file == null) {
                    file = new File(word);
                } else {
                    file = new File(file, word);
                }
            }
            return file;
        }

        /**
         * Format and publish a <tt>LogRecord</tt>.
         *
         * @param record description of the log event. A null record is
         *               silently ignored and is not published
         */
        public synchronized void publish(LogRecord record) {
            if (!isLoggable(record)) {
                return;
            }
            super.publish(record);
            flush();
        }

        /**
         * Close all the files.
         *
         * @throws SecurityException if a security manager exists and if
         *                           the caller does not have <tt>LoggingPermission("control")</tt>.
         */
        public synchronized void close() throws SecurityException {
            super.close();
            if (lockFileName == null) {
                return;
            }
            try {
                lockStream.close();
            } catch (IOException ex) {
            }
            synchronized (locks) {
                locks.remove(lockFileName);
            }
            new File(lockFileName).delete();
            lockFileName = null;
            lockStream = null;
        }

        private static class InitializationErrorManager extends ErrorManager {

            private Exception lastException;

            public void error(String msg, Exception ex, int code) {
                lastException = ex;
            }
        }
    }

    public static com.art.anette.common.logging.Logger getLogger(Class clazz) {
        return new com.art.anette.common.logging.Logger(Logger.getLogger(clazz.getName()));
    }

    public static com.art.anette.common.logging.Logger getLogger(String className) {
        return new com.art.anette.common.logging.Logger(Logger.getLogger(className));
    }
}
