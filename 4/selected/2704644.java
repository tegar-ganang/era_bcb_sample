package org.freehold.servomaster.device.impl.usb;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.usb.UsbConfiguration;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbServices;
import javax.usb.event.UsbServicesEvent;
import javax.usb.event.UsbServicesListener;
import org.freehold.servomaster.device.model.AbstractServoController;
import org.freehold.servomaster.device.model.HardwareServo;
import org.freehold.servomaster.device.model.Meta;
import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.ServoController;

/**
 * Base class for all USB servo controllers.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2005
 * @version $Id: AbstractUsbServoController.java,v 1.21 2006-12-14 09:17:11 vtt Exp $
 */
public abstract class AbstractUsbServoController extends AbstractServoController implements UsbServicesListener {

    private UsbHub virtualRootHub;

    /**
     * The revision to protocol handler map.
     *
     * <p>
     *
     * The key is the string formed as "${vendor-id}:${product-id}" (with
     * IDs being lowercase hex representations, no leading "0x". A
     * convenient way to obtain a signature is to call {@link #getSignature
     * getSignature()}), the value is the protocol handler. This is a little
     * bit of overhead, but adds flexibility.
     *
     * <p>
     *
     * At the instantiation time, the protocol handlers for all known
     * hardware revisions are instantiated and put into this map.
     *
     * <p>
     *
     * At the {@link #init init()} time, the hardware revision is looked up,
     * the proper protocol handler resolved and assigned to the {@link
     * #protocolHandler instance protocol handler}.
     */
    private Map<String, UsbProtocolHandler> protocolHandlerMap = new HashMap<String, UsbProtocolHandler>();

    /**
     * The protocol handler taking care of this specific instance.
     *
     * @see #init
     */
    protected UsbProtocolHandler protocolHandler;

    /**
     * Used to prevent polling too fast.
     *
     * VT: FIXME: This may not be necessary if javax.usb properly supports
     * arrival and departure notifications.
     */
    private long lastDetect;

    /**
     * The USB device corresponding to the servo controller.
     */
    protected UsbDevice theServoController;

    protected AbstractUsbServoController() {
        fillProtocolHandlerMap();
        if (isOnly()) {
            protocolHandler = (UsbProtocolHandler) protocolHandlerMap.values().toArray()[0];
            servoSet = new Servo[getServoCount()];
        }
        try {
            UsbServices usbServices = UsbHostManager.getUsbServices();
            virtualRootHub = usbServices.getRootUsbHub();
            usbServices.addUsbServicesListener(this);
        } catch (UsbException usbex) {
            throw (IllegalStateException) new IllegalStateException("USB failure").initCause(usbex);
        }
    }

    /**
     * Fill the protocol handler map.
     *
     * This class puts all the protocol handlers known to it into the {@link
     * #protocolHandlerMap protocol handler map}, so the devices can be
     * recognized. However, this presents some difficulties in relation to
     * disconnected mode if a particular, known beforehand, type of device
     * with a known identifier must be operated, but it is not be present at
     * the driver startup time. If this is the case, then subclasses of this
     * class must be used that fill the protocol handler map
     * with the only protocol handler.
     *
     * @see #isOnly
     */
    protected abstract void fillProtocolHandlerMap();

    protected final void registerHandler(String signature, UsbProtocolHandler handler) {
        protocolHandlerMap.put(signature, handler);
    }

    /**
     * Is this class handling only one type of a device?
     *
     * @return true if only one type of a USB controller is supported.
     */
    protected boolean isOnly() {
        return protocolHandlerMap.size() == 1;
    }

    /**
     * Is the device currently connected?
     *
     * <p>
     *
     * This method will check the presence of the device and return the
     * status.
     *
     * @return true if the device seems to be connected.
     */
    @Override
    public final synchronized boolean isConnected() {
        checkInit();
        if (theServoController != null) {
            return true;
        } else {
            try {
                theServoController = findUSB(portName);
                UsbConfiguration cf = theServoController.getActiveUsbConfiguration();
                UsbInterface iface = cf.getUsbInterface((byte) 0x00);
                if (iface.isClaimed()) {
                    throw new IOException("Can't claim interface - already claimed. " + "Make sure no other applications or modules (hid.o or phidgetservo.o in particular) use this device");
                }
                iface.claim();
                return true;
            } catch (Throwable t) {
                exception(t);
                return false;
            }
        }
    }

