package org.openoffice.oosvn;

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.WindowAttribute;
import com.sun.star.awt.WindowClass;
import com.sun.star.awt.WindowDescriptor;
import com.sun.star.awt.XMessageBox;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XDispatchHelper;
import com.sun.star.frame.XModel;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.lang.XMultiServiceFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.UIManager;
import org.tmatesoft.svn.core.SVNLogEntry;

/**
 * OpenOffice plugin allowing user to backup and co-operate via SVN
 * (<a href="http://subversion.tigris.org/">SubVersion</a>,
 * <a href="http://en.wikipedia.org/wiki/Subversion_(software)">wiki</a>).
 *
 * You don't need any SVN client on you computer, because a
 * <a href="http://svnkit.com/">SVNKit</a> is included with this plugin.
 *
 * The basic part of the OoSVN plugin for OpenOffice.
 *
 * @author      Stepan Cenek
 * @version     %I%, %G%
 *
 * Copyright 2007 Stepan Cenek
 * This program is distributed under the terms
 * of the GNU General Public License.
 */
public final class OoSvn extends WeakBase implements com.sun.star.lang.XServiceInfo, com.sun.star.frame.XDispatchProvider, com.sun.star.lang.XInitialization, com.sun.star.frame.XDispatch {

    private XComponentContext m_xContext;

    private XToolkit m_xToolkit;

    private XMultiServiceFactory m_xProvider = null;

    private XMultiComponentFactory m_xServiceManager = null;

    private com.sun.star.frame.XFrame m_xFrame;

    private static final String m_implementationName = OoSvn.class.getName();

    private static final String[] m_serviceNames = { "com.sun.star.frame.ProtocolHandler" };

    protected volatile OoSvnSettings settings;

    private Svn svnWorker = new Svn();

