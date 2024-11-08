package de.renier.vdr.channel.editor;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import de.renier.vdr.channel.Channel;
import de.renier.vdr.channel.ChannelCategory;
import de.renier.vdr.channel.ChannelElement;
import de.renier.vdr.channel.editor.container.RegularExpressionTextField;
import de.renier.vdr.channel.editor.util.Utils;

/**
 * ChannelPropertyPanel
 * 
 * @author <a href="mailto:editor@renier.de">Renier Roth</a>
 */
public class ChannelPropertyPanel extends JPanel {

    private static final long serialVersionUID = -2707429245092839974L;

    private JPanel jPanel = null;

    private JPanel jPanel1 = null;

    private JLabel nameLabel = null;

    private JTextField nameTextField = null;

    private JLabel frequenzLabel = null;

    private JTextField frequenzTextField = null;

    private JLabel sourceLabel = null;

    private JTextField sourceTextField = null;

    private JLabel parameterLabel = null;

    private JTextField parameterTextField = null;

    private JLabel symbolrateLabel = null;

    private JTextField symbolrateTextField = null;

    private JButton jButton = null;

    private JLabel vpidLabel = null;

    private JTextField vpidTextField = null;

    private JLabel apidLabel = null;

    private JTextField apidTextField = null;

    private JLabel tpidLabel = null;

    private JLabel sidLabel = null;

    private JLabel tidLabel = null;

    private JLabel caidLabel = null;

    private JLabel nidLabel = null;

    private JLabel ridLabel = null;

    private JLabel jLabel = null;

    private JTextField tpidTextField = null;

    private JTextField caidTextField = null;

    private JTextField sidTextField = null;

    private JTextField nidTextField = null;

    private JTextField tidTextField = null;

    private JTextField ridTextField = null;

    private ChannelElement channelElement = null;

    private JLabel bouqetLabel = null;

    private JTextField bouqetTextField = null;

    private JLabel startnrLabel = null;

    private JTextField startnrTextField = null;

    private boolean createMode = false;

    private JLabel aliasLabel = null;

    private JTextField aliasTextField = null;

    private JLabel aliasInfoLabel = null;

    /**
   * This is the default constructor
   */
    public ChannelPropertyPanel() {
        super();
        initialize();
    }

    /**
   * constructor
   */
    public ChannelPropertyPanel(boolean createMode) {
        super();
        this.createMode = createMode;
        initialize();
    }

    /**
   * This method initializes this
   * 
   * @return void
   */
    private void initialize() {
        this.setLayout(new BorderLayout());
        this.setSize(440, 276);
        this.add(getJPanel(), java.awt.BorderLayout.NORTH);
        if (createMode) {
            setVisibleFlagForChannelCategoryFields(false);
        } else {
            this.add(getJPanel1(), java.awt.BorderLayout.SOUTH);
            updateFields(ChannelEditor.nothingSelectedChannel);
        }
    }

