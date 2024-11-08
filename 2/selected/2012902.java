package org.snova.framework.shell.swing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.arch.util.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snova.framework.common.AppData;
import org.snova.framework.config.DesktopFrameworkConfiguration;
import org.snova.framework.config.SimpleSocketAddress;
import org.snova.framework.plugin.DesktopPluginManager;
import org.snova.framework.plugin.DesktopPluginManager.InstalledPlugin;
import org.snova.framework.plugin.PluginManager;
import org.snova.framework.plugin.ProductReleaseDetail.PluginReleaseDetail;
import org.snova.framework.util.SharedObjectHelper;

/**
 * 
 * @author wqy
 */
public class AvalablePluginPanel extends javax.swing.JPanel {

    static File downloadFile(Proxy proxy, URL url, File path, String fileName) throws IOException {
        URLConnection conn = null;
        if (null == proxy) {
            conn = url.openConnection();
        } else {
            conn = url.openConnection(proxy);
        }
        conn.connect();
        File destFile = new File(path, fileName);
        if (destFile.exists()) {
            return destFile;
        }
        FileOutputStream fos = new FileOutputStream(destFile);
        byte[] buffer = new byte[2048];
        try {
            while (true) {
                int len = conn.getInputStream().read(buffer);
                if (len < 0) {
                    break;
                } else {
                    fos.write(buffer, 0, len);
                }
            }
            fos.close();
        } catch (IOException e) {
            destFile.delete();
            throw e;
        }
        return destFile;
    }

    static File download(String url, File pluginhome, String filename) {
        try {
            return downloadFile(null, new URL(url), pluginhome, filename);
        } catch (Exception e) {
            logger.error("Failed to download file:" + url, e);
            DesktopFrameworkConfiguration conf = DesktopFrameworkConfiguration.getInstance();
            SimpleSocketAddress localServAddr = conf.getLocalProxyServerAddress();
            Proxy proxy = new Proxy(Type.HTTP, new InetSocketAddress(localServAddr.host, localServAddr.port));
            try {
                return downloadFile(proxy, new URL(url), pluginhome, filename);
            } catch (Exception e1) {
                logger.error("Failed to download file:" + url, e1);
                return null;
            }
        }
    }

    /** Creates new form PluginPanel */
    public AvalablePluginPanel(MainFrame owner, PluginReleaseDetail plugin) {
        this.plugin = plugin;
        this.owner = owner;
        initComponents();
        setButtonsVisible(false);
        allPluginPanels.add(this);
    }

    /**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        installButton = new javax.swing.JButton();
        iconLabel = new javax.swing.JLabel();
        descLabel = new javax.swing.JLabel();
        setBorder(javax.swing.BorderFactory.createTitledBorder((plugin.name + " " + plugin.version)));
        addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }
        });
        installButton.setIcon(ImageUtil.INSTALL);
        installButton.setText("Install");
        installButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                installButtonActionPerformed(evt);
            }
        });
        iconLabel.setIcon(ImageUtil.PLUGIN32);
        descLabel.setText(plugin.desc);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(iconLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(installButton, javax.swing.GroupLayout.Alignment.TRAILING).addComponent(descLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 270, Short.MAX_VALUE)).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(descLabel).addGap(18, 18, 18).addComponent(installButton)).addComponent(iconLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap()));
    }

    private void formMouseClicked(java.awt.event.MouseEvent evt) {
        setButtonsVisible(true);
        for (AvalablePluginPanel other : allPluginPanels) {
            if (other != this) {
                other.setButtonsVisible(false);
            }
        }
    }

    private void installButtonActionPerformed(java.awt.event.ActionEvent evt) {
        SharedObjectHelper.getGlobalThreadPool().submit(new Runnable() {

            public void run() {
                try {
                    installButton.setEnabled(false);
                    Future<Boolean> future = SharedObjectHelper.getGlobalThreadPool().submit(new Callable<Boolean>() {

                        public Boolean call() throws Exception {
                            try {
                                DesktopPluginManager pm = DesktopPluginManager.getInstance();
                                File pluginhome = AppData.getPluginsHome();
                                File downloadFile = download(plugin.url, pluginhome, "snova-" + plugin.name.toLowerCase() + "-" + plugin.version + ".zip");
                                if (null == downloadFile) {
                                    return Boolean.FALSE;
                                }
                                InstalledPlugin plugin = pm.loadPlugin(downloadFile);
                                pm.activatePlugin(plugin);
                                return Boolean.TRUE;
                            } catch (Exception ex) {
                                logger.error("Failed to install plugin:" + plugin, ex);
                            }
                            return Boolean.FALSE;
                        }
                    });
                    SwingHelper.showBusyButton(future, installButton, "Downloading...");
                    try {
                        if (!future.get()) {
                            descLabel.setText("Failed to install plugin:" + plugin.name);
                        } else {
                            descLabel.setText(plugin.name + " has been installed!");
                        }
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                } finally {
                }
            }
        });
    }

    private void setButtonsVisible(boolean isVisible) {
        installButton.setVisible(isVisible);
    }

    private javax.swing.JLabel descLabel;

    private javax.swing.JLabel iconLabel;

    private javax.swing.JButton installButton;

    private PluginReleaseDetail plugin;

    private MainFrame owner;

    protected static Logger logger = LoggerFactory.getLogger(AvalablePluginPanel.class);

    private static List<AvalablePluginPanel> allPluginPanels = new ArrayList<AvalablePluginPanel>();
}