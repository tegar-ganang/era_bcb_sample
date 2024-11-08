package signitserver.application.console;

import com.jhlabs.image.NoiseFilter;
import com.sun.jna.examples.WindowUtils;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.document.XDocumentInfo;
import com.sun.star.uno.Exception;
import com.sun.star.uno.XComponentContext;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import signitserver.application.MetaDataReader;
import signitserver.application.constants.SignItConstants;
import signitserver.application.enrollment.Enroll;
import signitserver.application.etc.GraphicsUtilities;
import signitserver.application.etc.RoundedBorder;
import signitserver.application.utils.Formatter;
import signitserver.application.verification.Verify;
import signitserver.vo.SignatureDataIR;
import signitserver.vo.SignatureDetails;

/**
 *
 * @author Sachin Sudheendra
 */
public class SignItServerConsole extends JFrame {

    private XComponentContext xLocalContext;

    private SignatureDataIR signatureReadFromMetaData;

    Preferences preferences;

    public SignItServerConsole(XComponentContext xcc) {
        super("Sign It! Server");
        this.xLocalContext = xcc;
        this.preferences = Preferences.userNodeForPackage(signitserver.SignItServer.class);
        this.signatureReadFromMetaData = null;
        initComponents();
    }

    {
        try {
            System.setProperty("sun.awt.noerasebackground", "true");
            System.setProperty("sun.java2d.noddraw", "true");
            System.setProperty("sun.java2d.opengl", "true");
            logoImg = ImageIO.read(signitserver.SignItServer.class.getResource("logo.png"));
            closeOverImg = ImageIO.read(signitserver.SignItServer.class.getResource("close_over.png"));
            closeImg = ImageIO.read(signitserver.SignItServer.class.getResource("close.png"));
            minimizeOverImg = ImageIO.read(signitserver.SignItServer.class.getResource("minimize_over.png"));
            minimizeImg = ImageIO.read(signitserver.SignItServer.class.getResource("minimize.png"));
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    private void initComponents() {
        decorateWindow();
        decorateMainPanel();
        formWindowOpened();
    }

    private void decorateMainPanel() {
        final Paint footerBackground = createBackgroundTexture(Color.LIGHT_GRAY, Color.DARK_GRAY, 65);
        mainPanel = new JPanel(new BorderLayout(), true) {

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setPaint(footerBackground);
                if (getExtendedState() != MAXIMIZED_BOTH) {
                    g2d.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 20, 20);
                } else {
                    g2d.fillRect(0, 0, this.getWidth(), this.getHeight());
                }
            }
        };
        mainPanel.setOpaque(true);
        mainPanel.setBorder(new RoundedBorder(20));
        mainPanel.add(getHeader(), BorderLayout.NORTH);
        mainPanel.add(getBody(), BorderLayout.CENTER);
        this.add(mainPanel);
    }

    private void decorateWindow() {
        super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        super.setSize(new Dimension(600, 300));
        super.setPreferredSize(new Dimension(600, 300));
        super.setMinimumSize(new Dimension(600, 300));
        super.setLocationRelativeTo(null);
        super.setUndecorated(true);
        WindowUtils.setWindowTransparent(this, true);
    }

    private Paint createBackgroundTexture(Color color1, Color color2, int size) {
        BufferedImage image = GraphicsUtilities.createTranslucentCompatibleImage(size, size);
        Graphics2D g2d = image.createGraphics();
        Paint paint = new GradientPaint(0, 0, color1, 0, size, color2);
        g2d.setPaint(paint);
        g2d.fillRect(0, 0, size, size);
        g2d.dispose();
        NoiseFilter filter = new NoiseFilter();
        filter.setAmount(10);
        filter.setDensity(0.5f);
        filter.setDistribution(NoiseFilter.UNIFORM);
        filter.setMonochrome(true);
        filter.filter(image, image);
        Paint result = new TexturePaint(image, new Rectangle(size, size));
        return result;
    }

