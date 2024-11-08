package ossobook2010.gui.components.content.admin;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ossobook2010.Messages;
import ossobook2010.exceptions.NoRightException;
import ossobook2010.exceptions.NotConnectedException;
import ossobook2010.exceptions.NotLoadedException;
import ossobook2010.exceptions.StatementNotExecutedException;
import ossobook2010.gui.MainFrame;
import ossobook2010.gui.components.content.Content;
import ossobook2010.gui.frameworks.GridBagHelper;
import ossobook2010.gui.frameworks.MultiLineTextLabel;
import ossobook2010.helpers.metainfo.UserRight;
import ossobook2010.querys.IUserManager.Right;

/**
 * The main screen of the artefacts screen.
 * 
 * Display and holds all input fields which are available for the
 * artefacts, but hides all input fields which are disabled in the
 * settings of OssoBook.
 * 
 * Supports several modes:
 * - New mode allows to add a new artefact element to the database.
 *   All input fields are set to standard.
 * - Edit mode allows to display and edit an existing artefact out of the database.
 * - Read mode allows to display an existing artefact out of the database.
 * 
 * This class is very similar to ossobook2010.gui.components.content.entry.Entry
 * 
 * @author Daniel Kaltenthaler
 */
public class Admin extends JPanel implements ActionListener {

    /** The neccessary serial version ID. */
    private static final long serialVersionUID = 60142828781146529L;

    /** The JPanel holding all the content. */
    private JPanel content;

    /** An ArrayList holding the data of all available users. */
    private ArrayList<UserRight> data;

    /** Holds all JCheckBoxes for the reading right status. */
    private ArrayList<JCheckBox> containterRead;

    /** Holds all JCheckBoxes for the writing right status. */
    private ArrayList<JCheckBox> containterWrite;

    /** Holds all JCheckBoxes for the admin right status. */
    private ArrayList<JCheckBox> containterAdmin;

    /** The basic mainFrame object. */
    private MainFrame mainFrame;

    /** The logger object. */
    private static final Log log = LogFactory.getLog(Admin.class);

    /**
     * Constructor of the Admin class.
     * 
     * @param mainFrame
	 *		The basic MainFrame object.
     */
    public Admin(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        JScrollPane sp = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(BorderFactory.createEmptyBorder());
        setBorder(new EmptyBorder(5, 5, 5, 5));
        add(BorderLayout.NORTH, new MultiLineTextLabel(Messages.getString("ADMIN>RIGHT_DESCRIPTION")));
        containterAdmin = new ArrayList<JCheckBox>();
        containterRead = new ArrayList<JCheckBox>();
        containterWrite = new ArrayList<JCheckBox>();
        try {
            data = mainFrame.getController().getAllUserRights();
        } catch (StatementNotExecutedException ex) {
            log.error(ex);
            mainFrame.displayError("StatementNotExecutedException");
            data = new ArrayList<UserRight>();
        } catch (NotConnectedException ex) {
            log.error(ex);
            mainFrame.displayError(Messages.getString("NO_USER_LOGGED_IN"));
            mainFrame.reloadGui(Content.Id.LOGIN);
        } catch (NoRightException ex) {
            log.error(ex);
            mainFrame.displayError(Messages.getString("NO_RIGHT_ERROR_OCCURED"));
            data = new ArrayList<UserRight>();
        } catch (NotLoadedException ex) {
            log.error(ex);
            mainFrame.displayError("NotConnectedException");
        }
        JPanel content2 = new JPanel();
        content = new JPanel(new GridBagLayout());
        content2.add(content);
        createFormular();
        sp.setViewportView(content2);
        add(BorderLayout.CENTER, sp);
        JPanel buttonPanel = new JPanel();
        JButton buttonSave = new JButton(Messages.getString("SAVE"));
        buttonSave.setActionCommand("button_save");
        buttonSave.addActionListener(this);
        buttonPanel.add(buttonSave);
        JButton buttonReset = new JButton(Messages.getString("RESET"));
        buttonReset.setActionCommand("button_reset");
        buttonReset.addActionListener(this);
        buttonPanel.add(buttonReset);
        add(BorderLayout.SOUTH, buttonPanel);
    }

