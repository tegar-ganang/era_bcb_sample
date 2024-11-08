package edu.umich.soar.debugger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import sml.Agent;
import sml.ClientAnalyzedXML;
import edu.umich.soar.debugger.dialogs.SwtInputDialog;
import edu.umich.soar.debugger.doc.DebuggerCommands;
import edu.umich.soar.debugger.doc.Document;
import edu.umich.soar.debugger.doc.NameRegister;
import edu.umich.soar.debugger.doc.ScriptCommands;
import edu.umich.soar.debugger.doc.events.AgentFocusGenerator;
import edu.umich.soar.debugger.doc.events.AgentFocusListener;
import edu.umich.soar.debugger.doc.events.SoarAgentEvent;
import edu.umich.soar.debugger.doc.events.SoarChangeListener;
import edu.umich.soar.debugger.doc.events.SoarConnectionEvent;
import edu.umich.soar.debugger.general.AppProperties;
import edu.umich.soar.debugger.jmx.SoarCommandLineMXBean;
import edu.umich.soar.debugger.manager.MainWindow;
import edu.umich.soar.debugger.menu.AgentMenu;
import edu.umich.soar.debugger.menu.CommandsMenu;
import edu.umich.soar.debugger.menu.DebugLevelMenu;
import edu.umich.soar.debugger.menu.FileMenu;
import edu.umich.soar.debugger.menu.HelpMenu;
import edu.umich.soar.debugger.menu.KernelMenu;
import edu.umich.soar.debugger.menu.LayoutMenu;
import edu.umich.soar.debugger.menu.PrintMenu;
import edu.umich.soar.debugger.modules.AbstractView;

/*******************************************************************************
 * 
 * The frame manages a top level window in the debugger and includes a menu bar.
 * 
 * There may be multiple MainFrames within a single debugger, sharing an
 * instance of Soar (a kernel). Soar can be running locally (within the
 * debugger) or remotely (inside another process--usually an environment).
 * 
 * If a user wishes to use multiple kernels at once they will need to start
 * multiple debugger processes (which they are free to do).
 * 
 * The most likely use of multiple MainFrames will be when there are multiple
 * agents with each agent having a separate window.
 * 
 * Each MainFrame is connected to a single "Document" which is shared by all
 * frames and which manages the Soar process itself.
 * 
 * The current design associates a default agent with a MainFrame window so it's
 * possible for children of that frame to inherit their choice of agent from the
 * MainFrame and have all windows working with a single agent. However, I would
 * that to just be a default and to support multiple windows within a single
 * MainFrame working with different agents if that proved useful (by having
 * module windows override that default choice).
 * 
 ******************************************************************************/
public class MainFrame {

    public static final FontData kDefaultFontData = new FontData("Courier New", 8, SWT.NORMAL);

    private static final String kNoAgent = "<no agent>";

    private static final String kWindowLayoutFile = "SoarDebuggerWindows.dlf";

    private Composite m_Parent = null;

    private MainWindow m_MainWindow = null;

    /** The menu bar */
    private Menu m_MenuBar = null;

    /** The menus in the menu bar */
    private FileMenu m_FileMenu = null;

    private KernelMenu m_KernelMenu = null;

    private AgentMenu m_AgentMenu = null;

    /**
     * The main document object -- represents the Soar process. There is only
     * one of these ever in the debugger.
     */
    private Document m_Document = null;

    /** Used to script the debugger itself */
    private ScriptCommands m_ScriptCommands = null;

    /**
     * Extended commands set that the user could type at the command line (might
     * fold this into scripts or vice versa one day--not sure)
     */
    private DebuggerCommands m_DebuggerCommands = null;

    /** Map of module names that are currently in use in this frame */
    private NameRegister m_NameMap = new NameRegister();

    /**
     * Each frame has a unique name within the debugger (for the life of one
     * running of the app)
     */
    private String m_Name;

    public Document getDocument() {
        return m_Document;
    }

    public Menu getMenuBar() {
        return m_MenuBar;
    }

    public MainWindow getMainWindow() {
        return m_MainWindow;
    }

    public Composite getWindow() {
        return m_MainWindow.getWindow();
    }

    /** The font to use for text output (e.g. output from Soar) */
    private Font m_TextFont = null;

    /**
     * Windows can register with the frame to learn when it switches focus to a
     * different agent
     */
    private AgentFocusGenerator m_AgentFocusGenerator = new AgentFocusGenerator();

