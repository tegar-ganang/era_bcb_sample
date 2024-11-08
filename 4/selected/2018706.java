package eu.irreality.age.swing.applet;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import javax.swing.JApplet;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import eu.irreality.age.ColoredSwingClient;
import eu.irreality.age.CommonClientUtilities;
import eu.irreality.age.FiltroFicheroEstado;
import eu.irreality.age.FiltroFicheroLog;
import eu.irreality.age.FiltroFicheroMundo;
import eu.irreality.age.GameEngineThread;
import eu.irreality.age.InputOutputClient;
import eu.irreality.age.ObjectCode;
import eu.irreality.age.SwingAetheriaGameLoader;
import eu.irreality.age.SwingAetheriaGameLoaderInterface;
import eu.irreality.age.World;
import eu.irreality.age.debug.Debug;
import eu.irreality.age.filemanagement.Paths;
import eu.irreality.age.filemanagement.WorldLoader;
import eu.irreality.age.i18n.UIMessages;
import eu.irreality.age.swing.CommonSwingFunctions;
import eu.irreality.age.swing.SwingMenuAetheria;
import eu.irreality.age.swing.sdi.NewFromFileListener;
import eu.irreality.age.swing.sdi.SwingSDIInterface;
import eu.irreality.age.windowing.AGEClientWindow;
import eu.irreality.age.windowing.UpdatingRun;

public class SwingSDIApplet extends JApplet implements AGEClientWindow {

    private World mundo;

    private boolean fullScreenMode;

    private InputOutputClient io;

    private JPanel mainPanel;

    private Vector gameLog;

    private Thread loaderThread = null;

    private GameEngineThread maquinaEstados;

    private String moduledir;

    private boolean usarLog;

    private InputStream logStream;

    private String stateFile;

    private Object mundoSemaphore = new Object();

    public void setMainPanel(JPanel panel) {
        if (mainPanel != null) getContentPane().remove(mainPanel);
        mainPanel = panel;
        getContentPane().add(panel);
    }

    public void write(String s) {
        io.write(s);
    }

    public InputOutputClient getIO() {
        return io;
    }

    public String getVersion() {
        return "Swing Applet AGE client, v0.1";
    }

    class LoaderThread extends Thread {

        public void run() {
            gameLog = new Vector();
            Debug.println("1");
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        getContentPane().removeAll();
                        mainPanel = new JPanel();
                        setMainPanel(mainPanel);
                        io = new ColoredSwingClient(SwingSDIApplet.this, gameLog);
                        CommonSwingFunctions.writeIntroductoryInfo(SwingSDIApplet.this);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("2");
            String worldName;
            World theWorld = null;
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
            System.out.println("3");
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
            org.w3c.dom.Document d = null;
            try {
                d = theWorld.getXMLRepresentation();
                System.out.println("D=null?" + (d == null));
            } catch (javax.xml.parsers.ParserConfigurationException exc) {
                System.out.println(exc);
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
                    logStream.mark(100000);
                    theWorld.prepareLog(logStream);
                    logStream.reset();
                    theWorld.setRandomNumberSeed(logStream);
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
            mundo = theWorld;
            synchronized (mundoSemaphore) {
                mundoSemaphore.notifyAll();
            }
            maquinaEstados = new GameEngineThread(theWorld, SwingSDIApplet.this, false);
            maquinaEstados.start();
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        repaint();
                        updateNow();
                        setVisible(false);
                        setVisible(true);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (io instanceof ColoredSwingClient) ((ColoredSwingClient) io).refreshFocus();
        }
    }

    public void start() {
        setVisible(true);
        this.requestFocus();
        this.requestFocusInWindow();
        this.requestFocus();
        if (io != null && io instanceof ColoredSwingClient) ((ColoredSwingClient) io).refreshFocus();
    }

    private void stopGameSaveAndUnlink() {
        if (maquinaEstados != null) maquinaEstados.exitNow(); else saveAndFreeResources();
    }

    public void exitNow() {
        write(UIMessages.getInstance().getMessage("applet.exit") + "\n");
    }

    public void destroy() {
        this.exitNow();
    }

