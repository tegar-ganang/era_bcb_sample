import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.io.*;
import java.net.URL;
import java.lang.reflect.Method;
import java.util.Arrays;

public class Common {

    public static int UNIT_INCREMENT = 16;

    public static final JFileChooser fc;

    public static final String HTML_EXT = ".html";

    static final String[] browsers = { "firefox", "opera", "konqueror", "epiphany", "seamonkey", "galeon", "kazehakase", "mozilla", "netscape", "chrome" };

    static final Runtime rt = Runtime.getRuntime();

    static final String osName = System.getProperty("os.name");

    static {
        JFileChooser jfc;
        try {
            jfc = new JFileChooser();
            jfc.addChoosableFileFilter(new FileFilter() {

                public boolean accept(File f) {
                    String n = f.getName().toLowerCase();
                    return f.isDirectory() || n.endsWith(HTML_EXT);
                }

                public String getDescription() {
                    return "HTML files (*.html)";
                }
            });
        } catch (Exception ex) {
            jfc = null;
            System.err.println("Could not create a file selector window");
        }
        fc = jfc;
    }

    private Common() {
    }

    public static JScrollPane scroller(Component c) {
        JScrollPane jsp = new JScrollPane(c);
        jsp.getVerticalScrollBar().setUnitIncrement(UNIT_INCREMENT);
        jsp.getHorizontalScrollBar().setUnitIncrement(UNIT_INCREMENT);
        return jsp;
    }

    public static void showError(Component p, String msg) {
        JOptionPane.showMessageDialog(p, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean showWarning(Component p, String msg) {
        int result = JOptionPane.showConfirmDialog(p, msg, "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }

    public static void rest(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
        }
    }

    public static String timestamp(Calendar c) {
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int min = c.get(Calendar.MINUTE);
        int sec = c.get(Calendar.SECOND);
        return (hour < 10 ? "0" : "") + hour + ":" + (min < 10 ? "0" : "") + min + ":" + (sec < 10 ? "0" : "") + sec;
    }

    public static String timestamp() {
        return timestamp(new GregorianCalendar());
    }

    public static String fileAsString(String fname) throws IOException {
        InputStream is = getLocalFileInputStream(fname);
        StringBuffer sb = new StringBuffer();
        byte[] buffer = new byte[1023];
        int len;
        while ((len = is.read(buffer)) >= 0) {
            sb.append(new String(buffer, 0, len));
        }
        return sb.toString();
    }

    public static InputStream getLocalFileInputStream(String fname) throws IOException {
        return Common.class.getResource(fname).openStream();
    }

    public static void stringToFile(String text, File f) throws IOException {
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(text.getBytes());
    }

    public static void guiWriteHtmlFile(String text, Component p) throws IOException {
        if (fc.showSaveDialog(p) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(HTML_EXT)) {
                f = new File(f.getParent(), f.getName() + HTML_EXT);
            }
            if (!(f.exists() && !showWarning(p, "The file " + f.getName() + " already exists. " + "Are you sure you want to " + "overwrite it?"))) {
                stringToFile(text, f);
            }
        }
    }

    public static void openURL(String url) throws Exception {
        if (osName.startsWith("Mac OS")) {
            Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
            Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] { String.class });
            openURL.invoke(null, new Object[] { url });
        } else if (osName.startsWith("Windows")) {
            rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
        } else {
            boolean found = false;
            for (String browser : browsers) {
                if (!found) {
                    found = rt.exec(new String[] { "which", browser }).waitFor() == 0;
                    if (found) rt.exec(new String[] { browser, url });
                }
            }
            if (!found) throw new Exception("Could not launch any web browser");
        }
    }
}
