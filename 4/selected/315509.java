package net.sourceforge.cridmanager.box;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import net.sourceforge.cridmanager.FsFile;
import net.sourceforge.cridmanager.IFile;
import net.sourceforge.cridmanager.ILocation;
import net.sourceforge.cridmanager.ISettings;
import net.sourceforge.cridmanager.Utils;
import net.sourceforge.cridmanager.box.channel.ChannelManager;
import net.sourceforge.cridmanager.box.firmware.FirmwareFactory;
import net.sourceforge.cridmanager.box.logging.DirectoryLogger;
import net.sourceforge.cridmanager.timer.ITimerManager;
import net.sourceforge.cridmanager.timer.RcTimerManager;
import net.sourceforge.cridremote.OsdReaderThread;
import net.sourceforge.cridremote.RemoteControl;
import org.apache.log4j.Logger;
import test.fp.UnaryFunction;

/**
 * Basis-Implementierung einer Box.
 */
public abstract class BaseBox implements IBox {

    private static Logger logger = Logger.getLogger("net.sourceforge.cridmanager.box.BaseBox");

    private static final String SERVICES_FILENAME = "/var/etc/services.txt";

    private static final String USER_SERVICES_FILENAME = "/var/etc/user_services.txt";

    protected String host;

    private IBoxFeatures features;

    protected Collection listeners;

    protected String name;

    protected boolean running;

    private RemoteControl rc;

    private OsdReaderThread osd;

    private boolean restart;

    private ChannelManager channelManager;

    protected String mac;

    private String[] fileNames = new String[] { SERVICES_FILENAME, USER_SERVICES_FILENAME };

    private boolean[] cacheFlags;

    private File cacheDir;

    private ILocation[] locations;

    private ITimerManager timerManager;

    private PingThread thread;

    public BaseBox(String host, String name, String mac) {
        this(host, name, mac, null);
    }

    public BaseBox(String host, String name, String mac, IBoxFeatures firmware) {
        this.host = host;
        this.name = name;
        this.mac = mac;
        locations = new ILocation[USB_HDD_2 + 1];
        initCache(mac);
        running = false;
        restart = false;
        if (firmware == null) {
            features = new DummyBoxfeatures();
        } else {
            features = firmware;
        }
        listeners = new ArrayList();
        startPing();
    }

    /**
	 * 
	 */
    private void startPing() {
        if (logger.isDebugEnabled()) {
            logger.debug("startPing() - start");
        }
        if (isNetworked() && thread == null) {
            thread = new PingThread();
            thread.start();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("startPing() - end");
        }
    }

    private boolean initCache(String mac) {
        if (logger.isDebugEnabled()) {
            logger.debug("initCache(String) - start");
        }
        cacheFlags = new boolean[fileNames.length];
        clearCacheflags();
        boolean result = false;
        if (mac.length() > 0) {
            String cachePath = ISettings.CACHE_DIR + "/" + mac;
            cacheDir = Utils.getUserHome(cachePath);
            if (!cacheDir.exists()) cacheDir.mkdirs();
            result = true;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("initCache(String) - end");
        }
        return result;
    }

