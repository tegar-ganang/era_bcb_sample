package com.hszt.util.log;

import java.io.*;

/**
 * @author danielroth
 * @author adrianchristen
 * @author matthiasschmid
 */
public class FileLog extends Log {

    private File logFile;

    protected static Log getInstance() {
        return getInstance(LOG_LEVEL.DEBUG);
    }

    protected static Log getInstance(LOG_LEVEL logLevel) {
        return getInstance(logLevel, LOG_LEVEL.DEBUG, null);
    }

    protected static Log getInstance(LOG_LEVEL displayLogLevel, LOG_LEVEL logLevel, String logPath) {
        if (logger == null) {
            logger = new FileLog(displayLogLevel, logLevel, logPath);
            return logger;
        }
        return logger;
    }

    private FileLog(LOG_LEVEL displayLogLevel, LOG_LEVEL logLevel, String logPath) {
        this.logLevel = logLevel;
        this.displayLogLevel = displayLogLevel;
        if (null != logPath) {
            logFile = new File(logPath, "current.log");
            log(LOG_LEVEL.DEBUG, "FileLog", "Initialising logfile " + logFile.getAbsolutePath() + " .");
            try {
                if (logFile.exists()) {
                    if (!logFile.renameTo(new File(logPath, System.currentTimeMillis() + ".log"))) {
                        File newFile = new File(logPath, System.currentTimeMillis() + ".log");
                        if (newFile.exists()) {
                            log(LOG_LEVEL.WARN, "FileLog", "The file (" + newFile.getAbsolutePath() + newFile.getName() + ") already exists, will overwrite it.");
                            newFile.delete();
                        }
                        newFile.createNewFile();
                        FileInputStream inStream = new FileInputStream(logFile);
                        FileOutputStream outStream = new FileOutputStream(newFile);
                        byte buffer[] = null;
                        int offSet = 0;
                        while (inStream.read(buffer, offSet, 2048) != -1) {
                            outStream.write(buffer);
                            offSet += 2048;
                        }
                        inStream.close();
                        outStream.close();
                        logFile.delete();
                        logFile = new File(logPath, "current.log");
                    }
                }
                logFile.createNewFile();
            } catch (IOException e) {
                logFile = null;
            }
        } else {
            logFile = null;
        }
    }

    protected void writeMessage(LOG_LEVEL level, String message) {
        FileOutputStream outStream;
        try {
            if (logFile != null) {
                outStream = new FileOutputStream(logFile, true);
                byte[] bytes = message.getBytes("UTF-8");
                outStream.write(bytes);
                outStream.close();
            } else {
            }
        } catch (FileNotFoundException e) {
            log(LOG_LEVEL.WARN, "FileLog", "Logfile does not exist.");
        } catch (IOException e) {
            log(LOG_LEVEL.WARN, "FileLog", "Logfile is not writeable.");
        }
    }
}
