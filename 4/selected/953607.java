package com.visitrend.ndvis.sql.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.visitrend.ndvis.app.NDVis;
import com.visitrend.ndvis.io.XMLDocReaderWriter;
import com.visitrend.ndvis.io.XMLTags;
import com.visitrend.ndvis.sql.DBSettings;
import com.visitrend.ndvis.sql.model.DBDriverProfile;
import com.visitrend.ndvis.sql.model.DBProfile;

/**
 * This is a JDialog for entering database connection values (e.g. username,
 * password). It follows the basic JDialog implementation and is modal (meaning
 * the user must make this window go away before they can act with the main
 * interface again - see Sun documentation for more technical description).
 * 
 * @author Diane Kramer - dkramer at visitrend dot com
 * 
 *         TODO: write some JUnit tests for this class.
 */
public class DBLogin2 extends JDialog implements ActionListener {

    public static final int CANCEL = 0;

    public static final int CONNECT = 1;

    private static final int LOADED = 0;

    private static final int EDITED = 1;

    private static final int SAVED = 2;

    private static final int DELETED = 3;

    private static DBLogin2 instance;

    private int dialogState = LOADED;

    private int exitValue = CANCEL;

    private List<DBProfile> profileList;

    private static final String PROFILES_FILE_NAME = "data" + File.separator + "DBProfiles.xml";

    private DBSettings dbs;

    private DBDriverDialog driverDialog;

    private NDVis view;

    private static final String CONN_NAME_ERROR = "You must supply a Connection name.";

    private static final String DB_TYPE_ERROR = "You must supply a Database Type.";

    private static final String CONN_METHOD_ERROR = "You must supply a Connection Method.";

    private static final String PORT_ERROR = "You must supply a Port value.";

    private static final String HOSTNAME_ERROR = "You must supply a Host name.";

    private static final String SCHEMA_ERROR = "You must supply a Schema.";

    private static final String USER_ERROR = "You must supply a User name.";

    public static final String DB_CONN = "Connected to Database";

    public static final String DB_DIS = "Not Connected to Database";

    private static final String CONN_NAME_TEXT = "Connection Name:";

    private static final String DB_TYPE_TEXT = "Database Type:";

    private static final String CONN_METHOD_TEXT = "Connection Method:";

    private static final String PORT_TEXT = "Port:";

    private static final String HOSTNAME_TEXT = "Hostname:";

    private static final String SCEHMA_TEXT = "Schema:";

    private static final String USERNAME_TEXT = "User Name:";

    private static final String PASSWORD_TEXT = "Password:";

    private static final String NEW_CONN_BUTTON = "New";

    private static final String SAVE_CONN_BUTTON = "Save";

    private static final String DEL_CONN_BUTTON = "Delete";

    private static final String NEW_DBTYPE_BUTTON = "New...";

    private static final String EDIT_DBTYPE_BUTTON = "Edit...";

    private static final String DEL_DBTYPE_BUTTON = "Delete";

    private static final String CONNECT_BUTTON = "Connect";

    private static final String CANCEL_BUTTON = "Cancel";

    private static final String DEFAULT_CONN_METHOD = "Standard (TCP/IP)";

    private static final String DEFAULT_PORT = "3306";

    private static final String DEFAULT_HOSTNAME = "127.0.0.1";

    JPopupMenu popupMenu;

    private JLabel statusLabel, connNameLabel, dbTypeLabel, connMethodLabel, portLabel, hostnameLabel, schemaLabel, usernameLabel, passwordLabel;

    private JComboBox connNameCombo, dbTypeCombo, connMethodCombo;

    private JTextField portField, hostnameField, schemaField, usernameField;

    private JPasswordField passwordField;

    private MyComboBoxModel model;

    private JButton newConnButton, saveConnButton, delConnButton, newDBTypeButton, editDBTypeButton, delDBTypeButton, connectButton, cancelButton;

    /**
	 * Default constructor - dialog will have no parent
	 */
    private DBLogin2() {
        this(null);
    }

    /**
	 * This is the constructor that should be used
	 * 
	 * @param parent
	 *            Specifies parent window
	 */
    private DBLogin2(Component parent) {
        super(JOptionPane.getFrameForComponent(parent), "Database Login", true);
        if (parent instanceof NDVis) {
            view = (NDVis) parent;
        }
        profileList = new ArrayList<DBProfile>();
        createUIElements();
        loadData();
        layoutUIElements();
    }

