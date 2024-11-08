package saadadb.newdatabase;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import saadadb.admin.SaadaDBAdmin;
import saadadb.database.Database;
import saadadb.database.DbmsWrapper;
import saadadb.database.InstallParamValidator;
import saadadb.database.MysqlWrapper;
import saadadb.database.PostgresWrapper;
import saadadb.database.SQLiteWrapper;
import saadadb.exceptions.FatalException;
import saadadb.exceptions.IgnoreException;
import saadadb.exceptions.QueryException;
import saadadb.util.HostAddress;
import saadadb.util.Messenger;
import saadadb.util.RegExp;

public class FormPanel extends JPanel {

    /**
	 *  @version $Id: FormPanel.java 366 2012-04-19 16:03:23Z laurent.mistahl $
	 * 
	 */
    private static final long serialVersionUID = 1L;

    protected JFrame frame;

    protected JTextField saadadb_name = new JTextField(10);

    protected JTextField saadadb_home = new JTextField(32);

    protected JTextField saadadb_rep = new JTextField(32);

    protected JComboBox dbms_combo = new JComboBox(new String[] { "SQLite", "Postgres", "MySQL" });

    protected JTextField dbms_server = new JTextField("127.0.0.1", 16);

    protected JTextField dbms_port = new JTextField(5);

    protected JTextField dbms_database_name = new JTextField(10);

    protected JTextField dbms_admin = new JTextField(System.getProperty("user.name"), 10);

    protected JPasswordField dbms_admin_passwd = new JPasswordField(10);

    protected DbmsWrapper dbmswrapper;

    protected JTextField dbms_reader = new JTextField(System.getProperty("user.name"), 10);

    protected JPasswordField dbms_reader_passwd = new JPasswordField(10);

    private JPanel dbms_panel;

    private JPanel dbms_admin_panel;

    protected JTextField tomcat_home = new JTextField(32);

    protected JTextField url_root = new JTextField(24);

    protected JComboBox att_type = new JComboBox(new String[] { "String", "int", "double", "boolean" });

    protected JTextField att_name = new JTextField(10);

    protected JComboBox att_list = new JComboBox();

    protected JRadioButton[] cat_button = { new JRadioButton("Misc"), new JRadioButton("Image"), new JRadioButton("Spectrum"), new JRadioButton("Table"), new JRadioButton("Entry"), new JRadioButton("Flatfile") };

    LinkedHashMap[] cat_att = { new LinkedHashMap<String, String>(), new LinkedHashMap<String, String>(), new LinkedHashMap<String, String>(), new LinkedHashMap<String, String>(), new LinkedHashMap<String, String>(), new LinkedHashMap<String, String>() };

    protected JComboBox coord_syst = new JComboBox(new String[] { "ICRS", "FK5,J2000", "FK4,1950", "Galactic", "Ecliptic" });

    protected JRadioButton[] channel_button = { new JRadioButton("Channel"), new JRadioButton("Energy"), new JRadioButton("Frequency"), new JRadioButton("Wavelength") };

    protected String[][] sp_units = { new String[0], { "eV", "keV", "MeV", "GeV", "TeV" }, { "Hz", "kHz", "MHz", "GHz" }, { "Angstrom", "nm", "um", "m", "mm", "cm", "km", "nm" } };

    protected JComboBox spec_syst = new JComboBox();

    /**
	 * Build the frame with the first panel open
	 * @param frame
	 */
    FormPanel(JFrame frame) {
        super();
        this.setBackground(SaadaDBAdmin.beige_color);
        this.setPreferredSize(new Dimension(550, 350));
        this.frame = frame;
        this.setLayout(new GridBagLayout());
        this.setSaadaDirPanel();
    }

