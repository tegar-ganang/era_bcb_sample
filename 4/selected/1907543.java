package com.notmacchallenge;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;

public class ServerSetup extends JFrame {

    public static String version = "1.1.0";

    public boolean enabled = false;

    NMCommon common_code = new NMCommon();

    private JPanel jPanel1 = new JPanel();

    private JButton enableButton = new JButton();

    private JTextField notMacIPText = new JTextField();

    private JLabel jLabel1 = new JLabel();

    private JButton disableButton = new JButton();

    private JPanel jPanel2 = new JPanel();

    private JTextField baseNotMacText = new JTextField();

    private JLabel jLabel4 = new JLabel();

    private JPanel jPanel3 = new JPanel();

    private JButton createUserButton = new JButton();

    private JButton changePasswordButton = new JButton();

    private JButton deleteUserButton = new JButton();

    private JButton userOptionsButton = new JButton();

    private JTextField notMacPortText = new JTextField();

    private JLabel jLabel2 = new JLabel();

    private JCheckBox httpTunnel_cb = new JCheckBox();

    public ServerSetup() {
        NMCommon.testAdmin();
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadPrefs();
    }

    private void jbInit() throws Exception {
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.getContentPane().setLayout(null);
        jPanel1.setBorder(BorderFactory.createEtchedBorder());
        jPanel1.setBounds(new Rectangle(9, 14, 495, 127));
        jPanel1.setLayout(null);
        enableButton.setBounds(new Rectangle(386, 17, 93, 33));
        enableButton.setText("Enable");
        enableButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                enableButton_actionPerformed(e);
            }
        });
        notMacIPText.setToolTipText("");
        notMacIPText.setText("192.168.2.10");
        notMacIPText.setBounds(new Rectangle(86, 55, 130, 22));
        jLabel1.setToolTipText("");
        jLabel1.setText("Enter IP : PORT for the NotMac server:");
        jLabel1.setBounds(new Rectangle(84, 29, 257, 16));
        this.setResizable(false);
        this.setSize(new Dimension(514, 407));
        this.setTitle("Server Setup : NotMac " + version);
        disableButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                disableButton_actionPerformed(e);
            }
        });
        disableButton.setText("Disable");
        disableButton.setBounds(new Rectangle(386, 65, 93, 33));
        jPanel2.setBorder(BorderFactory.createEtchedBorder());
        jPanel2.setBounds(new Rectangle(9, 162, 495, 46));
        jPanel2.setLayout(null);
        baseNotMacText.setToolTipText("");
        baseNotMacText.setEditable(false);
        baseNotMacText.setText("/Library/Application Support/NotMac/Storage/");
        baseNotMacText.setBounds(new Rectangle(161, 8, 328, 22));
        baseNotMacText.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyReleased(KeyEvent e) {
                baseNotMacText_keyReleased(e);
            }
        });
        jLabel4.setText("NotMac Storage Path:");
        jLabel4.setBounds(new Rectangle(11, 10, 148, 16));
        jPanel3.setBorder(BorderFactory.createEtchedBorder());
        jPanel3.setBounds(new Rectangle(9, 221, 495, 156));
        jPanel3.setLayout(null);
        createUserButton.setBounds(new Rectangle(117, 8, 262, 32));
        createUserButton.setText("Create New NotMac User");
        createUserButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                createUserButton_actionPerformed(e);
            }
        });
        changePasswordButton.setText("Change Password for NotMac User");
        changePasswordButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                changePasswordButton_actionPerformed(e);
            }
        });
        changePasswordButton.setBounds(new Rectangle(117, 77, 262, 32));
        deleteUserButton.setBounds(new Rectangle(117, 43, 262, 32));
        deleteUserButton.setText("Delete NotMac User");
        deleteUserButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                deleteUserButton_actionPerformed(e);
            }
        });
        userOptionsButton.setBounds(new Rectangle(117, 112, 262, 32));
        userOptionsButton.setToolTipText("");
        userOptionsButton.setText("Change NotMac User\'s Options");
        userOptionsButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                userOptionsButton_actionPerformed(e);
            }
        });
        notMacPortText.setText("443");
        notMacPortText.setBounds(new Rectangle(227, 54, 60, 22));
        jLabel2.setText(":");
        jLabel2.setBounds(new Rectangle(219, 56, 14, 16));
        httpTunnel_cb.setText("Advanced - Use httpTunnel");
        httpTunnel_cb.setBounds(new Rectangle(87, 84, 206, 22));
        jPanel1.add(enableButton, null);
        jPanel1.add(disableButton, null);
        jPanel1.add(notMacPortText, null);
        jPanel1.add(jLabel1, null);
        jPanel1.add(notMacIPText, null);
        jPanel1.add(jLabel2, null);
        jPanel1.add(httpTunnel_cb, null);
        this.getContentPane().add(jPanel2, null);
        jPanel2.add(baseNotMacText, null);
        this.getContentPane().add(jPanel1, null);
        jPanel2.add(jLabel4, null);
        this.getContentPane().add(jPanel3, null);
        jPanel3.add(createUserButton, null);
        jPanel3.add(deleteUserButton, null);
        jPanel3.add(changePasswordButton, null);
        jPanel3.add(userOptionsButton, null);
    }

    void enableButton_actionPerformed(ActionEvent e) {
        try {
            try {
                NMCommon.shutdownCrush();
            } catch (Exception ee) {
            }
            enabled = true;
            new File("/Library/Application Support/NotMac/backup/").mkdirs();
            Vector v = new Vector();
            if (httpTunnel_cb.isSelected()) {
                NMCommon.writeHosts(false, "127.0.0.1", "127.0.0.1", "127.0.0.1");
                NMCommon.installHttpTunnel(notMacIPText.getText(), "clientServer", Integer.parseInt(notMacPortText.getText()));
            } else {
                NMCommon.writeHosts(false, notMacIPText.getText(), notMacIPText.getText(), notMacIPText.getText());
            }
            v.addElement(new String[] { "chmod", "+x", "/Library/Application Support/NotMac/import_certs.command" });
            v.addElement(new String[] { "/Library/Application Support/NotMac/import_certs.command" });
            NMCommon.installHttpTunnel(notMacIPText.getText(), "bothServer", Integer.parseInt(notMacPortText.getText()));
            v.addElement(new String[] { "lookupd", "-flushcache" });
            NMCommon.exec(v);
            NMCommon.install_osx_service("/Library/Application Support/NotMac/CrushFTP/", true);
            NMCommon.startupCrush("/Library/Application Support/NotMac/CrushFTP/");
            NMCommon.enablePrefPane();
            Thread.sleep(1000);
        } catch (Exception ee) {
            JOptionPane.showMessageDialog(this, "Error:" + ee.getMessage());
            ee.printStackTrace();
        }
        savePrefs();
    }

    public void disableButton_actionPerformed(ActionEvent e) {
        try {
            NMCommon.disableNotMac();
            enabled = false;
        } catch (Exception ee) {
            JOptionPane.showMessageDialog(this, "Error:" + ee.getMessage());
            ee.printStackTrace();
        }
        savePrefs();
    }

    void createUserButton_actionPerformed(ActionEvent e) {
        String permissions = "(read)(write)(view)(delete)(resume)(rename)(makedir)(deletedir)(real_quota)";
        try {
            String username = writeUser(permissions);
            if (username != null) {
                NMCommon.recurseCopy("/Library/Application Support/NotMac/templateUserHome/", baseNotMacText.getText() + username + "/");
                JOptionPane.showMessageDialog(this, "User created.");
            }
        } catch (Exception ee) {
            ee.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error:" + ee.toString());
        }
    }

    public String writeUser(String privs) throws Exception {
        System.getProperties().put("crushftp.users", "/Library/Application Support/NotMac/CrushFTP/users/");
        String username = (String) JOptionPane.showInputDialog(null, "Enter a username:", "Username", 0, null, null, "");
        if (username != null) {
            if (username.length() < 4) {
                throw new Exception("Username too short.  Must be 4 characters at least.");
            } else if (username.indexOf("_") >= 0 || username.indexOf(" ") >= 0) {
                throw new Exception("Invalid characters in username.");
            }
            String password = (String) JOptionPane.showInputDialog(null, "Enter a password:", "Password", 0, null, null, "");
            if (password != null) {
                if (password.length() < 6) {
                    throw new Exception("Password too short.  (6 character minimum)");
                }
                new File(baseNotMacText.getText() + username + "/").mkdirs();
                new File("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/" + username + "/").mkdirs();
                common_code.writeNewUser(username, password, baseNotMacText.getText() + username + "/", privs, "", "", "127.0.0.1_53818");
                Properties user = (Properties) NMCommon.readXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/" + username + "/user.XML");
                user.put("defaut_owner_command", NMCommon.last(System.getProperty("user.home", "")));
                user.put("defaut_group_command", NMCommon.last(System.getProperty("user.home", "")));
                user.put("defaut_priv_command", "770");
                common_code.writeXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/" + username + "/user.XML", user, "user");
                new File("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/anonymous/VFS/" + username + "/").mkdirs();
                new File("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/public/VFS/" + username + "/").mkdirs();
                Properties pictures = new Properties();
                pictures.put("url", "FILE:/" + new File(baseNotMacText.getText() + username + "/Pictures/").getCanonicalPath());
                pictures.put("type", "dir");
                Vector v = new Vector();
                v.addElement(pictures);
                common_code.writeXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/anonymous/VFS/" + username + "/.Pictures", v, "VFS");
                Properties calendars = new Properties();
                calendars.put("url", "FILE:/" + new File(baseNotMacText.getText() + username + "/Sites/.calendars/").getCanonicalPath());
                calendars.put("type", "dir");
                v = new Vector();
                v.addElement(calendars);
                common_code.writeXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/anonymous/VFS/" + username + "/.calendars", v, "VFS");
                Properties iphoto = new Properties();
                iphoto.put("url", "FILE:" + new File(baseNotMacText.getText() + username + "/Web/Sites/iPhoto/").getCanonicalPath() + "/");
                iphoto.put("type", "dir");
                v = new Vector();
                v.addElement(iphoto);
                common_code.writeXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/anonymous/VFS/" + username + "/iPhoto", v, "VFS");
                Properties iweb = new Properties();
                iweb.put("url", "FILE:" + new File(baseNotMacText.getText() + username + "/Web/Sites/iWeb/").getCanonicalPath() + "/");
                iweb.put("type", "dir");
                v = new Vector();
                v.addElement(iweb);
                common_code.writeXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/anonymous/VFS/" + username + "/iWeb", v, "VFS");
                Properties web = new Properties();
                web.put("url", "FILE:" + new File(baseNotMacText.getText() + username + "/Web/Sites/").getCanonicalPath() + "/");
                web.put("type", "dir");
                v = new Vector();
                v.addElement(web);
                common_code.writeXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/anonymous/VFS/" + username + "/Web", v, "VFS");
                Properties public_folder = new Properties();
                public_folder.put("url", "FILE:" + new File(baseNotMacText.getText() + username + "/Public/").getCanonicalPath() + "/");
                public_folder.put("type", "dir");
                v = new Vector();
                v.addElement(public_folder);
                common_code.writeXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/public/VFS/" + username + "/Public", v, "VFS");
                Properties permissions = (Properties) NMCommon.readXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/anonymous/VFS.XML");
                permissions.put(("/anonymous/" + username + "/").toUpperCase(), "(read)(view)(resume)");
                try {
                    common_code.writeXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/anonymous/VFS.XML", permissions, "VFS");
                } catch (Exception ee) {
                }
                permissions = (Properties) NMCommon.readXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/public/VFS.XML");
                permissions.put(("/" + username + "/Public/").toUpperCase(), "(read)(view)(resume)");
                try {
                    common_code.writeXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/public/VFS.XML", permissions, "VFS");
                } catch (Exception ee) {
                }
                new File("/Library/Application Support/NotMac/syncxml/users/" + username + "/").mkdirs();
                NMCommon.recurseCopy("/Library/Application Support/NotMac/syncxmlTemplate/", "/Library/Application Support/NotMac/syncxml/users/" + username + "/");
                Properties info = (Properties) NMCommon.readXMLObject("/Library/Application Support/NotMac/syncxml/users/" + username + "/info.XML");
                info.put("resourceguid", NMCommon.makeGuid(""));
                info.put("resourceid", new Date().getTime() + "");
                try {
                    common_code.writeXMLObject("/Library/Application Support/NotMac/syncxml/users/" + username + "/info.XML", info, "props");
                } catch (Exception ee) {
                }
                new File("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/" + username + "/VFS/.Mac/").mkdirs();
                setQuota(username, "1000");
            }
        }
        return username;
    }

    void deleteUserButton_actionPerformed(ActionEvent e) {
        String username = (String) JOptionPane.showInputDialog(null, "Enter a username:", "Username", 0, null, null, "");
        int input_value = JOptionPane.showConfirmDialog(null, "This will permanently delete this user:" + username + "\r\n\r\nTheir account and all data files associated with them will be removed.", "Delete user?", 0);
        if (input_value == 0) {
            if (username != null) {
                if (username.equalsIgnoreCase("anonymous") || username.equalsIgnoreCase("public") || username.equals("NotMacAdmin") || username.indexOf(".") >= 0) {
                    JOptionPane.showMessageDialog(this, "Sorry, that user is a system account for NotMac.");
                    return;
                }
                NMCommon.recurseDelete("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/" + username + "/", false);
                NMCommon.recurseDelete("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/anonymous/VFS/anonymous/" + username + "/", false);
                NMCommon.recurseDelete("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/public/VFS/" + username + "/", false);
                NMCommon.recurseDelete("/Library/Application Support/NotMac/syncxml/users/" + username + "/", false);
                Properties permissions = (Properties) NMCommon.readXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/anonymous/VFS.XML");
                permissions.remove(("/anonymous/" + username + "/").toUpperCase());
                try {
                    common_code.writeXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/anonymous/VFS.XML", permissions, "VFS");
                } catch (Exception ee) {
                }
                permissions = (Properties) NMCommon.readXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/public/VFS.XML");
                permissions.remove(("/" + username + "/Public/").toUpperCase());
                try {
                    common_code.writeXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/public/VFS.XML", permissions, "VFS");
                } catch (Exception ee) {
                }
                NMCommon.recurseDelete(baseNotMacText.getText() + username + "/", false);
                JOptionPane.showMessageDialog(this, "User deleted.");
            }
        }
    }

    void changePasswordButton_actionPerformed(ActionEvent e) {
        String username = (String) JOptionPane.showInputDialog(null, "Enter a username:", "Username", 0, null, null, "");
        if (username != null) {
            String password = (String) JOptionPane.showInputDialog(null, "Enter a password:", "Password", 0, null, null, "");
            if (password != null) {
                try {
                    if (password.length() < 6) {
                        throw new Exception("Password too short.  (6 character minimum)");
                    }
                    Properties user = (Properties) NMCommon.readXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/" + username + "/user.XML");
                    user.put("password", common_code.encode_pass(password, "DES"));
                    common_code.writeXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/" + username + "/user.XML", user, "user");
                    JOptionPane.showMessageDialog(this, "Password Updated.");
                } catch (Exception ee) {
                    ee.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error:" + ee.toString());
                }
            }
        }
    }

    void userOptionsButton_actionPerformed(ActionEvent e) {
        String username = (String) JOptionPane.showInputDialog(null, "Enter a username:", "Username", 0, null, null, "");
        if (username != null) {
            String quota = (String) JOptionPane.showInputDialog(null, "Enter quota in Mega Bytes:", "Quota", 0, null, null, "100");
            if (quota != null) {
                try {
                    setQuota(username, quota);
                    JOptionPane.showMessageDialog(this, "Quota Set.");
                } catch (Exception ee) {
                    ee.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error:" + ee.toString());
                }
            }
        }
    }

    public void setQuota(String username, String quota) throws Exception {
        if (!quota.equals("")) quota = (Long.parseLong(quota) * 1024 * 1024) + "";
        Properties permissions = (Properties) NMCommon.readXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/" + username + "/VFS.XML");
        String data = permissions.getProperty(("/" + username + "/").toUpperCase(), "(read)(write)(resume)");
        if (data.indexOf("(quota") < 0) data += "(quota104857600)";
        String original_data = data;
        data = data.substring(data.indexOf("(quota") + 6, data.indexOf(")", data.indexOf("(quota")));
        data = NMCommon.replace_str(original_data, data, quota + "");
        permissions.put(("/" + username + "/").toUpperCase(), data);
        common_code.writeXMLObject("/Library/Application Support/NotMac/CrushFTP/users/127.0.0.1_53818/" + username + "/VFS.XML", permissions, "VFS");
    }

    public void loadPrefs() {
        if (new File("/Library/Application Support/NotMac/prefs/ServerSetup.XML").exists()) {
            Properties serverSetup = (Properties) NMCommon.readXMLObject("/Library/Application Support/NotMac/prefs/ServerSetup.XML");
            if (serverSetup != null) {
                enabled = serverSetup.getProperty("enabled", "").equals("true");
                notMacPortText.setText(serverSetup.getProperty("notMacPort", "443"));
                notMacIPText.setText(serverSetup.getProperty("notMacIP", ""));
                baseNotMacText.setText(serverSetup.getProperty("baseNotMacText", "/Library/Application Support/NotMac/Storage/"));
                httpTunnel_cb.setSelected(serverSetup.getProperty("httpTunnel", "false").equals("true"));
            }
        }
        baseNotMacText.setEnabled(!enabled);
        httpTunnel_cb.setEnabled(!enabled);
        enableButton.setEnabled(!enabled);
        disableButton.setEnabled(enabled);
        notMacIPText.setEnabled(!enabled);
        notMacPortText.setEnabled(!enabled);
    }

    public void savePrefs() {
        Properties serverSetup = new Properties();
        serverSetup.put("enabled", enabled + "");
        serverSetup.put("notMacPort", notMacPortText.getText());
        serverSetup.put("notMacIP", notMacIPText.getText());
        serverSetup.put("baseNotMacText", baseNotMacText.getText());
        serverSetup.put("httpTunnel", httpTunnel_cb.isSelected() + "");
        try {
            new File("/Library/Application Support/NotMac/prefs/").mkdirs();
            common_code.writeXMLObject("/Library/Application Support/NotMac/prefs/ServerSetup.XML", serverSetup, "prefs");
            RandomAccessFile out = new RandomAccessFile("/Library/Application Support/NotMac/url.txt", "rw");
            out.setLength(0);
            out.write(("http://" + notMacIPText.getText() + "/").getBytes());
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        baseNotMacText.setEnabled(!enabled);
        httpTunnel_cb.setEnabled(!enabled);
        enableButton.setEnabled(!enabled);
        disableButton.setEnabled(enabled);
        notMacIPText.setEnabled(!enabled);
        notMacPortText.setEnabled(!enabled);
    }

    void baseNotMacText_keyReleased(KeyEvent e) {
        savePrefs();
    }
}
