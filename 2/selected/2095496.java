package org.snova.framework.shell.swing;

import java.awt.AWTException;
import java.awt.Cursor;
import java.awt.Desktop;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snova.framework.Framework;
import org.snova.framework.common.Constants;
import org.snova.framework.common.Version;
import org.snova.framework.config.DesktopFrameworkConfiguration;
import org.snova.framework.config.SimpleSocketAddress;
import org.snova.framework.plugin.DesktopPluginManager;
import org.snova.framework.plugin.DesktopPluginManager.InstalledPlugin;
import org.snova.framework.plugin.ProductReleaseDetail;
import org.snova.framework.plugin.ProductReleaseDetail.PluginReleaseDetail;
import org.snova.framework.trace.Trace;
import org.snova.framework.util.PreferenceHelper;
import org.snova.framework.util.SharedObjectHelper;

/**
 * 
 * @author wqy
 */
public class MainFrame extends javax.swing.JFrame {

    /** Creates new form MainFrame */
    public MainFrame() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            LookAndFeelInfo[] allFeels = UIManager.getInstalledLookAndFeels();
            for (LookAndFeelInfo feel : allFeels) {
                if (feel.getName().contains("Nimbus")) {
                    UIManager.setLookAndFeel(feel.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        initComponents();
        fm = new Framework(DesktopFrameworkConfiguration.getInstance(), DesktopPluginManager.getInstance(), new GUITrace());
        this.setTitle(Constants.PROJECT_NAME + " V" + Version.value);
        this.setIconImage(ImageUtil.FLAG.getImage());
        installPluginsUIPanel = new JPanel();
        availablePluginsUIPanel = new JPanel();
        installedPluginPanel.setViewportView(installPluginsUIPanel);
        availableUIPanel.setViewportView(availablePluginsUIPanel);
        installPluginsUIPanel.setLayout(new BoxLayout(installPluginsUIPanel, BoxLayout.PAGE_AXIS));
        availablePluginsUIPanel.setLayout(new BoxLayout(availablePluginsUIPanel, BoxLayout.PAGE_AXIS));
        jTabbedPane2.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (jTabbedPane2.getSelectedComponent() == installedPluginPanel) {
                    displayInstalledPlugins();
                } else {
                    displayAvailablePlugins();
                }
            }
        });
        SharedObjectHelper.getGlobalThreadPool().submit(new Runnable() {

            @Override
            public void run() {
                displayInstalledPlugins();
                displayAvailablePlugins();
                new SysTray(MainFrame.this);
                if (Boolean.TRUE.toString().equals(PreferenceHelper.getPreference(FrameworkConfigDialog.AUTO_CONNECT))) {
                    updateexecuteStatus();
                }
            }
        });
        end = System.currentTimeMillis();
        if (logger.isDebugEnabled()) {
            logger.debug("Cost " + (end - start) + "ms to launch GUI frame.");
        }
    }

    static URLConnection openRemoteDescriptionFile(String urlstr) {
        URL url = null;
        try {
            url = new URL(urlstr);
            URLConnection conn = url.openConnection();
            conn.connect();
            return conn;
        } catch (Exception e) {
            logger.error("Failed to retrive desc file:" + url, e);
            DesktopFrameworkConfiguration conf = DesktopFrameworkConfiguration.getInstance();
            SimpleSocketAddress localServAddr = conf.getLocalProxyServerAddress();
            Proxy proxy = new Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(localServAddr.host, localServAddr.port));
            URLConnection conn;
            try {
                conn = url.openConnection(proxy);
                conn.connect();
                return conn;
            } catch (IOException e1) {
                logger.error("Failed to retrive desc file:" + url, e1);
            }
        }
        return null;
    }

    static ProductReleaseDetail getProductUpdateDetail() throws Exception {
        DesktopFrameworkConfiguration conf = DesktopFrameworkConfiguration.getInstance();
        URLConnection updateConn = openRemoteDescriptionFile(conf.getPluginRepository());
        if (null == updateConn) {
            return null;
        }
        JAXBContext updateContext = JAXBContext.newInstance(ProductReleaseDetail.class);
        Unmarshaller updateUnmarshaller = updateContext.createUnmarshaller();
        return (ProductReleaseDetail) updateUnmarshaller.unmarshal(updateConn.getInputStream());
    }

    private void displayAvailablePlugins() {
        availablePluginsUIPanel.removeAll();
        try {
            ProductReleaseDetail release = getProductUpdateDetail();
            List<PluginReleaseDetail> plugins = release.plugins;
            for (PluginReleaseDetail plugin : plugins) {
                if (!pm.isPluginInstalled(plugin.name)) {
                    availablePluginsUIPanel.add(new AvalablePluginPanel(this, plugin));
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
        }
    }

    private void displayInstalledPlugins() {
        installPluginsUIPanel.removeAll();
        Collection<InstalledPlugin> installedPlugins = pm.getAllInstalledPlugins();
        for (InstalledPlugin plugin : installedPlugins) {
            installPluginsUIPanel.add(new InstalledPluginPanel(this, plugin));
        }
    }

    /**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        statusTextArea = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        installedPluginPanel = new javax.swing.JScrollPane();
        availableUIPanel = new javax.swing.JScrollPane();
        jPanel3 = new javax.swing.JPanel();
        javax.swing.JLabel appTitleLabel = new javax.swing.JLabel();
        javax.swing.JLabel appDescLabel = new javax.swing.JLabel();
        javax.swing.JLabel versionLabel = new javax.swing.JLabel();
        javax.swing.JLabel appVendorLabel = new javax.swing.JLabel();
        javax.swing.JLabel appVersionLabel = new javax.swing.JLabel();
        javax.swing.JLabel vendorLabel = new javax.swing.JLabel();
        javax.swing.JLabel homepageLabel = new javax.swing.JLabel();
        appHomepageLabel = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        executeButton = new javax.swing.JButton();
        exitButton = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        helpButton = new javax.swing.JButton();
        twitterLabel = new javax.swing.JLabel();
        statusLabel = new javax.swing.JLabel();
        setLocationByPlatform(true);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowIconified(java.awt.event.WindowEvent evt) {
                formWindowIconified(evt);
            }
        });
        statusTextArea.setBackground(new java.awt.Color(0, 0, 0));
        statusTextArea.setColumns(20);
        statusTextArea.setEditable(false);
        statusTextArea.setForeground(new java.awt.Color(0, 204, 0));
        statusTextArea.setRows(5);
        jScrollPane1.setViewportView(statusTextArea);
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE));
        jTabbedPane1.addTab("Status", ImageUtil.SPEAKER, jPanel1);
        installedPluginPanel.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jTabbedPane2.addTab("Installed", installedPluginPanel);
        jTabbedPane2.addTab("Available", availableUIPanel);
        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jTabbedPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE));
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jTabbedPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE));
        jTabbedPane1.addTab("Plugins", ImageUtil.PLUGIN, jPanel2);
        appTitleLabel.setFont(appTitleLabel.getFont().deriveFont(appTitleLabel.getFont().getStyle() | java.awt.Font.BOLD, appTitleLabel.getFont().getSize() + 4));
        appTitleLabel.setText("Snova GUI Launcher");
        appDescLabel.setText("<html>The GUI launcher of snova");
        versionLabel.setFont(versionLabel.getFont().deriveFont(versionLabel.getFont().getStyle() | java.awt.Font.BOLD));
        versionLabel.setText("Product Version:");
        appVendorLabel.setText("yinqiwen@gmail.com");
        appVersionLabel.setText(Version.value);
        vendorLabel.setFont(vendorLabel.getFont().deriveFont(vendorLabel.getFont().getStyle() | java.awt.Font.BOLD));
        vendorLabel.setText("Vendor:");
        homepageLabel.setFont(homepageLabel.getFont().deriveFont(homepageLabel.getFont().getStyle() | java.awt.Font.BOLD));
        homepageLabel.setText("Home:");
        appHomepageLabel.setText("<html><font color=\"Blue\"> <u>snova.googlecode.com</u></font></html>");
        appHomepageLabel.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                appHomepageLabelMouseClicked(evt);
            }
        });
        appHomepageLabel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {

            public void mouseMoved(java.awt.event.MouseEvent evt) {
                appHomepageLabelMouseMoved(evt);
            }
        });
        jLabel2.setIcon(ImageUtil.FLAG);
        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup().addContainerGap(160, Short.MAX_VALUE).addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addComponent(homepageLabel).addGap(1, 1, 1).addComponent(appHomepageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addGroup(jPanel3Layout.createSequentialGroup().addComponent(vendorLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(appVendorLabel)).addGroup(jPanel3Layout.createSequentialGroup().addComponent(versionLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(appVersionLabel)).addComponent(appDescLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(appTitleLabel)).addGap(36, 36, 36)).addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addGap(12, 12, 12).addComponent(jLabel2).addContainerGap(362, Short.MAX_VALUE))));
        jPanel3Layout.setVerticalGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addGap(49, 49, 49).addComponent(appTitleLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(appDescLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(18, 18, 18).addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(versionLabel).addComponent(appVersionLabel)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(vendorLabel).addComponent(appVendorLabel)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(homepageLabel).addComponent(appHomepageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap(75, Short.MAX_VALUE)).addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addGap(20, 20, 20).addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 191, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(38, Short.MAX_VALUE))));
        jTabbedPane1.addTab("About", ImageUtil.ABOUT, jPanel3);
        executeButton.setIcon(ImageUtil.START);
        executeButton.setText("Start");
        executeButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                executeButtonActionPerformed(evt);
            }
        });
        exitButton.setIcon(ImageUtil.EXIT);
        exitButton.setText("Exit");
        exitButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButtonActionPerformed(evt);
            }
        });
        jButton4.setIcon(ImageUtil.CONFIG);
        jButton4.setText("Config");
        jButton4.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        helpButton.setIcon(ImageUtil.HELP);
        helpButton.setText("Help");
        helpButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpButtonActionPerformed(evt);
            }
        });
        twitterLabel.setIcon(ImageUtil.TWITTER);
        twitterLabel.setText("<html><font color=\"Blue\"> <u>@yinqiwen</u></font></html>");
        twitterLabel.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                twitterLabelMouseClicked(evt);
            }
        });
        twitterLabel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {

            public void mouseMoved(java.awt.event.MouseEvent evt) {
                twitterLabelMouseMoved(evt);
            }
        });
        statusLabel.setText("Idle");
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 379, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE).addComponent(helpButton, javax.swing.GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE).addComponent(exitButton, javax.swing.GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE).addComponent(executeButton, javax.swing.GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE))).addGroup(layout.createSequentialGroup().addComponent(twitterLabel).addGap(18, 18, 18).addComponent(statusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 406, Short.MAX_VALUE))).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(jTabbedPane1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)).addGroup(layout.createSequentialGroup().addGap(55, 55, 55).addComponent(executeButton).addGap(40, 40, 40).addComponent(jButton4).addGap(39, 39, 39).addComponent(helpButton).addGap(41, 41, 41).addComponent(exitButton).addGap(27, 27, 27))).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(twitterLabel).addComponent(statusLabel)).addContainerGap()));
        pack();
    }

    public void exit() {
        System.exit(1);
    }

    private void exitButtonActionPerformed(java.awt.event.ActionEvent evt) {
        exit();
    }

    private void formWindowIconified(java.awt.event.WindowEvent evt) {
        setVisible(false);
    }

    private void helpButtonActionPerformed(java.awt.event.ActionEvent evt) {
        go2Webpage(Constants.OFFICIAL_SITE);
    }

    private void twitterLabelMouseClicked(java.awt.event.MouseEvent evt) {
        go2Webpage(Constants.OFFICIAL_TWITTER);
    }

    private void twitterLabelMouseMoved(java.awt.event.MouseEvent evt) {
        evt.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void appHomepageLabelMouseClicked(java.awt.event.MouseEvent evt) {
        go2Webpage(Constants.OFFICIAL_SITE);
    }

    private void appHomepageLabelMouseMoved(java.awt.event.MouseEvent evt) {
        evt.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {
        getConfigDialog().start();
    }

    private void updateexecuteStatus() {
        if (!hasStarted) {
            statusLabel.setText("Starting...");
            executeButton.setText("Stop");
            executeButton.setIcon(ImageUtil.STOP);
            hasStarted = true;
            SharedObjectHelper.getGlobalThreadPool().submit(new Runnable() {

                public void run() {
                    fm.start();
                }
            });
        } else {
            statusLabel.setText("Stopped");
            executeButton.setText("Start");
            executeButton.setIcon(ImageUtil.START);
            hasStarted = false;
            SharedObjectHelper.getGlobalThreadPool().submit(new Runnable() {

                public void run() {
                    fm.stop();
                }
            });
        }
    }

    private void executeButtonActionPerformed(java.awt.event.ActionEvent evt) {
        updateexecuteStatus();
    }

    public void go2Webpage(String site) {
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.browse(new URI(site));
        } catch (Exception e) {
            logger.error("Failed to go to web site!", e);
        }
    }

    /**
	 * @param args
	 *            the command line arguments
	 */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new MainFrame().setVisible(true);
            }
        });
    }

    private FrameworkConfigDialog getConfigDialog() {
        if (null == config) {
            config = new FrameworkConfigDialog(this, true);
        }
        return config;
    }

    private javax.swing.JLabel appHomepageLabel;

    private javax.swing.JScrollPane availableUIPanel;

    private javax.swing.JButton executeButton;

    private javax.swing.JButton exitButton;

    private javax.swing.JButton helpButton;

    private javax.swing.JScrollPane installedPluginPanel;

    private javax.swing.JButton jButton4;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JTabbedPane jTabbedPane1;

    private javax.swing.JTabbedPane jTabbedPane2;

    private javax.swing.JLabel statusLabel;

    private javax.swing.JTextArea statusTextArea;

    private javax.swing.JLabel twitterLabel;

    private JPanel installPluginsUIPanel;

    private JPanel availablePluginsUIPanel;

    private boolean hasStarted = false;

    private FrameworkConfigDialog config;

    private Framework fm;

    private DesktopPluginManager pm = DesktopPluginManager.getInstance();

    protected static Logger logger = LoggerFactory.getLogger(MainFrame.class);

    Trace trace = new GUITrace();

    class GUITrace implements Trace {

        public void info(final String string) {
            statusTextArea.append(string);
            statusTextArea.append(System.getProperty("line.separator"));
            statusLabel.setText(string);
        }

        public void error(final String string) {
            statusTextArea.append(string);
            statusTextArea.append(System.getProperty("line.separator"));
            statusLabel.setText(string);
        }

        public void notice(final String string) {
            statusTextArea.append(string);
            statusTextArea.append(System.getProperty("line.separator"));
        }
    }
}
