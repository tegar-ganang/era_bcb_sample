package eu.irreality.age;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import eu.irreality.age.debug.Debug;
import eu.irreality.age.filemanagement.Paths;
import eu.irreality.age.filemanagement.WorldLoader;
import eu.irreality.age.i18n.UIMessages;
import eu.irreality.age.swing.CommonSwingFunctions;
import eu.irreality.age.swing.SwingMenuAetheria;
import eu.irreality.age.swing.config.AGEConfiguration;
import eu.irreality.age.util.VersionComparator;
import eu.irreality.age.windowing.AGEClientWindow;
import eu.irreality.age.windowing.UpdatingRun;
import java.io.*;

public class SwingAetheriaGameLoader extends JInternalFrame implements Informador, AGEClientWindow {

    /**
	* El contador de tiempo.
	*/
    protected static long timeCount;

    /**
	* Si salimos del juego.
	*/
    protected static boolean exitFlag;

    /**
	* Realizar� E/S general y se pasar� a todos los informadores.
	*/
    protected InputOutputClient io;

    /**
	* Log para guardar partida.
	*/
    protected Vector gameLog;

    /**
	* Mundo.
	*/
    protected World mundo;

    private JPanel mainPanel;

    private JFrame fullScreenFrame;

    private boolean fullScreenMode;

    private JMenuBar barraMenu;

    private GameEngineThread maquinaEstados;

    public World getMundo() {
        return mundo;
    }

    public String getVersion() {
        return "Swing-based MDI interface with colored text output, version 1.0";
    }

    protected SwingAetheriaGameLoader esto = this;

    protected Runnable updateCode = new UpdatingRun(this);

    public void updateNow() {
        Thread c = new Thread(updateCode);
        c.setPriority(Thread.MAX_PRIORITY);
        c.start();
    }

    private Object mundoSemaphore = new Object();

    public World waitForMundoToLoad() throws InterruptedException {
        synchronized (mundoSemaphore) {
            if (mundo != null) return mundo;
            while (mundo == null) {
                mundoSemaphore.wait();
            }
        }
        return mundo;
    }

