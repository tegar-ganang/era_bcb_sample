package org.magnesia.client.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import org.magnesia.client.gui.components.NarrowProgress;
import org.magnesia.client.gui.data.Properties;
import static org.magnesia.client.misc.Utils.i18n;

class Login extends JDialog implements ActionListener {

    private static final long serialVersionUID = -4522455900173051826L;

    private JPasswordField jpf;

    private JTextField host, username, port;

    private JCheckBox keepPassword, autoLogin;

    private Preferences p;

    private JButton ok, cancel;

    private JLabel status;

    private boolean done = false;

    private JProgressBar progress;

    public Login(Magnesia m) {
        super(m.getFrame());
        setModal(true);
        setTitle(i18n("login_caption"));
        GridBagLayout gbl = new GridBagLayout();
        setLayout(gbl);
        p = Preferences.userNodeForPackage(getClass());
        jpf = new JPasswordField();
        host = new JTextField(p.get("HOST", "localhost"));
        username = new JTextField(p.get("USERNAME", "jimbo"));
        port = new JTextField("" + p.getInt("PORT", 12345));
        keepPassword = new JCheckBox(i18n("login_keep"));
        keepPassword.setSelected(Properties.keepPassword());
        keepPassword.addActionListener(this);
        if (keepPassword.isSelected()) {
            jpf.setText(p.get("PASSWORD", ""));
        }
        autoLogin = new JCheckBox(i18n("login_auto"));
        autoLogin.setSelected(Properties.isAutoLogin());
        autoLogin.addActionListener(this);
        status = new JLabel();
        status.setForeground(Color.RED);
        status.setVisible(false);
        ok = new JButton(i18n("button_ok"));
        cancel = new JButton(i18n("button_cancel"));
        ok.addActionListener(this);
        cancel.addActionListener(this);
        progress = new NarrowProgress(Color.GREEN);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipadx = 15;
        gbc.ipady = 5;
        gbc.weightx = 0.0;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        add(new JLabel(i18n("login_host")), gbc);
        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(host, gbc);
        gbc.weightx = 0.0;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        add(new JLabel(i18n("login_port")), gbc);
        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(port, gbc);
        gbc.weightx = 0.0;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        add(new JLabel(i18n("login_username")), gbc);
        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(username, gbc);
        gbc.weightx = 0.0;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        add(new JLabel(i18n("login_password")), gbc);
        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(jpf, gbc);
        gbc.weightx = 0.0;
        add(keepPassword, gbc);
        add(autoLogin, gbc);
        gbc.weightx = 1.0;
        add(status, gbc);
        gbc.weighty = 0.0;
        add(progress, gbc);
        add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        JPanel buttons = new JPanel(new GridLayout(1, 2));
        buttons.add(ok);
        buttons.add(cancel);
        add(buttons, gbc);
        pack();
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(ok);
    }

    @Override
    public void setVisible(boolean b) {
        if (b && autoLogin.isSelected()) {
            progress.setIndeterminate(true);
            Authenticator a = new Authenticator();
            new Thread(a).start();
            super.setVisible(true);
        } else {
            super.setVisible(b);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == ok) {
            progress.setIndeterminate(true);
            Authenticator a = new Authenticator();
            new Thread(a).start();
        } else if ((e.getSource() == cancel)) {
            setVisible(false);
        } else if (e.getSource() == keepPassword) {
            if (!keepPassword.isSelected()) autoLogin.setSelected(false);
        } else if (e.getSource() == autoLogin) {
            if (autoLogin.isSelected()) keepPassword.setSelected(true);
        }
    }

    private void setStatus(String lang) {
        status.setText(i18n(lang));
        status.setVisible(true);
        pack();
    }

    private class Authenticator implements Runnable {

        public void run() {
            try {
                int portnum = 0;
                try {
                    portnum = Integer.parseInt(port.getText());
                } catch (NumberFormatException nfe) {
                }
                p.put("HOST", host.getText());
                p.putInt("PORT", portnum);
                p.put("USERNAME", username.getText());
                ClientConnection.getConnection().connect();
                done = ClientConnection.getConnection().login(username.getText(), new String(jpf.getPassword()));
                p.put("PASSWORD", keepPassword.isSelected() ? new String(jpf.getPassword()) : "");
                Properties.setKeepPassword(keepPassword.isSelected());
                Properties.setAutoLogin(autoLogin.isSelected());
                if (!done) {
                    p.flush();
                    p.sync();
                    setStatus("login_wrong");
                    autoLogin.setSelected(false);
                    setVisible(true);
                } else {
                    setVisible(false);
                }
            } catch (SocketException e1) {
                setStatus("login_reset");
            } catch (UnknownHostException e1) {
                setStatus("login_host_unknown");
            } catch (IOException e1) {
                setStatus("login_unable_to_connect");
            } catch (BackingStoreException e1) {
                e1.printStackTrace();
            }
            progress.setIndeterminate(false);
        }
    }
}
