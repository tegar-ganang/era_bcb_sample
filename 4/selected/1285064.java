package usb.linux;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import usb.core.*;

/**
 * Provides access to native USB host object for this process.
 *
 * @see usb.remote.HostProxy
 *
 * @author David Brownell
 * @version $Id: Linux.java,v 1.3 2000/12/15 19:02:21 dbrownell Exp $
 */
public final class Linux extends HostFactory {

    static final boolean trace = false;

    static final boolean debug = false;

    private static Linux.HostImpl self;

    /**
     * Not part of the API; implements reference implementation SPI.
     */
    public Linux() {
    }

    /**
     * Not part of the API; implements reference implementation SPI.
     */
    public Host createHost() throws IOException {
        return Linux.getHost();
    }

    /**
     * Provides access to the singleton USB Host.
     * This creates a "USB Watcher" daemon thread, which
     * keeps USB device and bus connectivity data current.
     *
     * @return the host, or null if USB support isn't available.
     * @exception IOException for file access problems
     * @exception SecurityException when usbdevfs hasn't been set
     *	up to allow this process to read and write all device nodes
     * @exception RuntimeException various runtime exceptions may
     *	be thrown if the USB information provided by the system
     *	doesn't appear to make sense.
     */
    public static Host getHost() throws IOException, SecurityException {
        synchronized (Host.class) {
            if (self == null) {
                File f = new File("/proc/bus/usb");
                if (!f.exists() || !f.isDirectory()) {
                    System.err.println("Java USB for Linux needs usbdevfs to run.");
                    return null;
                }
                if (!"libgcj".equals(System.getProperty("java.vm.name"))) System.loadLibrary("jusb");
                self = new Linux.HostImpl(f);
            }
        }
        return self;
    }

    private static String devfsPath;

    private static Watcher watcher;

    private static Thread daemon;

    /**
     * Represents a Linux host associated with one or more
     * Universal Serial Busses (USBs).
     */
    private static final class HostImpl implements Host {

        private final transient Hashtable busses = new Hashtable(3);

        private final transient Vector listeners = new Vector(3);

        HostImpl(File directory) throws IOException, SecurityException {
            super();
            devfsPath = directory.getAbsolutePath();
            watcher = new Watcher(directory, busses, listeners);
            daemon = new Thread(watcher, "USB-Watcher");
            daemon.setDaemon(true);
            daemon.start();
        }

        protected void finalize() {
            watcher.halt();
            daemon.interrupt();
        }

        public String toString() {
            return "Linux usbfs";
        }

        /**
	 * Returns an array of objects representing the USB busses currently
	 * in this system.
	 */
        public Bus[] getBusses() {
            synchronized (busses) {
                Bus retval[] = new Bus[busses.size()];
                int i = 0;
                for (Enumeration e = busses.keys(); e.hasMoreElements(); ) retval[i++] = (Bus) busses.get(e.nextElement());
                return retval;
            }
        }

        public usb.core.Device getDevice(String portId) throws IOException {
            return new PortIdentifier(portId).getDevice(this);
        }

        /** Adds a callback for USB structure changes */
        public void addUSBListener(USBListener l) {
            if (l == null) throw new IllegalArgumentException();
            listeners.addElement(l);
        }

        /** Removes a callback for USB structure changes */
        public void removeUSBListener(USBListener l) {
            listeners.removeElement(l);
        }
    }

    static final int POLL_PERIOD = 2;

    /**
     * Scan for bus additions/removals/changes, delegating
     * most work to the 
     */
    private static final class Watcher implements Runnable {

        private File dir;

        private File devices;

        private final Hashtable busses;

        private final Vector listeners;

        private long lastTime;

        Watcher(File d, Hashtable b, Vector l) throws IOException, SecurityException {
            dir = d;
            devices = new File(dir, "devices");
            busses = b;
            listeners = l;
            if (!dir.exists() || !dir.isDirectory()) throw new IOException("is usbdevfs mounted?  " + d.getAbsolutePath());
            while (scan()) continue;
            if (busses.isEmpty()) throw new IOException("no devices; maybe usbdevfs denies read/write access?");
        }

        public void run() {
            while (dir != null) {
                while (scan()) continue;
                try {
                    Thread.sleep(POLL_PERIOD * 1000);
                } catch (InterruptedException e) {
                }
            }
        }

        void halt() {
            dir = null;
        }

        private boolean scan() throws SecurityException {
            boolean changed = false;
            synchronized (busses) {
                long current = System.currentTimeMillis();
                long mtime = devices.lastModified();
                if (lastTime > mtime) {
                    if (trace) System.err.println("Host.scan: unmodified");
                    return false;
                }
                if (trace) System.err.println("Host.scan: modified ...");
                String kids[] = dir.list();
                Vector seen;
                if (kids.length < 2) throw new IllegalArgumentException(dir.getAbsolutePath());
                seen = new Vector(kids.length - 2);
                for (int i = 0; i < kids.length; i++) {
                    int busnum;
                    try {
                        busnum = Integer.parseInt(kids[i]);
                        seen.addElement(kids[i]);
                        USB bus = (USB) busses.get(kids[i]);
                        if (bus == null) {
                            mkBus(kids[i], busnum);
                            changed = true;
                        } else {
                            while (bus.scanBus()) changed = true;
                        }
                    } catch (IOException e) {
                        System.err.println("I/O problem: " + kids[i]);
                        e.printStackTrace();
                    } catch (SecurityException e) {
                        throw e;
                    } catch (Exception e) {
                        if ("devices".equals(kids[i])) continue;
                        if ("drivers".equals(kids[i])) continue;
                        System.err.println("Not a usbdevfs bus: " + kids[i]);
                        e.printStackTrace();
                    }
                }
                for (Enumeration e = busses.keys(); e.hasMoreElements(); ) {
                    Object busname = e.nextElement();
                    if (!seen.contains(busname)) {
                        if (trace) System.err.println("bus gone: " + busname);
                        rmBus(busname);
                        changed = true;
                    }
                }
                lastTime = current;
            }
            return changed;
        }

        private void rmBus(Object busname) {
            USB bus = (USB) busses.get(busname);
            if (trace) System.err.println("rmBus " + bus);
            for (int i = 0; i < listeners.size(); i++) {
                USBListener listener;
                listener = (USBListener) listeners.elementAt(i);
                try {
                    listener.busRemoved(bus);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            busses.remove(busname);
            bus.kill();
        }

        private void mkBus(String busname, int busnum) throws IOException, SecurityException {
            USB bus;
            bus = new USB(dir, busname, busnum, listeners, self);
            if (trace) System.err.println("mkBus " + bus);
            busses.put(busname, bus);
            for (int i = 0; i < listeners.size(); i++) {
                USBListener listener;
                listener = (USBListener) listeners.elementAt(i);
                try {
                    listener.busAdded(bus);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            while (bus.scanBus()) continue;
        }
    }
}
