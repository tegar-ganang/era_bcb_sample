package org.eclipse.help.internal.base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Map;
import java.util.Properties;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.help.internal.server.WebappManager;
import org.eclipse.osgi.util.NLS;

/**
 * Help application. Starts webserver and help web application for use by
 * infocenter and stand-alone help. Application takes a parameter "mode", that
 * can take values: "infocenter" - when help system should run as infocenter,
 * "standalone" - when help system should run as standalone.
 */
public class HelpApplication implements IApplication, IExecutableExtension {

    private static final String APPLICATION_LOCK_FILE = ".applicationlock";

    private static final int STATE_EXITING = 0;

    private static final int STATE_RUNNING = 1;

    private static final int STATE_RESTARTING = 2;

    private static int status = STATE_RUNNING;

    private File metadata;

    private FileLock lock;

    public synchronized Object start(IApplicationContext context) throws Exception {
        if (status == STATE_RESTARTING) {
            return EXIT_RESTART;
        }
        metadata = new File(Platform.getLocation().toFile(), ".metadata/");
        if (!BaseHelpSystem.ensureWebappRunning()) {
            System.out.println(NLS.bind(HelpBaseResources.HelpApplication_couldNotStart, Platform.getLogFileLocation().toOSString()));
            return EXIT_OK;
        }
        if (status == STATE_RESTARTING) {
            return EXIT_RESTART;
        }
        writeHostAndPort();
        obtainLock();
        if (BaseHelpSystem.MODE_STANDALONE == BaseHelpSystem.getMode()) {
            DisplayUtils.runUI();
        }
        while (status == STATE_RUNNING) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                break;
            }
        }
        releaseLock();
        if (status == STATE_RESTARTING) {
            return EXIT_RESTART;
        }
        return EXIT_OK;
    }

    public void stop() {
        stopHelp();
        synchronized (this) {
        }
        ;
    }

    /**
	 * Causes help service to stop and exit
	 */
    public static void stopHelp() {
        status = STATE_EXITING;
        if (BaseHelpSystem.MODE_STANDALONE == BaseHelpSystem.getMode()) {
            DisplayUtils.wakeupUI();
        }
    }

    /**
	 * Causes help service to exit and start again
	 */
    public static void restartHelp() {
        if (status != STATE_EXITING) {
            status = STATE_RESTARTING;
        }
    }

    public void setInitializationData(IConfigurationElement configElement, String propertyName, Object data) {
        String value = (String) ((Map) data).get("mode");
        if ("infocenter".equalsIgnoreCase(value)) {
            BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
        } else if ("standalone".equalsIgnoreCase(value)) {
            BaseHelpSystem.setMode(BaseHelpSystem.MODE_STANDALONE);
        }
    }

    private void writeHostAndPort() throws IOException {
        Properties p = new Properties();
        p.put("host", WebappManager.getHost());
        p.put("port", "" + WebappManager.getPort());
        File hostPortFile = new File(metadata, ".connection");
        hostPortFile.deleteOnExit();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(hostPortFile);
            p.store(out, null);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioe2) {
                }
            }
        }
    }

    private void obtainLock() {
        File lockFile = new File(metadata, APPLICATION_LOCK_FILE);
        try {
            RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
            lock = raf.getChannel().lock();
        } catch (IOException ioe) {
            lock = null;
        }
    }

    private void releaseLock() {
        if (lock != null) {
            try {
                lock.channel().close();
            } catch (IOException ioe) {
            }
        }
    }

    public static boolean isRunning() {
        return status == STATE_RUNNING;
    }
}
