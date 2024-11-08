import java.io.*;

public class Platform {

    private static boolean _trayEnabled = false;

    /**
   * @brief Clears up a PMD warning, as this class is entirely static,
   * but is NOT intended to be used as a Singleton.
   */
    private Platform() {
    }

    /** 
   * @brief In order to support the everpresent top-of-screen menu bar
   * on the Mac, we need this assistance function to add a menu bar to
   * any frames we create.
   * 
   * @param newFrame - The frame to add the standard menu bar to.
   */
    public static void setMacFrameMenu(javax.swing.JFrame newFrame) {
        if (isMac()) {
            JTabManager jtm = AuctionsUIModel.getTabManager();
            JBidMenuBar mb2 = JBidMenuBar.getInstance(jtm, "Search Editor");
            newFrame.setJMenuBar(mb2);
        }
    }

    /** 
   * @brief Is the current machine a Mac?
   *
   * Note: Since MacOS 9 and prior never had a Java runtime later than
   * 1.1.8, we MUST be running on OSX.  It's an assumption, but a good
   * one.
   * 
   * @return - true if we are running on a Mac, false otherwise.
   */
    public static boolean isMac() {
        return JConfig.queryConfiguration("mac", "false").equals("true");
    }

    public static boolean isLinux() {
        return JConfig.getOS().equalsIgnoreCase("linux");
    }

    public static boolean isWindows() {
        return JConfig.getOS().equalsIgnoreCase("windows");
    }

    /** 
   * @brief Set up the Mac UI information, based on the configuration.
   */
    public static void setupMacUI() {
        if (System.getProperty("mrj.version") != null) {
            JConfig.setConfiguration("mac", "true");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.macos.useScreenMenuBar", "true");
            if (JConfig.queryConfiguration("mac.useMetal", "true").equals("false")) {
                System.setProperty("apple.awt.brushMetalLook", "false");
            }
        }
    }

    public static void checkLaF(String lookAndFeel) {
        if (System.getProperty("mrj.version") != null) {
            if (javax.swing.UIManager.getLookAndFeel().getClass().getName().equals(lookAndFeel)) {
                JConfig.setConfiguration("mac.aqua", "true");
            } else {
                JConfig.setConfiguration("mac.aqua", "false");
            }
        } else {
            JConfig.setConfiguration("mac", "false");
            JConfig.setConfiguration("mac.aqua", "false");
        }
    }

    public static boolean extractAndLoadLibrary() {
        String outDll = JConfig.queryConfiguration("platform.path", null);
        File fp = new File(outDll);
        File output = new File(fp, "tray.dll");
        if (!output.exists() || output.length() == 0) {
            if (output.exists()) output.delete();
            if (!dumpFile("/platform/tray.dll", output.getAbsolutePath())) return false;
        }
        try {
            File doubleCheck = new File(fp, "tray.dll");
            if (doubleCheck.exists() && doubleCheck.length() != 0) {
                System.load(output.getAbsolutePath());
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    private static boolean dumpFile(String inJarName, String destination) {
        File f = new File(destination);
        try {
            f.createNewFile();
        } catch (Exception e) {
            ErrorManagement.handleException("Can't create output file to copy from JAR.", e);
            return false;
        }
        InputStream source = JBidWatch.class.getClass().getResourceAsStream(inJarName);
        if (source == null) {
            ErrorManagement.logDebug("Failed to open internal resource!");
        }
        BufferedInputStream in = new BufferedInputStream(source);
        try {
            FileOutputStream out = new FileOutputStream(f);
            int ch;
            while ((ch = in.read()) != -1) out.write(ch);
            try {
                in.close();
                out.close();
            } catch (Exception e) {
            }
        } catch (IOException e) {
            ErrorManagement.handleException("Couldn't extract file (" + inJarName + " from jar to " + destination + ".", e);
            return false;
        }
        return true;
    }

    public static void setTrayEnabled(boolean b) {
        _trayEnabled = b;
    }

    public static boolean isTrayEnabled() {
        return _trayEnabled;
    }
}
