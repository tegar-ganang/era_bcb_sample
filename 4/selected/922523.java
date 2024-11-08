package de.renier.vdr.channel.editor;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import de.renier.vdr.channel.editor.container.RegularExpressionTextField;
import de.renier.vdr.channel.editor.util.LocalProperties;
import de.renier.vdr.channel.editor.util.Utils;
import javax.swing.ImageIcon;
import javax.swing.JTextField;

/**
 * PreferencesDialog
 * 
 * @author <a href="mailto:editor@renier.de">Renier Roth</a>
 */
public class PreferencesDialog extends JDialog {

    private static final long serialVersionUID = 4354784092443473561L;

    private javax.swing.JPanel jContentPane = null;

    private JPanel jPanel = null;

    private JButton saveButton = null;

    private JButton cancelButton = null;

    private JPanel jPanel1 = null;

    private JLabel jLabel = null;

    private JComboBox iconsetComboBox = null;

    private JLabel jLabel1 = null;

    private JTextField fontSizeTextField = null;

    private JLabel jLabel2 = null;

    private JLabel jLabel3 = null;

    private JComboBox languageComboBox = null;

    /**
   * This is the default constructor
   */
    public PreferencesDialog(Frame frame) {
        super(frame, true);
        initialize();
        if (frame != null) {
            Point p = frame.getLocation();
            Dimension frameDim = frame.getSize();
            Dimension ownDim = this.getSize();
            this.setLocation((int) p.getX() + ((int) (frameDim.getWidth() - ownDim.getWidth()) / 2), (int) p.getY() + ((int) (frameDim.getHeight() - ownDim.getHeight()) / 2));
        }
        setVisible(true);
    }

    /**
   * This method initializes this
   * 
   * @return void
   */
    private void initialize() {
        this.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        this.setTitle(Messages.getString("PreferencesDialog.0"));
        this.setSize(450, 145);
        this.setContentPane(getJContentPane());
    }