    /**
	 * Display the panel #screen_number
	 * @param screen_number
	 * @throws UnknownHostException 
	 */
    public void jumpToPanel(int screen_number) {
        switch(screen_number) {
            case 0:
                this.setSaadaDirPanel();
                break;
            case 1:
                this.setDatabaseConnectPanel();
                try {
                    SQLiteWrapper.getExtensionFilePath();
                } catch (Exception e) {
                    dbms_combo.removeItemAt(0);
                    JOptionPane.showMessageDialog(this, "SQLite can not be used because the native SQL procedure libraries is not available for your platform\n" + e.getMessage(), "Missing resource", JOptionPane.WARNING_MESSAGE);
                }
                break;
            case 2:
                if (dbms_combo.getSelectedItem().toString().equalsIgnoreCase("sqlite")) {
                    this.setWebRootPanel();
                } else {
                    this.setDatabaseReaderPanel();
                }
                break;
            case 3:
                this.setWebRootPanel();
                break;
            case 4:
                this.setNewAttPanel();
                break;
            case 5:
                this.setSysAndUnitPanel();
                break;
        }
    }

    /**
	 * Valid the content of the panel the panel #screen_number
	 * @param screen_number
	 */
    public boolean validPanel(int screen_number) {
        switch(screen_number) {
            case 0:
                return this.validSaadaDirPanel();
            case 1:
                return this.validDatabaseConnectPanel();
            case 2:
                return this.validDatabaseReaderPanel();
            case 3:
                return this.validWebRootPanel();
            case 4:
                return this.validNewAttPanel();
            case 5:
                return this.validSysAndUnitPanel();
        }
        return false;
    }

