package com.primianotucci.jsmartcardexplorer;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 *
 * @author  Administrator
 */
public class FrmMain extends javax.swing.JFrame {

    SmartCard SC = new SmartCard();

    DefaultComboBoxModel listAPDUmod = new DefaultComboBoxModel();

    DefaultTreeModel fsTreeData;

    /** Creates new form FrmMain */
    public FrmMain() {
        initComponents();
        updateReaderCombo();
        fsTreeData = new DefaultTreeModel(new DefaultMutableTreeNode("/"));
        treeFS.setModel(fsTreeData);
        loadWizards();
        listAPDU.setModel(listAPDUmod);
    }

    private void saveProject() {
        try {
            JFileChooser f = new JFileChooser();
            f.showSaveDialog(this);
            File saveFile = f.getSelectedFile();
            if (saveFile == null) return;
            FileWriter w = new FileWriter(saveFile);
            for (int i = 0; i < listAPDUmod.getSize(); i++) {
                APDUCommandBoxing apdu = (APDUCommandBoxing) listAPDUmod.getElementAt(i);
                w.write(apdu.toString());
                w.write("\n");
            }
            w.close();
            JOptionPane.showMessageDialog(this, "Saved to " + saveFile.getAbsolutePath());
        } catch (Exception ex) {
            reportException(ex);
        }
    }

    private void loadProject() {
        try {
            JFileChooser f = new JFileChooser();
            f.showOpenDialog(this);
            File openFile = f.getSelectedFile();
            if (openFile == null || !openFile.exists()) return;
            BufferedReader r = new BufferedReader(new FileReader(openFile));
            String line = null;
            listAPDUmod.removeAllElements();
            ((DefaultTableModel) tabAPDU.getModel()).setRowCount(0);
            while ((line = r.readLine()) != null) {
                APDUCommandBoxing apdu = new APDUCommandBoxing(line);
                listAPDUmod.addElement(apdu);
            }
            r.close();
            tabs.setSelectedComponent(panSavedAPDU);
        } catch (Exception ex) {
            reportException(ex);
        }
    }