    /**
	 * Construct & initialize UI Elements
	 */
    private void createUIElements() {
        MyFocusListener fl = new MyFocusListener();
        MyKeyListener kl = new MyKeyListener();
        MyItemListener il = new MyItemListener();
        statusLabel = new JLabel(DB_DIS);
        connNameLabel = new JLabel(CONN_NAME_TEXT);
        dbTypeLabel = new JLabel(DB_TYPE_TEXT);
        connMethodLabel = new JLabel(CONN_METHOD_TEXT);
        portLabel = new JLabel(PORT_TEXT);
        hostnameLabel = new JLabel(HOSTNAME_TEXT);
        schemaLabel = new JLabel(SCEHMA_TEXT);
        usernameLabel = new JLabel(USERNAME_TEXT);
        passwordLabel = new JLabel(PASSWORD_TEXT);
        connNameCombo = new JComboBox();
        connNameCombo.setEditable(true);
        connNameCombo.addFocusListener(fl);
        connNameCombo.addItemListener(il);
        connNameCombo.setPrototypeDisplayValue("Sample Connection String");
        JTextField connNameTextField = getConnNameTextfield();
        connNameTextField.addActionListener(this);
        connNameTextField.addFocusListener(fl);
        connNameTextField.addKeyListener(kl);
        dbTypeCombo = new JComboBox();
        List<String> profileNames = DBDriverDialog.getDriverNames();
        for (String name : profileNames) {
            dbTypeCombo.addItem(name);
        }
        dbTypeCombo.setPrototypeDisplayValue("New DBType");
        dbTypeCombo.addItemListener(il);
        dbTypeCombo.setEditable(false);
        connMethodCombo = new JComboBox();
        connMethodCombo.addItem(DEFAULT_CONN_METHOD);
        connMethodCombo.addItemListener(il);
        connMethodCombo.setEditable(false);
        portField = new JTextField(DEFAULT_PORT, 5);
        portField.addFocusListener(fl);
        portField.addKeyListener(kl);
        hostnameField = new JTextField(DEFAULT_HOSTNAME, 20);
        hostnameField.addFocusListener(fl);
        hostnameField.addKeyListener(kl);
        schemaField = new JTextField("", 20);
        schemaField.addFocusListener(fl);
        schemaField.addKeyListener(kl);
        usernameField = new JTextField("", 20);
        usernameField.addFocusListener(fl);
        usernameField.addKeyListener(kl);
        passwordField = new JPasswordField("", 20);
        passwordField.addFocusListener(fl);
        passwordField.addKeyListener(kl);
        newConnButton = new JButton(NEW_CONN_BUTTON);
        newConnButton.addActionListener(this);
        saveConnButton = new JButton(SAVE_CONN_BUTTON);
        saveConnButton.addActionListener(this);
        delConnButton = new JButton(DEL_CONN_BUTTON);
        delConnButton.addActionListener(this);
        newDBTypeButton = new JButton(NEW_DBTYPE_BUTTON);
        newDBTypeButton.addActionListener(this);
        editDBTypeButton = new JButton(EDIT_DBTYPE_BUTTON);
        editDBTypeButton.addActionListener(this);
        delDBTypeButton = new JButton(DEL_DBTYPE_BUTTON);
        delDBTypeButton.addActionListener(this);
        connectButton = new JButton(CONNECT_BUTTON);
        connectButton.addActionListener(this);
        cancelButton = new JButton(CANCEL_BUTTON);
        cancelButton.addActionListener(this);
    }

    /**
	 * If there's a stored XML file, load data into UI fields. Otherwise fill
	 * with default data.
	 */
    private void loadData() {
        JTextField connNameTextField = getConnNameTextfield();
        readProfiles();
        if (profileList.isEmpty()) {
            connNameTextField.setText("Connection 1");
            connNameTextField.selectAll();
        } else {
            model = new MyComboBoxModel(profileList);
            connNameCombo.setModel(model);
            boolean preferenceLoaded = false;
            if (view != null) {
                String pref = view.getDBProfileNamePref();
                int index = this.findProfile(pref);
                if (index >= 0) {
                    loadUI(profileList.get(index));
                    preferenceLoaded = true;
                }
            }
            if (!preferenceLoaded) loadUI(profileList.get(0));
            setDialogState(LOADED);
        }
    }

