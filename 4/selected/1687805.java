package gui;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileFilter;
import plugins.IPlugin;
import plugins.Plugins;

public class PluginWindow extends JDialog {

    private static final long serialVersionUID = -973197112528551092L;

    private JList lstPlugins;

    private PluginList pluginList = new PluginList();

    private JButton btnDelete;

    static class PluginList extends AbstractListModel {

        private static final long serialVersionUID = -6774963582368412332L;

        @Override
        public Object getElementAt(int arg0) {
            return Plugins.getPlugins()[arg0].getName() + " - " + Plugins.getPlugins()[arg0].getAuthor();
        }

        @Override
        public int getSize() {
            return Plugins.getPlugins().length;
        }

        public void updateElementList() {
            this.fireContentsChanged(this, 0, getSize());
        }
    }

    /**
	 * Create the dialog.
	 */
    public PluginWindow() {
        setTitle(Messages.getString("PluginWindow.PlugIns"));
        setIconImage(Toolkit.getDefaultToolkit().getImage(PluginWindow.class.getResource("/gui/64.png")));
        setBounds(100, 100, 450, 300);
        JPanel pnlRight = new JPanel();
        getContentPane().add(pnlRight, BorderLayout.EAST);
        pnlRight.setLayout(new BorderLayout(0, 0));
        JPanel panel_1 = new JPanel();
        pnlRight.add(panel_1, BorderLayout.NORTH);
        panel_1.setLayout(new BorderLayout(0, 0));
        JPanel panel = new JPanel();
        panel_1.add(panel);
        panel.setLayout(new BorderLayout(0, 0));
        btnDelete = new JButton(Messages.getString("PluginWindow.Delete"));
        btnDelete.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                deletePlugin();
            }
        });
        panel.add(btnDelete, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        lstPlugins = new JList();
        lstPlugins.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scrollPane.setViewportView(lstPlugins);
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        JMenu mnPlugins = new JMenu(Messages.getString("PluginWindow.PlugIns"));
        menuBar.add(mnPlugins);
        JMenuItem mntmInstall = new JMenuItem(Messages.getString("PluginWindow.Install"));
        mntmInstall.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                installPlugin();
            }
        });
        mnPlugins.add(mntmInstall);
        loadPluginList();
        this.setLocationRelativeTo(null);
    }

    private void loadPluginList() {
        lstPlugins.setModel(pluginList);
        pluginList.updateElementList();
        boolean isEmpty = (pluginList.getSize() == 0);
        btnDelete.setEnabled(!isEmpty);
        if (!isEmpty) lstPlugins.setSelectedIndex(0);
    }

    private IPlugin getSelectedPlugin() {
        if (lstPlugins.getSelectedIndex() != -1) return Plugins.getPlugins()[lstPlugins.getSelectedIndex()];
        return null;
    }

    protected void deletePlugin() {
        if (lstPlugins.getSelectedIndex() != -1) {
            if (JOptionPane.showConfirmDialog(this, Messages.getString("PluginWindow.ConfirmDelete").replace("$PLUGIN$", getSelectedPlugin().getName()), Messages.getString("PluginWindow.TrustSource"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                File filePlugin = Plugins.getFileFromPlugin(getSelectedPlugin());
                if (filePlugin != null) {
                    if (filePlugin.exists()) {
                        filePlugin.delete();
                        JOptionPane.showMessageDialog(this, Messages.getString("PluginWindow.RestartHanasu"));
                    } else JOptionPane.showMessageDialog(this, Messages.getString("PluginWindow.PluginCouldNotDeletedMsg").replace("$FILE$", filePlugin.getAbsolutePath()));
                } else {
                    JOptionPane.showMessageDialog(this, Messages.getString("PluginWindow.PlugInCouldNotDeleted"));
                }
            }
        }
    }

    protected void installPlugin() {
        JFileChooser filechooser = new JFileChooser();
        filechooser.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File arg0) {
                return arg0.getAbsolutePath().toLowerCase().endsWith(".jar");
            }

            @Override
            public String getDescription() {
                return Messages.getString("PluginWindow.HanasuPlugInFiles");
            }
        });
        if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            if (JOptionPane.showConfirmDialog(this, Messages.getString("PluginWindow.InstallFromTrustableSource"), Messages.getString("PluginWindow.TrustSource"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                if (Plugins.loadPlugin(filechooser.getSelectedFile().getAbsolutePath())) {
                    File file = filechooser.getSelectedFile();
                    try {
                        copy(file.getAbsolutePath(), Plugins.getPluginPath() + file.getName());
                        loadPluginList();
                    } catch (IOException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(this, Messages.getString("PluginWindow.CouldNotInstallPlugin") + e.getLocalizedMessage());
                    }
                }
            }
        }
    }

    public static void copy(String fromFileName, String toFileName) throws IOException {
        File fromFile = new File(fromFileName);
        File toFile = new File(toFileName);
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) to.write(buffer, 0, bytesRead);
        } finally {
            from.close();
            to.close();
        }
    }
}