    private void clearCacheflags() {
        if (logger.isDebugEnabled()) {
            logger.debug("clearCacheflags() - start");
        }
        for (int i = 0; i < cacheFlags.length; i++) {
            cacheFlags[i] = true;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("clearCacheflags() - end");
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        if (logger.isDebugEnabled()) {
            logger.debug("setHost(String) - start");
        }
        if (!this.host.equals(host)) {
            this.host = host;
            if (thread != null) {
                thread.kill();
                thread.interrupt();
                thread = null;
                startPing();
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("setHost(String) - end");
        }
    }

    public IBoxFeatures getFeatures() {
        return features;
    }

    public boolean isRunning() {
        return running;
    }

    protected void setRunning(boolean running) {
        if (logger.isDebugEnabled()) {
            logger.debug("setRunning(boolean) - start");
        }
        if (running != this.running) {
            this.running = running;
            if (running) {
                features = FirmwareFactory.getBoxFeatures(this);
                clearRestart();
                clearCacheflags();
            } else {
                if (osd != null) {
                    osd.setInaktiv();
                    osd = null;
                }
            }
            fireBoxAccessStateChanged(BoxEvent.BOX_POWER);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("setRunning(boolean) - end");
        }
    }

    public boolean needsRestart() {
        return restart;
    }

    public void setRestart() {
        if (logger.isDebugEnabled()) {
            logger.debug("setRestart() - start");
        }
        if (!restart) {
            restart = true;
            fireBoxAccessStateChanged(BoxEvent.BOX_RESTART);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("setRestart() - end");
        }
    }

    private void clearRestart() {
        if (logger.isDebugEnabled()) {
            logger.debug("clearRestart() - start");
        }
        if (restart) {
            restart = false;
            fireBoxAccessStateChanged(BoxEvent.BOX_RESTART);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("clearRestart() - end");
        }
    }

    public void addBoxListener(IBoxListener newListener) {
        if (logger.isDebugEnabled()) {
            logger.debug("addBoxListener(IBoxListener) - start");
        }
        if (!listeners.contains(newListener)) listeners.add(newListener);
        if (logger.isDebugEnabled()) {
            logger.debug("addBoxListener(IBoxListener) - end");
        }
    }

    public void removeBoxListener(IBoxListener oldListener) {
        if (logger.isDebugEnabled()) {
            logger.debug("removeBoxListener(IBoxListener) - start");
        }
        listeners.remove(oldListener);
        if (logger.isDebugEnabled()) {
            logger.debug("removeBoxListener(IBoxListener) - end");
        }
    }

    protected void fireBoxAccessStateChanged(int type) {
        if (logger.isDebugEnabled()) {
            logger.debug("fireBoxAccessStateChanged(int) - start");
        }
        BoxEvent evt = new BoxEvent(this, type);
        for (Iterator iter = listeners.iterator(); iter.hasNext(); ) {
            IBoxListener listener = (IBoxListener) iter.next();
            listener.boxAccessStateChanged(evt);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("fireBoxAccessStateChanged(int) - end");
        }
    }

    /**
	 * Liefert die Referenz auf eine Datei auf der Box. Anzugeben ist der absolute Pfad, also z.B.
	 * /var/etc/settings.txt.
	 * <p>
	 * Voraussetzung ist ein Root-Zugriff auf die Box.
	 * 
	 * @param path Pfad zur Datei
	 * @return Zugriffsobjekt
	 */
    public IFile getBoxFile(String path) {
        if (logger.isDebugEnabled()) {
            logger.debug("getBoxFile(String) - start");
        }
        IFile result = null;
        if (isRunning() && getFeatures().hasFtpAccess() && getFeatures().isFtpActive() && getFeatures().hasRootFtpAccess()) {
            int lastSlash = path.lastIndexOf('/');
            String boxPath = path.substring(0, lastSlash);
            String boxFilename = path.substring(lastSlash + 1);
            ILocation boxDir = getFeatures().getFtpDir(boxPath, host);
            if (boxDir != null) {
                IFile boxFile = boxDir.getFile(boxFilename);
                result = boxFile;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getBoxFile(String) - end");
        }
        return result;
    }

    /**
	 * Liefert die Referenz auf ein Verzeichnis auf der Box. Anzugeben ist der absolute Pfad, also
	 * z.B. /data/.timer.
	 * <p>
	 * Voraussetzung ist ein Root-Zugriff auf die Box.
	 * 
	 * @param path Pfad zur Datei
	 * @return Zugriffsobjekt
	 */
    public ILocation getBoxDir(String path) {
        if (logger.isDebugEnabled()) {
            logger.debug("getBoxDir(String) - start");
        }
        ILocation result = null;
        if (isRunning() && getFeatures().hasFtpAccess() && getFeatures().isFtpActive() && getFeatures().hasRootFtpAccess()) {
            ILocation boxDir = getFeatures().getFtpDir(path, host);
            if (boxDir != null) {
                result = boxDir;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getBoxDir(String) - end");
        }
        return result;
    }

    public IFile getSystemFile(int which) {
        if (logger.isDebugEnabled()) {
            logger.debug("getSystemFile(int) - start");
        }
        if (which < 0 || which >= fileNames.length) throw new IllegalArgumentException("Unbekannte Systemdatei angefordert.");
        String filename = fileNames[which];
        IFile result = null;
        if (cacheFlags[which]) {
            result = getBoxFile(filename);
            if (result != null) {
                result = copyFileToCache(result, filename);
                cacheFlags[which] = result == null;
            }
        }
        if (result == null) result = getSystemFileFromCache(filename);
        if (logger.isDebugEnabled()) {
            logger.debug("getSystemFile(int) - end");
        }
        return result;
    }

    private IFile copyFileToCache(IFile boxFile, String filename) {
        if (logger.isDebugEnabled()) {
            logger.debug("copyFileToCache(IFile, String) - start");
        }
        IFile result = null;
        try {
            InputStream in = boxFile.getInput();
            File cacheFile = getCacheFile(filename);
            OutputStream out = new BufferedOutputStream(new FileOutputStream(cacheFile));
            int read = in.read();
            while (read >= 0) {
                out.write(read);
                read = in.read();
            }
            out.close();
            in.close();
            result = new FsFile(cacheFile);
        } catch (IOException e) {
            logger.error("copyFileToCache(IFile, String)", e);
            e.printStackTrace();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("copyFileToCache(IFile, String) - end");
        }
        return result;
    }

    /**
	 * Liefert eine System-Datei aus den Cache. Dabei wird zun�chst nach einer Datei gesucht, die dem
	 * kompletten Pfad der Systemdatei entspricht, wobei die '/' durch '$' ersetzt werden, also z.B.
	 * $var$etc$services.txt. Wenn die Datei nicht gefunden wird, wird nach einer Datei gesucht, die
	 * dem �bergebenen Namen ohne Pfad entspricht, also z.B. services.txt.
	 * 
	 * @param boxname
	 * @return
	 */
    private IFile getSystemFileFromCache(String boxname) {
        if (logger.isDebugEnabled()) {
            logger.debug("getSystemFileFromCache(String) - start");
        }
        IFile result = null;
        File f = getCacheFile(boxname);
        if (!f.exists() && boxname.indexOf('/') >= 0) {
            String name = boxname.substring(boxname.lastIndexOf('/') + 1);
            f = new File(cacheDir, name);
        }
        if (f.exists()) result = new FsFile(f);
        if (logger.isDebugEnabled()) {
            logger.debug("getSystemFileFromCache(String) - end");
        }
        return result;
    }

    /**
	 * @param boxname
	 * @return
	 */
    private File getCacheFile(String boxname) {
        if (logger.isDebugEnabled()) {
            logger.debug("getCacheFile(String) - start");
        }
        String localBoxname = boxname.replace('/', '$');
        File f = new File(cacheDir, localBoxname);
        if (logger.isDebugEnabled()) {
            logger.debug("getCacheFile(String) - end");
        }
        return f;
    }

    /**
	 * @return Returns the name.
	 */
    public String getName() {
        return name;
    }

    /**
	 * @param name The name to set.
	 */
    public void setName(String name) {
        this.name = name;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public RemoteControl getRemoteControl() {
        if (logger.isDebugEnabled()) {
            logger.debug("getRemoteControl() - start");
        }
        if (rc == null) {
            rc = new RemoteControl(this, null);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getRemoteControl() - end");
        }
        return rc;
    }

    public OsdReaderThread getOsdReader() {
        if (logger.isDebugEnabled()) {
            logger.debug("getOsdReader() - start");
        }
        if (osd == null) {
            osd = new OsdReaderThread(this);
            osd.start();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getOsdReader() - end");
        }
        return osd;
    }

    public ChannelManager getChannelManager() {
        if (logger.isDebugEnabled()) {
            logger.debug("getChannelManager() - start");
        }
        if (channelManager == null) {
            channelManager = new ChannelManager(this);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getChannelManager() - end");
        }
        return channelManager;
    }

    public DirectoryLogger getTimerDirLogger(UnaryFunction printer) {
        if (logger.isDebugEnabled()) {
            logger.debug("getTimerDirLogger(UnaryFunction) - start");
        }
        DirectoryLogger returnDirectoryLogger = new DirectoryLogger(this, printer);
        if (logger.isDebugEnabled()) {
            logger.debug("getTimerDirLogger(UnaryFunction) - end");
        }
        return returnDirectoryLogger;
    }

    public String toString() {
        if (logger.isDebugEnabled()) {
            logger.debug("toString() - start");
        }
        String returnString = getName();
        if (logger.isDebugEnabled()) {
            logger.debug("toString() - end");
        }
        return returnString;
    }

    private class PingThread extends Thread {

        /**
		 * Logger for this class
		 */
        private boolean active;

        public PingThread() {
            setName("Ping Box " + BaseBox.this.getName());
            active = true;
        }

        public void kill() {
            if (logger.isDebugEnabled()) {
                logger.debug("kill() - start");
            }
            active = false;
            if (logger.isDebugEnabled()) {
                logger.debug("kill() - end");
            }
        }

        public void run() {
            if (logger.isDebugEnabled()) {
                logger.debug("run() - start");
            }
            while (active) {
                try {
                    Socket so = new Socket();
                    so.connect(new InetSocketAddress(getHost(), 21), 1000);
                    InputStream st = so.getInputStream();
                    int ch = st.read();
                    setRunning(ch >= 0);
                    st.close();
                    so.close();
                } catch (IOException e) {
                    logger.error("run()", e);
                    setRunning(false);
                }
                try {
                    sleep(5000);
                } catch (InterruptedException e) {
                    logger.warn("run() - exception ignored", e);
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("run() - end");
            }
        }
    }

    public ITimerManager getTimerManager() {
        if (logger.isDebugEnabled()) {
            logger.debug("getTimerManager() - start");
        }
        if (timerManager == null) {
            UnaryFunction printer = new UnaryFunction() {

                public Object execute(Object arg) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("$UnaryFunction.execute(Object) - start");
                    }
                    System.out.println(arg);
                    if (logger.isDebugEnabled()) {
                        logger.debug("$UnaryFunction.execute(Object) - end");
                    }
                    return null;
                }
            };
            timerManager = new RcTimerManager(printer, this);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getTimerManager() - end");
        }
        return timerManager;
    }

    public ILocation getLocation(int which) {
        if (logger.isDebugEnabled()) {
            logger.debug("getLocation(int) - start");
        }
        if (which < IBox.USB_HDD_1 || which > IBox.USB_HDD_2) throw new IllegalArgumentException("Ungueltiger Freigabename der Box");
        ILocation returnILocation = locations[which];
        if (logger.isDebugEnabled()) {
            logger.debug("getLocation(int) - end");
        }
        return returnILocation;
    }

    public void setLocation(int which, ILocation location) {
        if (logger.isDebugEnabled()) {
            logger.debug("setLocation(int, ILocation) - start");
        }
        if (which < IBox.USB_HDD_1 || which > IBox.USB_HDD_2) throw new IllegalArgumentException("Ungueltiger Freigabename der Box");
        locations[which] = location;
        if (logger.isDebugEnabled()) {
            logger.debug("setLocation(int, ILocation) - end");
        }
    }

    protected void copyLocations(IBox result) {
        if (logger.isDebugEnabled()) {
            logger.debug("copyLocations(IBox) - start");
        }
        for (int i = USB_HDD_1; i <= USB_HDD_2; i++) result.setLocation(i, this.getLocation(i));
        if (logger.isDebugEnabled()) {
            logger.debug("copyLocations(IBox) - end");
        }
    }

    public boolean isNetworked() {
        if (logger.isDebugEnabled()) {
            logger.debug("isNetworked() - start");
        }
        boolean returnboolean = host != null && host.length() > 0;
        if (logger.isDebugEnabled()) {
            logger.debug("isNetworked() - end");
        }
        return returnboolean;
    }
}
