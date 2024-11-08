package portablepgp;

import java.io.OutputStream;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import java.util.Collection;
import org.bouncycastle.openpgp.PGPException;
import org.jdesktop.application.ResourceMap;
import portablepgp.core.PublicKeyCollection;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import portablepgp.core.DecryptionResult;
import portablepgp.core.PGPUtils;
import portablepgp.core.PrintablePGPPublicKey;
import portablepgp.core.PrintablePGPPublicKeyRing;
import portablepgp.core.PrintablePGPSecretKey;
import portablepgp.core.PrintablePGPSecretKeyRing;
import static portablepgp.core.PGPUtils.*;

/**
 * @author Primiano Tucci - http://www.primianotucci.com/
 * @author Garfield Geng, garfield.geng@gmail.com
 * @version $Revision: 1.13 $ $Date: 2011/07/02 06:49:41 $
 */
public class PortablePGPView extends FrameView {

    private final ResourceMap resMap = getResourceMap();

    private DefaultListModel privateKeys;

    private DefaultListModel publicKeys;

    private PublicKeyCollection multipleKeyCollection = new PublicKeyCollection();

    public PortablePGPView(SingleFrameApplication app) {
        super(app);
        getFrame().setResizable(false);
        initComponents();
    }

    /**
     * these functions should not be in the constructor of PortablePGPView.
     */
    public void init() {
        getFrame().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        privateKeys = ((DefaultListModel) listPrivateKeys.getModel());
        publicKeys = ((DefaultListModel) listPublicKeys.getModel());
        updatePubKeysList();
        updatePrivKeysList();
    }

    public void popupWelcome() {
        if (privateKeys.isEmpty()) {
            new WelcomeDialog(this).setVisible(true);
        }
    }

    private void popupLayer(Component p) {
        for (Component c : layers.getComponents()) {
            if (c != p) {
                c.setVisible(false);
            }
        }
        p.setVisible(true);
        layers.setPosition(p, 0);
    }

