package com.google.code.sagetvaddons.sjq.server;

import gkusnick.sagetv.api.API;
import java.io.StringWriter;
import java.util.Date;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * @author dbattams
 *
 */
public final class Butler implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(Butler.class);

    static final API SageApi = API.apiNullUI;

    private static Butler instance = null;

    public static synchronized void wakeQueueLoader() {
        if (instance != null) instance.wakeupQueueLoader();
        return;
    }

    public static synchronized boolean debugQueueLoader(String path) {
        if (instance != null) return instance.dbgQueueLoader(path);
        return false;
    }

    public static synchronized boolean isQueueLoaderDebugging() {
        if (instance != null) return instance.getQueueLoaderDebugStatus();
        return false;
    }

    public static synchronized void createActiveFileCleaner() {
        if (instance != null) instance.newFileCleaner();
    }

    public static synchronized void createActiveLogCleaner() {
        if (instance != null) instance.newLogCleaner();
    }

    public static synchronized void createActiveVacuumCleaner() {
        if (instance != null) instance.newVacuumCleaner();
    }

    public static synchronized String dumpAppTrace() {
        if (instance != null) return instance.dumpAppState(); else {
            LOG.warn("SJQ ERROR: Attempting to dump app state on null Butler object!");
            throw new RuntimeException("SJQ FATAL ERROR: Butler is null!");
        }
    }

    private MediaFileQueueLoader qLoader;

    private Thread qLoaderThread;

    private ClientMonitor cMon;

    private Thread cMonThread;

    private InternalTaskClient clnt;

    private Thread clntThread;

    private Thread activeFileCleaner;

    private Thread activeLogCleaner;

    private Thread activeVacuumCleaner;

    private SystemMessageQueueLoader sysMsgQ;

    private Thread sysMsgThread;

    public void contextDestroyed(ServletContextEvent arg0) {
        instance = null;
        if (qLoaderThread != null && qLoader != null && qLoaderThread.isAlive()) {
            qLoader.setKill(true);
            qLoaderThread.interrupt();
        }
        if (cMonThread != null && cMon != null && cMonThread.isAlive()) {
            cMon.setKill(true);
            cMonThread.interrupt();
        }
        if (clntThread != null && clnt != null && clntThread.isAlive()) {
            clnt.setKill(true);
            clntThread.interrupt();
        }
        if (sysMsgThread != null && sysMsgQ != null && sysMsgThread.isAlive()) {
            sysMsgQ.setKill(true);
            sysMsgThread.interrupt();
        }
        return;
    }

    public void contextInitialized(ServletContextEvent arg0) {
        instance = this;
        PropertyConfigurator.configure("sjq.log4j.properties");
        qLoader = new MediaFileQueueLoader();
        qLoaderThread = new Thread(qLoader);
        qLoaderThread.setName("SJQ-MediaFileQueueLoader");
        qLoaderThread.setDaemon(true);
        qLoaderThread.start();
        cMon = new ClientMonitor();
        cMonThread = new Thread(cMon);
        cMonThread.setName("SJQ-ClientMonitor");
        cMonThread.setDaemon(true);
        cMonThread.start();
        clnt = new InternalTaskClient();
        clntThread = new Thread(clnt);
        clntThread.setName("SJQ-InternalTaskClnt");
        clntThread.setDaemon(true);
        clntThread.start();
        sysMsgQ = new SystemMessageQueueLoader();
        sysMsgThread = new Thread(sysMsgQ);
        sysMsgThread.setName("SJQ-SystemMessageQueueLoader");
        sysMsgThread.setDaemon(true);
        sysMsgThread.start();
        return;
    }

    synchronized void newFileCleaner() {
        StringWriter msg = new StringWriter();
        if (activeFileCleaner != null && activeFileCleaner.isAlive()) {
            msg.write("Will NOT create a new FileCleaner thread while an old one is still running!\n");
            StackTraceElement[] dump = activeFileCleaner.getStackTrace();
            for (StackTraceElement s : dump) {
                msg.write("\t" + s.toString());
            }
            LOG.error(msg);
        } else {
            activeFileCleaner = new Thread(new FileCleaner());
            activeFileCleaner.setName("SJQ-FileCleaner");
            activeFileCleaner.setDaemon(true);
            activeFileCleaner.start();
        }
    }

    synchronized void newLogCleaner() {
        StringWriter msg = new StringWriter();
        if (activeLogCleaner != null && activeLogCleaner.isAlive()) {
            msg.write("Will NOT create a new LogCleaner while an old one is still running!\n");
            StackTraceElement[] dump = activeLogCleaner.getStackTrace();
            for (StackTraceElement s : dump) {
                msg.write("\t" + s.toString());
            }
            LOG.error(msg);
        } else {
            activeLogCleaner = new Thread(new LogCleaner());
            activeLogCleaner.setName("SJQ-LogCleaner");
            activeLogCleaner.setDaemon(true);
            activeLogCleaner.start();
        }
    }

    synchronized void newVacuumCleaner() {
        StringWriter msg = new StringWriter();
        if (activeVacuumCleaner != null && activeVacuumCleaner.isAlive()) {
            msg.write("Will not create a new VacuumCleaner while an old one is still running!\n");
            StackTraceElement[] dump = activeVacuumCleaner.getStackTrace();
            for (StackTraceElement s : dump) {
                msg.write("\t" + s.toString());
            }
            LOG.error(msg);
        } else {
            activeVacuumCleaner = new Thread(new VacuumCleaner());
            activeVacuumCleaner.setName("SJQ-VacuumCleaner");
            activeVacuumCleaner.setDaemon(true);
            activeVacuumCleaner.start();
        }
    }

    synchronized void wakeupQueueLoader() {
        if (qLoaderThread != null) qLoaderThread.interrupt();
        return;
    }

    synchronized boolean dbgQueueLoader(String path) {
        if (qLoader != null && qLoaderThread != null) {
            if (!qLoader.setDebugFile(path)) return false;
            qLoaderThread.interrupt();
            return true;
        }
        return false;
    }

    synchronized boolean getQueueLoaderDebugStatus() {
        return qLoader != null ? qLoader.getDebugFile() != null : false;
    }

    synchronized String dumpAppState() {
        StackTraceElement[] dump;
        StringWriter msg = new StringWriter();
        msg.write(new Date().toString() + ": SJQ Application Dump\n");
        if (qLoaderThread != null && qLoaderThread.isAlive()) {
            msg.write("\tMediaQueueLoader thread is alive...\n");
            dump = qLoaderThread.getStackTrace();
            for (StackTraceElement s : dump) msg.write("\t\t" + s.toString() + "\n");
        } else msg.write("\tMediaQueueLoader thread is dead!\n");
        if (sysMsgQ != null && sysMsgThread.isAlive()) {
            msg.write("\tSysMsgQueueLoader thread is alive...\n");
            dump = sysMsgThread.getStackTrace();
            for (StackTraceElement s : dump) msg.write("\t\t" + s.toString() + "\n");
        } else msg.write("\tSysMsgQueueLoader thread is dead!\n");
        if (cMonThread != null && cMonThread.isAlive()) {
            msg.write("\tClientMonitor thread is alive...\n");
            dump = cMonThread.getStackTrace();
            for (StackTraceElement s : dump) msg.write("\t\t" + s.toString() + "\n");
        } else msg.write("\tClientMonitor thread is dead!\n");
        if (clntThread != null && clntThread.isAlive()) {
            msg.write("\tInteralTaskClient thread is alive...\n");
            dump = clntThread.getStackTrace();
            for (StackTraceElement s : dump) msg.write("\t\t" + s.toString() + "\n");
        } else msg.write("\tInternalTaskClient thread is dead!\n");
        if (activeLogCleaner != null && activeLogCleaner.isAlive()) {
            msg.write("\tA LogCleaner thread is currently active...\n");
            dump = activeLogCleaner.getStackTrace();
            for (StackTraceElement s : dump) msg.write("\t\t" + s.toString() + "\n");
        } else if (activeLogCleaner != null) msg.write("\tMost recent LogCleaner thread is dead (this is normal behaviour)!\n"); else msg.write("\tNo LogCleaner thread has ever been created (this is ABNORMAL)!\n");
        if (activeFileCleaner != null && activeFileCleaner.isAlive()) {
            msg.write("\tA FileCleaner thread is currently active...\n");
            dump = activeFileCleaner.getStackTrace();
            for (StackTraceElement s : dump) msg.write("\t\t" + s.toString() + "\n");
        } else if (activeFileCleaner != null) msg.write("\tMost recent FileCleaner thread is dead (this is normal behaviour)!\n"); else msg.write("\tNo FileCleaner thread has ever been created (this is ABNORMAL)!\n");
        if (activeVacuumCleaner != null && activeVacuumCleaner.isAlive()) {
            msg.write("\tA VacuumCleaner thread is currently active...\n");
            dump = activeVacuumCleaner.getStackTrace();
            for (StackTraceElement s : dump) msg.write("\t\t" + s.toString() + "\n");
        } else if (activeVacuumCleaner != null) msg.write("\tMost recent VacuumCleaner thread is dead (this is normal behaviour)!\n"); else msg.write("\tNo VacuumCleaner thread has ever been created (this is ABNORMAL)!\n");
        LOG.warn(msg.toString());
        return msg.toString();
    }

    static void delay() {
        long sec = System.currentTimeMillis() % 60000;
        if (sec < 4000 || sec > 56000) try {
            Thread.sleep(12000);
        } catch (InterruptedException e) {
        }
    }
}
