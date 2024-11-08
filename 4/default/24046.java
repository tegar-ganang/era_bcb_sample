import java.awt.BorderLayout;
import java.io.*;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.comm.*;
import javax.speech.Central;
import javax.speech.synthesis.*;
import javax.swing.*;
import snoozesoft.systray4j.*;

/**
 * @author Owner
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class CallBlocker {

    private static OutputStream os;

    private static InputStream is;

    private static Synthesizer synthesizer;

    private static PrintWriter log;

    static Preferences prefs = Preferences.systemNodeForPackage(Preferences.class);

    static File logFile = new File(System.getProperty("user.home") + File.separator + "calls.log");

    static ControlPanel controlPanel = new ControlPanel(logFile);

    static DateFormat df = new SimpleDateFormat("EEE, MMM dd 'at' hh:mm:ss a");

    public static void main(String[] args) {
        try {
            WindowUtil.persistAndCenter(controlPanel);
            String drivername = "com.ibm.comm.IBMCommDriver";
            try {
                CommDriver driver = (CommDriver) Class.forName(drivername).newInstance();
                driver.initialize();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Using look and feel: " + UIManager.getLookAndFeel());
            initVoice();
            log = new PrintWriter(new OutputStreamWriter(new FileOutputStream(logFile, true)));
            String portName = prefs.get("port", null);
            if (portName == null) {
                portName = autoScanForModem();
                if (portName == null) return;
                prefs.put("port", portName);
            }
            CommPortIdentifier cpi = CommPortIdentifier.getPortIdentifier(portName);
            CommPort port = cpi.open("CallerBlock", 2000);
            port.enableReceiveTimeout(10000);
            try {
                os = port.getOutputStream();
                is = port.getInputStream();
                if (!isModem()) {
                    closeStreams();
                    try {
                        port.close();
                    } catch (Throwable t) {
                    }
                    ;
                    portName = autoScanForModem();
                    if (portName == null) return;
                    cpi = CommPortIdentifier.getPortIdentifier(portName);
                    port = cpi.open("CallerBlock", 2000);
                    port.enableReceiveTimeout(10000);
                    os = port.getOutputStream();
                    is = port.getInputStream();
                }
                initSysTray();
                outEchoCheck("AT#CID=1 S0=0 E0");
                readAll();
                String[] nameAndNumber = null;
                while (true) {
                    String line = readAll();
                    if (line == null) {
                        out("AT#CID=1 S0=0 E0");
                    }
                    nameAndNumber = extractNameAndNumber(line);
                    if (nameAndNumber != null) {
                        System.out.println("Found a name and number");
                        call(nameAndNumber[0], nameAndNumber[1]);
                    }
                }
            } finally {
                port.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            JFrame f = new JFrame("Error starting up");
            JComponent cp = (JComponent) f.getContentPane();
            cp.setLayout(new BorderLayout());
            cp.add(new JLabel(e.toString()));
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.pack();
            WindowUtil.center(f);
            f.setVisible(true);
        } finally {
            destroyVoice();
        }
    }

    /**
     * 
     */
    private static void initSysTray() {
        URL iconURL = CallBlocker.class.getResource("phone.ico");
        SysTrayMenuIcon sysTrayMenuIcon = new SysTrayMenuIcon(iconURL);
        SysTrayMenuListener listener = new SysTrayMenuListener() {

            public void menuItemSelected(SysTrayMenuEvent arg0) {
            }

            public void iconLeftClicked(SysTrayMenuEvent arg0) {
                trayClick();
            }

            public void iconLeftDoubleClicked(SysTrayMenuEvent arg0) {
                trayClick();
            }
        };
        sysTrayMenuIcon.addSysTrayMenuListener(listener);
        SysTrayMenu menu = new SysTrayMenu(sysTrayMenuIcon);
        menu.setToolTip("Call Blocker");
        SysTrayMenuItem exitMenuItem = new SysTrayMenuItem("Exit Call Blocker");
        exitMenuItem.addSysTrayMenuListener(new SysTrayMenuAdapter() {

            public void menuItemSelected(SysTrayMenuEvent arg0) {
                System.exit(0);
            }
        });
        menu.addItem(exitMenuItem);
        SysTrayMenuItem openMenuItem = new SysTrayMenuItem("Open");
        openMenuItem.addSysTrayMenuListener(new SysTrayMenuAdapter() {

            public void menuItemSelected(SysTrayMenuEvent arg0) {
                trayClick();
            }
        });
        menu.addItem(openMenuItem);
        if (getStartupFolder() != null && WebstartUtil.isWebstart()) {
            startupCheckbox = new CheckableMenuItem("Run On Startup");
            startupCheckbox.addSysTrayMenuListener(new SysTrayMenuAdapter() {

                public void menuItemSelected(SysTrayMenuEvent arg0) {
                    installOrUninstall();
                }
            });
            menu.addItem(startupCheckbox);
            if (getStartupFile() != null && getStartupFile().exists()) {
                copyJnlpToStartup();
            }
            setStartupCheckboxState();
        }
    }

    protected static void installOrUninstall() {
        if (startupCheckbox.getState()) {
            copyJnlpToStartup();
        } else {
            File startupFile = getStartupFile();
            if (startupFile != null) {
                startupFile.delete();
            }
        }
        setStartupCheckboxState();
    }

    /**
     * 
     */
    private static void copyJnlpToStartup() {
        File callBlockerJnlp = WebstartUtil.findCacheFile(synthesizer != null ? "AMcallblocker-vocal.jnlp" : "AMcallblocker.jnlp");
        File startupFile = getStartupFile();
        if (callBlockerJnlp != null && startupFile != null) {
            try {
                copyFile(startupFile, callBlockerJnlp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void copyFile(File destFile, File src) throws IOException {
        File destDir = destFile.getParentFile();
        File tempFile = new File(destFile + "_tmp");
        destDir.mkdirs();
        InputStream is = new FileInputStream(src);
        try {
            FileOutputStream os = new FileOutputStream(tempFile);
            try {
                byte[] buf = new byte[8192];
                int len;
                while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
            } finally {
                os.close();
            }
        } finally {
            is.close();
        }
        destFile.delete();
        if (!tempFile.renameTo(destFile)) throw new IOException("Unable to rename " + tempFile + " to " + destFile);
    }

    private static void setStartupCheckboxState() {
        File startupFile = getStartupFile();
        if (startupFile == null) return;
        startupCheckbox.setState(startupFile.isFile());
    }

    private static File getStartupFile() {
        File startupFolder = getStartupFolder();
        if (startupFolder != null) {
            return new File(startupFolder, "Call Blocker.jnlp");
        }
        return null;
    }

    private static File getStartupFolder() {
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        if (!isWindows) return null;
        String homeDir = System.getProperty("user.home");
        if (homeDir == null) return null;
        File programsDir = new File(new File(homeDir), "Start Menu\\Programs");
        if (!programsDir.isDirectory()) return null;
        File startupDir = new File(programsDir, "Startup");
        startupDir.mkdirs();
        if (!startupDir.isDirectory()) return null;
        return startupDir;
    }

    private static String autoScanForModem() {
        JFrame f = new JFrame("GreenScreen Call Screener");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        GridBag gb = new GridBag(1);
        JLabel status = new JLabel();
        gb.add("Scanning ports for modem...");
        gb.add(status);
        f.setContentPane(gb);
        f.setSize(400, 100);
        WindowUtil.center(f);
        f.setVisible(true);
        try {
            for (int i = 1; i <= 4; i++) {
                CommPort port = null;
                try {
                    String portName = "COM" + i;
                    status.setText("Checking " + portName + "...");
                    CommPortIdentifier cpi = CommPortIdentifier.getPortIdentifier(portName);
                    port = cpi.open("CallerBlock", 2000);
                    port.enableReceiveTimeout(2000);
                    os = port.getOutputStream();
                    is = port.getInputStream();
                    boolean isModem = isModem();
                    if (isModem) {
                        outEchoCheck("AT#CID=1 S0=0 E0");
                        String response = readAll();
                        if (!response.trim().equals("OK")) {
                            status.setText("Found a modem on " + portName + ", but it doesn't appear to have caller ID support. Close this window to exit.");
                            while (true) {
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e) {
                                    return null;
                                }
                            }
                        }
                        return portName;
                    }
                } catch (Throwable t) {
                } finally {
                    closeStreams();
                    if (port != null) try {
                        port.close();
                    } catch (Throwable t) {
                    }
                    ;
                }
            }
            status.setText("Can't find modem.  Close this window to exit.");
            while (true) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    return null;
                }
            }
        } finally {
            f.dispose();
        }
    }

    /**
     * 
     */
    private static void closeStreams() {
        if (os != null) try {
            os.close();
        } catch (Throwable t) {
        }
        ;
        if (is != null) try {
            is.close();
        } catch (Throwable t) {
        }
        ;
    }

    /**
     * @return
     * @throws IOException
     */
    private static boolean isModem() throws IOException {
        outEchoCheck("AT");
        String response = readAll();
        boolean isModem = response.trim().equals("OK");
        return isModem;
    }

    public static boolean isBlocked(String name, String number) {
        StringTokenizer st = new StringTokenizer(prefs.get("blockList", ""), "\r\n");
        String key = name + " " + number;
        while (st.hasMoreTokens()) {
            if (st.nextToken().equals(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     */
    protected static void trayClick() {
        controlPanel.setVisible(true);
        controlPanel.setExtendedState(JFrame.NORMAL);
    }

    /**
     * 
     */
    private static void destroyVoice() {
        if (synthesizer == null) return;
        try {
            synthesizer.waitEngineState(Synthesizer.QUEUE_EMPTY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            synthesizer.deallocate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     */
    private static void initVoice() {
        try {
            String voiceName = "kevin16";
            Central.registerEngineCentral("com.sun.speech.freetts.jsapi.FreeTTSEngineCentral");
            SynthesizerModeDesc desc = new SynthesizerModeDesc(null, "general", Locale.US, null, null);
            synthesizer = Central.createSynthesizer(desc);
            if (synthesizer == null) {
                System.err.println(noSynthesizerMessage());
            }
            synthesizer.allocate();
            synthesizer.resume();
            desc = (SynthesizerModeDesc) synthesizer.getEngineModeDesc();
            Voice[] voices = desc.getVoices();
            Voice voice = null;
            for (int i = 0; i < voices.length; i++) {
                if (voices[i].getName().equals(voiceName)) {
                    voice = voices[i];
                    break;
                }
            }
            if (voice == null) {
                System.err.println("Synthesizer does not have a voice named " + voiceName + ".");
            }
            synthesizer.getSynthesizerProperties().setVoice(voice);
        } catch (Throwable e) {
            e.printStackTrace();
            synthesizer = null;
        }
    }

    /**
     * @param name
     * @param nmbr
     */
    private static void call(String name, String nmbr) {
        if (name == null) name = "";
        if (nmbr == null) nmbr = "";
        String logLine = df.format(new Date()) + "\t" + name + "\t" + nmbr;
        log.println(logLine);
        log.flush();
        controlPanel.add(logLine);
        new Message(new CallPanel(name, nmbr));
        String toSay = name;
        if (isBlocked(name, nmbr)) {
            System.out.println("Attempting to hang up");
            try {
                out("ATH1");
                readAll();
                out("ATH");
                readAll();
            } catch (Exception e) {
                new Message("Error blocking: " + e.toString());
            }
            toSay += ": blocking";
        }
        say("kevin16", toSay);
    }

    /**
     * @param outString the string to write
     * @throws IOException
     */
    private static void outEchoCheck(String outString) throws IOException {
        System.out.println("> " + outString);
        outString += '\r';
        os.write(outString.getBytes());
        byte[] echoBytes = new byte[outString.length() + 2];
        is.read(echoBytes);
        String echo = new String(echoBytes);
        System.out.print("< " + echo);
        if (!echo.equals(outString + "\r\n")) {
            throw new IOException("Expected " + outString + " got echo: " + echo);
        }
    }

    /**
     * @param outString The string to send to the modem
     * @throws IOException
     */
    private static void out(String outString) throws IOException {
        System.out.println("> " + outString);
        outString += '\r';
        os.write(outString.getBytes());
    }

    private static String readAll() throws IOException {
        int ch = is.read();
        if (ch == -1) {
            return null;
        }
        byte[] bytes = new byte[is.available() + 1];
        bytes[0] = (byte) ch;
        is.read(bytes, 1, bytes.length - 1);
        String line = new String(bytes);
        System.out.print("< " + line);
        return line;
    }

    /**
     * Returns a "no synthesizer" message, and asks 
     * the user to check if the "speech.properties" file is
     * at <code>user.home</code> or <code>java.home/lib</code>.
     *
     * @return a no synthesizer message
     */
    private static String noSynthesizerMessage() {
        String message = "No synthesizer created.  This may be the result of any\n" + "number of problems.  It's typically due to a missing\n" + "\"speech.properties\" file that should be at either of\n" + "these locations: \n\n";
        message += "user.home    : " + System.getProperty("user.home") + "\n";
        message += "java.home/lib: " + System.getProperty("java.home") + File.separator + "lib\n\n" + "Another cause of this problem might be corrupt or missing\n" + "voice jar files in the freetts lib directory.  This problem\n" + "also sometimes arises when the freetts.jar file is corrupt\n" + "or missing.  Sorry about that.  Please check for these\n" + "various conditions and then try again.\n";
        return message;
    }

    private static void say(String voiceName, String text) {
        if (synthesizer == null) return;
        synthesizer.speakPlainText(text, null);
    }

    private static Pattern numberPattern = Pattern.compile("^NMBR=(.*)$", Pattern.MULTILINE);

    private static Pattern namePattern = Pattern.compile("^NAME=(.*)$", Pattern.MULTILINE);

    private static CheckableMenuItem startupCheckbox;

    /**
     * 
     * @param cid
     * @return a string array of length 2, where the first element is the name and the second is the number, or null if niether a name or a number was found
     */
    public static String[] extractNameAndNumber(String cid) {
        if (cid == null) return null;
        String number = null;
        String name = null;
        Matcher m = numberPattern.matcher(cid);
        if (m.find()) {
            number = m.group(1);
            if (number.equalsIgnoreCase("out of area") && m.find()) {
                name = number;
                number = m.group(1);
            }
        }
        m = namePattern.matcher(cid);
        if (m.find()) {
            name = m.group(1);
        }
        if (name == null && number == null) return null;
        return new String[] { name, number };
    }
}
