package org.ctext.ite.gui.dialogs;

import java.awt.Color;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.logging.Level;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AttributeSet;
import org.ctext.ite.utils.Logger;
import org.omegat.util.gui.Styles;

/**
 * The Initial Settings Dialog is where the initial settings can be set, and is
 * crucial for setting up the link to the OpenOffice.org component.
 * @author W. Fourie
 */
public class InitialSettingsDialog extends javax.swing.JDialog {

    /**
     * The colour to be used for the source text in the OmegaT translator environment.
     */
    public String srcColor;

    /**
     * The colour to be used for the target text in the OmegaT translator environment.
     */
    public String trgColor;

    /**
     * The colour to be used for the TransTips in the OmegaT translator environment.
     */
    public String ttColor;

    /**
     * The OpenOffice.org Executable path.
     */
    public String OOoExePath;

    private boolean enableActions = true;

    /**
     * The Initial Settings Dialog lets the user specify the initial settings to
     * be used with the OmegaT translator component. The source, target and transtip
     * text's colours can be set.
     * @param parent The parent frame.
     * @param modal A modal dialog is always on top of its parent window.
     */
    public InitialSettingsDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        SetupColourCombos();
        setIcon();
        setTitle("Setup Initial Settings");
        String detect = getOOoPath();
        if (detect.equals("Error")) {
            OOoExeField.setText("Error: No OpenOffice.org installation found !");
            OOoExePath = detect;
        } else {
            OOoExeField.setText(detect);
            OOoExePath = detect;
        }
        enableActions = true;
    }

    private void setIcon() {
        ImageIcon icon2 = new ImageIcon(getClass().getResource("/org/ctext/ite/gui/icons/ITE.JPG"));
        Image image = icon2.getImage();
        setIconImage(image);
    }

    private void initComponents() {
        okBut = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        ttColourCmbo = new javax.swing.JComboBox();
        ttField = new javax.swing.JTextField();
        srcColourCmbo = new javax.swing.JComboBox();
        trgColourCmbo = new javax.swing.JComboBox();
        srcField = new javax.swing.JTextField();
        trgField = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        OOoExeField = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        OOoExeBrowseBut = new javax.swing.JButton();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        okBut.setText("OK");
        okBut.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButActionPerformed(evt);
            }
        });
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Colours"));
        jLabel6.setText("TransTips Colour:");
        jLabel4.setText("Source Colour:");
        jLabel5.setText("Target Colour:");
        ttColourCmbo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        ttColourCmbo.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ttColourCmboActionPerformed(evt);
            }
        });
        ttField.setEditable(false);
        ttField.setText("TransTips");
        srcColourCmbo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        srcColourCmbo.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                srcColourCmboActionPerformed(evt);
            }
        });
        trgColourCmbo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        trgColourCmbo.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trgColourCmboActionPerformed(evt);
            }
        });
        srcField.setEditable(false);
        srcField.setText("Source");
        trgField.setEditable(false);
        trgField.setText("Target");
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addGap(1, 1, 1).addComponent(jLabel5)).addComponent(jLabel6).addComponent(jLabel4)).addGap(34, 34, 34).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(srcColourCmbo, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(trgColourCmbo, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)).addComponent(ttColourCmbo, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)).addGap(18, 18, 18).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(ttField).addComponent(trgField).addComponent(srcField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addGroup(jPanel1Layout.createSequentialGroup().addComponent(srcField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(trgField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(ttField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addGroup(jPanel1Layout.createSequentialGroup().addComponent(jLabel5).addGap(15, 15, 15).addComponent(jLabel6)).addGroup(jPanel1Layout.createSequentialGroup().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(srcColourCmbo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel4)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(trgColourCmbo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(ttColourCmbo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("OpenOffice.org Executable"));
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel7.setText("( The OpenOffice.org executable is usually found at:");
        jLabel7.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel8.setText("C:\\Program Files\\OpenOffice.org 3\\program\\soffice.exe )");
        OOoExeBrowseBut.setText("...");
        OOoExeBrowseBut.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OOoExeBrowseButActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel2Layout.createSequentialGroup().addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup().addComponent(OOoExeField, javax.swing.GroupLayout.DEFAULT_SIZE, 352, Short.MAX_VALUE).addGap(18, 18, 18).addComponent(OOoExeBrowseBut)).addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, 417, Short.MAX_VALUE).addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, 417, Short.MAX_VALUE)).addContainerGap()));
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel2Layout.createSequentialGroup().addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(OOoExeBrowseBut).addComponent(OOoExeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabel7).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabel8).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()).addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addComponent(okBut).addGap(194, 194, 194)))));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(okBut)));
        pack();
    }

    private void okButActionPerformed(java.awt.event.ActionEvent evt) {
        if (OOoExePath.equals("Error")) {
            JOptionPane.showMessageDialog(rootPane, "The Program requires an OpenOffice.org Installation.", "No OpenOffice.org Installation Specified", JOptionPane.ERROR_MESSAGE);
        } else {
            setVisible(false);
        }
    }

    private void srcColourCmboActionPerformed(java.awt.event.ActionEvent evt) {
        if (enableActions) {
            PopulateTarget();
            int keuse = srcColourCmbo.getSelectedIndex();
            switch(keuse) {
                case 0:
                    srcColor = "AQUA";
                    srcField.setBackground(new Color(183, 234, 223));
                    ttField.setBackground(new Color(183, 234, 223));
                    trgColourCmbo.removeItem("AQUA");
                    break;
                case 1:
                    srcColor = "BLUE";
                    srcField.setBackground(new Color(172, 223, 236));
                    ttField.setBackground(new Color(172, 223, 236));
                    trgColourCmbo.removeItem("BLUE");
                    break;
                case 2:
                    srcColor = "GREEN";
                    srcField.setBackground(new Color(192, 255, 192));
                    ttField.setBackground(new Color(192, 255, 192));
                    trgColourCmbo.removeItem("GREEN");
                    break;
                case 3:
                    srcColor = "PINK";
                    srcField.setBackground(new Color(228, 187, 230));
                    ttField.setBackground(new Color(228, 187, 230));
                    trgColourCmbo.removeItem("PINK");
                    break;
                case 4:
                    srcColor = "PLAIN";
                    srcField.setBackground(null);
                    ttField.setBackground(null);
                    trgColourCmbo.removeItem("PLAIN");
                    break;
                case 5:
                    srcColor = "PURPLE";
                    srcField.setBackground(new Color(187, 167, 207));
                    ttField.setBackground(new Color(187, 167, 207));
                    trgColourCmbo.removeItem("PURPLE");
                    break;
                case 6:
                    srcColor = "YELLOW";
                    srcField.setBackground(new Color(225, 235, 139));
                    ttField.setBackground(new Color(225, 235, 139));
                    trgColourCmbo.removeItem("YELLOW");
                    break;
                default:
                    srcColor = "PLAIN";
                    srcField.setBackground(null);
                    ttField.setBackground(null);
                    trgColourCmbo.removeItem("PLAIN");
                    break;
            }
        }
    }

    private void trgColourCmboActionPerformed(java.awt.event.ActionEvent evt) {
        if (enableActions) {
            String selected = "PLAIN";
            try {
                selected = trgColourCmbo.getSelectedItem().toString();
            } catch (NullPointerException npe) {
                selected = "PLAIN";
            }
            if (selected.equals("AQUA")) {
                trgColor = "AQUA";
                trgField.setBackground(new Color(183, 234, 223));
            } else if (selected.equals("BLUE")) {
                trgColor = "BLUE";
                trgField.setBackground(new Color(172, 223, 236));
            } else if (selected.equals("GREEN")) {
                trgColor = "GREEN";
                trgField.setBackground(new Color(192, 255, 192));
            } else if (selected.equals("PINK")) {
                trgColor = "PINK";
                trgField.setBackground(new Color(228, 187, 230));
            } else if (selected.equals("PLAIN")) {
                trgColor = "PLAIN";
                trgField.setBackground(null);
            } else if (selected.equals("PURPLE")) {
                trgColor = "PURPLE";
                trgField.setBackground(new Color(187, 167, 207));
            } else if (selected.equals("YELLOW")) {
                trgColor = "YELLOW";
                trgField.setBackground(new Color(225, 235, 139));
            } else {
                trgColor = "PLAIN";
                trgField.setBackground(null);
            }
        }
    }

    private void ttColourCmboActionPerformed(java.awt.event.ActionEvent evt) {
        if (enableActions) {
            int keuse = ttColourCmbo.getSelectedIndex();
            switch(keuse) {
                case 0:
                    ttColor = "RED";
                    ttField.setForeground(Color.RED);
                    break;
                case 1:
                    ttColor = "BLUE";
                    ttField.setForeground(Color.BLUE);
                    break;
                case 2:
                    ttColor = "GREEN";
                    ttField.setForeground(Color.GREEN);
                    break;
                case 3:
                    ttColor = "YELLOW";
                    ttField.setForeground(Color.YELLOW);
                    break;
                default:
                    ttColor = "RED";
                    ttField.setForeground(Color.RED);
                    break;
            }
        }
    }

    private void OOoExeBrowseButActionPerformed(java.awt.event.ActionEvent evt) {
        String oldVal = OOoExeField.getText();
        if (!new File(oldVal).exists()) oldVal = "Error";
        FileFilter ff = new FileNameExtensionFilter("OpenOffice.org Executable", "exe");
        JFileChooser fc = new JFileChooser();
        if (OOoExePath.equals("Error")) fc.setSelectedFile(new File("C:\\Program Files\\OpenOffice.org 3\\program\\soffice.exe")); else fc.setSelectedFile(new File(OOoExePath));
        fc.setMultiSelectionEnabled(false);
        fc.setFileFilter(ff);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setDialogTitle("Select OpenOffice.org executable (\"soffice.exe\")");
        int returnVal = fc.showOpenDialog(rootPane);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (fc.getSelectedFile().getName().equals("soffice.exe")) {
                OOoExePath = fc.getSelectedFile().getAbsolutePath().trim();
                OOoExeField.setText(OOoExePath);
            } else {
                JOptionPane.showMessageDialog(rootPane, "In order for the program to work select the \"soffice.exe\" OpenOffice.org executable.", "Wrong File Selected", JOptionPane.ERROR_MESSAGE);
                if (!oldVal.equals("Error")) {
                    OOoExePath = oldVal;
                    OOoExeField.setText(oldVal);
                } else {
                    OOoExePath = "Error";
                    OOoExeField.setText("Error: No OpenOffice.org installation found !");
                }
            }
        }
    }

    private javax.swing.JButton OOoExeBrowseBut;

    private javax.swing.JTextField OOoExeField;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JLabel jLabel5;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JLabel jLabel7;

    private javax.swing.JLabel jLabel8;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JButton okBut;

    private javax.swing.JComboBox srcColourCmbo;

    private javax.swing.JTextField srcField;

    private javax.swing.JComboBox trgColourCmbo;

    private javax.swing.JTextField trgField;

    private javax.swing.JComboBox ttColourCmbo;

    private javax.swing.JTextField ttField;

    private void SetupColourCombos() {
        srcColourCmbo.removeAllItems();
        ttColourCmbo.removeAllItems();
        srcColourCmbo.addItem("AQUA");
        srcColourCmbo.addItem("BLUE");
        srcColourCmbo.addItem("GREEN");
        srcColourCmbo.addItem("PINK");
        srcColourCmbo.addItem("PLAIN");
        srcColourCmbo.addItem("PURPLE");
        srcColourCmbo.addItem("YELLOW");
        PopulateTarget();
        ttColourCmbo.addItem("RED");
        ttColourCmbo.addItem("BLUE");
        ttColourCmbo.addItem("GREEN");
        ttColourCmbo.addItem("YELLOW");
        setKeys();
        srcColourCmbo.setSelectedIndex(2);
        trgColourCmbo.setSelectedIndex(4);
        ttColourCmbo.setSelectedIndex(0);
        srcColor = "GREEN";
        trgColor = "PLAIN";
        ttColor = "RED";
    }

    private void PopulateTarget() {
        trgColourCmbo.removeAllItems();
        trgColourCmbo.addItem("AQUA");
        trgColourCmbo.addItem("BLUE");
        trgColourCmbo.addItem("GREEN");
        trgColourCmbo.addItem("PINK");
        trgColourCmbo.addItem("PLAIN");
        trgColourCmbo.addItem("PURPLE");
        trgColourCmbo.addItem("YELLOW");
    }

    /**
     * Sets the values of the options in the Initial Settings Dialog.
     * @param settings Object[4] - The 4 settings are passed as an object array.
     * <ol>
     * <li>Source Text Colour. (ex. "GREEN")
     * <li>Target Text Colour. (ex. "PLAIN")
     * <li>TransTip Text Colour. (ex. "RED")
     * <li>The OpenOffice.org executable path.
     * </ol>
     */
    public void setValues(Object[] settings) {
        if ((AttributeSet) settings[0] == Styles.AQUA) srcColor = "AQUA"; else if ((AttributeSet) settings[0] == Styles.BLUE) srcColor = "BLUE"; else if ((AttributeSet) settings[0] == Styles.GREEN) srcColor = "GREEN"; else if ((AttributeSet) settings[0] == Styles.PINK) srcColor = "PINK"; else if ((AttributeSet) settings[0] == Styles.PLAIN) srcColor = "PLAIN"; else if ((AttributeSet) settings[0] == Styles.PURPLE) srcColor = "PURPLE"; else if ((AttributeSet) settings[0] == Styles.YELLOW) srcColor = "YELLOW"; else srcColor = "GREEN";
        if ((AttributeSet) settings[1] == Styles.AQUA) trgColor = "AQUA"; else if ((AttributeSet) settings[1] == Styles.BLUE) trgColor = "BLUE"; else if ((AttributeSet) settings[1] == Styles.GREEN) trgColor = "GREEN"; else if ((AttributeSet) settings[1] == Styles.PINK) trgColor = "PINK"; else if ((AttributeSet) settings[1] == Styles.PLAIN) trgColor = "PLAIN"; else if ((AttributeSet) settings[1] == Styles.PURPLE) trgColor = "PURPLE"; else if ((AttributeSet) settings[1] == Styles.YELLOW) trgColor = "YELLOW"; else trgColor = "PLAIN";
        if ((Color) settings[2] == Color.RED) ttColor = "RED"; else if ((Color) settings[2] == Color.BLUE) ttColor = "BLUE"; else if ((Color) settings[2] == Color.GREEN) ttColor = "GREEN"; else if ((Color) settings[2] == Color.YELLOW) ttColor = "YELLOW"; else ttColor = "RED";
        if (srcColor.equals("AQUA")) srcColourCmbo.setSelectedIndex(0); else if (srcColor.equals("BLUE")) srcColourCmbo.setSelectedIndex(1); else if (srcColor.equals("GREEN")) srcColourCmbo.setSelectedIndex(2); else if (srcColor.equals("PINK")) srcColourCmbo.setSelectedIndex(3); else if (srcColor.equals("PLAIN")) srcColourCmbo.setSelectedIndex(4); else if (srcColor.equals("PURPLE")) srcColourCmbo.setSelectedIndex(5); else if (srcColor.equals("YELLOW")) srcColourCmbo.setSelectedIndex(6); else srcColourCmbo.setSelectedIndex(2);
        if (trgColor.equals("AQUA")) trgColourCmbo.setSelectedItem("AQUA"); else if (trgColor.equals("BLUE")) trgColourCmbo.setSelectedItem("BLUE"); else if (trgColor.equals("GREEN")) trgColourCmbo.setSelectedItem("GREEN"); else if (trgColor.equals("PINK")) trgColourCmbo.setSelectedItem("PINK"); else if (trgColor.equals("PLAIN")) trgColourCmbo.setSelectedItem("PLAIN"); else if (trgColor.equals("PURPLE")) trgColourCmbo.setSelectedItem("PURPLE"); else if (trgColor.equals("YELLOW")) trgColourCmbo.setSelectedItem("YELLOW"); else trgColourCmbo.setSelectedItem("PLAIN");
        if (ttColor.equals("RED")) ttColourCmbo.setSelectedIndex(0); else if (ttColor.equals("BLUE")) ttColourCmbo.setSelectedIndex(1); else if (ttColor.equals("GREEN")) ttColourCmbo.setSelectedIndex(2); else if (ttColor.equals("YELLOW")) ttColourCmbo.setSelectedIndex(3); else ttColourCmbo.setSelectedIndex(0);
        OOoExePath = settings[3].toString();
        OOoExeField.setText(OOoExePath);
    }

    private String getOOoPath() {
        String result = null;
        try {
            result = System.getenv("OOO_PATH");
            if (result != null) {
                return result;
            }
            Process process = Runtime.getRuntime().exec("reg query \"HKLM\\Software\\OpenOffice.org\\OpenOffice.org\" /s");
            StreamReader reader = new StreamReader(process.getInputStream());
            reader.start();
            process.waitFor();
            reader.join();
            String regQuery = reader.getResult();
            if (!regQuery.contains("Error:") || !regQuery.equals("")) {
                String[] versions = regQuery.split("\n");
                String oldVer = "0";
                String newVer = "";
                for (int i = 0; i < versions.length; i++) {
                    if (versions[i].contains("HKEY_LOCAL_MACHINE\\Software\\OpenOffice.org\\OpenOffice.org\\")) {
                        newVer = versions[i].substring(versions[i].indexOf("HKEY_LOCAL_MACHINE\\Software\\OpenOffice.org\\OpenOffice.org\\") + 58);
                        if (newVer.contains("\\")) continue;
                        if (Float.valueOf(newVer) > Float.valueOf(oldVer)) oldVer = newVer;
                    }
                }
                oldVer = oldVer.trim();
                String newRegQuery = "reg query \"HKLM\\Software\\OpenOffice.org\\OpenOffice.org\\" + oldVer + "\" /v Path ";
                process = Runtime.getRuntime().exec(newRegQuery);
                reader = new StreamReader(process.getInputStream());
                reader.start();
                process.waitFor();
                reader.join();
                result = reader.getResult();
                result = result.substring(result.indexOf(":\\") - 1);
                result = result.trim();
            } else {
                result = "Error";
            }
        } catch (java.lang.Exception ex) {
            Logger.logger.log(Level.WARNING, "InitialSettings getOOoPath Error", ex);
            result = "Error";
        }
        return result;
    }

    static class StreamReader extends Thread {

        private InputStream is;

        private StringWriter sw;

        StreamReader(InputStream is) {
            this.is = is;
            sw = new StringWriter();
        }

        @Override
        public void run() {
            try {
                int c;
                while ((c = is.read()) != -1) sw.write(c);
            } catch (IOException e) {
            }
        }

        String getResult() {
            return sw.toString();
        }
    }

    private void setKeys() {
        InputMap iMap = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        ActionMap aMap = rootPane.getActionMap();
        aMap.put("enter", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Enter pressed");
                okButActionPerformed(new ActionEvent((Object) okBut, 1001, "OK"));
            }
        });
        KeyListener KL = new KeyListener() {

            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_ENTER) {
                    okButActionPerformed(new ActionEvent((Object) okBut, 1001, "OK"));
                }
            }

            public void keyReleased(KeyEvent e) {
            }
        };
        srcColourCmbo.addKeyListener(KL);
        trgColourCmbo.addKeyListener(KL);
        ttColourCmbo.addKeyListener(KL);
    }
}
