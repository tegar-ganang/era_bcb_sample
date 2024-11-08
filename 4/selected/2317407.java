package ie.omk.jest;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import ie.omk.smpp.*;
import ie.omk.smpp.message.*;
import ie.omk.smpp.net.*;
import ie.omk.debug.Debug;

public abstract class Connection {

    Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

    Cursor waitCursor = new Cursor(Cursor.WAIT_CURSOR);

    JMenuBar menuBar;

    static String fItems[] = { "Open", "Save", "Save As", "00sep", "Exit" };

    static int fShort[] = { KeyEvent.VK_O, KeyEvent.VK_S, KeyEvent.VK_A, 0, KeyEvent.VK_X };

    static String cItems[] = { "New", "Close", "00sep", "Force Close" };

    static int cShort[] = { KeyEvent.VK_W, KeyEvent.VK_L, 0, KeyEvent.VK_F };

    JButton details;

    JButton hex;

    JButton close;

    JCheckBox ackEnquire;

    JCheckBox ackDeliver;

    JList incoming;

    JList outgoing;

    DefaultListModel inTable;

    DefaultListModel outTable;

    JLabel connState;

    JLabel linkState;

    String cStates[] = { "Unbound", "Bound" };

    String lStates[] = { "Disconnected", "Connected" };

    InetAddress tcp_addr = null;

    int portNum = -1;

    SmscLink link = null;

    SmppConnection conn = null;

    ie.omk.jest.Jest jparent = null;

    boolean isTcp = true;

    boolean isTransmitter = true;

    boolean messageOps = true;

    boolean networkOps = true;

    String fileName = null;

    public Connection(ie.omk.jest.Jest jparent, boolean tcp, boolean trn) {
        if (jparent == null) throw new NullPointerException("jparent Frame is null");
        this.jparent = jparent;
        isTcp = tcp;
        isTransmitter = trn;
        inTable = new DefaultListModel();
        outTable = new DefaultListModel();
    }

    public final boolean openConn() {
        boolean retval = true;
        jparent.setCursor(waitCursor);
        try {
            if (isTcp) {
                String p = jparent.port.getText();
                try {
                    portNum = Integer.parseInt(p);
                } catch (NumberFormatException nx) {
                    portNum = -1;
                }
                tcp_addr = InetAddress.getByName((String) jparent.host.getSelectedItem());
                if (portNum == -1) link = new TcpLink(tcp_addr); else link = new TcpLink(tcp_addr, portNum);
                link.open();
                linkState.setText(lStates[1]);
            } else {
                jparent.setCursor(defaultCursor);
                throw new SMPPException("X.25 Connectivity Not implemented.");
            }
        } catch (UnknownHostException ux) {
            String errMsg = new String("Unknown Host " + (String) jparent.host.getSelectedItem());
            JOptionPane.showMessageDialog(jparent, errMsg, "Host not found", JOptionPane.ERROR_MESSAGE);
            retval = false;
        } catch (IOException ix) {
            String msg = new String("Network error: " + ix.getMessage());
            JOptionPane.showMessageDialog(jparent, msg, "Network error", JOptionPane.ERROR_MESSAGE);
            retval = false;
        }
        jparent.setCursor(defaultCursor);
        return retval;
    }

    public boolean closeConn() {
        return this.closeConn(false);
    }