    /** The agent this window is currently focused on -- can be null */
    private Agent m_AgentFocus;

    private SoarChangeListener m_SoarChangeListener = null;

    private boolean m_bClosing = false;

    /**
     * We'll keep a list of colors here that we wish to use elsewhere. When the
     * frame is disposed we should dispose them
     */
    public Color m_White;

    private ArrayList<Color> m_Colors = new ArrayList<Color>();

    public MainFrame(Composite parent, Document doc) {
        m_Parent = parent;
        m_Document = doc;
        String name = doc.addFrame(this);
        m_Name = name;
        m_ScriptCommands = new ScriptCommands(this, doc);
        m_DebuggerCommands = new DebuggerCommands(this, doc);
        m_White = new Color(getDisplay(), 255, 255, 255);
        m_Colors.add(m_White);
        m_MainWindow = new MainWindow(this, doc, parent);
        m_MenuBar = new Menu(getShell(), SWT.BAR);
        m_Parent.setLayout(new FillLayout(SWT.HORIZONTAL));
        m_SoarChangeListener = new SoarChangeListener() {

            public void soarConnectionChanged(SoarConnectionEvent e) {
                clearAgentFocus(false);
                updateMenus();
            }

            ;

            public void soarAgentListChanged(SoarAgentEvent e) {
                if (e.isAgentRemoved() && Document.isSameAgent(e.getAgent(), m_AgentFocus)) {
                    boolean destroyOnClose = m_Document.isCloseWindowWhenDestroyAgent();
                    if (destroyOnClose) {
                        getDisplay().asyncExec(new Runnable() {

                            public void run() {
                                close();
                            }
                        });
                        return;
                    }
                    clearAgentFocus(true);
                }
                updateMenus();
            }

            ;
        };
        getDocument().addSoarChangeListener(m_SoarChangeListener);
    }

    public static MainFrame createNewFrame(Display display, Document doc) {
        Shell shell = new Shell(display);
        MainFrame frame = new MainFrame(shell, doc);
        frame.initComponents(null);
        shell.open();
        return frame;
    }

    public String getName() {
        return m_Name;
    }

    public Shell getShell() {
        return m_Parent.getShell();
    }

    public Display getDisplay() {
        return m_Parent.getDisplay();
    }

    public static int ShowMessageBox(Shell shell, String title, String text, int style) {
        if (!shell.isVisible()) return SWT.CANCEL;
        if (title == null) title = "Error";
        if (text == null) text = "<No message>";
        MessageBox msg = new MessageBox(shell, style);
        msg.setText(title);
        msg.setMessage(text);
        int result = msg.open();
        return result;
    }

    public void ShowMessageBox(String title, String text) {
        ShowMessageBox(getShell(), title, text, 0);
    }

    public void ShowMessageBox(String text) {
        ShowMessageBox(getShell(), "Error", text, 0);
    }

    /***************************************************************************
     * 
     * Show a message box with a particular icon or set of buttons
     * 
     * @param title
     *            The title
     * @param text
     *            The content of the message box
     * @param style
     *            e.g. SWT.ICON_INFORMATION or SWT.OK | SWT.CANCEL
     * @return SWT.OK or SWT.CANCEL
     **************************************************************************/
    public int ShowMessageBox(String title, String text, int style) {
        return ShowMessageBox(getShell(), title, text, style);
    }

    public void setTextFont(FontData fontData) {
        Font oldFont = m_TextFont;
        this.setAppProperty("TextFont.Name", fontData.getName());
        this.setAppProperty("TextFont.Size", fontData.getHeight());
        this.setAppProperty("TextFont.Style", fontData.getStyle());
        m_TextFont = new Font(getDisplay(), fontData);
        getMainWindow().setTextFont(m_TextFont);
        if (oldFont != null) oldFont.dispose();
    }

    public FontData ShowFontDialog() {
        FontDialog dialog = new FontDialog(getShell());
        if (m_TextFont != null) dialog.setFontList(m_TextFont.getFontData());
        FontData data = dialog.open();
        return data;
    }

