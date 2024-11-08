package console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import channel.GrabChannelData;

/**
 *
 * @author Michael Hanns
 */
public class ServerBrowser extends javax.swing.JDialog {

    private static final long serialVersionUID = 6463127852149413989L;

    private ClientObserver console;

    String[][] serverDets;

    private javax.swing.table.TableModel serversModel;

    /** Creates new form ServerBrowser */
    public ServerBrowser(java.awt.Frame parent, boolean modal, ClientObserver cons) {
        super(parent, modal);
        this.console = cons;
        serverDets = new String[0][8];
        setServersTableModel();
        initComponents();
        setServersTableColumns();
    }

    private void setServersTableModel() {
        serversModel = new javax.swing.table.DefaultTableModel(new String[] { "IP", "Port", "Server Name", "Module", "Version", "#", "Max" }, serverDets.length) {

            private static final long serialVersionUID = -1130281695107395399L;

            boolean[] canEdit = new boolean[] { false, false, false, false, false };

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        };
    }

    private void setServersTableColumns() {
        serversTable.getColumnModel().getColumn(0).setMinWidth(100);
        serversTable.getColumnModel().getColumn(1).setMaxWidth(40);
        serversTable.getColumnModel().getColumn(2).setMinWidth(170);
        serversTable.getColumnModel().getColumn(3).setMinWidth(170);
        serversTable.getColumnModel().getColumn(4).setMaxWidth(45);
        serversTable.getColumnModel().getColumn(5).setMaxWidth(40);
        serversTable.getColumnModel().getColumn(6).setMaxWidth(40);
    }

    private void setDescription(int serverNo) {
        if (serverDets.length >= serverNo) {
            if (serverDets[serverNo][4] != null) {
                descriptionBox.setText(serverDets[serverNo][4]);
                descriptionBox.setCaretPosition(0);
            }
        }
    }

    private void initComponents() {
        getServersButton = new javax.swing.JButton();
        ipField = new javax.swing.JFormattedTextField();
        connectButton = new javax.swing.JButton();
        serverPane = new javax.swing.JScrollPane();
        serversTable = new javax.swing.JTable();
        descBoxPane = new javax.swing.JScrollPane();
        descriptionBox = new javax.swing.JTextArea();
        descriptionLabel = new javax.swing.JLabel();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("WITNA Server Browser");
        setLocationByPlatform(true);
        setResizable(false);
        getServersButton.setText("Get Servers");
        getServersButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                getServersButtonActionPerformed(evt);
            }
        });
        ipField.setToolTipText("Master Server IP Address");
        connectButton.setText("Connect");
        connectButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectButtonActionPerformed(evt);
            }
        });
        serverPane.setPreferredSize(new java.awt.Dimension(492, 419));
        serversTable.setModel(serversModel);
        serversTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        serversTable.setColumnSelectionAllowed(true);
        serversTable.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                serversTableMouseClicked(evt);
            }
        });
        serversTable.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                serversTablePropertyChange(evt);
            }
        });
        serverPane.setViewportView(serversTable);
        serversTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        descriptionBox.setColumns(20);
        descriptionBox.setEditable(false);
        descriptionBox.setLineWrap(true);
        descriptionBox.setRows(5);
        descriptionBox.setWrapStyleWord(true);
        descBoxPane.setViewportView(descriptionBox);
        descriptionLabel.setText("Description:");
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(serverPane, javax.swing.GroupLayout.DEFAULT_SIZE, 610, Short.MAX_VALUE).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addComponent(ipField, javax.swing.GroupLayout.DEFAULT_SIZE, 436, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(getServersButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(connectButton)).addComponent(descBoxPane, javax.swing.GroupLayout.DEFAULT_SIZE, 610, Short.MAX_VALUE).addComponent(descriptionLabel)).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap().addComponent(serverPane, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(descriptionLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(descBoxPane, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(ipField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(connectButton).addComponent(getServersButton)).addContainerGap()));
        ipField.setText(getMasterChannelIP());
        pack();
    }

    public static String getMasterChannelIP() {
        try {
            URL whatismyip = new URL("http://www.witna.co.uk/chanData/officialIP.html");
            InputStream stream = whatismyip.openStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(stream));
            String ip = in.readLine();
            stream.close();
            in.close();
            if (ip != null) {
                return ip;
            }
            return "127.0.0.1";
        } catch (IOException e) {
            return "127.0.0.1";
        }
    }

    private void getServersButtonActionPerformed(java.awt.event.ActionEvent evt) {
        String ipstr = ipField.getText();
        serverDets = GrabChannelData.getChannelData(ipstr);
        if (serverDets == null) {
            System.out.println("HUH HUH HUH");
            descriptionBox.setText("No WITNA channel found at " + ipstr + ".");
            serverDets = new String[0][8];
            setServersTableModel();
            serversTable.setModel(serversModel);
            setServersTableColumns();
            return;
        }
        setServersTableModel();
        serversTable.setModel(serversModel);
        setServersTableColumns();
        if (serverDets.length == 0) {
            descriptionBox.setText("No servers currently connected to channel " + ipstr + ".");
        } else {
            for (int x = 0; x < serverDets.length; x++) {
                for (int y = 0; y < 4; y++) {
                    serversTable.getModel().setValueAt(serverDets[x][y], x, y);
                }
                for (int y = 5; y < 8; y++) {
                    serversTable.getModel().setValueAt(serverDets[x][y], x, y - 1);
                }
            }
        }
    }

    private void connectButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int rowSel = serversTable.getSelectedRow();
        if (rowSel > -1) {
            String ip = serversTable.getModel().getValueAt(rowSel, 0).toString();
            String port = serversTable.getModel().getValueAt(rowSel, 1).toString();
            console.sendToShell("SERVERJOIN " + ip + " " + port);
            dispose();
        } else {
            descriptionBox.setText("You must select a server before connecting!");
        }
    }

    private void serversTablePropertyChange(java.beans.PropertyChangeEvent evt) {
    }

    private void serversTableMouseClicked(java.awt.event.MouseEvent evt) {
        if (serversTable.getSelectedRow() > -1) {
            setDescription(serversTable.getSelectedRow());
        }
    }

    private javax.swing.JButton connectButton;

    private javax.swing.JScrollPane descBoxPane;

    private javax.swing.JTextArea descriptionBox;

    private javax.swing.JLabel descriptionLabel;

    private javax.swing.JButton getServersButton;

    private javax.swing.JFormattedTextField ipField;

    private javax.swing.JScrollPane serverPane;

    private javax.swing.JTable serversTable;
}
