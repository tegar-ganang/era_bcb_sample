package org.gtdfree;

import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.ImageProducer;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.swing.ImageIcon;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import org.apache.commons.lang.CharUtils;
import org.apache.log4j.Logger;

/**
 * @author ikesan
 *
 */
public final class ApplicationHelper {

    public static final String EMPTY_STRING = "";

    private static File dataFile;

    private static File dataFolder;

    private static FileLock exclusiveLock;

    public static String DEFAULT_DATE_FORMAT_STRING = "EE dd/MMM yy";

    public static SimpleDateFormat defaultDateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT_STRING);

    private static SimpleDateFormat isoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private static SimpleDateFormat readableISODateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static Map<String, ImageIcon> iconCache = new HashMap<String, ImageIcon>(50);

    public static final String DATA_PROPERTY = "gtd-free.data";

    public static final String TITLE_PROPERTY = "gtd-free.title";

    public static final String LOCK_FILE_NAME = "gtd-free.lock";

    public static final String DEFAULT_DATA_FILE_NAME = "gtd-free-data.xml";

    public static final String SHUTDOWN_BACKUP_XML_DATA_FILE_NAME = "gtd-free-data.shutdown_backup.xml";

    public static final String BACKUP_DATA_FILE_NAME_PART = "gtd-free-data.backup";

    public static final String DEFAULT_DATA_FOLDER_NAME = ".gtd-free";

    public static final String CONFIGURATION_FILE_NAME = "gtd-free-config.properties";

    public static final String OPTIONS_FILE_NAME = "gtd-free-options.properties";

    public static String icon_name_large_add = "icons/gnome/16x16/actions/list-add.png";

    public static String icon_name_large_about = "icons/gnome/16x16/actions/help-about.png";

    public static String icon_name_large_browser = "icons/gnome/16x16/apps/web-browser.png";

    public static String icon_name_large_clear = "icons/gnome/16x16/actions/edit-clear.png";

    public static String icon_name_large_clone = "icons/gnome/16x16/actions/edit-copy.png";

    public static String icon_name_large_collecting = "icons/gnome/32x32/stock/generic/stock_notes.png";

    public static String icon_name_large_delete = "icons/gnome/16x16/actions/edit-delete.png";

    public static String icon_name_large_exit = "icons/gnome/16x16/actions/application-exit.png";

    public static String icon_name_large_export = "icons/Humanity/16x16/actions/document-export.png";

    public static String icon_name_large_import = "icons/Humanity/16x16/actions/document-import.png";

    public static String icon_name_large_journaling = "icons/gnome/16x16/stock/form/stock_form-time-field.png";

    public static String icon_name_large_logo = "splash48.png";

    public static String icon_name_large_move = "icons/gnome/16x16/actions/go-next.png";

    public static String icon_name_large_new = "icons/gnome/16x16/actions/document-new.png";

    public static String icon_name_large_print = "icons/gnome/16x16/actions/document-print.png";

    public static String icon_name_large_processing = "icons/gnome/32x32/actions/system-run.png";

    public static String icon_name_large_queue_execute = "icons/gnome/32x32/emblems/emblem-important.png";

    public static String icon_name_large_queue_off = "icons/gnome/16x16/emblems/emblem-important-gray.png";

    public static String icon_name_large_queue_on = "icons/gnome/16x16/emblems/emblem-important.png";

    public static String icon_name_large_rename = "icons/gnome/16x16/actions/gtk-edit.png";

    public static String icon_name_large_resolve = "icons/Human/16x16/actions/dialog-apply.png";

    public static String icon_name_large_review = "icons/gnome/32x32/apps/gnome-searchtool-animation-rest.png";

    public static String icon_name_large_save = "icons/gnome/16x16/actions/document-save.png";

    public static String icon_name_large_search = "icons/gnome/16x16/actions/edit-find.png";

    public static String icon_name_large_splash = "splash24.png";

    public static String icon_name_large_undelete = "icons/gnome/16x16/stock/generic/stock_undelete.png";

    public static String icon_name_large_tray_splash = "splash24.png";

    public static String icon_name_large_update = "icons/Tango/16x16/apps/system-software-update.png";

    public static String icon_name_small_add = "icons/gnome/12x12/actions/list-add.png";

    public static String icon_name_small_clear = "icons/gnome/12x12/actions/edit-clear.png";

    public static String icon_name_small_collecting = "icons/gnome/16x16/stock/generic/stock_notes.png";

    public static String icon_name_small_copy = "icons/gnome/12x12/actions/edit-copy.png";

    public static String icon_name_small_cut = "icons/gnome/12x12/actions/edit-cut.png";

    public static String icon_name_small_delete = "icons/gnome/12x12/actions/edit-delete.png";

    public static String icon_name_small_down = "icons/gnome/12x12/actions/go-down.png";

    public static String icon_name_small_folded = "icons/gnome/12x12/stock/data/stock_data-next.png";

    public static String icon_name_small_queue_execute = "icons/gnome/16x16/emblems/emblem-important.png";

    public static String icon_name_small_queue_off = "icons/gnome/12x12/emblems/emblem-important-gray.png";

    public static String icon_name_small_queue_on = "icons/gnome/12x12/emblems/emblem-important.png";

    public static String icon_name_small_next = "icons/gnome/12x12/actions/go-next.png";

    public static String icon_name_small_paste = "icons/gnome/12x12/actions/edit-paste.png";

    public static String icon_name_small_previous = "icons/gnome/12x12/actions/go-previous.png";

    public static String icon_name_small_processing = "icons/gnome/16x16/actions/system-run.png";

    public static String icon_name_small_overview = "splash16.png";

    public static String icon_name_small_redo = "icons/gnome/12x12/actions/edit-redo.png";

    public static String icon_name_small_remove = "icons/gnome/12x12/actions/list-remove.png";

    public static String icon_name_small_rename = "icons/gnome/12x12/actions/gtk-edit.png";

    public static String icon_name_small_resolve = "icons/Human/12x12/actions/dialog-apply.png";

    public static String icon_name_small_review = "icons/gnome/16x16/stock/navigation/stock_zoom-page.png";

    public static String icon_name_small_search = "icons/gnome/12x12/actions/edit-find.png";

    public static String icon_name_small_select_all = "icons/gnome/12x12/actions/edit-select-all.png";

    public static String icon_name_small_splash = "splash16.png";

    public static String icon_name_small_start = "icons/gnome/12x12/actions/media-playback-start.png";

    public static String icon_name_small_tray_splash = "splash16.png";

    public static String icon_name_small_undelete = "icons/gnome/12x12/stock/generic/stock_undelete.png";

    public static String icon_name_small_undo = "icons/gnome/12x12/actions/edit-undo.png";

    public static String icon_name_small_unfolded = "icons/gnome/12x12/stock/data/stock_data-down.png";

    public static String icon_name_small_up = "icons/gnome/12x12/actions/go-up.png";

    private static ThreadPoolExecutor backgroundExecutor;

    private static Boolean gtklaf;

    private static int defaultFieldHeigth;

    public static final String icon_name_tiny_options = "icons/gnome/9x9/stock/data/stock_data-down.png";

    public static final String icon_name_small_star_yellow = "icons/Neu/12x12/actions/help-about.png";

    public static final String icon_name_small_star_orange = "icons/Neu/12x12/actions/help-about-orange.png";

    public static final String icon_name_small_star_red = "icons/Neu/12x12/actions/help-about-red.png";

    public static final String icon_name_small_star_grey = "icons/Neu/12x12/actions/help-about-grey.png";

    public static final String icon_name_small_star_blue = "icons/Neu/12x12/actions/help-about-blue.png";

    public static ImageIcon getIcon(String name) {
        ImageIcon i = iconCache.get(name);
        if (i != null) {
            return i;
        }
        i = loadIcon(name);
        iconCache.put(name, i);
        return i;
    }

    public static final ImageIcon loadIcon(String resource) {
        try {
            URL url = ApplicationHelper.class.getClassLoader().getResource(resource);
            if (!(url.getContent() instanceof ImageProducer)) return null;
            return new ImageIcon(url);
        } catch (Exception e) {
            return null;
        }
    }

    public static final Image loadImage(String resource) {
        try {
            URL url = ApplicationHelper.class.getClassLoader().getResource(resource);
            if (!(url.getContent() instanceof ImageProducer)) return null;
            return new ImageIcon(url).getImage();
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] loadResource(String name) {
        InputStream is = null;
        try {
            is = ApplicationHelper.class.getResourceAsStream(name);
            if (is == null) {
                is = ClassLoader.getSystemClassLoader().getResourceAsStream(name);
                if (is == null) {
                    File f = new File(name);
                    if (f.exists()) {
                        is = new BufferedInputStream(new FileInputStream(f));
                    }
                } else {
                    Logger.getLogger(ApplicationHelper.class).info("Loaded '" + name + "' with SystemClassLoader");
                }
            } else {
                Logger.getLogger(ApplicationHelper.class).info("Loaded '" + name + "' with ApplicationHelper");
            }
            if (is == null) {
                Logger.getLogger(ApplicationHelper.class).error("Failed to load '" + name + "'.");
                return null;
            }
            ByteArrayOutputStream os = new ByteArrayOutputStream(is.available());
            while (is.available() > 0) {
                os.write(is.read());
            }
            return os.toByteArray();
        } catch (Exception e) {
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static final String formatLongISO(Date d) {
        return isoDateTimeFormat.format(d);
    }

    public static final Date parseLongISO(String d) throws ParseException {
        return isoDateTimeFormat.parse(d);
    }

    public static final File getDataFolder() {
        if (dataFolder == null) {
            String s = System.getProperty(DATA_PROPERTY);
            if (s != null) {
                File f = new File(s);
                if (!f.exists()) {
                    f.mkdirs();
                }
                if (f.isDirectory()) {
                    dataFolder = f;
                } else {
                    dataFolder = f.getParentFile();
                    dataFile = f;
                }
            }
            if (dataFolder == null) {
                dataFolder = new File(System.getProperty("user.home"));
                dataFolder = new File(dataFolder, DEFAULT_DATA_FOLDER_NAME);
                System.getProperties().setProperty(DATA_PROPERTY, dataFolder.toString());
            }
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
        }
        return dataFolder;
    }

    public static File getDataFile() {
        if (dataFile == null) {
            dataFile = new File(getDataFolder(), DEFAULT_DATA_FILE_NAME);
        }
        return dataFile;
    }

    public static File createBackupDataFile(File f, int i) {
        if (f.getName().toLowerCase().endsWith(".xml")) {
            String s = f.getAbsolutePath();
            return new File(s.substring(0, s.length() - 4) + "backup" + i + ".xml");
        }
        return new File(f.getParentFile(), BACKUP_DATA_FILE_NAME_PART + i + ".xml");
    }

    public static final synchronized boolean tryLock(File location) {
        if (location == null) {
            location = getDataFolder();
        }
        if (exclusiveLock != null) {
            return false;
        }
        try {
            FileChannel lock = new RandomAccessFile(new File(location, LOCK_FILE_NAME), "rw").getChannel();
            exclusiveLock = lock.tryLock();
        } catch (Exception e) {
            return false;
        }
        return exclusiveLock != null;
    }

    public static final synchronized void releaseLock() {
        if (exclusiveLock != null) {
            try {
                exclusiveLock.release();
            } catch (IOException e) {
                Logger.getLogger(ApplicationHelper.class).warn("Internal error.", e);
            }
            exclusiveLock = null;
        }
    }

    public static Properties loadConfiguration() {
        Properties p = new Properties();
        InputStream in = ApplicationHelper.class.getClassLoader().getResourceAsStream(CONFIGURATION_FILE_NAME);
        if (in != null) {
            try {
                p.load(in);
            } catch (Exception e) {
                Logger.getLogger(ApplicationHelper.class).error("Initialization error.", e);
            }
        }
        p.putAll(System.getProperties());
        return p;
    }

    public static String loadLicense() {
        return "This program is free software: you can redistribute it and/or modify\nit under the terms of the GNU General Public License as published by\nthe Free Software Foundation, either version 3 of the License, or\n(at your option) any later version.\n\nThis program is distributed in the hope that it will be useful,\nbut WITHOUT ANY WARRANTY; without even the implied warranty of\nMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\nGNU General Public License for more details.\n\nYou should have received a copy of the GNU General Public License\nalong with this program.  If not, see <http://www.gnu.org/licenses/>.";
    }

    public static String toDateString(Date date) {
        if (date == null) {
            return ApplicationHelper.EMPTY_STRING;
        }
        return defaultDateFormat.format(date);
    }

    public static String toISODateString(Date date) {
        if (date == null) {
            return ApplicationHelper.EMPTY_STRING;
        }
        return isoDateFormat.format(date);
    }

    /**
	 * Prints something like: '2009-02-24 14:35:06'
	 * @param date date, if null empty string is returned 
	 * @return something like: '2009-02-24 14:35:06'
	 */
    public static String toISODateTimeString(Date date) {
        if (date == null) {
            return ApplicationHelper.EMPTY_STRING;
        }
        return readableISODateTimeFormat.format(date);
    }

    public static void changeDefaultFontSize(float size, String key) {
        Font f = UIManager.getDefaults().getFont(key + ".font");
        if (f != null) {
            UIManager.getDefaults().put(key + ".font", new FontUIResource(f.deriveFont(f.getSize() + size)));
        }
    }

    public static void changeDefaultFontStyle(int style, String key) {
        Font f = UIManager.getDefaults().getFont(key + ".font");
        if (f != null) {
            UIManager.getDefaults().put(key + ".font", new FontUIResource(f.deriveFont(style)));
        }
    }

    /**
	 * Escape in Java style in readable friendly way common control characters and other control characters.
	 * @param in input string
	 * @return escaped string in Java style
	 */
    public static String escapeControls(String in) {
        if (in == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(in.length() + 10);
        for (int i = 0; i < in.length(); i++) {
            char ch = in.charAt(i);
            switch(ch) {
                case '\b':
                    sb.append('\\');
                    sb.append('b');
                    break;
                case '\n':
                    sb.append('\\');
                    sb.append('n');
                    break;
                case '\t':
                    sb.append('\\');
                    sb.append('t');
                    break;
                case '\f':
                    sb.append('\\');
                    sb.append('f');
                    break;
                case '\r':
                    sb.append('\\');
                    sb.append('r');
                    break;
                case '\\':
                    sb.append('\\');
                    sb.append('\\');
                    break;
                default:
                    if (Character.isISOControl(ch)) {
                        sb.append(CharUtils.unicodeEscaped(ch));
                    } else {
                        sb.append(ch);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    public static final String getVersion() {
        Properties p = loadConfiguration();
        String v = p.getProperty("build.version");
        String t = p.getProperty("build.type");
        if (t != null && t.trim().length() > 0) {
            return v.trim() + "-" + t.trim();
        }
        return v.trim();
    }

    public static synchronized void executeInBackground(Runnable r) {
        if (backgroundExecutor == null) {
            backgroundExecutor = new ThreadPoolExecutor(0, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("BackgroundExecutor");
                    t.setPriority(Thread.MIN_PRIORITY);
                    t.setDaemon(false);
                    return t;
                }
            });
        }
        backgroundExecutor.execute(r);
    }

    public static final synchronized void stopBackgroundExecutor() {
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdownNow();
            backgroundExecutor = null;
        }
    }

    public static final Insets getDefaultFatButtonMargin() {
        if (isGTKLaF()) {
            return new Insets(0, 2, 0, 2);
        }
        return new Insets(2, 4, 2, 4);
    }

    public static final Insets getDefaultSlimButtonMargin() {
        if (isGTKLaF()) {
            return new Insets(0, 0, 0, 0);
        }
        return new Insets(2, 2, 2, 2);
    }

    public static final boolean isGTKLaF() {
        if (gtklaf == null) {
            try {
                gtklaf = Class.forName("com.sun.java.swing.plaf.gtk.GTKLookAndFeel").isAssignableFrom(UIManager.getLookAndFeel().getClass());
            } catch (Exception e) {
                gtklaf = Boolean.FALSE;
            }
        }
        return gtklaf;
    }

    public static File getShutdownBackupXMLFile() {
        return new File(ApplicationHelper.getDataFolder(), ApplicationHelper.SHUTDOWN_BACKUP_XML_DATA_FILE_NAME);
    }

    public static String getLogFileName() {
        File log = new File(getDataFolder(), "log");
        if (!log.exists()) {
            log.mkdirs();
        }
        return new File(log, "log.txt").toString();
    }

    public static int getDefaultFieldHeigth() {
        if (defaultFieldHeigth <= 0) {
            JTextField jtf = new JTextField();
            jtf.setText("1234567890");
            defaultFieldHeigth = jtf.getPreferredSize().height;
            if (UIManager.getLookAndFeel() != null && UIManager.getLookAndFeel().getClass().getName().equals("com.sun.java.swing.plaf.gtk.GTKLookAndFeel")) {
                defaultFieldHeigth -= 4;
            }
            if (defaultFieldHeigth < 21) {
                defaultFieldHeigth = 21;
            }
        }
        return defaultFieldHeigth;
    }
}