    /**
	 * Inititalises and creates the admin formular holding all the users and the correspondenting checkboxes.
	 */
    private void createFormular() {
        boolean userIsAdmin = false;
        try {
            userIsAdmin = mainFrame.getController().isAdmin();
        } catch (StatementNotExecutedException ex) {
            Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NotConnectedException ex) {
            Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
        }
        GridBagConstraints gbc = new GridBagConstraints();
        JPanel userTitlePanel = new JPanel(new BorderLayout());
        JLabel labName = new JLabel("<html><b>Username</b></html>");
        labName.setBorder(new EmptyBorder(5, 5, 5, 5));
        userTitlePanel.add(BorderLayout.EAST, labName);
        userTitlePanel.setPreferredSize(new Dimension(180, 10));
        userTitlePanel.setBorder(new EmptyBorder(0, 0, 0, 10));
        GridBagHelper.setConstraints(gbc, 0, 0);
        content.add(userTitlePanel, gbc);
        GridBagHelper.setConstraints(gbc, 0, 1);
        JLabel labRead = new JLabel("<html><b>Lesen</b></html>");
        labRead.setBorder(new EmptyBorder(5, 5, 5, 5));
        content.add(labRead, gbc);
        GridBagHelper.setConstraints(gbc, 0, 2);
        JLabel labWrite = new JLabel("<html><b>Schreiben</b></html>");
        labWrite.setBorder(new EmptyBorder(5, 5, 5, 5));
        content.add(labWrite, gbc);
        if (userIsAdmin) {
            GridBagHelper.setConstraints(gbc, 0, 3);
            JLabel labAdmin = new JLabel("<html><b>Admin</b></html>");
            labAdmin.setBorder(new EmptyBorder(5, 5, 5, 5));
            content.add(labAdmin, gbc);
        }
        for (int i = 1; i <= data.size(); i++) {
            String userName = data.get(i - 1).getName();
            Right right = data.get(i - 1).getRight();
            boolean isAdmin = data.get(i - 1).isAdmin();
            JPanel userPanel = new JPanel(new BorderLayout());
            userPanel.add(BorderLayout.EAST, new JLabel(userName));
            userPanel.setPreferredSize(new Dimension(180, 10));
            userPanel.setBorder(new EmptyBorder(0, 0, 0, 10));
            GridBagHelper.setConstraints(gbc, i, 0);
            content.add(userPanel, gbc);
            GridBagHelper.setConstraints(gbc, i, 1);
            JCheckBox readCheck = new JCheckBox();
            readCheck.setActionCommand("READ_" + userName);
            if (right == Right.WRITE || right == Right.WRITE) {
                readCheck.setSelected(true);
            }
            readCheck.addActionListener(this);
            containterRead.add(readCheck);
            content.add(readCheck, gbc);
            GridBagHelper.setConstraints(gbc, i, 2);
            JCheckBox writeCheck = new JCheckBox();
            writeCheck.setActionCommand("WRITE_" + userName);
            if (right == Right.WRITE) {
                writeCheck.setSelected(true);
            }
            writeCheck.addActionListener(this);
            containterWrite.add(writeCheck);
            content.add(writeCheck, gbc);
            if (userIsAdmin) {
                GridBagHelper.setConstraints(gbc, i, 3);
                JCheckBox adminCheck = new JCheckBox();
                if (isAdmin) {
                    adminCheck.setSelected(true);
                }
                adminCheck.setActionCommand("ADMIN_" + userName);
                adminCheck.addActionListener(this);
                containterAdmin.add(adminCheck);
                content.add(adminCheck, gbc);
            }
        }
    }

    /**
	 * Set the reading right of a specific user to a new value.
	 *
	 * @param userName
	 *		The name of the user which status should be changed.
	 * @param value
	 *		<code>true</code> for set to reading right, else <code>false</code>
	 */
    private void setReadRight(String userName, boolean value) {
        for (JCheckBox check : containterRead) {
            if (check.getActionCommand().replaceFirst("READ_", "").equals(userName)) {
                check.setSelected(value);
                return;
            } else {
                continue;
            }
        }
    }

    /**
	 * Returns the reading right of a specific user.
	 *
	 * @param userName
	 *		The name of the user which reading right status should be returned.
	 * @return
	 *		<code>true</code> if the user has reading rights, else <code>false</code>
	 */
    private boolean getReadRight(String userName) {
        for (JCheckBox check : containterRead) {
            if (check.getActionCommand().replaceFirst("READ_", "").equals(userName)) {
                return check.isSelected();
            } else {
                continue;
            }
        }
        return false;
    }

