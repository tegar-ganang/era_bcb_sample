package com.beanstalktech.common.utility;

import com.beanstalktech.common.context.Application;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Information and error message logging utility for Beanstalk. Writes
 * messages to local filesystem log or to stdout. Logging levels
 * and name and location of the log file are
 * controlled by application properties.
 * <P>
 * Version 2.0: Converted from static member variables and methods to
 * instance member variables, methods and instance constructor. Each
 * Application object now has a separate Logger instance.
 * <P>
 * Version 3.0: Added unique sequence to filename for each instance of the
 * logger. This results in a separate log file for each application instance.
 *
 * @since Beanstalk V1.0
 * @version 1.1 2/9/2001 Message writing methods optimized: Date rollover
 * checked after message level check.
 * @version 2.0 7/18/2002
 * @version 3.0 8/13/2003
 **/
public class Logger {

    protected FileWriter m_messageLogFile;

    protected String m_dateQualifier = "";

    protected int m_messageMaxLevel = 10;

    protected int m_errorMaxLevel = 10;

    protected Application m_application;

    protected SimpleDateFormat m_dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss:SSS");

    protected SimpleDateFormat m_timeFormat = new SimpleDateFormat("HH:mm:ss:SSS");

    protected static int ms_instance = 1;

    /**
     * Constructs a Logger for a specified application
     * <P>
     * @param application The application for which the
     * Logger is to be constructed. The application is
     * assumed to have properties initialized that control
     * the behavior of the logger, as follows:
     * <P>
     * <UL>
     * <LI>logger_Message_MaximumLevel - Highest level message that should
     * be written to the log.
     * <LI>logger_Error_MaximumLevel - Highest level error that should be
     * written to the log.
     * <LI>logger_Message_FilePath - Directory for the log file
     * <LI>logger_Message_FileName - Root for the log file's name.
     * <LI>logger_Message_FileQualifier - Date mask for qualifying the log
     * file name. Controls whether the log is daily, monthly, etc.
     * </UL>
     */
    public Logger(Application application) {
        m_application = application;
        initializeMessageLogger();
    }

    /**
     * Constructs a default console logger. Should only be used
     * when it is necessary to log messages and no application-specific
     * logger is available.
     * <P>
     */
    public Logger() {
    }

    /**
     * Logs a message of a certain level
     *
     * @param level Number indicating level of message (0-10)
     * @param message The message text
     */
    public void logMessage(int level, String message) {
        try {
            if (level <= m_messageMaxLevel) {
                Thread currentThread = Thread.currentThread();
                String threadInfo = " [" + currentThread.getName() + ":" + currentThread.getPriority() + " ]";
                if (m_messageLogFile == null) {
                    System.out.println(m_timeFormat.format(new Date()) + threadInfo + "    (" + level + ") " + message);
                } else {
                    if (!m_dateQualifier.equals(getNow())) {
                        initializeMessageLogger();
                    }
                    m_messageLogFile.write(m_dateFormat.format(new Date()) + threadInfo + "    (" + level + ") " + message + "\r\n");
                }
            }
        } catch (Exception e) {
            System.out.println(new Date() + "    (" + level + ") " + message);
        }
    }

    public void logError(int severity, String message) {
        try {
            if (severity <= m_errorMaxLevel) {
                Thread currentThread = Thread.currentThread();
                String threadInfo = " [" + currentThread.getName() + ":" + currentThread.getPriority() + " ]";
                if (m_messageLogFile == null) {
                    System.out.println(m_timeFormat.format(new Date()) + threadInfo + " ** (" + severity + ") " + message);
                } else {
                    if (!m_dateQualifier.equals(getNow())) {
                        initializeMessageLogger();
                    }
                    m_messageLogFile.write(m_dateFormat.format(new Date()) + threadInfo + " ** (" + severity + ") " + message + "\r\n");
                    m_messageLogFile.flush();
                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + message);
        }
    }

    public synchronized void initializeMessageLogger() {
        try {
            m_messageMaxLevel = Integer.parseInt(m_application.getApplicationContext().getProperty("logger_Message_MaximumLevel"));
            m_errorMaxLevel = Integer.parseInt(m_application.getApplicationContext().getProperty("logger_Error_MaximumLevel"));
            String filePath = m_application.getApplicationContext().getProperty("logger_Message_FilePath");
            String fileName = m_application.getApplicationContext().getProperty("logger_Message_FileName");
            if (filePath == null || fileName == null) {
                m_messageLogFile = null;
                return;
            }
            String now = getNow();
            m_messageLogFile = new FileWriter(filePath + fileName + now + "_" + ms_instance + ".log", true);
            System.out.println(m_timeFormat.format(new Date()) + "Redirecting log messages to: " + filePath + fileName + now + "_" + ms_instance + ".log");
            m_dateQualifier = now;
        } catch (Exception e) {
            System.out.println(m_timeFormat.format(new Date()) + "Warning: Logger failed to create message log file -- using console");
        }
    }

    protected String getNow() {
        try {
            Date date = Calendar.getInstance().getTime();
            String fileQualifier = m_application.getApplicationContext().getProperty("logger_Message_FileQualifier");
            return fileQualifier == null ? "" : new SimpleDateFormat(fileQualifier).format(date);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Flushes the log.
     *
     */
    public synchronized void flush() {
        try {
            m_messageLogFile.flush();
        } catch (Exception e) {
        }
    }
}
