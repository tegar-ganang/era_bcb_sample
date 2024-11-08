package com.adidas.micoach.agent.utils;

import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Document;
import com.adidas.micoach.agent.AppImages;
import com.adidas.micoach.agent.os.IOSDependent;
import com.adidas.micoach.agent.resource.AppResources;
import com.adidas.micoach.agent.settings.AppSettings;
import com.adidas.micoach.agent.settings.SettingID;
import com.adidas.micoach.agent.settings.SettingsManager;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class Utils {

    public static int BACKGROUND_COLOR = 1;

    public static Color LINK_COLOR = new Color(Display.getCurrent(), 2, 124, 197);

    public static final String UPDATE_FILE_NAME = "DesktopManagerUpdate.jar";

    private static AppImages appImages = new AppImages(Display.getDefault());

    public static void showHelp() {
        Program.launch(SettingsManager.debugSettings().getString(SettingID.HelpURL_Debug));
    }

    public static Image getImage(String id) {
        return appImages.getImage(id);
    }

    public static Font getBoldFont(Display display, Font font) {
        FontData[] fontData = font.getFontData();
        for (FontData fd : fontData) {
            fd.setStyle(1);
        }
        return new Font(display, fontData);
    }

    public static int[] circle(int r, int xx, int yy) {
        int offsetX = r;
        int offsetY = r;
        int[] polygon = new int[8 * r + 4];
        for (int i = 0; i < 2 * r + 1; ++i) {
            int x = i - r;
            int y = (int) Math.sqrt(r * r - (x * x));
            polygon[(2 * i)] = (xx + offsetX + x);
            polygon[(2 * i + 1)] = (yy + offsetY + y);
            polygon[(8 * r - (2 * i) - 2)] = (xx + offsetX + x);
            polygon[(8 * r - (2 * i) - 1)] = (yy + offsetY - y);
        }
        return polygon;
    }

    public static void setBackground(Composite c, Color color) {
        for (Control ctrl : c.getChildren()) {
            if (Composite.class.isAssignableFrom(ctrl.getClass())) {
                setBackground((Composite) ctrl, color);
            } else {
                ctrl.setBackground(color);
            }
        }
        c.setBackground(color);
    }

    public static void centerWindow(Display display, Composite parent, Composite child) {
        Rectangle bounds;
        if (parent == null) {
            Monitor primary = display.getPrimaryMonitor();
            bounds = primary.getBounds();
        } else {
            bounds = parent.getBounds();
        }
        Rectangle rect = child.getBounds();
        int x = bounds.x + (bounds.width - rect.width) / 2;
        int y = bounds.y + (bounds.height - rect.height) / 2;
        child.setLocation(x, y);
    }

    public static Label addEmptyLabel(Composite c) {
        Label l = new Label(c, 0);
        l.setText("");
        l.setLayoutData(new GridData(512));
        return l;
    }

    public static void createFilePath(String path, boolean isDirectory) {
        if (path.lastIndexOf(File.separator) > 0) new File((isDirectory) ? path : path.substring(0, path.lastIndexOf(File.separator))).mkdirs();
    }

    public static IOSDependent getOSDependent() {
        IOSDependent osDependent = null;
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("windows")) {
            try {
                osDependent = (IOSDependent) Class.forName("com.adidas.micoach.agent.os.win.Win32Dependent").newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (osName.startsWith("mac")) {
            try {
                osDependent = (IOSDependent) Class.forName("com.adidas.micoach.agent.os.osx.OsxDependent").newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (osName.startsWith("linux")) {
            try {
                osDependent = (IOSDependent) Class.forName("com.adidas.micoach.agent.os.linux.LinuxDependent").newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return osDependent;
    }

    public static void focusText(Text txt) {
        txt.setSelection(0, txt.getText().length());
        txt.setFocus();
    }

    public static void setProxy(HttpClient client, HttpMethod method) {
        AppSettings settings = SettingsManager.appSettings();
        if (!(settings.getBoolean(SettingID.UseProxy).booleanValue())) return;
        client.getHostConfiguration().setProxy(settings.getString(SettingID.ProxyServer), settings.getInteger(SettingID.ProxyPort).intValue());
        client.getState().setProxyCredentials(AuthScope.ANY, new NTCredentials(settings.getString(SettingID.ProxyUsername), settings.getString(SettingID.ProxyPassword), (String) System.getenv().get("COMPUTERNAME"), (String) System.getenv().get("USERDOMAIN")));
        method.setDoAuthentication(true);
    }

    public static String getProgramFolder() {
        String s = Utils.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("windows")) {
            return s.substring(0, s.lastIndexOf("/")).replaceAll("%20", " ");
        }
        if (osName.startsWith("mac")) {
            if (s.indexOf("miCoachManager.app") != -1) {
                return s.substring(0, s.indexOf("miCoachManager.app")).replaceAll("%20", " ");
            }
            return s;
        }
        return "";
    }

    public static Document createXMLDocument() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.newDocument();
    }

    public static String xml2String(Document dom) {
        OutputFormat format = new OutputFormat(dom);
        format.setIndenting(true);
        StringWriter sw = new StringWriter();
        XMLSerializer serializer = new XMLSerializer(sw, format);
        try {
            serializer.serialize(dom);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sw.getBuffer().toString();
    }

    public static String getXMLObject(Object o) {
        final StringWriter sw = new StringWriter();
        XMLEncoder xmlEncoder = new XMLEncoder(new OutputStream() {

            public void write(int b) throws IOException {
                sw.write(b);
            }
        });
        xmlEncoder.writeObject(o);
        xmlEncoder.close();
        try {
            sw.close();
            return sw.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "error";
    }

    public static void copyFile(String src, String dest) throws FileNotFoundException, IOException {
        File destFile = new File(dest);
        InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(src);
        OutputStream out = new FileOutputStream(destFile);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
        System.out.println("File copied.");
    }

    public static Font getFont(int style, int height) {
        if ((AppResources.localeString != null) && (AppResources.localeString.equalsIgnoreCase("kor"))) {
            return new Font(Display.getCurrent(), "", height - 2, style);
        }
        return new Font(Display.getCurrent(), "arial", height - 2, style);
    }

    public static Font getBoldFont(int height) {
        return getFont(1, height);
    }

    public static Color getColor(int hex) {
        return new Color(Display.getCurrent(), (hex & 0xFF0000) >> 16, (hex & 0xFF00) >> 8, hex & 0xFF);
    }

    public static String getOSVersion() {
        return Version.getInstance().getPlatform();
    }

    public static boolean hasEqualBytes(byte[] one, byte[] two) {
        if ((one == null) || (two == null)) {
            return false;
        }
        if (one.length != two.length) {
            return false;
        }
        int length = one.length;
        int byteCount = 0;
        while (byteCount < length) {
            if (one[byteCount] != two[byteCount]) {
                return false;
            }
            ++byteCount;
        }
        return true;
    }

    public static boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    public abstract static interface Delegate {

        public abstract void method();
    }

    public static Font getFontForCheckBox(FontData fontData, int style, int height) {
        if (AppResources.localeString != null && AppResources.localeString.equalsIgnoreCase("kor")) {
            fontData.setHeight(height - 2);
            return new Font(Display.getCurrent(), fontData);
        } else {
            return new Font(Display.getCurrent(), "arial", height - 2, style);
        }
    }

    public static boolean validateEmail(String emailId) {
        String pattern = "^[A-Za-z0-9!#$%&amp;'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&amp;'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?$";
        return emailId.matches(pattern);
    }

    public static void copyFileTo(String src, String dest) throws FileNotFoundException, IOException {
        File destFile = new File(dest);
        InputStream in = new FileInputStream(new File(src));
        OutputStream out = new FileOutputStream(destFile);
        byte buf[] = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close();
        out.close();
    }

    public static boolean deleteFile(String filePath) {
        File file = null;
        boolean flag;
        try {
            file = new File(filePath);
            flag = file.delete();
            file = null;
            return flag;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            file = null;
        }
        return false;
    }

    public static String getGMTDateFormat(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy H:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(date);
    }

    public static String getXORString(String inputString) {
        StringBuffer finalString = new StringBuffer();
        String key = "!miC0@ch!";
        if (inputString != null && inputString.length() > 0 && key != null && key.length() > 0) {
            int j = -1;
            for (int i = 0; i < inputString.length(); i++) {
                char actualChar = inputString.charAt(i);
                if (++j >= key.length()) j = 0;
                char keyChar = key.charAt(j);
                int intKeyChar = keyChar;
                int hashedCharValue = actualChar ^ intKeyChar;
                if (hashedCharValue <= 32) hashedCharValue += 32;
                finalString.append((char) hashedCharValue);
            }
        } else {
            log.error(" inputString/key may be not valid");
        }
        return finalString.toString();
    }

    public static boolean isWinXP() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows xp");
    }

    public static boolean isAdmin() {
        try {
            Class NTSystem = Class.forName("com.sun.security.auth.module.NTSystem");
            Method method = NTSystem.getMethod("getGroupIDs", new Class[0]);
            Object ntSystem = NTSystem.newInstance();
            String groups[] = (String[]) (String[]) method.invoke(ntSystem, new Object[0]);
            for (int i = 0; i < groups.length; ++i) {
                String group = groups[i];
                if (!group.equals("S-1-5-32-544")) {
                    log.info((new StringBuilder()).append("Current system user is belongs to admin group and SID: ").append(group).toString());
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Error occured in loading NTSystem class");
        }
        return false;
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    protected static final DebugLog log = DebugLog.getLog("Dialog");

    public static final String SETUP_LAUNCHER_FILE_NAME = "miCoachSetupLauncher.exe";

    public static final short PACER_VID = 7894;

    public static final short PACER_PID = -4377;

    public static final byte PACER_UNLOCK_KEY[] = { 65, 68, 48, 56, 50, 48, 48, 56, 66, 49, 68, 48, 48, 49 };

    public static final byte PACER_LOCK_KEY[] = { 95, 95, 76, 79, 67, 75, 95, 95 };

    public static final byte PACER_FORMAT_KEY[] = { 95, 95, 70, 79, 82, 77, 65, 84, 95, 95 };

    public static final byte PACER_STOPUNIT_KEY[] = { 95, 95, 83, 84, 79, 80, 85, 78, 73, 84, 95, 95 };
}
