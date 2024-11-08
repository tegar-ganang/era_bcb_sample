package de.jlab.ui.connectionsetting;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import de.jlab.GlobalsLocator;
import de.jlab.config.ConnectionChannelConfig;
import de.jlab.config.ConnectionConfig;

/**
 * dialog for selecting the connectiontype and parameters.
 * 
 * @author Volker Raum (C) 2007
 */
@SuppressWarnings("serial")
public class ConnectionSelectionDialog extends JDialog {

    JPanel jPanelControl = new JPanel();

    JButton jButtonOK = new JButton(GlobalsLocator.translate("button-OK"));

    JButton jButtonCancel = new JButton(GlobalsLocator.translate("button-Cancel"));

    JButton jButtonAddChannel = new JButton(GlobalsLocator.translate("button-AddChannel"));

    JPanel mainPanel = new JPanel();

    boolean okPressed = false;

    List<ConnectionChannelSelectorPanel> panels = new ArrayList();

    public ConnectionSelectionDialog(Frame parent, String title) {
        super(parent, title, true);
        initUI();
    }

    private void initUI() {
        mainPanel.setLayout(new GridBagLayout());
        jPanelControl.add(jButtonOK);
        jPanelControl.add(jButtonAddChannel);
        jPanelControl.add(jButtonCancel);
        this.setContentPane(mainPanel);
        jButtonOK.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (channelNamesOK()) {
                    okPressed = true;
                    dispose();
                }
            }
        });
        jButtonCancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                okPressed = false;
                dispose();
            }
        });
        jButtonAddChannel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                addChannel();
            }
        });
    }

    private void addChannel() {
        ConnectionChannelConfig newChannelConfig = new ConnectionChannelConfig();
        ConnectionChannelSelectorPanel newPanel = new ConnectionChannelSelectorPanel(this);
        newPanel.setConnectionChannelConfig(newChannelConfig);
        panels.add(newPanel);
        reorganizePanels();
    }

    public void removePanel(ConnectionChannelSelectorPanel panel) {
        panels.remove(panel);
        reorganizePanels();
    }

    public boolean channelNamesOK() {
        boolean isOK = true;
        Set<String> names = new HashSet();
        boolean illegalChannelNameFound = false;
        for (ConnectionChannelSelectorPanel currPanel : panels) {
            String channelName = currPanel.getConnectionChannelConfig().getChannelname();
            if (channelName.length() == 0) {
                illegalChannelNameFound = true;
                break;
            }
            names.add(channelName);
        }
        if (names.size() != panels.size() || illegalChannelNameFound) {
            JOptionPane.showMessageDialog(GlobalsLocator.getMainFrame(), GlobalsLocator.translate("connection-channelnames-warning"));
            isOK = false;
        }
        return isOK;
    }

    public void setConnectionConfig(ConnectionConfig param) {
        mainPanel.removeAll();
        panels.clear();
        int row = 0;
        for (ConnectionChannelConfig channelConfig : param.getCommChannels()) {
            ConnectionChannelSelectorPanel newPanel = new ConnectionChannelSelectorPanel(this);
            newPanel.setConnectionChannelConfig(channelConfig);
            panels.add(newPanel);
            mainPanel.add(newPanel, new GridBagConstraints(0, row, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
            row++;
        }
        mainPanel.add(jPanelControl, new GridBagConstraints(0, row, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
    }

    private void reorganizePanels() {
        mainPanel.removeAll();
        int row = 0;
        for (ConnectionChannelSelectorPanel channelConfigPanel : panels) {
            mainPanel.add(channelConfigPanel, new GridBagConstraints(0, row, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
            row++;
        }
        mainPanel.add(jPanelControl, new GridBagConstraints(0, row, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
        Dimension dim = this.getSize();
        dim.height = panels.size() * 115 + 90;
        this.setSize(dim);
    }

    public ConnectionConfig getConnectionConfig() {
        ConnectionConfig newConf = new ConnectionConfig();
        for (ConnectionChannelSelectorPanel currPanel : panels) {
            newConf.addCommChannel(currPanel.getConnectionChannelConfig());
        }
        return newConf;
    }

    public boolean isOkPressed() {
        return okPressed;
    }

    public void setDefault(ConnectionConfig defaultConfig) {
    }
}
