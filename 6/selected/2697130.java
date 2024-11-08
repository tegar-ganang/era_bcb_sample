package games.midhedava.client.gui;

import games.midhedava.client.midhedava;
import games.midhedava.client.MidhedavaClient;
import games.midhedava.client.gui.login.Profile;
import games.midhedava.client.gui.login.ProfileList;
import games.midhedava.client.update.ClientGameConfiguration;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import marauroa.client.ariannexpTimeoutException;
import marauroa.common.Log4J;
import marauroa.common.io.Persistence;

/**
 * Server login dialog.
 * 
 */
public class LoginDialog extends JDialog {

    private static final long serialVersionUID = -1182930046629241075L;

    private JComboBox profilesComboBox;

    private JCheckBox saveLoginBox;

    private JCheckBox savePasswordBox;

    private JTextField usernameField;

    private JPasswordField passwordField;

    private JTextField serverField;

    private JTextField serverPortField;

    private JButton loginButton;

    private JPanel contentPane;

    private MidhedavaClient client;

    private Frame owner;

    protected ProfileList profiles;

    public LoginDialog(Frame owner, MidhedavaClient client) {
        super(owner, true);
        this.client = client;
        this.owner = owner;
        initializeComponent();
        this.setVisible(true);
    }

    private void initializeComponent() {
        JLabel l;
        System.out.println("it got here");
        this.setTitle("Login to Server");
        this.setResizable(false);
        contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout(new GridBagLayout());
        contentPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        l = new JLabel("Account profiles");
        c.insets = new Insets(4, 4, 15, 4);
        c.gridx = 0;
        c.gridy = 0;
        contentPane.add(l, c);
        profilesComboBox = new JComboBox();
        profilesComboBox.addActionListener(new ProfilesCB());
        c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        contentPane.add(profilesComboBox, c);
        l = new JLabel("Server name");
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0;
        c.gridy = 1;
        contentPane.add(l, c);
        serverField = new JTextField(ClientGameConfiguration.get("DEFAULT_SERVER"));
        c.gridx = 1;
        c.gridy = 1;
        c.fill = GridBagConstraints.BOTH;
        contentPane.add(serverField, c);
        l = new JLabel("Server port");
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0;
        c.gridy = 2;
        contentPane.add(l, c);
        serverPortField = new JTextField(ClientGameConfiguration.get("DEFAULT_PORT"));
        c.gridx = 1;
        c.gridy = 2;
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.BOTH;
        contentPane.add(serverPortField, c);
        l = new JLabel("Type your username");
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0;
        c.gridy = 3;
        contentPane.add(l, c);
        usernameField = new JTextField();
        usernameField.requestFocusInWindow();
        c.gridx = 1;
        c.gridy = 3;
        c.fill = GridBagConstraints.BOTH;
        contentPane.add(usernameField, c);
        l = new JLabel("Type your password");
        c.gridx = 0;
        c.gridy = 4;
        c.fill = GridBagConstraints.NONE;
        contentPane.add(l, c);
        passwordField = new JPasswordField();
        c.gridx = 1;
        c.gridy = 4;
        c.fill = GridBagConstraints.BOTH;
        contentPane.add(passwordField, c);
        saveLoginBox = new JCheckBox("Save login profile locally");
        saveLoginBox.setSelected(false);
        c.gridx = 0;
        c.gridy = 5;
        c.fill = GridBagConstraints.NONE;
        contentPane.add(saveLoginBox, c);
        savePasswordBox = new JCheckBox("Save password");
        savePasswordBox.setSelected(true);
        savePasswordBox.setEnabled(false);
        c.gridx = 0;
        c.gridy = 6;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 20, 0, 0);
        contentPane.add(savePasswordBox, c);
        loginButton = new JButton();
        loginButton.setText("Login to Server");
        loginButton.setFocusable(true);
        InputMap map = loginButton.getInputMap();
        map.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "pressed");
        map.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "released");
        getRootPane().setDefaultButton(loginButton);
        loginButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                loginButton_actionPerformed();
            }
        });
        c.gridx = 1;
        c.gridy = 5;
        c.gridheight = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(15, 4, 4, 4);
        contentPane.add(loginButton, c);
        profiles = loadProfiles();
        populateProfiles(profiles);
        saveLoginBox.addChangeListener(new SaveProfileStateCB());
        this.pack();
        this.setLocationRelativeTo(owner);
    }

    private void loginButton_actionPerformed() {
        Profile profile;
        profile = new Profile();
        profile.setHost((serverField.getText()).trim());
        try {
            profile.setPort(Integer.parseInt(serverPortField.getText().trim()));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "That is not a valid port number. Please try again.", "Invalid port", JOptionPane.WARNING_MESSAGE);
            return;
        }
        profile.setUser(usernameField.getText().trim());
        profile.setPassword(new String(passwordField.getPassword()));
        if (saveLoginBox.isSelected()) {
            profiles.add(profile);
            populateProfiles(profiles);
            if (savePasswordBox.isSelected()) {
                saveProfiles(profiles);
            } else {
                String pw = profile.getPassword();
                profile.setPassword("");
                saveProfiles(profiles);
                profile.setPassword(pw);
            }
        }
        Thread t = new Thread(new ConnectRunnable(profile));
        t.start();
    }

    /**
	 * Connect to a server using a given profile.
	 */
    protected void connect(Profile profile) {
        final ProgressBar progressBar = new ProgressBar(this);
        progressBar.start();
        setEnabled(false);
        try {
            client.connect(profile.getHost(), profile.getPort(), true);
            progressBar.step();
        } catch (Exception ex) {
            progressBar.cancel();
            setEnabled(true);
            Log4J.getLogger(LoginDialog.class).error("unable to connect to server", ex);
            JOptionPane.showMessageDialog(this, "Unable to connect to server. Did you misspell the server name?");
            return;
        }
        try {
            if (client.login(profile.getUser(), profile.getPassword()) == false) {
                String result = client.getEvent();
                if (result == null) {
                    result = "Server is not available right now. The server " + "may be down or, if you are using a custom server, " + "you may have entered its name and port number incorrectly.";
                }
                progressBar.cancel();
                setEnabled(true);
                JOptionPane.showMessageDialog(this, result, "Error Logging In", JOptionPane.ERROR_MESSAGE);
            } else {
                progressBar.step();
                progressBar.finish();
                setVisible(false);
                owner.setVisible(false);
                midhedava.doLogin = true;
                client.setUserName(profile.getUser());
            }
        } catch (ariannexpTimeoutException ex) {
            progressBar.cancel();
            setEnabled(true);
            JOptionPane.showMessageDialog(this, "Server does not respond. The server may be down or, " + "if you are using a custom server, you may have entered " + "its name and port number incorrectly.", "Error Logging In", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            progressBar.cancel();
            setEnabled(true);
            JOptionPane.showMessageDialog(this, "Midhedava cannot connect. Please check that your connection " + "is set up and active, then try again.", "Error Logging In", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
	 * Load saves profiles.
	 */
    private ProfileList loadProfiles() {
        ProfileList profiles;
        profiles = new ProfileList();
        try {
            InputStream is = Persistence.get().getInputStream(true, "midhedava", "user.dat");
            try {
                profiles.load(is);
            } finally {
                is.close();
            }
        } catch (FileNotFoundException fnfe) {
        } catch (IOException ioex) {
            JOptionPane.showMessageDialog(this, "An error occurred while loading your login information", "Error Loading Login Information", JOptionPane.WARNING_MESSAGE);
        }
        return profiles;
    }

    /**
	 * Populate the profiles combobox and select the default.
	 */
    protected void populateProfiles(ProfileList profiles) {
        Iterator iter;
        int count;
        profilesComboBox.removeAllItems();
        iter = profiles.iterator();
        while (iter.hasNext()) {
            profilesComboBox.addItem(iter.next());
        }
        if ((count = profilesComboBox.getItemCount()) != 0) {
            profilesComboBox.setSelectedIndex(count - 1);
        }
    }

    /**
	 * Called when a profile selection is changed.
	 */
    protected void profilesCB() {
        Profile profile;
        String host;
        profile = (Profile) profilesComboBox.getSelectedItem();
        if (profile != null) {
            host = profile.getHost();
            serverField.setText(host);
            serverPortField.setText(String.valueOf(profile.getPort()));
            usernameField.setText(profile.getUser());
            passwordField.setText(profile.getPassword());
        } else {
            serverPortField.setText(String.valueOf(Profile.DEFAULT_SERVER_PORT));
            usernameField.setText("");
            passwordField.setText("");
        }
    }

    private void saveProfiles(ProfileList profiles) {
        try {
            OutputStream os = Persistence.get().getOutputStream(true, "midhedava", "user.dat");
            try {
                profiles.save(os);
            } catch (IOException e) {
            } finally {
                os.close();
            }
        } catch (IOException ioex) {
            ioex.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occurred while saving your login information", "Error Saving Login Information", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
	 * Called when save profile selection change.
	 */
    protected void saveProfileStateCB() {
        savePasswordBox.setEnabled(saveLoginBox.isSelected());
    }

    /**
	 * Server connect thread runnable.
	 */
    protected class ConnectRunnable implements Runnable {

        protected Profile profile;

        public ConnectRunnable(Profile profile) {
            this.profile = profile;
        }

        public void run() {
            connect(profile);
        }
    }

    /**
	 * Profiles combobox selection change listener.
	 */
    protected class ProfilesCB implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            profilesCB();
        }
    }

    /**
	 * Save profile selection change.
	 */
    protected class SaveProfileStateCB implements ChangeListener {

        public void stateChanged(ChangeEvent ev) {
            saveProfileStateCB();
        }
    }
}