    /***************************************************************************
     * 
     * Close the window when the close box is clicked.
     * 
     * @param e
     *            Window closing event
     * 
     **************************************************************************/
    private void thisWindowClosing() {
        m_bClosing = true;
        Thread clearFocus = new Thread() {

            public void run() {
                clearAgentFocus(false);
            }
        };
        clearFocus.start();
        RecordWindowPositions();
        saveCurrentLayoutFile();
        try {
            this.getAppProperties().Save();
        } catch (Throwable t) {
            edu.umich.soar.debugger.general.Debug.println("Error saving properties file: " + t.getMessage());
        }
        this.getDocument().removeFrame(this);
        for (int i = 0; i < m_Colors.size(); i++) {
            Color color = (Color) m_Colors.get(i);
            color.dispose();
        }
        if (m_TextFont != null) m_TextFont.dispose();
        if (this.getDocument().getNumberFrames() == 0) {
            getDocument().close(true);
        }
    }

    public void close() {
        thisWindowClosing();
        this.getShell().dispose();
    }

    public String ShowInputDialog(String title, String prompt, String initialValue) {
        if (initialValue == null) initialValue = "";
        String name = SwtInputDialog.showDialog(this.getShell(), title, prompt, initialValue);
        return name;
    }

    public void clearAgentFocus(boolean canUnregisterEvents) {
        setAgentFocusInternal(null, canUnregisterEvents);
    }

    public void setAgentFocus(Agent agent) {
        setAgentFocusInternal(agent, true);
    }

    private static final String kBeanName = "SoarCommandLine:name=";

