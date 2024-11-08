package org.microemu.applet;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Vector;
import javax.microedition.lcdui.Image;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.swing.Timer;
import org.microemu.DeviceComponent;
import org.microemu.DisplayComponent;
import org.microemu.EmulatorContext;
import org.microemu.MIDletBridge;
import org.microemu.MIDletContext;
import org.microemu.MicroEmulator;
import org.microemu.RecordStoreManager;
import org.microemu.app.launcher.Launcher;
import org.microemu.app.ui.swing.SwingDeviceComponent;
import org.microemu.app.util.MIDletResourceLoader;
import org.microemu.app.util.MIDletSystemProperties;
import org.microemu.device.DeviceDisplay;
import org.microemu.device.DeviceFactory;
import org.microemu.device.FontManager;
import org.microemu.device.InputMethod;
import org.microemu.device.impl.DeviceDisplayImpl;
import org.microemu.device.impl.DeviceImpl;
import org.microemu.device.j2se.J2SEDevice;
import org.microemu.device.j2se.J2SEDeviceDisplay;
import org.microemu.device.j2se.J2SEFontManager;
import org.microemu.device.j2se.J2SEInputMethod;
import org.microemu.log.Logger;
import org.microemu.util.JadMidletEntry;
import org.microemu.util.JadProperties;
import org.microemu.util.MemoryRecordStoreManager;

public class Main extends Applet implements MicroEmulator {

    private static final long serialVersionUID = 1L;

    private MIDlet midlet = null;

    private RecordStoreManager recordStoreManager;

    private JadProperties manifest = new JadProperties();

    private SwingDeviceComponent devicePanel;

    /**
	 * Host name accessible by MIDlet
	 */
    private String accessibleHost;

    private EmulatorContext emulatorContext = new EmulatorContext() {

        private InputMethod inputMethod = new J2SEInputMethod();

        private DeviceDisplay deviceDisplay = new J2SEDeviceDisplay(this);

        private FontManager fontManager = new J2SEFontManager();

        public DisplayComponent getDisplayComponent() {
            return devicePanel.getDisplayComponent();
        }

        public InputMethod getDeviceInputMethod() {
            return inputMethod;
        }

        public DeviceDisplay getDeviceDisplay() {
            return deviceDisplay;
        }

        public FontManager getDeviceFontManager() {
            return fontManager;
        }

        /**
		 * BlueWhaleSystems fix: Ramesh Nair - 16 Nov 2007
		 * 
		 * Initial implementation for vibrate functionality. Phone will now shake sideways when a vibrate is requested.
		 */
        public DeviceComponent getDeviceComponent() {
            return devicePanel;
        }

        public InputStream getResourceAsStream(String name) {
            return getClass().getResourceAsStream(name);
        }
    };

    public Main() {
        devicePanel = new SwingDeviceComponent(false);
        devicePanel.addKeyListener(devicePanel);
    }

