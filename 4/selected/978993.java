package raptor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import raptor.connector.Connector;
import raptor.connector.fics.FicsConnector;
import raptor.international.L10n;
import raptor.pref.PreferenceKeys;
import raptor.pref.RaptorPreferenceStore;
import raptor.service.ActionScriptService;
import raptor.service.AliasService;
import raptor.service.CheckUpdates;
import raptor.service.ChessBoardCacheService;
import raptor.service.ConnectorService;
import raptor.service.DictionaryService;
import raptor.service.EcoService;
import raptor.service.MemoService;
import raptor.service.ScriptService;
import raptor.service.SoundService;
import raptor.service.ThemeService;
import raptor.service.ThreadService;
import raptor.service.UCIEngineService;
import raptor.service.UserTagService;
import raptor.service.XboardEngineService;
import raptor.swt.ErrorDialog;
import raptor.swt.InputDialog;
import raptor.swt.RaptorCursorRegistry;
import raptor.swt.RaptorImageRegistry;
import raptor.util.BrowserUtils;
import raptor.util.FileUtils;
import raptor.util.OSUtils;
import raptor.util.RaptorLogger;
import raptor.util.RaptorRunnable;

/**
 * Raptor is a singleton representing the application. It contains methods to
 * get various pieces of the application (e.g. the
 * RaptorWindow,preferences,etc).
 * 
 * This classes main is the application main.
 */
public class Raptor implements PreferenceKeys {

    public static final File DEFAULT_HOME_DIR = new File("defaultHomeDir/");

    public static final String APP_HOME_DIR = ".raptor/";

    public static final File USER_RAPTOR_DIR = new File(System.getProperty("user.home") + "/" + APP_HOME_DIR);

    public static final String USER_RAPTOR_HOME_PATH = USER_RAPTOR_DIR.getAbsolutePath();

    public static final String ICONS_DIR = "resources/icons/";

    public static final String IMAGES_DIR = "resources/images/";

    public static final String RESOURCES_SCRIPTS = "resources/scripts";

    public static final String RESOURCES_DIR = "resources/";

    public static final String ENGINES_DIR = USER_RAPTOR_HOME_PATH + "/engines";

    private static Raptor instance;

    private static Display display;

    protected static L10n local;

    static {
        RaptorLogger.initializeLogger();
    }

    public static void createInstance() {
        instance = new Raptor();
        instance.init();
    }

    /**
	 * Returns the singleton raptor instance.
	 * 
	 * @return
	 */
    public static Raptor getInstance() {
        return instance;
    }

    /**
	 * The applications main method. Takes no arguments.
	 */
    public static void main(String args[]) {
        local = L10n.getInstance();
        Locale.setDefault(L10n.currentLocale);
        try {
            Display.setAppName("Raptor");
            display = new Display();
            createInstance();
            if (L10n.noSavedLocaleFile) L10n.updateLanguage(true);
            display.addListener(SWT.Close, new Listener() {

                public void handleEvent(Event event) {
                    getInstance().shutdown();
                }
            });
            instance.raptorWindow = new RaptorWindow();
            instance.raptorWindow.setBlockOnOpen(true);
            Connector[] connectors = ConnectorService.getInstance().getConnectors();
            for (final Connector connector : connectors) {
                ThreadService.getInstance().scheduleOneShot(750, new Runnable() {

                    public void run() {
                        boolean connected = connector.onAutoConnect();
                        if (!connected && connector instanceof FicsConnector && Raptor.getInstance().getPreferences().getBoolean(APP_IS_LAUNCHING_LOGIN_DIALOG)) {
                            ((FicsConnector) connector).showLoginDialog();
                        }
                    }
                });
            }
            display.timerExec(500, new Runnable() {

                public void run() {
                    if (!OSUtils.isLikelyOSX() && !getInstance().getPreferences().getBoolean("ready-to-update") && getInstance().getPreferences().getBoolean("app-update")) CheckUpdates.checkUpdates(); else if (getInstance().getPreferences().getBoolean("ready-to-update")) getInstance().getPreferences().setValue("ready-to-update", "false");
                    try {
                        if (getInstance().getPreferences().getBoolean(APP_IS_LAUNCHING_HOME_PAGE) && BrowserUtils.internalBrowserSupported()) {
                            BrowserUtils.openUrl(getInstance().getPreferences().getString(PreferenceKeys.APP_HOME_URL));
                        }
                        Raptor.getInstance().cursorRegistry.setDefaultCursor(Raptor.getInstance().getWindow().getShell().getCursor());
                        FileUtils.deleteDir(new File(USER_RAPTOR_HOME_PATH + "/imagecache"));
                    } catch (Throwable t) {
                        Raptor.getInstance().onError("Error initializing Raptor", t);
                    }
                }
            });
            instance.raptorWindow.open();
        } catch (Throwable t) {
            if (t instanceof SWTException) {
                instance.LOG.info("SWTException: (If this is a widget is disposed error just ignore it its nothing)", t);
            } else {
                instance.LOG.error("Error occured in main: (If this is a widget is disposed error just ignore it its nothing)", t);
            }
        } finally {
            if (instance != null) {
                instance.shutdown();
            }
        }
    }