    private void registerBean() {
        assert m_AgentFocus != null;
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName mbeanName = new ObjectName(kBeanName + m_AgentFocus.GetAgentName());
            mbs.registerMBean(commandLineMXBean, mbeanName);
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        } catch (NotCompliantMBeanException e) {
            e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            e.printStackTrace();
        } catch (InstanceAlreadyExistsException e) {
            e.printStackTrace();
        }
    }

    private void unregisterBean() {
        assert m_AgentFocus != null;
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName mbeanName = new ObjectName(kBeanName + m_AgentFocus.GetAgentName());
            mbs.unregisterMBean(mbeanName);
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        } catch (InstanceNotFoundException e) {
            e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            e.printStackTrace();
        }
    }

    /** Switch to focusing on a new agent. */
    private void setAgentFocusInternal(Agent agent, boolean canUnregisterEvents) {
        if (m_AgentFocus == agent) return;
        if (m_AgentFocus != null) {
            if (m_Document.isAgentValid(m_AgentFocus) && canUnregisterEvents) m_AgentFocusGenerator.fireAgentLosingFocus(this, m_AgentFocus); else m_AgentFocusGenerator.fireAgentGone(this);
            unregisterBean();
        }
        m_AgentFocus = agent;
        if (m_AgentFocus != null) {
            registerBean();
            m_AgentFocusGenerator.fireAgentGettingFocus(this, m_AgentFocus);
        }
        if (this.getMainWindow().getWindow().isDisposed()) return;
        updateTitle();
        updateMenus();
    }

    /**
     * Let windows listener for when the frame changes focus to a different
     * agent
     */
    public synchronized void addAgentFocusListener(AgentFocusListener listener) {
        m_AgentFocusGenerator.addAgentFocusListener(listener);
    }

    public synchronized void removeAgentFocusListener(AgentFocusListener listener) {
        m_AgentFocusGenerator.removeAgentFocusListener(listener);
    }

    public Agent getAgentFocus() {
        return m_AgentFocus;
    }

    private void updateTitle() {
        if (this.m_bClosing) return;
        final String agentName = (m_AgentFocus == null ? kNoAgent : m_AgentFocus.GetAgentName());
        boolean remote = m_Document.isRemote();
        final String remoteString = remote ? "remote " : "";
        Display.getDefault().asyncExec(new Runnable() {

            public void run() {
                getShell().setText("Soar Debugger in Java - " + remoteString + agentName);
            }
        });
    }

    /**
     * Enable/disable menu items to reflect the current state of the Soar/the
     * debugger.
     */
    private void updateMenus() {
        m_KernelMenu.updateMenu();
        m_AgentMenu.updateMenu();
    }

    /***************************************************************************
     * 
     * Save the current layout so when we next launch the app next we'll go back
     * to this layout.
     * 
     * @return True if save file successfully
     **************************************************************************/
    public boolean saveCurrentLayoutFile() {
        File layoutFile = AppProperties.GetSettingsFilePath(kWindowLayoutFile);
        return this.saveLayoutFile(layoutFile.toString());
    }

    /***************************************************************************
     * 
     * Load a layout (window positions, types of windows etc.) from a file.
     * 
     * @param filename
     *            The file to load
     * @param showErrors
     *            If true display message box on error during load
     * @return True if load file successfully
     **************************************************************************/
    public boolean loadLayoutFile(String filename, boolean showErrors) {
        return getMainWindow().loadLayoutFromFile(filename, showErrors);
    }

    public boolean loadLayoutFileSpecial(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            return loadLayoutFile(filename, true);
        }
        if (file.isAbsolute()) return false;
        file = AppProperties.GetSettingsFilePath(filename);
        return file.exists() ? loadLayoutFile(file.toString(), true) : false;
    }

    public boolean saveLayoutFile(String filename) {
        return getMainWindow().saveLayoutToFile(filename);
    }

    public void useDefaultLayout() {
        getMainWindow().useDefaultLayout("default-layout.dlf");
    }

    public void useDefaultTextLayout() {
        getMainWindow().useDefaultLayout("default-text.dlf");
    }

    /***************************************************************************
     * 
     * Generates a unique name from a base name (e.g. from "trace" might create
     * "trace3"). The name is unique within the frame so we can use it to
     * cross-reference windows within a layout. It may not be unique within the
     * entire debugger.
     * 
     * @param baseName
     *            Cannot contain digits
     * @return The generated name (which has been registered as in use)
     **************************************************************************/
    public String generateName(String baseName, AbstractView view) {
        for (int i = 1; i < 200; i++) {
            String candidate = baseName + i;
            if (!m_NameMap.isNameInUse(candidate)) {
                m_NameMap.registerName(candidate, view);
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique name for basename " + baseName);
    }

    /** Look up the view based on its name * */
    public AbstractView getView(String viewName) {
        if (viewName == null) return null;
        AbstractView view = m_NameMap.getView(viewName);
        return view;
    }

    public void registerViewName(String name, AbstractView view) {
        m_NameMap.registerName(name, view);
    }

    public void unregisterViewName(String name) {
        m_NameMap.unregisterName(name);
    }

    public boolean isViewNameInUse(String name) {
        return m_NameMap.isNameInUse(name);
    }

    public void clearNameRegistry() {
        m_NameMap.clear();
    }

    public void loadSource() {
        m_FileMenu.loadSource();
    }

    public FileMenu getFileMenu() {
        return m_FileMenu;
    }

    public KernelMenu getKernelMenu() {
        return m_KernelMenu;
    }

    private static String getUserLayoutFilename(String version) {
        return "SoarDebuggerWindows" + version + ".dlf";
    }

    public boolean loadUserLayoutFile() {
        File layoutFile = AppProperties.GetSettingsFilePath(kWindowLayoutFile);
        for (int i = 0; !layoutFile.exists() && i < Document.kPrevVersions.length; i++) {
            layoutFile = AppProperties.GetSettingsFilePath(getUserLayoutFilename(Document.kPrevVersions[i]));
        }
        boolean loaded = layoutFile.exists() ? loadLayoutFile(layoutFile.toString(), true) : false;
        return loaded;
    }

    /***************************************************************************
     * 
     * Initializes the frame and all of its children.
     * 
     * Called by Application after the frame is constructed.
     * 
     **************************************************************************/
    public void initComponents(String alternateLayout) {
        m_FileMenu = FileMenu.createMenu(this, getDocument(), "&File");
        edu.umich.soar.debugger.menu.EditMenu.createMenu(this, getDocument(), "&Edit");
        PrintMenu.createMenu(this, getDocument(), "&Print");
        CommandsMenu.createMenu(this, getDocument(), "&Commands");
        DebugLevelMenu.createMenu(this, getDocument(), "&Debug Level");
        LayoutMenu.createMenu(this, getDocument(), "&Layout");
        m_AgentMenu = AgentMenu.createMenu(this, getDocument(), "&Agents");
        m_KernelMenu = KernelMenu.createMenu(this, getDocument(), "&Kernel");
        HelpMenu.createMenu(this, getDocument(), "&Help");
        getShell().setMenuBar(m_MenuBar);
        boolean loaded = (alternateLayout != null) ? loadLayoutFileSpecial(alternateLayout) : false;
        loaded = loaded || loadUserLayoutFile();
        install(new String[] { "default-layout.dlf", "default-text.dlf" });
        if (!loaded) {
            System.out.println("Failed to load the stored layout, so using default instead");
            useDefaultLayout();
        }
        getShell().addShellListener(new ShellAdapter() {

            public void shellClosed(ShellEvent e) {
                thisWindowClosing();
            }
        });
        boolean max = this.getAppBooleanProperty("Window.Max", true);
        if (max) {
            getShell().setMaximized(true);
        } else {
            int width = this.getAppIntegerProperty("Window.width");
            int height = this.getAppIntegerProperty("Window.height");
            int xPos = this.getAppIntegerProperty("Window.x");
            int yPos = this.getAppIntegerProperty("Window.y");
            boolean cascade = this.getAppBooleanProperty("Window.Cascade", true);
            cascade = true;
            if (cascade) {
                int offset = (m_Document.getNumberFrames() - 1) * 20;
                xPos += offset;
                yPos += offset;
            }
            if (width > 0 && width < Integer.MAX_VALUE && height > 0 && height < Integer.MAX_VALUE) getShell().setSize(width, height);
            if (xPos >= 0 && xPos < Integer.MAX_VALUE && yPos > 0 && yPos != Integer.MAX_VALUE) getShell().setLocation(xPos, yPos);
        }
        String fontName = this.getAppStringProperty("TextFont.Name");
        int fontSize = this.getAppIntegerProperty("TextFont.Size");
        int fontStyle = this.getAppIntegerProperty("TextFont.Style");
        if (fontName != null && fontName.length() > 0 && fontSize != Integer.MAX_VALUE && fontSize > 0 && fontStyle != Integer.MAX_VALUE && fontStyle >= 0) {
            setTextFont(new FontData(fontName, fontSize, fontStyle));
        } else {
            setTextFont(kDefaultFontData);
        }
        updateMenus();
        updateTitle();
    }

    private void install(String[] resources) {
        for (String resource : resources) {
            File resourceFile = AppProperties.GetSettingsFilePath(resource);
            if (resourceFile.exists()) continue;
            String jarpath = "/" + resource;
            InputStream is = this.getClass().getResourceAsStream(jarpath);
            if (is == null) {
                System.err.println("Failed to find " + jarpath + " in the JAR file");
                continue;
            }
            try {
                FileOutputStream os = new FileOutputStream(resourceFile);
                byte bytes[] = new byte[2048];
                int read;
                while (true) {
                    read = is.read(bytes);
                    if (read == -1) break;
                    os.write(bytes, 0, read);
                }
                is.close();
                os.close();
                System.out.println("Installed " + resourceFile + " onto the local disk from JAR file");
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to install " + resourceFile);
            }
        }
    }

    public Font getTextFont() {
        return m_TextFont;
    }

    public AppProperties getAppProperties() {
        return this.getDocument().getAppProperties();
    }

    public void setAppProperty(String property, String value) {
        this.getAppProperties().setAppProperty(property, value);
    }

    public void setAppProperty(String property, double value) {
        this.getAppProperties().setAppProperty(property, value);
    }

    public void setAppProperty(String property, int value) {
        this.getAppProperties().setAppProperty(property, value);
    }

    public void setAppProperty(String property, boolean value) {
        this.getAppProperties().setAppProperty(property, value);
    }

    public String getAppStringProperty(String property) {
        return this.getAppProperties().getAppStringProperty(property);
    }

    public boolean getAppBooleanProperty(String property, boolean defaultValue) {
        return this.getAppProperties().getAppBooleanProperty(property, defaultValue);
    }

    public double getAppDoubleProperty(String property) {
        return this.getAppProperties().getAppDoubleProperty(property);
    }

    public int getAppIntegerProperty(String property) {
        return this.getAppProperties().getAppIntegerProperty(property);
    }

    public int getWidth() {
        return m_MainWindow.getWidth();
    }

    public int getHeight() {
        return m_MainWindow.getHeight();
    }

    /**
     * The prime view is generally the trace window. More specifically it's the
     * first view that reports it can be a prime view
     */
    public AbstractView getPrimeView() {
        return m_MainWindow.getPrimeView();
    }

    public AbstractView[] getAllViews() {
        return m_MainWindow.getAllViews(false);
    }

    public AbstractView[] getAllOutputViews() {
        return m_MainWindow.getAllViews(true);
    }

    /**
     * Executes a command in the prime view (if there is one). If there is none,
     * just executes it directly and eats the output
     */
    public String executeCommandPrimeView(String commandLine, boolean echoCommand) {
        AbstractView prime = getPrimeView();
        String result = null;
        if (prime != null) {
            result = prime.executeAgentCommand(commandLine, echoCommand);
        } else {
            if (getAgentFocus() != null) result = m_Document.sendAgentCommand(getAgentFocus(), commandLine);
        }
        return result;
    }

    /***************************************************************************
     * 
     * Execute a command and return the XML form of the output.
     * 
     * This is used to evaluate a command which we wish to parse within the
     * debugger e.g. "set-library-location" (with no args) returns the path to
     * the library. We ask for the XML form and parse that, making us robust
     * against changes to the string form shown to the user.
     * 
     * NOTE: You should do explicit clean up of the 'response' object we can
     * check for memory leaks when the debugger exits. If we don't do this
     * explicitly, the memory will eventually get cleaned up, but only once the
     * gc runs...could be a while and may not happen before we exit (not
     * required by Java to do so) so it looks like a leak.
     * 
     * So the caller should follow this pattern: AnalyzeXML response = new
     * AnalyzeXML() ; bool ok = executeCommandXML(commandLine, response) ; ...
     * process response ... response.delete() ;
     * 
     * @param commandLine
     *            The command to execute (e.g. "watch")
     * @param response
     *            An XML object which is filled in by Soar
     * @return
     **************************************************************************/
    public boolean executeCommandXML(String commandLine, ClientAnalyzedXML response) {
        if (response == null) throw new IllegalArgumentException("Must allocate the response and pass it in.  The contents will be filled in during the call.");
        boolean result = false;
        if (getAgentFocus() != null) result = m_Document.sendAgentCommandXML(getAgentFocus(), commandLine, response);
        return result;
    }

    public String getCommandResult(String command, String resultParameter) {
        ClientAnalyzedXML response = new ClientAnalyzedXML();
        boolean ok = executeCommandXML(command, response);
        if (!ok) {
            response.delete();
            return "";
        }
        String result = response.GetArgString(resultParameter);
        response.delete();
        return result;
    }

    /**
     * Executes a command that affects the debugger itself, using some form of
     * scripting language we define
     */
    public void executeScriptCommand(AbstractView view, String commandLine, boolean echoCommand) {
        commandLine = m_ScriptCommands.replaceVariables(this, view, commandLine);
        m_ScriptCommands.executeCommand(commandLine, echoCommand);
    }

    /***************************************************************************
     * 
     * Debugger commands are like scripting commands, but they could potentially
     * be typed by the user at the command line.
     * 
     * @param commandLine
     * @return True if is a command we recognize
     **************************************************************************/
    public boolean isDebuggerCommand(String commandLine) {
        return m_DebuggerCommands.isCommand(commandLine);
    }

    public String getExpandedCommand(String commandLine) {
        return m_DebuggerCommands.getExpandedCommand(commandLine);
    }

    public String executeDebuggerCommand(AbstractView view, String commandLine, boolean echoCommand) {
        return m_DebuggerCommands.executeCommand(view, commandLine, echoCommand);
    }

    /***************************************************************************
     * 
     * Display the given text in this view (if possible).
     * 
     * This method is used to programmatically insert text that Soar doesn't
     * generate into the output window.
     * 
     **************************************************************************/
    public void displayTextInPrimeView(String text) {
        AbstractView prime = getPrimeView();
        if (prime == null) return;
        prime.displayText(text);
    }

    protected void RecordWindowPositions() {
        this.setAppProperty("Window.Max", getShell().getMaximized());
        int width = getShell().getSize().x;
        int height = getShell().getSize().y;
        this.setAppProperty("Window.width", width);
        this.setAppProperty("Window.height", height);
        int xPos = getShell().getLocation().x;
        int yPos = getShell().getLocation().y;
        this.setAppProperty("Window.x", xPos);
        this.setAppProperty("Window.y", yPos);
    }

    private String m_CommandLineResult;

    private final SoarCommandLineMXBean commandLineMXBean = new SoarCommandLineMXBean() {

        public String getName() {
            if (m_AgentFocus == null) return null;
            return m_AgentFocus.GetAgentName();
        }

        public String executeCommandLine(final String line) {
            m_CommandLineResult = null;
            getDisplay().syncExec(new Runnable() {

                public void run() {
                    m_CommandLineResult = executeCommandPrimeView(line, true);
                }
            });
            return m_CommandLineResult;
        }
    };
}