    /**
   * This method initializes jPanel
   * 
   * @return javax.swing.JPanel
   */
    private JPanel getJPanel() {
        if (jPanel == null) {
            aliasInfoLabel = new JLabel();
            aliasLabel = new JLabel();
            GridBagConstraints gridBagConstraints113 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints26 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints32 = new GridBagConstraints();
            startnrLabel = new JLabel();
            GridBagConstraints gridBagConstraints25 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints112 = new GridBagConstraints();
            bouqetLabel = new JLabel();
            GridBagConstraints gridBagConstraints110 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints24 = new GridBagConstraints();
            sourceLabel = new JLabel();
            parameterLabel = new JLabel();
            symbolrateLabel = new JLabel();
            vpidLabel = new JLabel();
            apidLabel = new JLabel();
            tpidLabel = new JLabel();
            sidLabel = new JLabel();
            tidLabel = new JLabel();
            caidLabel = new JLabel();
            nidLabel = new JLabel();
            ridLabel = new JLabel();
            jLabel = new JLabel();
            GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints41 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints31 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints111 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints12 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints13 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints14 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints15 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints16 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints17 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints18 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints19 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints20 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints211 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints22 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints23 = new GridBagConstraints();
            frequenzLabel = new JLabel();
            nameLabel = new JLabel();
            GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
            jPanel = new JPanel();
            jPanel.setLayout(new GridBagLayout());
            gridBagConstraints1.gridx = 0;
            gridBagConstraints1.gridy = 0;
            gridBagConstraints1.insets = new java.awt.Insets(10, 10, 0, 5);
            gridBagConstraints1.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints1.weightx = 0.0D;
            nameLabel.setText(Messages.getString("ChannelPropertyPanel.0"));
            gridBagConstraints2.gridx = 1;
            gridBagConstraints2.gridy = 0;
            gridBagConstraints2.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints2.insets = new java.awt.Insets(10, 0, 0, 10);
            gridBagConstraints3.gridx = 0;
            gridBagConstraints3.gridy = 1;
            gridBagConstraints3.insets = new java.awt.Insets(0, 10, 0, 5);
            gridBagConstraints3.anchor = java.awt.GridBagConstraints.WEST;
            frequenzLabel.setText(Messages.getString("ChannelPropertyPanel.1"));
            gridBagConstraints4.gridx = 1;
            gridBagConstraints4.gridy = 1;
            gridBagConstraints4.weightx = 1.0;
            gridBagConstraints4.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints4.insets = new java.awt.Insets(0, 0, 0, 10);
            gridBagConstraints11.gridx = 0;
            gridBagConstraints11.gridy = 2;
            sourceLabel.setText(Messages.getString("ChannelPropertyPanel.2"));
            gridBagConstraints11.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints11.insets = new java.awt.Insets(0, 10, 0, 5);
            gridBagConstraints21.gridx = 1;
            gridBagConstraints21.gridy = 2;
            gridBagConstraints21.weightx = 1.0;
            gridBagConstraints21.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints21.insets = new java.awt.Insets(0, 0, 0, 10);
            gridBagConstraints31.gridx = 2;
            gridBagConstraints31.gridy = 1;
            parameterLabel.setText(Messages.getString("ChannelPropertyPanel.3"));
            gridBagConstraints41.gridx = 4;
            gridBagConstraints41.gridy = 1;
            gridBagConstraints41.weightx = 1.0;
            gridBagConstraints41.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints41.insets = new java.awt.Insets(0, 0, 0, 10);
            gridBagConstraints31.insets = new java.awt.Insets(0, 0, 0, 5);
            gridBagConstraints5.gridx = 2;
            gridBagConstraints5.gridy = 2;
            symbolrateLabel.setText(Messages.getString("ChannelPropertyPanel.4"));
            gridBagConstraints31.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints31.weightx = 0.0D;
            gridBagConstraints5.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints5.insets = new java.awt.Insets(0, 0, 0, 5);
            gridBagConstraints6.gridx = 4;
            gridBagConstraints6.gridy = 2;
            gridBagConstraints6.weightx = 1.0;
            gridBagConstraints6.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints6.insets = new java.awt.Insets(0, 0, 0, 10);
            jPanel.setName(Messages.getString("ChannelPropertyPanel.5"));
            gridBagConstraints7.gridx = 0;
            gridBagConstraints7.gridy = 3;
            vpidLabel.setText(Messages.getString("ChannelPropertyPanel.6"));
            gridBagConstraints7.insets = new java.awt.Insets(0, 10, 0, 5);
            gridBagConstraints7.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints8.gridx = 1;
            gridBagConstraints8.gridy = 3;
            gridBagConstraints8.weightx = 1.0;
            gridBagConstraints8.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints8.insets = new java.awt.Insets(0, 0, 0, 10);
            gridBagConstraints9.gridx = 2;
            gridBagConstraints9.gridy = 3;
            apidLabel.setText(Messages.getString("ChannelPropertyPanel.7"));
            gridBagConstraints9.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints9.insets = new java.awt.Insets(0, 0, 0, 5);
            gridBagConstraints10.gridx = 4;
            gridBagConstraints10.gridy = 3;
            gridBagConstraints10.weightx = 1.0;
            gridBagConstraints10.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints10.insets = new java.awt.Insets(0, 0, 0, 10);
            gridBagConstraints111.gridx = 0;
            gridBagConstraints111.gridy = 4;
            gridBagConstraints111.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints111.insets = new java.awt.Insets(0, 10, 0, 5);
            tpidLabel.setText(Messages.getString("ChannelPropertyPanel.8"));
            gridBagConstraints12.gridx = 0;
            gridBagConstraints12.gridy = 5;
            gridBagConstraints12.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints12.insets = new java.awt.Insets(0, 10, 0, 5);
            sidLabel.setText(Messages.getString("ChannelPropertyPanel.9"));
            gridBagConstraints13.gridx = 0;
            gridBagConstraints13.gridy = 6;
            gridBagConstraints13.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints13.insets = new java.awt.Insets(0, 10, 0, 5);
            tidLabel.setText(Messages.getString("ChannelPropertyPanel.10"));
            gridBagConstraints14.gridx = 2;
            gridBagConstraints14.gridy = 4;
            gridBagConstraints14.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints14.insets = new java.awt.Insets(0, 0, 0, 5);
            caidLabel.setText(Messages.getString("ChannelPropertyPanel.11"));
            gridBagConstraints15.gridx = 2;
            gridBagConstraints15.gridy = 5;
            gridBagConstraints15.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints15.insets = new java.awt.Insets(0, 0, 0, 5);
            nidLabel.setText(Messages.getString("ChannelPropertyPanel.12"));
            gridBagConstraints16.gridx = 2;
            gridBagConstraints16.gridy = 6;
            gridBagConstraints16.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints16.insets = new java.awt.Insets(0, 0, 0, 5);
            ridLabel.setText(Messages.getString("ChannelPropertyPanel.13"));
            gridBagConstraints17.gridx = 0;
            gridBagConstraints17.gridy = 9;
            gridBagConstraints17.insets = new java.awt.Insets(0, 0, 10, 0);
            jLabel.setText(Messages.getString("ChannelPropertyPanel.14"));
            gridBagConstraints18.gridx = 1;
            gridBagConstraints18.gridy = 4;
            gridBagConstraints18.weightx = 1.0;
            gridBagConstraints18.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints18.insets = new java.awt.Insets(0, 0, 0, 10);
            gridBagConstraints19.gridx = 4;
            gridBagConstraints19.gridy = 4;
            gridBagConstraints19.weightx = 1.0;
            gridBagConstraints19.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints19.insets = new java.awt.Insets(0, 0, 0, 10);
            gridBagConstraints20.gridx = 1;
            gridBagConstraints20.gridy = 5;
            gridBagConstraints20.weightx = 1.0;
            gridBagConstraints20.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints20.insets = new java.awt.Insets(0, 0, 0, 10);
            gridBagConstraints211.gridx = 4;
            gridBagConstraints211.gridy = 5;
            gridBagConstraints211.weightx = 1.0;
            gridBagConstraints211.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints211.insets = new java.awt.Insets(0, 0, 0, 10);
            gridBagConstraints22.gridx = 1;
            gridBagConstraints22.gridy = 6;
            gridBagConstraints22.weightx = 1.0;
            gridBagConstraints22.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints22.insets = new java.awt.Insets(0, 0, 0, 10);
            gridBagConstraints23.gridx = 4;
            gridBagConstraints23.gridy = 6;
            gridBagConstraints23.weightx = 1.0;
            gridBagConstraints23.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints23.insets = new java.awt.Insets(0, 0, 0, 10);
            jPanel.add(tidLabel, gridBagConstraints13);
            jPanel.add(getApidTextField(), gridBagConstraints10);
            jPanel.add(apidLabel, gridBagConstraints9);
            jPanel.add(tpidLabel, gridBagConstraints111);
            jPanel.add(sidLabel, gridBagConstraints12);
            jPanel.add(caidLabel, gridBagConstraints14);
            jPanel.add(nidLabel, gridBagConstraints15);
            jPanel.add(ridLabel, gridBagConstraints16);
            jPanel.add(getTpidTextField(), gridBagConstraints18);
            jPanel.add(getCaidTextField(), gridBagConstraints19);
            jPanel.add(getSidTextField(), gridBagConstraints20);
            jPanel.add(getNidTextField(), gridBagConstraints211);
            jPanel.add(getTidTextField(), gridBagConstraints22);
            jPanel.add(getRidTextField(), gridBagConstraints23);
            jPanel.add(getVpidTextField(), gridBagConstraints8);
            jPanel.add(vpidLabel, gridBagConstraints7);
            jPanel.add(getSymbolrateTextField(), gridBagConstraints6);
            jPanel.add(symbolrateLabel, gridBagConstraints5);
            jPanel.add(sourceLabel, gridBagConstraints11);
            jPanel.add(getParameterTextField(), gridBagConstraints41);
            gridBagConstraints2.gridwidth = 1;
            gridBagConstraints2.weightx = 1.0D;
            gridBagConstraints110.gridx = 2;
            gridBagConstraints110.gridy = 0;
            gridBagConstraints110.insets = new java.awt.Insets(10, 0, 0, 5);
            gridBagConstraints110.anchor = java.awt.GridBagConstraints.WEST;
            bouqetLabel.setText(Messages.getString("ChannelPropertyPanel.15"));
            gridBagConstraints24.gridx = 4;
            gridBagConstraints24.gridy = 0;
            gridBagConstraints24.weightx = 1.0;
            gridBagConstraints24.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints24.insets = new java.awt.Insets(10, 0, 0, 10);
            gridBagConstraints112.gridx = 0;
            gridBagConstraints112.gridy = 7;
            gridBagConstraints112.insets = new java.awt.Insets(0, 10, 0, 5);
            gridBagConstraints112.anchor = java.awt.GridBagConstraints.WEST;
            startnrLabel.setText(Messages.getString("ChannelPropertyPanel.16"));
            gridBagConstraints25.gridx = 1;
            gridBagConstraints25.gridy = 7;
            gridBagConstraints25.weightx = 1.0;
            gridBagConstraints25.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints25.insets = new java.awt.Insets(0, 0, 0, 10);
            gridBagConstraints113.gridx = 0;
            gridBagConstraints113.gridy = 8;
            gridBagConstraints113.insets = new java.awt.Insets(0, 10, 0, 5);
            gridBagConstraints113.anchor = java.awt.GridBagConstraints.WEST;
            aliasLabel.setText(Messages.getString("ChannelPropertyPanel.17"));
            gridBagConstraints26.gridx = 1;
            gridBagConstraints26.gridy = 8;
            gridBagConstraints26.weightx = 1.0;
            gridBagConstraints26.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints26.insets = new java.awt.Insets(0, 0, 0, 10);
            gridBagConstraints32.gridx = 2;
            gridBagConstraints32.gridy = 8;
            gridBagConstraints32.insets = new java.awt.Insets(0, 0, 0, 5);
            gridBagConstraints32.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints32.gridwidth = 3;
            aliasInfoLabel.setText(Messages.getString("ChannelPropertyPanel.18"));
            jPanel.add(getNameTextField(), gridBagConstraints2);
            jPanel.add(jLabel, gridBagConstraints17);
            jPanel.add(parameterLabel, gridBagConstraints31);
            jPanel.add(nameLabel, gridBagConstraints1);
            jPanel.add(getSourceTextField(), gridBagConstraints21);
            jPanel.add(frequenzLabel, gridBagConstraints3);
            jPanel.add(getFrequenzTextField(), gridBagConstraints4);
            jPanel.add(bouqetLabel, gridBagConstraints110);
            jPanel.add(getBouqetTextField(), gridBagConstraints24);
            jPanel.add(startnrLabel, gridBagConstraints112);
            jPanel.add(getStartnrTextField(), gridBagConstraints25);
            jPanel.add(aliasLabel, gridBagConstraints113);
            jPanel.add(getAliasTextField(), gridBagConstraints26);
            jPanel.add(aliasInfoLabel, gridBagConstraints32);
        }
        return jPanel;
    }

