package org.knopflerfish.bundle.consoletty;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Dictionary;
import java.util.Hashtable;
import org.knopflerfish.service.console.ConsoleService;
import org.knopflerfish.service.console.Session;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * * Bundle activator implementation. * *
 * 
 * @author Jan Stein *
 * @version $Revision: 1.1.1.1 $
 */
public class ConsoleTty implements BundleActivator, ManagedService, ServiceTrackerCustomizer {

    private static final String logServiceName = LogService.class.getName();

    private static final String consoleServiceName = ConsoleService.class.getName();

    private static final String NONBLOCKING = "nonblocking";

    boolean nonblocking = false;

    private ServiceTracker consoleTracker;

    private Session consoleSession = null;

    private BundleContext bc;

    private Reader reader;

    private PrintWriter writer;

    /**
     * * Called by the framework when this bundle is started. * *
     * 
     * @param bc
     *            Bundle context. *
     * @exception BundleException
     *                shold be thrown when implemented
     */
    public void start(BundleContext bc) throws Exception {
        this.bc = bc;
        log(LogService.LOG_INFO, "Starting");
        Hashtable p = new Hashtable();
        p.put(Constants.SERVICE_PID, getClass().getName());
        bc.registerService(ManagedService.class.getName(), this, p);
        PrintStream out = null;
        try {
            ServiceReference[] srl = bc.getServiceReferences(PrintStream.class.getName(), "(service.pid=java.lang.System.out)");
            if (srl != null && srl.length == 1) {
                out = (PrintStream) bc.getService(srl[0]);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        if (out == null) {
            out = System.out;
        }
        reader = new InputStreamReader(new SystemIn(bc));
        writer = new PrintWriter(out);
        consoleTracker = new ServiceTracker(bc, consoleServiceName, this);
        consoleTracker.open();
    }

    /**
     * * Called by the framework when this bundle is stopped. * *
     * 
     * @param bc
     *            Bundle context.
     */
    public synchronized void stop(BundleContext bc) {
        log(LogService.LOG_INFO, "Stopping");
        consoleTracker.close();
        try {
            if (consoleSession != null) {
                consoleSession.close();
            }
        } catch (Exception e) {
            log(LogService.LOG_ERROR, "Failed to close session", e);
        }
    }

    /**
     * * Called by CM when it got this bundles configuration. * *
     * 
     * @param cfg
     *            contains the new configuration properties.
     */
    public synchronized void updated(Dictionary cfg) throws IllegalArgumentException {
        if (cfg != null) {
            Boolean b = (Boolean) cfg.get(NONBLOCKING);
            if (b != null) {
                nonblocking = b.booleanValue();
            }
        } else {
            nonblocking = false;
        }
    }

    public Object addingService(ServiceReference reference) {
        ConsoleService console = (ConsoleService) bc.getService(reference);
        try {
            consoleSession = console.runSession("console tty", reader, writer);
        } catch (IOException ioe) {
            log(LogService.LOG_ERROR, "Failed to start console session, can not continue");
        }
        return console;
    }

    public void modifiedService(ServiceReference reference, Object service) {
    }

    public void removedService(ServiceReference reference, Object service) {
        if (consoleSession != null) {
            consoleSession.close();
            consoleSession = null;
        }
    }

    /**
     * * Utility method used for logging. * *
     * 
     * @param level
     *            Log level *
     * @param msg
     *            Log message
     */
    public void log(int level, String msg) {
        log(level, msg, null);
    }

    public void log(int level, String msg, Exception e) {
        ServiceReference srLog = bc.getServiceReference(logServiceName);
        if (srLog != null) {
            LogService sLog = (LogService) bc.getService(srLog);
            if (sLog != null) {
                if (e == null) {
                    sLog.log(level, msg);
                } else {
                    sLog.log(level, msg, e);
                }
            }
            bc.ungetService(srLog);
        }
    }

    class SystemIn extends InputStream {

        InputStream in;

        SystemIn(BundleContext bc) {
            try {
                ServiceReference[] srl = bc.getServiceReferences(InputStream.class.getName(), "(service.pid=java.lang.System.in)");
                if (srl != null && srl.length == 1) {
                    in = (InputStream) bc.getService(srl[0]);
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            if (in == null) {
                in = System.in;
            }
        }

        public void close() throws IOException {
            in.close();
        }

        public int available() throws IOException {
            return in.available();
        }

        public int read() throws IOException {
            byte[] b = new byte[1];
            if (read(b) == 1) {
                return b[0];
            }
            return -1;
        }

        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        public int read(byte[] buf, int off, int len) throws IOException {
            if (nonblocking) {
                int nap = 50;
                while (in.available() == 0) {
                    try {
                        Thread.sleep(nap);
                    } catch (InterruptedException e) {
                    }
                    nap = 200;
                }
            }
            return in.read(buf, off, len);
        }
    }
}
