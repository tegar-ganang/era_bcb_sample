package JavaTron;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.Component;

/**
 * Dialog to Edit the radio list
 * @author Joe Culbreth
 * @version 0.5
 *
 */
public class ATRadioEditorDlg extends javax.swing.JFrame {

    AudioTronState at;

    ArrayList<StationEntry> stationList;

    RadioTableModel rtm;

    RadioTableListener listener;

    JComboBox formats;

    private String radioURL;

    private String currentOpenFile;

    private static String LIVE_FILE = "AUDIOTRONLIVE";

    boolean showExtra = false;

    /** Creates new form ATRadioEditorDlg */
    public ATRadioEditorDlg(AudioTronState at_, java.awt.Frame parent, String label_, JToggleButton toggle) {
        super(label_);
        at = at_;
        stationList = new ArrayList<StationEntry>();
        rtm = new RadioTableModel();
        formats = createFormatBox();
        initComponents();
        listener = new RadioTableListener();
        TableColumn formatColumn = radioTable.getColumnModel().getColumn(4);
        formatColumn.setCellEditor(new DefaultCellEditor(formats));
        int rows = radioTable.getRowCount();
        for (int i = rows - 1; i > 0; i--) {
            rtm.removeRow(i);
        }
        stationCounter.setText("0");
        rptText.setText("");
        currentOpenFile = new String();
        setIconImage(new ImageIcon(getClass().getResource("resources/mainIcon.png")).getImage());
    }

