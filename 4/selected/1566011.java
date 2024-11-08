package jpdstore.gui;

import jpdstore.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class MainWindow extends JFrame {

    private JTable jt;

    private OpenFileModel ofm;

    private Controller controller;

    private JLabel status;

    private char layerindex = 'A';

    private Map openLayers = new HashMap();

    public MainWindow(Controller c, Map namedLayers) {
        super();
        controller = c;
        openLayers.putAll(namedLayers);
        Container cpane = getContentPane();
        cpane.setLayout(new BorderLayout());
        cpane.add("South", status = new JLabel());
        cpane.add("Center", new JScrollPane(jt = new JTable(ofm = new OpenFileModel())));
        refreshStatus();
        JMenuBar mb = new JMenuBar();
        JMenu mm;
        JMenuItem mi;
        mb.add(mm = new JMenu("Store"));
        mm.setMnemonic(KeyEvent.VK_S);
        mm.add(mi = new JMenuItem("Select store directory..."));
        mi.setMnemonic(KeyEvent.VK_S);
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                File sdir = selectDir("Select store directory", controller.getDirectory());
                if (sdir != null) {
                    showWaitCursor();
                    controller.setDirectory(sdir);
                    if (controller.canBlockCountBeSet()) {
                        askForBlockCount();
                    }
                    refreshStatus();
                    showNormalCursor();
                }
            }
        });
        mm.add(mi = new JMenuItem("Import from Local store..."));
        mi.setMnemonic(KeyEvent.VK_L);
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                File sdir = selectDir("Import From", controller.getDirectory());
                if (sdir != null) {
                    try {
                        importFrom(sdir.toURI().toURL());
                    } catch (MalformedURLException ex) {
                        JOptionPane.showMessageDialog(MainWindow.this, "Incorrect Store URL", "Import Layer", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        mm.add(mi = new JMenuItem("Import from Remote store (URL)..."));
        mi.setMnemonic(KeyEvent.VK_U);
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                String urlS = JOptionPane.showInputDialog(MainWindow.this, "URL:", "Import Layer", JOptionPane.QUESTION_MESSAGE);
                if (urlS != null) {
                    try {
                        importFrom(new URL(urlS));
                    } catch (MalformedURLException ex) {
                        JOptionPane.showMessageDialog(MainWindow.this, "Incorrect Store URL", "Import Layer", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        mm.addSeparator();
        mm.add(mi = new JMenuItem("Exit"));
        mi.setMnemonic(KeyEvent.VK_X);
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                dispose();
            }
        });
        mb.add(mm = new JMenu("Layer"));
        mm.setMnemonic(KeyEvent.VK_L);
        mm.add(mi = new JMenuItem("Open..."));
        mi.setMnemonic(KeyEvent.VK_O);
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                String pw = readPassword("Open Layer (Name: " + layerindex + ")");
                if (pw != null) {
                    showWaitCursor();
                    controller.open(pw);
                    openLayers.put(pw, "" + (layerindex++));
                    refreshStatus();
                    showNormalCursor();
                }
            }
        });
        mm.add(mi = new JMenuItem("Close"));
        mi.setMnemonic(KeyEvent.VK_L);
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                String[] pwds = getSelectedLayers();
                showWaitCursor();
                for (int i = 0; i < pwds.length; i++) {
                    controller.close(pwds[i]);
                }
                refreshStatus();
                showNormalCursor();
            }
        });
        mm.addSeparator();
        mm.add(mi = new JMenuItem("Create..."));
        mi.setMnemonic(KeyEvent.VK_C);
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                String pw = readPassword("Create Layer (Name: " + layerindex + ")");
                if (pw != null) {
                    if (pw.length() == 0) {
                        JOptionPane.showMessageDialog(MainWindow.this, "Password must be  at least " + "one character long!", "Create Layer", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    try {
                        int count = Integer.parseInt(JOptionPane.showInputDialog(MainWindow.this, "How many blocks?", "Create Layer", JOptionPane.QUESTION_MESSAGE));
                        if (count <= 0) throw new NumberFormatException();
                        showWaitCursor();
                        if (controller.create(pw, count)) {
                            controller.open(pw);
                            openLayers.put(pw, "" + (layerindex++));
                            refreshStatus();
                        } else {
                            JOptionPane.showMessageDialog(MainWindow.this, "Could not create layer!", "Create Layer", JOptionPane.ERROR_MESSAGE);
                        }
                        showNormalCursor();
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(MainWindow.this, "Incorrect number!", "Create Layer", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        mm.add(mi = new JMenuItem("Create Garbage..."));
        mi.setMnemonic(KeyEvent.VK_G);
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                try {
                    int count = Integer.parseInt(JOptionPane.showInputDialog(MainWindow.this, "How many blocks?", "Create Layer", JOptionPane.QUESTION_MESSAGE));
                    if (count <= 0) throw new NumberFormatException();
                    showWaitCursor();
                    if (controller.create("", count)) {
                        refreshStatus();
                    } else {
                        JOptionPane.showMessageDialog(MainWindow.this, "Could not create layer!", "Create Layer", JOptionPane.ERROR_MESSAGE);
                    }
                    showNormalCursor();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(MainWindow.this, "Incorrect number!", "Create Layer", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        mm.add(mi = new JMenuItem("Delete..."));
        mi.setMnemonic(KeyEvent.VK_D);
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                String[] pwds = getSelectedLayers();
                int res = JOptionPane.showConfirmDialog(MainWindow.this, "Really delete these " + pwds.length + " Layers?", "Delete Layers", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (res == JOptionPane.YES_OPTION) {
                    showWaitCursor();
                    for (int i = 0; i < pwds.length; i++) {
                        controller.close(pwds[i]);
                        controller.delete(pwds[i]);
                    }
                    refreshStatus();
                    showNormalCursor();
                }
            }
        });
        mm.add(mi = new JMenuItem("Delete all closed..."));
        mi.setMnemonic(KeyEvent.VK_A);
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                int res = JOptionPane.showConfirmDialog(MainWindow.this, "Really delete all closed Layers?", "Delete Layers", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (res == JOptionPane.YES_OPTION) {
                    showWaitCursor();
                    controller.deleteClosed();
                    refreshStatus();
                    showNormalCursor();
                }
            }
        });
        mb.add(mm = new JMenu("File"));
        mm.setMnemonic(KeyEvent.VK_F);
        mm.add(mi = new JMenuItem("Insert..."));
        mi.setMnemonic(KeyEvent.VK_I);
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                int[] ind = jt.getSelectedRows();
                if (ind.length != 1 || ofm.getFile(ind[0]).getContentName() != null) {
                    JOptionPane.showMessageDialog(MainWindow.this, "Select an empty slot", "Insert File", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JFileChooser jfc = new JFileChooser();
                jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                jfc.setMultiSelectionEnabled(false);
                jfc.setDialogTitle("Insert File");
                if (jfc.showOpenDialog(MainWindow.this) == JFileChooser.APPROVE_OPTION) {
                    if (jfc.getSelectedFile().exists()) {
                        showWaitCursor();
                        controller.insert(jfc.getSelectedFile(), ofm.getFile(ind[0]));
                        refreshStatus();
                        showNormalCursor();
                    }
                }
            }
        });
        mm.add(mi = new JMenuItem("Save as..."));
        mi.setMnemonic(KeyEvent.VK_S);
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                int[] ind = jt.getSelectedRows();
                JFileChooser jfc = new JFileChooser();
                jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                jfc.setMultiSelectionEnabled(false);
                jfc.setDialogTitle("Save as");
                for (int i = 0; i < ind.length; i++) {
                    OpenFile of = ofm.getFile(ind[i]);
                    if (of.getContentName() != null) {
                        jfc.setSelectedFile(new File(jfc.getCurrentDirectory(), of.getContentName()));
                        if (jfc.showSaveDialog(MainWindow.this) != JFileChooser.APPROVE_OPTION) {
                            return;
                        }
                        if (jfc.getSelectedFile().exists()) {
                            int res = JOptionPane.showConfirmDialog(MainWindow.this, "Really overwrite?", "File already exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (res != JOptionPane.YES_OPTION) {
                                continue;
                            }
                        }
                        showWaitCursor();
                        controller.fetch(of, jfc.getSelectedFile());
                        showNormalCursor();
                    }
                }
            }
        });
        mm.add(mi = new JMenuItem("Delete..."));
        mi.setMnemonic(KeyEvent.VK_D);
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                int res = JOptionPane.showConfirmDialog(MainWindow.this, "Really delete all selected files?", "Delete Files", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (res != JOptionPane.YES_OPTION) {
                    return;
                }
                int[] ind = jt.getSelectedRows();
                showWaitCursor();
                for (int i = 0; i < ind.length; i++) {
                    OpenFile of = ofm.getFile(ind[i]);
                    if (of.getContentName() != null) {
                        controller.delete(of);
                    }
                }
                refreshStatus();
                showNormalCursor();
            }
        });
        mb.add(mm = new JMenu("Filesystem"));
        mm.setMnemonic(KeyEvent.VK_S);
        mm.add(mi = new JMenuItem("Access 10% of all files"));
        mi.setMnemonic(KeyEvent.VK_C);
        mi.addActionListener(new TouchActionListener(10, Controller.TOUCH_MODE_ACCESS));
        mm.add(mi = new JMenuItem("Modify 10% of all files"));
        mi.setMnemonic(KeyEvent.VK_O);
        mi.addActionListener(new TouchActionListener(10, Controller.TOUCH_MODE_MODIFY));
        mm.add(mi = new JMenuItem("Recreate 10% of all files"));
        mi.setMnemonic(KeyEvent.VK_E);
        mi.addActionListener(new TouchActionListener(10, Controller.TOUCH_MODE_RECREATE));
        mm.add(mi = new JMenuItem("Double-copy 10% of all files"));
        mi.setMnemonic(KeyEvent.VK_U);
        mi.addActionListener(new TouchActionListener(10, Controller.TOUCH_MODE_COPY_TWICE));
        mm.addSeparator();
        mm.add(mi = new JMenuItem("Access all files"));
        mi.setMnemonic(KeyEvent.VK_A);
        mi.addActionListener(new TouchActionListener(100, Controller.TOUCH_MODE_ACCESS));
        mm.add(mi = new JMenuItem("Modify all files"));
        mi.setMnemonic(KeyEvent.VK_M);
        mi.addActionListener(new TouchActionListener(100, Controller.TOUCH_MODE_MODIFY));
        mm.add(mi = new JMenuItem("Recreate all files"));
        mi.setMnemonic(KeyEvent.VK_R);
        mi.addActionListener(new TouchActionListener(100, Controller.TOUCH_MODE_RECREATE));
        mm.add(mi = new JMenuItem("Double-copy all files"));
        mi.setMnemonic(KeyEvent.VK_D);
        mi.addActionListener(new TouchActionListener(100, Controller.TOUCH_MODE_COPY_TWICE));
        getRootPane().setJMenuBar(mb);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        if (controller.canBlockCountBeSet()) {
            askForBlockCount();
        }
    }

    private void refreshStatus() {
        status.setText("" + controller.getClosedFiles().length + " closed blocks; " + controller.getEmptyChunks() + " empty blocks and " + controller.getOpenFilesCount() + " file blocks in " + controller.getLayersCount() + " open layers");
        ofm.refresh();
        if (ofm.getRowCount() == 0) {
        } else {
        }
        String title = "JPDStore " + Main.VERSION;
        try {
            title = controller.getDirectory().getCanonicalPath() + " - " + title;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        setTitle(title);
    }

    public String getLayerName(String password) {
        String name = (String) openLayers.get(password);
        return name == null ? "'" + password + "'" : name;
    }

    private String readPassword(String reason) {
        return new PasswordAskDialog(this, reason).getPasswordString();
    }

    private File selectDir(String title, File start) {
        JFileChooser jfc = new JFileChooser(start);
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jfc.setMultiSelectionEnabled(false);
        jfc.setDialogTitle(title);
        if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            if (jfc.getSelectedFile().exists()) {
                return jfc.getSelectedFile();
            }
            JOptionPane.showMessageDialog(this, "Directory does not exist", title, JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private void showWaitCursor() {
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
    }

    private void showNormalCursor() {
        setCursor(null);
    }

    private String[] getSelectedLayers() {
        int[] ind = jt.getSelectedRows();
        Set s = new HashSet();
        for (int i = 0; i < ind.length; i++) {
            s.add(ofm.getFile(ind[i]).getPassword());
        }
        return (String[]) s.toArray(new String[s.size()]);
    }

    private void importFrom(URL url) {
        String pw = readPassword("Import Layer");
        if (pw != null) {
            showWaitCursor();
            controller.importData(url, pw);
            controller.open(pw);
            openLayers.put(pw, "" + (layerindex++));
            refreshStatus();
            showNormalCursor();
        }
    }

    public class OpenFileModel extends AbstractTableModel {

        private OpenFile[] files = new OpenFile[0];

        /**
	 * Describe <code>getColumnCount</code> method here.
	 *
	 * @return an <code>int</code> value
	 */
        public int getColumnCount() {
            return 4;
        }

        /**
	 * Describe <code>getRowCount</code> method here.
	 *
	 * @return an <code>int</code> value
	 */
        public int getRowCount() {
            return files.length;
        }

        public Object getValueAt(int row, int col) {
            OpenFile of = files[row];
            switch(col) {
                case 0:
                    String name = of.getContentName();
                    return name == null ? "<empty>" : name;
                case 1:
                    return "" + of.getContentLength() + " bytes";
                case 2:
                    return "" + of.getFiles().length;
                case 3:
                    return getLayerName(of.getPassword()) + "." + of.getFirstIndex();
                default:
                    return null;
            }
        }

        /**
	 * Describe <code>getColumnName</code> method here.
	 *
	 * @param n an <code>int</code> value
	 * @return a <code>String</code> value
	 */
        public String getColumnName(int col) {
            return new String[] { "Filename", "Size", "Blocks", "Layer" }[col];
        }

        public OpenFile getFile(int row) {
            return files[row];
        }

        public void refresh() {
            files = controller.getOpenFiles();
            fireTableDataChanged();
        }
    }

    protected void askForBlockCount() {
        String msg = "By default, each file will be " + controller.getRawBlockSize() + " bytes large and contain " + controller.getPayloadBlockSize() + " bytes of payload.\n" + "You can make files larger (multiply their size) by " + "setting blockCount > 1. Block count to use?";
        while (true) {
            String bcString = JOptionPane.showInputDialog(this, msg, "1");
            if (bcString == null) return;
            try {
                int bc = Integer.parseInt(bcString);
                if (bc == 1) return; else if (bc > 1) {
                    controller.setBlockCount(bc);
                    return;
                }
            } catch (NumberFormatException ex) {
            }
            JOptionPane.showMessageDialog(this, "Invalid value.");
        }
    }

    private class TouchActionListener implements ActionListener {

        private final int percentage;

        private final int mode;

        public TouchActionListener(int percentage, int mode) {
            this.percentage = percentage;
            this.mode = mode;
        }

        public void actionPerformed(ActionEvent e) {
            showWaitCursor();
            controller.touchSomeFiles(percentage, mode);
            showNormalCursor();
        }
    }
}
