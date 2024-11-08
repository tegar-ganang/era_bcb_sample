package vademecum.ui.dialogs;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Map;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.java.plugin.JpfException;
import org.java.plugin.PluginManager;
import org.java.plugin.PluginManager.PluginLocation;
import org.java.plugin.registry.ManifestProcessingException;
import org.java.plugin.standard.StandardPluginLocation;
import vademecum.Core;
import vademecum.core.model.PluginManagerTableModel;
import vademecum.ui.project.Expertice;

public class PluginManagerDialog extends JDialog implements ActionListener {

    private static Log log = LogFactory.getLog(PluginManagerDialog.class);

    private static PluginManagerDialog instance;

    private PluginManager manager;

    private PluginManagerTableModel dataModel;

    private PluginManagerDialog(Frame owner, PluginManager m) {
        super(owner);
        this.setLocationRelativeTo(owner);
        this.setTitle("Plugin Manager");
        manager = m;
        initDialog();
        this.setModal(true);
    }

    public static PluginManagerDialog getInstance(Frame owner, PluginManager m) {
        if (instance == null) instance = new PluginManagerDialog(owner, m);
        return instance;
    }

    private void initDialog() {
        GridBagLayout layout = new GridBagLayout();
        layout.columnWeights = new double[] { 1 };
        layout.rowWeights = new double[] { .75, .05 };
        this.setLayout(layout);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.BOTH;
        dataModel = new PluginManagerTableModel(manager.getRegistry());
        final JTable table = new JTable();
        table.setModel(dataModel);
        table.setShowHorizontalLines(false);
        JScrollPane scrollPane = new JScrollPane(table);
        layout.setConstraints(scrollPane, constraints);
        final JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("deaktivieren");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int[] selection = table.getSelectedRows();
                log.debug(selection.length + " selected elements");
                String[] ids = new String[selection.length];
                for (int i = 0; i < selection.length; i++) {
                    ids[i] = (String) dataModel.getValueAt(selection[i], PluginManagerTableModel.COL_PLUGIN_ID);
                    manager.deactivatePlugin(ids[i]);
                }
                Collection uids = manager.getRegistry().unregister(ids);
                log.debug("unregistered " + uids.toString());
                Vector<Expertice> expertices = Core.projectPanel.getExpertices();
                for (Expertice ex : expertices) {
                    ex.getDataNavigation().rebuildMenus();
                }
            }
        });
        popup.add(menuItem);
        menuItem = new JMenuItem("reload");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int[] selection = table.getSelectedRows();
                log.debug(selection.length + " selected elements");
                String[] ids = new String[selection.length];
                URL[] urls = new URL[selection.length];
                PluginLocation[] loc = new PluginLocation[selection.length];
                for (int i = 0; i < selection.length; i++) {
                    ids[i] = (String) dataModel.getValueAt(selection[i], PluginManagerTableModel.COL_PLUGIN_ID);
                    try {
                        urls[i] = manager.getPathResolver().getRegisteredContext(ids[i]);
                        File f = new File(urls[i].getFile().substring(5, urls[i].getFile().length() - 2));
                        log.debug(urls[i].getFile().substring(0, urls[i].getFile().length() - 2));
                        log.debug("reloading plugin url " + f.getName());
                        loc[i] = new StandardPluginLocation(f, "/plugin.xml");
                        log.debug("reloading " + loc[i]);
                    } catch (Exception e1) {
                        log.error(e1);
                    }
                }
                try {
                    log.debug("unregistering selected plugins...");
                    manager.getRegistry().unregister(ids);
                    log.debug("republishing selected plugins");
                    Map map = manager.publishPlugins(loc);
                    log.debug("published " + map);
                    Vector<Expertice> expertices = Core.projectPanel.getExpertices();
                    for (Expertice ex : expertices) {
                        log.debug("rebuilding menu for Expertise " + ex.getName());
                        ex.getDataNavigation().rebuildMenus();
                    }
                } catch (Exception e1) {
                    log.error(e1);
                    return;
                }
            }
        });
        popup.add(menuItem);
        table.add(popup);
        table.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() && table.getSelectedRow() >= 0) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() && table.getSelectedRow() >= 0) {
                }
            }
        });
        this.add(scrollPane);
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.NORTHEAST;
        JButton button = new JButton("Add a new Plugin...");
        layout.setConstraints(button, constraints);
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser();
                jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                jfc.addActionListener(PluginManagerDialog.this);
                jfc.showOpenDialog(PluginManagerDialog.this);
            }
        });
        this.add(button);
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.SOUTH;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        layout.setConstraints(buttonPanel, constraints);
        JButton buttonOk = new JButton("OK");
        buttonOk.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                PluginManagerDialog.this.setVisible(false);
            }
        });
        JButton buttonCancel = new JButton("Cancel");
        buttonCancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                PluginManagerDialog.this.setVisible(false);
            }
        });
        buttonPanel.add(buttonOk, JPanel.LEFT_ALIGNMENT);
        buttonPanel.add(buttonCancel, JPanel.RIGHT_ALIGNMENT);
        this.add(buttonPanel);
        this.setSize(450, 350);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
            int result = JOptionPane.showConfirmDialog(Core.frame, "Would you like to copy the plugin into the Vademecum plugin folder ?");
            boolean copy = false;
            switch(result) {
                case JOptionPane.CANCEL_OPTION:
                    return;
                case JOptionPane.YES_OPTION:
                    copy = true;
                    break;
            }
            JFileChooser jfc = (JFileChooser) e.getSource();
            PluginLocation loc;
            try {
                loc = StandardPluginLocation.create(jfc.getSelectedFile());
                PluginLocation[] locations = new PluginLocation[1];
                locations[0] = loc;
                if (loc != null) {
                    Map map = manager.publishPlugins(locations);
                    log.debug("published " + map);
                    Vector<Expertice> expertices = Core.projectPanel.getExpertices();
                    for (Expertice ex : expertices) {
                        ex.getDataNavigation().rebuildMenus();
                    }
                    if (copy) {
                        File src = jfc.getSelectedFile();
                        File dst = new File(System.getProperty("user.dir") + File.separator + "plugins" + File.separator + src.getName());
                        try {
                            copyFiles(src, dst);
                            JOptionPane.showMessageDialog(Core.frame, "Plugin " + src.getName() + " installed successfully !");
                        } catch (IOException e1) {
                            JOptionPane.showMessageDialog(Core.frame, "Could not copy file.Error:" + e1.getMessage());
                        }
                    }
                } else {
                    log.info("Invalid Plugin: " + jfc.getSelectedFile().toString());
                }
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            } catch (JpfException e2) {
                e2.printStackTrace();
            }
        }
        log.debug(e);
    }

    /** Fast & simple file copy. */
    public static void copyFiles(File source, File dest) throws IOException {
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }
}
