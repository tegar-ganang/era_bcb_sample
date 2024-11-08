package com.ca.directory.jxplorer.editor;

import com.ca.commons.cbutil.*;
import com.ca.directory.jxplorer.HelpIDs;
import com.ca.directory.jxplorer.JXConfig;
import com.ca.directory.jxplorer.JXplorer;
import java.security.SecureRandom;
import java.security.MessageDigest;
import com.ca.commons.cbutil.CBBase64;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class userpasswordeditor extends JDialog implements abstractbinaryeditor {

    /**
     * MD5.
     */
    public static final String MD5 = "MD5";

    /**
     * SHA.
     */
    public static final String SHA = "SHA";

    /**
     * SSHA.
     */
    public static final String SSHA = "SSHA";

    /**
     * SMD5.
     */
    public static final String SMD5 = "SMD5";

    protected JTextField oldPwd, newPwd;

    protected CBButton btnOK, btnCancel, btnHelp;

    protected editablebinary editMe = null;

    protected CBPanel display;

    protected JLabel oldLabel, newLabel;

    protected CBJComboBox comboType;

    protected boolean firstClick = true;

    protected boolean hidingPasswords = true;

    protected static int default_encryption = 4;

    private static Logger log = Logger.getLogger(userpasswordeditor.class.getName());

    /**
     * Constructor - sets up the gui.
     */
    public userpasswordeditor(Frame owner) {
        super(owner);
        setModal(true);
        setTitle(CBIntText.get("User Password"));
        display = new CBPanel();
        if (JXConfig.getProperty("mask.raw.passwords").equalsIgnoreCase("false")) {
            oldPwd = new JTextField();
            newPwd = new JTextField();
            hidingPasswords = false;
        } else {
            oldPwd = new JPasswordField();
            newPwd = new JPasswordField();
        }
        oldPwd.setBackground(Color.white);
        oldPwd.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
                if (firstClick) {
                    if (hidingPasswords) oldPwd.setText("");
                    firstClick = false;
                }
            }
        });
        newPwd.setBackground(Color.white);
        oldLabel = new JLabel(CBIntText.get("Enter Password") + ":");
        newLabel = new JLabel(CBIntText.get("Re-enter Password") + ":");
        btnOK = new CBButton(CBIntText.get("OK"), CBIntText.get("Click here to save the changes (remember to click Submit in the table editor)."));
        btnOK.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                load();
            }
        });
        btnCancel = new CBButton(CBIntText.get("Cancel"), CBIntText.get("Click here to exit."));
        btnCancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                quit();
            }
        });
        comboType = new CBJComboBox();
        comboType.addItem(CBIntText.get("verify"));
        comboType.addItem(CBIntText.get("plain"));
        comboType.addItem(CBIntText.get(MD5));
        comboType.addItem(CBIntText.get(SMD5));
        comboType.addItem(CBIntText.get(SHA));
        comboType.addItem(CBIntText.get(SSHA));
        comboType.setEditable(false);
        comboType.setSelectedIndex(default_encryption);
        comboType.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (comboType.getSelectedIndex() == 0) setNewPwdFieldEnabled(false); else setNewPwdFieldEnabled(true);
            }
        });
        comboType.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                default_encryption = comboType.getSelectedIndex();
            }
        });
        btnHelp = new CBButton(CBIntText.get("Help"), CBIntText.get("Click here for Help."));
        CBHelpSystem.useDefaultHelp(btnHelp, HelpIDs.ATTR_PASSWORD);
        display.makeHeavy();
        display.addln(oldLabel);
        display.addln(oldPwd);
        display.addln(newLabel);
        display.addln(newPwd);
        display.add(comboType);
        display.addln(new JLabel(" "));
        display.makeLight();
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnOK);
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnHelp);
        display.addln(buttonPanel);
        display.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "enter");
        display.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "escape");
        display.getActionMap().put("enter", new MyAction(CBAction.ENTER));
        display.getActionMap().put("escape", new MyAction(CBAction.ESCAPE));
        setSize(300, 170);
        CBUtility.center(this, owner);
        setTitle(CBIntText.get("User Password Data"));
        getContentPane().add(display);
        if (comboType.getSelectedIndex() == 0) setNewPwdFieldEnabled(false); else setNewPwdFieldEnabled(true);
    }

    /**
     * Enables or disables the newPwd password field.
     * @param enabled True to enable the newPwd password field and false to disable the newPwd password field.
     */
    protected void setNewPwdFieldEnabled(boolean enabled) {
        newPwd.setFocusable(enabled);
        newPwd.setEnabled(enabled);
        newLabel.setEnabled(enabled);
        if (enabled) newPwd.setBackground(Color.white); else newPwd.setBackground(Color.lightGray);
    }

    /**
     * Apparently it is better to use key bindings rather than adding a KeyListener... "for reacting in a special way to
     * particular keys, you usually should use key bindings instead of a key listener". This class lets the user set the
     * key as an int.  If a key is pressed and it matches the assigned int, a check is done for if it is an escape or
     * enter key. (27 or 10).  If escape, the quit method is called.  If enter, the apply method is called. Bug 4646.
     * @author Trudi.
     */
    private class MyAction extends CBAction {

        /**
         * Calls super constructor.
         * @param key
         */
        public MyAction(int key) {
            super(key);
        }

        /**
         * quit is called if the Esc key pressed, load is called if Enter key is pressed.
         * @param e never used.
         */
        public void actionPerformed(ActionEvent e) {
            if (getKey() == ESCAPE) quit(); else if (getKey() == ENTER) load();
        }
    }

    /**
     * This is the AbstractBinaryEditor interface method which is called when the user wants to edit the password
     */
    public void setValue(editablebinary editMe) {
        this.editMe = editMe;
        oldPwd.setText(stringEncode(editMe.getValue()));
    }

    /**
     * 
     * @param s
     */
    protected byte[] plainDecode(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.log(Level.WARNING, "Unexpected error encoding password ", e);
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
     * Calculates the pwd hash to be stored in the userPassword field.
     * @param s The password in plaintext that should be hashed.
     * @param type The encryption scheme (The {CRYPT} scheme is currently unsupported): t == 2 means MD5  (salt needs to
     * be null) t == 3 means SMD5 (needs a salt != null) t == 4 means SHA  (salt needs to be null) t == 5 means SSHA
     * (needs a salt != null)
     * @param salt The salt that is to be used together with the schemes {SMD5} and {SSHA}. Should be between 8 and 16
     * Bytes. salt should be null for any other scheme.
     * @return The base64-encoded hashed pwd with the following format: - {MD5}base64(MD5-hash) for MD5 hashes -
     *         {SHA}base64(SHA-hash) for SHA hashes - {SMD5}base64(MD5-hash+salt bytes) for SMD5 hashes -
     *         {SSHA}base64(SHA-hash+salt bytes) for SSHA hashes Or null if t is not one of 2, 3, 4, 5.
     */
    protected byte[] mdDecode(String s, int type, byte[] salt) {
        try {
            MessageDigest md;
            StringBuffer hexString = new StringBuffer();
            if ((type == 5) && (salt != null)) {
                md = MessageDigest.getInstance(SHA);
                hexString.append("{" + SSHA + "}");
            } else if (type == 4) {
                md = MessageDigest.getInstance(SHA);
                hexString.append("{" + SHA + "}");
            } else if ((type == 3) && (salt != null)) {
                md = MessageDigest.getInstance(MD5);
                hexString.append("{" + SMD5 + "}");
            } else if (type == 2) {
                md = MessageDigest.getInstance(MD5);
                hexString.append("{" + MD5 + "}");
            } else {
                return (null);
            }
            md.reset();
            md.update(s.getBytes("UTF-8"));
            if (salt != null) {
                md.update(salt);
                byte[] buff = md.digest();
                byte[] new_buf = new byte[buff.length + salt.length];
                for (int x = 0; x < buff.length; x++) new_buf[x] = buff[x];
                for (int x = buff.length; x < new_buf.length; x++) new_buf[x] = salt[x - buff.length];
                hexString.append(CBBase64.binaryToString(new_buf));
            } else {
                byte[] buff = md.digest();
                hexString.append(CBBase64.binaryToString(buff));
            }
            return hexString.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.log(Level.WARNING, "Unexpected error encoding password ", e);
            e.printStackTrace();
            return new byte[0];
        } catch (java.security.NoSuchAlgorithmException e) {
            log.log(Level.WARNING, "Unexpected error encoding password ", e);
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
     * Converts between text and a byte array
     */
    protected byte[] stringDecode(String s, byte[] salt) {
        if (s == null) {
            return (new byte[0]);
        } else {
            switch(comboType.getSelectedIndex()) {
                case 1:
                    return plainDecode(s);
                case 2:
                    return mdDecode(s, 2, null);
                case 3:
                    return mdDecode(s, 3, salt);
                case 4:
                    return mdDecode(s, 4, null);
                case 5:
                    return mdDecode(s, 5, salt);
                default:
                    return mdDecode(s, 4, null);
            }
        }
    }

    /**
     * Converts between a byte array and text
     */
    protected String stringEncode(byte[] b) {
        if (b == null || b.length == 0) {
            return new String();
        } else {
            try {
                return new String(b, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.log(Level.WARNING, "Unexpected error decoding password ", e);
                e.printStackTrace();
                return new String(b);
            }
        }
    }

    /**
     * Verifies the given pwd if "verify" is selected or sets the value of the EditableBinary object with whatever the
     * user has entered into the password text field.
     */
    protected void load() {
        if (comboType.getSelectedIndex() == 0) {
            String msg_1 = CBIntText.get("Password not verified.");
            String msg_2 = CBIntText.get("Password Verification.");
            String nPwd = oldPwd.getText();
            String oPwd = stringEncode(editMe.getValue());
            if (passwordVerify(oPwd, nPwd)) msg_1 = CBIntText.get("Password verified.");
            JOptionPane.showMessageDialog(display, msg_1, msg_2, JOptionPane.INFORMATION_MESSAGE);
        } else if (passwordConfirm()) {
            byte[] salt = null;
            if ((comboType.getSelectedIndex() == 3) || (comboType.getSelectedIndex() == 5)) {
                try {
                    SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
                    salt = new byte[8];
                    random.nextBytes(salt);
                } catch (java.security.NoSuchAlgorithmException e) {
                    log.log(Level.WARNING, "Unexpected error encoding password ", e);
                    e.printStackTrace();
                    return;
                }
            }
            editMe.setValue(stringDecode(newPwd.getText(), salt));
            quit();
        }
    }

    /**
     * Verifies a given password against the password stored in the userPassword-attribute.
     * <p/>
     * The userPassword-value should follow the following format: - {MD5}base64(MD5-hash) - {SHA}base64(SHA-hash) -
     * {SMD5}base64(MD5-hash+salt bytes) - {SSHA}base64(SHA-hash+salt bytes) - plaintext password
     * <p/>
     * If the userPassword value does not start with one of the prefixes {MD5}, {SMD5}, {SHA} or {SSHA} it will be
     * handled as a plaintext pwd.
     * <p/>
     * The Unix {CRYPT}-Scheme is currently not supported.
     * @param oPwd The original pwd stored in the userPassword value.
     * @param nPwd The password in plaintext that should be verified against the hashed pwd stored in the userPassword
     * field.
     * @return True - if the given plaintext pwd matches with the hashed pwd in the userPassword field, otherwise false.
     *         Returns also false for the {CRYPT}-scheme as it is currently unsupported.
     */
    protected boolean passwordVerify(String oPwd, String nPwd) {
        if (oPwd.startsWith("{MD5}")) {
            nPwd = new String(mdDecode(nPwd, 2, null));
        } else if (oPwd.startsWith("{SMD5}")) {
            byte[] tmp = CBBase64.stringToBinary(oPwd.substring(6));
            if (tmp != null) {
                int len = tmp.length - 16;
                if (len > 0) {
                    byte[] salt = new byte[len];
                    for (int x = 0; x < len; x++) salt[x] = tmp[x + 16];
                    nPwd = new String(mdDecode(nPwd, 3, salt));
                }
            }
        } else if (oPwd.startsWith("{SHA}")) {
            nPwd = new String(mdDecode(nPwd, 4, null));
        } else if (oPwd.startsWith("{SSHA}")) {
            byte[] tmp = CBBase64.stringToBinary(oPwd.substring(6));
            if (tmp != null) {
                int len = tmp.length - 20;
                if (len > 0) {
                    byte[] salt = new byte[len];
                    for (int x = 0; x < len; x++) salt[x] = tmp[x + 20];
                    nPwd = new String(mdDecode(nPwd, 5, salt));
                }
            }
        } else {
            nPwd = new String(plainDecode(nPwd));
        }
        if (nPwd.equals(oPwd)) return (true);
        return (false);
    }

    /**
     * Does some checks on the password.
     * @return True - if the two password fields match. False - if the new password field is empty (an error message is
     *         displayed). False - if the password fields don't match (an error message is displayed).
     */
    protected boolean passwordConfirm() {
        if (newPwd.getText().equals(oldPwd.getText())) {
            return true;
        } else if (newPwd.getText().equals("")) {
            JOptionPane.showMessageDialog(display, CBIntText.get("Empty password field, please fill in both fields"), CBIntText.get("Warning message"), JOptionPane.INFORMATION_MESSAGE);
            newPwd.setText("");
            return false;
        } else {
            JOptionPane.showMessageDialog(display, CBIntText.get("Password typed incorrectly, please try again"), CBIntText.get("Warning message"), JOptionPane.INFORMATION_MESSAGE);
            newPwd.setText("");
            return false;
        }
    }

    /**
     * Shuts down the gui.
     */
    protected void quit() {
        setVisible(false);
        dispose();
    }
}