    public OoSvn(XComponentContext context) {
        m_xContext = context;
        m_xServiceManager = context.getServiceManager();
        try {
            m_xProvider = createProvider();
        } catch (com.sun.star.uno.Exception ex) {
            error(ex);
        }
        settings = new OoSvnSettings();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            error(ex);
        }
    }

    ;

    public static XSingleComponentFactory __getComponentFactory(String sImplementationName) {
        XSingleComponentFactory xFactory = null;
        if (sImplementationName.equals(m_implementationName)) xFactory = Factory.createComponentFactory(OoSvn.class, m_serviceNames);
        return xFactory;
    }

    public static boolean __writeRegistryServiceInfo(XRegistryKey xRegistryKey) {
        return Factory.writeRegistryServiceInfo(m_implementationName, m_serviceNames, xRegistryKey);
    }

    public String getImplementationName() {
        return m_implementationName;
    }

    public boolean supportsService(String sService) {
        int len = m_serviceNames.length;
        for (int i = 0; i < len; i++) {
            if (sService.equals(m_serviceNames[i])) return true;
        }
        return false;
    }

    public String[] getSupportedServiceNames() {
        return m_serviceNames;
    }

    public com.sun.star.frame.XDispatch queryDispatch(com.sun.star.util.URL aURL, String sTargetFrameName, int iSearchFlags) {
        if (aURL.Protocol.compareTo("org.openoffice.oosvn.oosvn:") == 0) {
            if (aURL.Path.compareTo("svnUpdate") == 0) return this;
            if (aURL.Path.compareTo("svnCommit") == 0) return this;
            if (aURL.Path.compareTo("svnHistory") == 0) return this;
            if (aURL.Path.compareTo("settings") == 0) return this;
            if (aURL.Path.compareTo("about") == 0) return this;
        }
        return null;
    }

    public com.sun.star.frame.XDispatch[] queryDispatches(com.sun.star.frame.DispatchDescriptor[] seqDescriptors) {
        int nCount = seqDescriptors.length;
        com.sun.star.frame.XDispatch[] seqDispatcher = new com.sun.star.frame.XDispatch[seqDescriptors.length];
        for (int i = 0; i < nCount; ++i) {
            seqDispatcher[i] = queryDispatch(seqDescriptors[i].FeatureURL, seqDescriptors[i].FrameName, seqDescriptors[i].SearchFlags);
        }
        return seqDispatcher;
    }

    public void initialize(Object[] object) throws com.sun.star.uno.Exception {
        if (object.length > 0) {
            m_xFrame = (com.sun.star.frame.XFrame) UnoRuntime.queryInterface(com.sun.star.frame.XFrame.class, object[0]);
            m_xToolkit = (XToolkit) UnoRuntime.queryInterface(XToolkit.class, m_xContext.getServiceManager().createInstanceWithContext("com.sun.star.awt.Toolkit", m_xContext));
        }
    }

    public void dispatch(com.sun.star.util.URL aURL, com.sun.star.beans.PropertyValue[] aArguments) {
        if (aURL.Protocol.compareTo("org.openoffice.oosvn.oosvn:") == 0) {
            OoDocProperty docProperty = getProperty();
            settings.setCancelFired(false);
            if (aURL.Path.compareTo("svnUpdate") == 0) {
                try {
                    try {
                        settings = getSerializedSettings(docProperty);
                    } catch (NullPointerException ex) {
                        new DialogSettings(new javax.swing.JFrame(), true, settings).setVisible(true);
                        if (settings.getCancelFired()) return;
                        new DialogFileChooser(new javax.swing.JFrame(), true, settings).setVisible(true);
                        if (settings.getCancelFired()) return;
                    } catch (Exception ex) {
                        error("Error getting settings", ex);
                        return;
                    }
                    Object[][] logs = getLogs(settings);
                    long checkVersion = -1;
                    if (logs.length == 0) {
                        error("Sorry, the specified repository is empty.");
                        return;
                    }
                    new DialogSVNHistory(new javax.swing.JFrame(), true, settings, logs).setVisible(true);
                    if (settings.getCancelFired()) return;
                    File tempDir = new File(settings.getCheckoutPath() + svnWorker.tempDir);
                    if (tempDir.exists()) {
                        if (deleteFileDir(tempDir) == false) {
                            error("Error while deleting temporary checkout dir.");
                        }
                    }
                    svnWorker.checkout(settings);
                    File[] tempFiles = tempDir.listFiles();
                    File anyOdt = null;
                    File thisOdt = null;
                    for (int j = 0; j < tempFiles.length; j++) {
                        if (tempFiles[j].toString().endsWith(".odt")) anyOdt = tempFiles[j];
                        if (tempFiles[j].toString().equals(settings.getCheckoutDoc()) && settings.getCheckoutDoc() != null) thisOdt = tempFiles[j];
                    }
                    if (thisOdt != null) anyOdt = thisOdt;
                    String url;
                    if (settings.getCheckoutDoc() == null || !settings.getCheckoutDoc().equals(anyOdt.getName())) {
                        File newOdt = new File(settings.getCheckoutPath() + "/" + anyOdt.getName());
                        if (newOdt.exists()) newOdt.delete();
                        anyOdt.renameTo(newOdt);
                        File svnInfo = new File(settings.getCheckoutPath() + svnWorker.tempDir + "/.svn");
                        File newSvnInfo = new File(settings.getCheckoutPath() + "/.svn");
                        if (newSvnInfo.exists()) {
                            if (deleteFileDir(newSvnInfo) == false) {
                                error("Error while deleting temporary checkout dir.");
                            }
                        }
                        url = "file:///" + newOdt.getPath().replace("\\", "/");
                        svnInfo.renameTo(newSvnInfo);
                        anyOdt = newOdt;
                        loadDocumentFromUrl(url);
                        settings.setCheckoutDoc(anyOdt.getName());
                        try {
                            settings.serializeOut();
                        } catch (Exception ex) {
                            error("Error occured when re-newing settings.", ex);
                        }
                    } else {
                        try {
                            settings.serializeOut();
                        } catch (Exception ex) {
                            error("Error occured when re-newing settings.", ex);
                        }
                        url = "file:///" + anyOdt.getPath().replace("\\", "/");
                        XDispatchProvider xDispatchProvider = (XDispatchProvider) UnoRuntime.queryInterface(XDispatchProvider.class, m_xFrame);
                        PropertyValue property[] = new PropertyValue[1];
                        property[0] = new PropertyValue();
                        property[0].Name = "URL";
                        property[0].Value = url;
                        XMultiServiceFactory xMSF = createProvider();
                        Object objDispatchHelper = m_xServiceManager.createInstanceWithContext("com.sun.star.frame.DispatchHelper", m_xContext);
                        XDispatchHelper xDispatchHelper = (XDispatchHelper) UnoRuntime.queryInterface(XDispatchHelper.class, objDispatchHelper);
                        xDispatchHelper.executeDispatch(xDispatchProvider, ".uno:CompareDocuments", "", 0, property);
                    }
                } catch (Exception ex) {
                    error(ex);
                }
                return;
            }
            if (aURL.Path.compareTo("svnCommit") == 0) {
                try {
                    try {
                        settings = getSerializedSettings(docProperty);
                    } catch (Exception ex) {
                        error("Error getting settings", ex);
                        return;
                    }
                    Collection logs = svnWorker.getLogs(settings);
                    long headRevision = svnWorker.getHeadRevisionNumber(logs);
                    long committedRevision = -1;
                    new DialogCommitMessage(new javax.swing.JFrame(), true, settings).setVisible(true);
                    if (settings.getCancelFired()) return;
                    try {
                        settings.serializeOut();
                    } catch (Exception ex) {
                        error("Error occured when re-newing settings.", ex);
                    }
                    if (headRevision == 0) {
                        File impDir = new File(settings.getCheckoutPath() + svnWorker.tempDir + "/.import");
                        if (impDir.exists()) if (deleteFileDir(impDir) == false) {
                            error("Error while creating temporary import directory.");
                            return;
                        }
                        if (!impDir.mkdirs()) {
                            error("Error while creating temporary import directory.");
                            return;
                        }
                        File impFile = new File(settings.getCheckoutPath() + svnWorker.tempDir + "/.import/" + settings.getCheckoutDoc());
                        try {
                            FileChannel srcChannel = new FileInputStream(settings.getCheckoutPath() + "/" + settings.getCheckoutDoc()).getChannel();
                            FileChannel dstChannel = new FileOutputStream(impFile).getChannel();
                            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                            srcChannel.close();
                            dstChannel.close();
                        } catch (Exception ex) {
                            error("Error while importing file", ex);
                            return;
                        }
                        final String checkoutPath = settings.getCheckoutPath();
                        try {
                            settings.setCheckoutPath(impDir.toString());
                            committedRevision = svnWorker.importDirectory(settings, false).getNewRevision();
                        } catch (Exception ex) {
                            settings.setCheckoutPath(checkoutPath);
                            error("Error while importing file", ex);
                            return;
                        }
                        settings.setCheckoutPath(checkoutPath);
                        if (impDir.exists()) if (deleteFileDir(impDir) == false) error("Error while creating temporary import directory.");
                        try {
                            File newSvnInfo = new File(settings.getCheckoutPath() + "/.svn");
                            if (newSvnInfo.exists()) {
                                if (deleteFileDir(newSvnInfo) == false) {
                                    error("Error while deleting temporary checkout dir.");
                                }
                            }
                            File tempDir = new File(settings.getCheckoutPath() + svnWorker.tempDir);
                            if (tempDir.exists()) {
                                if (deleteFileDir(tempDir) == false) {
                                    error("Error while deleting temporary checkout dir.");
                                }
                            }
                            svnWorker.checkout(settings);
                            File svnInfo = new File(settings.getCheckoutPath() + svnWorker.tempDir + "/.svn");
                            svnInfo.renameTo(newSvnInfo);
                            if (deleteFileDir(tempDir) == false) {
                                error("Error while managing working copy");
                            }
                            try {
                                settings.serializeOut();
                            } catch (Exception ex) {
                                error("Error occured when re-newing settings.", ex);
                            }
                        } catch (Exception ex) {
                            error("Error while checking out a working copy for the location", ex);
                        }
                        showMessageBox("Import succesful", "Succesfully imported as revision no. " + committedRevision);
                        return;
                    } else {
                        try {
                            committedRevision = svnWorker.commit(settings, false).getNewRevision();
                        } catch (Exception ex) {
                            error("Error while committing changes. If the file is not working copy, you must use 'Checkout / Update' first.", ex);
                        }
                        if (committedRevision == -1) {
                            showMessageBox("Update - no changes", "No changes was made. Maybe you must just save the changes.");
                        } else {
                            showMessageBox("Commit succesfull", "Commited as revision no. " + committedRevision);
                        }
                    }
                } catch (Exception ex) {
                    error(ex);
                }
                return;
            }
            if (aURL.Path.compareTo("svnHistory") == 0) {
                try {
                    try {
                        settings = getSerializedSettings(docProperty);
                    } catch (Exception ex) {
                        error("Error getting settings", ex);
                        return;
                    }
                    Object[][] logs = getLogs(settings);
                    long checkVersion = settings.getCheckoutVersion();
                    settings.setCheckoutVersion(-99);
                    new DialogSVNHistory(new javax.swing.JFrame(), true, settings, logs).setVisible(true);
                    settings.setCheckoutVersion(checkVersion);
                } catch (Exception ex) {
                    error(ex);
                }
                return;
            }
            if (aURL.Path.compareTo("settings") == 0) {
                try {
                    settings = getSerializedSettings(docProperty);
                } catch (NoSerializedSettingsException ex) {
                    try {
                        settings.setCheckout(docProperty.getDocURL());
                    } catch (Exception exx) {
                    }
                } catch (Exception ex) {
                    error("Error getting settings; maybe you" + " need to save your document." + " If this is your first" + " checkout of the document, use Checkout" + " function directly.");
                    return;
                }
                new DialogSettings(new javax.swing.JFrame(), true, settings).setVisible(true);
                try {
                    settings.serializeOut();
                } catch (Exception ex) {
                    error("Error occured when saving settings.", ex);
                }
                return;
            }
            if (aURL.Path.compareTo("about") == 0) {
                showMessageBox("OoSvn :: About", "Autor: �t�p�n Cenek (stepan@geek.cz)");
                return;
            }
        }
    }

    /**
     * Gets SVN logs by calling svnWorker.getLogs and then making
     * some small
     *
     * @param settings OoSvnSettings object with all informations needed
     *                 for connection into repository
     *
     * @return two dimensional array containing the logs
     */
    private Object[][] getLogs(OoSvnSettings settings) throws Exception {
        Collection logs = svnWorker.getLogs(settings);
        Object[][] logsArray = new Object[logs.size() - 1][4];
        int i = 0;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Iterator entries = logs.iterator(); entries.hasNext(); ) {
            SVNLogEntry logEntry = (SVNLogEntry) entries.next();
            if (logEntry.getRevision() != 0) {
                logsArray[i][0] = logEntry.getRevision();
                logsArray[i][1] = logEntry.getAuthor();
                logsArray[i][2] = df.format(logEntry.getDate());
                logsArray[i][3] = logEntry.getMessage();
                i++;
            }
        }
        return logsArray;
    }

    /**
     * Method takes informations about actual OpenOffice document and then
     * returns them in new OoDocProperty object.
     *
     * @return      OoDocProperty object
     */
    private OoDocProperty getProperty() {
        String m_sURL = null;
        String m_sTitle = null;
        try {
            Object desktop = m_xServiceManager.createInstanceWithContext("com.sun.star.frame.Desktop", m_xContext);
            XDesktop xDesktop = (XDesktop) UnoRuntime.queryInterface(XDesktop.class, desktop);
            XComponent xComponent = xDesktop.getCurrentComponent();
            XTextDocument xTextDoc = (XTextDocument) UnoRuntime.queryInterface(XTextDocument.class, xComponent);
            PropertyValue[] lDescriptor = ((XModel) xTextDoc).getArgs();
            for (int i = 0; i < lDescriptor.length; i++) {
                try {
                    if (lDescriptor[i].Name.equals("URL")) m_sURL = AnyConverter.toString(lDescriptor[i].Value); else if (lDescriptor[i].Name.equals("Title")) m_sTitle = AnyConverter.toString(lDescriptor[i].Value);
                } catch (com.sun.star.lang.IllegalArgumentException e) {
                    error("Error during parsing document properties", e);
                }
            }
        } catch (Exception e) {
            error("Error during getting document properties", e);
        }
        OoDocProperty docProperty = new OoDocProperty(m_sTitle, m_sURL);
        return docProperty;
    }

    /**
     * Method search property identified by name
     *
     * @return      Object founded object
     */
    private Object getPropertyByName(String name) {
        Object foundedObject = null;
        try {
            Object desktop = m_xServiceManager.createInstanceWithContext("com.sun.star.frame.Desktop", m_xContext);
            XDesktop xDesktop = (XDesktop) UnoRuntime.queryInterface(XDesktop.class, desktop);
            XComponent xComponent = xDesktop.getCurrentComponent();
            XTextDocument xTextDoc = (XTextDocument) UnoRuntime.queryInterface(XTextDocument.class, xComponent);
            PropertyValue[] lDescriptor = ((XModel) xTextDoc).getArgs();
            for (int i = 0; i < lDescriptor.length; i++) {
                if (lDescriptor[i].Name.equals(name)) foundedObject = lDescriptor[i].Value;
            }
        } catch (Exception ex) {
            error("Error during getting document properties", ex);
        }
        return foundedObject;
    }

    /**
     * Get serialized settings
     *
     * @param docProperty properties of the current document
     *
     * @return object with settings (OoSvnSettings)
     */
    private OoSvnSettings getSerializedSettings(OoDocProperty docProperty) throws Exception {
        OoSvnSettings settingsTemp = new OoSvnSettings();
        OoSvnSettings settings = new OoSvnSettings();
        try {
            settingsTemp.setCheckout(docProperty.getDocURL());
        } catch (NullPointerException ex) {
            throw new NullPointerException("Please save the document.");
        } catch (UnsupportedEncodingException ex) {
            throw new UnsupportedEncodingException("Invalid URI.");
        }
        String uri = null;
        try {
            settings = new OoSvnSettings().serializeIn(settingsTemp);
            settings.setCancelFired(false);
        } catch (Exception ex) {
            throw new NoSerializedSettingsException("Maybe you didn't use Settings before first Update.");
        }
        return settings;
    }

    /**
     * Load document using OpenOffice callings
     *
     * @param url url (in String) of the documents which we need to be loaded
     */
    private void loadDocumentFromUrl(String url) throws com.sun.star.uno.Exception, BootstrapException, Exception {
        XDispatchProvider xDispatchProvider = (XDispatchProvider) UnoRuntime.queryInterface(XDispatchProvider.class, m_xFrame);
        PropertyValue property[] = new PropertyValue[1];
        property[0] = new PropertyValue();
        property[0].Name = "URL";
        property[0].Value = url;
        Object objDispatchHelper = m_xServiceManager.createInstanceWithContext("com.sun.star.frame.DispatchHelper", m_xContext);
        XDispatchHelper xDispatchHelper = (XDispatchHelper) UnoRuntime.queryInterface(XDispatchHelper.class, objDispatchHelper);
        xDispatchHelper.executeDispatch(xDispatchProvider, ".uno:Open", "", 0, property);
    }

    /**
     * Method for easy display message box with various informations.
     *
     * @param sTitle   a title of the message, will be displayed as caption
     *                 text of the message box
     * @param sMessage a text content of the message box
     */
    public void showMessageBox(String sTitle, String sMessage) {
        try {
            if (null != m_xFrame && null != m_xToolkit) {
                WindowDescriptor aDescriptor = new WindowDescriptor();
                aDescriptor.Type = WindowClass.MODALTOP;
                aDescriptor.WindowServiceName = new String("infobox");
                aDescriptor.ParentIndex = -1;
                aDescriptor.Parent = (XWindowPeer) UnoRuntime.queryInterface(XWindowPeer.class, m_xFrame.getContainerWindow());
                aDescriptor.Bounds = new Rectangle(0, 0, 300, 200);
                aDescriptor.WindowAttributes = WindowAttribute.BORDER | WindowAttribute.MOVEABLE | WindowAttribute.CLOSEABLE;
                XWindowPeer xPeer = m_xToolkit.createWindow(aDescriptor);
                if (xPeer != null) {
                    XMessageBox xMsgBox = (XMessageBox) UnoRuntime.queryInterface(XMessageBox.class, xPeer);
                    if (xMsgBox != null) {
                        xMsgBox.setCaptionText(sTitle);
                        xMsgBox.setMessageText(sMessage);
                        xMsgBox.execute();
                    }
                }
            }
        } catch (com.sun.star.uno.Exception e) {
        }
    }

    /**
     * Delete recursively all files and directories on selected path
     *
     * @param path to the root element which is supposed to be deleted
     *
     * @return true if the delete was success full; false if there was any problem
     *              and something wasn't deleted
     */
    private boolean deleteFileDir(File path) {
        String[] files = path.list();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                deleteFileDir(new File(path + "/" + files[i]));
            }
        }
        return path.delete();
    }

    public void addStatusListener(com.sun.star.frame.XStatusListener xControl, com.sun.star.util.URL aURL) {
    }

    public void removeStatusListener(com.sun.star.frame.XStatusListener xControl, com.sun.star.util.URL aURL) {
    }

    /** Create a default configuration provider
     */
    private XMultiServiceFactory createProvider() throws com.sun.star.uno.Exception {
        final String sProviderService = "com.sun.star.configuration.ConfigurationProvider";
        XMultiServiceFactory xProvider = (XMultiServiceFactory) UnoRuntime.queryInterface(XMultiServiceFactory.class, m_xServiceManager.createInstanceWithContext(sProviderService, m_xContext));
        return xProvider;
    }

    /**
     * Method for handling exceptions, because you don't have any console output
     * in OpenOffice. So exception is displayed as message box informing about
     * the error.
     *
     * @param e an exception catched somewhere in the program that will be
     *          displayed to user
     */
    private void error(Exception ex) {
        showMessageBox("Error", ex.getMessage());
    }

    /**
     * Method for handling exceptions, because you don't have any console output
     * in OpenOffice. So exception is displayed as message box informing about
     * the error.
     *
     * @param message custom text you need to show with the error to the user
     * @param e       an exception catched somewhere in the program that will be
     *                displayed to user
     */
    private void error(String message, Exception ex) {
        showMessageBox("Error", message + (ex != null ? ": " + ex.getMessage() : ""));
    }

    /**
     * Method for handling exceptions or errors, because you don't have any
     * console output in OpenOffice. The message will be displayed as message
     * box informing about the error.
     *
     * @param message custom text you need to show to the user
     */
    private void error(String message) {
        showMessageBox("Error", message);
    }
}
