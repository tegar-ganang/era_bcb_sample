package javax.microedition.midlet;

import java.applet.Applet;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.media.Player;
import org.me4se.Initializer;
import org.me4se.JadFile;
import org.me4se.System;
import org.me4se.impl.Log;
import org.me4se.impl.MIDletChooser;
import org.me4se.scm.ScmContainer;
import org.me4se.scm.ScmWrapper;

/**
 * This class is needed *here* in order to be able to call the protected MIDlet
 * startApp() method (etc.). It should perhaps be hidden from the documentation.
 * 
 * @ME4SE INTERNAL
 */
public class ApplicationManager {

    public static final String ME4SE_VERSION_NUMBER = "3.1.9";

    static final int[] PNG_SIGNATURE = { 137, 80, 78, 71, 13, 10, 26, 10 };

    static final int DEFAULT_KEYCODE_UP = -1;

    static final int DEFAULT_KEYCODE_DOWN = -2;

    static final int DEFAULT_KEYCODE_LEFT = -3;

    static final int DEFAULT_KEYCODE_RIGHT = -4;

    static final int DEFAULT_KEYCODE_SELECT = -5;

    static final int DEFAULT_KEYCODE_LSK = -6;

    static final int DEFAULT_KEYCODE_RSK = -7;

    static final int DEFAULT_KEYCODE_CLEAR = -8;

    public static final int DEFAULT_KEYCODE_MENU = -12;

    static final int DEFAULT_KEYCODE_MODE = -101;

    static final String[] INITIALIZERS = { "media.MediaInitializer", "rms.RecordStoreInitializer" };

    static final String DEFAULT_PLATFORM = "ME4SE";

    static final String DEFAULT_CONFIGURATION = "CLDC-1.1";

    static final String DEFAULT_PROFILES = "MIDP-2.0";

    static final String DEFAULT_ENCODING = "ISO8859_1";

    private static ApplicationManager manager;

    public MIDlet active;

    public Applet applet;

    public int colorCount = 256 * 256 * 256;

    public boolean isColor = true;

    /**
   * Please do not access directly, use getProperty instead! getProperty takes
   * other property sources (applet, system into account
   */
    public Properties properties;

    public java.awt.Color bgColor = java.awt.Color.white;

    public ScmContainer skin;

    public JadFile jadFile = new JadFile();

    public ScmContainer displayContainer;

    public ScmWrapper wrapper;

    public java.awt.Frame frame;

    public java.awt.Container awtContainer;

    public int screenWidth;

    public int screenHeight;

    /** Actually shown displayable, regardless of the active MIDlet */
    public Displayable currentlyShown;

    public String documentBase;

    public ClassLoader classLoader;

    /** Maps VK_ codes to button names. Filled in class Skin */
    public Hashtable virtualKeyMap = new Hashtable();

    /** Maps device key codes to game action codes. Filled in class Skin */
    public Hashtable gameActions = new Hashtable();

    /** Maps device key codes back to button names */
    public Hashtable keyCodeToButtonName = new Hashtable();

    private Hashtable imageCache = new Hashtable();

    Class activeClass;

    public int keyStates;

    /**
   * List of active media players -- need to be stopped when an applet is
   * terminated
   */
    public Vector activePlayers = new Vector();

    public long firstKeyPress;

    public Image timeoutImage;

    public static boolean isInitialized() {
        return manager != null;
    }

    public static ApplicationManager getInstance() {
        return manager != null ? manager : createInstance(null, null);
    }

