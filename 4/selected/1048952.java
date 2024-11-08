package com.empower.client.view.accounting;

import java.awt.Dimension;
import java.util.ResourceBundle;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import com.empower.client.utils.LabeledBorder;
import com.empower.client.utils.WidgetProperties;
import com.empower.constants.AppClientConstants;

public class TransferMoneyFrame extends JInternalFrame {

    private JButton cancelButton;

    private JButton okButton;

    private JTextField memoTxf;

    private JLabel memoLabel;

    private JComboBox transferToNameCbx;

    private JLabel transferToNameLabel;

    private JComboBox transferToCbx;

    private JLabel transferToLabel;

    private JFormattedTextField amountTxf;

    private JLabel amountLabel;

    private JLabel transferFromNameLabel;

    private JComboBox transferFromNameCbx;

    private JComboBox transferFromCbx;

    private JLabel transferFromLabel;

    private JLabel fundTransferDateLabel;

    private JSpinner fundTransferDateSpnr;

    ResourceBundle resLbl = ResourceBundle.getBundle("com.empower.client.Label");

    ResourceBundle resBtn = ResourceBundle.getBundle("com.empower.client.Buttons");

    ResourceBundle resWindowTitles = ResourceBundle.getBundle("com.empower.client.WindowTitles");

    ImageIcon requiredIcon = new ImageIcon(getClassLoader().getResource(AppClientConstants.IMG_PKG_PATH.concat("required_field.gif")));

    public TransferMoneyFrame(String frameTitle, boolean isResizable, boolean isClosable, boolean isMaximzable, boolean isMinimizable) {
        super(frameTitle, isResizable, isClosable, isMaximzable, isMinimizable);
        ImageIcon ecsIcon = new ImageIcon(getClassLoader().getResource(AppClientConstants.IMG_PKG_PATH.concat("empower_logo.JPG")));
        setFrameIcon(ecsIcon);
        final SpringLayout springLayout = new SpringLayout();
        getContentPane().setLayout(springLayout);
        setSize(new Dimension(410, 265));
        setName("TRANSFER MONEY");
        final JPanel dataEntryPanel = new JPanel();
        dataEntryPanel.setLayout(null);
        getContentPane().add(dataEntryPanel);
        LabeledBorder compDtlsBrdr = new LabeledBorder();
        compDtlsBrdr.setTitle(null);
        dataEntryPanel.setBorder(compDtlsBrdr);
        springLayout.putConstraint(SpringLayout.SOUTH, dataEntryPanel, 200, SpringLayout.NORTH, getContentPane());
        springLayout.putConstraint(SpringLayout.NORTH, dataEntryPanel, 5, SpringLayout.NORTH, getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, dataEntryPanel, 395, SpringLayout.WEST, getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, dataEntryPanel, 5, SpringLayout.WEST, getContentPane());
        dataEntryPanel.add(getFundTransferDateLabel());
        dataEntryPanel.add(getFundTrnsfrDateSpnr());
        dataEntryPanel.add(getTransferFromLabel());
        dataEntryPanel.add(getTransferFromCbx());
        dataEntryPanel.add(getTransferFromNameCbx());
        dataEntryPanel.add(getTransferFromNameLabel());
        dataEntryPanel.add(getAmountLabel());
        dataEntryPanel.add(getAmountTxf());
        dataEntryPanel.add(getTransferToLabel());
        dataEntryPanel.add(getTransferToCbx());
        dataEntryPanel.add(getTransferToNameLabel());
        dataEntryPanel.add(getTransferToNameCbx());
        dataEntryPanel.add(getMemoLabel());
        dataEntryPanel.add(getMemoTxf());
        getContentPane().add(getOkButton());
        springLayout.putConstraint(SpringLayout.SOUTH, getOkButton(), 225, SpringLayout.NORTH, getContentPane());
        springLayout.putConstraint(SpringLayout.NORTH, getOkButton(), 200, SpringLayout.NORTH, getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, getOkButton(), 105, SpringLayout.WEST, getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, getOkButton(), 5, SpringLayout.WEST, getContentPane());
        getContentPane().add(getCancelButton());
        springLayout.putConstraint(SpringLayout.SOUTH, getCancelButton(), 225, SpringLayout.NORTH, getContentPane());
        springLayout.putConstraint(SpringLayout.NORTH, getCancelButton(), 200, SpringLayout.NORTH, getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, getCancelButton(), 395, SpringLayout.WEST, getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, getCancelButton(), 295, SpringLayout.WEST, getContentPane());
    }