    /**
	 * Set the writing right of a specific user to a new value.
	 *
	 * @param userName
	 *		The name of the user which status should be changed.
	 * @param value
	 *		<code>true</code> for set to writing right, else <code>false</code>
	 */
    private void setWriteRight(String userName, boolean value) {
        for (JCheckBox check : containterWrite) {
            if (check.getActionCommand().replaceFirst("WRITE_", "").equals(userName)) {
                check.setSelected(value);
                return;
            } else {
                continue;
            }
        }
    }

    /**
	 * Returns the writing right of a specific user.
	 *
	 * @param userName
	 *		The name of the user which writing right status should be returned.
	 * @return
	 *		<code>true</code> if the user has writing rights, else <code>false</code>
	 */
    private boolean getWriteRight(String userName) {
        for (JCheckBox check : containterWrite) {
            if (check.getActionCommand().replaceFirst("WRITE_", "").equals(userName)) {
                return check.isSelected();
            } else {
                continue;
            }
        }
        return false;
    }

    /**
	 * Set the admin right of a specific user to a new value.
	 *
	 * @param userName
	 *		The name of the user which status should be changed.
	 * @param value
	 *		<code>true</code> for set to admin right, else <code>false</code>
	 */
    private void setAdminRight(String userName, boolean value) {
        for (JCheckBox check : containterAdmin) {
            if (check.getActionCommand().replaceFirst("ADMIN_", "").equals(userName)) {
                check.setSelected(value);
                return;
            } else {
                continue;
            }
        }
    }

    /**
	 * Returns the admin right of a specific user.
	 *
	 * @param userName
	 *		The name of the user which admin right status should be returned.
	 * @return
	 *		<code>true</code> if the user has admin rights, else <code>false</code>
	 */
    private boolean getAdminRight(String userName) {
        for (JCheckBox check : containterAdmin) {
            if (check.getActionCommand().replaceFirst("ADMIN_", "").equals(userName)) {
                return check.isSelected();
            } else {
                continue;
            }
        }
        return false;
    }

    /**
	 * Saves the data to the database.
	 */
    private void save() {
        ArrayList<UserRight> al = new ArrayList<UserRight>();
        for (UserRight data2 : data) {
            String userName = data2.getName();
            boolean read = getReadRight(userName);
            boolean write = getWriteRight(userName);
            boolean admin = getAdminRight(userName);
            System.out.println(userName + " admin: " + admin);
            Right right;
            if ((read && write) || (!read && write)) {
                right = Right.WRITE;
            } else if (read && !write) {
                right = Right.READ;
            } else {
                right = Right.NORIGHTS;
            }
            al.add(new UserRight(userName, right, admin));
        }
        try {
            if (mainFrame.getController().isAdmin()) mainFrame.getController().saveUserRights(al); else mainFrame.getController().saveUserRightsProject(al);
            mainFrame.displayConfirmation(Messages.getString("CHANGES_WERE_SAVED"));
        } catch (StatementNotExecutedException ex) {
            log.error(ex);
        } catch (NoRightException ex) {
            log.error(ex);
            mainFrame.displayError(Messages.getString("NO_RIGHT_ERROR_OCCURED"));
        } catch (NotLoadedException ex) {
            log.error(ex);
            mainFrame.displayError(Messages.getString("NO_USER_LOGGED_IN"));
            mainFrame.reloadGui(Content.Id.LOGIN);
        } catch (NotConnectedException ex) {
            log.error(ex);
            mainFrame.displayError("NotConnectedException");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();
        if (actionCommand.startsWith("READ_")) {
            String userName = actionCommand.replaceFirst("READ_", "");
            if (!getReadRight(userName)) {
                setWriteRight(userName, false);
            }
        } else if (actionCommand.startsWith("WRITE_")) {
            String userName = actionCommand.replaceFirst("WRITE_", "");
            if (getWriteRight(userName)) {
                setReadRight(userName, true);
            }
        } else if (actionCommand.startsWith("ADMIN_")) {
        } else if (actionCommand.equals("button_reset")) {
            mainFrame.reloadGui(Content.Id.ADMIN);
        } else if (actionCommand.equals("button_save")) {
            save();
        }
    }
}
