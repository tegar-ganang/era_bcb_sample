package com.visitrend.ndvis.sql.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.visitrend.ndvis.io.XMLDocReaderWriter;
import com.visitrend.ndvis.io.XMLTags;
import com.visitrend.ndvis.sql.model.DBDriverProfile;

public class DBDriverDialog extends JDialog implements ActionListener {

    public static final int CANCEL = 0;

    public static final int OK = 1;

    private static final String DRIVER_NAME_EXAMPLE = "E.g., PostgreSQL";

    private static final String JAR_FILE_EXAMPLE = "E.g., postgresql-8.4-701.jdbc4.jar";

    private static final String CLASS_NAME_EXAMPLE = "E.g., org.postgresql.Driver";

    private static final String URL_PREFIX_EXAMPLE = "E.g., jdbc:postgresql://";

    private static final String DRIVER_NAME_ERROR = "You must supply Driver Name.";

    private static final String JAR_FILE_ERROR = "You must supply a Jar File.";

    private static final String CLASS_NAME_ERROR = "You must supply a Class Name.";

    private static final String URL_PREFIX_ERROR = "You must supply a URL Prefix.";

    private static final String DIALOG_TITLE = "New Database Driver";

    private static final String DRIVER_NAME_LABEL = "Name:";

    private static final String JAR_FILE_LABEL = "Jar File:";

    private static final String CLASS_NAME_LABEL = "Class:";

    private static final String URL_PREFIX_LABEL = "URL Prefix:";

    private static final String OK_BUTTON = "OK";

    private static final String CANCEL_BUTTON = "Cancel";

    private static final String BROWSE_BUTTON = "Browse...";

    private static final String PROFILES_FILE_NAME = "data" + File.separator + "DriverProfiles.xml";

    private static List<DBDriverProfile> profileList;

    private static DBDriverDialog instance;

    private int exitValue = 0;

    private JTextField driverNameField, jarFileField, classNameField, urlPrefixField;

    private JLabel driverNameLabel, jarFileLabel, classNameLabel, urlPrefixLabel;

    private JButton okButton, cancelButton, browseButton;

    private JFileChooser fileChooser;

    private DBDriverDialog() {
        this(null);
    }

    private DBDriverDialog(Frame frame) {
        super(frame, DIALOG_TITLE, true);
        if (profileList == null) {
            profileList = new ArrayList<DBDriverProfile>();
            readProfiles();
        }
        createUIElements();
        layoutUIElements();
    }

    private void createUIElements() {
        MyFocusListener fl = new MyFocusListener();
        driverNameLabel = new JLabel(DRIVER_NAME_LABEL);
        jarFileLabel = new JLabel(JAR_FILE_LABEL);
        classNameLabel = new JLabel(CLASS_NAME_LABEL);
        urlPrefixLabel = new JLabel(URL_PREFIX_LABEL);
        driverNameField = new JTextField("", 30);
        driverNameField.addFocusListener(fl);
        driverNameField.setToolTipText(DRIVER_NAME_EXAMPLE);
        jarFileField = new JTextField("", 21);
        jarFileField.addFocusListener(fl);
        jarFileField.setToolTipText(JAR_FILE_EXAMPLE);
        classNameField = new JTextField("", 30);
        classNameField.addFocusListener(fl);
        classNameField.setToolTipText(CLASS_NAME_EXAMPLE);
        urlPrefixField = new JTextField("", 30);
        urlPrefixField.addFocusListener(fl);
        urlPrefixField.setToolTipText(URL_PREFIX_EXAMPLE);
        fileChooser = new JFileChooser();
        okButton = new JButton(OK_BUTTON);
        okButton.addActionListener(this);
        cancelButton = new JButton(CANCEL_BUTTON);
        cancelButton.addActionListener(this);
        browseButton = new JButton(BROWSE_BUTTON);
        browseButton.addActionListener(this);
    }

