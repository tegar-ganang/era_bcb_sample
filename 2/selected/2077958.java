package net.suberic.pooka;

import net.suberic.pooka.gui.*;
import net.suberic.util.VariableBundle;
import net.suberic.pooka.resource.*;
import java.awt.*;
import javax.swing.*;
import javax.help.*;
import java.util.logging.*;

public class Pooka {

    /** The configuration for this instance of Pooka. */
    public static PookaManager sManager;

    /**
   * Runs Pooka.  Takes the following arguments:
   *
   * -nf 
   * --noOpenSavedFolders    don't open saved folders on startup.
   * 
   * -rc [FILE]
   * --rcfile [FILE]    use the given file as the pooka startup file.
   *
   * --http [URL]   runs with a configuration file loaded via http
   *
   * --help shows these options.
   */
    public static void main(String argv[]) {
        sManager = new PookaManager();
        sStartupManager = new StartupManager(sManager);
        sStartupManager.runPooka(argv);
    }

    public static StartupManager sStartupManager = null;

    /**
   * Loads the initial resources for Pooka.  These are used during startup.
   */
    public static void loadInitialResources() {
        try {
            ClassLoader cl = new Pooka().getClass().getClassLoader();
            java.net.URL url;
            if (cl == null) {
                url = ClassLoader.getSystemResource("net/suberic/pooka/Pookarc");
            } else {
                url = cl.getResource("net/suberic/pooka/Pookarc");
            }
            if (url == null) {
                url = new Pooka().getClass().getResource("/net/suberic/pooka/Pookarc");
            }
            java.io.InputStream is = url.openStream();
            VariableBundle resources = new net.suberic.util.VariableBundle(is, "net.suberic.pooka.Pooka");
            sManager.setResources(resources);
        } catch (Exception e) {
            System.err.println("caught exception loading system resources:  " + e);
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
   * Loads all the resources for Pooka.
   */
    public static void loadResources(boolean pUseLocalFiles, boolean pUseHttp) {
        if (sManager == null || sManager.getResources() == null) {
            System.err.println("Error starting up Pooka:  No system resource files found.");
            System.exit(-1);
        }
        try {
            net.suberic.util.VariableBundle pookaDefaultBundle = sManager.getResources();
            ResourceManager resourceManager = null;
            if (!pUseLocalFiles || pookaDefaultBundle.getProperty("Pooka.useLocalFiles", "true").equalsIgnoreCase("false")) {
                resourceManager = new DisklessResourceManager();
            } else {
                resourceManager = new FileResourceManager();
            }
            sManager.setResourceManager(resourceManager);
            if (sManager.getLocalrc() == null) {
                String localrc = new String(System.getProperty("user.home") + System.getProperty("file.separator") + ".pookarc");
                sManager.setLocalrc(localrc);
            }
            sManager.setResources(sManager.getResourceManager().createVariableBundle(sManager.getLocalrc(), pookaDefaultBundle));
        } catch (Exception e) {
            System.err.println("caught exception:  " + e);
            e.printStackTrace();
        }
        if (pUseHttp || sManager.getResources().getProperty("Pooka.httpConfig", "false").equalsIgnoreCase("true")) {
            net.suberic.pooka.gui.LoadHttpConfigPooka configPooka = new net.suberic.pooka.gui.LoadHttpConfigPooka();
            configPooka.start();
        }
    }

    /**
   * Exits Pooka.  Attempts to close all stores first.
   */
    public static void exitPooka(int exitValue, Object pSource) {
        final int fExitValue = exitValue;
        final Object fSource = pSource;
        Runnable runMe = new Runnable() {

            public void run() {
                sStartupManager.stopMainPookaWindow(fSource);
                System.exit(fExitValue);
            }
        };
        if (Pooka.getMainPanel() != null) Pooka.getMainPanel().setCursor(java.awt.Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Thread shutdownThread = new Thread(runMe);
        shutdownThread.start();
    }

    /**
   * Convenience method for getting Pooka configuration properties.  Calls
   * getResources().getProperty(propName, defVal).
   */
    public static String getProperty(String propName, String defVal) {
        return (getResources().getProperty(propName, defVal));
    }

    /**
   * Convenience method for getting Pooka configuration properties.  Calls
   * getResources().getProperty(propName).
   */
    public static String getProperty(String propName) {
        return (getResources().getProperty(propName));
    }

    /**
   * Convenience method for setting Pooka configuration properties.  Calls
   * getResources().setProperty(propName, propValue).
   */
    public static void setProperty(String propName, String propValue) {
        getResources().setProperty(propName, propValue);
    }

    /**
   * Returns the VariableBundle which provides all of the Pooka resources.
   */
    public static net.suberic.util.VariableBundle getResources() {
        return sManager.getResources();
    }

    /**
   * Sets the VariableBundle which provides all of the Pooka resources.
   */
    public static void setResources(net.suberic.util.VariableBundle pResources) {
        sManager.setResources(pResources);
    }

    /**
   * Returns whether or not debug is enabled for this Pooka instance.
   * 
   * @deprecated Use Logger.getLogger("Pooka.debug") instead.
   * 
   */
    public static boolean isDebug() {
        if (getResources().getProperty("Pooka.debug", "true").equals("true")) return true; else if (Logger.getLogger("Pooka.debug").isLoggable(Level.FINE)) return true; else return false;
    }

    /**
   * Returns the DateFormatter used by Pooka.
   */
    public static DateFormatter getDateFormatter() {
        return sManager.getDateFormatter();
    }

    /**
   * Returns the mailcap command map.  This is what is used to determine
   * which external programs are used to handle files of various MIME
   * types.
   */
    public static javax.activation.CommandMap getMailcap() {
        return sManager.getMailcap();
    }

    /**
   * Returns the Mime Types map.  This is used to map file extensions to
   * MIME types.
   */
    public static javax.activation.MimetypesFileTypeMap getMimeTypesMap() {
        return sManager.getMimeTypesMap();
    }

    /**
   * Gets the default mail Session for Pooka.
   */
    public static javax.mail.Session getDefaultSession() {
        return sManager.getDefaultSession();
    }

    /**
   * Gets the default authenticator for Pooka.
   */
    public static javax.mail.Authenticator getDefaultAuthenticator() {
        return sManager.getDefaultAuthenticator();
    }

    /**
   * Gets the Folder Tracker thread.  This is the thread that monitors the
   * individual folders and checks to make sure that they stay connected,
   * checks for new email, etc.
   */
    public static net.suberic.pooka.thread.FolderTracker getFolderTracker() {
        return sManager.getFolderTracker();
    }

    /**
   * Gets the Pooka Main Panel.  This is the root of the entire Pooka UI.
   */
    public static MainPanel getMainPanel() {
        return sManager.getMainPanel();
    }

    /**
   * The Store Manager.  This tracks all of the Mail Stores that Pooka knows
   * about.
   */
    public static StoreManager getStoreManager() {
        return sManager.getStoreManager();
    }

    /**
   * The Search Manager.  This manages the Search Terms that Pooka knows 
   * about, and also can be used to construct Search queries from sets
   * of properties.
   */
    public static SearchTermManager getSearchManager() {
        return sManager.getSearchManager();
    }

    /**
   * The UIFactory for Pooka.  This is used to create just about all of the
   * graphical UI components for Pooka.  Usually this is either an instance
   * of PookaDesktopPaneUIFactory or PookaPreviewPaneUIFactory, for the
   * Desktop and Preview UI styles, respectively.
   */
    public static PookaUIFactory getUIFactory() {
        return sManager.getUIFactory();
    }

    /**
   * The Search Thread.  This is the thread that folder searches are done
   * on.
   */
    public static net.suberic.util.thread.ActionThread getSearchThread() {
        return sManager.getSearchThread();
    }

    /**
   * The Address Book Manager keeps track of all of the configured Address 
   * Books.
   */
    public static AddressBookManager getAddressBookManager() {
        return sManager.getAddressBookManager();
    }

    /**
   * The ConnectionManager tracks the configured Network Connections.
   */
    public static NetworkConnectionManager getConnectionManager() {
        return sManager.getConnectionManager();
    }

    /**
   * The OutgoingMailManager tracks the various SMTP server that Pooka can
   * use to send mail.
   */
    public static OutgoingMailServerManager getOutgoingMailManager() {
        return sManager.getOutgoingMailManager();
    }

    /**
   * The EncryptionManager, not surprisingly, manages Pooka's encryption
   * facilities.
   */
    public static PookaEncryptionManager getCryptoManager() {
        return sManager.getCryptoManager();
    }

    /**
   * The HelpBroker is used to bring up the Pooka help system.
   */
    public static HelpBroker getHelpBroker() {
        return sManager.getHelpBroker();
    }

    /**
   * The ResourceManager controls access to resource files.
   */
    public static ResourceManager getResourceManager() {
        return sManager.getResourceManager();
    }

    /**
   * The SSL Trust Manager.
   */
    public static net.suberic.pooka.ssl.PookaTrustManager getTrustManager() {
        return sManager.getTrustManager();
    }

    /**
   * The SSL Trust Manager.
   */
    public static void setTrustManager(net.suberic.pooka.ssl.PookaTrustManager pTrustManager) {
        sManager.setTrustManager(pTrustManager);
    }

    /**
   * The Log Manager.
   */
    public static PookaLogManager getLogManager() {
        return sManager.getLogManager();
    }

    /**
   * The Pooka configuration manager itself.
   */
    public static PookaManager getPookaManager() {
        return sManager;
    }
}