    /**
	 * Don't make this static.
	 */
    private final RaptorLogger LOG = RaptorLogger.getLog(Raptor.class);

    protected RaptorImageRegistry imageRegistry = new RaptorImageRegistry(Display.getCurrent());

    protected FontRegistry fontRegistry = new FontRegistry(Display.getCurrent());

    protected ColorRegistry colorRegistry = new ColorRegistry(Display.getCurrent());

    protected RaptorCursorRegistry cursorRegistry = new RaptorCursorRegistry(Display.getCurrent());

    protected RaptorPreferenceStore preferences;

    protected RaptorWindow raptorWindow;

    protected Clipboard clipboard;

    protected boolean isShutdown = false;

    /**
	 * The list contains hash codes of already displayed to user 
	 * errors to avoid duplication and be less annoying.
	 */
    private List<Integer> errorsDisplayed = new ArrayList<Integer>();

    public Raptor() {
        clipboard = new Clipboard(display);
    }

    /**
	 * Displays an alert message centered in the RaptorWindow.
	 */
    public void alert(final String message) {
        if (!isDisposed()) {
            instance.raptorWindow.getShell().getDisplay().asyncExec(new RaptorRunnable() {

                @Override
                public void execute() {
                    MessageDialog.openInformation(Raptor.getInstance().getWindow().getShell(), local.getString("alert"), message);
                }
            });
        }
    }

    /**
	 * Displays a confirm message centered in the RaptorWindow. If the user
	 * presses yes true is returned, otherwise false.
	 */
    public boolean confirm(final String question) {
        if (!isDisposed()) {
            return MessageDialog.openConfirm(Raptor.getInstance().raptorWindow.getShell(), local.getString("confirm"), question);
        }
        return false;
    }

    public Clipboard getClipboard() {
        return clipboard;
    }

    /**
	 * Returns the color registry. All colors should be placed in this registry
	 * so they can be properly managed.
	 */
    public ColorRegistry getColorRegistry() {
        return colorRegistry;
    }

    /**
	 * Returns the cursor registry. All cursors should be placed in this
	 * registry so they can be properly managed.
	 */
    public RaptorCursorRegistry getCursorRegistry() {
        return cursorRegistry;
    }

    public Display getDisplay() {
        return display;
    }

    /**
	 * Returns the font registry. All fonts should be placed in this registry so
	 * they can be properly managed.
	 */
    public FontRegistry getFontRegistry() {
        return fontRegistry;
    }

    /**
	 * The name of the file in the resources/icons directory to load. Do not
	 * append the suffix. All files in this directory end in .png and this
	 * method handles that for you.
	 */
    public Image getIcon(String nameOfFileInIconsWithoutPng) {
        String iconSize = preferences.getString(PreferenceKeys.APP_ICON_SIZE);
        String fileName = ICONS_DIR + "/" + iconSize + "/" + nameOfFileInIconsWithoutPng + ".png";
        return getImage(fileName);
    }