    /**
   * This method initializes jPanel1
   * 
   * @return javax.swing.JPanel
   */
    private JPanel getJPanel1() {
        if (jPanel1 == null) {
            jPanel1 = new JPanel();
            jPanel1.setName(Messages.getString("ChannelPropertyPanel.19"));
            jPanel1.add(getJButton(), null);
        }
        return jPanel1;
    }

    /**
   * This method initializes jTextField
   * 
   * @return javax.swing.JTextField
   */
    private JTextField getNameTextField() {
        if (nameTextField == null) {
            nameTextField = new JTextField();
            nameTextField.setHorizontalAlignment(SwingConstants.LEFT);
            nameTextField.setDocument(new ChannelNameDocument());
            nameTextField.setPreferredSize(new java.awt.Dimension(100, 20));
            nameTextField.getDocument().addDocumentListener(new FieldChangeListener());
        }
        return nameTextField;
    }

    public void updateFields(ChannelElement channelElement) {
        this.channelElement = channelElement;
        if (channelElement instanceof Channel) {
            Channel channel = (Channel) channelElement;
            setVisibleFlagForChannelFields(true);
            setVisibleFlagForChannelCategoryFields(false);
            setValueForChannelFields(channel);
        } else if (channelElement instanceof ChannelCategory) {
            ChannelCategory channelCategory = (ChannelCategory) channelElement;
            setVisibleFlagForChannelFields(false);
            setVisibleFlagForChannelCategoryFields(true);
            setValueForChannelCategoryFields(channelCategory);
        } else {
            setVisibleFlagForChannelFields(false);
            setVisibleFlagForChannelCategoryFields(false);
        }
        jButton.setEnabled(false);
        jPanel.validate();
    }

