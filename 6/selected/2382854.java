package net.guoquan.network.chat.chatRoom.client.UI;

import java.io.IOException;
import javax.security.auth.login.LoginException;
import net.guoquan.network.chat.chatRoom.client.context.ClientSessionHandler;

/**
 * 
 * @author __USER__
 */
public class LoginDialog extends javax.swing.JDialog {

    /**
	 * 
	 */
    private static final long serialVersionUID = -3040554067533465730L;

    private ClientSessionHandler handler;

    /** Creates new form LoginDialog */
    public LoginDialog(java.awt.Frame parent, boolean modal, ClientSessionHandler handler) {
        super(parent, modal);
        this.handler = handler;
        initComponents();
    }

    public String getUsername() {
        return jTextField1.getText();
    }

    public String getPassword() {
        return new String(jPasswordField1.getPassword());
    }

    private void initComponents() {
        jTextField1 = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jPasswordField1 = new javax.swing.JPasswordField();
        jButton1 = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jProgressBar1 = new javax.swing.JProgressBar();
        jComboBox1 = new javax.swing.JComboBox();
        jComboBox2 = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        setTitle("LobbyTalk Internet Chatting room");
        setAlwaysOnTop(true);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });
        jLabel1.setText("Username: ");
        jLabel2.setText("Password: ");
        jButton1.setText("Login");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/javax/swing/plaf/metal/icons/ocean/question.png")));
        jProgressBar1.setString("");
        jProgressBar1.setStringPainted(true);
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "127.0.0.1" }));
        jComboBox1.setEditable(true);
        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "12345" }));
        jComboBox2.setEditable(true);
        jLabel4.setText(":");
        jLabel5.setText("Server:");
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup().addContainerGap().add(jProgressBar1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 246, Short.MAX_VALUE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jButton1).addContainerGap()).add(jLabel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 331, Short.MAX_VALUE).add(layout.createSequentialGroup().addContainerGap().add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jLabel1).add(jLabel2).add(jLabel5)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().add(jComboBox1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 168, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jLabel4).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jComboBox2, 0, 72, Short.MAX_VALUE)).add(org.jdesktop.layout.GroupLayout.TRAILING, jTextField1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 252, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.TRAILING, jPasswordField1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 252, Short.MAX_VALUE)).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup().add(jLabel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 89, Short.MAX_VALUE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel5).add(jComboBox1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(jLabel4).add(jComboBox2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel1).add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel2).add(jPasswordField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false).add(jProgressBar1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(jButton1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addContainerGap()));
        pack();
    }

    private void formWindowClosed(java.awt.event.WindowEvent evt) {
        System.exit(0);
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        new Thread() {

            public void run() {
                jProgressBar1.setIndeterminate(true);
                jTextField1.setEnabled(false);
                jPasswordField1.setEnabled(false);
                jButton1.setEnabled(false);
                jComboBox1.setEnabled(false);
                jComboBox2.setEnabled(false);
                try {
                    boolean con = handler.connect(jComboBox1.getSelectedItem().toString(), Integer.parseInt(jComboBox2.getSelectedItem().toString()));
                    if (con && 0 != handler.login(jTextField1.getText(), new String(jPasswordField1.getPassword()))) {
                        try {
                            handler.users();
                        } catch (LoginException e) {
                            try {
                                handler.bye();
                            } catch (IOException e1) {
                            } finally {
                                handler.close();
                            }
                        }
                        getParent().setVisible(true);
                        setVisible(false);
                    } else {
                        jProgressBar1.setString("Wrong name or bad password.");
                    }
                } catch (IOException e) {
                    jProgressBar1.setString("Network Error.");
                    e.printStackTrace();
                } finally {
                    jProgressBar1.setIndeterminate(false);
                    jTextField1.setEnabled(true);
                    jPasswordField1.setEnabled(true);
                    jButton1.setEnabled(true);
                    jComboBox1.setEnabled(true);
                    jComboBox2.setEnabled(true);
                }
            }
        }.start();
    }

    /**
	 * @param args
	 *            the command line arguments
	 */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                LoginDialog dialog = new LoginDialog(new javax.swing.JFrame(), true, null);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {

                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    private javax.swing.JButton jButton1;

    private javax.swing.JComboBox jComboBox1;

    private javax.swing.JComboBox jComboBox2;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JLabel jLabel5;

    private javax.swing.JPasswordField jPasswordField1;

    private javax.swing.JProgressBar jProgressBar1;

    private javax.swing.JTextField jTextField1;
}