    private Component getHeader() {
        if (header == null) {
            JLabel logo = new JLabel(new ImageIcon(logoImg));
            JPanel decoration = new JPanel(true);
            decoration.setLayout(new BoxLayout(decoration, BoxLayout.LINE_AXIS));
            decoration.setOpaque(false);
            JButton minimize = new JButton(new ImageIcon(minimizeImg));
            minimize.setRolloverIcon(new ImageIcon(minimizeOverImg));
            minimize.setBorder(null);
            minimize.setFocusPainted(false);
            minimize.setContentAreaFilled(false);
            minimize.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    setExtendedState(ICONIFIED);
                }
            });
            decoration.add(minimize);
            JButton close = new JButton(new ImageIcon(closeImg));
            close.setRolloverIcon(new ImageIcon(closeOverImg));
            close.setBorder(null);
            close.setFocusPainted(false);
            close.setContentAreaFilled(false);
            close.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
            decoration.add(close);
            boxAfterClose = Box.createHorizontalBox();
            boxAfterClose.setPreferredSize(new Dimension(15, 1));
            decoration.add(boxAfterClose);
            final Paint backgroundMenu = createBackgroundTexture(Color.DARK_GRAY, Color.GRAY, 65);
            JPanel upper = new JPanel(new GridBagLayout(), true) {

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setPaint(backgroundMenu);
                    if (getExtendedState() != MAXIMIZED_BOTH) {
                        g2d.fillRoundRect(0, 0, this.getWidth(), this.getHeight() + 5, 20, 20);
                    } else {
                        g2d.fillRect(0, 0, this.getWidth(), this.getHeight() + 5);
                    }
                }
            };
            upper.setOpaque(false);
            upper.setPreferredSize(new Dimension(100, 65));
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.NONE;
            c.weighty = 100;
            c.anchor = GridBagConstraints.CENTER;
            c.gridx = 0;
            c.gridy = 0;
            c.gridheight = 2;
            upper.add(logo, c);
            final JLabel heading = new JLabel("SignIt! Server");
            heading.setFont(new java.awt.Font("Calibri", Font.BOLD, 15));
            heading.setForeground(Color.WHITE);
            upper.add(heading);
            c.anchor = GridBagConstraints.NORTHEAST;
            c.fill = GridBagConstraints.NONE;
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 100;
            upper.add(decoration, c);
            c.anchor = GridBagConstraints.SOUTHWEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.gridy = 1;
            c.weightx = 100;
            header = new JPanel(new BorderLayout(), true);
            header.setOpaque(false);
            header.add(upper, BorderLayout.CENTER);
        }
        return header;
    }

    private Component getBody() {
        if (bodyPanel == null) {
            final Paint backgroundMenu = createBackgroundTexture(Color.WHITE, Color.WHITE, 65);
            bodyPanel = new JPanel(new BorderLayout(), true) {

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setPaint(backgroundMenu);
                    if (getExtendedState() != MAXIMIZED_BOTH) {
                        g2d.fillRoundRect(0, 0, this.getWidth(), this.getHeight() + 5, 20, 20);
                    } else {
                        g2d.fillRect(0, 0, this.getWidth(), this.getHeight() + 5);
                    }
                }
            };
            bodyPanel.setOpaque(false);
            tabPane = new JTabbedPane(JTabbedPane.LEFT);
            tabPane.add(setVerificationPanel());
            tabPane.add(setEnrollmentPanel());
            tabPane.add(setLogPanel());
            tabPane.add(setSettingsPanel());
            bodyPanel.add(tabPane, BorderLayout.CENTER);
        }
        return bodyPanel;
    }

    private JPanel setVerificationPanel() {
        verificationDetailsPanel = new JPanel();
        verificationDetailsPanel.setLayout(null);
        verificationDetailsPanel.setName("Verification");
        vfirstNameLabel = new JLabel();
        vfirstNameLabel.setText("First Name");
        verificationDetailsPanel.add(vfirstNameLabel);
        vfirstNameLabel.setBounds(30, 20, 150, 14);
        vfirstNameTextField = new JTextField();
        verificationDetailsPanel.add(vfirstNameTextField);
        vfirstNameTextField.setBounds(190, 20, 220, 20);
        vfirstNameTextField.setMaximumSize(new java.awt.Dimension(26, 20));
        vlastNameLabel = new JLabel();
        vlastNameLabel.setText("Last Name");
        verificationDetailsPanel.add(vlastNameLabel);
        vlastNameLabel.setBounds(30, 50, 150, 14);
        vlastNameTextField = new JTextField();
        verificationDetailsPanel.add(vlastNameTextField);
        vlastNameTextField.setBounds(190, 50, 220, 20);
        vlastNameTextField.setMaximumSize(new java.awt.Dimension(26, 20));
        vidLabel = new JLabel();
        vidLabel.setText("Identification Number");
        verificationDetailsPanel.add(vidLabel);
        vidLabel.setBounds(30, 80, 150, 14);
        vidTextField = new JTextField();
        verificationDetailsPanel.add(vidTextField);
        vidTextField.setBounds(190, 80, 220, 20);
        vidTextField.setMaximumSize(new java.awt.Dimension(26, 20));
        verifyButton = new JButton("Verify");
        verifyButton.setBounds(150, 110, 80, 30);
        verificationDetailsPanel.add(verifyButton);
        resultPanel = new JPanel();
        resultPanel.setLayout(null);
        resultPanel.setBounds(100, 160, 230, 60);
        resultPanel.setBorder(BorderFactory.createTitledBorder("Result"));
        verificationDetailsPanel.add(resultPanel);
        resultTextField = new JTextField();
        resultTextField.setFont(new java.awt.Font("Calibri", 1, 14));
        resultTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        resultTextField.setToolTipText("Similarity Measure");
        resultTextField.setEnabled(false);
        resultTextField.setBounds(20, 20, 185, 25);
        resultTextField.setMinimumSize(new Dimension(185, 25));
        resultPanel.add(resultTextField);
        verifyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                verifyButtonActionPerformed(e);
            }
        });
        return verificationDetailsPanel;
    }

    private JPanel setEnrollmentPanel() {
        enrollmentPanel = new JPanel();
        enrollmentPanel.setLayout(null);
        enrollmentPanel.setName("Enrollment");
        efirstNameLabel = new JLabel();
        efirstNameLabel.setText("First Name");
        enrollmentPanel.add(efirstNameLabel);
        efirstNameLabel.setBounds(30, 20, 150, 14);
        efirstNameTextField = new JTextField();
        enrollmentPanel.add(efirstNameTextField);
        efirstNameTextField.setBounds(190, 20, 220, 20);
        efirstNameTextField.setMaximumSize(new java.awt.Dimension(26, 20));
        elastNameLabel = new JLabel();
        elastNameLabel.setText("Last Name");
        enrollmentPanel.add(elastNameLabel);
        elastNameLabel.setBounds(30, 50, 150, 14);
        elastNameTextField = new JTextField();
        enrollmentPanel.add(elastNameTextField);
        elastNameTextField.setBounds(190, 50, 220, 20);
        elastNameTextField.setMaximumSize(new java.awt.Dimension(26, 20));
        eidLabel = new JLabel();
        eidLabel.setText("Identification Number");
        enrollmentPanel.add(eidLabel);
        eidLabel.setBounds(30, 80, 150, 14);
        eidTextField = new JTextField();
        enrollmentPanel.add(eidTextField);
        eidTextField.setBounds(190, 80, 220, 20);
        eidTextField.setMaximumSize(new java.awt.Dimension(26, 20));
        enrollButton = new JButton("Enroll");
        enrollButton.setBounds(150, 110, 80, 30);
        enrollmentPanel.add(enrollButton);
        enrollButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                enrollButtonActionPerformed(e);
            }
        });
        saveFromMetaData = new JButton("Save from MetaData");
        saveFromMetaData.setBounds(110, 150, 150, 30);
        saveFromMetaData.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    saveFromMetaDataActionPerformed(e);
                } catch (java.lang.Exception e1) {
                    writeToLogs(e1.getMessage(), true);
                }
            }
        });
        enrollmentPanel.add(saveFromMetaData);
        return enrollmentPanel;
    }

    private void enrollButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (isEnrollFieldsEmpty()) {
            writeToLogs("[enroll] Please enter all necessary fields.", true);
        } else {
            new Enroll(new SignatureDetails(efirstNameTextField.getText(), elastNameTextField.getText(), eidTextField.getText()));
        }
    }

    private void saveFromMetaDataActionPerformed(ActionEvent evt) throws FileNotFoundException, IOException {
        int selectedOption = JOptionPane.showConfirmDialog(new JFrame(), "Are you sure you want to replicate the MetaData into two sample set?\nTip: This would reduce the accuracy of verification by half.", "Are you sure?", JOptionPane.OK_CANCEL_OPTION);
        if (selectedOption == JOptionPane.CANCEL_OPTION) {
            return;
        }
        String datapath = preferences.get("datapath", "C:\\Signit\\Data");
        String folderName = datapath + "\\" + efirstNameTextField.getText() + "_" + elastNameTextField.getText() + "_" + eidTextField.getText();
        File file = new File(folderName);
        if (file.exists()) {
            throw new RuntimeException("[exception] User already exists. Cannot overwrite.");
        }
        file.mkdir();
        for (int i = 1; i <= 2; i++) {
            FileOutputStream fOS = new FileOutputStream(folderName + "\\" + i);
            PrintWriter pw = new PrintWriter(fOS);
            pw.println(Formatter.listToString(signatureReadFromMetaData.getX(), SignItConstants.DELIMITER));
            pw.println(Formatter.listToString(signatureReadFromMetaData.getY(), SignItConstants.DELIMITER));
            pw.println(Formatter.byteArrayToString(signatureReadFromMetaData.getDigest()));
            pw.close();
            fOS.close();
        }
    }

    private boolean isEnrollFieldsEmpty() {
        if (efirstNameTextField.getText().isEmpty() || elastNameTextField.getText().isEmpty() || eidTextField.getText().isEmpty()) {
            return true;
        }
        return false;
    }

    private JPanel setLogPanel() {
        logPanel = new JPanel();
        logPanel.setLayout(null);
        logPanel.setName("Logs");
        logTextArea = new JTextArea();
        logTextArea.setColumns(20);
        logTextArea.setFont(new java.awt.Font("Calibri", 0, 12));
        logTextArea.setLineWrap(true);
        logTextArea.setRows(5);
        logTextArea.setEditable(false);
        logTextArea.setMinimumSize(new Dimension(200, 200));
        logTextArea.setWrapStyleWord(true);
        logTextArea.setBounds(0, 0, 500, 300);
        logPanel.add(logTextArea);
        return logPanel;
    }

    private JPanel setSettingsPanel() {
        settingsPanel = new JPanel();
        settingsPanel.setLayout(null);
        settingsPanel.setName("Settings");
        sdataPathLabel = new JLabel("Data Path");
        sdataPathLabel.setBounds(30, 20, 150, 14);
        settingsPanel.add(sdataPathLabel);
        sdataPathTextField = new JTextField(preferences.get("datapath", "C:\\SignIt\\Data"));
        sdataPathTextField.setBounds(190, 20, 220, 20);
        settingsPanel.add(sdataPathTextField);
        ssaveButton = new JButton("Save");
        ssaveButton.setBounds(150, 50, 80, 30);
        ssaveButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String dataPathValue = sdataPathTextField.getText();
                if (!dataPathValue.isEmpty()) {
                    int answer = JOptionPane.showConfirmDialog(new JFrame(), "Are you sure you want to change the location?\nMake sure the directory structure exists on disk.", "Confirm", JOptionPane.OK_CANCEL_OPTION);
                    if (answer == JOptionPane.OK_OPTION) {
                        preferences.put("datapath", dataPathValue);
                        try {
                            preferences.flush();
                            preferences.sync();
                        } catch (BackingStoreException ex) {
                            writeToLogs("[exception] Could not save preference. " + ex.getMessage(), true);
                        }
                    } else {
                        sdataPathTextField.setText(preferences.get("datapath", "C:\\SignIt\\Data"));
                    }
                }
            }
        });
        settingsPanel.add(ssaveButton);
        return settingsPanel;
    }

    public void writeToLogs(String message, boolean bringIntoFocus) {
        logTextArea.append(message + "\n");
        if (bringIntoFocus) {
            tabPane.setSelectedComponent(logPanel);
        }
    }

    private void formWindowOpened() {
        try {
            readSignatureDataFromMetaData();
        } catch (java.lang.Exception e) {
            writeToLogs("[info] Error reading MetaData.", true);
        }
        if (signatureReadFromMetaData == null) {
            writeToLogs("[info] This document is not signed.", true);
            saveFromMetaData.setEnabled(false);
        } else {
            writeToLogs("[info] This document appears to have been signed.", false);
            saveFromMetaData.setEnabled(true);
        }
    }

    private void readSignatureDataFromMetaData() throws Exception, BootstrapException, NoSuchAlgorithmException, IOException {
        XDocumentInfo document = null;
        try {
            document = MetaDataReader.getDocumentInfo(xLocalContext);
        } catch (java.lang.Exception ex) {
            writeToLogs("[exception] Exception occured while trying to read MetaData.", true);
        }
        String signatureXString = MetaDataReader.readValueFromMetaData(document, SignItConstants.META_DATA_ARG0);
        String signatureYString = MetaDataReader.readValueFromMetaData(document, SignItConstants.META_DATA_ARG1);
        String signatureDigest = MetaDataReader.readValueFromMetaData(document, SignItConstants.META_DATA_ARG2);
        signatureReadFromMetaData = new SignatureDataIR(Formatter.stringToList(signatureXString, SignItConstants.DELIMITER), Formatter.stringToList(signatureYString, SignItConstants.DELIMITER), 0);
        signatureReadFromMetaData.setDigest(Formatter.stringToByteArray(signatureDigest));
    }

    private void verifyButtonActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            readSignatureDataFromMetaData();
        } catch (Exception ex) {
            writeToLogs(ex.getMessage(), true);
        } catch (BootstrapException ex) {
            writeToLogs(ex.getMessage(), true);
        } catch (java.lang.Exception ex) {
            writeToLogs(ex.getMessage(), true);
        }
        if (signatureReadFromMetaData == null) {
            writeToLogs("[verification] This document is not signed. Cannot continue.", true);
        } else if (isVerificationDetailsEmpty()) {
            writeToLogs("[verification] Please enter all necessary fields.", true);
        } else {
            SignatureDetails signatureDetails = new SignatureDetails(vfirstNameTextField.getText(), vlastNameTextField.getText(), vidTextField.getText());
            Verify verify = new Verify(signatureReadFromMetaData, signatureDetails);
            try {
                Double finalValue = verify.doVerify();
                resultTextField.setText(Double.toString(finalValue * 100) + "%");
            } catch (java.lang.Exception e) {
                writeToLogs(e.getMessage(), true);
            }
        }
    }

    private boolean isVerificationDetailsEmpty() {
        if (vfirstNameTextField.getText().isEmpty() || vlastNameTextField.getText().isEmpty() || vidTextField.getText().isEmpty()) {
            return true;
        }
        return false;
    }

    private JButton saveFromMetaData;

    private JPanel settingsPanel;

    private JLabel sdataPathLabel;

    private JTextField sdataPathTextField;

    private JButton ssaveButton;

    private JPanel verificationDetailsPanel;

    private JPanel enrollmentPanel;

    private JPanel logPanel;

    private JTextArea logTextArea;

    private JButton verifyButton;

    private JLabel vfirstNameLabel;

    private JTextField vfirstNameTextField;

    private JLabel vlastNameLabel;

    private JTextField vlastNameTextField;

    private JLabel vidLabel;

    private JTextField vidTextField;

    private JButton enrollButton;

    private JLabel efirstNameLabel;

    private JTextField efirstNameTextField;

    private JLabel elastNameLabel;

    private JTextField elastNameTextField;

    private JLabel eidLabel;

    private JTextField eidTextField;

    private JTabbedPane tabPane;

    private JPanel bodyPanel;

    private JPanel mainPanel;

    private JPanel header;

    private Box boxAfterClose;

    private JPanel resultPanel;

    private JTextField resultTextField;

    private static BufferedImage minimizeImg, closeImg, minimizeOverImg, closeOverImg, logoImg;
}