    public BufferedImage createImage(byte[] data, int start, int len) {
        try {
            return createImage(new ByteArrayInputStream(data, start, len));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public BufferedImage createImage(InputStream is) throws IOException {
        String version = System.getProperty("java.version");
        if (!version.startsWith("1.4")) {
            ImageIO.setUseCache(false);
        }
        BufferedInputStream bis = new BufferedInputStream(is);
        bis.mark(PNG_SIGNATURE.length);
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (bis.read() != PNG_SIGNATURE[i]) {
                bis.reset();
                BufferedImage img = ImageIO.read(bis);
                if (img == null) {
                    throw new IOException("Image broken!");
                }
                return img;
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            baos.write(PNG_SIGNATURE[i]);
        }
        DataOutputStream dos = new DataOutputStream(baos);
        DataInputStream dis = new DataInputStream(bis);
        int bitDepth = -1;
        StringBuffer sb = new StringBuffer();
        while (true) {
            int len = dis.readInt();
            sb.setLength(4);
            for (int i = 0; i < 4; i++) {
                char c = (char) dis.read();
                sb.setCharAt(i, c);
            }
            String type = sb.toString();
            if (sb.toString().equals("IHDR")) {
                dos.writeInt(len);
                dos.write(type.getBytes());
                int w = dis.readInt();
                int h = dis.readInt();
                bitDepth = dis.read();
                int misc = dis.readInt();
                int crc = dis.readInt();
                dos.writeInt(w);
                dos.writeInt(h);
                dos.write(bitDepth);
                dos.writeInt(misc);
                dos.writeInt(crc);
            } else if (sb.toString().equals("PLTE")) {
                int padding = (2 << (bitDepth - 1)) * 3 - len;
                dos.writeInt(len + padding);
                dos.write(type.getBytes());
                for (int i = 0; i < len; i++) {
                    dos.write(dis.read());
                }
                for (int i = 0; i < padding; i++) {
                    dos.write(0);
                }
                dos.writeInt(dis.readInt());
            } else {
                dos.writeInt(len);
                dos.write(type.getBytes());
                for (int i = 0; i < len; i++) {
                    dos.write(dis.read());
                }
                dos.writeInt(dis.readInt());
                if (sb.toString().equals("IEND")) {
                    break;
                }
            }
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        return ImageIO.read(bais);
    }

    /**
   * loads an image from the given file name, using the openInputStream()
   * method.
   */
    public BufferedImage getImage(String fileName) throws IOException {
        BufferedImage img = (BufferedImage) imageCache.get(fileName);
        if (img != null) return img;
        InputStream is = openInputStream(fileName);
        if (is == null) throw new IOException("null stream opening: " + fileName);
        img = createImage(is);
        imageCache.put(fileName, img);
        return img;
    }

    /**
   * Tries to instantiate the class with the name org.me4se.psi.java1.%name%
   * 
   * @param prefix
   * @param name
   * @return
   */
    public Object instantiate(String name) throws ClassNotFoundException {
        String implementation = getProperty("me4se.implementation");
        if (implementation != null) {
            implementation += ";";
        } else {
            implementation = "";
        }
        implementation += "org.me4se.psi.java1;org.me4se.psi.j2se";
        int pos = 0;
        do {
            int cut = implementation.indexOf(';', pos);
            if (cut == -1) cut = implementation.length();
            String qName = implementation.substring(pos, cut) + "." + name;
            try {
                Class clazz = Class.forName(qName);
                return clazz.newInstance();
            } catch (Exception e) {
            }
            pos = cut + 1;
        } while (pos < implementation.length());
        throw new ClassNotFoundException("No implementation class found for suffix " + name);
    }

    public static InputStream openInputStream(Class clazz, String name) {
        if (name.startsWith("/")) {
            try {
                return ApplicationManager.manager.openInputStream(name);
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
        } else {
            try {
                Package p = clazz.getPackage();
                String s = "";
                if (p != null) {
                    s = p.getName().replace('.', '/');
                }
                return ApplicationManager.manager.openInputStream(s + "/" + name);
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    /** Read the whole stream and return a byteArrayInputStream */
    public InputStream openInputStream(String fileName) throws IOException {
        InputStream is = _openInputStream(fileName);
        if (is == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        while (true) {
            int count = is.read(buf);
            if (count <= 0) break;
            baos.write(buf, 0, count);
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
   * opens an input stream on the given resource; if the path is not absolute
   * and not an URL, the documentBase is taken into account.
   */
    public InputStream _openInputStream(String fileName) throws IOException {
        Log.log(Log.IO, "open stream: " + fileName);
        fileName = concatPath(documentBase, fileName);
        if (isUrl(fileName)) {
            return new URL(fileName).openStream();
        }
        try {
            InputStream result = null;
            if (activeClass != null) {
                result = activeClass.getResourceAsStream(fileName);
                if (result != null) return result;
            }
            if (classLoader != null) {
                result = classLoader.getResourceAsStream(fileName);
                if (result != null) return result;
            }
            result = ApplicationManager.class.getResourceAsStream(fileName);
            if (result != null) return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (applet == null && new File(fileName).exists()) {
            return new FileInputStream(fileName);
        } else {
            System.out.println("WARNING: Resource not found: " + fileName);
            return null;
        }
    }

    public String getProperty(String name) {
        String result = properties.getProperty(name.toLowerCase());
        if (result == null) {
            if (applet != null) {
                result = applet.getParameter(name);
            } else {
                try {
                    result = System.getProperty(name);
                } catch (Throwable th) {
                }
            }
        }
        return result;
    }

    public String getProperty(String name, String dflt) {
        String result = getProperty(name);
        return result != null ? result : dflt;
    }

    public static String[] split(String s) {
        if (s == null) return new String[0];
        s = s.trim();
        if (s.length() == 0) return new String[0];
        Vector v = new Vector();
        while (true) {
            int cut = s.indexOf(' ');
            if (cut == -1) break;
            v.addElement(s.substring(0, cut));
            s = s.substring(cut + 1).trim();
        }
        String[] res = new String[v.size() + 1];
        for (int i = 0; i < res.length - 1; i++) {
            res[i] = (String) v.elementAt(i);
        }
        res[res.length - 1] = s;
        return res;
    }

    /** determines whether path starts with http:// or file:// */
    public static boolean isUrl(String path) {
        String test = path.toLowerCase();
        return test.startsWith("http://") || test.startsWith("file:");
    }

    /**
   * if file is absolute (starts with /, http:// or file://, or the base path is
   * null, file is returned; otherwise the last index of / or \ is searched in
   * base and the strings are concatenated. Please note that this is not
   * equivalent to kobjects.buildUrl; buildUrl always adds "file://" for urls
   * with no location type given.
   * 
   * @IMPROVE: It may make sense to use Util.buildUrl here and map file:///
   *           secretly to "resource:" in openStream??
   */
    public static String concatPath(String base, String file) {
        if (base == null || file.startsWith("/") || file.startsWith("\\") || (file.length() >= 2 && file.charAt(1) == ':') || isUrl(file)) return file;
        int cut = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        return base.substring(0, cut + 1) + file;
    }

    public static ApplicationManager createInstance(java.awt.Container container, Properties properties) {
        return new ApplicationManager(container, properties);
    }

    private ApplicationManager(java.awt.Container container, Properties properties) {
        try {
            java.lang.System.setProperty("sun.awt.noerasebackground", "true");
        } catch (Throwable th) {
        }
        this.properties = properties == null ? new Properties() : properties;
        System.out.println("---=== ME4SE Version " + ME4SE_VERSION_NUMBER + " ===---");
        manager = this;
        if (container instanceof Applet) {
            applet = (Applet) container;
            documentBase = applet.getDocumentBase().toString();
        }
        String locale = getProperty("microedition.locale", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());
        setSystemProperty("microedition.platform", DEFAULT_PLATFORM);
        setSystemProperty("microedition.encoding", DEFAULT_ENCODING);
        setSystemProperty("microedition.locale", locale);
        setSystemProperty("microedition.configuration", DEFAULT_CONFIGURATION);
        if (!"AVETANA-BT".equals(getSystemProperty("microedition.profiles"))) {
            setSystemProperty("microedition.profiles", DEFAULT_PROFILES);
        }
        if (locale != null) {
            InputStream is = null;
            try {
                is = openInputStream("/locale-" + locale + ".properties");
            } catch (Exception e) {
            }
            try {
                if (is == null) {
                    is = openInputStream("/locale-" + locale.substring(0, 2) + ".properties");
                }
                if (is != null) {
                    properties.load(is);
                }
            } catch (Exception e) {
            }
        }
        wrapper = new ScmWrapper(this, new Float(getProperty("scale", "1")).floatValue());
        if (container != null) {
            awtContainer = container;
            container.setLayout(new java.awt.BorderLayout());
            screenWidth = container.getSize().width;
            screenHeight = container.getSize().height;
        } else {
            frame = new java.awt.Frame(getProperty("frame.title", "ME4SE MIDlet"));
            if (getProperty("frame.menu", null) != null) {
                frame.setMenuBar(createMenuBar());
            }
            screenWidth = getIntProperty("width", getIntProperty("screen.width", 150));
            screenHeight = getIntProperty("height", getIntProperty("screen.height", 200));
            frame.addWindowListener(new java.awt.event.WindowAdapter() {

                public void windowClosing(java.awt.event.WindowEvent ev) {
                    destroy(true, true);
                }
            });
            awtContainer = frame;
        }
        if (getProperty("skin") != null) {
            try {
                skin = (ScmContainer) Class.forName("org.me4se.impl.skins.Skin").newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            displayContainer = skin;
            screenWidth = getIntProperty("screen.width", screenWidth);
            screenHeight = getIntProperty("screen.height", screenHeight);
        } else {
            displayContainer = new ScmContainer() {

                public Dimension getPreferredSize() {
                    return getMinimumSize();
                }

                public Dimension getMinimumSize() {
                    return new Dimension(screenWidth, screenHeight);
                }

                public void paint(java.awt.Graphics g) {
                    if (getComponentCount() == 0) {
                        g.drawString("Loading; please wait...", 20, 20);
                    } else {
                        super.paint(g);
                    }
                }
            };
        }
        wrapper.setComponent(displayContainer);
        awtContainer.add(wrapper, java.awt.BorderLayout.CENTER);
        if (skin != null && frame != null) {
            frame.pack();
            frame.show();
        } else if (applet != null) {
            wrapper.invalidate();
            awtContainer.validate();
        }
        String initializers = getProperty("initializers");
        if (initializers != null) {
            StringTokenizer tokens = new StringTokenizer(initializers, ",");
            while (tokens.hasMoreTokens()) {
                String name = tokens.nextToken();
                try {
                    Initializer init = (Initializer) instantiate(name);
                    System.out.println("initializer parameter found for " + name + ": " + init.getClass());
                    init.initialize(this);
                } catch (ClassNotFoundException e) {
                    System.out.println("cannot initialize module: " + name);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        for (int i = 0; i < INITIALIZERS.length; i++) {
            String name = INITIALIZERS[i];
            try {
                Initializer init = (Initializer) instantiate(name);
                System.out.println("initializer found for " + name + ": " + init.getClass());
                init.initialize(this);
            } catch (ClassNotFoundException e) {
                System.out.println("cannot initialize module: " + name);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.mask = getIntProperty("me4se.eventlog", 0);
    }

    /** not called if MIDlet provides a main method */
    public void launch(JadFile jadFileParam, int midletNr) {
        if (jadFileParam != null) {
            jadFile = jadFileParam;
        } else {
            jadFile = new JadFile();
            String jadUrl = getProperty("jad");
            if (jadUrl != null) {
                try {
                    jadFile.load(jadUrl);
                } catch (Exception e) {
                    System.err.println("JAD access error: " + e + "; trying midlet/jam property");
                }
            }
        }
        JadFile manifest = new JadFile();
        try {
            InputStream is = this.getClass().getResourceAsStream("/me4se.properties");
            if (is != null) {
                manifest.load(is);
            } else {
                Enumeration e = this.getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
                while (e.hasMoreElements()) {
                    URL url = (URL) e.nextElement();
                    manifest.load(url.openStream());
                    if (manifest.getMIDletCount() != 0) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error while reading/searching MANIFEST.MF");
            e.printStackTrace();
        }
        if (manifest.getMIDletCount() > 0) {
            for (Enumeration e = manifest.keys(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                if (key != null) {
                    String value = manifest.getValue(key);
                    if (jadFile.getValue(key) == null) {
                        jadFile.setValue(key, value);
                    }
                    properties.setProperty(key, value);
                    if (key.startsWith("microedition.")) {
                        setSystemProperty(key, value);
                    }
                }
            }
        }
        if (jadFile.getValue("MIDlet-1") == null) {
            String midlet = getProperty("MIDlet");
            if (midlet != null) {
                jadFile.setValue("MIDlet-1", midlet + ",," + midlet);
            } else {
                throw new RuntimeException("JAD File does not contain 'MIDLet' or 'MIDLet-1' property");
            }
        }
        try {
            if (jadFile.getMIDletCount() == 1 || midletNr > 0) {
                activeClass = Class.forName(jadFile.getMIDlet(Math.max(midletNr, 1)).getClassName());
                active = ((MIDlet) activeClass.newInstance());
            } else active = new MIDletChooser();
            Displayable d = Display.getDisplay(active).getCurrent();
            if (d != null) {
                Display.getDisplay(active).setCurrent(d);
            }
            active.startApp();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public int getIntProperty(String name, int dflt) {
        try {
            return Integer.decode((String) ApplicationManager.manager.getProperty(name)).intValue();
        } catch (Exception e) {
            return dflt;
        }
    }

    int discretize(int value, int steps) {
        double v = value / 255.0;
        v = (int) ((steps - 1) * v + 0.5);
        v *= 255.0 / (steps - 1);
        return (int) v;
    }

    public int getDeviceColor(int color) {
        if (isColor && colorCount > 65536 && java.awt.Color.white.equals(ApplicationManager.manager.bgColor)) return color;
        int r = (color >> 16) & 255;
        int g = (color >> 8) & 255;
        int b = (color & 255);
        if (isColor) {
            int scnt = 2;
            int cnt = 8;
            while (cnt < colorCount) {
                scnt *= 2;
                cnt *= 8;
            }
            r = discretize(r, scnt);
            g = discretize(g, scnt);
            b = discretize(b, scnt);
        } else {
            double grayscale = (r + g + b) / (3.0 * 255.0);
            grayscale = (int) ((colorCount - 1) * grayscale + 0.5);
            grayscale /= (colorCount - 1);
            java.awt.Color bg = ApplicationManager.manager.bgColor;
            r = (int) grayscale * bg.getRed();
            g = (int) grayscale * bg.getGreen();
            b = (int) grayscale * bg.getBlue();
        }
        return (color & 0x0ff000000) | (r << 16) | (g << 8) | b;
    }

    public void start() {
        if (active != null) {
        }
    }

    public void pause() {
        if (active != null) {
            try {
                active.pauseApp();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
 * Destruction requested "externally" or by midlet
 * @param notifyMIDlet - should the midlet be told
 * @param killAll - true if the event comes from the [x] in the window, and signals Everything must be closed
 */
    public void destroy(boolean notifyMIDlet, boolean killAll) {
        String activeName = active == null ? "" : active.getClass().getName();
        if (notifyMIDlet && active != null) {
            try {
                active.inDestruction = true;
                active.destroyApp(true);
                active = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (int i = activePlayers.size() - 1; i >= 0; i--) {
            ((Player) activePlayers.elementAt(i)).close();
        }
        int midletCount = jadFile.getMIDletCount();
        try {
            if ((midletCount > 1 || applet != null) && !killAll && !activeName.equals("org.me4se.impl.MIDletChooser")) {
                launch(jadFile, 0);
            } else {
                active = null;
                org.me4se.System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            active = null;
            org.me4se.System.exit(0);
        }
    }

    /**
   * Instantiates and launches the given MIDlet. DestroyApp is called for any
   * running MIDlet.
   * 
   * public void startMIDlet(String name) throws InstantiationException,
   * ClassNotFoundException, IllegalAccessException {
   * 
   * if(active != null && !active.inDestruction){ try{ active.inDestruction =
   * true; active.destroyApp(true); } catch(Exception e){ e.printStackTrace(); } }
   * 
   * MIDlet midlet = null;
   * 
   * if (classLoader != null) { midlet = ((MIDlet)
   * classLoader.loadClass(name).newInstance()); } else { midlet = ((MIDlet)
   * Class.forName(name).newInstance()); }
   *  // Throw away everything in the current DisplayContainer. Tell // the
   * ApplicationManager that the newly created MIDlet is the // active one. Then
   * run it. // displayContainer.removeAll(); //displayContainer.setLayout(new
   * java.awt.BorderLayout()); //displayContainer.setBackground(bgColor);
   * 
   * active = midlet; start(); }
   */
    public Object getComponent(String name) {
        String custom = getProperty(name + ".component");
        if (custom == null) return null;
        try {
            return Class.forName("javax.microedition.lcdui." + custom).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean getFlag(String flag) {
        String f = getProperty("me4se.flags");
        return f == null ? false : f.toLowerCase().indexOf(flag.toLowerCase()) != -1;
    }

    public boolean getBooleanProperty(String name, boolean dflt) {
        String v = getProperty(name);
        return v == null ? dflt : "true".equalsIgnoreCase(v.trim());
    }

    /** Maps a virtual key code to a button name */
    public String getButtonName(KeyEvent e) {
        int vk = e.getKeyCode();
        String name = (String) virtualKeyMap.get(new Integer(vk));
        if (name != null) {
            return name;
        }
        if (vk >= KeyEvent.VK_F1 && vk <= KeyEvent.VK_F8) {
            return "SOFT" + (vk - KeyEvent.VK_F1 + 1);
        }
        boolean swapNumPad = getBooleanProperty("me4se.swapNumPad", true);
        switch(vk) {
            case KeyEvent.VK_TAB:
                return e.isShiftDown() ? "UP" : "DOWN";
            case KeyEvent.VK_NUMPAD1:
                return swapNumPad ? "7" : "1";
            case KeyEvent.VK_NUMPAD2:
                return swapNumPad ? "8" : "2";
            case KeyEvent.VK_NUMPAD3:
                return swapNumPad ? "9" : "3";
            case KeyEvent.VK_NUMPAD4:
                return "4";
            case KeyEvent.VK_NUMPAD5:
                return "5";
            case KeyEvent.VK_NUMPAD6:
                return "6";
            case KeyEvent.VK_NUMPAD7:
                return swapNumPad ? "1" : "7";
            case KeyEvent.VK_NUMPAD8:
                return swapNumPad ? "2" : "8";
            case KeyEvent.VK_NUMPAD9:
                return swapNumPad ? "3" : "9";
            case KeyEvent.VK_ESCAPE:
                return "BACK";
            case KeyEvent.VK_DELETE:
                return "DELETE";
            case KeyEvent.VK_BACK_SPACE:
                return "CLEAR";
            case KeyEvent.VK_LEFT:
                return "LEFT";
            case KeyEvent.VK_RIGHT:
                return "RIGHT";
            case KeyEvent.VK_UP:
                return "UP";
            case KeyEvent.VK_DOWN:
                return "DOWN";
            case KeyEvent.VK_SPACE:
                return "SPACE";
            case KeyEvent.VK_CONTROL:
                return "CONTROL";
        }
        return "" + e.getKeyChar();
    }

    String getButtonName(int deviceKeyCode) {
        String name = (String) keyCodeToButtonName.get(new Integer(deviceKeyCode));
        if (name != null) return name;
        switch(deviceKeyCode) {
            case DEFAULT_KEYCODE_CLEAR:
                return "CLEAR";
            case DEFAULT_KEYCODE_DOWN:
                return "DOWN";
            case DEFAULT_KEYCODE_LEFT:
                return "LEFT";
            case DEFAULT_KEYCODE_RIGHT:
                return "RIGHT";
            case DEFAULT_KEYCODE_SELECT:
                return "SELECT";
            case DEFAULT_KEYCODE_UP:
                return "UP";
            case ' ':
                return "SPACE";
        }
        return "" + (char) deviceKeyCode;
    }

    /** Translate a device key code to a game action */
    public int getGameAction(int deviceKeyCode) {
        String buttonName = getButtonName(deviceKeyCode);
        Integer ga = ((Integer) gameActions.get(buttonName));
        if (ga != null) {
            return ga.intValue();
        }
        if (buttonName.equals("2") || buttonName.equals("UP")) {
            return Canvas.UP;
        }
        if (buttonName.equals("8") || buttonName.equals("DOWN")) {
            return Canvas.DOWN;
        }
        if (buttonName.equals("4") || buttonName.equals("LEFT")) {
            return Canvas.LEFT;
        }
        if (buttonName.equals("6") || buttonName.equals("RIGHT")) {
            return Canvas.RIGHT;
        }
        if (buttonName.equals("5") || buttonName.equals("SELECT") || buttonName.equals("SPACE") || '\n' == deviceKeyCode) {
            return Canvas.FIRE;
        }
        return 0;
    }

    /** Maps a button name to a device key code */
    public int getDeviceKeyCode(String buttonName) {
        int i = getIntProperty("keycode." + buttonName, -12345);
        if (i != -12345) return i;
        if (buttonName.length() == 1) return buttonName.charAt(0);
        buttonName = buttonName.toUpperCase();
        if (buttonName.equals("POUND")) return '#';
        if (buttonName.equals("ASTERISK")) return '*';
        if (buttonName.equals("UP")) return DEFAULT_KEYCODE_UP;
        if (buttonName.equals("DOWN")) return DEFAULT_KEYCODE_DOWN;
        if (buttonName.equals("LEFT")) return DEFAULT_KEYCODE_LEFT;
        if (buttonName.equals("RIGHT")) return DEFAULT_KEYCODE_RIGHT;
        if (buttonName.equals("SELECT")) return DEFAULT_KEYCODE_SELECT;
        if (buttonName.equals("SOFT1")) return DEFAULT_KEYCODE_LSK;
        if (buttonName.equals("SOFT2")) return DEFAULT_KEYCODE_RSK;
        if (buttonName.equals("SOFT3")) return -9;
        if (buttonName.equals("SOFT4")) return -10;
        if (buttonName.equals("CLEAR")) return DEFAULT_KEYCODE_CLEAR;
        if (buttonName.equals("MENU")) return DEFAULT_KEYCODE_MENU;
        if (buttonName.equals("MODE")) return DEFAULT_KEYCODE_MODE;
        if (buttonName.equals("SPACE")) return 32;
        if (buttonName.equals("DELETE")) return 127;
        if (buttonName.equals("CONTROL")) return -50;
        if (buttonName.equals("BACK")) return -11;
        return i;
    }

    public static String decodeJavaEscape(String encoded) {
        if (encoded.indexOf('\\') == -1) return encoded;
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < encoded.length(); i++) {
            if (encoded.charAt(i) == '\\') {
                char c = encoded.charAt(++i);
                switch(c) {
                    case 't':
                        buf.append('\t');
                        break;
                    case 'n':
                        buf.append('\r');
                        break;
                    case 'r':
                        buf.append('\n');
                        break;
                    case '\\':
                        buf.append('\\');
                        break;
                    case 'u':
                        buf.append((char) Integer.parseInt(encoded.substring(i + 1, i + 5), 16));
                        i += 4;
                        break;
                    default:
                        buf.append('\\');
                        buf.append(c);
                }
            } else {
                buf.append(encoded.charAt(i));
            }
        }
        return buf.toString();
    }

    /**
   * This method must be called to set system properties because of the applet
   * security issues
   */
    public void setSystemProperty(String key, String value) {
        if (applet != null) {
            org.me4se.System.properties.setProperty(key, value);
        } else {
            try {
                java.lang.System.setProperty(key, value);
            } catch (Throwable th) {
            }
        }
    }

    public String getSystemProperty(String key) {
        if (applet != null) {
            return org.me4se.System.properties.getProperty(key);
        } else {
            try {
                return java.lang.System.getProperty(key);
            } catch (Throwable th) {
            }
            return null;
        }
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("MIDlet");
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ApplicationManager.manager.active.notifyDestroyed();
            }
        });
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        return menuBar;
    }
}