    public void init() {
        if (midlet != null) {
            return;
        }
        MIDletSystemProperties.applyToJavaSystemProperties = false;
        MIDletBridge.setMicroEmulator(this);
        URL baseURL = getCodeBase();
        if (baseURL != null) {
            accessibleHost = baseURL.getHost();
        }
        recordStoreManager = new MemoryRecordStoreManager();
        setLayout(new BorderLayout());
        add(devicePanel, "Center");
        DeviceImpl device;
        String deviceParameter = getParameter("device");
        if (deviceParameter == null) {
            device = new J2SEDevice();
            DeviceFactory.setDevice(device);
            device.init(emulatorContext);
        } else {
            try {
                Class cl = Class.forName(deviceParameter);
                device = (DeviceImpl) cl.newInstance();
                DeviceFactory.setDevice(device);
                device.init(emulatorContext);
            } catch (ClassNotFoundException ex) {
                try {
                    device = DeviceImpl.create(emulatorContext, Main.class.getClassLoader(), deviceParameter, J2SEDevice.class);
                    DeviceFactory.setDevice(device);
                } catch (IOException ex1) {
                    Logger.error(ex);
                    return;
                }
            } catch (IllegalAccessException ex) {
                Logger.error(ex);
                return;
            } catch (InstantiationException ex) {
                Logger.error(ex);
                return;
            }
        }
        devicePanel.init();
        manifest.clear();
        try {
            URL url = getClass().getClassLoader().getResource("META-INF/MANIFEST.MF");
            manifest.load(url.openStream());
            if (manifest.getProperty("MIDlet-Name") == null) {
                manifest.clear();
            }
        } catch (IOException e) {
            Logger.error(e);
        }
        String midletClassName = null;
        String jadFile = getParameter("jad");
        if (jadFile != null) {
            InputStream jadInputStream = null;
            try {
                URL jad = new URL(getCodeBase(), jadFile);
                jadInputStream = jad.openStream();
                manifest.load(jadInputStream);
                Vector entries = manifest.getMidletEntries();
                if (entries.size() > 0) {
                    JadMidletEntry entry = (JadMidletEntry) entries.elementAt(0);
                    midletClassName = entry.getClassName();
                }
            } catch (IOException e) {
            } finally {
                if (jadInputStream != null) {
                    try {
                        jadInputStream.close();
                    } catch (IOException e1) {
                    }
                }
            }
        }
        if (midletClassName == null) {
            midletClassName = getParameter("midlet");
            if (midletClassName == null) {
                Logger.debug("There is no midlet parameter");
                return;
            }
        }
        MIDletResourceLoader.classLoader = this.getClass().getClassLoader();
        Class midletClass;
        try {
            midletClass = Class.forName(midletClassName);
        } catch (ClassNotFoundException ex) {
            Logger.error("Cannot find " + midletClassName + " MIDlet class");
            return;
        }
        try {
            midlet = (MIDlet) midletClass.newInstance();
        } catch (Exception ex) {
            Logger.error("Cannot initialize " + midletClass + " MIDlet class", ex);
            return;
        }
        if (((DeviceDisplayImpl) device.getDeviceDisplay()).isResizable()) {
            resize(device.getDeviceDisplay().getFullWidth(), device.getDeviceDisplay().getFullHeight());
        } else {
            resize(device.getNormalImage().getWidth(), device.getNormalImage().getHeight());
        }
        return;
    }

    public void start() {
        devicePanel.requestFocus();
        new Thread("midlet_starter") {

            public void run() {
                try {
                    MIDletBridge.getMIDletAccess(midlet).startApp();
                } catch (MIDletStateChangeException ex) {
                    System.err.println(ex);
                }
            }
        }.start();
        Timer timer = new Timer(1000, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                devicePanel.requestFocus();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    public void stop() {
        MIDletBridge.getMIDletAccess(midlet).pauseApp();
    }

    public void destroy() {
        try {
            MIDletBridge.getMIDletAccess(midlet).destroyApp(true);
        } catch (MIDletStateChangeException ex) {
            System.err.println(ex);
        }
    }

    public RecordStoreManager getRecordStoreManager() {
        return recordStoreManager;
    }

    public String getAppProperty(String key) {
        if (key.equals("applet")) {
            return "yes";
        }
        String value = null;
        if (key.equals("microedition.platform")) {
            value = "MicroEmulator";
        } else if (key.equals("microedition.profiles")) {
            value = "MIDP-2.0";
        } else if (key.equals("microedition.configuration")) {
            value = "CLDC-1.0";
        } else if (key.equals("microedition.locale")) {
            value = Locale.getDefault().getLanguage();
        } else if (key.equals("microedition.encoding")) {
            value = System.getProperty("file.encoding");
        } else if (key.equals("microemu.applet")) {
            value = "true";
        } else if (key.equals("microemu.accessible.host")) {
            value = accessibleHost;
        } else if (getParameter(key) != null) {
            value = getParameter(key);
        } else {
            value = manifest.getProperty(key);
        }
        return value;
    }

    public InputStream getResourceAsStream(String name) {
        return emulatorContext.getResourceAsStream(name);
    }

    public int checkPermission(String permission) {
        return 0;
    }

    public boolean platformRequest(String url) {
        try {
            getAppletContext().showDocument(new URL(url), "mini");
        } catch (Exception e) {
        }
        return false;
    }

    public void notifyDestroyed(MIDletContext midletContext) {
    }

    public void destroyMIDletContext(MIDletContext midletContext) {
    }

    public Launcher getLauncher() {
        return null;
    }

    public String getAppletInfo() {
        return "Title: MicroEmulator \nAuthor: Bartek Teodorczyk, 2001";
    }

    public String[][] getParameterInfo() {
        String[][] info = { { "midlet", "MIDlet class name", "The MIDlet class name. This field is mandatory." } };
        return info;
    }
}