    public void repaint() {
        super.repaint();
        if (fullScreenMode) fullScreenFrame.repaint();
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public void setMainPanel(JPanel p) {
        Container relevantContentPane;
        if (fullScreenMode) relevantContentPane = fullScreenFrame.getContentPane(); else relevantContentPane = getContentPane();
        if (mainPanel != null) relevantContentPane.remove(mainPanel);
        mainPanel = p;
        relevantContentPane.add(p);
    }

    public JMenuBar getTheJMenuBar() {
        if (barraMenu != null) return barraMenu; else {
            barraMenu = getJMenuBar();
            return barraMenu;
        }
    }

    public void setTheJMenuBar(JMenuBar jmb) {
        barraMenu = jmb;
        if (!fullScreenMode) setJMenuBar(jmb); else fullScreenFrame.setJMenuBar(jmb);
    }

    public InputOutputClient getClient() {
        return io;
    }

    public SwingAetheriaGameLoader(final String title, final JDesktopPane gui) {
        super(title, true, true, true);
        try {
            Image iconito = this.getToolkit().getImage(this.getClass().getClassLoader().getResource("images/intficon.gif"));
            setFrameIcon(new ImageIcon(iconito));
        } catch (Exception e) {
            e.printStackTrace();
        }
        new SwingMenuAetheria(this).addToWindow();
        addInternalFrameListener(new InternalFrameAdapter() {

            public void internalFrameClosing(InternalFrameEvent e) {
                saveWindowCoordinates();
                exitNow();
            }
        });
        gui.add(this);
        setSize(AGEConfiguration.getInstance().getIntegerProperty("mdiSubwindowWidth"), AGEConfiguration.getInstance().getIntegerProperty("mdiSubwindowHeight"));
        if (AGEConfiguration.getInstance().getBooleanProperty("mdiSubwindowMaximized")) this.getDesktopPane().getDesktopManager().maximizeFrame(this);
        setVisible(true);
        mainPanel = new JPanel();
        setMainPanel(mainPanel);
        io = new ColoredSwingClient(this, new Vector());
    }

    private Thread loaderThread = null;

    private String moduledir;

    private JDesktopPane gui;

    private boolean usarLog;

    private String logFile;

    private String stateFile;

    private boolean noSerCliente;

    class LoaderThread extends Thread {

        public void run() {
            gameLog = new Vector();
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        getContentPane().removeAll();
                        mainPanel = new JPanel();
                        setMainPanel(mainPanel);
                        io = new ColoredSwingClient(esto, gameLog);
                        if (logFile != null) {
                            if (((ColoredSwingClient) io).getSoundClient() instanceof AGESoundClient) {
                                AGESoundClient asc = (AGESoundClient) ((ColoredSwingClient) io).getSoundClient();
                                asc.deactivate();
                            }
                        }
                        CommonSwingFunctions.writeIntroductoryInfo(SwingAetheriaGameLoader.this);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            String worldName;
            World theWorld;
            if (moduledir == null || moduledir.length() == 0) moduledir = "aetherworld";
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        repaint();
                        updateNow();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            theWorld = WorldLoader.loadWorld(moduledir, gameLog, io, mundoSemaphore);
            if (theWorld == null || io.isDisconnected()) return;
            mundo = theWorld;
            final World theFinalWorld = theWorld;
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        updateNow();
                        if (theFinalWorld.getModuleName() != null && theFinalWorld.getModuleName().length() > 0) setTitle(theFinalWorld.getModuleName());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (new VersionComparator().compare(GameEngineThread.getVersionNumber(), theWorld.getRequiredAGEVersion()) < 0) {
                String mess = UIMessages.getInstance().getMessage("age.version.warning", "$curversion", GameEngineThread.getVersionNumber(), "$reqversion", theWorld.getRequiredAGEVersion(), "$world", theWorld.getModuleName());
                mess = mess + " " + UIMessages.getInstance().getMessage("age.download.url");
                JOptionPane.showMessageDialog(SwingAetheriaGameLoader.this, mess, UIMessages.getInstance().getMessage("age.version.warning.title"), JOptionPane.WARNING_MESSAGE);
            }
            if (Debug.DEBUG_OUTPUT) {
                org.w3c.dom.Document d = null;
                try {
                    d = theWorld.getXMLRepresentation();
                } catch (javax.xml.parsers.ParserConfigurationException exc) {
                    System.out.println(exc);
                }
                javax.xml.transform.stream.StreamResult sr = null;
                try {
                    sr = new javax.xml.transform.stream.StreamResult(new FileOutputStream("theworld.xml"));
                } catch (FileNotFoundException fnfe) {
                    System.out.println(fnfe);
                }
                try {
                    javax.xml.transform.Transformer tr = javax.xml.transform.TransformerFactory.newInstance().newTransformer();
                    tr.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "ISO-8859-1");
                    javax.xml.transform.Source s = new javax.xml.transform.dom.DOMSource(d);
                    tr.transform(s, sr);
                } catch (javax.xml.transform.TransformerConfigurationException tfe) {
                    System.out.println(tfe);
                } catch (javax.xml.transform.TransformerException te) {
                    System.out.println(te);
                }
            }
            if (stateFile != null) {
                try {
                    theWorld.loadState(stateFile);
                } catch (Exception exc) {
                    write(UIMessages.getInstance().getMessage("swing.cannot.read.state", "$file", stateFile));
                    write(exc.toString());
                    exc.printStackTrace();
                }
            }
            if (usarLog) {
                try {
                    System.out.println("RLTLTL");
                    System.out.println("Player list is " + theWorld.getPlayerList());
                    System.out.println("PECADORL");
                    theWorld.prepareLog(logFile);
                    theWorld.setRandomNumberSeed(logFile);
                } catch (Exception exc) {
                    write(UIMessages.getInstance().getMessage("swing.cannot.read.log", "$exc", exc.toString()));
                    exc.printStackTrace();
                    return;
                }
            } else {
                theWorld.setRandomNumberSeed();
            }
            gameLog.addElement(String.valueOf(theWorld.getRandomNumberSeed()));
            setVisible(true);
            timeCount = 0;
            mundo = theWorld;
            synchronized (mundoSemaphore) {
                mundoSemaphore.notifyAll();
            }
            maquinaEstados = new GameEngineThread(theWorld, esto, false);
            maquinaEstados.start();
            System.out.println("noSerCliente = " + noSerCliente);
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        repaint();
                        updateNow();
                        if (!fullScreenMode) {
                            setVisible(false);
                            setVisible(true);
                        } else {
                            fullScreenFrame.setVisible(false);
                            fullScreenFrame.setVisible(true);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (io instanceof ColoredSwingClient) ((ColoredSwingClient) io).refreshFocus();
        }
    }

    public boolean isFullScreenMode() {
        return fullScreenMode;
    }

    /**
	 * Saves this window's coordinates to the adequate properties file so next time a window from this class
	 * is constructed (i.e. next execution) it will have the same location and size.
	 */
    public void saveWindowCoordinates() {
        try {
            if (!this.isMaximum()) {
                AGEConfiguration.getInstance().setProperty("mdiSubwindowWidth", String.valueOf(this.getWidth()));
                AGEConfiguration.getInstance().setProperty("mdiSubwindowHeight", String.valueOf(this.getHeight()));
                AGEConfiguration.getInstance().setProperty("mdiSubwindowMaximized", "false");
                AGEConfiguration.getInstance().setProperty("mdiSubwindowLocationX", String.valueOf(this.getX()));
                AGEConfiguration.getInstance().setProperty("mdiSubwindowLocationY", String.valueOf(this.getY()));
            } else {
                AGEConfiguration.getInstance().setProperty("mdiSubwindowMaximized", "true");
            }
            ;
            AGEConfiguration.getInstance().storeProperties();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public SwingAetheriaGameLoader(final String moduledir, final JDesktopPane gui, final boolean usarLog, final String logFile, final String stateFile, final boolean noSerCliente) {
        super(moduledir, true, true, true, true);
        this.moduledir = moduledir;
        this.gui = gui;
        this.usarLog = usarLog;
        this.logFile = logFile;
        this.stateFile = stateFile;
        this.noSerCliente = noSerCliente;
        System.out.println("A");
        try {
            Image iconito = this.getToolkit().getImage(this.getClass().getClassLoader().getResource("images/intficon.gif"));
            setFrameIcon(new ImageIcon(iconito));
        } catch (Exception e) {
            e.printStackTrace();
        }
        new SwingMenuAetheria(this).addToWindow();
        addInternalFrameListener(new InternalFrameAdapter() {

            public void internalFrameClosing(InternalFrameEvent e) {
                System.out.println("Frame closed.");
                saveWindowCoordinates();
                exitNow();
            }
        });
        System.out.println("B");
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        setSize(500, 400);
        if (moduledir.equalsIgnoreCase("")) {
            setTitle("Aetheria Game Engine. " + UIMessages.getInstance().getMessage("swing.default.title.module") + " (sin nombre)");
        } else {
            setTitle("Aetheria Game Engine. " + UIMessages.getInstance().getMessage("swing.default.title.module") + " " + moduledir);
        }
        System.out.println("C");
        gui.add(this);
        setSize(AGEConfiguration.getInstance().getIntegerProperty("mdiSubwindowWidth"), AGEConfiguration.getInstance().getIntegerProperty("mdiSubwindowHeight"));
        if (AGEConfiguration.getInstance().getBooleanProperty("mdiSubwindowMaximized")) this.getDesktopPane().getDesktopManager().maximizeFrame(this);
        setVisible(true);
        final SwingAetheriaGameLoader esto = this;
        System.out.println("D");
        loaderThread = this.new LoaderThread();
        loaderThread.start();
    }

    public void reinit() {
        if (loaderThread != null) {
            final boolean fsm = fullScreenMode;
            setFullScreenMode(false);
            maquinaEstados.exitNow();
            Thread thr = new Thread() {

                public void run() {
                    loaderThread = SwingAetheriaGameLoader.this.new LoaderThread();
                    loaderThread.start();
                    try {
                        loaderThread.join();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                    setFullScreenMode(fsm);
                }
            };
            thr.start();
        }
    }

    public void setFullScreenMode(boolean onOrOff) {
        if (fullScreenFrame == null) fullScreenFrame = new JFrame();
        if (onOrOff) {
            System.out.println("Setting full-screen dedicated mode ON");
            if (fullScreenMode) return;
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice device = env.getDefaultScreenDevice();
            if (!device.isFullScreenSupported()) {
                JOptionPane.showMessageDialog(this, UIMessages.getInstance().getMessage("dialog.fullscreen.error"), UIMessages.getInstance().getMessage("dialog.fullscreen.error.not.supported"), JOptionPane.ERROR_MESSAGE);
                return;
            } else if (device.getDisplayMode() == null) {
                JOptionPane.showMessageDialog(this, UIMessages.getInstance().getMessage("dialog.fullscreen.error"), UIMessages.getInstance().getMessage("dialog.fullscreen.error.null.display"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            fullScreenMode = true;
            remove(getMainPanel());
            fullScreenFrame.getContentPane().add(getMainPanel());
            setJMenuBar(new JMenuBar());
            fullScreenFrame.setJMenuBar(barraMenu);
            if (!fullScreenFrame.isDisplayable()) fullScreenFrame.setUndecorated(true);
            fullScreenFrame.setResizable(false);
            DisplayMode dm = device.getDisplayMode();
            fullScreenFrame.setSize(new Dimension(dm.getWidth(), dm.getHeight()));
            fullScreenFrame.validate();
            fullScreenFrame.paintAll(fullScreenFrame.getGraphics());
            device.setFullScreenWindow(fullScreenFrame);
            fullScreenFrame.requestFocus();
            Runnable updateCode = new UpdatingRun(fullScreenFrame);
            Thread c = new Thread(updateCode);
            c.setPriority(Thread.MAX_PRIORITY);
            c.start();
            fullScreenFrame.setVisible(true);
            this.setVisible(false);
            fullScreenFrame.requestFocus();
            if (io instanceof ColoredSwingClient) ((ColoredSwingClient) io).refreshFocus();
        } else {
            System.out.println("Setting full-screen dedicated mode OFF");
            if (!fullScreenMode) return;
            fullScreenMode = false;
            fullScreenFrame.setJMenuBar(new JMenuBar());
            fullScreenFrame.remove(mainPanel);
            setMainPanel(mainPanel);
            setTheJMenuBar(barraMenu);
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] devices = env.getScreenDevices();
            GraphicsDevice device = env.getDefaultScreenDevice();
            device.setFullScreenWindow(null);
            DisplayMode dm = device.getDisplayMode();
            fullScreenFrame.setVisible(false);
            this.setVisible(true);
        }
    }

    public void exit() {
        exitFlag = true;
    }

    public void exitNow() {
        saveWindowCoordinates();
        stopGameSaveAndUnlink();
        this.dispose();
    }

    public void stopGameSaveAndUnlink() {
        if (maquinaEstados != null) maquinaEstados.exitNow(); else saveAndFreeResources();
    }

    public void saveAndFreeResources() {
        io.write(UIMessages.getInstance().getMessage("swing.saving") + "\n");
        try {
            CommonClientUtilities.guardarLog(new File("autosave.alf"), gameLog);
        } catch (Exception exc) {
            io.write(UIMessages.getInstance().getMessage("swing.cannot.save.log") + "\n");
        }
        io.write(UIMessages.getInstance().getMessage("swing.bye") + "\n");
        if (fullScreenMode) setFullScreenMode(false);
        if (this.getClient() instanceof ColoredSwingClient) {
            ((ColoredSwingClient) this.getClient()).uninitClientMenu(this);
            ((ColoredSwingClient) this.getClient()).exit();
        }
        if (fullScreenFrame != null) {
            fullScreenFrame.dispose();
            fullScreenFrame = null;
        }
        if (maquinaEstados != null) maquinaEstados.uninitServerMenu(this);
        maquinaEstados = null;
        Runtime.getRuntime().gc();
    }

    public void unlinkWorld() {
        mundo = null;
    }

    /**
	 * @deprecated Use {@link #write(String)} instead
	 */
    public void escribir(String s) {
        write(s);
    }

    public void write(String s) {
        io.write(s);
    }

    public void setIO(InputOutputClient es) {
        io = es;
    }

    public InputOutputClient getIO() {
        return io;
    }

    public void guardarLog() {
        File elFichero = null;
        JFileChooser selectorFichero = new JFileChooser(Paths.SAVE_PATH);
        selectorFichero.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FiltroFicheroLog filtro = new FiltroFicheroLog();
        selectorFichero.setFileFilter(filtro);
        int returnVal = selectorFichero.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            elFichero = selectorFichero.getSelectedFile();
            try {
                if (!elFichero.toString().toLowerCase().endsWith(".alf")) {
                    elFichero = new File(elFichero.toString() + ".alf");
                }
                CommonClientUtilities.guardarLog(elFichero, gameLog);
            } catch (Exception exc) {
                write(UIMessages.getInstance().getMessage("swing.cannot.save.log") + "\n");
                write(exc.toString());
            }
        }
    }

    public void guardarEstado() {
        File elFichero = null;
        JFileChooser selectorFichero = new JFileChooser(Paths.SAVE_PATH);
        selectorFichero.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FiltroFicheroEstado filtro = new FiltroFicheroEstado();
        selectorFichero.setFileFilter(filtro);
        int returnVal = selectorFichero.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            elFichero = selectorFichero.getSelectedFile();
            try {
                if (!elFichero.toString().toLowerCase().endsWith(".asf")) {
                    elFichero = new File(elFichero.toString() + ".asf");
                }
                CommonClientUtilities.guardarEstado(elFichero, mundo);
            } catch (Exception exc) {
                write(UIMessages.getInstance().getMessage("swing.cannot.save.state") + "\n");
                write(exc.toString());
            }
        }
    }

    public boolean supportsFullScreen() {
        return true;
    }
}
