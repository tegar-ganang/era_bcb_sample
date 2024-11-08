package org.makagiga.commons;

import static java.awt.event.KeyEvent.*;
import static org.makagiga.commons.UI._;
import java.awt.Window;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

public abstract class Application {

    private static AppAction quitAction;

    private static Application _instance;

    private static boolean callOnShutDown = true;

    private static boolean initialized;

    private static boolean safeMode;

    private static FileLock fileLock;

    private static String lockPath;

    public static final boolean isInitialized() {
        return initialized;
    }

    /**
	 * Returns the lock file path or @c null if the @ref isLocked method was not called.
	 */
    public static String getLockPath() {
        return lockPath;
    }

    /**
	 * Returns the @i Quit action, which exits the application.
	 *
	 * @since 1.2
	 */
    public static synchronized AppAction getQuitAction() {
        if (quitAction == null) {
            quitAction = new AppAction(_("Quit"), "ui/quit", VK_Q, CTRL_MASK + SHIFT_MASK) {

                @Override
                public void onAction() {
                    quit();
                }
            };
        }
        return quitAction;
    }

    /**
	 * Returns @c true if application is locked.
	 *
	 * EXAMPLE:
	 * @code
	 * if (Application.isLocked()) {
	 *   System.out.println("Only one instance is allowed.");
	 *   System.exit(0);
	 * }
	 * System.out.println("Loading...");
	 * @endcode
	 *
	 * @throws IllegalStateException If @ref org.makagiga.commons.AppInfo.internalName property is not set
	 */
    public static boolean isLocked() {
        AppInfo.checkState();
        lockPath = FS.makeConfigPath(AppInfo.internalName + ".lock");
        try {
            fileLock = new FileOutputStream(lockPath).getChannel().tryLock();
        } catch (FileNotFoundException exception) {
            return false;
        } catch (IOException exception) {
            return false;
        }
        return (fileLock == null);
    }

    public static final boolean isSafeMode() {
        return safeMode;
    }

    public static final void setSafeMode(final boolean value) {
        safeMode = value;
    }

    /**
	 * Quits the application, if @ref onQuit return @c true.
	 * @see onQuit
	 * @throws IllegalStateException If application instance is null
	 */
    public static final void quit() {
        if (_instance == null) throw new IllegalStateException("Application instance is null");
        if (_instance.onQuit()) _instance.shutDown();
    }

    protected Application() {
        AppInfo.checkState();
        _instance = this;
        MLogger.info("core", AppInfo.getTitle());
        createArgs();
        Args.add("safe-mode", _("Safe mode"));
        Args.check();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
                toolTipManager.setDismissDelay(8000);
                createAndShowGUI();
                initialized = true;
                installShutDownHooks();
            }
        });
    }

    /**
	 * NOTE: The return value is not used since version 1.2,
	 * so you can return @c null.
	 */
    protected abstract Window createAndShowGUI();

    protected static void init(final String[] args) {
        OS.init();
        Args.init(args);
        safeMode = Args.isSet("safe-mode");
        Config.setDefaultComment(AppInfo.fileInfo.get());
    }

    /**
	 * @deprecated Since 1.2
	 */
    @Deprecated
    protected void createArgs() {
    }

    /**
	 * Invoked when the user quits the application.
	 * @return @c true - quit application, @c false - cancel quit
	 */
    protected boolean onQuit() {
        return true;
    }

    /**
	 * Invoked when the program exits normally,
	 * or when the @b JVM is terminated (e.g. system shut down).
	 */
    protected void onShutDown() {
    }

    private void installShutDownHooks() {
        if (!initialized) return;
        if (OS.isWindows()) {
            try {
                sun.misc.Signal.handle(new sun.misc.Signal("TERM"), new sun.misc.SignalHandler() {

                    public void handle(final sun.misc.Signal sig) {
                        if (callOnShutDown) shutDown();
                    }
                });
            } catch (NoClassDefFoundError error) {
                MLogger.exception(error);
            }
        } else {
            Runtime.getRuntime().addShutdownHook(new Thread() {

                public void run() {
                    if (callOnShutDown) shutDown();
                }
            });
        }
    }

    private synchronized void shutDown() {
        callOnShutDown = false;
        if (SwingUtilities.isEventDispatchThread()) {
            onShutDown();
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        onShutDown();
                    }
                });
            } catch (Exception exception) {
                MLogger.exception(exception);
            }
        }
        System.exit(0);
    }

    /**
	 * @since 1.2
	 */
    public static final class AboutAction extends AppAction {

        public AboutAction() {
            super(_("About"));
            setHTMLHelp(_("Displays the information about this application."));
        }

        @Override
        public void onAction() {
            MMainWindow.getMain().about();
        }
    }

    /**
	 * @since 1.2
	 */
    public static class ConfigureAction extends AppAction {

        public ConfigureAction() {
            super(_("Configure..."), "ui/configure");
        }
    }
}