    private void setValueForChannelCategoryFields(ChannelCategory channelCategory) {
        nameTextField.setText(channelCategory.getName());
        startnrTextField.setText(String.valueOf(channelCategory.getNumberAt()));
    }

    private void setValueForChannelFields(Channel channel) {
        nameTextField.setText(channel.getNameOnly());
        bouqetTextField.setText(channel.getBouqet());
        frequenzTextField.setText(channel.getFrequenz());
        parameterTextField.setText(channel.getParameter());
        sourceTextField.setText(channel.getSource());
        symbolrateTextField.setText(channel.getSymbolrate());
        vpidTextField.setText(channel.getVPid());
        apidTextField.setText(channel.getAPid());
        tpidTextField.setText(channel.getTPid());
        caidTextField.setText(channel.getCaId());
        sidTextField.setText(channel.getSid());
        nidTextField.setText(channel.getNid());
        tidTextField.setText(channel.getTid());
        ridTextField.setText(channel.getRid());
        aliasTextField.setText(channel.getAlias());
    }

    private void setVisibleFlagForChannelCategoryFields(boolean flag) {
        startnrLabel.setVisible(flag);
        startnrTextField.setVisible(flag);
    }

    private void setVisibleFlagForChannelFields(boolean flag) {
        bouqetLabel.setVisible(flag);
        bouqetTextField.setVisible(flag);
        frequenzLabel.setVisible(flag);
        frequenzTextField.setVisible(flag);
        parameterLabel.setVisible(flag);
        parameterTextField.setVisible(flag);
        sourceLabel.setVisible(flag);
        sourceTextField.setVisible(flag);
        symbolrateLabel.setVisible(flag);
        symbolrateTextField.setVisible(flag);
        vpidLabel.setVisible(flag);
        vpidTextField.setVisible(flag);
        apidLabel.setVisible(flag);
        apidTextField.setVisible(flag);
        tpidLabel.setVisible(flag);
        tpidTextField.setVisible(flag);
        caidLabel.setVisible(flag);
        caidTextField.setVisible(flag);
        sidLabel.setVisible(flag);
        sidTextField.setVisible(flag);
        nidLabel.setVisible(flag);
        nidTextField.setVisible(flag);
        tidLabel.setVisible(flag);
        tidTextField.setVisible(flag);
        ridLabel.setVisible(flag);
        ridTextField.setVisible(flag);
        aliasLabel.setVisible(flag);
        aliasTextField.setVisible(flag);
        aliasInfoLabel.setVisible(flag);
    }