    private void loadWizards() {
        ActionListener mnuAct = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                mnuSelectActionPerformed(e);
            }
        };
        try {
            BufferedReader bi = new BufferedReader(new InputStreamReader(CardList.class.getResourceAsStream("resources/commands.txt")));
            String line = null;
            while ((line = bi.readLine()) != null) {
                String arr[] = line.split("=", 2);
                if (arr.length != 2) continue;
                JMenuItem itm = new JMenuItem(arr[0]);
                itm.setToolTipText(arr[1]);
                itm.addActionListener(mnuAct);
                mnuWizards.add(itm);
            }
            mnuWizards.add(new JSeparator());
            bi.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void insertStringData() {
        String str = JOptionPane.showInputDialog(this, "Type ascii data to convert");
        if (str == null) return;
        tbAPDUDatain.setText(StringUtil.byteArrToString(str.getBytes(), " "));
    }

    private String getProtocolString() {
        switch(cbProto.getSelectedIndex()) {
            case 1:
                return "T=0";
            case 0:
                return "T=1";
            default:
                return "*";
        }
    }

    private void restoreAPDUFromHistory() {
        if (listAPDU.getSelectedValue() == null) return;
        CommandAPDU apdu = ((APDUCommandBoxing) listAPDU.getSelectedValue()).getAPDU();
        tbAPDUClass.setText(StringUtil.byteToHex(apdu.getCLA()));
        tbAPDUIns.setText(StringUtil.byteToHex(apdu.getINS()));
        tbAPDUP1.setText(StringUtil.byteToHex(apdu.getP1()));
        tbAPDUP2.setText(StringUtil.byteToHex(apdu.getP2()));
        tbAPDUP3.setText(StringUtil.byteToHex(apdu.getNc()));
        tbAPDUDatain.setText(StringUtil.byteArrToString(apdu.getData(), " "));
        tbAPDULe.setText(StringUtil.byteToHex(apdu.getNe()));
        tabs.setSelectedIndex(0);
    }

    private void mnuSelectActionPerformed(java.awt.event.ActionEvent evt) {
        if (!(evt.getSource() instanceof JMenuItem)) return;
        JMenuItem itm = (JMenuItem) evt.getSource();
        String tip = itm.getToolTipText();
        if (tip.length() < 3) return;
        tip = tip.substring(1, tip.length() - 1);
        for (String el : tip.split(",")) {
            String[] k = el.trim().split(":");
            if (k.length != 2) continue;
            String key = k[0];
            String val = k[1];
            if (key.equals("CLA")) tbAPDUClass.setText(val); else if (key.equals("INS")) tbAPDUIns.setText(val); else if (key.equals("INS")) tbAPDUIns.setText(val); else if (key.equals("P1")) tbAPDUP1.setText(val); else if (key.equals("P2")) tbAPDUP2.setText(val); else if (key.equals("P3")) tbAPDUP3.setText(val); else if (key.equals("DATA")) tbAPDUDatain.setText(val); else if (key.equals("LE")) tbAPDULe.setText(val);
        }
    }

    void updateSelectedTableRow() {
        int row = tabAPDU.getSelectedRow();
        if (row < 0) return;
        APDUSet apdu = getTableAPDU();
        tbHistClass.setText(StringUtil.byteToHex(apdu.Cmd.getCLA()));
        tbHistIns.setText(StringUtil.byteToHex(apdu.Cmd.getINS()));
        tbHistP1.setText(StringUtil.byteToHex(apdu.Cmd.getP1()));
        tbHistP2.setText(StringUtil.byteToHex(apdu.Cmd.getP2()));
        tbHistP3.setText(StringUtil.byteToHex(apdu.Cmd.getNc()));
        tbHistDataIn.setText(StringUtil.byteArrToString(apdu.Cmd.getData(), " "));
        tbHistDataIn.setToolTipText(StringUtil.byteArrToPrintableString(apdu.Cmd.getData()));
        tbHistDataOut.setText(StringUtil.byteArrToString(apdu.Resp.getData(), " "));
        tbHistDataOut.setToolTipText(StringUtil.byteArrToPrintableString(apdu.Resp.getData()));
        tbHistSW.setText(StringUtil.byteToHex(apdu.Resp.getSW1()) + " " + StringUtil.byteToHex(apdu.Resp.getSW2()));
        tbHistSW.setToolTipText(ISO7816Response.getResponseString(apdu.Resp.getSW1(), apdu.Resp.getSW2()));
        tbSentData.setText(StringUtil.hexDump(apdu.Cmd.getData(), 16));
        tbRecvData.setText(StringUtil.hexDump(apdu.Resp.getData(), 16));
        try {
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) fsTreeData.getRoot();
            root.removeAllChildren();
            ISO7816FileSystem.DF fs = ISO7816FileSystem.parseStructure(apdu.Resp.getData(), 0);
            addDFToTreeNode(root, fs);
            treeFS.updateUI();
        } catch (Exception ex) {
        }
    }

    private void addDFToTreeNode(DefaultMutableTreeNode iNode, ISO7816FileSystem.DF iDF) {
        DefaultMutableTreeNode curRoot = new DefaultMutableTreeNode(iDF);
        iNode.add(curRoot);
        for (ISO7816FileSystem.EF ef : iDF.getSubEFs()) curRoot.add(new DefaultMutableTreeNode(ef));
        for (ISO7816FileSystem.DF df : iDF.getSubDFs()) addDFToTreeNode(curRoot, df);
    }

    APDUSet getTableAPDU() {
        APDUSet outSet = new APDUSet();
        int row = tabAPDU.getSelectedRow();
        if (row < 0) return null;
        DefaultTableModel mod = (DefaultTableModel) tabAPDU.getModel();
        String Scla = mod.getValueAt(row, 0).toString();
        String Sins = mod.getValueAt(row, 1).toString();
        String Sp1 = mod.getValueAt(row, 2).toString();
        String Sp2 = mod.getValueAt(row, 3).toString();
        String Sp3 = mod.getValueAt(row, 4).toString();
        String Sle = mod.getValueAt(row, 5).toString();
        String Sdata = mod.getValueAt(row, 6).toString();
        String Sdataresp = mod.getValueAt(row, 11).toString();
        Integer cla = StringUtil.parseHex(Scla);
        Integer ins = StringUtil.parseHex(Sins);
        Integer p1 = StringUtil.parseHex(Sp1);
        Integer p2 = StringUtil.parseHex(Sp2);
        Integer p3 = StringUtil.parseHex(Sp3);
        Integer le = StringUtil.parseHex(Sle);
        byte[] data = StringUtil.stringToByteArr(Sdata);
        byte[] respBytes = StringUtil.stringToByteArr(Sdataresp);
        CommandAPDU apdu = null;
        if (p3 == null && Sdata.length() == 0 && le == null) apdu = new CommandAPDU(cla, ins, p1, p2); else if (p3 == null && le == null) apdu = new CommandAPDU(cla, ins, p1, p2, data); else if (le == null) apdu = new CommandAPDU(cla, ins, p1, p2, data, 0, p3); else {
            if (p3 == null) p3 = 0;
            apdu = new CommandAPDU(cla, ins, p1, p2, data, 0, p3, le);
        }
        ResponseAPDU apdures = new ResponseAPDU(respBytes);
        outSet.Cmd = apdu;
        outSet.Resp = apdures;
        return outSet;
    }

    void sendAPDU() throws CardException {
        CommandAPDU apdu = null;
        if (!SC.isConnected()) {
            this.reportException(new Exception("Please connect first"));
            return;
        }
        String Sdata = tbAPDUDatain.getText().trim();
        String Scla = tbAPDUClass.getText().trim();
        String Sins = tbAPDUIns.getText().trim();
        String Sp1 = tbAPDUP1.getText().trim();
        String Sp2 = tbAPDUP2.getText().trim();
        String Sp3 = tbAPDUP3.getText().trim();
        String Sle = tbAPDULe.getText().trim();
        Integer cla = StringUtil.parseHex(Scla);
        Integer ins = StringUtil.parseHex(Sins);
        Integer p1 = StringUtil.parseHex(Sp1);
        Integer p2 = StringUtil.parseHex(Sp2);
        Integer p3 = StringUtil.parseHex(Sp3);
        Integer le = StringUtil.parseHex(Sle);
        if (cla == null) {
            this.reportException(new Exception("Invalid value for CLASS"));
            return;
        }
        if (ins == null) {
            this.reportException(new Exception("Invalid value for INS"));
            return;
        }
        if (p1 == null || p2 == null) {
            this.reportException(new Exception("Invalid value for P1/2"));
            return;
        }
        byte[] data = StringUtil.stringToByteArr(Sdata);
        if (Sdata.length() == 0 && le == null) apdu = new CommandAPDU(cla, ins, p1, p2); else if (p3 == null && le == null) apdu = new CommandAPDU(cla, ins, p1, p2, data); else if (le == null) apdu = new CommandAPDU(cla, ins, p1, p2, data, 0, p3); else {
            if (p3 == null) p3 = data.length;
            apdu = new CommandAPDU(cla, ins, p1, p2, data, 0, p3, le);
        }
        ResponseAPDU res = SC.getChannel().transmit(apdu);
        String sw = StringUtil.byteToHex(res.getSW1()) + " " + StringUtil.byteToHex(res.getSW2());
        ISO7816Response isoResp = new ISO7816Response(res.getSW1(), res.getSW2());
        String status = isoResp.toString();
        if (isoResp.isGood()) lbLastStatus.setForeground(Color.GREEN); else lbLastStatus.setForeground(Color.RED);
        lbLastStatus.setText(status);
        String dataoutHex = StringUtil.byteArrToString(res.getData(), " ");
        String dataoutStr = StringUtil.byteArrToPrintableString(res.getData());
        String fullResHex = StringUtil.byteArrToString(res.getBytes(), " ");
        DefaultTableModel table = (DefaultTableModel) tabAPDU.getModel();
        table.addRow(new Object[] { Scla, Sins, Sp1, Sp2, Sp3, Sle, Sdata, sw, status, dataoutHex, dataoutStr, fullResHex });
        table.fireTableDataChanged();
    }

    void updateReaderCombo() {
        try {
            cbReader.setModel(new DefaultComboBoxModel(SC.listReaders()));
        } catch (Exception ex) {
            reportException(ex);
        }
    }

    void reportException(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Exception", JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
    }

    void selectCurrentReader() {
        try {
            if (cbReader.getSelectedItem() != null) SC.setReader(cbReader.getSelectedItem());
        } catch (Exception ex) {
            reportException(ex);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        mnuTable = new javax.swing.JPopupMenu();
        mnuResendAPDU = new javax.swing.JMenuItem();
        mnuSaveAPDU = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        mnuClearHist = new javax.swing.JMenuItem();
        mnuWizards = new javax.swing.JPopupMenu();
        mnuSavedAPDU = new javax.swing.JPopupMenu();
        mnuResend = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        mnuDelAPDU = new javax.swing.JMenuItem();
        jPanel1 = new javax.swing.JPanel();
        cbReader = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        btReloadReaders = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        lbStatus = new javax.swing.JTextField();
        btConnect = new javax.swing.JToggleButton();
        cbProto = new javax.swing.JComboBox();
        jLabel23 = new javax.swing.JLabel();
        tbCardATR = new javax.swing.JTextField();
        btLoadProject = new javax.swing.JButton();
        btSaveProject = new javax.swing.JButton();
        splitter = new javax.swing.JSplitPane();
        panHist = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tabAPDU = new javax.swing.JTable();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        tbHistClass = new javax.swing.JTextField();
        tbHistIns = new javax.swing.JTextField();
        tbHistP1 = new javax.swing.JTextField();
        tbHistP2 = new javax.swing.JTextField();
        tbHistP3 = new javax.swing.JTextField();
        tbHistDataIn = new javax.swing.JTextField();
        tbHistDataLen = new javax.swing.JTextField();
        tbHistSW = new javax.swing.JTextField();
        tbHistDataOut = new javax.swing.JTextField();
        tabs = new javax.swing.JTabbedPane();
        panSendAPDU = new javax.swing.JPanel();
        tbAPDUClass = new javax.swing.JFormattedTextField();
        tbAPDUIns = new javax.swing.JFormattedTextField();
        tbAPDUP1 = new javax.swing.JFormattedTextField();
        tbAPDUP2 = new javax.swing.JFormattedTextField();
        tbAPDUP3 = new javax.swing.JFormattedTextField();
        tbAPDULe = new javax.swing.JFormattedTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        btWizard = new javax.swing.JButton();
        tbAPDUDatain = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        lbLastStatus = new javax.swing.JLabel();
        panSavedAPDU = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        listAPDU = new javax.swing.JList();
        jLabel19 = new javax.swing.JLabel();
        panDataView = new javax.swing.JPanel();
        jLabel20 = new javax.swing.JLabel();
        scrollSentData = new javax.swing.JScrollPane();
        tbSentData = new javax.swing.JTextArea();
        scrollRecvData = new javax.swing.JScrollPane();
        tbRecvData = new javax.swing.JTextArea();
        jLabel22 = new javax.swing.JLabel();
        panFS = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        treeFS = new javax.swing.JTree();
        scrollFSData = new javax.swing.JScrollPane();
        tbFSData = new javax.swing.JTextArea();
        mnuResendAPDU.setText("Resend APDU");
        mnuResendAPDU.setEnabled(false);
        mnuTable.add(mnuResendAPDU);
        mnuSaveAPDU.setText("Save APDU");
        mnuSaveAPDU.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuSaveAPDUActionPerformed(evt);
            }
        });
        mnuTable.add(mnuSaveAPDU);
        mnuTable.add(jSeparator1);
        mnuClearHist.setText("Clear");
        mnuClearHist.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuClearHistActionPerformed(evt);
            }
        });
        mnuTable.add(mnuClearHist);
        mnuResend.setText("Resend");
        mnuResend.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuResendActionPerformed(evt);
            }
        });
        mnuSavedAPDU.add(mnuResend);
        mnuSavedAPDU.add(jSeparator2);
        mnuDelAPDU.setText("Remove");
        mnuDelAPDU.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuDelAPDUActionPerformed(evt);
            }
        });
        mnuSavedAPDU.add(mnuDelAPDU);
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("JSmartCard Explorer 1.0.4         Primiano Tucci -  http://www.primianotucci.com");
        setFont(new java.awt.Font("Tahoma", 0, 12));
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Reader Settings"));
        cbReader.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbReaderItemStateChanged(evt);
            }
        });
        jLabel1.setText("Reader");
        btReloadReaders.setBackground(new java.awt.Color(204, 255, 204));
        btReloadReaders.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/primianotucci/jsmartcardexplorer/resources/reload.png")));
        btReloadReaders.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btReloadReadersActionPerformed(evt);
            }
        });
        jLabel6.setText("Status:");
        lbStatus.setBackground(java.awt.Color.red);
        lbStatus.setEditable(false);
        lbStatus.setFont(new java.awt.Font("Verdana", 0, 10));
        lbStatus.setText("Card status");
        lbStatus.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        btConnect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/primianotucci/jsmartcardexplorer/resources/connect.png")));
        btConnect.setText("Connect");
        btConnect.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btConnectActionPerformed(evt);
            }
        });
        cbProto.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Protocol: Auto", "T=0", "T=1" }));
        jLabel23.setText("Card ATR:");
        tbCardATR.setEditable(false);
        btLoadProject.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/primianotucci/jsmartcardexplorer/resources/open.png")));
        btLoadProject.setText("Load project");
        btLoadProject.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btLoadProjectActionPerformed(evt);
            }
        });
        btSaveProject.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/primianotucci/jsmartcardexplorer/resources/save.png")));
        btSaveProject.setText("Save project");
        btSaveProject.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btSaveProjectActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 41, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addGroup(jPanel1Layout.createSequentialGroup().addComponent(cbReader, javax.swing.GroupLayout.PREFERRED_SIZE, 226, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btReloadReaders, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)).addComponent(lbStatus)).addGap(28, 28, 28).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(cbProto, 0, 135, Short.MAX_VALUE).addComponent(btConnect, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE)).addGap(52, 52, 52).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addComponent(jLabel23).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(tbCardATR, javax.swing.GroupLayout.DEFAULT_SIZE, 284, Short.MAX_VALUE)).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup().addComponent(btLoadProject, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(18, 18, 18).addComponent(btSaveProject, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE))).addContainerGap()));
        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { btLoadProject, btSaveProject });
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(btReloadReaders, javax.swing.GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE).addComponent(cbReader, javax.swing.GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE).addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)).addGap(11, 11, 11).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lbStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE))).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel23).addComponent(tbCardATR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(cbProto, javax.swing.GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btSaveProject).addComponent(btLoadProject).addComponent(btConnect, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)))).addContainerGap()));
        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] { btReloadReaders, cbProto, cbReader, jLabel1 });
        splitter.setBorder(null);
        splitter.setDividerLocation(240);
        splitter.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitter.setResizeWeight(0.8);
        panHist.setBorder(javax.swing.BorderFactory.createTitledBorder("APDU History"));
        jScrollPane1.setBackground(new java.awt.Color(255, 255, 255));
        tabAPDU.setModel(new javax.swing.table.DefaultTableModel(new Object[][] {}, new String[] { "Class", "INS", "P1", "P2", "P3/Lc", "Len", "Data (Hex)", "SW", "Response (verbose)", "Data Out (Hex)", "Data Out (String)", "Full Response (Hex)" }) {

            Class[] types = new Class[] { java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class };

            boolean[] canEdit = new boolean[] { false, false, false, false, false, false, false, false, false, false, false, false };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        tabAPDU.setEditingColumn(0);
        tabAPDU.setEditingRow(0);
        tabAPDU.setMaximumSize(new java.awt.Dimension(2147483647, 1000));
        tabAPDU.setMinimumSize(null);
        tabAPDU.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tabAPDUMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tabAPDU);
        tabAPDU.getColumnModel().getColumn(0).setMaxWidth(30);
        tabAPDU.getColumnModel().getColumn(1).setMaxWidth(30);
        tabAPDU.getColumnModel().getColumn(2).setMaxWidth(30);
        tabAPDU.getColumnModel().getColumn(3).setMaxWidth(30);
        tabAPDU.getColumnModel().getColumn(4).setMaxWidth(30);
        tabAPDU.getColumnModel().getColumn(5).setMaxWidth(30);
        tabAPDU.getColumnModel().getColumn(7).setMaxWidth(90);
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel10.setText("Class");
        jLabel10.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel11.setText("INS");
        jLabel11.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel12.setText("P1");
        jLabel12.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel13.setText("P2");
        jLabel13.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel14.setText("Lc");
        jLabel14.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel15.setText("Data Sent (Hex)");
        jLabel15.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel16.setText("Le");
        jLabel16.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel17.setText("SW");
        jLabel17.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel18.setText("Data Recv (Hex)");
        jLabel18.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        tbHistClass.setBackground(new java.awt.Color(236, 234, 231));
        tbHistClass.setEditable(false);
        tbHistClass.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        tbHistClass.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        tbHistIns.setBackground(new java.awt.Color(236, 234, 231));
        tbHistIns.setEditable(false);
        tbHistIns.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        tbHistIns.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        tbHistP1.setBackground(new java.awt.Color(236, 234, 231));
        tbHistP1.setEditable(false);
        tbHistP1.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        tbHistP1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        tbHistP2.setBackground(new java.awt.Color(236, 234, 231));
        tbHistP2.setEditable(false);
        tbHistP2.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        tbHistP2.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        tbHistP3.setBackground(new java.awt.Color(236, 234, 231));
        tbHistP3.setEditable(false);
        tbHistP3.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        tbHistP3.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        tbHistDataIn.setBackground(new java.awt.Color(236, 234, 231));
        tbHistDataIn.setEditable(false);
        tbHistDataIn.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        tbHistDataIn.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        tbHistDataLen.setBackground(new java.awt.Color(236, 234, 231));
        tbHistDataLen.setEditable(false);
        tbHistDataLen.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        tbHistDataLen.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        tbHistSW.setBackground(new java.awt.Color(236, 234, 231));
        tbHistSW.setEditable(false);
        tbHistSW.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        tbHistSW.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        tbHistDataOut.setBackground(new java.awt.Color(236, 234, 231));
        tbHistDataOut.setEditable(false);
        tbHistDataOut.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        tbHistDataOut.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        javax.swing.GroupLayout panHistLayout = new javax.swing.GroupLayout(panHist);
        panHist.setLayout(panHistLayout);
        panHistLayout.setHorizontalGroup(panHistLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panHistLayout.createSequentialGroup().addContainerGap().addGroup(panHistLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 870, Short.MAX_VALUE).addGroup(panHistLayout.createSequentialGroup().addGroup(panHistLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(tbHistClass, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 30, Short.MAX_VALUE)).addGap(6, 6, 6).addGroup(panHistLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(tbHistIns, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panHistLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(tbHistP1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panHistLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(tbHistP2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panHistLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(tbHistP3, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)).addGap(6, 6, 6).addGroup(panHistLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel15, javax.swing.GroupLayout.DEFAULT_SIZE, 295, Short.MAX_VALUE).addComponent(tbHistDataIn, javax.swing.GroupLayout.DEFAULT_SIZE, 295, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panHistLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(tbHistDataLen).addComponent(jLabel16, javax.swing.GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panHistLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(tbHistSW, javax.swing.GroupLayout.DEFAULT_SIZE, 52, Short.MAX_VALUE).addComponent(jLabel17, javax.swing.GroupLayout.DEFAULT_SIZE, 52, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panHistLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(tbHistDataOut, javax.swing.GroupLayout.DEFAULT_SIZE, 291, Short.MAX_VALUE).addComponent(jLabel18, javax.swing.GroupLayout.DEFAULT_SIZE, 291, Short.MAX_VALUE)))).addContainerGap()));
        panHistLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { tbHistClass, tbHistIns, tbHistP1, tbHistP2, tbHistP3 });
        panHistLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { jLabel10, jLabel11, jLabel12, jLabel13, jLabel14 });
        panHistLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { jLabel17, tbHistSW });
        panHistLayout.setVerticalGroup(panHistLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panHistLayout.createSequentialGroup().addComponent(jScrollPane1).addGap(6, 6, 6).addGroup(panHistLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addGroup(panHistLayout.createSequentialGroup().addGroup(panHistLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panHistLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE, false).addComponent(tbHistClass, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(tbHistIns, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(tbHistP1, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(tbHistP2, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(tbHistDataIn, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(tbHistDataLen, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(tbHistDataOut, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(tbHistSW, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))).addGroup(panHistLayout.createSequentialGroup().addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(tbHistP3, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)))));
        panHistLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] { tbHistClass, tbHistDataIn, tbHistDataLen, tbHistDataOut, tbHistIns, tbHistP1, tbHistP2, tbHistP3, tbHistSW });
        panHistLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] { jLabel10, jLabel11, jLabel12, jLabel13, jLabel14, jLabel15, jLabel16, jLabel17, jLabel18 });
        splitter.setLeftComponent(panHist);
        tabs.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);
        panSendAPDU.setBackground(new java.awt.Color(213, 221, 224));
        try {
            tbAPDUClass.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.MaskFormatter("HH")));
        } catch (java.text.ParseException ex) {
            ex.printStackTrace();
        }
        tbAPDUClass.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        tbAPDUClass.setFont(new java.awt.Font("Tahoma", 1, 12));
        tbAPDUClass.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusGained(java.awt.event.FocusEvent evt) {
                tbAPDUClassFocusGained(evt);
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                tbAPDUClassFocusLost(evt);
            }
        });
        try {
            tbAPDUIns.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.MaskFormatter("HH")));
        } catch (java.text.ParseException ex) {
            ex.printStackTrace();
        }
        tbAPDUIns.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        tbAPDUIns.setFont(new java.awt.Font("Tahoma", 1, 12));
        tbAPDUIns.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusGained(java.awt.event.FocusEvent evt) {
                tbAPDUInsFocusGained(evt);
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                tbAPDUInsFocusLost(evt);
            }
        });
        try {
            tbAPDUP1.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.MaskFormatter("HH")));
        } catch (java.text.ParseException ex) {
            ex.printStackTrace();
        }
        tbAPDUP1.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        tbAPDUP1.setFont(new java.awt.Font("Tahoma", 1, 12));
        tbAPDUP1.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusGained(java.awt.event.FocusEvent evt) {
                tbAPDUP1FocusGained(evt);
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                tbAPDUP1FocusLost(evt);
            }
        });
        try {
            tbAPDUP2.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.MaskFormatter("HH")));
        } catch (java.text.ParseException ex) {
            ex.printStackTrace();
        }
        tbAPDUP2.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        tbAPDUP2.setFont(new java.awt.Font("Tahoma", 1, 12));
        tbAPDUP2.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusGained(java.awt.event.FocusEvent evt) {
                tbAPDUP2FocusGained(evt);
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                tbAPDUP2FocusLost(evt);
            }
        });
        try {
            tbAPDUP3.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.MaskFormatter("HH")));
        } catch (java.text.ParseException ex) {
            ex.printStackTrace();
        }
        tbAPDUP3.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        tbAPDUP3.setToolTipText("Data length");
        tbAPDUP3.setFont(new java.awt.Font("Tahoma", 1, 12));
        tbAPDUP3.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusGained(java.awt.event.FocusEvent evt) {
                tbAPDUP3FocusGained(evt);
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                tbAPDUP3FocusLost(evt);
            }
        });
        try {
            tbAPDULe.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.MaskFormatter("****")));
        } catch (java.text.ParseException ex) {
            ex.printStackTrace();
        }
        tbAPDULe.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        tbAPDULe.setFont(new java.awt.Font("Tahoma", 1, 12));
        tbAPDULe.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusGained(java.awt.event.FocusEvent evt) {
                tbAPDULeFocusGained(evt);
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                tbAPDULeFocusLost(evt);
            }
        });
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Class");
        jLabel2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("INS");
        jLabel3.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("P1");
        jLabel4.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("P2");
        jLabel5.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("Lc");
        jLabel7.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setText("Data IN (Hex)");
        jLabel8.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel9.setText("Le");
        jLabel9.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jButton1.setFont(new java.awt.Font("Tahoma", 1, 11));
        jButton1.setText("Send");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        btWizard.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/primianotucci/jsmartcardexplorer/resources/wizard.png")));
        btWizard.setText("...");
        btWizard.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btWizardActionPerformed(evt);
            }
        });
        tbAPDUDatain.setFont(new java.awt.Font("Tahoma", 1, 11));
        tbAPDUDatain.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tbAPDUDatainMouseClicked(evt);
            }
        });
        tbAPDUDatain.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                tbAPDUDatainFocusLost(evt);
            }
        });
        jLabel21.setText("Last command status:");
        javax.swing.GroupLayout panSendAPDULayout = new javax.swing.GroupLayout(panSendAPDU);
        panSendAPDU.setLayout(panSendAPDULayout);
        panSendAPDULayout.setHorizontalGroup(panSendAPDULayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panSendAPDULayout.createSequentialGroup().addContainerGap().addGroup(panSendAPDULayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jButton1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 877, Short.MAX_VALUE).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panSendAPDULayout.createSequentialGroup().addComponent(btWizard, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panSendAPDULayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(tbAPDUClass, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 30, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panSendAPDULayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE).addComponent(tbAPDUIns, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panSendAPDULayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE).addComponent(tbAPDUP1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panSendAPDULayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(tbAPDUP2, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panSendAPDULayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE).addComponent(tbAPDUP3, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panSendAPDULayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, 575, Short.MAX_VALUE).addComponent(tbAPDUDatain, javax.swing.GroupLayout.PREFERRED_SIZE, 575, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panSendAPDULayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, 46, Short.MAX_VALUE).addComponent(tbAPDULe, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))).addGroup(panSendAPDULayout.createSequentialGroup().addComponent(jLabel21).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(lbLastStatus, javax.swing.GroupLayout.DEFAULT_SIZE, 744, Short.MAX_VALUE))).addContainerGap()));
        panSendAPDULayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { jLabel2, tbAPDUClass });
        panSendAPDULayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { jLabel3, tbAPDUIns });
        panSendAPDULayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { jLabel4, tbAPDUP1 });
        panSendAPDULayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { jLabel5, tbAPDUP2 });
        panSendAPDULayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { jLabel7, tbAPDUP3 });
        panSendAPDULayout.setVerticalGroup(panSendAPDULayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panSendAPDULayout.createSequentialGroup().addContainerGap().addGroup(panSendAPDULayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(btWizard, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE).addGroup(panSendAPDULayout.createSequentialGroup().addGroup(panSendAPDULayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel2).addComponent(jLabel3).addComponent(jLabel4).addComponent(jLabel5).addComponent(jLabel7)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panSendAPDULayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(tbAPDUClass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(tbAPDUIns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(tbAPDUP1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(tbAPDUP2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(tbAPDUP3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(tbAPDUDatain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))).addGroup(panSendAPDULayout.createSequentialGroup().addGroup(panSendAPDULayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel9).addComponent(jLabel8)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(tbAPDULe, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 72, Short.MAX_VALUE).addGroup(panSendAPDULayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lbLastStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap()));
        tabs.addTab("Send APDU", panSendAPDU);
        jScrollPane2.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        listAPDU.setFont(new java.awt.Font("Monospaced", 0, 11));
        listAPDU.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listAPDU.setToolTipText("Double-click to copy into \"Send APDU\"");
        listAPDU.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                listAPDUMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(listAPDU);
        jLabel19.setFont(new java.awt.Font("Tahoma", 0, 10));
        jLabel19.setForeground(new java.awt.Color(102, 102, 102));
        jLabel19.setText("Double-click to copy into \"Send APDU\"");
        jLabel19.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        javax.swing.GroupLayout panSavedAPDULayout = new javax.swing.GroupLayout(panSavedAPDU);
        panSavedAPDU.setLayout(panSavedAPDULayout);
        panSavedAPDULayout.setHorizontalGroup(panSavedAPDULayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panSavedAPDULayout.createSequentialGroup().addContainerGap().addGroup(panSavedAPDULayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 877, Short.MAX_VALUE).addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap()));
        panSavedAPDULayout.setVerticalGroup(panSavedAPDULayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panSavedAPDULayout.createSequentialGroup().addComponent(jLabel19).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 173, Short.MAX_VALUE).addContainerGap()));
        tabs.addTab("Saved APDUs", panSavedAPDU);
        jLabel20.setFont(new java.awt.Font("Tahoma", 0, 10));
        jLabel20.setForeground(new java.awt.Color(102, 102, 102));
        jLabel20.setText("Sent data");
        jLabel20.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        tbSentData.setColumns(20);
        tbSentData.setEditable(false);
        tbSentData.setFont(new java.awt.Font("Monospaced", 0, 11));
        tbSentData.setRows(5);
        scrollSentData.setViewportView(tbSentData);
        tbRecvData.setColumns(20);
        tbRecvData.setEditable(false);
        tbRecvData.setFont(new java.awt.Font("Monospaced", 0, 11));
        tbRecvData.setRows(5);
        scrollRecvData.setViewportView(tbRecvData);
        jLabel22.setFont(new java.awt.Font("Tahoma", 0, 10));
        jLabel22.setForeground(new java.awt.Color(102, 102, 102));
        jLabel22.setText("Received data");
        jLabel22.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        javax.swing.GroupLayout panDataViewLayout = new javax.swing.GroupLayout(panDataView);
        panDataView.setLayout(panDataViewLayout);
        panDataViewLayout.setHorizontalGroup(panDataViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panDataViewLayout.createSequentialGroup().addContainerGap().addGroup(panDataViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel20).addComponent(scrollSentData, javax.swing.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panDataViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(scrollRecvData, javax.swing.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE).addComponent(jLabel22)).addContainerGap()));
        panDataViewLayout.setVerticalGroup(panDataViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panDataViewLayout.createSequentialGroup().addGroup(panDataViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel20).addComponent(jLabel22)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panDataViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(scrollRecvData, javax.swing.GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE).addComponent(scrollSentData, javax.swing.GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE))));
        tabs.addTab("Data view", panDataView);
        treeFS.setModel(null);
        treeFS.setRootVisible(false);
        treeFS.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {

            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                treeFSValueChanged(evt);
            }
        });
        jScrollPane5.setViewportView(treeFS);
        tbFSData.setColumns(20);
        tbFSData.setEditable(false);
        tbFSData.setFont(new java.awt.Font("Monospaced", 0, 11));
        tbFSData.setRows(5);
        scrollFSData.setViewportView(tbFSData);
        javax.swing.GroupLayout panFSLayout = new javax.swing.GroupLayout(panFS);
        panFS.setLayout(panFSLayout);
        panFSLayout.setHorizontalGroup(panFSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panFSLayout.createSequentialGroup().addContainerGap().addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 274, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(scrollFSData, javax.swing.GroupLayout.DEFAULT_SIZE, 596, Short.MAX_VALUE).addContainerGap()));
        panFSLayout.setVerticalGroup(panFSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panFSLayout.createSequentialGroup().addContainerGap().addGroup(panFSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(scrollFSData, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE).addComponent(jScrollPane5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE)).addContainerGap()));
        tabs.addTab("FileSystem Data (Experimental)", panFS);
        splitter.setRightComponent(tabs);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(splitter, javax.swing.GroupLayout.DEFAULT_SIZE, 906, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(splitter, javax.swing.GroupLayout.DEFAULT_SIZE, 478, Short.MAX_VALUE)));
        pack();
    }

    private void btReloadReadersActionPerformed(java.awt.event.ActionEvent evt) {
        updateReaderCombo();
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            sendAPDU();
        } catch (Exception ex) {
            reportException(ex);
        }
    }

    private void btConnectActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            if (btConnect.isSelected()) {
                selectCurrentReader();
                lbStatus.setText(SC.connect(getProtocolString()));
                tbCardATR.setText(SC.getCurrentCardATR());
                lbStatus.setBackground(Color.GREEN);
            } else {
                SC.disconnect();
                lbStatus.setBackground(Color.RED);
                lbStatus.setText("Disconnected");
            }
        } catch (Exception ex) {
            lbStatus.setBackground(Color.RED);
            lbStatus.setText("Error");
            btConnect.setSelected(!btConnect.isSelected());
            reportException(ex);
        }
    }

    private void cbReaderItemStateChanged(java.awt.event.ItemEvent evt) {
        selectCurrentReader();
    }

    private void tabAPDUMouseClicked(java.awt.event.MouseEvent evt) {
        updateSelectedTableRow();
        if (evt.getButton() > 1) mnuTable.show(tabAPDU, evt.getX(), evt.getY());
    }

    private void mnuSaveAPDUActionPerformed(java.awt.event.ActionEvent evt) {
        CommandAPDU apdu = getTableAPDU().Cmd;
        if (apdu == null) return;
        String mnemonic = JOptionPane.showInputDialog("APDU mnemonic");
        if (mnemonic == null) return;
        mnemonic = mnemonic.replace(' ', '_');
        listAPDUmod.addElement(new APDUCommandBoxing(apdu, mnemonic));
        listAPDU.updateUI();
    }

    private void mnuClearHistActionPerformed(java.awt.event.ActionEvent evt) {
        ((DefaultTableModel) tabAPDU.getModel()).setRowCount(0);
    }

    private void btWizardActionPerformed(java.awt.event.ActionEvent evt) {
        mnuWizards.show(btWizard, 0, 0);
    }

    private void listAPDUMouseClicked(java.awt.event.MouseEvent evt) {
        if (evt.getButton() > 1) mnuSavedAPDU.show(listAPDU, evt.getX(), evt.getY()); else if (evt.getClickCount() > 1) restoreAPDUFromHistory();
    }

    private void selectAll(JTextField iObj) {
        iObj.setText(iObj.getText());
        iObj.selectAll();
    }

    private void tbAPDUClassFocusGained(java.awt.event.FocusEvent evt) {
        selectAll(tbAPDUClass);
    }

    private void tbAPDUInsFocusGained(java.awt.event.FocusEvent evt) {
        selectAll(tbAPDUIns);
    }

    private void tbAPDUP1FocusGained(java.awt.event.FocusEvent evt) {
        selectAll(tbAPDUP1);
    }

    private void tbAPDUP2FocusGained(java.awt.event.FocusEvent evt) {
        selectAll(tbAPDUP2);
    }

    private void tbAPDUP3FocusGained(java.awt.event.FocusEvent evt) {
        selectAll(tbAPDUP3);
    }

    private void tbAPDULeFocusGained(java.awt.event.FocusEvent evt) {
        selectAll(tbAPDULe);
    }

    private void tbAPDUP3FocusLost(java.awt.event.FocusEvent evt) {
        if (tbAPDUP3.getText().trim().length() == 0) tbAPDUP3.setValue(null);
        tbAPDUP3.setBackground(Color.WHITE);
    }

    private void tbAPDULeFocusLost(java.awt.event.FocusEvent evt) {
        if (tbAPDULe.getText().trim().length() == 0) tbAPDULe.setValue(null);
    }

    private void tbAPDUDatainFocusLost(java.awt.event.FocusEvent evt) {
        try {
            byte[] data = StringUtil.stringToByteArr(tbAPDUDatain.getText());
            if (data.length > 0) {
                tbAPDUP3.setText(StringUtil.byteToHex(data.length));
                tbAPDUP3.setBackground(Color.YELLOW);
            }
        } catch (Exception ex) {
        }
    }

    private void treeFSValueChanged(javax.swing.event.TreeSelectionEvent evt) {
        DefaultMutableTreeNode selnode = (DefaultMutableTreeNode) treeFS.getSelectionPath().getLastPathComponent();
        Object sel = selnode.getUserObject();
        if (!(sel instanceof ISO7816FileSystem.EF)) return;
        ISO7816FileSystem.EF ef = (ISO7816FileSystem.EF) sel;
        tbFSData.setText(StringUtil.hexDump(ef.getData(), 16));
    }

    private void btSaveProjectActionPerformed(java.awt.event.ActionEvent evt) {
        saveProject();
    }

    private void btLoadProjectActionPerformed(java.awt.event.ActionEvent evt) {
        loadProject();
    }

    private void mnuResendActionPerformed(java.awt.event.ActionEvent evt) {
        restoreAPDUFromHistory();
    }

    private void mnuDelAPDUActionPerformed(java.awt.event.ActionEvent evt) {
        if (listAPDU.getSelectedIndex() >= 0) listAPDUmod.removeElementAt(listAPDU.getSelectedIndex());
    }

    private void tbAPDUClassFocusLost(java.awt.event.FocusEvent evt) {
        if (tbAPDUClass.getText().trim().length() == 0) tbAPDUClass.setText("00");
    }

    private void tbAPDUInsFocusLost(java.awt.event.FocusEvent evt) {
        if (tbAPDUIns.getText().trim().length() == 0) tbAPDUIns.setText("00");
    }

    private void tbAPDUP1FocusLost(java.awt.event.FocusEvent evt) {
        if (tbAPDUP1.getText().trim().length() == 0) tbAPDUP1.setText("00");
    }

    private void tbAPDUP2FocusLost(java.awt.event.FocusEvent evt) {
        if (tbAPDUP2.getText().trim().length() == 0) tbAPDUP2.setText("00");
    }

    private void tbAPDUDatainMouseClicked(java.awt.event.MouseEvent evt) {
        if (evt.getClickCount() > 1) insertStringData();
    }

    private javax.swing.JToggleButton btConnect;

    private javax.swing.JButton btLoadProject;

    private javax.swing.JButton btReloadReaders;

    private javax.swing.JButton btSaveProject;

    private javax.swing.JButton btWizard;

    private javax.swing.JComboBox cbProto;

    private javax.swing.JComboBox cbReader;

    private javax.swing.JButton jButton1;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel10;

    private javax.swing.JLabel jLabel11;

    private javax.swing.JLabel jLabel12;

    private javax.swing.JLabel jLabel13;

    private javax.swing.JLabel jLabel14;

    private javax.swing.JLabel jLabel15;

    private javax.swing.JLabel jLabel16;

    private javax.swing.JLabel jLabel17;

    private javax.swing.JLabel jLabel18;

    private javax.swing.JLabel jLabel19;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel20;

    private javax.swing.JLabel jLabel21;

    private javax.swing.JLabel jLabel22;

    private javax.swing.JLabel jLabel23;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JLabel jLabel5;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JLabel jLabel7;

    private javax.swing.JLabel jLabel8;

    private javax.swing.JLabel jLabel9;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JScrollPane jScrollPane5;

    private javax.swing.JSeparator jSeparator1;

    private javax.swing.JSeparator jSeparator2;

    private javax.swing.JLabel lbLastStatus;

    private javax.swing.JTextField lbStatus;

    private javax.swing.JList listAPDU;

    private javax.swing.JMenuItem mnuClearHist;

    private javax.swing.JMenuItem mnuDelAPDU;

    private javax.swing.JMenuItem mnuResend;

    private javax.swing.JMenuItem mnuResendAPDU;

    private javax.swing.JMenuItem mnuSaveAPDU;

    private javax.swing.JPopupMenu mnuSavedAPDU;

    private javax.swing.JPopupMenu mnuTable;

    private javax.swing.JPopupMenu mnuWizards;

    private javax.swing.JPanel panDataView;

    private javax.swing.JPanel panFS;

    private javax.swing.JPanel panHist;

    private javax.swing.JPanel panSavedAPDU;

    private javax.swing.JPanel panSendAPDU;

    private javax.swing.JScrollPane scrollFSData;

    private javax.swing.JScrollPane scrollRecvData;

    private javax.swing.JScrollPane scrollSentData;

    private javax.swing.JSplitPane splitter;

    private javax.swing.JTable tabAPDU;

    private javax.swing.JTabbedPane tabs;

    private javax.swing.JFormattedTextField tbAPDUClass;

    private javax.swing.JTextField tbAPDUDatain;

    private javax.swing.JFormattedTextField tbAPDUIns;

    private javax.swing.JFormattedTextField tbAPDULe;

    private javax.swing.JFormattedTextField tbAPDUP1;

    private javax.swing.JFormattedTextField tbAPDUP2;

    private javax.swing.JFormattedTextField tbAPDUP3;

    private javax.swing.JTextField tbCardATR;

    private javax.swing.JTextArea tbFSData;

    private javax.swing.JTextField tbHistClass;

    private javax.swing.JTextField tbHistDataIn;

    private javax.swing.JTextField tbHistDataLen;

    private javax.swing.JTextField tbHistDataOut;

    private javax.swing.JTextField tbHistIns;

    private javax.swing.JTextField tbHistP1;

    private javax.swing.JTextField tbHistP2;

    private javax.swing.JTextField tbHistP3;

    private javax.swing.JTextField tbHistSW;

    private javax.swing.JTextArea tbRecvData;

    private javax.swing.JTextArea tbSentData;

    private javax.swing.JTree treeFS;

    class APDUSet {

        public CommandAPDU Cmd;

        public ResponseAPDU Resp;
    }
}
