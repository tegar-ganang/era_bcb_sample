package jcfs.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import jcfs.core.client.JCFS;
import jcfs.core.fs.RFile;
import jcfs.core.fs.RFileInputStream;
import jcfs.core.fs.RSearchAnswer;
import org.apache.commons.io.IOUtils;

/**
 * main form
 * @author enrico
 */
public class JCFSClientGui extends javax.swing.JFrame {

    /** Creates new form JCFSClientGui */
    public JCFSClientGui() {
        initComponents();
        chooser = new JFileChooser();
    }

    private void initComponents() {
        jScrollPane1 = new javax.swing.JScrollPane();
        filesPane = new javax.swing.JList();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        searchName = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        maxFiles = new javax.swing.JSpinner();
        jToolBar1 = new javax.swing.JToolBar();
        jButton1 = new javax.swing.JButton();
        searchButton = new javax.swing.JButton();
        downloadButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        menuFile = new javax.swing.JMenu();
        quitItem = new javax.swing.JMenuItem();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Java Cluster File System Client GUI");
        jScrollPane1.setViewportView(filesPane);
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Search criteria"));
        jLabel1.setText("Name:");
        searchName.setText("*");
        jLabel2.setText("Max results:");
        maxFiles.setValue(10);
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addComponent(jLabel1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(searchName, javax.swing.GroupLayout.DEFAULT_SIZE, 278, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(jLabel2).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(maxFiles, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel1).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(searchName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(maxFiles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel2))).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jToolBar1.setFloatable(false);
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jcfs/client/servers.png")));
        jButton1.setText("Servers");
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton1);
        searchButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jcfs/client/refresh.png")));
        searchButton.setText("Search");
        searchButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        searchButton.setMaximumSize(new java.awt.Dimension(71, 49));
        searchButton.setMinimumSize(new java.awt.Dimension(71, 49));
        searchButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        searchButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(searchButton);
        downloadButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jcfs/client/search.png")));
        downloadButton.setText("Download");
        downloadButton.setFocusable(false);
        downloadButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        downloadButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        downloadButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(downloadButton);
        deleteButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jcfs/client/deletebutton.png")));
        deleteButton.setText("Delete");
        deleteButton.setFocusable(false);
        deleteButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        deleteButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        deleteButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(deleteButton);
        menuFile.setText("File");
        quitItem.setText("Exit");
        menuFile.add(quitItem);
        menuBar.add(menuFile);
        setJMenuBar(menuBar);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 473, Short.MAX_VALUE).addGroup(layout.createSequentialGroup().addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addContainerGap()).addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 473, Short.MAX_VALUE))));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap().addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(18, 18, 18).addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()));
        pack();
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        new ServersDialog(this, true).setVisible(true);
    }

    private void searchButtonActionPerformed(java.awt.event.ActionEvent evt) {
        performSearch();
    }

    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {
        List<String> selected = filesPane.getSelectedValuesList();
        if (selected == null || selected.isEmpty()) {
            return;
        }
        String msg = "Delete selected files ?";
        if (selected.size() == 1) {
            msg = "Delete selected file '" + selected.get(0) + "' ?";
        }
        int ok = JOptionPane.showConfirmDialog(this, msg, "Warning delete", JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.OK_OPTION) {
            for (String s : selected) {
                try {
                    JCFS.getInstance().deleteFile(new RFile(s));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            performSearch();
        }
    }

    private void downloadButtonActionPerformed(java.awt.event.ActionEvent evt) {
        performDownload();
    }

    private JFileChooser chooser;

    private void performDownload() {
        List<String> selected = filesPane.getSelectedValuesList();
        if (selected == null || selected.isEmpty() || selected.size() != 1) {
            JOptionPane.showMessageDialog(this, "Please select one path");
            return;
        }
        RFile file = new RFile(selected.get(0));
        if (!file.isFile()) {
            JOptionPane.showMessageDialog(this, "file does not exist anymore");
            return;
        }
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), file.getName()));
        int ok = chooser.showSaveDialog(this);
        if (ok != JFileChooser.APPROVE_OPTION) {
            return;
        }
        FileOutputStream fout = null;
        RFileInputStream in = null;
        try {
            fout = new FileOutputStream(chooser.getSelectedFile());
            in = new RFileInputStream(file);
            IOUtils.copy(in, fout);
            JOptionPane.showMessageDialog(this, "File downloaded to " + chooser.getSelectedFile(), "Download finished", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException iOException) {
            JOptionPane.showMessageDialog(this, "Error: " + iOException, "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable t) {
                }
            }
            if (fout != null) {
                try {
                    fout.close();
                } catch (Throwable t) {
                }
            }
        }
    }

    private void performSearch() {
        String search = searchName.getText();
        DefaultListModel<String> searchRes = new DefaultListModel<String>();
        try {
            int mFiles = ((Number) maxFiles.getValue()).intValue();
            List<RSearchAnswer> files = JCFS.getInstance().searchFile(new RFile(search), mFiles);
            boolean onefound = false;
            for (RSearchAnswer ans : files) {
                if (ans.isFileFound()) {
                    onefound = true;
                    searchRes.addElement(ans.getPath());
                }
            }
            if (!onefound) {
                searchRes.addElement(" - no file matched expression -");
            }
        } catch (Throwable t) {
            searchRes.addElement(" - error dunring search " + t + " -");
        }
        filesPane.setModel(searchRes);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        String configFile = "classes/config.properties";
        if (args.length > 0) {
            configFile = args[0];
        }
        Properties p = new Properties();
        File file = new File(configFile);
        System.out.println("Java cluster file client is starting...");
        System.out.println("configuration file is " + file.getAbsolutePath());
        if (file.isFile()) {
            FileInputStream configIn = new FileInputStream(file);
            p.load(configIn);
            configIn.close();
        }
        JCFS.configure(p);
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new JCFSClientGui().setVisible(true);
            }
        });
    }

    private javax.swing.JButton deleteButton;

    private javax.swing.JButton downloadButton;

    private javax.swing.JList filesPane;

    private javax.swing.JButton jButton1;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JToolBar jToolBar1;

    private javax.swing.JSpinner maxFiles;

    private javax.swing.JMenuBar menuBar;

    private javax.swing.JMenu menuFile;

    private javax.swing.JMenuItem quitItem;

    private javax.swing.JButton searchButton;

    private javax.swing.JTextField searchName;
}