    /**
   * This method initializes jTextField
   * 
   * @return javax.swing.JTextField
   */
    private JTextField getFrequenzTextField() {
        if (frequenzTextField == null) {
            frequenzTextField = new RegularExpressionTextField("[0-9]{0,9}");
            frequenzTextField.getDocument().addDocumentListener(new FieldChangeListener());
        }
        return frequenzTextField;
    }

    /**
   * This method initializes jTextField
   * 
   * @return javax.swing.JTextField
   */
    private JTextField getSourceTextField() {
        if (sourceTextField == null) {
            sourceTextField = new RegularExpressionTextField("[0-9\\.SCTEWsctew]+");
            sourceTextField.getDocument().addDocumentListener(new FieldChangeListener());
        }
        return sourceTextField;
    }

    /**
   * This method initializes jTextField
   * 
   * @return javax.swing.JTextField
   */
    private JTextField getParameterTextField() {
        if (parameterTextField == null) {
            parameterTextField = new RegularExpressionTextField("[iIcCdDmMbBtTgGyYhHvVrRlLeE0-9]+");
            parameterTextField.getDocument().addDocumentListener(new FieldChangeListener());
        }
        return parameterTextField;
    }

    /**
   * This method initializes jTextField
   * 
   * @return javax.swing.JTextField
   */
    private JTextField getSymbolrateTextField() {
        if (symbolrateTextField == null) {
            symbolrateTextField = new RegularExpressionTextField("[0-9]{0,5}");
            symbolrateTextField.getDocument().addDocumentListener(new FieldChangeListener());
        }
        return symbolrateTextField;
    }