    /**
	 * Returns the image with the specified relative path.
	 */
    public Image getImage(String fileName) {
        Image result = imageRegistry.get(fileName);
        if (result == null) {
            try {
                ImageData data = new ImageData(fileName);
                imageRegistry.put(fileName, result = new Image(Display.getCurrent(), data));
            } catch (RuntimeException e) {
                LOG.error("Error loading image " + fileName, e);
                throw e;
            }
        }
        return result;
    }

    /**
	 * Returns the image registry. All images should be registered in this
	 * registry.
	 * 
	 * @return
	 */
    public RaptorImageRegistry getImageRegistry() {
        return imageRegistry;
    }

    /**
	 * Returns the RaptorPreferenceStore used by the application. All
	 * preferences should be stored and loaded form here.
	 */
    public RaptorPreferenceStore getPreferences() {
        return preferences;
    }

    /**
	 * Returns the RaptorWindow (the main application window.
	 */
    public RaptorWindow getWindow() {
        return raptorWindow;
    }

    public boolean isDisposed() {
        return isShutdown || instance == null || instance.raptorWindow == null || instance.raptorWindow.getShell() != null && instance.raptorWindow.getShell().isDisposed();
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    /**
	 * Handles an error in a way the user is notified and can report an issue.
	 * If possible try and use a connectors on error if you have access to one,
	 * otherwise you can use this.
	 */
    public void onError(final String error) {
        onError(error, null);
    }

    /**
	 * Handles an error in a way the user is notified and can report an issue.
	 * If possible try and use a connectors on error if you have access to one,
	 * otherwise you can use this.
	 */
    public void onError(final String error, final Throwable throwable) {
        LOG.error(error, throwable);
        if (!isDisposed()) {
            if (errorsDisplayed.contains(error.hashCode())) return;
            errorsDisplayed.add(error.hashCode());
            instance.raptorWindow.getShell().getDisplay().asyncExec(new Runnable() {

                public void run() {
                    ErrorDialog dialog = new ErrorDialog(Raptor.getInstance().getWindow().getShell(), local.getString("rapErr") + error + "\n" + (throwable != null ? ExceptionUtils.getMessage(throwable) + "\n" + ExceptionUtils.getFullStackTrace(throwable) : ""));
                    dialog.open();
                }
            });
        }
    }

    /**
	 * Prompts a user for the answer to a question. The user enters text. The
	 * text the user entered is returned.
	 */
    public String promptForText(final String question) {
        if (!isDisposed()) {
            InputDialog dialog = new InputDialog(Raptor.getInstance().raptorWindow.getShell(), local.getString("entText"), question);
            return dialog.open();
        } else {
            return null;
        }
    }

    /**
	 * Prompts a user for the answer to a question. The user enters text. The
	 * text the user entered is returned.
	 * 
	 * @answer the initial text to place in the users answer.
	 */
    public String promptForText(final String question, String answer) {
        if (!isDisposed()) {
            InputDialog dialog = new InputDialog(Raptor.getInstance().raptorWindow.getShell(), local.getString("entText"), question);
            if (answer != null) {
                dialog.setInput(answer);
            }
            return dialog.open();
        } else {
            return null;
        }
    }

    public void setClipboard(Clipboard clipboard) {
        this.clipboard = clipboard;
    }

    /**
	 * Cleanly shuts down raptor. Please use this method instead of System.exit!
	 */
    public void shutdown() {
        shutdown(false);
    }

    public void shutdownWithoutExit(boolean isIgnoringPreferenceSaves) {
        if (isShutdown) {
            return;
        }
        isShutdown = true;
        try {
            if (instance.raptorWindow != null && !instance.raptorWindow.getShell().isDisposed()) {
                instance.raptorWindow.storeWindowPreferences();
            }
        } catch (Throwable t) {
            LOG.warn("Error in storeWindowPreferences", t);
        }
        try {
            ConnectorService.getInstance().dispose();
        } catch (Throwable t) {
            LOG.warn("Error shutting down ConnectorService", t);
        }
        if (!isIgnoringPreferenceSaves) {
            preferences.save();
        }
        clipboard.dispose();
        if (EcoService.serviceCreated) {
            try {
                EcoService.getInstance().dispose();
            } catch (Throwable t) {
                LOG.warn("Error shutting down EcoService", t);
            }
        }
        if (SoundService.serviceCreated) {
            try {
                SoundService.getInstance().dispose();
            } catch (Throwable t) {
                LOG.warn("Error shutting down SoundService", t);
            }
        }
        if (ChessBoardCacheService.serviceCreated) {
            try {
                ChessBoardCacheService.getInstance().dispose();
            } catch (Throwable t) {
                LOG.warn("Error shutting ChessBoardCacheService", t);
            }
        }
        if (ThemeService.serviceCreated) {
            try {
                ThemeService.getInstance().dispose();
            } catch (Throwable t) {
                LOG.warn("Error shutting ThemeService", t);
            }
        }
        if (UCIEngineService.serviceCreated) {
            try {
                UCIEngineService.getInstance().dispose();
            } catch (Throwable t) {
                LOG.warn("Error shutting UCIEngineService", t);
            }
        }
        if (XboardEngineService.serviceCreated) {
            try {
                XboardEngineService.getInstance().dispose();
            } catch (Throwable t) {
                LOG.warn("Error shutting UCIEngineService", t);
            }
        }
        try {
            ThreadService.getInstance().dispose();
        } catch (Throwable t) {
            LOG.warn("Error shutting down ThreadService", t);
        }
        if (ActionScriptService.serviceCreated) {
            try {
                ActionScriptService.getInstance().dispose();
            } catch (Throwable t) {
                LOG.warn("Error shutting down ActionService", t);
            }
        }
        if (ScriptService.serviceCreated) {
            try {
                ScriptService.getInstance().dispose();
            } catch (Throwable t) {
                LOG.warn("Error shutting down ScriptService", t);
            }
        }
        if (AliasService.serviceCreated) {
            try {
                AliasService.getInstance().dispose();
            } catch (Throwable t) {
                LOG.warn("Error shutting down AliasService", t);
            }
        }
        if (MemoService.serviceCreated) {
            try {
                MemoService.getInstance().dispose();
            } catch (Throwable t) {
                LOG.warn("Error shutting down MemoService", t);
            }
        }
        if (UserTagService.serviceCreated) {
            try {
                UserTagService.getInstance().dispose();
            } catch (Throwable t) {
                LOG.warn("Error shutting down UserTagService", t);
            }
        }
        if (DictionaryService.serviceCreated) {
            try {
                DictionaryService.getInstance().dispose();
            } catch (Throwable t) {
                LOG.warn("Error shutting down DictionaryService", t);
            }
        }
        try {
            if (raptorWindow != null && !raptorWindow.getShell().isDisposed()) {
                raptorWindow.close();
            }
        } catch (Throwable t) {
            LOG.warn("Error shutting down raptor window", t);
        }
        try {
            if (display != null && !display.isDisposed()) {
                display.dispose();
            }
        } catch (Throwable t) {
            LOG.warn("Error shutting down display", t);
        }
        if (!isIgnoringPreferenceSaves) {
            preferences.save();
        }
        LOG.info("Shutdown Raptor");
    }

    /**
	 * Cleanly shuts down raptor. Please use this method instead of System.exit!
	 */
    public void shutdown(boolean isIgnoringPreferenceSaves) {
        shutdownWithoutExit(isIgnoringPreferenceSaves);
        System.exit(0);
    }

    /**
	 * Initializes raptor.
	 */
    private void init() {
        preferences = new RaptorPreferenceStore();
        install();
        ThreadService.getInstance();
        ConnectorService.getInstance();
    }

    /**
	 * Installs raptor. Currently this places everything in the default home
	 * directory in the users raptor directory. This is so new releases will
	 * always take effect.
	 */
    private void install() {
        try {
            FileUtils.copyFiles(DEFAULT_HOME_DIR, USER_RAPTOR_DIR);
            if (!new File(preferences.getString(APP_PGN_FILE)).exists()) {
                new File(preferences.getString(APP_PGN_FILE)).createNewFile();
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
