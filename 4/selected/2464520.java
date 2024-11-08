package de.jlab.ui.connectionsetting;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import de.jlab.GlobalsLocator;
import de.jlab.communication.tcpip.TcpipBoardInterface;
import de.jlab.config.ConnectionChannelConfig;

/**
 * Panel for entering the Connection type and the parameters.
 * 
 * @author Volker Raum (C) 2007
 */
@SuppressWarnings("serial")
public class ConnectionChannelSelectorPanel extends JPanel {

    ButtonGroup conntypeGroup = new ButtonGroup();

    JLabel jLabelConnTCPIPHostname = new JLabel(GlobalsLocator.translate("connection-tcpip-hostname"));

    JTextField jTextFieldTCPIPHostname = new JTextField(20);

    JLabel jLabelConnTCPIPPort = new JLabel(GlobalsLocator.translate("connection-tcpip-port"));

    JTextField jTextFieldTCPIPPort = new JTextField(5);

    JLabel jLabelChannelName = new JLabel(GlobalsLocator.translate("connection-channel-name"));

    JTextField jTextFieldChannelName = new JTextField(5);

    JLabel jLabelConnRXTXSerialInterface = new JLabel(GlobalsLocator.translate("connection-interfacename"));

    JLabel jLabelConnUserInterfaceValue = new JLabel();

    JRadioButton jRadioButtonConnTCPIP = new JRadioButton("TCPIP");

    JRadioButton jRadioButtonConnRXTX = new JRadioButton("RXTX");

    JPanel jPanelTCPIPPanel = new JPanel();

    JPanel jPanelRXTXPanel = new JPanel();

    JComboBox jComboBoxComPorts = new JComboBox();

    ConnectionChannelConfig originalConnParam = null;

    JButton jButtonRemove = new JButton(GlobalsLocator.translate("connection-button-remove-channel"));

    ConnectionSelectionDialog dialog = null;

    public ConnectionChannelSelectorPanel(ConnectionSelectionDialog dialog) {
        this.dialog = dialog;
        initUI();
    }

    private void initUI() {
        jPanelTCPIPPanel.add(jLabelConnTCPIPHostname);
        jPanelTCPIPPanel.add(jTextFieldTCPIPHostname);
        jPanelTCPIPPanel.add(jLabelConnTCPIPPort);
        jPanelTCPIPPanel.add(jTextFieldTCPIPPort);
        jPanelRXTXPanel.add(jLabelConnRXTXSerialInterface);
        jPanelRXTXPanel.add(jComboBoxComPorts);
        this.setLayout(new GridBagLayout());
        this.add(jLabelChannelName, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
        this.add(jTextFieldChannelName, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
        this.add(jButtonRemove, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
        this.add(jRadioButtonConnTCPIP, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
        this.add(jPanelTCPIPPanel, new GridBagConstraints(1, 1, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
        this.add(jRadioButtonConnRXTX, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
        this.add(jPanelRXTXPanel, new GridBagConstraints(1, 2, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
        conntypeGroup.add(jRadioButtonConnRXTX);
        conntypeGroup.add(jRadioButtonConnTCPIP);
        ConnTypeListener connTypeListener = new ConnTypeListener();
        jRadioButtonConnTCPIP.addActionListener(connTypeListener);
        jRadioButtonConnRXTX.addActionListener(connTypeListener);
        jRadioButtonConnTCPIP.setSelected(true);
        connTypeListener.actionPerformed(null);
        try {
            Class.forName("gnu.io.CommPortIdentifier");
            ComPortHelper.initComboBoxPorts(jComboBoxComPorts);
        } catch (Throwable e) {
            jRadioButtonConnRXTX.setEnabled(false);
            jRadioButtonConnTCPIP.setSelected(true);
        }
        jButtonRemove.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removePanel();
            }
        });
    }

    private void removePanel() {
        dialog.removePanel(this);
    }

    public void setConnectionChannelConfig(ConnectionChannelConfig connParam) {
        originalConnParam = connParam;
        jTextFieldChannelName.setText(connParam.getChannelname());
        if (connParam.getParams() == null) {
            jTextFieldTCPIPHostname.setText("<HOST>");
            jTextFieldTCPIPPort.setText("<PORT>");
            jRadioButtonConnTCPIP.setSelected(true);
        } else if (connParam.getClassname().equals(TcpipBoardInterface.class.getName())) {
            if (connParam.getParams().get(0).getName().equals(TcpipBoardInterface.HOSTNAME_CONFIG_NAME)) {
                jTextFieldTCPIPHostname.setText(connParam.getParams().get(0).getValue());
                jTextFieldTCPIPPort.setText(connParam.getParams().get(1).getValue());
            } else {
                jTextFieldTCPIPHostname.setText(connParam.getParams().get(1).getValue());
                jTextFieldTCPIPPort.setText(connParam.getParams().get(2).getValue());
            }
            jRadioButtonConnTCPIP.setSelected(true);
        } else if (connParam.getClassname().equals("de.jlab.communication.rxtx.RxTxBoardInterface")) {
            jComboBoxComPorts.setSelectedItem(connParam.getParams().get(0).getValue());
            jRadioButtonConnRXTX.setSelected(true);
        }
        this.setBorder(BorderFactory.createTitledBorder(connParam.getChannelname()));
    }

    public ConnectionChannelConfig getConnectionChannelConfig() {
        ConnectionChannelConfig param = new ConnectionChannelConfig();
        if (jRadioButtonConnRXTX.isSelected()) {
            param.setClassname("de.jlab.communication.rxtx.RxTxBoardInterface");
            param.addParam("Interface", jComboBoxComPorts.getSelectedItem().toString());
        } else if (jRadioButtonConnTCPIP.isSelected()) {
            param.setClassname(TcpipBoardInterface.class.getName());
            param.addParam(TcpipBoardInterface.HOSTNAME_CONFIG_NAME, jTextFieldTCPIPHostname.getText());
            param.addParam(TcpipBoardInterface.PORT_CONFIG_NAME, jTextFieldTCPIPPort.getText());
        }
        param.setChannelname(jTextFieldChannelName.getText());
        return param;
    }

    class ConnTypeListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            jPanelTCPIPPanel.setEnabled(false);
            jPanelRXTXPanel.setEnabled(false);
            jPanelTCPIPPanel.setEnabled(jRadioButtonConnTCPIP.isSelected());
            jPanelRXTXPanel.setEnabled(jRadioButtonConnRXTX.isSelected());
        }
    }
}