    private void loadUI(DBProfile pro) {
        connNameCombo.setSelectedItem(pro);
        JTextField connNameField = getConnNameTextfield();
        connNameField.setText(connNameCombo.getSelectedItem().toString());
        dbTypeCombo.setSelectedItem(pro.getDriver().getDriverName());
        connMethodCombo.setSelectedItem(pro.getConnMethod());
        portField.setText(pro.getPort());
        hostnameField.setText(pro.getHostname());
        schemaField.setText(pro.getSchema());
        usernameField.setText(pro.getUsername());
        passwordField.setText("");
        connNameField.selectAll();
        connNameCombo.requestFocus();
    }

    /**
	 * Layout UI Elements
	 * 
	 * 3 main areas of UI: top = status (connected to DB or not) middle = user
	 * input fields (combo boxes, text fields, etc.) bottom = buttons (connect,
	 * save and cancel)
	 */
    private void layoutUIElements() {
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        statusPanel.add(statusLabel);
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        int row = 0, col = 0;
        c.gridx = col;
        c.gridy = row;
        c.gridwidth = 5;
        fieldsPanel.add(new DialogSeparator(), c);
        row++;
        col = 0;
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        Insets first = new Insets(0, 5, 5, 5);
        c.insets = first;
        c.anchor = GridBagConstraints.CENTER;
        fieldsPanel.add(connNameLabel, c);
        c = new GridBagConstraints();
        Insets rest = new Insets(0, 0, 5, 5);
        c.gridx = col++;
        c.gridy = row;
        Dimension size = connNameCombo.getPreferredSize();
        int length = size.width + 1;
        c.ipadx = 190 - length;
        c.insets = rest;
        fieldsPanel.add(connNameCombo, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.insets = rest;
        fieldsPanel.add(newConnButton, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.insets = rest;
        fieldsPanel.add(saveConnButton, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.insets = rest;
        fieldsPanel.add(delConnButton, c);
        row++;
        col = 0;
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.insets = first;
        c.anchor = GridBagConstraints.LINE_END;
        fieldsPanel.add(dbTypeLabel, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.ipadx = 90;
        c.insets = rest;
        fieldsPanel.add(dbTypeCombo, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.insets = rest;
        fieldsPanel.add(newDBTypeButton, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.insets = rest;
        fieldsPanel.add(editDBTypeButton, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.insets = rest;
        fieldsPanel.add(delDBTypeButton, c);
        row++;
        col = 0;
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.insets = first;
        c.anchor = GridBagConstraints.LINE_END;
        fieldsPanel.add(connMethodLabel, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.ipadx = 60;
        c.insets = rest;
        fieldsPanel.add(connMethodCombo, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.insets = rest;
        c.anchor = GridBagConstraints.LINE_END;
        fieldsPanel.add(portLabel, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.insets = rest;
        c.ipady = 5;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.LINE_START;
        fieldsPanel.add(portField, c);
        row++;
        col = 0;
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.insets = first;
        c.anchor = GridBagConstraints.LINE_END;
        fieldsPanel.add(hostnameLabel, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.gridwidth = 4;
        c.insets = rest;
        c.ipady = 5;
        c.anchor = GridBagConstraints.LINE_START;
        fieldsPanel.add(hostnameField, c);
        row++;
        col = 0;
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.insets = first;
        c.anchor = GridBagConstraints.LINE_END;
        fieldsPanel.add(schemaLabel, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.gridwidth = 4;
        c.insets = rest;
        c.ipady = 5;
        c.anchor = GridBagConstraints.LINE_START;
        fieldsPanel.add(schemaField, c);
        row++;
        col = 0;
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.insets = first;
        c.anchor = GridBagConstraints.LINE_END;
        fieldsPanel.add(usernameLabel, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.gridwidth = 4;
        c.insets = rest;
        c.ipady = 5;
        c.anchor = GridBagConstraints.LINE_START;
        fieldsPanel.add(usernameField, c);
        row++;
        col = 0;
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.insets = first;
        c.anchor = GridBagConstraints.LINE_END;
        fieldsPanel.add(passwordLabel, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.gridwidth = 4;
        c.insets = rest;
        c.ipady = 5;
        c.anchor = GridBagConstraints.LINE_START;
        fieldsPanel.add(passwordField, c);
        row++;
        col = 0;
        c = new GridBagConstraints();
        c.gridx = col;
        c.gridy = row;
        c.gridwidth = 5;
        fieldsPanel.add(new DialogSeparator(), c);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        buttonPanel.add(connectButton);
        buttonPanel.add(cancelButton);
        Container con = this.getContentPane();
        con.add(statusPanel, BorderLayout.NORTH);
        con.add(fieldsPanel, BorderLayout.CENTER);
        con.add(buttonPanel, BorderLayout.SOUTH);
        addWindowListener(new MyWindowListener());
        pack();
    }

    /**
	 * The parent parameter can be null here. This pops up a dialog so the user
	 * can enter Database connection values. Whatever object launches this
	 * dialog will get a reference to this class returned, after calling this
	 * method, and after the user presses Connect, Save or Cancel, or closes the
	 * window. The returned DBLogin object can then have its accessor methods
	 * called to get the info resulting from user interaction.
	 * 
	 * @param parent
	 *            the parent component launching this dialog - this can be null
	 * @param msg
	 *            any message the caller wants displayed in the dialog - this
	 *            can also be null
	 * @return a reference to this object
	 */
    public static DBLogin2 showDialog(Component parent, String msg) {
        if (instance == null) {
            instance = new DBLogin2(parent);
        }
        if (msg != null) {
            instance.setStatus(msg);
        } else if (instance.view != null) {
            if (instance.view.getSql().isConnected()) {
                instance.setStatus(DB_CONN);
            } else {
                instance.setStatus(DB_DIS);
            }
        } else {
            instance.setStatus("");
        }
        instance.setVisible(true);
        return instance;
    }

    public static DBLogin2 showDialog(Component parent) {
        return showDialog(parent, null);
    }

    /**
	 * Set the connection status
	 */
    public void setStatus(String status) {
        statusLabel.setText(" " + status);
    }

    /**
	 * @return Returns the connection name
	 */
    public String getConnectionName() {
        return getConnNameTextfield().getText();
    }

    private JTextField getConnNameTextfield() {
        return (JTextField) connNameCombo.getEditor().getEditorComponent();
    }

    /**
	 * @return Returns the database type.
	 */
    public String getDbType() {
        return dbTypeCombo.getSelectedItem() + "";
    }

    /**
	 * @return Returns the connection method
	 */
    public String getConnMethod() {
        return connMethodCombo.getSelectedItem() + "";
    }

    /**
	 * @return Returns the port number as a string.
	 */
    public String getPort() {
        return portField.getText();
    }

    /**
	 * @return Returns the host name.
	 */
    public String getHostname() {
        return hostnameField.getText();
    }

    /**
	 * @return Returns the user name.
	 */
    public String getUsername() {
        return usernameField.getText();
    }

    /**
	 * @return Returns the schema.
	 */
    public String getSchema() {
        return schemaField.getText();
    }

    /**
	 * @return Returns the password.
	 */
    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    /**
	 * @return Returns the exitValue.
	 */
    public int getExitValue() {
        return exitValue;
    }

    public DBSettings getDBSettings() {
        return dbs;
    }

    /**
	 * Keep track of current state of dialog
	 * 
	 * @param state
	 *            new state to set - can be one of {DELETED, LOADED, EDITED,
	 *            SAVED}
	 */
    private void setDialogState(int state) {
        if (state == DELETED) {
            dialogState = DELETED;
            saveConnButton.setEnabled(true);
        } else if (state == LOADED) {
            if (dialogState != DELETED) {
                dialogState = LOADED;
                saveConnButton.setEnabled(false);
            }
        } else if (state == EDITED) {
            if (dialogState != DELETED) dialogState = EDITED;
            saveConnButton.setEnabled(true);
        } else if (state == SAVED) {
            dialogState = SAVED;
            saveConnButton.setEnabled(false);
        } else {
            throw new IllegalArgumentException("Attempt to set dialog state to illegal value: " + state);
        }
    }

    /**
	 * handle button events
	 * 
	 * TODO: Change action methods into ActionListener classes so we don't need
	 * all these if statements
	 */
    public void actionPerformed(ActionEvent ae) {
        String command = ae.getActionCommand();
        Object src = ae.getSource();
        if (command.equals(CONNECT_BUTTON)) {
            connectAction();
        } else if (command.equals(SAVE_CONN_BUTTON)) {
            saveConnAction();
        } else if (command.equals(CANCEL_BUTTON)) {
            cancelAction();
        } else if (command.equals(NEW_CONN_BUTTON)) {
            newConnAction();
        } else if (command.equals(NEW_DBTYPE_BUTTON)) {
            newDBAction();
        } else if (command.equals(DEL_CONN_BUTTON)) {
            if (src == this.delConnButton) {
                deleteConnAction();
            } else if (src == delDBTypeButton) {
                deleteDriverAction();
            }
        } else if (command.equals(EDIT_DBTYPE_BUTTON)) {
            editDBAction();
        } else {
            throw new IllegalArgumentException("Invalid command in DBLogin2.actionPerformed: " + command);
        }
    }

    /**
	 * This is what happens when the connect button is pressed
	 */
    private void connectAction() {
        exitValue = DBLogin2.CONNECT;
        dbs = new DBSettings();
        dbs.setName(getConnectionName());
        dbs.setUser(getUsername());
        dbs.setPass(getPassword());
        dbs.setSchema(getSchema());
        dbs.setHost(getHostname());
        DBDriverProfile pro = DBDriverDialog.getDBDriverProfile(getDbType());
        dbs.setDriver(pro.getClassName());
        dbs.setUrlPrefix(pro.getUrlPrefix());
        setVisible(false);
    }

    /**
	 * This is what happens when the delete connection button is pressed
	 */
    private void deleteConnAction() {
        String sel = connNameCombo.getSelectedItem().toString();
        int index = connNameCombo.getSelectedIndex();
        int size = connNameCombo.getItemCount();
        if (index == size - 1) index = 0;
        model.delete(sel);
        setDialogState(DELETED);
        if (size > 1) {
            connNameCombo.setSelectedIndex(index);
            getConnNameTextfield().setText(connNameCombo.getSelectedItem().toString());
        } else {
            getConnNameTextfield().setText("");
            connNameCombo.setSelectedIndex(-1);
        }
        writeProfiles();
        setDialogState(SAVED);
        connNameCombo.requestFocus();
    }

    /**
	 * This is what happens when the delete DBType button is pressed
	 * 
	 * TODO: This method is almost identical to the delConnAction above. Can the
	 * common parts be factored out into a separate method?
	 */
    private void deleteDriverAction() {
        if (!DBDriverDialog.remove(getDbType())) throw new RuntimeException("Error trying to remove DB Driver " + getDbType()); else {
            String sel = dbTypeCombo.getSelectedItem() + "";
            int index = dbTypeCombo.getSelectedIndex();
            int size = dbTypeCombo.getItemCount();
            if (index == size - 1) index = 0;
            dbTypeCombo.removeItem(sel);
            if (size > 1) {
                dbTypeCombo.setSelectedIndex(index);
            } else {
                dbTypeCombo.setSelectedIndex(-1);
            }
            dbTypeCombo.requestFocus();
        }
    }

    /**
	 * Get data entered by user, store into profile
	 */
    public DBProfile getDialogData() {
        DBProfile profileData = new DBProfile();
        getDialogData(profileData);
        return profileData;
    }

    public void getDialogData(DBProfile profileData) {
        profileData.setConnName(instance.getConnectionName());
        profileData.setDriver(DBDriverDialog.getDBDriverProfile(instance.dbTypeCombo.getSelectedItem() + ""));
        profileData.setConnMethod(instance.connMethodCombo.getSelectedItem() + "");
        profileData.setPort(instance.portField.getText());
        profileData.setHostname(instance.hostnameField.getText());
        profileData.setSchema(instance.schemaField.getText());
        profileData.setUsername(instance.usernameField.getText());
        profileData.setPassword(new String(instance.passwordField.getPassword()));
    }

    /**
	 * This is what happens when the cancel button is pressed
	 */
    private void cancelAction() {
        int conf = JOptionPane.YES_OPTION;
        if (dialogState == EDITED) {
            conf = JOptionPane.showConfirmDialog(this, "Cancel without saving data?", "Are you sure?", JOptionPane.YES_NO_OPTION);
        }
        if (conf == JOptionPane.YES_OPTION) {
            exitValue = DBLogin2.CANCEL;
            setVisible(false);
        }
    }

    /**
	 * This is what happens when the save button is pressed
	 */
    private void saveConnAction() {
        String name = null;
        if (validateDataEntry()) {
            int conf = JOptionPane.YES_OPTION;
            int overwrite = -1;
            if (dialogState == EDITED) {
                name = getConnectionName();
                overwrite = findProfile(name);
                if (overwrite >= 0) {
                    conf = JOptionPane.showConfirmDialog(this, "Overwrite " + name + " profile?", "Profile already exists", JOptionPane.YES_NO_OPTION);
                }
            }
            if (conf == JOptionPane.YES_OPTION) {
                if (dialogState == EDITED) {
                    addToList(overwrite);
                    model = new MyComboBoxModel(profileList);
                    connNameCombo.setModel(model);
                    getConnNameTextfield().setText(name);
                }
                writeProfiles();
                setDialogState(SAVED);
                connNameCombo.requestFocus();
            }
        }
    }

    /**
	 * Search for a profile with the given name
	 * 
	 * @param name
	 *            the name to search for
	 * @return the index of the profile with the given name, or -1 if the
	 *         profile cannot be found
	 */
    private int findProfile(String name) {
        int found = -1;
        for (int i = 0, len = profileList.size(); i < len; i++) {
            DBProfile p = profileList.get(i);
            if (name.equals(p.getConnName())) {
                found = i;
                break;
            }
        }
        return found;
    }

    /**
	 * Either insert or replace an item in the profile list
	 * 
	 * @param index
	 *            if index is less than zero, then add the profile to the list.
	 *            Otherwise replace the profile at the given index in the list.
	 */
    private void addToList(int index) {
        DBProfile dialogData = getDialogData();
        if (index < 0) {
            profileList.add(dialogData);
        } else {
            profileList.set(index, dialogData);
        }
    }

    /**
	 * Validate that all data fields (except PW) are filled in. If any data is
	 * missing, give an appropriate error message.
	 * 
	 * @return <code>true</code> if everything required is filled in,
	 *         <code>false</code> otherwise
	 */
    private boolean validateDataEntry() {
        boolean valid = true;
        if (getConnectionName().length() == 0) {
            saveError(CONN_NAME_ERROR);
            valid = false;
        }
        if (valid && getDbType().length() == 0) {
            saveError(DB_TYPE_ERROR);
            valid = false;
        }
        if (valid && getConnMethod().length() == 0) {
            saveError(CONN_METHOD_ERROR);
            valid = false;
        }
        if (valid && getPort().length() == 0) {
            saveError(PORT_ERROR);
            valid = false;
        }
        if (valid && getHostname().length() == 0) {
            saveError(HOSTNAME_ERROR);
            valid = false;
        }
        if (valid && getSchema().length() == 0) {
            saveError(SCHEMA_ERROR);
            valid = false;
        }
        if (valid && getUsername().length() == 0) {
            saveError(USER_ERROR);
            valid = false;
        }
        return valid;
    }

    private void saveError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Insufficient Information", JOptionPane.ERROR_MESSAGE);
    }

    /**
	 * This is what happens when the newConnection button is pressed
	 */
    private void newConnAction() {
        int conf = JOptionPane.YES_OPTION;
        if (dialogState == EDITED) {
            conf = JOptionPane.showConfirmDialog(this, "Warning: This action will erase your current edits.\n" + "Are you sure you want to continue?", "Profile has changed", JOptionPane.YES_NO_OPTION);
        }
        if (conf == JOptionPane.YES_OPTION) {
            reset();
            getConnNameTextfield().setText(newConnectionName());
            setDialogState(SAVED);
        }
    }

    /**
	 * Generate a new connection name - use when user clicks New Connection
	 * button
	 */
    private String newConnectionName() {
        int number = 1;
        String name = "Connection " + number;
        while (model.findName(name) != null) {
            number++;
            name = "Connection " + number;
        }
        return name;
    }

    /**
	 * This is what happens when the newDBType button is pressed
	 */
    private void newDBAction() {
        addOrEditDB(null);
    }

    /**
	 * This is what happens when the Edit DBType button is pressed
	 */
    private void editDBAction() {
        addOrEditDB(getDbType());
    }

    private void addOrEditDB(String driverName) {
        driverDialog = DBDriverDialog.showDialog(this, driverName);
        if (driverDialog.getExitValue() == DBDriverDialog.OK) {
            String name = driverDialog.getDriverName();
            if (!findItem(dbTypeCombo, name)) {
                dbTypeCombo.addItem(name);
                dbTypeCombo.setSelectedItem(name);
                dbTypeCombo.requestFocus();
            }
        }
    }

    private boolean findItem(JComboBox combo, String item) {
        boolean found = false;
        for (int i = 0, max = combo.getItemCount(); i < max; i++) {
            String next = combo.getItemAt(i).toString();
            if (next.equals(item)) {
                found = true;
                break;
            }
        }
        return found;
    }

    /**
	 * This resets the GUI elements to default values
	 */
    private void reset() {
        getConnNameTextfield().setText("");
        dbTypeCombo.setSelectedIndex(0);
        connMethodCombo.setSelectedIndex(0);
        portField.setText(DEFAULT_PORT);
        hostnameField.setText(DEFAULT_HOSTNAME);
        schemaField.setText("");
        usernameField.setText("");
        passwordField.setText("");
    }

    private void readProfiles() {
        File file = new File(PROFILES_FILE_NAME);
        if (!file.exists()) {
            return;
        }
        Document doc = null;
        try {
            doc = XMLDocReaderWriter.readXMLFile(file);
        } catch (ParserConfigurationException e1) {
            e1.printStackTrace();
        }
        Element e = doc.getDocumentElement();
        if (e == null) {
            System.out.println("e is null");
        }
        NodeList nodes = null;
        if (e.getTagName().equals(XMLTags.DATABASE_PROFILES)) {
            nodes = e.getChildNodes();
        } else {
            nodes = e.getElementsByTagName(XMLTags.DATABASE_PROFILES);
            Node node = nodes.item(0);
            nodes = node.getChildNodes();
        }
        for (int k = 0, n = nodes.getLength(); k < n; k++) {
            Node nd = nodes.item(k);
            if (nd.getNodeName().equals(XMLTags.DB_PROFILE)) {
                DBProfile dbp = DBProfile.readFromXML((Element) nd);
                profileList.add(dbp);
            }
        }
    }

    private void writeProfiles() {
        Document doc = null;
        try {
            doc = XMLDocReaderWriter.createXMLFile();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Node n = doc.createElement(XMLTags.DATABASE_PROFILES);
        for (DBProfile profile : profileList) {
            if (profile != null) {
                profile.writeToXML(n);
            }
        }
        doc.appendChild(n);
        XMLDocReaderWriter.writeXMLFile(doc, PROFILES_FILE_NAME);
    }

    /**
	 * On focus gained, select text in a field when you tab between them.
	 */
    private class MyFocusListener extends FocusAdapter {

        public void focusGained(FocusEvent arg0) {
            Object o = arg0.getSource();
            if (o instanceof JTextField) {
                ((JTextField) o).selectAll();
            } else if (o.equals(connNameCombo)) {
                getConnNameTextfield().selectAll();
            }
        }
    }

    /**
	 * Listen for keystrokes because this means some UI element has been edited.
	 */
    private class MyKeyListener extends KeyAdapter {

        public void keyTyped(KeyEvent ke) {
            Object o = ke.getSource();
            if (!o.equals(passwordField)) setDialogState(EDITED);
        }
    }

    /**
	 * Listen for item changes in combo boxes because this means some UI element
	 * has been edited.
	 * 
	 * If change occurs in ConnName combo, load profile. Note this doesn't mean
	 * something has been edited.
	 */
    private class MyItemListener implements ItemListener {

        public void itemStateChanged(ItemEvent ie) {
            Object o = ie.getSource();
            if (o.equals(dbTypeCombo)) {
                setDialogState(EDITED);
            } else if (o.equals(connMethodCombo)) {
                setDialogState(EDITED);
            } else if (o.equals(connNameCombo)) {
                String name = ie.getItem().toString();
                DBProfile found = null;
                for (DBProfile pro : profileList) {
                    if (name.equals(pro.getConnName())) {
                        found = pro;
                        break;
                    }
                }
                if (found != null) {
                    DBProfile profileData = found;
                    loadUI(profileData);
                    setDialogState(LOADED);
                }
            }
        }
    }

    /**
	 * Simple window listener that grabs window events and calls cancelAction()
	 * to do any clean up. This way, even if the user hits the X in the upper
	 * right to close the dialog, you still capture that info and can respond
	 * appropriately.
	 * 
	 * @author John T. Langton - jlangton at visitrend dot com
	 * 
	 */
    private class MyWindowListener extends WindowAdapter {

        public void windowClosing(WindowEvent arg0) {
            cancelAction();
        }
    }

    /**
	 * Model for connection name combo box - model is the list of profiles,
	 * where the name is the "key". Allows for insertion and deletion
	 */
    private class MyComboBoxModel extends DefaultComboBoxModel {

        private DBProfile selectedItem;

        private List<DBProfile> arrayList;

        public MyComboBoxModel(List<DBProfile> profileList) {
            arrayList = profileList;
        }

        public Object getSelectedItem() {
            return selectedItem;
        }

        public void setSelectedItem(Object newValue) {
            if (newValue instanceof DBProfile) selectedItem = (DBProfile) newValue; else selectedItem = findName((String) newValue);
        }

        public int getSize() {
            return arrayList.size();
        }

        public Object getElementAt(int i) {
            return arrayList.get(i);
        }

        public void addElement(Object obj) {
            arrayList.add((DBProfile) obj);
        }

        public void insertElementAt(Object obj, int index) {
            arrayList.add(index, (DBProfile) obj);
        }

        public void removeElement(Object obj) {
            arrayList.remove(obj);
        }

        public void removeElementAt(int index) {
            arrayList.remove(index);
        }

        /**
		 * Remove the specified item from the list
		 * 
		 * @param item
		 *            the name of the item to remove
		 */
        public void delete(String item) {
            DBProfile pro = findName(item);
            arrayList.remove(pro);
        }

        /**
		 * Search for a profile with the given name
		 * 
		 * @param name
		 *            the name to search for
		 * @return the profile with the given name, or null if the profile
		 *         cannot be found
		 */
        private DBProfile findName(String name) {
            DBProfile found = null;
            for (DBProfile pro : arrayList) {
                if (name.equals(pro.getConnName())) {
                    found = pro;
                    break;
                }
            }
            return found;
        }
    }

    private class DialogSeparator extends JLabel {

        private static final int OFFSET = 15;

        public DialogSeparator() {
        }

        public Dimension getPreferredSize() {
            return new Dimension(getParent().getWidth(), 20);
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public void paint(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            Dimension d = getSize();
            int y = (d.height - 3) / 2;
            g.setColor(Color.white);
            g.drawLine(1, y, d.width - 1, y);
            y++;
            g.drawLine(0, y, 1, y);
            g.setColor(Color.gray);
            g.drawLine(d.width - 1, y, d.width, y);
            y++;
            g.drawLine(1, y, d.width - 1, y);
            String text = getText();
            if (text.length() == 0) return;
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            y = (d.height + fm.getAscent()) / 2;
            int fontWidth = fm.stringWidth(text);
            g.setColor(getBackground());
            g.fillRect(OFFSET - 5, 0, OFFSET + fontWidth, d.height);
            g.setColor(getForeground());
            g.drawString(text, OFFSET, y);
        }
    }

    public static void main(String[] args) {
        UIManager.put("Button.defaultButtonFollowsFocus", Boolean.TRUE);
        DBLogin2 log = DBLogin2.showDialog(null);
        int result = log.getExitValue();
        String msg = "Dialog exited with ";
        switch(result) {
            case CONNECT:
                msg += "Connect button pressed.";
                break;
            case CANCEL:
                msg += "Cancel button pressed.";
                break;
            default:
                msg = "Unknown value returned from dialog: " + result;
        }
        System.out.println(msg);
        System.out.println("  Connection Name: " + log.getConnectionName());
        System.out.println("  Database Type: " + log.getDbType());
        System.out.println("  Connection Method: " + log.getConnMethod());
        System.out.println("  Port: " + log.getPort());
        System.out.println("  Hostname: " + log.getHostname());
        System.out.println("  Schema: " + log.getSchema());
        System.out.println("  Username: " + log.getUsername());
        System.out.println("  Password: " + log.getPassword());
        System.exit(0);
    }
}