    private void browseFile(JTextField target) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(getFrame()) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        target.setText(chooser.getSelectedFile().getPath());
    }

    private boolean confirmDelete() {
        String prompt0 = resMap.getString("Are.you.absolutely.sure.you.want.to.delete.this.key");
        String prompt1 = resMap.getString("Confirm.delete");
        return JOptionPane.showConfirmDialog(getFrame(), prompt0, prompt1, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void deleteSelectedPublicKey() {
        if (listPublicKeys.getSelectedValue() == null) {
            return;
        }
        if (!confirmDelete()) {
            return;
        }
        try {
            PrintablePGPPublicKeyRing k = (PrintablePGPPublicKeyRing) listPublicKeys.getSelectedValue();
            PGPUtils.deletePublicKey(k.getPublicKeyRing());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(getFrame(), ex.getMessage(), Error, JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
        updatePubKeysList();
    }

    private String getPassphrase(String iTitle) {
        final JPasswordField jPasswordField = new JPasswordField();
        JOptionPane jOptionPane = new JOptionPane(jPasswordField, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = jOptionPane.createDialog(iTitle);
        dialog.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentShown(ComponentEvent e) {
                jPasswordField.requestFocusInWindow();
            }
        });
        dialog.setVisible(true);
        dialog.dispose();
        if (null == jOptionPane.getValue()) {
            return null;
        }
        if (JOptionPane.OK_OPTION == Integer.parseInt(jOptionPane.getValue().toString())) {
            return new String(jPasswordField.getPassword());
        } else {
            return null;
        }
    }

    private String getPassphrase() {
        return getPassphrase(resMap.getString("Enter.passphrase"));
    }

    private void exportSelectedPublicKey() {
        if (listPublicKeys.getSelectedValue() == null) {
            return;
        }
        try {
            PrintablePGPPublicKeyRing k = (PrintablePGPPublicKeyRing) listPublicKeys.getSelectedValue();
            saveKey(k.getPublicKeyRing().getEncoded());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(getFrame(), ex.getMessage(), Error, JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void exportSelectedPrivateKey() {
        if (listPrivateKeys.getSelectedValue() == null) {
            return;
        }
        try {
            PrintablePGPSecretKeyRing k = (PrintablePGPSecretKeyRing) listPrivateKeys.getSelectedValue();
            saveKey(k.getSecretKeyRing().getEncoded());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(getFrame(), ex.getMessage(), Error, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveKey(byte[] data) throws Exception {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(getFrame()) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File outFile = chooser.getSelectedFile();
        PGPUtils.saveKey(data, outFile);
        JOptionPane.showMessageDialog(getFrame(), resMap.getString("Exported.to") + BS + outFile.getPath());
    }

    private void doVerify() {
        try {
            getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            String res = null;
            if (rdVerifyFile.isSelected()) {
                FileInputStream fsignin = new FileInputStream(tbVerifyPath.getText());
                FileInputStream fdatain = new FileInputStream(tbVerifyPathFile.getText());
                res = PGPUtils.verifyFile(fdatain, fsignin);
            } else {
                res = PGPUtils.verifyText(tbVerifyText.getText());
            }
            if (res != null) {
                JOptionPane.showMessageDialog(getFrame(), resMap.getString("Verification.successful.", res), Ok, JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(getFrame(), resMap.getString("Verification.failed!"), Error, JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(getFrame(), ex.getMessage(), Error, JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } finally {
            getFrame().setCursor(Cursor.getDefaultCursor());
        }
    }

    private void doSign() {
        try {
            getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            String passwd = getPassphrase();
            if (passwd == null) {
                return;
            }
            PGPSecretKey sigKey = ((PrintablePGPSecretKeyRing) cbSignKey.getSelectedItem()).getSigningKey();
            if (rdSignFile.isSelected()) {
                JFileChooser chooser = new JFileChooser();
                chooser.setSelectedFile(new File(tbSignPath.getText() + PGPUtils.SIG));
                chooser.setDialogTitle(resMap.getString("Select.detach.sign.file.path"));
                if (chooser.showSaveDialog(getFrame()) != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                File outFile = chooser.getSelectedFile();
                FileOutputStream fout = new FileOutputStream(outFile);
                FileInputStream fin = new FileInputStream(tbSignPath.getText());
                PGPUtils.signFile(fin, fout, sigKey, passwd.toCharArray(), false);
                IOUtils.closeQuietly(fin);
                IOUtils.closeQuietly(fout);
                JOptionPane.showMessageDialog(getFrame(), resMap.getString("Detached.signature.stored.to.file") + BS + outFile.getPath(), Ok, JOptionPane.INFORMATION_MESSAGE);
            } else {
                String sText = PGPUtils.signText(formatLineBreak(tbSignText.getText()), sigKey, passwd.toCharArray());
                new TextShowDialog(getFrame(), sText).display();
            }
        } catch (PGPException ex0) {
            JOptionPane.showMessageDialog(getFrame(), resMap.getString("passphrase.privatekey.wrong"), Error, JOptionPane.ERROR_MESSAGE);
            ex0.printStackTrace();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(getFrame(), ex.getMessage(), Error, JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } finally {
            getFrame().setCursor(Cursor.getDefaultCursor());
        }
    }

    private void doDecrypt() {
        try {
            getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            String passwd = getPassphrase();
            if (passwd == null) {
                return;
            }
            DecryptionResult decryptionResult;
            if (rdDecryptFile.isSelected()) {
                File tmpFile = File.createTempFile("dcx", ".dec");
                FileOutputStream fout = new FileOutputStream(tmpFile);
                FileInputStream fin = new FileInputStream(tbDecPath.getText());
                decryptionResult = PGPUtils.decryptFile(fin, passwd.toCharArray(), fout);
                if (confirmDecryption(decryptionResult)) {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle(resMap.getString("Select.decrypted.file.path"));
                    chooser.setSelectedFile(new File(decryptionResult.getDecryptFileName()));
                    if (chooser.showSaveDialog(getFrame()) != JFileChooser.APPROVE_OPTION) {
                        return;
                    }
                    FileUtils.copyFile(tmpFile, chooser.getSelectedFile());
                }
                tmpFile.delete();
            } else {
                decryptionResult = PGPUtils.decryptText(tbDecText.getText(), passwd.toCharArray());
                String decText = decryptionResult.getDecryptedText();
                if (confirmDecryption(decryptionResult)) {
                    new TextShowDialog(getFrame(), decText).display();
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(getFrame(), ex.getMessage(), Error, JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } finally {
            getFrame().setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * doDecrypt called 2 times.
     */
    private boolean confirmDecryption(DecryptionResult res) {
        if (!res.isIsSigned()) {
            return true;
        } else if (res.isIsSigned() && res.isIsSignatureValid()) {
            JOptionPane.showMessageDialog(getFrame(), resMap.getString("Signature.Verification.succedeed") + BS + res.getSignee());
            return true;
        } else {
            String prompt0 = resMap.getString("Signature.for.the.message.is.NOT.valid");
            String prompt1 = resMap.getString("Do.you.want.to.proceed.and.decrypt.it.anyway");
            return JOptionPane.showConfirmDialog(getFrame(), prompt0 + BS + res.getSignatureException().getMessage() + prompt1, Warning, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
        }
    }

    private void doEncrypt() {
        try {
            getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            Object selObj = cbEncTarget.getSelectedItem();
            if (selObj == null) {
                return;
            }
            PGPSecretKey signKey = null;
            char[] signPasswd = null;
            String textToEnc = formatLineBreak(tbEncText.getText());
            if (cbEncSign.getSelectedIndex() > 0) {
                signKey = ((PrintablePGPSecretKeyRing) cbEncSign.getSelectedItem()).getSigningKey();
                String passwd = getPassphrase();
                if (passwd == null) {
                    return;
                }
                signPasswd = passwd.toCharArray();
            }
            Collection<PGPPublicKey> recipients = PGPUtils.getPGPPublicKeyCollection(selObj);
            if (rdEncryptFile.isSelected()) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle(resMap.getString("Select.encrypted.file.path"));
                if (chooser.showSaveDialog(getFrame()) != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                File outFile = chooser.getSelectedFile();
                FileOutputStream fout = new FileOutputStream(outFile);
                PGPUtils.encryptFile(fout, tbEncPath.getText(), recipients, signKey, ckEncAscii.isSelected(), ckEncIntegrity.isSelected(), signPasswd);
            } else {
                String encText = PGPUtils.encryptText(textToEnc, recipients, signKey, signPasswd);
                new TextShowDialog(getFrame(), encText).display();
            }
        } catch (PGPException ex0) {
            JOptionPane.showMessageDialog(getFrame(), resMap.getString("passphrase.privatekey.wrong"), Error, JOptionPane.ERROR_MESSAGE);
            ex0.printStackTrace();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(getFrame(), ex.getMessage(), Error, JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } finally {
            getFrame().setCursor(Cursor.getDefaultCursor());
        }
    }

    private void importPubKey() {
        JFileChooser chooser = new JFileChooser();
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            PGPPublicKeyRing ring = PGPUtils.importPublicKey(chooser.getSelectedFile());
            JOptionPane.showMessageDialog(getFrame(), resMap.getString("import.key.success") + BS + new PrintablePGPPublicKey(ring.getPublicKey()), Ok, JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(getFrame(), resMap.getString("Cannot.import.public.Key.") + BS + ex.getMessage(), Error, JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
        updatePubKeysList();
    }

    public void importPrivKey() {
        JFileChooser chooser = new JFileChooser();
        if (JFileChooser.APPROVE_OPTION != chooser.showOpenDialog(null)) {
            return;
        }
        try {
            PGPSecretKeyRing ring = PGPUtils.importPrivateKey(chooser.getSelectedFile());
            JOptionPane.showMessageDialog(getFrame(), resMap.getString("import.key.success") + BS + new PrintablePGPSecretKey(ring.getSecretKey()), Ok, JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(getFrame(), resMap.getString("Cannot.import.private.Key.") + ex.getMessage(), Error, JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
        updatePrivKeysList();
        updatePubKeysList();
    }

    public void newKeyPair() {
        NewKeyDialog dialog = new NewKeyDialog(this);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    public void updatePubKeysList() {
        publicKeys.clear();
        for (PrintablePGPPublicKeyRing k : PGPUtils.getPublicKeys()) {
            publicKeys.addElement(k);
        }
        DefaultComboBoxModel encMod = new DefaultComboBoxModel(publicKeys.toArray());
        encMod.insertElementAt(multipleKeyCollection, 0);
        cbEncTarget.setModel(encMod);
        if (encMod.getSize() > 1) {
            cbEncTarget.setSelectedIndex(1);
        }
    }

    public void updatePrivKeysList() {
        privateKeys.clear();
        for (PrintablePGPSecretKeyRing k : PGPUtils.getPrivateKeys()) {
            privateKeys.addElement(k);
        }
        cbSignKey.setModel(new DefaultComboBoxModel(privateKeys.toArray()));
        DefaultComboBoxModel encSignModel = new DefaultComboBoxModel(privateKeys.toArray());
        encSignModel.insertElementAt(resMap.getString("No.signature,just.encrypt"), 0);
        cbEncSign.setModel(encSignModel);
        cbEncSign.setSelectedIndex(0);
    }

    private void selectMultipleRecipients() {
        SelectRecipientsDialog frm = new SelectRecipientsDialog(multipleKeyCollection.getKeys());
        frm.setVisible(true);
        if (frm.hasCancelled()) {
            return;
        }
        multipleKeyCollection.setKeys(frm.getSelectedKeys());
    }

    private void deleteSelectedPrivateKey() {
        if (listPrivateKeys.getSelectedValue() == null) {
            return;
        }
        if (!confirmDelete()) {
            return;
        }
        try {
            PrintablePGPSecretKeyRing k = (PrintablePGPSecretKeyRing) listPrivateKeys.getSelectedValue();
            PGPUtils.deletePrivateKey(k.getSecretKeyRing());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(getFrame(), ex.getMessage(), Error, JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
        updatePrivKeysList();
    }

    private void initComponents() {
        mainPanel = new javax.swing.JPanel();
        panLeftMenu = new javax.swing.JPanel();
        btn_encrypt = new javax.swing.JButton();
        btn_decrypt = new javax.swing.JButton();
        btn_sign = new javax.swing.JButton();
        btn_verify = new javax.swing.JButton();
        btn_keyring = new javax.swing.JButton();
        panTop = new javax.swing.JPanel();
        lbAuthor1 = new javax.swing.JLabel();
        lbAuthor2 = new javax.swing.JLabel();
        header = new javax.swing.JLabel();
        layers = new javax.swing.JLayeredPane();
        panKeys = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        lbPrivateKeys = new javax.swing.JLabel();
        btImportPrivatekey = new javax.swing.JButton();
        btExportPrivatekey = new javax.swing.JButton();
        btDeletePrivatekey = new javax.swing.JButton();
        btNewPrivatekey = new javax.swing.JButton();
        btChangeKeyPass = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        listPrivateKeys = new javax.swing.JList();
        jPanel3 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        lbPublicKeys = new javax.swing.JLabel();
        btImportPubkey = new javax.swing.JButton();
        btExportPublicKey = new javax.swing.JButton();
        btDeletePubkey = new javax.swing.JButton();
        btSearchKey = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        listPublicKeys = new javax.swing.JList();
        panSign = new javax.swing.JPanel();
        rdSignFile = new javax.swing.JRadioButton();
        rdSignText = new javax.swing.JRadioButton();
        lbSignPath = new javax.swing.JLabel();
        tbSignPath = new javax.swing.JTextField();
        btSignBrowse = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        tbSignText = new javax.swing.JTextArea();
        lbPublicKeys2 = new javax.swing.JLabel();
        cbSignKey = new javax.swing.JComboBox();
        btSign = new javax.swing.JButton();
        panDecrypt = new javax.swing.JPanel();
        rdDecryptFile = new javax.swing.JRadioButton();
        rdDecryptText = new javax.swing.JRadioButton();
        lbDecPath = new javax.swing.JLabel();
        tbDecPath = new javax.swing.JTextField();
        btDecBrowse = new javax.swing.JButton();
        btDecrypt = new javax.swing.JButton();
        btDecPaste = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        tbDecText = new javax.swing.JTextArea();
        panEncrypt = new javax.swing.JPanel();
        rdEncryptFile = new javax.swing.JRadioButton();
        rdEncryptText = new javax.swing.JRadioButton();
        lbEncPath = new javax.swing.JLabel();
        tbEncPath = new javax.swing.JTextField();
        btEncBrowse = new javax.swing.JButton();
        lbPublicKeys1 = new javax.swing.JLabel();
        cbEncTarget = new javax.swing.JComboBox();
        btEncrypt = new javax.swing.JButton();
        ckEncAscii = new javax.swing.JCheckBox();
        ckEncIntegrity = new javax.swing.JCheckBox();
        cbEncSign = new javax.swing.JComboBox();
        lbPublicKeys3 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tbEncText = new javax.swing.JTextArea();
        panVerify = new javax.swing.JPanel();
        rdVerifyFile = new javax.swing.JRadioButton();
        rdVerifyText = new javax.swing.JRadioButton();
        lbVerifyPath = new javax.swing.JLabel();
        tbVerifyPath = new javax.swing.JTextField();
        btVerifyBrowse = new javax.swing.JButton();
        jScrollPane6 = new javax.swing.JScrollPane();
        tbVerifyText = new javax.swing.JTextArea();
        btVerify = new javax.swing.JButton();
        lbVerifyPathFile = new javax.swing.JLabel();
        tbVerifyPathFile = new javax.swing.JTextField();
        btVerifyBrowseFile = new javax.swing.JButton();
        btVerifyPaste = new javax.swing.JButton();
        lbI18nContactEmail = new javax.swing.JLabel();
        lbI18nName = new javax.swing.JLabel();
        lbI18nTranslator = new javax.swing.JLabel();
        lbAppVersion = new javax.swing.JLabel();
        btgEncrypt = new javax.swing.ButtonGroup();
        btgSign = new javax.swing.ButtonGroup();
        btgDecrypt = new javax.swing.ButtonGroup();
        btgVerify = new javax.swing.ButtonGroup();
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(portablepgp.PortablePGPApp.class).getContext().getResourceMap(PortablePGPView.class);
        mainPanel.setBackground(resourceMap.getColor("mainPanel.background"));
        mainPanel.setFont(resourceMap.getFont("mainPanel.font"));
        mainPanel.setMaximumSize(new java.awt.Dimension(600, 500));
        mainPanel.setMinimumSize(new java.awt.Dimension(600, 500));
        mainPanel.setName("mainPanel");
        mainPanel.setPreferredSize(new java.awt.Dimension(600, 500));
        panLeftMenu.setBackground(resourceMap.getColor("panLeftMenu.background"));
        panLeftMenu.setName("panLeftMenu");
        panLeftMenu.setLayout(new java.awt.GridLayout(5, 1, 0, 4));
        btn_encrypt.setBackground(resourceMap.getColor("btn_encrypt.background"));
        btn_encrypt.setIcon(resourceMap.getIcon("btn_encrypt.icon"));
        btn_encrypt.setText(resourceMap.getString("btn_encrypt.text"));
        btn_encrypt.setBorder(null);
        btn_encrypt.setBorderPainted(false);
        btn_encrypt.setFocusPainted(false);
        btn_encrypt.setName("btn_encrypt");
        btn_encrypt.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_encryptActionPerformed(evt);
            }
        });
        panLeftMenu.add(btn_encrypt);
        btn_decrypt.setBackground(resourceMap.getColor("btn_decrypt.background"));
        btn_decrypt.setIcon(resourceMap.getIcon("btn_decrypt.icon"));
        btn_decrypt.setBorder(null);
        btn_decrypt.setBorderPainted(false);
        btn_decrypt.setFocusPainted(false);
        btn_decrypt.setName("btn_decrypt");
        btn_decrypt.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_decryptActionPerformed(evt);
            }
        });
        panLeftMenu.add(btn_decrypt);
        btn_sign.setBackground(resourceMap.getColor("btn_sign.background"));
        btn_sign.setIcon(resourceMap.getIcon("btn_sign.icon"));
        btn_sign.setBorder(null);
        btn_sign.setBorderPainted(false);
        btn_sign.setFocusPainted(false);
        btn_sign.setName("btn_sign");
        btn_sign.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_signActionPerformed(evt);
            }
        });
        panLeftMenu.add(btn_sign);
        btn_verify.setBackground(resourceMap.getColor("btn_verify.background"));
        btn_verify.setIcon(resourceMap.getIcon("btn_verify.icon"));
        btn_verify.setBorder(null);
        btn_verify.setBorderPainted(false);
        btn_verify.setFocusPainted(false);
        btn_verify.setName("btn_verify");
        btn_verify.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_verifyActionPerformed(evt);
            }
        });
        panLeftMenu.add(btn_verify);
        btn_keyring.setBackground(resourceMap.getColor("btn_keyring.background"));
        btn_keyring.setIcon(resourceMap.getIcon("btn_keyring.icon"));
        btn_keyring.setBorder(null);
        btn_keyring.setBorderPainted(false);
        btn_keyring.setFocusPainted(false);
        btn_keyring.setName("btn_keyring");
        btn_keyring.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_keyringActionPerformed(evt);
            }
        });
        panLeftMenu.add(btn_keyring);
        panTop.setBackground(new java.awt.Color(255, 255, 255));
        panTop.setMinimumSize(new java.awt.Dimension(600, 113));
        panTop.setName("panTop");
        panTop.setPreferredSize(new java.awt.Dimension(600, 113));
        panTop.setLayout(null);
        lbAuthor1.setFont(resourceMap.getFont("email.font"));
        lbAuthor1.setForeground(resourceMap.getColor("lbAuthor.foreground"));
        lbAuthor1.setText(resourceMap.getString("lbAuthor1.text"));
        lbAuthor1.setName("lbAuthor1");
        panTop.add(lbAuthor1);
        lbAuthor1.setBounds(203, 89, 383, 15);
        lbAuthor2.setFont(resourceMap.getFont("email.font"));
        lbAuthor2.setForeground(resourceMap.getColor("lbAuthor.foreground"));
        lbAuthor2.setText(resourceMap.getString("lbAuthor2.text"));
        lbAuthor2.setName("lbAuthor2");
        panTop.add(lbAuthor2);
        lbAuthor2.setBounds(203, 72, 383, 15);
        header.setFont(resourceMap.getFont("small.font"));
        header.setIcon(resourceMap.getIcon("header_img"));
        header.setText(resourceMap.getString("header.text"));
        header.setName("header");
        panTop.add(header);
        header.setBounds(0, 0, 600, 113);
        layers.setBackground(resourceMap.getColor("layers.background"));
        layers.setMinimumSize(new java.awt.Dimension(440, 380));
        layers.setName("layers");
        panKeys.setBackground(resourceMap.getColor("panKeys.background"));
        panKeys.setFont(resourceMap.getFont("mainPanel.font"));
        panKeys.setMaximumSize(new java.awt.Dimension(440, 381));
        panKeys.setMinimumSize(new java.awt.Dimension(440, 381));
        panKeys.setName("panKeys");
        panKeys.setPreferredSize(new java.awt.Dimension(440, 381));
        panKeys.setRequestFocusEnabled(false);
        panKeys.setLayout(new java.awt.GridLayout(2, 0));
        jPanel2.setName("jPanel2");
        jPanel2.setLayout(new java.awt.BorderLayout());
        jPanel4.setName("jPanel4");
        jPanel4.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbPrivateKeys.setFont(resourceMap.getFont("mainPanel.font"));
        lbPrivateKeys.setIcon(resourceMap.getIcon("lbPrivateKeys.icon"));
        lbPrivateKeys.setText(resourceMap.getString("lbPrivateKeys.text"));
        lbPrivateKeys.setName("lbPrivateKeys");
        jPanel4.add(lbPrivateKeys, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));
        btImportPrivatekey.setBackground(resourceMap.getColor("btDeletePrivatekey.background"));
        btImportPrivatekey.setIcon(resourceMap.getIcon("btImportPrivatekey.icon"));
        btImportPrivatekey.setText(resourceMap.getString("btImportPrivatekey.text"));
        btImportPrivatekey.setToolTipText(resourceMap.getString("btImportPrivatekey.toolTipText"));
        btImportPrivatekey.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btImportPrivatekey.setFocusPainted(false);
        btImportPrivatekey.setMaximumSize(new java.awt.Dimension(32, 32));
        btImportPrivatekey.setMinimumSize(new java.awt.Dimension(32, 32));
        btImportPrivatekey.setName("btImportPrivatekey");
        btImportPrivatekey.setPreferredSize(new java.awt.Dimension(32, 32));
        btImportPrivatekey.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btImportPrivatekeyActionPerformed(evt);
            }
        });
        jPanel4.add(btImportPrivatekey, new org.netbeans.lib.awtextra.AbsoluteConstraints(132, 2, -1, 30));
        btExportPrivatekey.setBackground(resourceMap.getColor("btExportPrivatekey.background"));
        btExportPrivatekey.setIcon(resourceMap.getIcon("btExportPrivatekey.icon"));
        btExportPrivatekey.setToolTipText(resourceMap.getString("btExportPrivatekey.toolTipText"));
        btExportPrivatekey.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btExportPrivatekey.setFocusPainted(false);
        btExportPrivatekey.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btExportPrivatekey.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btExportPrivatekey.setMaximumSize(new java.awt.Dimension(32, 32));
        btExportPrivatekey.setMinimumSize(new java.awt.Dimension(32, 32));
        btExportPrivatekey.setName("btExportPrivatekey");
        btExportPrivatekey.setPreferredSize(new java.awt.Dimension(32, 32));
        btExportPrivatekey.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btExportPrivatekeyActionPerformed(evt);
            }
        });
        jPanel4.add(btExportPrivatekey, new org.netbeans.lib.awtextra.AbsoluteConstraints(174, 2, -1, 30));
        btDeletePrivatekey.setBackground(resourceMap.getColor("btDeletePrivatekey.background"));
        btDeletePrivatekey.setIcon(resourceMap.getIcon("btDeletePrivatekey.icon"));
        btDeletePrivatekey.setText(resourceMap.getString("btDeletePrivatekey.text"));
        btDeletePrivatekey.setToolTipText(resourceMap.getString("btDeletePrivatekey.toolTipText"));
        btDeletePrivatekey.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btDeletePrivatekey.setFocusPainted(false);
        btDeletePrivatekey.setMaximumSize(new java.awt.Dimension(32, 32));
        btDeletePrivatekey.setMinimumSize(new java.awt.Dimension(32, 32));
        btDeletePrivatekey.setName("btDeletePrivatekey");
        btDeletePrivatekey.setPreferredSize(new java.awt.Dimension(32, 32));
        btDeletePrivatekey.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btDeletePrivatekeyActionPerformed(evt);
            }
        });
        jPanel4.add(btDeletePrivatekey, new org.netbeans.lib.awtextra.AbsoluteConstraints(212, 2, -1, 30));
        btNewPrivatekey.setBackground(resourceMap.getColor("btDeletePrivatekey.background"));
        btNewPrivatekey.setIcon(resourceMap.getIcon("btNewPrivatekey.icon"));
        btNewPrivatekey.setText(resourceMap.getString("btNewPrivatekey.text"));
        btNewPrivatekey.setToolTipText(resourceMap.getString("btNewPrivatekey.toolTipText"));
        btNewPrivatekey.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btNewPrivatekey.setFocusPainted(false);
        btNewPrivatekey.setMaximumSize(new java.awt.Dimension(32, 32));
        btNewPrivatekey.setMinimumSize(new java.awt.Dimension(32, 32));
        btNewPrivatekey.setName("btNewPrivatekey");
        btNewPrivatekey.setPreferredSize(new java.awt.Dimension(32, 32));
        btNewPrivatekey.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btNewPrivatekeyActionPerformed(evt);
            }
        });
        jPanel4.add(btNewPrivatekey, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 2, -1, 30));
        btChangeKeyPass.setBackground(resourceMap.getColor("btChangeKeyPass.background"));
        btChangeKeyPass.setIcon(resourceMap.getIcon("btChangeKeyPass.icon"));
        btChangeKeyPass.setToolTipText(resourceMap.getString("btChangeKeyPass.toolTipText"));
        btChangeKeyPass.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btChangeKeyPass.setFocusPainted(false);
        btChangeKeyPass.setMaximumSize(new java.awt.Dimension(32, 32));
        btChangeKeyPass.setMinimumSize(new java.awt.Dimension(32, 32));
        btChangeKeyPass.setName("btChangeKeyPass");
        btChangeKeyPass.setPreferredSize(new java.awt.Dimension(32, 32));
        btChangeKeyPass.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btChangeKeyPassActionPerformed(evt);
            }
        });
        btChangeKeyPass.setVisible(false);
        jPanel4.add(btChangeKeyPass, new org.netbeans.lib.awtextra.AbsoluteConstraints(288, 2, -1, 30));
        jPanel2.add(jPanel4, java.awt.BorderLayout.NORTH);
        jScrollPane1.setName("jScrollPane1");
        listPrivateKeys.setModel(new DefaultListModel());
        listPrivateKeys.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listPrivateKeys.setName("listPrivateKeys");
        jScrollPane1.setViewportView(listPrivateKeys);
        jPanel2.add(jScrollPane1, java.awt.BorderLayout.CENTER);
        panKeys.add(jPanel2);
        jPanel3.setName("jPanel3");
        jPanel3.setLayout(new java.awt.BorderLayout());
        jPanel5.setName("jPanel5");
        jPanel5.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbPublicKeys.setFont(resourceMap.getFont("mainPanel.font"));
        lbPublicKeys.setIcon(resourceMap.getIcon("lbPublicKeys.icon"));
        lbPublicKeys.setText(resourceMap.getString("lbPublicKeys.text"));
        lbPublicKeys.setName("lbPublicKeys");
        jPanel5.add(lbPublicKeys, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 3, -1, -1));
        btImportPubkey.setBackground(resourceMap.getColor("btDeletePrivatekey.background"));
        btImportPubkey.setIcon(resourceMap.getIcon("btImportPubkey.icon"));
        btImportPubkey.setText(resourceMap.getString("btImportPubkey.text"));
        btImportPubkey.setToolTipText(resourceMap.getString("btImportPubkey.toolTipText"));
        btImportPubkey.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btImportPubkey.setFocusPainted(false);
        btImportPubkey.setName("btImportPubkey");
        btImportPubkey.setPreferredSize(new java.awt.Dimension(32, 32));
        btImportPubkey.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btImportPubkeyActionPerformed(evt);
            }
        });
        jPanel5.add(btImportPubkey, new org.netbeans.lib.awtextra.AbsoluteConstraints(131, 4, 34, 31));
        btExportPublicKey.setBackground(resourceMap.getColor("btExportPublicKey.background"));
        btExportPublicKey.setIcon(resourceMap.getIcon("btExportPublicKey.icon"));
        btExportPublicKey.setToolTipText(resourceMap.getString("btExportPublicKey.toolTipText"));
        btExportPublicKey.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btExportPublicKey.setFocusPainted(false);
        btExportPublicKey.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btExportPublicKey.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btExportPublicKey.setName("btExportPublicKey");
        btExportPublicKey.setPreferredSize(new java.awt.Dimension(32, 32));
        btExportPublicKey.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btExportPublicKeyActionPerformed(evt);
            }
        });
        jPanel5.add(btExportPublicKey, new org.netbeans.lib.awtextra.AbsoluteConstraints(175, 4, 30, 30));
        btDeletePubkey.setBackground(resourceMap.getColor("btDeletePrivatekey.background"));
        btDeletePubkey.setIcon(resourceMap.getIcon("btDeletePubkey.icon"));
        btDeletePubkey.setText(resourceMap.getString("btDeletePubkey.text"));
        btDeletePubkey.setToolTipText(resourceMap.getString("btDeletePubkey.toolTipText"));
        btDeletePubkey.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btDeletePubkey.setFocusPainted(false);
        btDeletePubkey.setName("btDeletePubkey");
        btDeletePubkey.setPreferredSize(new java.awt.Dimension(32, 32));
        btDeletePubkey.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btDeletePubkeyActionPerformed(evt);
            }
        });
        jPanel5.add(btDeletePubkey, new org.netbeans.lib.awtextra.AbsoluteConstraints(215, 4, 30, 30));
        btSearchKey.setBackground(resourceMap.getColor("btSearchKey.background"));
        btSearchKey.setIcon(resourceMap.getIcon("btSearchKey.icon"));
        btSearchKey.setToolTipText(resourceMap.getString("btSearchKey.toolTipText"));
        btSearchKey.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btSearchKey.setFocusPainted(false);
        btSearchKey.setName("btSearchKey");
        btSearchKey.setPreferredSize(new java.awt.Dimension(32, 32));
        btSearchKey.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btSearchKeyActionPerformed(evt);
            }
        });
        btSearchKey.setVisible(false);
        jPanel5.add(btSearchKey, new org.netbeans.lib.awtextra.AbsoluteConstraints(255, 4, 31, 31));
        jPanel3.add(jPanel5, java.awt.BorderLayout.NORTH);
        jScrollPane2.setName("jScrollPane2");
        listPublicKeys.setModel(new DefaultListModel());
        listPublicKeys.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listPublicKeys.setName("listPublicKeys");
        jScrollPane2.setViewportView(listPublicKeys);
        jPanel3.add(jScrollPane2, java.awt.BorderLayout.CENTER);
        panKeys.add(jPanel3);
        panKeys.setBounds(0, 0, 440, 381);
        layers.add(panKeys, javax.swing.JLayeredPane.POPUP_LAYER);
        panSign.setBackground(resourceMap.getColor("panSign.background"));
        panSign.setFont(resourceMap.getFont("mainPanel.font"));
        panSign.setMaximumSize(new java.awt.Dimension(440, 381));
        panSign.setMinimumSize(new java.awt.Dimension(440, 381));
        panSign.setName("panSign");
        btgSign.add(rdSignFile);
        rdSignFile.setFont(resourceMap.getFont("mainPanel.font"));
        rdSignFile.setText(resourceMap.getString("rdSignFile.text"));
        rdSignFile.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        rdSignFile.setName("rdSignFile");
        rdSignFile.setOpaque(false);
        rdSignFile.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rdSignFileStateChanged(evt);
            }
        });
        btgSign.add(rdSignText);
        rdSignText.setFont(resourceMap.getFont("mainPanel.font"));
        rdSignText.setSelected(true);
        rdSignText.setText(resourceMap.getString("rdSignText.text"));
        rdSignText.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        rdSignText.setName("rdSignText");
        rdSignText.setOpaque(false);
        rdSignText.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rdSignTextStateChanged(evt);
            }
        });
        lbSignPath.setFont(resourceMap.getFont("mainPanel.font"));
        lbSignPath.setText(resourceMap.getString("lbSignPath.text"));
        lbSignPath.setEnabled(false);
        lbSignPath.setName("lbSignPath");
        tbSignPath.setFont(resourceMap.getFont("mainPanel.font"));
        tbSignPath.setEnabled(false);
        tbSignPath.setName("tbSignPath");
        btSignBrowse.setFont(resourceMap.getFont("mainPanel.font"));
        btSignBrowse.setText(resourceMap.getString("btSignBrowse.text"));
        btSignBrowse.setEnabled(false);
        btSignBrowse.setName("btSignBrowse");
        btSignBrowse.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btSignBrowseActionPerformed(evt);
            }
        });
        jScrollPane5.setAutoscrolls(true);
        jScrollPane5.setName("jScrollPane5");
        tbSignText.setColumns(20);
        tbSignText.setFont(resourceMap.getFont("mainPanel.font"));
        tbSignText.setRows(5);
        tbSignText.setName("tbSignText");
        jScrollPane5.setViewportView(tbSignText);
        lbPublicKeys2.setFont(resourceMap.getFont("mainPanel.font"));
        lbPublicKeys2.setText(resourceMap.getString("lbPublicKeys2.text"));
        lbPublicKeys2.setName("lbPublicKeys2");
        cbSignKey.setFont(resourceMap.getFont("mainPanel.font"));
        cbSignKey.setName("cbSignKey");
        btSign.setBackground(resourceMap.getColor("btSign.background"));
        btSign.setFont(resourceMap.getFont("mainPanel.font"));
        btSign.setIcon(resourceMap.getIcon("btSign.icon"));
        btSign.setText(resourceMap.getString("btSign.text"));
        btSign.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btSign.setName("btSign");
        btSign.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btSignActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout panSignLayout = new javax.swing.GroupLayout(panSign);
        panSign.setLayout(panSignLayout);
        panSignLayout.setHorizontalGroup(panSignLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panSignLayout.createSequentialGroup().addContainerGap().addGroup(panSignLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panSignLayout.createSequentialGroup().addComponent(lbPublicKeys2).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(cbSignKey, javax.swing.GroupLayout.PREFERRED_SIZE, 274, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btSign, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE)).addComponent(rdSignFile, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE).addGroup(panSignLayout.createSequentialGroup().addComponent(lbSignPath).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(tbSignPath, javax.swing.GroupLayout.PREFERRED_SIZE, 257, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(9, 9, 9).addComponent(btSignBrowse, javax.swing.GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)).addComponent(rdSignText, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE).addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)).addContainerGap()));
        panSignLayout.setVerticalGroup(panSignLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panSignLayout.createSequentialGroup().addComponent(rdSignFile).addGap(7, 7, 7).addGroup(panSignLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lbSignPath).addComponent(tbSignPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(btSignBrowse)).addGap(18, 18, 18).addComponent(rdSignText).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 221, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panSignLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(cbSignKey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lbPublicKeys2).addComponent(btSign, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap()));
        panSign.setBounds(0, 0, 440, 381);
        layers.add(panSign, javax.swing.JLayeredPane.POPUP_LAYER);
        panDecrypt.setBackground(resourceMap.getColor("panDecrypt.background"));
        panDecrypt.setFont(resourceMap.getFont("mainPanel.font"));
        panDecrypt.setMinimumSize(new java.awt.Dimension(440, 381));
        panDecrypt.setName("panDecrypt");
        panDecrypt.setPreferredSize(new java.awt.Dimension(440, 381));
        btgDecrypt.add(rdDecryptFile);
        rdDecryptFile.setFont(resourceMap.getFont("mainPanel.font"));
        rdDecryptFile.setText(resourceMap.getString("rdDecryptFile.text"));
        rdDecryptFile.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        rdDecryptFile.setName("rdDecryptFile");
        rdDecryptFile.setOpaque(false);
        rdDecryptFile.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rdDecryptFileStateChanged(evt);
            }
        });
        btgDecrypt.add(rdDecryptText);
        rdDecryptText.setFont(resourceMap.getFont("mainPanel.font"));
        rdDecryptText.setSelected(true);
        rdDecryptText.setText(resourceMap.getString("rdDecryptText.text"));
        rdDecryptText.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        rdDecryptText.setName("rdDecryptText");
        rdDecryptText.setOpaque(false);
        rdDecryptText.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rdDecryptTextStateChanged(evt);
            }
        });
        lbDecPath.setFont(resourceMap.getFont("mainPanel.font"));
        lbDecPath.setText(resourceMap.getString("lbDecPath.text"));
        lbDecPath.setEnabled(false);
        lbDecPath.setName("lbDecPath");
        tbDecPath.setFont(resourceMap.getFont("mainPanel.font"));
        tbDecPath.setEnabled(false);
        tbDecPath.setName("tbDecPath");
        btDecBrowse.setFont(resourceMap.getFont("mainPanel.font"));
        btDecBrowse.setText(resourceMap.getString("btDecBrowse.text"));
        btDecBrowse.setEnabled(false);
        btDecBrowse.setName("btDecBrowse");
        btDecBrowse.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btDecBrowseActionPerformed(evt);
            }
        });
        btDecrypt.setBackground(resourceMap.getColor("btDecrypt.background"));
        btDecrypt.setFont(resourceMap.getFont("mainPanel.font"));
        btDecrypt.setIcon(resourceMap.getIcon("btDecrypt.icon"));
        btDecrypt.setText(resourceMap.getString("btDecrypt.text"));
        btDecrypt.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btDecrypt.setFocusPainted(false);
        btDecrypt.setName("btDecrypt");
        btDecrypt.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btDecryptActionPerformed(evt);
            }
        });
        btDecPaste.setBackground(resourceMap.getColor("btDecPaste.background"));
        btDecPaste.setFont(resourceMap.getFont("mainPanel.font"));
        btDecPaste.setIcon(resourceMap.getIcon("btDecPaste.icon"));
        btDecPaste.setText(resourceMap.getString("btDecPaste.text"));
        btDecPaste.setFocusPainted(false);
        btDecPaste.setName("btDecPaste");
        btDecPaste.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btDecPasteActionPerformed(evt);
            }
        });
        jScrollPane4.setName("jScrollPane4");
        tbDecText.setColumns(20);
        tbDecText.setFont(resourceMap.getFont("mainPanel.font"));
        tbDecText.setRows(5);
        tbDecText.setName("tbDecText");
        jScrollPane4.setViewportView(tbDecText);
        javax.swing.GroupLayout panDecryptLayout = new javax.swing.GroupLayout(panDecrypt);
        panDecrypt.setLayout(panDecryptLayout);
        panDecryptLayout.setHorizontalGroup(panDecryptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panDecryptLayout.createSequentialGroup().addGroup(panDecryptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panDecryptLayout.createSequentialGroup().addGap(21, 21, 21).addComponent(lbDecPath).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(tbDecPath, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(btDecBrowse, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)).addComponent(rdDecryptFile, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE).addGroup(panDecryptLayout.createSequentialGroup().addGap(21, 21, 21).addComponent(btDecPaste).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 50, Short.MAX_VALUE).addComponent(btDecrypt, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)).addGroup(panDecryptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addComponent(rdDecryptText, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(javax.swing.GroupLayout.Alignment.LEADING, panDecryptLayout.createSequentialGroup().addGap(21, 21, 21).addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 408, javax.swing.GroupLayout.PREFERRED_SIZE)))).addContainerGap()));
        panDecryptLayout.setVerticalGroup(panDecryptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panDecryptLayout.createSequentialGroup().addComponent(rdDecryptFile).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(panDecryptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lbDecPath).addComponent(tbDecPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(btDecBrowse)).addGap(18, 18, 18).addComponent(rdDecryptText).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 224, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panDecryptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(btDecrypt, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE).addComponent(btDecPaste, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addContainerGap()));
        panDecrypt.setBounds(0, 0, 440, 381);
        layers.add(panDecrypt, javax.swing.JLayeredPane.POPUP_LAYER);
        panEncrypt.setBackground(resourceMap.getColor("panEncrypt.background"));
        panEncrypt.setFont(resourceMap.getFont("mainPanel.font"));
        panEncrypt.setMinimumSize(new java.awt.Dimension(440, 381));
        panEncrypt.setName("panEncrypt");
        panEncrypt.setPreferredSize(new java.awt.Dimension(440, 381));
        btgEncrypt.add(rdEncryptFile);
        rdEncryptFile.setFont(resourceMap.getFont("mainPanel.font"));
        rdEncryptFile.setText(resourceMap.getString("rdEncryptFile.text"));
        rdEncryptFile.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        rdEncryptFile.setName("rdEncryptFile");
        rdEncryptFile.setOpaque(false);
        rdEncryptFile.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rdEncryptFileStateChanged(evt);
            }
        });
        btgEncrypt.add(rdEncryptText);
        rdEncryptText.setFont(resourceMap.getFont("mainPanel.font"));
        rdEncryptText.setSelected(true);
        rdEncryptText.setText(resourceMap.getString("rdEncryptText.text"));
        rdEncryptText.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        rdEncryptText.setName("rdEncryptText");
        rdEncryptText.setOpaque(false);
        rdEncryptText.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rdEncryptTextStateChanged(evt);
            }
        });
        lbEncPath.setFont(resourceMap.getFont("mainPanel.font"));
        lbEncPath.setText(resourceMap.getString("lbEncPath.text"));
        lbEncPath.setEnabled(false);
        lbEncPath.setName("lbEncPath");
        tbEncPath.setFont(resourceMap.getFont("mainPanel.font"));
        tbEncPath.setText(resourceMap.getString("tbEncPath.text"));
        tbEncPath.setEnabled(false);
        tbEncPath.setName("tbEncPath");
        btEncBrowse.setFont(resourceMap.getFont("mainPanel.font"));
        btEncBrowse.setText(resourceMap.getString("btEncBrowse.text"));
        btEncBrowse.setEnabled(false);
        btEncBrowse.setName("btEncBrowse");
        btEncBrowse.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btEncBrowseActionPerformed(evt);
            }
        });
        lbPublicKeys1.setFont(resourceMap.getFont("mainPanel.font"));
        lbPublicKeys1.setText(resourceMap.getString("lbPublicKeys1.text"));
        lbPublicKeys1.setName("lbPublicKeys1");
        cbEncTarget.setFont(resourceMap.getFont("mainPanel.font"));
        cbEncTarget.setName("cbEncTarget");
        cbEncTarget.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                cbEncTargetMouseReleased(evt);
            }
        });
        cbEncTarget.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbEncTargetActionPerformed(evt);
            }
        });
        btEncrypt.setFont(resourceMap.getFont("mainPanel.font"));
        btEncrypt.setIcon(resourceMap.getIcon("btEncrypt.icon"));
        btEncrypt.setText(resourceMap.getString("btEncrypt.text"));
        btEncrypt.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btEncrypt.setFocusPainted(false);
        btEncrypt.setName("btEncrypt");
        btEncrypt.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btEncryptActionPerformed(evt);
            }
        });
        ckEncAscii.setFont(resourceMap.getFont("mainPanel.font"));
        ckEncAscii.setText(resourceMap.getString("ckEncAscii.text"));
        ckEncAscii.setName("ckEncAscii");
        ckEncAscii.setOpaque(false);
        ckEncIntegrity.setFont(resourceMap.getFont("mainPanel.font"));
        ckEncIntegrity.setSelected(true);
        ckEncIntegrity.setText(resourceMap.getString("ckEncIntegrity.text"));
        ckEncIntegrity.setName("ckEncIntegrity");
        ckEncIntegrity.setOpaque(false);
        cbEncSign.setFont(resourceMap.getFont("mainPanel.font"));
        cbEncSign.setName("cbEncSign");
        lbPublicKeys3.setFont(resourceMap.getFont("mainPanel.font"));
        lbPublicKeys3.setText(resourceMap.getString("lbPublicKeys3.text"));
        lbPublicKeys3.setName("lbPublicKeys3");
        jScrollPane3.setName("jScrollPane3");
        tbEncText.setColumns(20);
        tbEncText.setFont(resourceMap.getFont("mainPanel.font"));
        tbEncText.setRows(5);
        tbEncText.setName("tbEncText");
        jScrollPane3.setViewportView(tbEncText);
        javax.swing.GroupLayout panEncryptLayout = new javax.swing.GroupLayout(panEncrypt);
        panEncrypt.setLayout(panEncryptLayout);
        panEncryptLayout.setHorizontalGroup(panEncryptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panEncryptLayout.createSequentialGroup().addGroup(panEncryptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addGroup(panEncryptLayout.createSequentialGroup().addGap(21, 21, 21).addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 411, Short.MAX_VALUE).addGap(10, 10, 10)).addComponent(rdEncryptText, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE).addComponent(rdEncryptFile, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE).addGroup(panEncryptLayout.createSequentialGroup().addContainerGap().addGroup(panEncryptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panEncryptLayout.createSequentialGroup().addGap(11, 11, 11).addComponent(lbEncPath).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panEncryptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panEncryptLayout.createSequentialGroup().addComponent(tbEncPath, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(btEncBrowse, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)).addGroup(panEncryptLayout.createSequentialGroup().addComponent(ckEncIntegrity).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(ckEncAscii))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, javax.swing.GroupLayout.PREFERRED_SIZE)).addGroup(panEncryptLayout.createSequentialGroup().addGroup(panEncryptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lbPublicKeys1).addComponent(lbPublicKeys3)).addGap(4, 4, 4).addGroup(panEncryptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(cbEncSign, 0, 251, Short.MAX_VALUE).addComponent(cbEncTarget, 0, 251, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btEncrypt, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(12, 12, 12))))).addGap(0, 0, 0)));
        panEncryptLayout.setVerticalGroup(panEncryptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panEncryptLayout.createSequentialGroup().addComponent(rdEncryptFile).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(panEncryptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lbEncPath).addComponent(tbEncPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(btEncBrowse)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panEncryptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(ckEncIntegrity).addComponent(ckEncAscii)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(rdEncryptText).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panEncryptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addGroup(panEncryptLayout.createSequentialGroup().addGroup(panEncryptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(cbEncTarget, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lbPublicKeys1)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 16, Short.MAX_VALUE).addGroup(panEncryptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lbPublicKeys3).addComponent(cbEncSign, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))).addComponent(btEncrypt, javax.swing.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE)).addContainerGap()));
        panEncrypt.setBounds(0, 0, 440, 381);
        layers.add(panEncrypt, javax.swing.JLayeredPane.POPUP_LAYER);
        panVerify.setBackground(resourceMap.getColor("panVerify.background"));
        panVerify.setFont(resourceMap.getFont("mainPanel.font"));
        panVerify.setMaximumSize(new java.awt.Dimension(440, 381));
        panVerify.setMinimumSize(new java.awt.Dimension(440, 381));
        panVerify.setName("panVerify");
        btgVerify.add(rdVerifyFile);
        rdVerifyFile.setFont(resourceMap.getFont("mainPanel.font"));
        rdVerifyFile.setText(resourceMap.getString("rdVerifyFile.text"));
        rdVerifyFile.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        rdVerifyFile.setName("rdVerifyFile");
        rdVerifyFile.setOpaque(false);
        rdVerifyFile.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rdVerifyFileStateChanged(evt);
            }
        });
        btgVerify.add(rdVerifyText);
        rdVerifyText.setFont(resourceMap.getFont("mainPanel.font"));
        rdVerifyText.setSelected(true);
        rdVerifyText.setText(resourceMap.getString("rdVerifyText.text"));
        rdVerifyText.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        rdVerifyText.setName("rdVerifyText");
        rdVerifyText.setOpaque(false);
        rdVerifyText.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rdVerifyTextStateChanged(evt);
            }
        });
        lbVerifyPath.setFont(resourceMap.getFont("mainPanel.font"));
        lbVerifyPath.setText(resourceMap.getString("lbVerifyPath.text"));
        lbVerifyPath.setEnabled(false);
        lbVerifyPath.setName("lbVerifyPath");
        tbVerifyPath.setFont(resourceMap.getFont("mainPanel.font"));
        tbVerifyPath.setEnabled(false);
        tbVerifyPath.setName("tbVerifyPath");
        tbVerifyPath.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbVerifyPathActionPerformed(evt);
            }
        });
        btVerifyBrowse.setFont(resourceMap.getFont("mainPanel.font"));
        btVerifyBrowse.setText(resourceMap.getString("btVerifyBrowse.text"));
        btVerifyBrowse.setEnabled(false);
        btVerifyBrowse.setName("btVerifyBrowse");
        btVerifyBrowse.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btVerifyBrowseActionPerformed(evt);
            }
        });
        jScrollPane6.setAutoscrolls(true);
        jScrollPane6.setName("jScrollPane6");
        tbVerifyText.setColumns(20);
        tbVerifyText.setFont(resourceMap.getFont("mainPanel.font"));
        tbVerifyText.setRows(5);
        tbVerifyText.setName("tbVerifyText");
        jScrollPane6.setViewportView(tbVerifyText);
        btVerify.setBackground(resourceMap.getColor("btVerify.background"));
        btVerify.setFont(resourceMap.getFont("mainPanel.font"));
        btVerify.setIcon(resourceMap.getIcon("btVerify.icon"));
        btVerify.setText(resourceMap.getString("btVerify.text"));
        btVerify.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btVerify.setName("btVerify");
        btVerify.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btVerifyActionPerformed(evt);
            }
        });
        lbVerifyPathFile.setFont(resourceMap.getFont("mainPanel.font"));
        lbVerifyPathFile.setText(resourceMap.getString("lbVerifyPathFile.text"));
        lbVerifyPathFile.setEnabled(false);
        lbVerifyPathFile.setName("lbVerifyPathFile");
        tbVerifyPathFile.setFont(resourceMap.getFont("mainPanel.font"));
        tbVerifyPathFile.setEnabled(false);
        tbVerifyPathFile.setName("tbVerifyPathFile");
        btVerifyBrowseFile.setFont(resourceMap.getFont("mainPanel.font"));
        btVerifyBrowseFile.setText(resourceMap.getString("btVerifyBrowseFile.text"));
        btVerifyBrowseFile.setEnabled(false);
        btVerifyBrowseFile.setName("btVerifyBrowseFile");
        btVerifyBrowseFile.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btVerifyBrowseFileActionPerformed(evt);
            }
        });
        btVerifyPaste.setBackground(resourceMap.getColor("btVerifyPaste.background"));
        btVerifyPaste.setFont(resourceMap.getFont("mainPanel.font"));
        btVerifyPaste.setIcon(resourceMap.getIcon("btVerifyPaste.icon"));
        btVerifyPaste.setText(resourceMap.getString("btVerifyPaste.text"));
        btVerifyPaste.setFocusPainted(false);
        btVerifyPaste.setName("btVerifyPaste");
        btVerifyPaste.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btVerifyPasteActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout panVerifyLayout = new javax.swing.GroupLayout(panVerify);
        panVerify.setLayout(panVerifyLayout);
        panVerifyLayout.setHorizontalGroup(panVerifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panVerifyLayout.createSequentialGroup().addGroup(panVerifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addGroup(javax.swing.GroupLayout.Alignment.LEADING, panVerifyLayout.createSequentialGroup().addGap(21, 21, 21).addGroup(panVerifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lbVerifyPathFile).addComponent(lbVerifyPath)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(panVerifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(tbVerifyPath, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(tbVerifyPathFile, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)).addGap(26, 26, 26).addGroup(panVerifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(btVerifyBrowseFile, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE).addComponent(btVerifyBrowse, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE))).addGroup(javax.swing.GroupLayout.Alignment.LEADING, panVerifyLayout.createSequentialGroup().addGap(21, 21, 21).addGroup(panVerifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panVerifyLayout.createSequentialGroup().addComponent(btVerifyPaste, javax.swing.GroupLayout.PREFERRED_SIZE, 276, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 33, Short.MAX_VALUE).addComponent(btVerify, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)).addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 409, Short.MAX_VALUE))).addGroup(javax.swing.GroupLayout.Alignment.LEADING, panVerifyLayout.createSequentialGroup().addContainerGap().addGroup(panVerifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(rdVerifyFile, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 424, Short.MAX_VALUE).addComponent(rdVerifyText, javax.swing.GroupLayout.DEFAULT_SIZE, 424, Short.MAX_VALUE)))).addContainerGap()));
        panVerifyLayout.setVerticalGroup(panVerifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panVerifyLayout.createSequentialGroup().addComponent(rdVerifyFile).addGap(7, 7, 7).addGroup(panVerifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lbVerifyPath).addComponent(tbVerifyPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(btVerifyBrowse)).addGap(2, 2, 2).addGroup(panVerifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lbVerifyPathFile).addComponent(tbVerifyPathFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(btVerifyBrowseFile)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(rdVerifyText).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 206, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(panVerifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btVerify, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(btVerifyPaste, 0, 0, Short.MAX_VALUE)).addContainerGap()));
        panVerify.setBounds(0, 0, 440, 381);
        layers.add(panVerify, javax.swing.JLayeredPane.POPUP_LAYER);
        lbI18nContactEmail.setFont(resourceMap.getFont("email.font"));
        lbI18nContactEmail.setForeground(resourceMap.getColor("lbAuthor.foreground"));
        lbI18nContactEmail.setText(resourceMap.getString("lbI18nContactEmail.text"));
        lbI18nContactEmail.setName("lbI18nContactEmail");
        lbI18nName.setFont(resourceMap.getFont("mainPanel.font"));
        lbI18nName.setForeground(resourceMap.getColor("lbAuthor.foreground"));
        lbI18nName.setText(resourceMap.getString("lbI18nName.text"));
        lbI18nName.setName("lbI18nName");
        lbI18nTranslator.setFont(resourceMap.getFont("mainPanel.font"));
        lbI18nTranslator.setForeground(resourceMap.getColor("lbAuthor.foreground"));
        lbI18nTranslator.setText(resourceMap.getString("lbI18nTranslator.text"));
        lbI18nTranslator.setName("lbI18nTranslator");
        lbAppVersion.setFont(resourceMap.getFont("mainPanel.font"));
        lbAppVersion.setForeground(resourceMap.getColor("lbAuthor.foreground"));
        lbAppVersion.setText(resourceMap.getString("lbAppVersion.text"));
        lbAppVersion.setName("lbAppVersion");
        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(mainPanelLayout.createSequentialGroup().addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainPanelLayout.createSequentialGroup().addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addGroup(mainPanelLayout.createSequentialGroup().addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(lbI18nName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(panLeftMenu, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addGap(8, 8, 8)).addComponent(lbI18nTranslator, javax.swing.GroupLayout.Alignment.LEADING).addComponent(lbI18nContactEmail, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)).addComponent(lbAppVersion, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(layers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addComponent(panTop, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap()));
        mainPanelLayout.setVerticalGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(mainPanelLayout.createSequentialGroup().addComponent(panTop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(mainPanelLayout.createSequentialGroup().addGap(28, 28, 28).addComponent(panLeftMenu, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(50, 50, 50).addComponent(lbAppVersion).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(lbI18nName, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(lbI18nTranslator).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(lbI18nContactEmail)).addGroup(mainPanelLayout.createSequentialGroup().addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(layers, javax.swing.GroupLayout.PREFERRED_SIZE, 384, javax.swing.GroupLayout.PREFERRED_SIZE))).addGap(25, 25, 25)));
        setComponent(mainPanel);
    }

    private void btImportPubkeyActionPerformed(java.awt.event.ActionEvent evt) {
        importPubKey();
    }

    private void btImportPrivatekeyActionPerformed(java.awt.event.ActionEvent evt) {
        importPrivKey();
    }

    private void btNewPrivatekeyActionPerformed(java.awt.event.ActionEvent evt) {
        newKeyPair();
    }

    private void btn_keyringActionPerformed(java.awt.event.ActionEvent evt) {
        popupLayer(panKeys);
    }

    private void btn_encryptActionPerformed(java.awt.event.ActionEvent evt) {
        popupLayer(panEncrypt);
    }

    private void rdEncryptFileStateChanged(javax.swing.event.ChangeEvent evt) {
        boolean f = rdEncryptFile.isSelected();
        tbEncPath.setEnabled(f);
        btEncBrowse.setEnabled(f);
        lbEncPath.setEnabled(f);
        ckEncAscii.setEnabled(f);
        ckEncIntegrity.setEnabled(f);
    }

    private void rdEncryptTextStateChanged(javax.swing.event.ChangeEvent evt) {
        tbEncText.setEnabled(rdEncryptText.isSelected());
    }

    private void btEncryptActionPerformed(java.awt.event.ActionEvent evt) {
        doEncrypt();
    }

    private void rdDecryptFileStateChanged(javax.swing.event.ChangeEvent evt) {
        boolean f = rdDecryptFile.isSelected();
        tbDecPath.setEnabled(f);
        btDecBrowse.setEnabled(f);
        lbDecPath.setEnabled(f);
    }

    private void rdDecryptTextStateChanged(javax.swing.event.ChangeEvent evt) {
        tbDecText.setEnabled(rdDecryptText.isSelected());
    }

    private void btDecryptActionPerformed(java.awt.event.ActionEvent evt) {
        doDecrypt();
    }

    private void btn_decryptActionPerformed(java.awt.event.ActionEvent evt) {
        popupLayer(panDecrypt);
    }

    private void rdSignFileStateChanged(javax.swing.event.ChangeEvent evt) {
        boolean is = rdSignFile.isSelected();
        tbSignPath.setEnabled(is);
        btSignBrowse.setEnabled(is);
        lbSignPath.setEnabled(is);
    }

    private void rdSignTextStateChanged(javax.swing.event.ChangeEvent evt) {
        tbSignText.setEnabled(rdSignText.isSelected());
    }

    private void btSignActionPerformed(java.awt.event.ActionEvent evt) {
        doSign();
    }

    private void btn_signActionPerformed(java.awt.event.ActionEvent evt) {
        popupLayer(panSign);
    }

    private void btExportPrivatekeyActionPerformed(java.awt.event.ActionEvent evt) {
        exportSelectedPrivateKey();
    }

    private void btExportPublicKeyActionPerformed(java.awt.event.ActionEvent evt) {
        exportSelectedPublicKey();
    }

    private void btDeletePrivatekeyActionPerformed(java.awt.event.ActionEvent evt) {
        deleteSelectedPrivateKey();
    }

    private void btDeletePubkeyActionPerformed(java.awt.event.ActionEvent evt) {
        deleteSelectedPublicKey();
    }

    private void btSignBrowseActionPerformed(java.awt.event.ActionEvent evt) {
        browseFile(tbSignPath);
    }

    private void btDecBrowseActionPerformed(java.awt.event.ActionEvent evt) {
        browseFile(tbDecPath);
    }

    private void btEncBrowseActionPerformed(java.awt.event.ActionEvent evt) {
        browseFile(tbEncPath);
    }

    private void btDecPasteActionPerformed(java.awt.event.ActionEvent evt) {
        tbDecText.setText(EMPTY);
        tbDecText.paste();
    }

    private void rdVerifyFileStateChanged(javax.swing.event.ChangeEvent evt) {
        boolean is = rdVerifyFile.isSelected();
        tbVerifyPath.setEnabled(is);
        btVerifyBrowse.setEnabled(is);
        lbVerifyPath.setEnabled(is);
        tbVerifyPathFile.setEnabled(is);
        btVerifyBrowseFile.setEnabled(is);
        lbVerifyPathFile.setEnabled(is);
    }

    private void rdVerifyTextStateChanged(javax.swing.event.ChangeEvent evt) {
        tbVerifyText.setEnabled(rdVerifyText.isSelected());
    }

    private void btVerifyBrowseActionPerformed(java.awt.event.ActionEvent evt) {
        browseFile(tbVerifyPath);
        tbVerifyPathFile.setText(tbVerifyPath.getText().replaceAll(".sig$", EMPTY));
    }

    private void btVerifyActionPerformed(java.awt.event.ActionEvent evt) {
        doVerify();
    }

    private void btVerifyBrowseFileActionPerformed(java.awt.event.ActionEvent evt) {
        browseFile(tbVerifyPathFile);
    }

    private void btVerifyPasteActionPerformed(java.awt.event.ActionEvent evt) {
        tbVerifyText.setText(EMPTY);
        tbVerifyText.paste();
    }

    private void tbVerifyPathActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void btn_verifyActionPerformed(java.awt.event.ActionEvent evt) {
        popupLayer(panVerify);
    }

    private void btSearchKeyActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void btChangeKeyPassActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void cbEncTargetMouseReleased(java.awt.event.MouseEvent evt) {
        if (cbEncTarget.getSelectedItem() instanceof PublicKeyCollection) {
            selectMultipleRecipients();
        }
    }

    private void cbEncTargetActionPerformed(java.awt.event.ActionEvent evt) {
        if (cbEncTarget.getSelectedItem() instanceof PublicKeyCollection) {
            selectMultipleRecipients();
        }
    }

    private javax.swing.JButton btChangeKeyPass;

    private javax.swing.JButton btDecBrowse;

    private javax.swing.JButton btDecPaste;

    private javax.swing.JButton btDecrypt;

    private javax.swing.JButton btDeletePrivatekey;

    private javax.swing.JButton btDeletePubkey;

    private javax.swing.JButton btEncBrowse;

    private javax.swing.JButton btEncrypt;

    private javax.swing.JButton btExportPrivatekey;

    private javax.swing.JButton btExportPublicKey;

    private javax.swing.JButton btImportPrivatekey;

    private javax.swing.JButton btImportPubkey;

    private javax.swing.JButton btNewPrivatekey;

    private javax.swing.JButton btSearchKey;

    private javax.swing.JButton btSign;

    private javax.swing.JButton btSignBrowse;

    private javax.swing.JButton btVerify;

    private javax.swing.JButton btVerifyBrowse;

    private javax.swing.JButton btVerifyBrowseFile;

    private javax.swing.JButton btVerifyPaste;

    private javax.swing.ButtonGroup btgDecrypt;

    private javax.swing.ButtonGroup btgEncrypt;

    private javax.swing.ButtonGroup btgSign;

    private javax.swing.ButtonGroup btgVerify;

    private javax.swing.JButton btn_decrypt;

    private javax.swing.JButton btn_encrypt;

    private javax.swing.JButton btn_keyring;

    private javax.swing.JButton btn_sign;

    private javax.swing.JButton btn_verify;

    private javax.swing.JComboBox cbEncSign;

    private javax.swing.JComboBox cbEncTarget;

    private javax.swing.JComboBox cbSignKey;

    private javax.swing.JCheckBox ckEncAscii;

    private javax.swing.JCheckBox ckEncIntegrity;

    private javax.swing.JLabel header;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JPanel jPanel4;

    private javax.swing.JPanel jPanel5;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JScrollPane jScrollPane3;

    private javax.swing.JScrollPane jScrollPane4;

    private javax.swing.JScrollPane jScrollPane5;

    private javax.swing.JScrollPane jScrollPane6;

    private javax.swing.JLayeredPane layers;

    private javax.swing.JLabel lbAppVersion;

    private javax.swing.JLabel lbAuthor1;

    private javax.swing.JLabel lbAuthor2;

    private javax.swing.JLabel lbDecPath;

    private javax.swing.JLabel lbEncPath;

    private javax.swing.JLabel lbI18nContactEmail;

    private javax.swing.JLabel lbI18nName;

    private javax.swing.JLabel lbI18nTranslator;

    private javax.swing.JLabel lbPrivateKeys;

    private javax.swing.JLabel lbPublicKeys;

    private javax.swing.JLabel lbPublicKeys1;

    private javax.swing.JLabel lbPublicKeys2;

    private javax.swing.JLabel lbPublicKeys3;

    private javax.swing.JLabel lbSignPath;

    private javax.swing.JLabel lbVerifyPath;

    private javax.swing.JLabel lbVerifyPathFile;

    private javax.swing.JList listPrivateKeys;

    private javax.swing.JList listPublicKeys;

    private javax.swing.JPanel mainPanel;

    private javax.swing.JPanel panDecrypt;

    private javax.swing.JPanel panEncrypt;

    private javax.swing.JPanel panKeys;

    private javax.swing.JPanel panLeftMenu;

    private javax.swing.JPanel panSign;

    private javax.swing.JPanel panTop;

    private javax.swing.JPanel panVerify;

    private javax.swing.JRadioButton rdDecryptFile;

    private javax.swing.JRadioButton rdDecryptText;

    private javax.swing.JRadioButton rdEncryptFile;

    private javax.swing.JRadioButton rdEncryptText;

    private javax.swing.JRadioButton rdSignFile;

    private javax.swing.JRadioButton rdSignText;

    private javax.swing.JRadioButton rdVerifyFile;

    private javax.swing.JRadioButton rdVerifyText;

    private javax.swing.JTextField tbDecPath;

    private javax.swing.JTextArea tbDecText;

    private javax.swing.JTextField tbEncPath;

    private javax.swing.JTextArea tbEncText;

    private javax.swing.JTextField tbSignPath;

    private javax.swing.JTextArea tbSignText;

    private javax.swing.JTextField tbVerifyPath;

    private javax.swing.JTextField tbVerifyPathFile;

    private javax.swing.JTextArea tbVerifyText;
}