    public ATRadioEditorDlg(AudioTronState at_, java.awt.Frame parent, String label_, JToggleButton toggle, ArrayList<StationEntry> stationList_) {
        super(label_);
        at = at_;
        stationList = stationList_;
        rtm = new RadioTableModel();
        formats = createFormatBox();
        initComponents();
        rptText.setText("");
        listener = new RadioTableListener();
        TableColumn formatColumn = radioTable.getColumnModel().getColumn(4);
        formatColumn.setCellEditor(new DefaultCellEditor(formats));
        fillTable();
        saveBackup();
        currentOpenFile = LIVE_FILE;
        setIconImage(new ImageIcon(getClass().getResource("resources/mainIcon.png")).getImage());
        System.out.println(this.getIconImage().toString());
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        rtPopup = new javax.swing.JPopupMenu();
        poptop = new javax.swing.JMenu();
        Delete = new javax.swing.JMenuItem();
        jScrollPane1 = new javax.swing.JScrollPane();
        radioTable = new javax.swing.JTable();
        deleteButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        importButton = new javax.swing.JButton();
        replaceButton = new javax.swing.JButton();
        jToolBar1 = new javax.swing.JToolBar();
        saveButtonBar = new javax.swing.JButton();
        installButtonBar = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        jUpButton = new javax.swing.JButton();
        jDownButton = new javax.swing.JButton();
        deleteButtonBar = new javax.swing.JButton();
        jSeparator5 = new javax.swing.JToolBar.Separator();
        jLabel1 = new javax.swing.JLabel();
        stationCounter = new javax.swing.JLabel();
        rptText = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        mNew = new javax.swing.JMenuItem();
        mOpen = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        mSave = new javax.swing.JMenuItem();
        mSaveAs = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        mExit = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem12 = new javax.swing.JMenuItem();
        jMenuItem11 = new javax.swing.JMenuItem();
        jMenuItem10 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        mInstall = new javax.swing.JMenuItem();
        mImportCSV = new javax.swing.JMenuItem();
        mExportCSV = new javax.swing.JMenuItem();
        rtPopup.setName("rtPopup");
        poptop.setText("jMenu4");
        poptop.setName("poptop");
        Delete.setText("Delete Row");
        Delete.setName("Delete");
        Delete.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteActionPerformed(evt);
            }
        });
        poptop.add(Delete);
        rtPopup.add(poptop);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setIconImage(this.getToolkit().getImage("resources/smIcon.png"));
        jScrollPane1.setName("jScrollPane1");
        radioTable.setModel(rtm);
        radioTable.setName("radioTable");
        radioTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        radioTable.getTableHeader().setReorderingAllowed(false);
        radioTable.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                radioTableMouseClicked(evt);
            }

            public void mousePressed(java.awt.event.MouseEvent evt) {
                radioTableMousePressed(evt);
            }
        });
        jScrollPane1.setViewportView(radioTable);
        radioTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        radioTable.getColumnModel().getColumn(0).setPreferredWidth(15);
        radioTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        radioTable.getColumnModel().getColumn(5).setPreferredWidth(15);
        deleteButton.setText("Delete Selected");
        deleteButton.setName("deleteButton");
        deleteButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });
        saveButton.setText("Save");
        saveButton.setName("saveButton");
        saveButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });
        cancelButton.setText("Cancel");
        cancelButton.setName("cancelButton");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        importButton.setText("Import CSV");
        importButton.setName("importButton");
        importButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importButtonActionPerformed(evt);
            }
        });
        replaceButton.setText("Install");
        replaceButton.setName("replaceButton");
        replaceButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replaceButtonActionPerformed(evt);
            }
        });
        jToolBar1.setRollover(true);
        jToolBar1.setName("jToolBar1");
        saveButtonBar.setText("Save");
        saveButtonBar.setFocusable(false);
        saveButtonBar.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        saveButtonBar.setName("saveButtonBar");
        saveButtonBar.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        saveButtonBar.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonBarActionPerformed(evt);
            }
        });
        jToolBar1.add(saveButtonBar);
        installButtonBar.setText("Install");
        installButtonBar.setFocusable(false);
        installButtonBar.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        installButtonBar.setName("installButtonBar");
        installButtonBar.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        installButtonBar.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                installButtonBarActionPerformed(evt);
            }
        });
        jToolBar1.add(installButtonBar);
        jSeparator3.setName("jSeparator3");
        jToolBar1.add(jSeparator3);
        jUpButton.setText("Up");
        jUpButton.setFocusable(false);
        jUpButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jUpButton.setName("jUpButton");
        jUpButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jUpButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jUpButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(jUpButton);
        jDownButton.setText("Down");
        jDownButton.setFocusable(false);
        jDownButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jDownButton.setName("jDownButton");
        jDownButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jDownButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jDownButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(jDownButton);
        deleteButtonBar.setText("Delete");
        deleteButtonBar.setFocusable(false);
        deleteButtonBar.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        deleteButtonBar.setName("deleteButtonBar");
        deleteButtonBar.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        deleteButtonBar.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonBarActionPerformed(evt);
            }
        });
        jToolBar1.add(deleteButtonBar);
        jSeparator5.setName("jSeparator5");
        jToolBar1.add(jSeparator5);
        jLabel1.setText("Stations:");
        jLabel1.setName("jLabel1");
        stationCounter.setText("215");
        stationCounter.setName("stationCounter");
        rptText.setText("jLabel2");
        rptText.setName("rptText");
        jMenuBar1.setName("jMenuBar1");
        jMenu1.setText("File");
        jMenu1.setName("jMenu1");
        mNew.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        mNew.setText("New");
        mNew.setName("mNew");
        mNew.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mNewActionPerformed(evt);
            }
        });
        jMenu1.add(mNew);
        mOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        mOpen.setText("Open");
        mOpen.setName("mOpen");
        mOpen.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mOpenActionPerformed(evt);
            }
        });
        jMenu1.add(mOpen);
        jSeparator2.setName("jSeparator2");
        jMenu1.add(jSeparator2);
        mSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        mSave.setText("Save");
        mSave.setName("mSave");
        mSave.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mSaveActionPerformed(evt);
            }
        });
        jMenu1.add(mSave);
        mSaveAs.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.ALT_MASK));
        mSaveAs.setText("Save As");
        mSaveAs.setName("mSaveAs");
        mSaveAs.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mSaveAsActionPerformed(evt);
            }
        });
        jMenu1.add(mSaveAs);
        jSeparator1.setName("jSeparator1");
        jMenu1.add(jSeparator1);
        mExit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        mExit.setText("Exit");
        mExit.setName("mExit");
        mExit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mExitActionPerformed(evt);
            }
        });
        jMenu1.add(mExit);
        jMenuBar1.add(jMenu1);
        jMenu2.setText("Edit");
        jMenu2.setName("jMenu2");
        jMenuItem12.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem12.setText("Cut");
        jMenuItem12.setName("jMenuItem12");
        jMenu2.add(jMenuItem12);
        jMenuItem11.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem11.setText("Copy");
        jMenuItem11.setName("jMenuItem11");
        jMenu2.add(jMenuItem11);
        jMenuItem10.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem10.setText("Paste");
        jMenuItem10.setName("jMenuItem10");
        jMenu2.add(jMenuItem10);
        jMenuItem4.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem4.setText("Delete");
        jMenuItem4.setName("jMenuItem4");
        jMenu2.add(jMenuItem4);
        jMenuBar1.add(jMenu2);
        jMenu3.setText("Operations");
        jMenu3.setName("jMenu3");
        mInstall.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.ALT_MASK));
        mInstall.setText("Install");
        mInstall.setName("mInstall");
        mInstall.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mInstallActionPerformed(evt);
            }
        });
        jMenu3.add(mInstall);
        mImportCSV.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        mImportCSV.setText("Import CSV");
        mImportCSV.setEnabled(false);
        mImportCSV.setName("mImportCSV");
        jMenu3.add(mImportCSV);
        mExportCSV.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.ALT_MASK));
        mExportCSV.setText("Export CSV");
        mExportCSV.setEnabled(false);
        mExportCSV.setName("mExportCSV");
        jMenu3.add(mExportCSV);
        jMenuBar1.add(jMenu3);
        setJMenuBar(jMenuBar1);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addGap(18, 18, 18).addComponent(jLabel1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(stationCounter, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(40, 40, 40).addComponent(rptText, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 82, Short.MAX_VALUE).addComponent(importButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(deleteButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(saveButton).addGap(14, 14, 14).addComponent(replaceButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(cancelButton).addGap(8, 8, 8)).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 744, Short.MAX_VALUE).addGroup(layout.createSequentialGroup().addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 418, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 326, Short.MAX_VALUE))))).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 464, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(cancelButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel1).addComponent(stationCounter).addComponent(rptText)).addComponent(saveButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(deleteButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(importButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(replaceButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addContainerGap()));
        pack();
    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int rows = radioTable.getRowCount();
        for (int i = rows - 2; i > -1; i--) {
            if (showExtra) {
                System.out.println("i=" + i + ", value=" + radioTable.getValueAt(i, 0));
            }
            try {
                if (radioTable.getValueAt(i, 0).equals(Boolean.TRUE)) rtm.removeRow(i);
            } catch (Exception e) {
            }
            setCount();
        }
    }

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {
        save();
    }

    private void replaceButtonActionPerformed(java.awt.event.ActionEvent evt) {
        install();
    }

    private void installButtonBarActionPerformed(java.awt.event.ActionEvent evt) {
        install();
    }

    private void saveButtonBarActionPerformed(java.awt.event.ActionEvent evt) {
        final java.awt.event.ActionEvent e = evt;
        Runnable r = new Runnable() {

            public void run() {
                save();
            }
        };
        SwingUtilities.invokeLater(r);
    }

    private void mExitActionPerformed(java.awt.event.ActionEvent evt) {
        cancelButtonActionPerformed(evt);
    }

    private void mSaveActionPerformed(java.awt.event.ActionEvent evt) {
        final java.awt.event.ActionEvent e = evt;
        Runnable r = new Runnable() {

            public void run() {
                saveButtonActionPerformed(e);
            }
        };
        SwingUtilities.invokeLater(r);
    }

    private void mNewActionPerformed(java.awt.event.ActionEvent evt) {
        int res = JOptionPane.showConfirmDialog(this, "This will clear the list and you will lose any changes. Are you sure you want to?", "Confirm Action", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            rtm.setRowCount(0);
            rtm.setRowCount(1);
            currentOpenFile = new String();
        }
    }

    private void deleteButtonBarActionPerformed(java.awt.event.ActionEvent evt) {
        deleteButtonActionPerformed(evt);
    }

    private void mOpenActionPerformed(java.awt.event.ActionEvent evt) {
        open();
    }

    private void mSaveAsActionPerformed(java.awt.event.ActionEvent evt) {
        saveAs();
    }

    private void jDownButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int[] selrow = radioTable.getSelectedRows();
        int start = selrow[0];
        int end = start + selrow.length - 1;
        System.out.println(start + "," + end + "," + selrow.length);
        rtm.moveRow(start, end, start + 1);
        radioTable.changeSelection(start + 1, 1, false, false);
        for (int i = start + 1; i < start + selrow.length + 1; i++) {
            radioTable.changeSelection(i, 1, false, true);
        }
    }

    private void radioTableMouseClicked(java.awt.event.MouseEvent evt) {
    }

    private void radioTableMousePressed(java.awt.event.MouseEvent evt) {
    }

    private void DeleteActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void importButtonActionPerformed(java.awt.event.ActionEvent evt) {
        File csvfile = getImportFile();
        if (csvfile == null) {
            return;
        }
        ATRadioFile csvradio = new ATRadioFile();
        try {
            Scanner parser = new Scanner(new FileReader(csvfile));
            parser.useDelimiter("\n");
            while (parser.hasNext()) {
                String record = parser.next();
                System.out.println("R: " + record);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mInstallActionPerformed(java.awt.event.ActionEvent evt) {
        replaceButtonActionPerformed(evt);
    }

    private void jUpButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int[] selrow = radioTable.getSelectedRows();
        int start = selrow[0];
        int end = start + selrow.length - 1;
        System.out.println(start + "," + end + "," + selrow.length);
        rtm.moveRow(start, end, start - 1);
        radioTable.changeSelection(start - 1, 1, false, false);
        for (int i = start - 1; i < start + selrow.length - 1; i++) {
            radioTable.changeSelection(i, 1, false, true);
        }
    }

    private File getImportFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import CSV File");
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        int retVal = chooser.showDialog(this, "Open");
        if (retVal == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    private void open() {
        if (rtm.getRowCount() > 1) {
            int res = JOptionPane.showConfirmDialog(this, "This will clear the list and you will lose any changes. Are you sure you want to?", "Confirm Action", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                rtm.setRowCount(0);
                rtm.setRowCount(1);
                currentOpenFile = new String();
            } else {
                return;
            }
        }
        File openFile = chooseInFile();
        if (openFile == null) {
            return;
        }
        System.out.println(openFile.getPath());
        try {
            ATRadioFile rf = new ATRadioFile(openFile.getPath());
            stationList = rf.getStationList();
            System.out.println("Open: " + stationList.size());
        } catch (ATRadioFile.NotRadioFileException nre) {
            JPop j = new JPop("Holy Read Errors Batman!", "This doesn't appear to be a Radio List file");
        } catch (ATRadioFile.MalformedRadioFileException mre) {
            JPop j = new JPop("Holy Format Corruption Batman!", "There's something wrong with this file");
        }
        currentOpenFile = openFile.getPath();
        fillTable();
    }

    private void saveAs() {
        String radioBuffer = generateFile();
        File outfile = chooseOutFile();
        if (outfile == null) {
            return;
        }
        if (outfile.exists()) {
            if (!confirm("File Exists.\r\nOverwrite?")) {
                System.out.println("Refusing to overwrite");
                return;
            }
        }
        try {
            FileOutputStream fw = new FileOutputStream(outfile);
            fw.write(radioBuffer.getBytes());
        } catch (IOException e) {
            System.out.println("Couldn't write to " + outfile.getName());
            JPop j = new JPop("Failure", "Couldn't save " + outfile.getName());
            e.printStackTrace();
            return;
        }
    }

    public void save() {
        String radioBuffer = generateFile();
        if (currentOpenFile != null && !currentOpenFile.equals(LIVE_FILE) && !currentOpenFile.equals("")) {
            File outfile = new File(currentOpenFile);
            try {
                FileOutputStream fw = new FileOutputStream(outfile);
                fw.write(radioBuffer.getBytes());
            } catch (IOException e) {
                System.out.println("Couldn't write to " + outfile.getName());
                JPop j = new JPop("Failure", "Couldn't save " + outfile.getName());
                e.printStackTrace();
                return;
            }
        } else {
            saveAs();
        }
    }

    private void install() {
        class writeRadio extends Thread {

            public void run() {
                ATRadioFile rf = new ATRadioFile();
                setRadioURL();
                rf.setRadioURL(radioURL);
                rf.setUsername(Configuration.getProperty(Configuration.KEY_NET_USERNAME));
                try {
                    rf.setPassword(JTP.decrypt(Configuration.getProperty(Configuration.KEY_NET_PASSWORD)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                rf.setHost(Configuration.getProperty(Configuration.KEY_RADIO_HOST));
                rf.setTbUsername(Configuration.getProperty(Configuration.KEY_RADIO_USERNAME));
                if (rf.getHost() == null || rf.getPassword() == null || rf.getUsername() == null) {
                    JPop j = new JPop("Not enough Information", "Network Resrouces Incomplete\r\nPlease Check the Radio section in Preferences");
                    return;
                }
                if (rf.getHost().isEmpty() || rf.getPassword().isEmpty() || rf.getUsername().isEmpty()) {
                    JPop j = new JPop("Not enough Information", "Network Resrouces Incomplete\r\nPlease Check the Radio section in Preferences");
                    return;
                }
                boolean v = true;
                int rows = radioTable.getRowCount();
                for (int i = 0; i < rows - 1; i++) {
                    if (showExtra) {
                        System.out.print(i + "...");
                    }
                    StationEntry se = new StationEntry(radioTable.getValueAt(i, 3).toString(), radioTable.getValueAt(i, 1).toString(), radioTable.getValueAt(i, 2).toString(), radioTable.getValueAt(i, 4).toString(), v);
                    se.Validate();
                    if (showExtra) {
                        System.out.println(se.toString());
                    }
                    rf.addStation(se);
                }
                rptText.setForeground(JTP.parseRGB("0,0,0"));
                rptText.setText("Installing New Radio.txt file...");
                try {
                    rf.writeRadioFile();
                } catch (Exception e) {
                    JPop j = new JPop("Rats...", "URL: " + radioURL);
                    rptText.setForeground(Color.RED);
                    rptText.setText("Installation Failed...");
                    return;
                }
                rptText.setForeground(new Color(31, 156, 67));
                rptText.setText("New Stations Installed");
            }
        }
        writeRadio r = new writeRadio();
        r.start();
        rptText.setForeground(JTP.parseRGB("0,0,0"));
        rptText.setText("Generating new Radio.txt...");
    }

    public JComboBox createFormatBox() {
        JComboBox cb = new JComboBox();
        cb.addItem("Shoutcast");
        cb.addItem("IceCast");
        cb.addItem("WindowsMedia");
        return (cb);
    }

    private void fillTable() {
        int c = 0;
        Iterator i = stationList.iterator();
        while (i.hasNext()) {
            StationEntry se = (StationEntry) i.next();
            radioTable.setValueAt(Boolean.FALSE, c, 0);
            radioTable.setValueAt(se.getTitle(), c, 1);
            radioTable.setValueAt(se.getLocation(), c, 2);
            radioTable.setValueAt(se.getCategory(), c, 3);
            radioTable.setValueAt(se.getFormat(), c, 4);
            radioTable.setValueAt(se.isValid(), c, 5);
            if (c == radioTable.getRowCount() - 1) rtm.addRow();
            c++;
        }
        stationCounter.setText(String.valueOf(stationList.size()));
    }

    private void insertRow(RadioTableModel rtm, int index) {
    }

    private String generateFile() {
        System.out.println("Generating Radio.txt file");
        ATRadioFile rf = new ATRadioFile();
        boolean v = true;
        int rows = radioTable.getRowCount();
        for (int i = 0; i < rows - 1; i++) {
            if (showExtra) {
                System.out.print(i + "...");
            }
            StationEntry se = new StationEntry(radioTable.getValueAt(i, 3).toString(), radioTable.getValueAt(i, 1).toString(), radioTable.getValueAt(i, 2).toString(), radioTable.getValueAt(i, 4).toString(), v);
            se.Validate();
            if (showExtra) {
                System.out.println(se.toString());
            }
            rf.addStation(se);
        }
        return (new String(rf.getRadioText()));
    }

    private void setCount() {
        stationCounter.setText(Integer.toString(radioTable.getRowCount() - 1));
    }

    public void setRadioURL() {
        String ru = "smb://" + Configuration.getProperty(Configuration.KEY_RADIO_HOST) + "/" + Configuration.getProperty(Configuration.KEY_RADIO_SHARE) + "/" + "radio.txt";
        setRadioURL(ru);
    }

    public void setRadioURL(String url) {
        radioURL = url;
    }

    private void saveBackup() {
        System.out.println("Creating a backup...");
        String radioBuffer = generateFile();
        try {
            BufferedWriter os = new BufferedWriter(new FileWriter("./radio.txt"));
            int size = radioBuffer.length();
            byte[] data = new byte[size];
            data = radioBuffer.getBytes();
            os.write(radioBuffer, 0, size);
            os.flush();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File chooseOutFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Radio file");
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        int returnVal = chooser.showDialog(this, "Save");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            System.out.println("You chose to save to this file: " + chooser.getSelectedFile().getName());
            return (chooser.getSelectedFile());
        }
        return null;
    }

    private File chooseInFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Radio File");
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        int retVal = chooser.showDialog(this, "Open");
        if (retVal == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    /**
	 * RadioTableModel Class
	 */
    private class RadioTableModel extends DefaultTableModel {

        Class[] types = new Class[] { java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, javax.swing.JComboBox.class, java.lang.Boolean.class };

        public RadioTableModel() {
            super(new Object[][] { new Object[] { Boolean.FALSE, null, null, null, null, Boolean.TRUE } }, new String[] { "Select", "Title", "Location", "Category", "Format", "Valid" });
        }

        public Class getColumnClass(int columnIndex) {
            return types[columnIndex];
        }

        public void addRow() {
            super.addRow(new Object[] { Boolean.FALSE, null, null, null, null, Boolean.TRUE });
            setCount();
        }
    }

    /**
	 * RadioTableListener Class
	 */
    public class RadioTableListener implements TableModelListener {

        public RadioTableListener() {
            radioTable.getModel().addTableModelListener(this);
        }

        public void tableChanged(TableModelEvent e) {
            if (showExtra) {
                System.out.println("Table Changed...");
            }
            boolean populated = false;
            if (showExtra) {
                if (e.getType() == e.INSERT) System.out.println("Insert"); else if (e.getType() == e.UPDATE) System.out.println("Update"); else if (e.getType() == e.DELETE) System.out.println("Delete"); else System.out.println("OTHER");
            }
            int row = e.getFirstRow();
            int column = e.getColumn();
            TableModel model = (TableModel) e.getSource();
            String columnName = model.getColumnName(column);
            if (e.getType() == e.UPDATE && row == radioTable.getRowCount() - 1) {
                rtm.addRow();
            }
        }
    }

    protected boolean confirm(String msg) {
        Object[] options = { "Yes", "No" };
        int sel = JOptionPane.showOptionDialog(null, msg, "Warning", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        return (sel == 0);
    }

    private javax.swing.JMenuItem Delete;

    private javax.swing.JButton cancelButton;

    private javax.swing.JButton deleteButton;

    private javax.swing.JButton deleteButtonBar;

    private javax.swing.JButton importButton;

    private javax.swing.JButton installButtonBar;

    private javax.swing.JButton jDownButton;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JMenu jMenu1;

    private javax.swing.JMenu jMenu2;

    private javax.swing.JMenu jMenu3;

    private javax.swing.JMenuBar jMenuBar1;

    private javax.swing.JMenuItem jMenuItem10;

    private javax.swing.JMenuItem jMenuItem11;

    private javax.swing.JMenuItem jMenuItem12;

    private javax.swing.JMenuItem jMenuItem4;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JPopupMenu.Separator jSeparator1;

    private javax.swing.JPopupMenu.Separator jSeparator2;

    private javax.swing.JToolBar.Separator jSeparator3;

    private javax.swing.JToolBar.Separator jSeparator5;

    private javax.swing.JToolBar jToolBar1;

    private javax.swing.JButton jUpButton;

    private javax.swing.JMenuItem mExit;

    private javax.swing.JMenuItem mExportCSV;

    private javax.swing.JMenuItem mImportCSV;

    private javax.swing.JMenuItem mInstall;

    private javax.swing.JMenuItem mNew;

    private javax.swing.JMenuItem mOpen;

    private javax.swing.JMenuItem mSave;

    private javax.swing.JMenuItem mSaveAs;

    private javax.swing.JMenu poptop;

    private javax.swing.JTable radioTable;

    private javax.swing.JButton replaceButton;

    private javax.swing.JLabel rptText;

    private javax.swing.JPopupMenu rtPopup;

    private javax.swing.JButton saveButton;

    private javax.swing.JButton saveButtonBar;

    private javax.swing.JLabel stationCounter;
}
