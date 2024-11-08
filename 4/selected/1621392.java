package pspdash;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import javax.swing.JOptionPane;

/**
 * This class is used for launching the current platforms browser.
 * @author Fredrik Ehnbom <fredde@gjt.org>
 * @version $Id: Browser.java 1570 2003-01-12 05:02:37Z tuma $
 */
public class Browser {

    private static String defaultHost = "localhost";

    private static int defaultPort = PSPDashboard.DEFAULT_WEB_PORT;

    public static final String BROWSER_LAUNCHER = "BrowserLauncher";

    public static void setDefaults(String host, int port) {
        defaultHost = host;
        defaultPort = port;
    }

    public static String mapURL(String url) {
        if (url.startsWith("http:/") || url.startsWith("file:/") || url.startsWith("mailto:")) return url;
        if (!url.startsWith("/")) url = "/" + url;
        url = "http://" + defaultHost + ":" + defaultPort + url;
        return url;
    }

    /**
     * Starts the browser for the current platform.
     * @param url The link to point the browser to.
     */
    public static void launch(String url) {
        launch(url, false);
    }

    public static void openDoc(String url) {
        launch(url, true);
    }

    private static void launch(String url, boolean document) {
        url = mapURL(url);
        String cmd = Settings.getFile("browser.command");
        if (document && isWindows()) cmd = null;
        try {
            if (cmd != null) {
                if (BROWSER_LAUNCHER.equalsIgnoreCase(cmd)) try {
                    BrowserLauncher.openURL(url);
                } catch (IOException ble) {
                    System.err.println(ble);
                    throw ble;
                } else {
                    cmd = cmd + " " + url;
                    Runtime.getRuntime().exec(cmd);
                }
            } else if (isWindows()) {
                cmd = ("rundll32 url.dll,FileProtocolHandler " + maybeFixupURLForWindows(url));
                Runtime.getRuntime().exec(cmd);
            } else {
                String windowName = ",window" + System.currentTimeMillis();
                cmd = "netscape -remote openURL(" + url + windowName + ")";
                Process p = Runtime.getRuntime().exec(cmd);
                int exitcode = p.waitFor();
                if (exitcode != 0) {
                    cmd = "netscape " + url;
                    Runtime.getRuntime().exec(cmd);
                }
            }
        } catch (InterruptedException ie) {
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(null, errorMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String[] errorMessage() {
        String cmd = Settings.getFile("browser.command");
        if (cmd == null) ERROR_MESSAGE[1] = "you need to specify the browser that you " + "want the dashboard to use."; else ERROR_MESSAGE[1] = "could not execute '" + cmd + "'";
        ERROR_MESSAGE[3] = "             " + InternalSettings.getSettingsFileName();
        return ERROR_MESSAGE;
    }

    private static String[] ERROR_MESSAGE = { "The Process Dashboard was unable to launch a web browser;", "", "To solve this problem, create or edit the file, ", "", "Add a line of the form, 'browser.command=command-to-run-browser',", "where command-to-run-browser is the complete path to a web browser", "executable such as Internet Explorer or Netscape.  Then restart", "the Process Dashboard." };

    private static String maybeFixupURLForWindows(String url) {
        if (url == null || url.length() < 2 || url.charAt(0) == '\\' || url.charAt(1) == ':') return url;
        if (url.startsWith("file:/")) return url.substring(6);
        String lower_url = url.toLowerCase();
        int i = badEndings.length;
        while (i-- > 0) if (lower_url.endsWith(badEndings[i])) return fixupURLForWindows(url);
        return url;
    }

    private static final String[] badEndings = { ".htm", ".html", ".htw", ".mht", ".cdf", ".mhtml", ".stm", ".shtm" };

    private static String fixupURLForWindows(String url) {
        if (url.indexOf('?') == -1) return url + "?"; else return url + "&workaroundStupidWindowsBug";
    }

    /**
     * Checks if the OS is windows.
     * @return true if it is, false if it's not.
     */
    public static boolean isWindows() {
        if (System.getProperty("os.name").indexOf("Windows") != -1) {
            return true;
        } else {
            return false;
        }
    }

    private static File getTrustlibDir() {
        File result = null;
        result = new File("c:\\windows\\java\\trustlib");
        if (result.isDirectory()) return result;
        result = new File("c:\\winnt\\java\\trustlib");
        if (result.isDirectory()) return result;
        return null;
    }

    private static void debug(String msg) {
    }

    private static void maybeSetupForWindowsIE() throws IOException {
        debug("maybeSetupForWindowsIE");
        if (!isWindows()) return;
        debug("isWindows.");
        File trustlibdir = getTrustlibDir();
        if (trustlibdir == null) return;
        debug("got trustlibdir");
        File pspdashdir = new File(trustlibdir, "pspdash");
        if (!(pspdashdir.isDirectory() || pspdashdir.mkdir())) return;
        debug("made pspdashdir");
        File datadir = new File(pspdashdir, "data");
        if (!(datadir.isDirectory() || datadir.mkdir())) return;
        debug("made datadir");
        copyClassFile(datadir, "OLEDBDSLWrapper.class");
        copyClassFile(datadir, "OLEDBListenerWrapper.class");
        debug("copied classes");
    }

    private static void copyClassFile(File destdir, String classFileName) throws IOException {
        File destFile = new File(destdir, classFileName);
        if (destFile.exists()) return;
        FileOutputStream out = new FileOutputStream(destFile);
        InputStream in = Browser.class.getResourceAsStream("/pspdash/data/" + classFileName);
        byte[] buffer = new byte[3000];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) > -1) out.write(buffer, 0, bytesRead);
        out.close();
        in.close();
    }
}