    private ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }

    public JSpinner getFundTrnsfrDateSpnr() {
        if (null == fundTransferDateSpnr) {
            fundTransferDateSpnr = WidgetProperties.setJSpinnerDate(fundTransferDateSpnr);
            fundTransferDateSpnr.setBounds(10, 30, 104, 20);
        }
        return fundTransferDateSpnr;
    }

    public JLabel getFundTransferDateLabel() {
        if (fundTransferDateLabel == null) {
            fundTransferDateLabel = new JLabel();
            fundTransferDateLabel.setText("Transfer Date");
            fundTransferDateLabel.setBounds(10, 11, 129, 20);
            setRequiredIcon(fundTransferDateLabel);
            WidgetProperties.setLabelProperties(fundTransferDateLabel);
        }
        return fundTransferDateLabel;
    }

    public JLabel getTransferFromLabel() {
        if (transferFromLabel == null) {
            transferFromLabel = new JLabel();
            transferFromLabel.setText("Transfer from");
            transferFromLabel.setBounds(10, 56, 141, 20);
            setRequiredIcon(transferFromLabel);
            WidgetProperties.setLabelProperties(transferFromLabel);
        }
        return transferFromLabel;
    }

    public JComboBox getTransferFromCbx() {
        if (transferFromCbx == null) {
            transferFromCbx = new JComboBox();
            transferFromCbx.setBounds(10, 75, 141, 20);
        }
        return transferFromCbx;
    }

    public JComboBox getTransferFromNameCbx() {
        if (transferFromNameCbx == null) {
            transferFromNameCbx = new JComboBox();
            transferFromNameCbx.setBounds(175, 75, 200, 20);
        }
        return transferFromNameCbx;
    }

    public JLabel getTransferFromNameLabel() {
        if (transferFromNameLabel == null) {
            transferFromNameLabel = new JLabel();
            transferFromNameLabel.setText("Bank Name");
            transferFromNameLabel.setBounds(175, 56, 200, 20);
            setRequiredIcon(transferFromNameLabel);
            WidgetProperties.setLabelProperties(transferFromNameLabel);
        }
        return transferFromNameLabel;
    }

    public JLabel getAmountLabel() {
        if (amountLabel == null) {
            amountLabel = new JLabel();
            amountLabel.setText("Amount to transfer");
            amountLabel.setBounds(175, 11, 121, 20);
            setRequiredIcon(amountLabel);
            WidgetProperties.setLabelProperties(amountLabel);
        }
        return amountLabel;
    }

    public JFormattedTextField getAmountTxf() {
        if (amountTxf == null) {
            amountTxf = WidgetProperties.setFloatFmtToTxf(amountTxf);
            amountTxf.setBounds(175, 32, 120, 20);
            amountTxf.setName(getAmountLabel().getText());
        }
        return amountTxf;
    }

    public JLabel getTransferToLabel() {
        if (transferToLabel == null) {
            transferToLabel = new JLabel();
            transferToLabel.setText("Transfer to");
            transferToLabel.setBounds(10, 101, 141, 20);
            setRequiredIcon(transferToLabel);
            WidgetProperties.setLabelProperties(transferToLabel);
        }
        return transferToLabel;
    }

    public JComboBox getTransferToCbx() {
        if (transferToCbx == null) {
            transferToCbx = new JComboBox();
            transferToCbx.setBounds(10, 120, 141, 20);
        }
        return transferToCbx;
    }

    public JLabel getTransferToNameLabel() {
        if (transferToNameLabel == null) {
            transferToNameLabel = new JLabel();
            transferToNameLabel.setText("Bank Name");
            transferToNameLabel.setBounds(175, 101, 200, 20);
            setRequiredIcon(transferToNameLabel);
            WidgetProperties.setLabelProperties(transferToNameLabel);
        }
        return transferToNameLabel;
    }

    public JComboBox getTransferToNameCbx() {
        if (transferToNameCbx == null) {
            transferToNameCbx = new JComboBox();
            transferToNameCbx.setBounds(175, 120, 200, 20);
        }
        return transferToNameCbx;
    }

    public JLabel getMemoLabel() {
        if (memoLabel == null) {
            memoLabel = new JLabel();
            memoLabel.setText("Memo");
            memoLabel.setBounds(10, 146, 129, 20);
            WidgetProperties.setLabelProperties(memoLabel);
        }
        return memoLabel;
    }

    public JTextField getMemoTxf() {
        if (memoTxf == null) {
            memoTxf = new JTextField();
            memoTxf.setBounds(10, 164, 365, 20);
        }
        return memoTxf;
    }

    public JButton getOkButton() {
        if (okButton == null) {
            okButton = new JButton();
            okButton.setText("OK");
        }
        return okButton;
    }

    public JButton getCancelButton() {
        if (cancelButton == null) {
            cancelButton = new JButton();
            cancelButton.setText("Cancel");
        }
        return cancelButton;
    }

    private void setRequiredIcon(JLabel label) {
        if (label != null) {
            label.setHorizontalTextPosition(SwingConstants.LEFT);
            label.setIcon(requiredIcon);
        }
    }
}