    /**
	 * First panel SaadaDB + directories
	 */
    public void setSaadaDirPanel() {
        this.removeAll();
        GridBagConstraints c0 = new GridBagConstraints();
        c0.fill = GridBagConstraints.HORIZONTAL;
        c0.weightx = 1;
        c0.weighty = 0;
        JPanel jp = new JPanel();
        jp.setBackground(SaadaDBAdmin.beige_color);
        jp.setLayout(new GridBagLayout());
        jp.setBorder(BorderFactory.createTitledBorder("SaadaDB Name"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 1;
        jp.add(saadadb_name, c);
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        jp.add(getFixedText("The SaadaDB name is used to build the database URL. Database files are installed in subdirectories with that name"), c);
        c0.gridx = 0;
        c0.gridy = 0;
        this.add(jp, c0);
        jp = new JPanel();
        jp.setBackground(SaadaDBAdmin.beige_color);
        jp.setLayout(new GridBagLayout());
        jp.setBorder(BorderFactory.createTitledBorder("SaadaDB Installation Directory"));
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        jp.add(saadadb_home, c);
        c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        JButton browse1 = new JButton("Browse");
        browse1.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                JFileChooser fcd = new JFileChooser(SaadaDBAdmin.current_dir);
                fcd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int retour = fcd.showOpenDialog(FormPanel.this.frame);
                if (retour == JFileChooser.APPROVE_OPTION) {
                    File selected_file = fcd.getSelectedFile();
                    SaadaDBAdmin.current_dir = selected_file.getParent();
                    FormPanel.this.saadadb_home.setText(selected_file.getAbsolutePath());
                }
            }
        });
        jp.add(browse1, c);
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 2;
        jp.add(getFixedText("The SaadaDB core will be installed in a subdirectory of the installation directory."), c);
        c0.gridx = 0;
        c0.gridy = 1;
        this.add(jp, c0);
        jp = new JPanel();
        jp.setBackground(SaadaDBAdmin.beige_color);
        jp.setLayout(new GridBagLayout());
        jp.setBorder(BorderFactory.createTitledBorder("SaadaDB Repository"));
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        jp.add(saadadb_rep, c);
        c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        JButton browse2 = new JButton("Browse");
        browse2.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                JFileChooser fcd = new JFileChooser(SaadaDBAdmin.current_dir);
                fcd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int retour = fcd.showOpenDialog(FormPanel.this.frame);
                if (retour == JFileChooser.APPROVE_OPTION) {
                    File selected_file = fcd.getSelectedFile();
                    FormPanel.this.saadadb_rep.setText(selected_file.getAbsolutePath());
                }
            }
        });
        jp.add(browse2, c);
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 2;
        jp.add(getFixedText("The repository will contain a copy of all loaded files."), c);
        c0.gridx = 0;
        c0.gridy = 2;
        this.add(jp, c0);
        this.updateUI();
    }

    /**
	 * Returns true if the values set in this panel are OK
	 * @return
	 */
    protected boolean validSaadaDirPanel() {
        try {
            InstallParamValidator.validName(saadadb_name.getText().trim());
            InstallParamValidator.isDirectoryWritable(saadadb_home.getText().trim());
            InstallParamValidator.isDirectoryWritable(saadadb_rep.getText().trim());
        } catch (QueryException e) {
            Messenger.printStackTrace(e);
            JOptionPane.showMessageDialog(frame, e.getMessage(), "Configuration Error", JOptionPane.ERROR_MESSAGE);
            return true;
        }
        if (saadadb_rep.getText().trim().equals(saadadb_home.getText().trim())) {
            JOptionPane.showMessageDialog(frame, "The SaadaDB installation can not be the same as the repository", "Configuration Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
	 * Second panel: database access
	 */
    public void setDatabaseConnectPanel() {
        this.removeAll();
        GridBagConstraints c0 = new GridBagConstraints();
        c0.fill = GridBagConstraints.HORIZONTAL;
        dbms_panel = new JPanel();
        dbms_panel.setBackground(SaadaDBAdmin.beige_color);
        dbms_panel.setLayout(new GridBagLayout());
        dbms_panel.setBorder(BorderFactory.createTitledBorder("Database Management System"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        dbms_panel.add(getPlainLabel("Database Management System."), c);
        c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        dbms_combo.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                String dbms = (String) cb.getSelectedItem();
                if (dbms.equalsIgnoreCase("SQLITE")) {
                    FormPanel.this.enableDatabaseConnectPanel(false);
                } else {
                    FormPanel.this.enableDatabaseConnectPanel(true);
                }
            }
        });
        dbms_panel.add(dbms_combo, c);
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_END;
        dbms_panel.add(getPlainLabel("Database Server."), c);
        c.gridx = 1;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
        dbms_panel.add(dbms_server, c);
        c.gridx = 0;
        c.gridy = 2;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_END;
        dbms_panel.add(getPlainLabel("Server Port."), c);
        c.gridx = 1;
        c.gridy = 2;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
        dbms_panel.add(dbms_port, c);
        c.gridx = 0;
        c.gridy = 3;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_END;
        dbms_panel.add(getPlainLabel("Relational Database Name"), c);
        c.gridx = 1;
        c.gridy = 3;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
        dbms_database_name.setText(saadadb_name.getText().trim());
        dbms_panel.add(dbms_database_name, c);
        c0.gridx = 0;
        c0.gridy = 0;
        this.add(dbms_panel, c0);
        dbms_admin_panel = new JPanel();
        dbms_admin_panel.setBackground(SaadaDBAdmin.beige_color);
        dbms_admin_panel.setLayout(new GridBagLayout());
        dbms_admin_panel.setBorder(BorderFactory.createTitledBorder("Administrator Role"));
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_END;
        dbms_admin_panel.add(getPlainLabel("Admin Role Name"), c);
        c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
        dbms_admin_panel.add(dbms_admin, c);
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_END;
        dbms_admin_panel.add(getPlainLabel("Admin Role password."), c);
        c.gridx = 1;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
        dbms_admin_panel.add(dbms_admin_passwd, c);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        dbms_admin_panel.add(getFixedText("The administrator role must have permission du modify the DBMS. Password is not stored within Saada but prompted."), c);
        c0.gridx = 0;
        c0.gridy = 1;
        this.add(dbms_admin_panel, c0);
        this.enableDatabaseConnectPanel(false);
        this.updateUI();
    }

    /**
	 *  @@@@@@@@@@@@@@@@@@
	 * @param enabled
	 */
    private void enableDatabaseConnectPanel(boolean enabled) {
        for (Component cp : dbms_panel.getComponents()) {
            if (!(cp instanceof JComboBox) && cp.toString().indexOf("Manage") == -1) cp.setEnabled(enabled);
        }
        for (Component cp : dbms_admin_panel.getComponents()) {
            cp.setEnabled(enabled);
        }
        TitledBorder tb = ((TitledBorder) (dbms_admin_panel.getBorder()));
        if (!enabled) {
            tb.setTitleColor(Color.GRAY);
        } else {
            tb.setTitleColor(Color.BLACK);
        }
    }

    /**
	 * Returns true if the values set in this panel are OK
	 * @return
	 */
    protected boolean validDatabaseConnectPanel() {
        Cursor cursor_org = frame.getCursor();
        boolean retour = true;
        try {
            frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            if (dbms_combo.getSelectedItem().toString().equalsIgnoreCase("postgres")) {
                dbmswrapper = PostgresWrapper.getWrapper(dbms_server.getText().trim(), dbms_port.getText().trim());
            } else if (dbms_combo.getSelectedItem().toString().equalsIgnoreCase("mysql")) {
                dbmswrapper = MysqlWrapper.getWrapper(dbms_server.getText().trim(), dbms_port.getText().trim());
            } else if (dbms_combo.getSelectedItem().toString().equalsIgnoreCase("sqlite")) {
                dbmswrapper = SQLiteWrapper.getWrapper(dbms_server.getText().trim(), dbms_port.getText().trim());
            } else {
                JOptionPane.showMessageDialog(frame, "Select a DBMS.", "Configuration Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            dbmswrapper.setAdminAuth(dbms_admin.getText().trim(), dbms_admin_passwd.getPassword());
            dbmswrapper.checkAdminPrivileges(((NewSaadaDBTool) (frame)).saada_home + Database.getSepar() + "tmp", false);
            if (!dbms_database_name.getText().trim().matches(RegExp.DBNAME)) {
                JOptionPane.showMessageDialog(frame, "Relational database name badly formed.", "Configuration Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            String db_local_name = dbms_database_name.getText().trim();
            String rep_path = (new File(saadadb_rep.getText().trim(), db_local_name)).getAbsolutePath();
            if (dbmswrapper.dbExists(rep_path, db_local_name)) {
                if (JOptionPane.showConfirmDialog(frame, "Relational database <" + db_local_name + "> already exists. Do you want to remove it?", "Configuration Question", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    dbmswrapper.dropDB(rep_path, db_local_name);
                } else {
                    return false;
                }
            }
        } catch (ClassNotFoundException e) {
            Messenger.printStackTrace(e);
            JOptionPane.showMessageDialog(frame, "Fatal error: Can not build the DB wrapper: " + e, "Configuration Error", JOptionPane.ERROR_MESSAGE);
            retour = false;
        } catch (SQLException e) {
            Messenger.printStackTrace(e);
            JOptionPane.showMessageDialog(frame, "SQL Fatal error: " + e, "Configuration Error", JOptionPane.ERROR_MESSAGE);
            retour = false;
        } catch (Exception e) {
            Messenger.printStackTrace(e);
            JOptionPane.showMessageDialog(frame, "Fatal error: " + e, "Configuration Error", JOptionPane.ERROR_MESSAGE);
            retour = false;
        } finally {
            frame.setCursor(cursor_org);
        }
        return retour;
    }

    /**
	 * 
	 */
    public void setDatabaseReaderPanel() {
        this.removeAll();
        GridBagConstraints c0 = new GridBagConstraints();
        c0.fill = GridBagConstraints.HORIZONTAL;
        JPanel jp = new JPanel();
        jp.setBackground(SaadaDBAdmin.beige_color);
        jp.setLayout(new GridBagLayout());
        jp.setBorder(BorderFactory.createTitledBorder("Database Management System"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_END;
        jp.add(getPlainLabel("Select Reader Role."), c);
        c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
        jp.add(dbms_reader, c);
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_END;
        jp.add(getPlainLabel("Select Reader Role password."), c);
        c.gridx = 1;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
        jp.add(dbms_reader_passwd, c);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        jp.add(getFixedText("The reader role must have read permission. Password is stored within Saada."), c);
        c0.gridx = 0;
        c0.gridy = 0;
        this.add(jp, c0);
        this.updateUI();
    }

    /**
	 * Returns true if the values set in this panel are OK
	 * @return
	 */
    protected boolean validDatabaseReaderPanel() {
        Cursor cursor_org = frame.getCursor();
        boolean retour = true;
        try {
            if (dbmswrapper == null) {
                JOptionPane.showMessageDialog(frame, "Fatal error: DBMS wrapper not set: valid the prvious screen please.", "Configuration Error", JOptionPane.ERROR_MESSAGE);
                return false;
            } else {
                frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                dbmswrapper.setReaderAuth(dbms_reader.getText().trim(), dbms_reader_passwd.getPassword());
                dbmswrapper.checkReaderPrivileges();
                Thread.sleep(3000);
                dbmswrapper.cleanUp();
            }
        } catch (SQLException e) {
            Messenger.printStackTrace(e);
            JOptionPane.showMessageDialog(frame, "SQL Fatal error: " + e.getMessage(), "Configuration Error", JOptionPane.ERROR_MESSAGE);
            retour = false;
        } catch (IgnoreException e) {
            Messenger.printStackTrace(e);
            JOptionPane.showMessageDialog(frame, "Internal Fatal error: " + e.getMessage(), "Configuration Error", JOptionPane.ERROR_MESSAGE);
            retour = false;
        } catch (InterruptedException e) {
            Messenger.printStackTrace(e);
            JOptionPane.showMessageDialog(frame, "Thread Fatal error: " + e.getMessage(), "Configuration Error", JOptionPane.ERROR_MESSAGE);
            retour = false;
        } catch (Exception e) {
            Messenger.printStackTrace(e);
            JOptionPane.showMessageDialog(frame, "Exception: " + e.getMessage(), "Configuration Error", JOptionPane.ERROR_MESSAGE);
            retour = false;
        } finally {
            frame.setCursor(cursor_org);
        }
        return retour;
    }

    /**
	 * @throws UnknownHostException 
	 * 
	 */
    public void setWebRootPanel() {
        this.removeAll();
        GridBagConstraints c0 = new GridBagConstraints();
        c0.fill = GridBagConstraints.HORIZONTAL;
        JPanel jp = new JPanel();
        jp.setBackground(SaadaDBAdmin.beige_color);
        jp.setLayout(new GridBagLayout());
        jp.setBorder(BorderFactory.createTitledBorder("Tomcat Deployment"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        jp.add(tomcat_home, c);
        c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        JButton browse1 = new JButton("Browse");
        browse1.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                JFileChooser fcd = new JFileChooser();
                fcd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int retour = fcd.showOpenDialog(FormPanel.this.frame);
                if (retour == JFileChooser.APPROVE_OPTION) {
                    File selected_file = fcd.getSelectedFile();
                    FormPanel.this.tomcat_home.setText(selected_file.getAbsolutePath());
                }
            }
        });
        jp.add(browse1, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        jp.add(getFixedText("The SaadaDB administrator must have permission to write in TOMCAT_HOME"), c);
        c0.gridx = 0;
        c0.gridy = 0;
        this.add(jp, c0);
        jp = new JPanel();
        jp.setBackground(SaadaDBAdmin.beige_color);
        jp.setLayout(new GridBagLayout());
        jp.setBorder(BorderFactory.createTitledBorder("SaadaDB Base URL"));
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        if (url_root.getText().trim().length() == 0) {
            url_root.setText("http://" + HostAddress.getCanonicalHostname() + ":8080/" + saadadb_name.getText().trim());
        }
        jp.add(url_root, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        jp.add(getFixedText("The SaadaDB base URL can differ from the default to pass a proxy e.g."), c);
        c0.gridx = 0;
        c0.gridy = 1;
        this.add(jp, c0);
        this.updateUI();
    }

    /**
	 * Returns true if the values set in this panel are OK
	 * @return
	 */
    protected boolean validWebRootPanel() {
        try {
            try {
                InstallParamValidator.canBeTomcatDir(tomcat_home.getText().trim());
                tomcat_home.setText(tomcat_home.getText().trim() + Database.getSepar() + "webapps");
            } catch (QueryException e) {
                InstallParamValidator.canBeTomcatWebappsDir(tomcat_home.getText().trim());
            }
            InstallParamValidator.validURL(url_root.getText().trim());
        } catch (QueryException e) {
            Messenger.printStackTrace(e);
            JOptionPane.showMessageDialog(frame, e.getMessage(), "Configuration Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    class CategoryActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            FormPanel.this.att_list.removeAllItems();
            FormPanel.this.att_name.setText("");
            LinkedHashMap<String, String> lhm = null;
            for (int i = 0; i < cat_att.length; i++) {
                if (cat_button[i].isSelected()) {
                    lhm = cat_att[i];
                    for (Entry<String, String> entry : lhm.entrySet()) {
                        FormPanel.this.att_list.addItem(entry.getKey() + " (" + entry.getValue() + ")");
                    }
                }
            }
        }
    }

    public void setNewAttPanel() {
        this.removeAll();
        GridBagConstraints c0 = new GridBagConstraints();
        c0.fill = GridBagConstraints.HORIZONTAL;
        JPanel jp = new JPanel();
        jp.setBackground(SaadaDBAdmin.beige_color);
        jp.setLayout(new GridBagLayout());
        jp.setBorder(BorderFactory.createTitledBorder("Product Category"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        ButtonGroup bg = new ButtonGroup();
        ActionListener al = new CategoryActionListener();
        for (int i = 0; i < cat_att.length; i++) {
            c.gridx = i;
            c.gridy = 0;
            jp.add(cat_button[i]);
            cat_button[i].addActionListener(al);
            cat_button[i].setBackground(SaadaDBAdmin.beige_color);
            bg.add(cat_button[i]);
        }
        cat_button[0].setSelected(true);
        this.add(jp, c);
        c0.gridx = 0;
        c0.gridy = 0;
        this.add(jp, c0);
        jp = new JPanel();
        jp.setBackground(SaadaDBAdmin.beige_color);
        jp.setLayout(new GridBagLayout());
        jp.setBorder(BorderFactory.createTitledBorder("User Attribute Definition"));
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        jp.add(getPlainLabel("Attribute Name."), c);
        c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        jp.add(att_name, c);
        c.gridx = 2;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        jp.add(getPlainLabel("of type"), c);
        c.gridx = 3;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        jp.add(att_type, c);
        c.gridx = 4;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        JButton button = new JButton("add");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(@SuppressWarnings("unused") ActionEvent arg0) {
                String name = att_name.getText().trim();
                if (name.matches(RegExp.EXTATTRIBUTE)) {
                    FormPanel.this.att_list.addItem(name + " (" + att_type.getSelectedItem() + ")");
                    LinkedHashMap<String, String> lhm = null;
                    for (int i = 0; i < cat_att.length; i++) {
                        if (cat_button[i].isSelected()) {
                            lhm = cat_att[i];
                            lhm.put(name, att_type.getSelectedItem().toString());
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "Attribute name <" + name + "> badly formed", "Configuration Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        jp.add(button, c);
        c.gridwidth = 3;
        c.gridy = 1;
        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_END;
        c.fill = GridBagConstraints.NONE;
        jp.add(att_list, c);
        button = new JButton("Remove");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(@SuppressWarnings("unused") ActionEvent arg0) {
                String name = FormPanel.this.att_list.getSelectedItem().toString().split(" ")[0];
                FormPanel.this.att_list.removeItem(FormPanel.this.att_list.getSelectedItem());
                LinkedHashMap<String, String> lhm = null;
                for (int i = 0; i < cat_att.length; i++) {
                    if (cat_button[i].isSelected()) {
                        lhm = cat_att[i];
                        lhm.remove(name);
                    }
                }
            }
        });
        c.gridwidth = 1;
        c.gridy = 1;
        c.gridx = 4;
        c.fill = GridBagConstraints.NONE;
        jp.add(button, c);
        this.add(jp, c);
        c0.gridx = 0;
        c0.gridy = 1;
        this.add(jp, c0);
        this.updateUI();
    }

    /**
	 * Returns true if the values set in this panel are OK
	 * @return
	 */
    protected boolean validNewAttPanel() {
        try {
            String filename = ((NewSaadaDBTool) (frame)).saada_home + Database.getSepar() + "config" + Database.getSepar() + "collection_attr.xml";
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            bw.write("<?xml version=\"1.0\" encoding=\"iso-8859-1\" standalone=\"no\"?>\n");
            bw.write("<!DOCTYPE collection_attr SYSTEM \"collection_attr.dtd\">\n");
            bw.write("<collection_attr>\n");
            for (int i = 0; i < cat_button.length; i++) {
                bw.write("    <list name=\"" + cat_button[i].getText().toUpperCase() + "\">\n");
                LinkedHashMap<String, String> lhm = cat_att[i];
                for (Entry<String, String> entry : lhm.entrySet()) {
                    bw.write("        <attr name=\"" + entry.getKey().trim() + "\" type=\"" + entry.getValue().trim() + "\"/>\n");
                }
                bw.write("    </list>\n");
            }
            bw.write("</collection_attr>\n");
            bw.close();
        } catch (Exception ex) {
            Messenger.printStackTrace(ex);
            JOptionPane.showMessageDialog(frame, "Writing extended attr file: " + ex.getMessage(), "Configuration Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    class SpectCoordActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < sp_units.length; i++) {
                if (i == 0) {
                    spec_syst.setVisible(false);
                } else if (channel_button[i].isSelected()) {
                    spec_syst.removeAllItems();
                    String[] units = sp_units[i];
                    for (int j = 0; j < units.length; j++) {
                        spec_syst.addItem(units[j]);
                    }
                    spec_syst.setVisible(true);
                }
            }
        }
    }

    /**
	 * 
	 */
    public void setSysAndUnitPanel() {
        this.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 5, 10, 5);
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        JPanel cat_panel = new JPanel();
        cat_panel.setPreferredSize(new Dimension(450, 100));
        cat_panel.setBackground(SaadaDBAdmin.beige_color);
        cat_panel.setBorder(BorderFactory.createTitledBorder("GLobal Coordinate System"));
        cat_panel.setLayout(new BorderLayout());
        cat_panel.add(getFixedText("Select the coordinate system used at collection level\nAll ingested data will be expressed with this system in addition with their native coordinates"), BorderLayout.PAGE_START);
        cat_panel.add(coord_syst, BorderLayout.SOUTH);
        this.add(cat_panel, c);
        cat_panel = new JPanel();
        cat_panel.setPreferredSize(new Dimension(450, 130));
        cat_panel.setBackground(SaadaDBAdmin.beige_color);
        cat_panel.setBorder(BorderFactory.createTitledBorder("GLobal Spectral Unit"));
        cat_panel.setLayout(new BorderLayout());
        cat_panel.add(getFixedText("Select the spectal units used at collection level\nAll ingested spectra will be expressed with this units in addition with their native units"), BorderLayout.PAGE_START);
        JPanel btpn = new JPanel();
        btpn.setBackground(SaadaDBAdmin.beige_color);
        ButtonGroup bg = new ButtonGroup();
        ActionListener al = new SpectCoordActionListener();
        for (int i = 0; i < channel_button.length; i++) {
            btpn.add(channel_button[i]);
            channel_button[i].addActionListener(al);
            channel_button[i].setBackground(SaadaDBAdmin.beige_color);
            bg.add(channel_button[i]);
        }
        channel_button[0].setSelected(true);
        cat_panel.add(btpn, BorderLayout.LINE_START);
        cat_panel.add(spec_syst, BorderLayout.PAGE_END);
        spec_syst.setVisible(false);
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.BOTH;
        this.add(cat_panel, c);
        this.updateUI();
    }

    /**
	 * Returns true if the values set in this panel are OK
	 * @return
	 */
    protected boolean validSysAndUnitPanel() {
        String filename = ((NewSaadaDBTool) (frame)).saada_home + Database.getSepar() + "config" + Database.getSepar() + "saadadb.xml";
        Messenger.printMsg(Messenger.TRACE, "Save SaadaDB config in <" + filename + ">");
        try {
            String dbname = saadadb_name.getText().trim();
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            bw.write("<?xml version=\"1.0\" encoding=\"iso-8859-1\" standalone=\"no\"?>\n");
            bw.write("<!DOCTYPE saadadb SYSTEM \"saadadb.dtd\">\n");
            bw.write("<saadadb>\n");
            bw.write("    <database>\n");
            bw.write("        <name><![CDATA[" + dbname + "]]></name>\n");
            bw.write("        <description><![CDATA[]]></description>\n");
            bw.write("        <root_dir><![CDATA[" + (new File(saadadb_home.getText().trim(), dbname)).getAbsolutePath() + "]]></root_dir>\n");
            bw.write("        <repository_dir><![CDATA[" + (new File(saadadb_rep.getText().trim(), dbname)).getAbsolutePath() + "]]></repository_dir>\n");
            bw.write("    </database>\n");
            bw.write("    <relational_database>\n");
            bw.write("        <name><![CDATA[" + dbms_database_name.getText().trim() + "]]></name>\n");
            bw.write("        <administrator>\n");
            bw.write("            <name><![CDATA[" + dbms_admin.getText().trim() + "]]></name>\n");
            bw.write("        </administrator>\n");
            bw.write("        <reader>\n");
            bw.write("            <name><![CDATA[" + dbms_reader.getText().trim() + "]]></name>\n");
            bw.write("            <password><![CDATA[" + new String(dbms_reader_passwd.getPassword()) + "]]></password>\n");
            bw.write("        </reader>\n");
            if (dbmswrapper == null) {
                JOptionPane.showMessageDialog(frame, "DBMS wrapper not set, please fill all panels", "Configuration Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            bw.write("        <jdbc_driver>" + dbmswrapper.getDriver() + "</jdbc_driver>\n");
            bw.write("        <jdbc_url>" + dbmswrapper.getJdbcURL((new File(saadadb_rep.getText().trim(), dbname)).getAbsolutePath(), dbms_database_name.getText().trim()) + "</jdbc_url>\n");
            bw.write("    </relational_database>\n");
            bw.write("    <web_interface>\n");
            bw.write("        <webapp_home><![CDATA[" + (new File(tomcat_home.getText().trim())).getAbsolutePath() + "]]></webapp_home>\n");
            bw.write("        <url_root><![CDATA[" + url_root.getText().trim() + "]]></url_root>\n");
            bw.write("    </web_interface>\n");
            bw.write("    <spectral_coordinate>\n");
            for (int i = 0; i < channel_button.length; i++) {
                if (channel_button[i].isSelected()) {
                    String spsys = "channel";
                    if (spec_syst != null && spec_syst.getSelectedItem() != null) {
                        spsys = spec_syst.getSelectedItem().toString();
                    }
                    bw.write("        <abscisse type=\"" + channel_button[i].getText().toUpperCase() + "\" unit=\"" + spsys + "\"/>\n");
                    break;
                }
            }
            bw.write("    </spectral_coordinate>\n");
            bw.write("    <coordinate_system>\n");
            String[] se = coord_syst.getSelectedItem().toString().split(",");
            bw.write("        <system>" + se[0] + "</system>\n");
            if (se.length == 2) {
                bw.write("        <equinox>" + se[1] + "</equinox>\n");
            } else {
                bw.write("        <equinox></equinox>\n");
            }
            bw.write("    </coordinate_system>\n");
            bw.write("</saadadb>\n");
            bw.close();
        } catch (IOException e) {
            Messenger.printStackTrace(e);
            JOptionPane.showMessageDialog(frame, "Writing saadadb confif file: " + e.getMessage(), "Configuration Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private static JLabel getPlainLabel(String txt) {
        JLabel retour = new JLabel(txt);
        retour.setFont(SaadaDBAdmin.plain_font);
        return retour;
    }

    private JTextArea getFixedText(String txt) {
        JTextArea jta = new JTextArea(3, 40);
        jta.setEditable(false);
        jta.setLineWrap(true);
        jta.setWrapStyleWord(true);
        jta.setText(txt);
        return jta;
    }
}