    protected boolean closeConn(boolean force) {
        if (conn == null) return true;
        if (conn.isbound() && !force) {
            JOptionPane.showMessageDialog(jparent, "You must unbind from the Smsc first", "Close Network connection", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (conn.isbound() && force) {
            try {
                conn.force_unbind();
            } catch (SMPPException sx) {
                JOptionPane.showMessageDialog(jparent, sx.getMessage(), "Smpp Exception", JOptionPane.ERROR_MESSAGE);
            }
        }
        try {
            if (link != null) ;
            link.close();
        } catch (IOException ix) {
            JOptionPane.showMessageDialog(jparent, "Network error: " + ix.getMessage(), "Close Network connection", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        connState.setText(cStates[0]);
        linkState.setText(lStates[0]);
        return true;
    }

    void newConn() {
        Container pane = null;
        Dimension scn = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension d = null;
        if (conn.isbound()) {
            JOptionPane.showMessageDialog(jparent, "Cannot open a new link while bound to the Smsc.", "Open network link", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (link != null && link.isConnected()) {
            JOptionPane.showMessageDialog(jparent, "Cannot open a new link while connected.", "Open network link", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int op = JOptionPane.showConfirmDialog(jparent, "This will lose all your current data.", "Are you sure?", JOptionPane.YES_NO_OPTION);
        if (op == JOptionPane.NO_OPTION) return;
        conn.reset();
        conn = null;
        link = null;
        JPanel p = jparent.initControls();
        JMenuBar m = jparent.initMenus();
        JLabel l = jparent.getLogo();
        jparent.setVisible(false);
        pane = jparent.getContentPane();
        pane.removeAll();
        pane.setLayout(new BorderLayout());
        pane.add("North", l);
        pane.add("Center", p);
        jparent.setJMenuBar(m);
        jparent.pack();
        d = jparent.getSize();
        jparent.setLocation((scn.width - d.width) / 2, (scn.height - d.height) / 2);
        jparent.setVisible(true);
    }

    public void loadTables(InputStream in) throws IOException {
        int outSize, inSize, loop;
        SMPPPacket pak;
        if (inTable == null || outTable == null) return;
        outSize = SMPPPacket.readInt(in, 4);
        for (loop = 0; loop < outSize; loop++) {
            pak = SMPPPacket.readPacket(in);
            outTable.addElement(pak);
        }
        inSize = SMPPPacket.readInt(in, 4);
        for (loop = 0; loop < inSize; loop++) {
            pak = SMPPPacket.readPacket(in);
            inTable.addElement(pak);
        }
    }

    public synchronized void fileSaveAs(boolean as) {
        FileDialog d = null;
        File file = null;
        int op, inSize, outSize;
        FileOutputStream out;
        Enumeration e;
        if (as || fileName == null) {
            d = new FileDialog(jparent, "Save As...", FileDialog.SAVE);
            d.setFile("*.jst");
            d.setDirectory(".");
            d.setVisible(true);
            fileName = d.getFile();
            if (fileName == null) return;
        }
        file = new File(fileName);
        if (file.exists()) {
            op = JOptionPane.showConfirmDialog(jparent, "File " + fileName + "already exists.  Overwrite?", "File exists", JOptionPane.YES_NO_OPTION);
            if (op == JOptionPane.NO_OPTION) return;
        }
        try {
            out = new FileOutputStream(fileName);
            inSize = inTable.size();
            outSize = inTable.size();
            if (this instanceof Transmitter) out.write((int) 't'); else out.write((int) 'r');
            SMPPPacket.writeInt(outSize, 4, out);
            e = outTable.elements();
            while (e.hasMoreElements()) {
                ((SMPPPacket) e.nextElement()).writeTo(out);
            }
            SMPPPacket.writeInt(inSize, 4, out);
            e = inTable.elements();
            while (e.hasMoreElements()) {
                ((SMPPPacket) e.nextElement()).writeTo(out);
            }
            out.close();
            JOptionPane.showMessageDialog(jparent, "File saved successfully.", "File save", JOptionPane.PLAIN_MESSAGE);
        } catch (IOException ix) {
            JOptionPane.showMessageDialog(jparent, ix.getMessage(), "Save error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public final JPanel initControls() {
        incoming = new JList(inTable);
        outgoing = new JList(outTable);
        incoming.setPrototypeCellValue("query_msg_details_resp");
        outgoing.setPrototypeCellValue("query_msg_details");
        IL il = new IL();
        incoming.addListSelectionListener(il);
        outgoing.addListSelectionListener(il);
        hex = new JButton("Hex dump...");
        details = new JButton("Packet details...");
        close = new JButton("Close connection");
        ackEnquire = new JCheckBox("Ack Link Tests");
        ackDeliver = new JCheckBox("Ack Messages");
        if (conn == null) {
            ackEnquire.setSelected(true);
            ackDeliver.setSelected(false);
        } else {
            ackEnquire.setSelected(conn.isAckingLinks());
            ackDeliver.setSelected(conn.isAckingMessages());
        }
        hex.setToolTipText("Hex dump of current packet");
        details.setToolTipText("Details of current packet");
        close.setToolTipText("Close the network link");
        ackEnquire.setToolTipText("Automatically ack link queries");
        ackDeliver.setToolTipText("Automatically ack Delivered messages");
        ML ml = new ML();
        hex.addMouseListener(ml);
        details.addMouseListener(ml);
        close.addMouseListener(ml);
        ackEnquire.addMouseListener(ml);
        ackDeliver.addMouseListener(ml);
        JScrollPane p2 = new JScrollPane(incoming);
        JScrollPane p3 = new JScrollPane(outgoing);
        Border b1 = BorderFactory.createLoweredBevelBorder();
        p2.setBorder(BorderFactory.createTitledBorder(b1, "Incoming Packets", TitledBorder.CENTER, TitledBorder.TOP));
        p3.setBorder(BorderFactory.createTitledBorder(b1, "Outgoing Packets", TitledBorder.CENTER, TitledBorder.TOP));
        int connStart = (conn != null && conn.isbound()) ? 1 : 0;
        connState = new JLabel(cStates[connStart], JLabel.CENTER);
        int linkStart = (link != null && link.isConnected()) ? 1 : 0;
        linkState = new JLabel(lStates[linkStart], JLabel.CENTER);
        JPanel cp = new JPanel(new BorderLayout());
        JPanel lp = new JPanel(new BorderLayout());
        cp.setBorder(BorderFactory.createTitledBorder("SMSC Link"));
        lp.setBorder(BorderFactory.createTitledBorder("Network Link"));
        cp.add("Center", connState);
        lp.add("Center", linkState);
        JPanel p4 = new JPanel();
        p4.setLayout(new GridLayout(12, 1));
        p4.add(cp);
        p4.add(lp);
        p4.add(new JLabel(" "));
        p4.add(new JLabel(" "));
        p4.add(ackEnquire);
        p4.add(ackDeliver);
        p4.add(new JLabel(" "));
        p4.add(new JLabel(" "));
        p4.add(new JLabel(" "));
        p4.add(hex);
        p4.add(details);
        p4.add(close);
        JPanel panel = new JPanel();
        JPanel p1 = getButtonPanel();
        GridBagLayout gb = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = gbc.BOTH;
        gbc.gridwidth = 1;
        gbc.weighty = 1.0;
        gbc.weightx = 0.0;
        gb.setConstraints(p1, gbc);
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        gb.setConstraints(p2, gbc);
        gb.setConstraints(p3, gbc);
        gbc.weightx = 0.0;
        gbc.gridwidth = gbc.REMAINDER;
        gb.setConstraints(p4, gbc);
        panel.setLayout(gb);
        panel.add(p1);
        panel.add(p2);
        panel.add(p3);
        panel.add(p4);
        return panel;
    }

    public abstract JPanel getButtonPanel();

    public final JMenuBar initJMenu() {
        menuBar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenu conn = new JMenu("Connection");
        JMenu msg = getMessageMenu();
        AL al = new AL();
        JMenuItem item;
        for (int loop = 0; loop < fItems.length; loop++) {
            if (fItems[loop].equals("00sep")) file.addSeparator(); else {
                item = new JMenuItem(fItems[loop], fShort[loop]);
                item.setActionCommand(fItems[loop]);
                item.addActionListener(al);
                file.add(item);
            }
        }
        for (int loop = 0; loop < cItems.length; loop++) {
            if (cItems[loop].equals("00sep")) conn.addSeparator(); else {
                item = new JMenuItem(cItems[loop], cShort[loop]);
                item.setActionCommand(cItems[loop]);
                item.addActionListener(al);
                conn.add(item);
            }
        }
        menuBar.add(file);
        menuBar.add(conn);
        menuBar.add(msg);
        return menuBar;
    }

    public abstract JMenu getMessageMenu();

    public SMPPPacket getBindDetails() {
        String val;
        jparent.setCursor(waitCursor);
        BindPanel bp;
        Dimension scn = Toolkit.getDefaultToolkit().getScreenSize();
        BindTransmitter bt = new BindTransmitter(0x01);
        BindReceiver br = new BindReceiver(0x01);
        if (jparent instanceof ie.omk.jest.Jest) {
            Hashtable ht = ((Jest) jparent).configTable;
            val = (String) ht.get("Transmitter.system_id");
            if (val != null) bt.setSystemId(val);
            val = (String) ht.get("Transmitter.password");
            if (val != null) bt.setPassword(val);
            val = (String) ht.get("Transmitter.system_type");
            if (val != null) bt.setSystemType(val);
            val = (String) ht.get("Transmitter.ton");
            if (val != null) {
                try {
                    bt.setAddressTon(Integer.parseInt(val));
                } catch (NumberFormatException nx) {
                    ht.remove("Transmitter.ton");
                }
            }
            val = (String) ht.get("Transmitter.npi");
            if (val != null) {
                try {
                    bt.setAddressNpi(Integer.parseInt(val));
                } catch (NumberFormatException nx) {
                    ht.remove("Transmitter.npi");
                }
            }
            val = (String) ht.get("Transmitter.range");
            if (val != null) bt.setAddressRange(val);
        }
        while (true) {
            PacketDialog dlg = new PacketDialog(jparent, bt, true);
            dlg.setModal(true);
            dlg.setBounds((scn.width - 500) / 2, (scn.height - 350) / 2, 500, 350);
            dlg.setVisible(true);
            if (dlg.action == dlg.CANCEL) {
                jparent.setCursor(defaultCursor);
                dlg.dispose();
                return null;
            }
            try {
                bp = (BindPanel) dlg.ppanel;
            } catch (ClassCastException ccx) {
                JOptionPane.showMessageDialog(jparent, "An Internal Error has occurred", "Internal Class Cast error", JOptionPane.ERROR_MESSAGE);
                dlg.dispose();
                jparent.setCursor(defaultCursor);
                return null;
            }
            if (bp.f_sysId.getText().equals("")) {
                JOptionPane.showMessageDialog(jparent, "System Id cannot be blank", "Input error", JOptionPane.ERROR_MESSAGE);
                dlg.dispose();
                continue;
            } else if (bp.f_password.getText().equals("")) {
                JOptionPane.showMessageDialog(jparent, "Password field cannot be blank", "Input error", JOptionPane.ERROR_MESSAGE);
                dlg.dispose();
                continue;
            } else if (bp.f_sysType.getText().equals("")) {
                JOptionPane.showMessageDialog(jparent, "System type field cannot be blank", "Input error", JOptionPane.ERROR_MESSAGE);
                dlg.dispose();
                continue;
            }
            try {
                if (this instanceof Transmitter) {
                    br = null;
                    bt.setSystemId(bp.f_sysId.getText());
                    bt.setPassword(bp.f_password.getText());
                    bt.setSystemType(bp.f_sysType.getText());
                    String ston = (String) bp.f_ton.getSelectedItem();
                    String snpi = (String) bp.f_npi.getSelectedItem();
                    int ton = SmeAddress.getTonValue(ston);
                    int npi = SmeAddress.getNpiValue(snpi);
                    bt.setAddressTon(ton);
                    bt.setAddressNpi(npi);
                    bt.setAddressRange(bp.f_addrRange.getText());
                } else {
                    bt = null;
                    br.setSystemId(bp.f_sysId.getText());
                    br.setPassword(bp.f_password.getText());
                    br.setSystemType(bp.f_sysType.getText());
                    String ston = (String) bp.f_ton.getSelectedItem();
                    String snpi = (String) bp.f_npi.getSelectedItem();
                    int ton = SmeAddress.getTonValue(ston);
                    int npi = SmeAddress.getNpiValue(snpi);
                    br.setAddressTon(ton);
                    br.setAddressNpi(npi);
                    br.setAddressRange(bp.f_addrRange.getText());
                }
                dlg.dispose();
                break;
            } catch (SMPPException x) {
                JOptionPane.showMessageDialog(jparent, x.getMessage(), "Smpp Exception", JOptionPane.ERROR_MESSAGE);
                dlg.dispose();
                continue;
            }
        }
        if (bt == null) return br; else return bt;
    }

    public final void unbind() {
        if (conn == null || !conn.isbound()) {
            JOptionPane.showMessageDialog(jparent, "You are not currently bound to the Smsc", "Unbind", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (link == null || !link.isConnected()) {
            JOptionPane.showMessageDialog(jparent, "You are not currently connected to the Smsc", "Unbind", JOptionPane.WARNING_MESSAGE);
            return;
        }
        jparent.setCursor(waitCursor);
        Unbind ubd = new Unbind(conn.currentPacket());
        PacketDialog dlg = new PacketDialog(jparent, ubd, true);
        dlg.setModal(true);
        dlg.pack();
        Dimension scn = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension d = dlg.getSize();
        dlg.setLocation((scn.width - d.width) / 2, (scn.height - d.height) / 2);
        dlg.setVisible(true);
        if (dlg.action == dlg.CANCEL) return;
        try {
            conn.unbind();
            outTable.addElement(conn.getOutwardPacket(ubd.getSeqNo()));
            jparent.getGlassPane().setCursor(waitCursor);
            jparent.getGlassPane().setVisible(true);
        } catch (SMPPException sx) {
            JOptionPane.showMessageDialog(jparent, sx.getMessage(), "Smpp Exception", JOptionPane.WARNING_MESSAGE);
        } catch (IOException ix) {
            JOptionPane.showMessageDialog(jparent, ix.getMessage(), "I/O Error", JOptionPane.ERROR_MESSAGE);
        }
        jparent.setCursor(defaultCursor);
    }

    public SMPPPacket getSelectedPacket() {
        int index;
        SMPPPacket pak = null;
        index = incoming.getSelectedIndex();
        if (index != -1) pak = (SMPPPacket) inTable.elementAt(index); else {
            index = outgoing.getSelectedIndex();
            if (index == -1) return null;
            pak = (SMPPPacket) outTable.elementAt(index);
        }
        return pak;
    }

    public void showHexDump() {
        Dimension scn = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension d = null;
        SMPPPacket pak = getSelectedPacket();
        if (pak == null) return;
        String sdump = HexDump.getHexDump(pak);
        TextArea dump = new TextArea(sdump, 8, 80);
        Font f = new Font("Monospaced", Font.PLAIN, 12);
        dump.setFont(f);
        JOptionPane.showMessageDialog(jparent, dump, "Packet Hex dump", JOptionPane.PLAIN_MESSAGE);
    }

    public void showDetails() {
        Dimension scn = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension d = null;
        JDialog dlg = null;
        SMPPPacket pak = getSelectedPacket();
        if (pak == null) return;
        if (pak instanceof QueryLastMsgsResp) {
            dlg = new ListDialog(((QueryLastMsgsResp) pak).getMessageIds());
            dlg.setTitle("Message Ids");
        } else {
            dlg = new PacketDialog(jparent, pak, false);
            dlg.setTitle("Packet view: " + pak.toString());
        }
        dlg.setModal(true);
        dlg.pack();
        d = dlg.getSize();
        if (d.width > scn.width || d.height > scn.height) {
            dlg.setBounds(1, 1, scn.width - 60, scn.height - 60);
            dlg.getContentPane().doLayout();
        } else dlg.setLocation((scn.width - d.width) / 2, (scn.height - d.height) / 2);
        dlg.setVisible(true);
        dlg.dispose();
    }

    public class ML extends java.awt.event.MouseAdapter {

        public void mouseClicked(MouseEvent e) {
            Object s = e.getSource();
            if (s.equals(details)) showDetails(); else if (s.equals(hex)) showHexDump(); else if (s.equals(close)) closeConn(); else if (s.equals(ackEnquire)) conn.autoAckLink(ackEnquire.isSelected()); else if (s.equals(ackDeliver)) conn.autoAckMessages(ackDeliver.isSelected());
        }
    }

    public class AL implements java.awt.event.ActionListener {

        public void actionPerformed(ActionEvent e) {
            Component pane = null;
            String s = e.getActionCommand();
            if (s.equals("Exit")) {
                if (closeConn()) jparent.quit();
            } else if (s.equals("Open")) {
                if (conn != null && conn.isbound()) {
                    JOptionPane.showMessageDialog(jparent, "Cannot open a file while bound.", "File load error", JOptionPane.ERROR_MESSAGE);
                } else if (link != null && link.isConnected()) {
                    JOptionPane.showMessageDialog(jparent, "Cannot open a file while connected.", "File load error", JOptionPane.ERROR_MESSAGE);
                } else jparent.fileOpen();
            } else if (s.equals("Save")) {
                fileSaveAs(false);
            } else if (s.equals("Save As")) {
                fileSaveAs(true);
            }
            if (!networkOps) {
                JOptionPane.showMessageDialog(jparent, "Network operations have been disabled", "Connection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (s.equals("New")) {
                newConn();
            } else if (s.equals("Close")) {
                closeConn();
            } else if (s.equals("Force Close")) {
                int sure = 0;
                sure = JOptionPane.showConfirmDialog(jparent, "Forcing close is a Bad Thing.  Are you absolutly sure?");
                if (sure == JOptionPane.CANCEL_OPTION || sure == JOptionPane.NO_OPTION) return;
                closeConn(true);
            }
        }
    }

    public class IL implements javax.swing.event.ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            Object s = e.getSource();
            int index;
            if (s.equals(incoming)) {
                index = outgoing.getSelectedIndex();
                if (index != -1) outgoing.clearSelection();
            } else if (s.equals(outgoing)) {
                index = incoming.getSelectedIndex();
                if (index != -1) incoming.clearSelection();
            }
            return;
        }
    }
}