    public void saveAndFreeResources() {
        io.write(UIMessages.getInstance().getMessage("swing.bye") + "\n");
        if (this.getClient() instanceof ColoredSwingClient) {
            ((ColoredSwingClient) this.getClient()).uninitClientMenu(this);
            ((ColoredSwingClient) this.getClient()).exit();
        }
        if (maquinaEstados != null) maquinaEstados.uninitServerMenu(this);
        maquinaEstados = null;
        Runtime.getRuntime().gc();
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public void unlinkWorld() {
        mundo = null;
    }

    public InputOutputClient getClient() {
        return io;
    }

    public World getMundo() {
        return mundo;
    }

    public JMenuBar getTheJMenuBar() {
        return getJMenuBar();
    }

    public void setTheJMenuBar(JMenuBar jmb) {
        setJMenuBar(jmb);
    }

    public SwingSDIApplet() {
        this("AGE Applet");
    }

    public SwingSDIApplet(String title) {
        super();
        this.title = title;
        new SwingMenuAetheria(this).addToWindow();
        JMenu menuArchivo = getTheJMenuBar().getMenu(0);
        JMenuItem itemLoadLog = new JMenuItem(UIMessages.getInstance().getMessage("menu.load.log"));
        menuArchivo.add(itemLoadLog, 1);
        itemLoadLog.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                loadLogFromCookie();
            }
        });
        menuArchivo.add(new JSeparator(), 3);
        setSize(500, 400);
    }

    public void loadLogFromCookie() {
        String logAsString = CookieUtils.readCookie(SwingSDIApplet.this, "log");
        try {
            logStream = new ByteArrayInputStream(logAsString.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
            write(uee.toString());
        }
        usarLog = true;
        write("El contenido del log es:\n");
        write("[" + CookieUtils.readCookie(SwingSDIApplet.this, "log"));
        write("]\n");
        reinit();
    }

    public void startGame(final String moduledir, final boolean usarLog, final InputStream logStream, final String stateFile) {
        if (loaderThread != null) {
            stopGameSaveAndUnlink();
        }
        this.moduledir = moduledir;
        this.usarLog = usarLog;
        this.logStream = logStream;
        this.stateFile = stateFile;
        System.out.println("B");
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        if (moduledir.equalsIgnoreCase("")) {
            setTitle("Aetheria Game Engine. " + UIMessages.getInstance().getMessage("swing.default.title.module") + " (sin nombre)");
        } else {
            setTitle("Aetheria Game Engine. " + UIMessages.getInstance().getMessage("swing.default.title.module") + " " + moduledir);
        }
        setVisible(true);
        loaderThread = this.new LoaderThread();
        loaderThread.start();
    }

    public SwingSDIApplet(final String moduledir, final boolean usarLog, final InputStream logStream, final String stateFile) {
        this(moduledir);
        startGame(moduledir, usarLog, logStream, stateFile);
    }

    public void reinit() {
        if (loaderThread != null) {
            maquinaEstados.exitNow();
            Thread thr = new Thread() {

                public void run() {
                    loaderThread = SwingSDIApplet.this.new LoaderThread();
                    loaderThread.start();
                    try {
                        loaderThread.join();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            };
            thr.start();
        }
    }

    protected Runnable updateCode = new UpdatingRun(this);

    public void updateNow() {
        Thread c = new Thread(updateCode);
        c.setPriority(Thread.MAX_PRIORITY);
        c.start();
    }

    public void guardarLog() {
        write(UIMessages.getInstance().getMessage("applet.save.warning"));
        String logToString = "";
        for (int i = 0; i < gameLog.size(); i++) {
            logToString += gameLog.get(i);
            logToString += "\\n";
        }
        CookieUtils.eraseCookie(this, "log");
        CookieUtils.createCookie(this, "log", logToString, 100);
        write(UIMessages.getInstance().getMessage("applet.save.done") + "\n");
    }

    public void guardarEstado() {
        write(UIMessages.getInstance().getMessage("applet.state.warning") + "\n");
        guardarEstado();
    }

    public void init() {
        System.err.println(this.getClass().getResource("libinvoke.bsh"));
        SwingAetheriaGameLoaderInterface.loadFont();
        startGame(this.getParameter("worldUrl"), usarLog, logStream, stateFile);
    }

    private String title = "AGE Applet";

    public String getTitle() {
        return title;
    }

    public boolean isFullScreenMode() {
        return false;
    }

    public void setFullScreenMode(boolean b) {
        ;
    }

    public void setTitle(String s) {
        title = s;
    }

    public boolean supportsFullScreen() {
        return false;
    }

    public void repaint() {
        validate();
        super.repaint(100);
    }
}