    protected final UsbDevice findUSB(String portName) throws IOException {
        try {
            if (System.currentTimeMillis() - lastDetect < 1000) {
                throw new IOException("Polling too fast");
            }
            lastDetect = System.currentTimeMillis();
            Set<UsbDevice> found = new HashSet<UsbDevice>();
            try {
                find(portName, virtualRootHub, found, true);
            } catch (BootException bex) {
                throw new IllegalStateException("BootException shouldn't have propagated here", bex);
            }
            if (found.size() == 1) {
                return (UsbDevice) found.toArray()[0];
            }
            if (portName == null) {
                if (found.isEmpty()) {
                    throw new IOException("No compatible devices found. Make sure you have /proc/bus/usb read/write permissions.");
                } else {
                    tooManyDevices(found);
                }
            } else {
                if (found.isEmpty()) {
                    throw new IOException("Device with a serial number '" + portName + "' is not connected");
                } else {
                    tooManyDevices(found);
                }
            }
            throw new IOException("No device found with serial number " + portName);
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("\nMake sure you have the directory containing libJavaxUsb.so in your LD_LIBRARY_PATH\n");
            throw ule;
        } catch (UsbException usbex) {
            throw (IOException) new IOException("USB failure").initCause(usbex);
        }
    }

    /**
     * Find the controller.
     *
     * @param portName Port name.
     *
     * @param root Device to start walking down from. Quite possibly this
     * may be the device being eventually added to the set. Can be <code>null</code>.
     *
     * @param found Set to put the found devices into.
     *
     * @param boot Whether to boot the device if one is found.
     *
     * @throws IOException if there's a hardware error.
     * @throws UsbException if there's a USB protocol level error.
     * @throws BootException if one of the devices encountered in the enumeration had to be booted,
     * thus interrupting normal enumeration flow.
     */
    private void find(String portName, UsbDevice root, Set<UsbDevice> found, boolean boot) throws IOException, UsbException, BootException {
        if (root == null) {
            return;
        }
        if (root.isUsbHub()) {
            List<UsbDevice> devices = ((UsbHub) root).getAttachedUsbDevices();
            for (Iterator<UsbDevice> i = devices.iterator(); i.hasNext(); ) {
                try {
                    find(portName, i.next(), found, true);
                } catch (BootException bex) {
                    try {
                        find(portName, root, found, false);
                        return;
                    } catch (BootException bex2) {
                        System.err.println("Failed to boot a bootable device");
                    }
                }
            }
        } else {
            UsbDeviceDescriptor dd = root.getUsbDeviceDescriptor();
            String signature = getSignature(dd);
            UsbProtocolHandler handler = protocolHandlerMap.get(signature);
            if (handler != null) {
                if (handler.isBootable()) {
                    if (!boot) {
                        throw new BootException("Second time, refusing to boot");
                    }
                    handler.boot(root);
                    throw new BootException("Need to rescan the bus - device booted");
                }
                if (portName == null) {
                    found.add(root);
                    return;
                }
                String serial = root.getSerialNumberString();
                System.err.println("Serial found: " + serial);
                if (serial == null) {
                    serial = "null";
                }
                if (serial.equals(portName)) {
                    found.add(root);
                    return;
                }
            }
            System.err.println("Unknown device: " + signature);
        }
    }

    /**
     * Get device signature, in "${vendor-id}:${product-id}" form.
     *
     * @param dd Device descriptor to extract the signature from.
     *
     * @return Device signature.
     */
    protected final String getSignature(UsbDeviceDescriptor dd) {
        return Integer.toHexString(dd.idVendor() & 0xFFFF) + ":" + Integer.toHexString(dd.idProduct() & 0xFFFF);
    }

    /**
     * Get a protocol handler for a device, if one exists.
     *
     * @param target Device to get the handler for.
     * @return The protocol handler, or {@code null} if there's none.
     */
    protected final UsbProtocolHandler getProtocolHandler(UsbDevice target) {
        String signature = getSignature(target.getUsbDeviceDescriptor());
        return protocolHandlerMap.get(signature);
    }

