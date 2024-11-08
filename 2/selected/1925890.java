package com.barteo.emulator.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Locale;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import com.barteo.emulator.EmulatorContext;
import com.barteo.emulator.MIDletBridge;
import com.barteo.emulator.MIDletEntry;
import com.barteo.emulator.MicroEmulator;
import com.barteo.emulator.RecordStoreManager;
import com.barteo.emulator.app.launcher.Launcher;
import com.barteo.emulator.app.ui.ResponseInterfaceListener;
import com.barteo.emulator.app.ui.StatusBarListener;
import com.barteo.emulator.app.util.ProgressEvent;
import com.barteo.emulator.app.util.ProgressJarClassLoader;
import com.barteo.emulator.app.util.ProgressListener;
import com.barteo.emulator.device.Device;
import com.barteo.emulator.device.DeviceFactory;
import com.barteo.emulator.util.JadMidletEntry;
import com.barteo.emulator.util.JadProperties;

public class Common implements MicroEmulator {

    private static Launcher launcher;

    protected EmulatorContext emulatorContext;

    protected String captureFile = null;

    protected JadProperties jad = new JadProperties();

    protected JadProperties manifest = new JadProperties();

    private RecordStoreManager recordStoreManager;

    private StatusBarListener statusBarListener = null;

    private ResponseInterfaceListener responseInterfaceListener = null;

    private ProgressListener progressListener = new ProgressListener() {

        int percent = -1;

        public void stateChanged(ProgressEvent event) {
            int newpercent = (int) ((float) event.getCurrent() / (float) event.getMax() * 100);
            if (newpercent != percent) {
                setStatusBar("Loading... (" + newpercent + " %)");
                percent = newpercent;
            }
        }
    };

    public Common(EmulatorContext context) {
        emulatorContext = context;
        launcher = new Launcher();
        launcher.setCurrentMIDlet(launcher);
        recordStoreManager = new AppRecordStoreManager(launcher);
        MIDletBridge.setMicroEmulator(this);
    }

    public RecordStoreManager getRecordStoreManager() {
        return recordStoreManager;
    }

    public String getAppProperty(String key) {
        if (key.equals("microedition.platform")) {
            return "MicroEmulator";
        } else if (key.equals("microedition.profile")) {
            return "MIDP-1.0";
        } else if (key.equals("microedition.configuration")) {
            return "CLDC-1.0";
        } else if (key.equals("microedition.locale")) {
            return Locale.getDefault().getLanguage();
        } else if (key.equals("microedition.encoding")) {
            return System.getProperty("file.encoding");
        }
        String result = jad.getProperty(key);
        if (result == null) {
            result = manifest.getProperty(key);
        }
        return result;
    }

    public void notifyDestroyed() {
        startMidlet(launcher);
    }

    public void notifySoftkeyLabelsChanged() {
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public MIDlet loadMidlet(String name, Class midletClass) {
        MIDlet result;
        try {
            result = (MIDlet) midletClass.newInstance();
            launcher.addMIDletEntry(new MIDletEntry(name, result));
        } catch (Exception ex) {
            System.out.println("Cannot initialize " + midletClass + " MIDlet class");
            System.out.println(ex);
            ex.printStackTrace();
            return null;
        }
        return result;
    }

    public void openJadFile(URL url) {
        try {
            setStatusBar("Loading...");
            jad.clear();
            jad.load(url.openStream());
            loadFromJad(url);
        } catch (FileNotFoundException ex) {
            System.err.println("Cannot found " + url.getPath());
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            System.err.println("Cannot open jad " + url.getPath());
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            System.err.println("Cannot open jad " + url.getPath());
        } catch (IOException ex) {
            ex.printStackTrace();
            System.err.println("Cannot open jad " + url.getPath());
        }
    }

    public void startMidlet(MIDlet m) {
        try {
            launcher.setCurrentMIDlet(m);
            MIDletBridge.getMIDletAccess(m).startApp();
        } catch (MIDletStateChangeException ex) {
            System.err.println(ex);
        }
    }

    public void setStatusBarListener(StatusBarListener listener) {
        statusBarListener = listener;
    }

    public void setResponseInterfaceListener(ResponseInterfaceListener listener) {
        responseInterfaceListener = listener;
    }

    protected void close() {
        if (captureFile != null) {
        }
    }

    protected void loadFromJad(URL jadUrl) {
        final ProgressJarClassLoader loader = (ProgressJarClassLoader) emulatorContext.getClassLoader();
        setResponseInterface(false);
        URL url = null;
        try {
            if (jadUrl.getProtocol().equals("file")) {
                String tmp = jadUrl.getFile();
                File f = new File(tmp.substring(0, tmp.lastIndexOf('/')), jad.getJarURL());
                url = f.toURL();
            } else {
                url = new URL(jad.getJarURL());
            }
        } catch (MalformedURLException ex) {
            System.err.println(ex);
            setResponseInterface(true);
        }
        loader.addRepository(url);
        launcher.removeMIDletEntries();
        manifest.clear();
        try {
            manifest.load(loader.getResourceAsStream("/META-INF/MANIFEST.MF"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        Thread task = new Thread() {

            public void run() {
                loader.setProgressListener(progressListener);
                launcher.setSuiteName(jad.getSuiteName());
                try {
                    for (Enumeration e = jad.getMidletEntries().elements(); e.hasMoreElements(); ) {
                        JadMidletEntry jadEntry = (JadMidletEntry) e.nextElement();
                        Class midletClass = loader.loadClass(jadEntry.getClassName());
                        loadMidlet(jadEntry.getName(), midletClass);
                    }
                    notifyDestroyed();
                } catch (ClassNotFoundException ex) {
                    System.err.println(ex);
                }
                loader.setProgressListener(null);
                setStatusBar("");
                setResponseInterface(true);
            }
        };
        task.start();
    }

    protected void setDevice(Device device) {
        if (captureFile != null) {
        }
        DeviceFactory.setDevice(device);
        if (captureFile != null) {
        }
    }

    private void setStatusBar(String text) {
        if (statusBarListener != null) {
            statusBarListener.statusBarChanged(text);
        }
    }

    private void setResponseInterface(boolean state) {
        if (responseInterfaceListener != null) {
            responseInterfaceListener.stateChanged(state);
        }
    }
}
