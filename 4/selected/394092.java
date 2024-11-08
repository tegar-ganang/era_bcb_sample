package integration;

import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Map;
import net.sf.wraplog.SystemLogger;
import winstone.Logger;

public class KenshiraDesktop implements ActionListener, MouseListener {

    private static final int BUFFER_BLOCK_SIZE = 4 * 1024;

    Frame frame;

    private Font font;

    private Image icon16, icon24;

    private Button startBrowser;

    private TextField text;

    private boolean isWindows;

    WinstoneLauncher launcher;

    BrowserLauncher browserLauncher;

    Map args;

    String kenshiraUrl;

    /**
     * The command line interface for this tool.
     * The command line options are the same as in the Server tool,
     * but this tool will always start the TCP, TCP and PG server.
     * Options are case sensitive.
     *
     * @param argv the command line arguments
     */
    public static void main(String[] argv) throws IOException {
        int exitCode = new KenshiraDesktop(argv).run();
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public KenshiraDesktop(String[] argv) throws IOException {
        isWindows = getStringSetting("os.name", "").startsWith("Windows");
        args = WinstoneLauncher.getArgsFromCommandLine(argv);
    }

    private int run() {
        int exitCode = 0;
        try {
            showWindow(true, true);
            prepareEmbeddedWebappToBeDeployable();
            startWinstoneServer();
            initBrowserLauncher();
            startBrowser.setVisible(true);
            text.setText(kenshiraUrl);
            frame.invalidate();
            frame.validate();
            startBrowser();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return exitCode;
    }

    private void initBrowserLauncher() {
        try {
            browserLauncher = new BrowserLauncher(new SystemLogger());
            StringBuilder sb = new StringBuilder("Detected browsers: ");
            for (Object o : browserLauncher.getBrowserList()) {
                sb.append("'" + o + "' ");
            }
            Logger.logDirectMessage(Logger.MAX, "Kenshira", sb.toString(), null);
        } catch (BrowserLaunchingInitializingException e) {
            e.printStackTrace();
        } catch (UnsupportedOperatingSystemException e) {
            e.printStackTrace();
        }
    }

    public String prepareEmbeddedWebappToBeDeployable() throws IOException {
        args.remove("usage");
        args.remove("help");
        String kenshiraWebRoot = (String) args.remove("kenshira.webroot");
        System.getProperties().setProperty("kenshira.desktop", "true");
        if (kenshiraWebRoot != null) {
            args.put("webroot", kenshiraWebRoot);
            System.getProperties().setProperty("kenshira.webroot", kenshiraWebRoot);
        } else {
            String embeddedWarfileName = (String) args.get("war.embedded");
            if (embeddedWarfileName == null) embeddedWarfileName = "/kenshira.war";
            InputStream embeddedWarfile = KenshiraDesktop.class.getResourceAsStream(embeddedWarfileName);
            if (embeddedWarfile != null) {
                File tempWarfile = File.createTempFile("embedded", ".war").getAbsoluteFile();
                tempWarfile.getParentFile().mkdirs();
                tempWarfile.deleteOnExit();
                File tempWebroot = new File(tempWarfile.getParentFile(), "kenshiraRoot" + System.currentTimeMillis());
                tempWebroot.mkdirs();
                OutputStream out = new FileOutputStream(tempWarfile, true);
                int read = 0;
                byte buffer[] = new byte[BUFFER_BLOCK_SIZE];
                while ((read = embeddedWarfile.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.close();
                embeddedWarfile.close();
                args.put("warfile", tempWarfile.getAbsolutePath());
                args.put("webroot", tempWebroot.getAbsolutePath());
                args.remove("webappsDir");
                args.remove("hostsDir");
                kenshiraWebRoot = tempWebroot.getAbsolutePath();
                System.getProperties().setProperty("kenshira.warfile", tempWarfile.getAbsolutePath());
                System.getProperties().setProperty("kenshira.webroot", kenshiraWebRoot);
            } else {
                throw new RuntimeException(embeddedWarfileName + " no encontrado");
            }
        }
        return kenshiraWebRoot;
    }

    private void startWinstoneServer() throws IOException {
        launcher = new WinstoneLauncher(args);
        if (launcher.getHttpPort() == 80) {
            kenshiraUrl = "http://localhost";
        } else {
            kenshiraUrl = "http://localhost:" + launcher.getHttpPort();
        }
        System.getProperties().setProperty("kenshira.url", kenshiraUrl);
    }

    private Image loadImage(String name) throws IOException {
        byte[] imageData = getResource(name);
        if (imageData == null) {
            return null;
        }
        return Toolkit.getDefaultToolkit().createImage(imageData);
    }

    public static byte[] getResource(String name) throws IOException {
        InputStream in = KenshiraDesktop.class.getResourceAsStream(name);
        try {
            int length = Integer.MAX_VALUE;
            int block = Math.min(BUFFER_BLOCK_SIZE, length);
            ByteArrayOutputStream out = new ByteArrayOutputStream(block);
            byte[] buff = new byte[block];
            while (length > 0) {
                int len = Math.min(block, length);
                len = in.read(buff, 0, len);
                if (len < 0) {
                    break;
                }
                out.write(buff, 0, len);
                length -= len;
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    public void shutdown() {
        stopAll();
    }

    void stopAll() {
        launcher.shutdown();
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
        System.exit(0);
    }

    private boolean createTrayIcon() {
        try {
            Boolean supported = (Boolean) Class.forName("java.awt.SystemTray").getMethod("isSupported", new Class[0]).invoke(null, new Object[0]);
            if (!supported.booleanValue()) {
                return false;
            }
            PopupMenu menuTray = new PopupMenu();
            MenuItem itemOpenBrowser = new MenuItem("Abrir Kenshira");
            itemOpenBrowser.setActionCommand("startBrowser");
            itemOpenBrowser.addActionListener(this);
            itemOpenBrowser.setFont(font);
            menuTray.add(itemOpenBrowser);
            MenuItem itemStatus = new MenuItem("Status");
            itemStatus.setActionCommand("status");
            itemStatus.addActionListener(this);
            itemStatus.setFont(font);
            menuTray.add(itemStatus);
            MenuItem itemExit = new MenuItem("Salir");
            itemExit.setFont(font);
            itemExit.setActionCommand("exit");
            itemExit.addActionListener(this);
            menuTray.add(itemExit);
            Object tray = Class.forName("java.awt.SystemTray").getMethod("getSystemTray", new Class[0]).invoke(null, new Object[0]);
            Dimension d = (Dimension) Class.forName("java.awt.SystemTray").getMethod("getTrayIconSize", new Class[0]).invoke(tray, new Object[0]);
            Image icon = (d.width >= 24 && d.height >= 24) ? icon24 : icon16;
            Object trayIcon = Class.forName("java.awt.TrayIcon").getConstructor(new Class[] { Image.class, String.class, PopupMenu.class }).newInstance(new Object[] { icon, "Kenshira", menuTray });
            trayIcon.getClass().getMethod("addMouseListener", new Class[] { MouseListener.class }).invoke(trayIcon, new Object[] { this });
            tray.getClass().getMethod("add", new Class[] { Class.forName("java.awt.TrayIcon") }).invoke(tray, new Object[] { trayIcon });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void showWindow(final boolean closeButtonExits, boolean firstTime) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        if (isWindows) {
            font = new Font("Dialog", Font.PLAIN, 11);
        } else {
            font = new Font("Dialog", Font.PLAIN, 12);
        }
        try {
            icon16 = loadImage("/integration/h2.png");
            icon24 = loadImage("/integration/h2b.png");
        } catch (IOException e) {
            ;
        }
        String frameTitle = (String) args.get("frame.title");
        if (frameTitle == null) frameTitle = "Kenshira Desktop";
        String frameLabel = (String) args.get("frame.label");
        if (frameLabel == null) frameLabel = "Kenshira Desktop";
        String frameButton = (String) args.get("frame.button");
        if (frameButton == null) frameButton = "Abrir navegador";
        frame = new Frame(frameTitle);
        frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent we) {
                if (closeButtonExits) {
                    stopAll();
                } else {
                    frame.dispose();
                }
            }
        });
        if (icon16 != null) {
            frame.setIconImage(icon16);
        }
        frame.setResizable(false);
        frame.setBackground(SystemColor.control);
        GridBagLayout layout = new GridBagLayout();
        frame.setLayout(layout);
        Panel mainPanel = new Panel(layout);
        GridBagConstraints constraintsPanel = new GridBagConstraints();
        constraintsPanel.gridx = 0;
        constraintsPanel.weightx = 1.0D;
        constraintsPanel.weighty = 1.0D;
        constraintsPanel.fill = GridBagConstraints.BOTH;
        constraintsPanel.insets = new Insets(0, 10, 0, 10);
        constraintsPanel.gridy = 0;
        GridBagConstraints constraintsButton = new GridBagConstraints();
        constraintsButton.gridx = 0;
        constraintsButton.gridwidth = 2;
        constraintsButton.insets = new Insets(10, 0, 0, 0);
        constraintsButton.gridy = 1;
        constraintsButton.anchor = GridBagConstraints.EAST;
        GridBagConstraints constraintsTextField = new GridBagConstraints();
        constraintsTextField.fill = GridBagConstraints.HORIZONTAL;
        constraintsTextField.gridy = 0;
        constraintsTextField.weightx = 1.0;
        constraintsTextField.insets = new Insets(0, 5, 0, 0);
        constraintsTextField.gridx = 1;
        GridBagConstraints constraintsLabel = new GridBagConstraints();
        constraintsLabel.gridx = 0;
        constraintsLabel.gridy = 0;
        Label label = new Label(frameLabel, Label.LEFT);
        label.setFont(font);
        mainPanel.add(label, constraintsLabel);
        text = new TextField();
        text.setEditable(false);
        text.setFont(font);
        if (firstTime) {
            text.setText("Cargando...");
        } else {
            text.setText(kenshiraUrl);
        }
        if (isWindows) {
            text.setFocusable(false);
        }
        mainPanel.add(text, constraintsTextField);
        startBrowser = new Button(frameButton);
        startBrowser.setFocusable(false);
        startBrowser.setActionCommand("startBrowser");
        startBrowser.addActionListener(this);
        startBrowser.setFont(font);
        if (firstTime) {
            startBrowser.setVisible(false);
        }
        mainPanel.add(startBrowser, constraintsButton);
        frame.add(mainPanel, constraintsPanel);
        int width = 300, height = 120;
        frame.setSize(width, height);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((screenSize.width - width) / 2, (screenSize.height - height) / 2);
        try {
            frame.setVisible(true);
        } catch (Throwable t) {
        }
    }

    private void startBrowser() {
        openURL(kenshiraUrl);
    }

    public void openURL(String url) {
        if (browserLauncher != null) {
            browserLauncher.openURLinBrowser(url);
        } else {
            try {
                integration.BrowserLauncher.openURL(url);
            } catch (IOException e) {
                Logger.logDirectMessage(Logger.MIN, "Kenshira", "Failed to start a browser to open the url " + url, e);
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if ("startBrowser".equals(command)) {
            startBrowser();
        } else if ("status".equals(command)) {
            showWindow(false, false);
        } else if ("exit".equals(command)) {
            stopAll();
        } else if (startBrowser == e.getSource()) {
            startBrowser();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            startBrowser();
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    private static String getStringSetting(String name, String defaultValue) {
        String s = System.getProperty(name);
        return s == null ? defaultValue : s;
    }

    public Map getArgs() {
        return args;
    }
}