    /**
     * Initialize the controller.
     *
     * @param portName The controller board unique serial number in a string
     * representation. If this is null, then all the PhidgetServo devices
     * connected will be found. If the only device is found, then it is
     * used, and its serial number will be assigned to
     * <code>portName</code>.
     *
     * @exception IllegalArgumentException if the <code>portName</code> is
     * null and none or more than one device were found, or the device
     * corresponding to the name specified is not currently connected and
     * {@link #allowDisconnect disconnected mode} is not enabled.
     *
     * @exception UnsupportedOperationException if the device revision is
     * not supported by this driver.
     */
    @Override
    protected void doInit(String portName) throws IOException {
        try {
            theServoController = findUSB(portName);
            UsbConfiguration cf = theServoController.getActiveUsbConfiguration();
            UsbInterface iface = cf.getUsbInterface((byte) 0x00);
            if (iface.isClaimed()) {
                throw new IOException("Can't claim interface - already claimed. " + "Make sure no other applications or modules (hid.o or phidgetservo.o in particular) use this device");
            }
            iface.claim();
            UsbDeviceDescriptor dd = theServoController.getUsbDeviceDescriptor();
            String serial = theServoController.getSerialNumberString();
            String signature = getSignature(dd);
            if (serial == null) {
                serial = "null";
            }
            protocolHandler = protocolHandlerMap.get(signature);
            if (protocolHandler == null) {
                throw new UnsupportedOperationException("Vendor/product ID '" + signature + "' is not supported");
            }
            servoSet = new Servo[getServoCount()];
            this.portName = serial;
            connected = true;
        } catch (Throwable t) {
            exception(t);
            if (isDisconnectAllowed() && portName != null) {
                this.portName = portName;
                synchronized (System.err) {
                    System.err.println("Working in the disconnected mode, cause:");
                    t.printStackTrace();
                }
                return;
            }
            if (t instanceof IOException) {
                throw (IOException) t;
            }
            throw (IOException) (new IOException().initCause(t));
        }
        for (Iterator<Servo> i = getServos(); i.hasNext(); ) {
            Servo s = i.next();
            s.setPosition(0.5);
        }
    }

    @Override
    public synchronized Meta getMeta() {
        checkInit();
        if (protocolHandler == null) {
            throw new IllegalStateException("Hardware not yet connected, try later");
        }
        return protocolHandler.getMeta();
    }

    @Override
    protected synchronized void checkInit() {
        if (protocolHandler == null && portName == null) {
            throw new IllegalStateException("Not initialized");
        }
    }

    public synchronized void reset() throws IOException {
        checkInit();
        if (protocolHandler != null) {
            try {
                protocolHandler.reset();
            } catch (UsbException usbex) {
                throw (IOException) new IOException("Failed to reset USB device").initCause(usbex);
            }
        }
    }

    public int getServoCount() {
        checkInit();
        if (protocolHandler == null) {
            throw new IllegalStateException("Not Initialized (can't determine hardware type?)");
        }
        return protocolHandler.getServoCount();
    }

    /**
     * Create the servo instance.
     *
     * This is a template method used to instantiate the proper servo
     * implementation class.
     *
     * @param id Servo ID to create.
     *
     * @exception IOException if there was a problem communicating with the
     * hardware controller.
     */
    @Override
    protected Servo createServo(int id) throws IOException {
        return protocolHandler.createServo(this, id);
    }

