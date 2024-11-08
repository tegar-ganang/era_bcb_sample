package app;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.Properties;

public class AppConfig {

    private static AppConfig theInstance;

    private static boolean disposed = false;

    private boolean open = false;

    private Properties properties = null;

    private int instanceCount;

    private FileChannel fileChannel;

    private FileLock fileLock;

    public static AppConfig getInstance() {
        if (disposed) {
            throw new Error("Cannot access AppSumConfig once it's been disposed of");
        }
        if (theInstance == null) {
            theInstance = new AppConfig();
        }
        return theInstance;
    }

    /**
	 * Singleton class: private constructor
	 */
    private AppConfig() {
    }

    /**
	 * Open and lock the configuration file.
	 * 
	 * @throws IOException
	 */
    public void open() throws IOException {
        properties = new Properties();
        String basename = System.getProperty("user.home") + "/.eaton-product.";
        instanceCount = 0;
        while (fileLock == null && instanceCount < 20) {
            File file = new File(basename + instanceCount + ".properties");
            fileChannel = new RandomAccessFile(file, "rw").getChannel();
            try {
                fileLock = fileChannel.tryLock();
            } catch (OverlappingFileLockException e) {
            }
            if (fileLock == null) {
                fileChannel.close();
                instanceCount++;
            }
        }
        if (fileLock != null) {
            open = true;
            properties.load(Channels.newInputStream(fileChannel));
        }
    }

    public void close() {
        if (open) {
            saveToFile();
            finalize();
        }
    }

    protected void finalize() {
        try {
            disposed = true;
            if (fileLock != null) fileLock.release();
            if (fileChannel != null) fileChannel.close();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private void saveToFile() {
        try {
            fileChannel.truncate(0);
            OutputStream os = Channels.newOutputStream(fileChannel);
            String comments = "Eaton product name. (C) 2006 Eaton Corporation.";
            properties.store(os, comments);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    /*******
	 * Add required property setter/getters
	 * 
	 * Don't bother checking or asserting on this.open, as we want the app to
	 * operate ok if the config failed to open for some reason.
	 */
    public File getLastDir() {
        String lastDir = properties.getProperty("lastDirectory");
        if (lastDir != null) {
            return new File(lastDir);
        }
        return null;
    }

    public void setLastDir(File value) {
        properties.setProperty("lastDirectory", value.getAbsolutePath());
    }

    public File getLastFile() {
        String lastFile = properties.getProperty("lastFile");
        if (lastFile != null) {
            return new File(lastFile);
        }
        return null;
    }

    public void setLastFile(File value) {
        if (value == null) {
            properties.remove("lastFile");
        } else {
            properties.setProperty("lastFile", value.getPath());
        }
    }

    public Rectangle getMainFrameBounds() {
        Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle def = new Rectangle(10, 10, ss.width * 2 / 3, ss.height * 2 / 3);
        try {
            Rectangle res = new Rectangle(Integer.parseInt(properties.getProperty("mainFrame.Left", Integer.toString(def.x))), Integer.parseInt(properties.getProperty("mainFrame.Top", Integer.toString(def.y))), Integer.parseInt(properties.getProperty("mainFrame.Width", Integer.toString(def.width))), Integer.parseInt(properties.getProperty("mainFrame.Height", Integer.toString(def.height))));
            Rectangle vis = new Rectangle(new Point(0, 0), ss);
            vis.grow(-10, -10);
            if (res.intersects(vis)) return res;
        } catch (NumberFormatException e) {
            System.err.println("Handled: " + e);
        }
        return def;
    }

    public void setMainFrameBounds(Rectangle rec) {
        properties.setProperty("mainFrame.Left", Integer.toString(rec.x));
        properties.setProperty("mainFrame.Top", Integer.toString(rec.y));
        properties.setProperty("mainFrame.Width", Integer.toString(rec.width));
        properties.setProperty("mainFrame.Height", Integer.toString(rec.height));
    }

    public void setSplitterPos(int splitterPos) {
        properties.setProperty("splitterPos", "" + splitterPos);
    }

    public int getSplitterPos() {
        try {
            return Integer.parseInt(properties.getProperty("splitterPos", "-1"));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public String getLogFileDir() {
        String logDir = properties.getProperty("logFileDirectory");
        if (logDir == null) {
            logDir = System.getProperty("user.dir");
        }
        return logDir;
    }

    public void setLogFileDir(String value) {
        properties.setProperty("logFileDirectory", value);
    }
}