    /**
   * This method initializes jContentPane
   * 
   * @return javax.swing.JPanel
   */
    private javax.swing.JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new javax.swing.JPanel();
            jContentPane.setLayout(new java.awt.BorderLayout());
            jContentPane.add(getJPanel(), java.awt.BorderLayout.SOUTH);
            jContentPane.add(getJPanel1(), java.awt.BorderLayout.CENTER);
        }
        return jContentPane;
    }

    /**
   * This method initializes jPanel
   * 
   * @return javax.swing.JPanel
   */
    private JPanel getJPanel() {
        if (jPanel == null) {
            jPanel = new JPanel();
            jPanel.add(getSaveButton(), null);
            jPanel.add(getCancelButton(), null);
        }
        return jPanel;
    }

    /**
   * This method initializes jButton
   * 
   * @return javax.swing.JButton
   */
    private JButton getSaveButton() {
        if (saveButton == null) {
            saveButton = new JButton();
            saveButton.setText(Messages.getString("PreferencesDialog.1"));
            saveButton.setIcon(new ImageIcon(getClass().getResource("/org/javalobby/icons/20x20/Save.gif")));
            saveButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    String iconsetName = (String) iconsetComboBox.getSelectedItem();
                    LocalProperties.getInstance().setProperty(LocalProperties.PROP_ICONSET, iconsetName);
                    LocalProperties.getInstance().setProperty(LocalProperties.PROP_FONTSIZE, fontSizeTextField.getText());
                    Utils.changeChannelIconSet(iconsetName);
                    LocalProperties.getInstance().setProperty(LocalProperties.PROP_SYSTEM_LANGUAGE, String.valueOf(languageComboBox.getSelectedItem()));
                    LocalProperties.getInstance().storeLocalProps();
                    ChannelEditor.application.getChannelListingPanel().treeNodeStructureChanged(ChannelEditor.application.getChannelListingPanel().getRootNode());
                    setVisible(false);
                    dispose();
                }
            });
        }
        return saveButton;
    }

    /**
   * This method initializes jButton1
   * 
   * @return javax.swing.JButton
   */
    private JButton getCancelButton() {
        if (cancelButton == null) {
            cancelButton = new JButton();
            cancelButton.setText(Messages.getString("PreferencesDialog.3"));
            cancelButton.setIcon(new ImageIcon(getClass().getResource("/org/javalobby/icons/20x20/Stop.gif")));
            cancelButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    setVisible(false);
                    dispose();
                }
            });
        }
        return cancelButton;
    }

    /**
   * This method initializes jPanel1
   * 
   * @return javax.swing.JPanel
   */
    private JPanel getJPanel1() {
        if (jPanel1 == null) {
            GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
            gridBagConstraints11.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints11.gridy = 2;
            gridBagConstraints11.weightx = 1.0;
            gridBagConstraints11.anchor = GridBagConstraints.WEST;
            gridBagConstraints11.gridx = 1;
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 5);
            gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
            jLabel3 = new JLabel();
            jLabel3.setText(Messages.getString("PreferencesDialog.8"));
            jLabel2 = new JLabel();
            jLabel1 = new JLabel();
            GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
            jLabel = new JLabel();
            GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            jPanel1 = new JPanel();
            jPanel1.setLayout(new GridBagLayout());
            gridBagConstraints1.gridx = 0;
            gridBagConstraints1.gridy = 0;
            gridBagConstraints1.insets = new java.awt.Insets(0, 10, 0, 5);
            gridBagConstraints1.anchor = java.awt.GridBagConstraints.EAST;
            jLabel.setText(Messages.getString("PreferencesDialog.5"));
            gridBagConstraints2.gridx = 1;
            gridBagConstraints2.gridy = 0;
            gridBagConstraints2.weightx = 1.0;
            gridBagConstraints2.fill = java.awt.GridBagConstraints.NONE;
            gridBagConstraints2.insets = new java.awt.Insets(0, 0, 0, 10);
            gridBagConstraints2.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints3.gridx = 0;
            gridBagConstraints3.gridy = 1;
            gridBagConstraints3.insets = new java.awt.Insets(0, 10, 0, 5);
            gridBagConstraints3.anchor = java.awt.GridBagConstraints.EAST;
            jLabel1.setText(Messages.getString("PreferencesDialog.6"));
            gridBagConstraints4.gridx = 1;
            gridBagConstraints4.gridy = 1;
            gridBagConstraints4.weightx = 1.0;
            gridBagConstraints4.fill = java.awt.GridBagConstraints.NONE;
            gridBagConstraints4.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints5.gridx = 2;
            gridBagConstraints5.gridy = 1;
            gridBagConstraints5.insets = new java.awt.Insets(0, 0, 0, 10);
            jLabel2.setText(Messages.getString("PreferencesDialog.7"));
            jPanel1.add(jLabel, gridBagConstraints1);
            jPanel1.add(getIconsetComboBox(), gridBagConstraints2);
            jPanel1.add(jLabel1, gridBagConstraints3);
            jPanel1.add(getFontSizeTextField(), gridBagConstraints4);
            jPanel1.add(jLabel2, gridBagConstraints5);
            jPanel1.add(jLabel3, gridBagConstraints);
            jPanel1.add(getLanguageComboBox(), gridBagConstraints11);
        }
        return jPanel1;
    }

    /**
   * This method initializes jComboBox
   * 
   * @return javax.swing.JComboBox
   */
    private JComboBox getIconsetComboBox() {
        if (iconsetComboBox == null) {
            Vector iconsetList = new Vector();
            iconsetList.add("default");
            iconsetList.add("small");
            iconsetList.add("medium");
            iconsetList.add("big");
            iconsetComboBox = new JComboBox(iconsetList);
            iconsetComboBox.setPreferredSize(new java.awt.Dimension(120, 25));
            String iconsetName = LocalProperties.getInstance().getProperty(LocalProperties.PROP_ICONSET);
            iconsetComboBox.setSelectedItem(iconsetName);
        }
        return iconsetComboBox;
    }

    /**
   * This method initializes jTextField
   * 
   * @return javax.swing.JTextField
   */
    private JTextField getFontSizeTextField() {
        if (fontSizeTextField == null) {
            fontSizeTextField = new RegularExpressionTextField("\\d*");
            fontSizeTextField.setPreferredSize(new java.awt.Dimension(30, 20));
            fontSizeTextField.setText(LocalProperties.getInstance().getProperty(LocalProperties.PROP_FONTSIZE));
        }
        return fontSizeTextField;
    }

    /**
   * This method initializes languageComboBox	
   * 	
   * @return javax.swing.JComboBox	
   */
    private JComboBox getLanguageComboBox() {
        if (languageComboBox == null) {
            languageComboBox = new JComboBox();
            languageComboBox = new JComboBox();
            languageComboBox.addItem("de");
            languageComboBox.addItem("en");
            languageComboBox.addItem("it");
            String language = LocalProperties.getInstance().getProperty(LocalProperties.PROP_SYSTEM_LANGUAGE);
            if (!Utils.isEmpty(language)) {
                languageComboBox.setSelectedItem(language);
            }
        }
        return languageComboBox;
    }
}