    private void layoutUIElements() {
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        int row = 0, col = 0;
        c.gridx = col++;
        c.gridy = row;
        c.insets = new Insets(10, 5, 5, 5);
        c.anchor = GridBagConstraints.LINE_END;
        fieldsPanel.add(driverNameLabel, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.gridwidth = 2;
        c.insets = new Insets(10, 0, 5, 5);
        c.ipady = 6;
        c.anchor = GridBagConstraints.LINE_START;
        fieldsPanel.add(driverNameField, c);
        row++;
        col = 0;
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        Insets firstCol = new Insets(0, 5, 5, 5);
        c.insets = firstCol;
        c.anchor = GridBagConstraints.LINE_END;
        fieldsPanel.add(jarFileLabel, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.ipady = 6;
        Insets rest = new Insets(0, 0, 5, 0);
        c.insets = rest;
        fieldsPanel.add(jarFileField, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.insets = rest;
        fieldsPanel.add(browseButton, c);
        row++;
        col = 0;
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.insets = firstCol;
        c.anchor = GridBagConstraints.LINE_END;
        fieldsPanel.add(classNameLabel, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.gridwidth = 2;
        c.insets = rest;
        c.ipady = 6;
        c.anchor = GridBagConstraints.LINE_START;
        fieldsPanel.add(classNameField, c);
        row++;
        col = 0;
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.insets = firstCol;
        c.anchor = GridBagConstraints.LINE_END;
        fieldsPanel.add(urlPrefixLabel, c);
        c = new GridBagConstraints();
        c.gridx = col++;
        c.gridy = row;
        c.gridwidth = 2;
        c.insets = rest;
        c.ipady = 6;
        c.anchor = GridBagConstraints.LINE_START;
        fieldsPanel.add(urlPrefixField, c);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        Container con = this.getContentPane();
        con.add(fieldsPanel, BorderLayout.CENTER);
        con.add(buttonPanel, BorderLayout.SOUTH);
        addWindowListener(new MyWindowListener());
        setResizable(false);
        pack();
    }

    /**
	 * The parent parameter can be null here. This pops up a dialog so the user
	 * can enter the new driver data. Whatever object launches this dialog
	 * will get a reference to this class returned, after calling this method,
	 * and after the user presses OK, cancel, or closes the window. The returned
	 * DBDriver object can then have its getter methods called to get the info
	 * resulting from user interaction.
	 * 
	 * @param parent
	 *            the parent component launching this dialog - this can be null
	 * @param toLoad
	 * 			  the name of the DB Driver Profile to load - this can be null
	 * @return a reference to this object
	 */
    public static DBDriverDialog showDialog(Component parent, String toLoad) {
        Frame frame = JOptionPane.getFrameForComponent(parent);
        if (instance == null) {
            instance = new DBDriverDialog(frame);
        }
        if (toLoad == null) {
            instance.reset();
        } else {
            instance.loadUI(toLoad);
        }
        instance.setVisible(true);
        return instance;
    }

    /**
	 * Get the current DBDriverProfile info from the UI
	 * @return the current DBDriverProfile info from the UI,
	 * or null if the dialog has not been initialized
	 */
    public static DBDriverProfile getDBDriverProfile() {
        if (instance == null) return null;
        DBDriverProfile pro = new DBDriverProfile(instance.getDriverName(), instance.getJarFile(), instance.getJarFilePath(), instance.getClassName(), instance.getUrlPrefix());
        return pro;
    }

    /**
	 * Get the DBDriverProfile corresponding to the given name
	 * @return the DBDriverProfile corresponding to the given name,
	 * or null if the dialog has not been initialized, or a Driver
	 * profile with the given name doesn't exist.
	 */
    public static DBDriverProfile getDBDriverProfile(String name) {
        int index = findDriver(name);
        if (index >= 0) {
            return profileList.get(index);
        } else {
            return null;
        }
    }

    /**
	 * Remove a DB Profile from the list and write the 
	 * changes out to XML
	 * 
	 * @param name The name of the profile to remove
	 * @return true if the profile with the given name was
	 * successfully removed, or false if a profile with the
	 * given name could not be found in the list.
	 */
    public static boolean remove(String name) {
        int index = findDriver(name);
        if (index < 0) return false;
        profileList.remove(index);
        writeProfiles();
        return true;
    }

    private void loadUI(String profileName) {
        DBDriverProfile toLoad = null;
        for (DBDriverProfile pro : profileList) {
            if (pro.getDriverName().equals(profileName)) {
                toLoad = pro;
                break;
            }
        }
        if (toLoad != null) {
            driverNameField.setText(toLoad.getDriverName());
            jarFileField.setText(toLoad.getJarFile());
            fileChooser.setCurrentDirectory(new File(toLoad.getJarFilePath()));
            classNameField.setText(toLoad.getClassName());
            urlPrefixField.setText(toLoad.getUrlPrefix());
        }
    }

    /**
	 * This resets the values that can be retrieved from this object (not any
	 * gui stuff currently).
	 */
    private void reset() {
        driverNameField.setText("");
        jarFileField.setText("");
        fileChooser.setSelectedFile(null);
        classNameField.setText("");
        urlPrefixField.setText("");
    }

    /**
	 * @return Returns the driver name.
	 */
    public String getDriverName() {
        return driverNameField.getText();
    }

    /**
	 * @return a list of driver names
	 */
    public static List<String> getDriverNames() {
        List<String> names = new ArrayList<String>();
        if (profileList == null) {
            readProfiles();
        }
        for (DBDriverProfile pro : profileList) {
            names.add(pro.getDriverName());
        }
        return names;
    }

    /**
	 * @return Returns the jar file.
	 */
    public String getJarFile() {
        return jarFileField.getText();
    }

    /**
	 * @return Returns the jar file path.
	 */
    public String getJarFilePath() {
        if (fileChooser == null) return null;
        File sel = fileChooser.getSelectedFile();
        if (sel == null) return fileChooser.getCurrentDirectory().getPath();
        return sel.getPath();
    }

    /**
	 * @return Returns the class name
	 */
    public String getClassName() {
        return classNameField.getText();
    }

    /**
	 * @return Returns the url prefix.
	 */
    public String getUrlPrefix() {
        return urlPrefixField.getText();
    }

    /**
	 * @return Returns the exitValue.
	 */
    public int getExitValue() {
        return exitValue;
    }

    public void actionPerformed(ActionEvent ae) {
        String msg = ae.getActionCommand();
        if (msg.equals(OK_BUTTON)) {
            okAction();
        } else if (msg.equals(CANCEL_BUTTON)) {
            cancelAction();
        } else if (msg.equals(BROWSE_BUTTON)) {
            chooseFileAction();
        } else {
            throw new IllegalArgumentException("Unknown command in actionPerformed: " + msg);
        }
    }

    private void okAction() {
        if (validateDataEntry()) {
            int conf = JOptionPane.YES_OPTION;
            int overwrite = -1;
            String name = getDriverName();
            overwrite = findDriver(name);
            if (overwrite >= 0) {
                conf = JOptionPane.showConfirmDialog(this, "Overwrite " + name + " driver?", "Driver already exists", JOptionPane.YES_NO_CANCEL_OPTION);
            }
            if (conf == JOptionPane.YES_OPTION) {
                addToList(overwrite);
                writeProfiles();
            }
            if (conf != JOptionPane.CANCEL_OPTION) {
                exitValue = DBDriverDialog.OK;
                setVisible(false);
            }
        }
    }

    /**
	 * Either insert or replace an item in the profile list
	 * 
	 * @param index if less than zero, then add the
	 * profile to the list.  Otherwise replace the profile
	 * at the given index in the list.
	 */
    private void addToList(int index) {
        DBDriverProfile dialogData = new DBDriverProfile(getDriverName(), getJarFile(), getJarFilePath(), getClassName(), getUrlPrefix());
        if (index < 0) {
            profileList.add(dialogData);
        } else {
            profileList.set(index, dialogData);
        }
    }

    /**
	 * Validate that all data fields are filled in and that driver
	 * name is unique.  If any data is missing or invalid, give an
	 * appropriate error message.
	 * 
	 * @return <code>true</code> if everything required is filled in
	 * with valid data, <code>false</code> otherwise
	 */
    private boolean validateDataEntry() {
        if (getDriverName().length() == 0) {
            saveError(DRIVER_NAME_ERROR);
            return false;
        }
        if (getJarFile().length() == 0) {
            saveError(JAR_FILE_ERROR);
            return false;
        }
        if (getClassName().length() == 0) {
            saveError(CLASS_NAME_ERROR);
            return false;
        }
        if (getUrlPrefix().length() == 0) {
            saveError(URL_PREFIX_ERROR);
            return false;
        }
        return true;
    }

    private void saveError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Insufficient Information", JOptionPane.ERROR_MESSAGE);
    }

    /**
	 * Search for a driver with the given name
	 * 
	 * @param name the name to search for
	 * @return the index of the driver with the given name, or -1
	 * if the profile cannot be found
	 */
    private static int findDriver(String name) {
        int found = -1;
        if (profileList == null) {
            readProfiles();
            if (profileList == null) {
                return found;
            }
        }
        for (int i = 0, len = profileList.size(); i < len; i++) {
            DBDriverProfile p = profileList.get(i);
            if (name.equals(p.getDriverName())) {
                found = i;
                break;
            }
        }
        return found;
    }

    private void cancelAction() {
        exitValue = DBDriverDialog.CANCEL;
        setVisible(false);
    }

    private void chooseFileAction() {
        int retVal = fileChooser.showOpenDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            jarFileField.setText(fileChooser.getSelectedFile().getName());
        }
    }

    /**
	 * Simple focus listener that select all text in a field when you tab
	 * between them.
	 * 
	 * @author John T. Langton - jlangton at visitrend dot com
	 * 
	 */
    public class MyFocusListener extends FocusAdapter {

        public void focusGained(FocusEvent arg0) {
            Object o = arg0.getSource();
            if (o instanceof JTextField) {
                ((JTextField) o).selectAll();
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
    public class MyWindowListener extends WindowAdapter {

        public void windowClosing(WindowEvent arg0) {
            cancelAction();
        }
    }

    private static void readProfiles() {
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
        if (e.getTagName().equals(XMLTags.DATABASE_DRIVER_PROFILES)) {
            nodes = e.getChildNodes();
        } else {
            nodes = e.getElementsByTagName(XMLTags.DATABASE_DRIVER_PROFILES);
            Node node = nodes.item(0);
            nodes = node.getChildNodes();
        }
        if (profileList == null) profileList = new ArrayList<DBDriverProfile>();
        for (int k = 0, n = nodes.getLength(); k < n; k++) {
            Node nd = nodes.item(k);
            if (nd.getNodeName().equals(XMLTags.DB_DRIVER_PROFILE)) {
                DBDriverProfile dbdp = DBDriverProfile.readFromXML((Element) nd);
                profileList.add(dbdp);
            }
        }
    }

    private static void writeProfiles() {
        Document doc = null;
        try {
            doc = XMLDocReaderWriter.createXMLFile();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Node n = doc.createElement(XMLTags.DATABASE_DRIVER_PROFILES);
        for (DBDriverProfile profile : profileList) {
            if (profile != null) {
                profile.writeToXML(n);
            }
        }
        doc.appendChild(n);
        XMLDocReaderWriter.writeXMLFile(doc, PROFILES_FILE_NAME);
    }

    public static void main(String[] args) {
        UIManager.put("Button.defaultButtonFollowsFocus", Boolean.TRUE);
        DBDriverDialog log = DBDriverDialog.showDialog(null, null);
        int val = log.getExitValue();
        if (val == DBDriverDialog.OK) {
            System.out.println("OK Button Pressed.");
        } else if (val == DBDriverDialog.CANCEL) {
            System.out.println("Cancel Button Pressed.");
        } else {
            System.err.println("Unknown exit value: " + val);
        }
        System.out.println("  Driver Name: " + log.getDriverName());
        System.out.println("     Jar File: " + log.getJarFile());
        System.out.println("Jar File Path: " + log.getJarFilePath());
        System.out.println("   Class Name: " + log.getClassName());
        System.out.println("   URL Prefix: " + log.getUrlPrefix());
        System.exit(0);
    }
}