    public final synchronized void usbDeviceAttached(UsbServicesEvent e) {
        try {
            System.out.println("*** USB device attached: " + e.getUsbDevice().getProductString());
            UsbDevice arrival = e.getUsbDevice();
            UsbProtocolHandler handler = getProtocolHandler(arrival);
            if (handler == null) {
                return;
            }
            if (handler.isBootable()) {
                handler.boot(arrival);
                return;
            }
            String arrivalSerial = arrival.getSerialNumberString();
            if (portName != null) {
                if (portName.equals(arrivalSerial)) {
                    theServoController = arrival;
                    UsbConfiguration cf = theServoController.getActiveUsbConfiguration();
                    UsbInterface iface = cf.getUsbInterface((byte) 0x00);
                    if (iface.isClaimed()) {
                        throw new IOException("Can't claim interface - already claimed. " + "Make sure no other applications or modules (hid.o or phidgetservo.o in particular) use this device");
                    }
                    iface.claim();
                    protocolHandler = handler;
                    handler.reset();
                    System.err.println("*** Restored device");
                    return;
                } else {
                    return;
                }
            } else {
                throw new Error("Not Implemented: restoring a device with null serial number");
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public final synchronized void usbDeviceDetached(UsbServicesEvent e) {
        try {
            System.out.println("*** USB device detached: " + e.getUsbDevice().getProductString());
            UsbDevice departure = e.getUsbDevice();
            if (departure == theServoController) {
                connected = false;
                theServoController = null;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Unconditionally throw the <code>IOException</code>.
     *
     * @param found Set of devices found during enumeration.
     * @exception IOException with the list of device serial numbers found.
     * @throws UsbException if there was a problem on a USB protocol level.
     */
    private void tooManyDevices(Set<UsbDevice> found) throws IOException, UsbException {
        String message = "No port name specified, multiple PhidgetServo devices found:";
        for (Iterator<UsbDevice> i = found.iterator(); i.hasNext(); ) {
            UsbDevice next = i.next();
            String serial = next.getSerialNumberString();
            message += " " + serial;
        }
        throw new IOException(message);
    }

    /**
     * This exception gets thrown whenever there was a USB device (such as a
     * SoftPhidget) that had to be booted, therefore the normal device
     * enumeration was broken.
     */
    protected class BootException extends Exception {

        BootException(String message) {
            super(message);
        }
    }

    /**
     * An abstraction for the object handling the communications with the
     * arbitrary hardware revision of the PhidgetServo controller.
     *
     * <p>
     *
     * For every hardware revision, there will be a separate protocol
     * handler.
     */
    protected abstract class UsbProtocolHandler {

        /**
         * Controller metadata.
         */
        private final Meta meta;

        protected UsbProtocolHandler() {
            if (isBootable()) {
                meta = null;
                return;
            }
            meta = createMeta();
        }

        protected abstract Meta createMeta();

        public final Meta getMeta() {
            return meta;
        }

        /**
         * Whether a device is bootable.
         *
         * Generally, it's not the case, this is why the default
         * implementation returning false is provided.
         *
         * @return true if the device is bootable.
         */
        public boolean isBootable() {
            return false;
        }

        public void boot(UsbDevice target) throws UsbException {
            throw new IllegalAccessError("Operation not supported");
        }

        /**
         * Get the device model name.
         *
         * This method is here because the protocol handlers are create
         * before the actual devices are found. Ideally, the model name
         * should be retrieved from the USB device (and possibly, it will be
         * done so later), but so far this will do.
         *
         * @return Human readable model name.
         */
        protected abstract String getModelName();

        /**
         * Reset the controller.
         *
         * @throws UsbException if there was a problem at USB protocol level.
         */
        public abstract void reset() throws UsbException;

        /**
         * @return the number of servos the controller supports.
         */
        public abstract int getServoCount();

        /**
         * Set the servo position.
         *
         * <p>
         *
         * <strong>NOTE:</strong> originally this method was named
         * <code>setActualPosition()</code>. This worked all right with JDK
         * 1.4.1, however, later it turned out that JDK 1.3.1 was not able
         * to properly resolve the names and thought that this method
         * belongs to <code>PhidgetServo</code>, though the signature was
         * different. The name was changed to satisfy JDK 1.3.1, but this
         * points out JDK 1.3.1's deficiency in handling the inner classes.
         * Caveat emptor. You better upgrade.
         *
         * @param id Servo number.
         *
         * @param position Desired position.
         *
         * @exception UsbException if there was a problem sending data to
         * the USB device.
         */
        public abstract void setPosition(int id, double position) throws UsbException;

        /**
         * Silence the controller.
         *
         * VT: FIXME: This better be deprecated - each servo can be silenced
         * on its own
         *
         * @throws UsbException if there was a problem at USB protocol level.
         */
        public abstract void silence() throws UsbException;

        public abstract Servo createServo(ServoController sc, int id) throws IOException;

        public abstract class UsbServo extends HardwareServo {

            protected UsbServo(ServoController sc, int id) throws IOException {
                super(sc, id);
            }

            @Override
            protected final void setActualPosition(double position) throws IOException {
                checkInit();
                checkPosition(position);
                try {
                    protocolHandler.setPosition(id, position);
                    actualPosition = position;
                    actualPositionChanged();
                } catch (UsbException usbex) {
                    connected = false;
                    theServoController = null;
                    if (!isDisconnectAllowed()) {
                        throw (IOException) new IOException("Device departed, disconnect not allowed").initCause(usbex);
                    }
                    System.err.println("Assumed disconnect, reason:");
                    usbex.printStackTrace();
                }
            }
        }
    }
}