    /**
   * This method initializes jButton
   * 
   * @return javax.swing.JButton
   */
    private JButton getJButton() {
        if (jButton == null) {
            jButton = new JButton();
            jButton.setText(Messages.getString("ChannelPropertyPanel.24"));
            jButton.setIcon(new ImageIcon(getClass().getResource("/org/javalobby/icons/20x20/SaveDB.gif")));
            jButton.setEnabled(false);
            jButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    submitValues_actionPerformed();
                }
            });
        }
        return jButton;
    }

    /**
   * This method initializes jTextField
   * 
   * @return javax.swing.JTextField
   */
    private JTextField getVpidTextField() {
        if (vpidTextField == null) {
            vpidTextField = new RegularExpressionTextField("(\\d*)|(\\d*\\+\\d*)");
            vpidTextField.getDocument().addDocumentListener(new FieldChangeListener());
        }
        return vpidTextField;
    }

    /**
   * This method initializes jTextField
   * 
   * @return javax.swing.JTextField
   */
    private JTextField getApidTextField() {
        if (apidTextField == null) {
            apidTextField = new RegularExpressionTextField("[^:-]+");
            apidTextField.getDocument().addDocumentListener(new FieldChangeListener());
        }
        return apidTextField;
    }

    /**
   * This method initializes jTextField
   * 
   * @return javax.swing.JTextField
   */
    private JTextField getTpidTextField() {
        if (tpidTextField == null) {
            tpidTextField = new RegularExpressionTextField("\\d*");
            tpidTextField.getDocument().addDocumentListener(new FieldChangeListener());
        }
        return tpidTextField;
    }

    /**
   * This method initializes jTextField
   * 
   * @return javax.swing.JTextField
   */
    private JTextField getCaidTextField() {
        if (caidTextField == null) {
            caidTextField = new RegularExpressionTextField("[0-9a-fA-F, ]+");
            caidTextField.getDocument().addDocumentListener(new FieldChangeListener());
        }
        return caidTextField;
    }

    /**
   * This method initializes jTextField
   * 
   * @return javax.swing.JTextField
   */
    private JTextField getSidTextField() {
        if (sidTextField == null) {
            sidTextField = new RegularExpressionTextField("\\d*");
            sidTextField.getDocument().addDocumentListener(new FieldChangeListener());
        }
        return sidTextField;
    }

    /**
   * This method initializes jTextField
   * 
   * @return javax.swing.JTextField
   */
    private JTextField getNidTextField() {
        if (nidTextField == null) {
            nidTextField = new RegularExpressionTextField("\\d*");
            nidTextField.getDocument().addDocumentListener(new FieldChangeListener());
        }
        return nidTextField;
    }

    /**
   * This method initializes jTextField
   * 
   * @return javax.swing.JTextField
   */
    private JTextField getTidTextField() {
        if (tidTextField == null) {
            tidTextField = new RegularExpressionTextField("\\d*");
            tidTextField.getDocument().addDocumentListener(new FieldChangeListener());
        }
        return tidTextField;
    }

    /**
   * This method initializes jTextField
   * 
   * @return javax.swing.JTextField
   */
    private JTextField getRidTextField() {
        if (ridTextField == null) {
            ridTextField = new RegularExpressionTextField("\\d*");
            ridTextField.getDocument().addDocumentListener(new FieldChangeListener());
        }
        return ridTextField;
    }

    /**
   * submitValues_actionPerformed
   */
    private void submitValues_actionPerformed() {
        if (channelElement instanceof Channel) {
            Channel channel = (Channel) channelElement;
            String name = nameTextField.getText();
            String bouqet = bouqetTextField.getText();
            if (Utils.isEmpty(bouqet)) {
                channelElement.setName(name);
            } else {
                channelElement.setName(name + ";" + bouqet);
            }
            channel.setFrequenz(frequenzTextField.getText());
            channel.setParameter(parameterTextField.getText());
            channel.setSource(sourceTextField.getText());
            channel.setSymbolrate(symbolrateTextField.getText());
            channel.setVPid(vpidTextField.getText());
            channel.setAPid(apidTextField.getText());
            channel.setTPid(tpidTextField.getText());
            channel.setCaId(caidTextField.getText());
            channel.setSid(sidTextField.getText());
            channel.setNid(nidTextField.getText());
            channel.setTid(tidTextField.getText());
            channel.setRid(ridTextField.getText());
            channel.setAlias(aliasTextField.getText());
        } else if (channelElement instanceof ChannelCategory) {
            ChannelCategory channelCategory = (ChannelCategory) channelElement;
            channelCategory.setName(nameTextField.getText());
            int numberAt = 0;
            try {
                numberAt = Integer.parseInt(startnrTextField.getText());
            } catch (NumberFormatException e) {
            }
            channelCategory.setNumberAt(numberAt);
        } else {
            channelElement.setName(nameTextField.getText());
        }
        TreePath treePath = ChannelEditor.application.getChannelListingPanel().getLeadSelectionPath();
        if (treePath != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            ChannelEditor.application.getChannelListingPanel().treeNodeChanged(node);
        }
        jButton.setEnabled(false);
        ChannelEditor.application.setModified(true);
    }

    class FieldChangeListener implements DocumentListener {

        private void fieldChange() {
            if (!createMode) {
                jButton.setEnabled(true);
            }
        }

        public void changedUpdate(DocumentEvent e) {
            fieldChange();
        }

        public void insertUpdate(DocumentEvent e) {
            fieldChange();
        }

        public void removeUpdate(DocumentEvent e) {
            fieldChange();
        }
    }

    /**
   * ChannelNameDocument
   * 
   * @author <a href="mailto:editor@renier.de">Renier Roth</a>
   */
    class ChannelNameDocument extends PlainDocument {

        private static final long serialVersionUID = 3964010528297090406L;

        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            char[] source = str.toCharArray();
            char[] result = new char[source.length];
            int j = 0;
            for (int i = 0; i < result.length; i++) {
                if (source[i] == ':') {
                    result[j++] = '|';
                } else if (source[i] == ';') {
                    Toolkit.getDefaultToolkit().beep();
                } else {
                    result[j++] = source[i];
                }
            }
            super.insertString(offs, new String(result, 0, j), a);
        }
    }

    /**
   * This method initializes jTextField
   * 
   * @return javax.swing.JTextField
   */
    private JTextField getBouqetTextField() {
        if (bouqetTextField == null) {
            bouqetTextField = new JTextField();
            bouqetTextField.setDocument(new ChannelNameDocument());
            bouqetTextField.setPreferredSize(new java.awt.Dimension(100, 20));
            bouqetTextField.getDocument().addDocumentListener(new FieldChangeListener());
        }
        return bouqetTextField;
    }

    /**
   * This method initializes jTextField
   * 
   * @return javax.swing.JTextField
   */
    private JTextField getStartnrTextField() {
        if (startnrTextField == null) {
            startnrTextField = new RegularExpressionTextField("[0-9]+");
            startnrTextField.getDocument().addDocumentListener(new FieldChangeListener());
        }
        return startnrTextField;
    }

    /**
   * getChannel
   * 
   * @return
   */
    public Channel getChannel() {
        Channel ret = null;
        String name = nameTextField.getText();
        String bouqet = bouqetTextField.getText();
        String nameBouqet = name;
        if (!Utils.isEmpty(bouqet)) {
            nameBouqet += ";" + bouqet;
        }
        ret = new Channel(nameBouqet);
        ret.setFrequenz(frequenzTextField.getText());
        ret.setParameter(parameterTextField.getText());
        ret.setSource(sourceTextField.getText());
        ret.setSymbolrate(symbolrateTextField.getText());
        ret.setVPid(vpidTextField.getText());
        ret.setAPid(apidTextField.getText());
        ret.setTPid(tpidTextField.getText());
        ret.setCaId(caidTextField.getText());
        ret.setSid(sidTextField.getText());
        ret.setNid(nidTextField.getText());
        ret.setTid(tidTextField.getText());
        ret.setRid(ridTextField.getText());
        return ret;
    }

    /**
   * This method initializes jTextField
   * 
   * @return javax.swing.JTextField
   */
    private JTextField getAliasTextField() {
        if (aliasTextField == null) {
            aliasTextField = new RegularExpressionTextField("[^:-]+");
            aliasTextField.getDocument().addDocumentListener(new FieldChangeListener());
        }
        return aliasTextField;
    }
}
