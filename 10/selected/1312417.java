package org.dinopolis.timmon.backend.sql;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.dinopolis.timmon.BaseBackEnd;
import org.dinopolis.timmon.TimmonBackEndException;
import org.dinopolis.timmon.TimmonConstants;
import org.dinopolis.timmon.TimmonFilter;
import org.dinopolis.timmon.TimmonId;
import org.dinopolis.timmon.frontend.treetable.DurationEditor;
import org.dinopolis.timmon.frontend.treetable.TimeEditor;
import org.dinopolis.timmon.frontend.treetable.TreeTableFrontEnd;
import org.dinopolis.timmon.property.Duration;
import org.dinopolis.timmon.property.Period;
import org.dinopolis.timmon.property.Right;
import org.dinopolis.util.Debug;
import org.dinopolis.util.ResourceManager;
import org.dinopolis.util.Resources;
import org.dinopolis.util.gui.ResourceEditorFrame;

/**
 * This is the SQL backand interface usable for all frontends.
 *
 * @author dfreis
 */
public class SQLBackEnd extends BaseBackEnd {

    /** the name of the resource file */
    private static final String RESOURCE_BOUNDLE_NAME = "SQLBackEnd";

    /** the name of the resource file */
    private static final String RESOURCE_DIR_NAME = ".timmon";

    private static final String HOSTNAME_KEY = "db.host";

    private static final String USERNAME_KEY = "db.user";

    private static final String PASSWORD_KEY = "db.password";

    private static final String KEY_DRIVER = "driver";

    private Connection connection_;

    private PreparedStatement sql_add_user_stmt_;

    private PreparedStatement sql_add_user_to_group_stmt_;

    private PreparedStatement sql_delete_user_from_group_stmt_;

    private PreparedStatement sql_add_group_stmt_;

    private PreparedStatement sql_select_groups_stmt_;

    private PreparedStatement sql_select_users_stmt_;

    private PreparedStatement sql_select_users_from_group_stmt_;

    private PreparedStatement sql_get_children_tasks_stmt_;

    private PreparedStatement sql_get_children_activities_stmt_;

    private PreparedStatement sql_get_parent_of_task_stmt_;

    private PreparedStatement sql_get_parent_of_activity_stmt_;

    private PreparedStatement sql_delete_stmt_;

    private PreparedStatement sql_change_activity_parent_stmt_;

    private PreparedStatement sql_change_task_parent_stmt_;

    private PreparedStatement sql_insert_property_stmt_;

    private PreparedStatement sql_change_property_stmt_;

    private PreparedStatement sql_delete_property_stmt_;

    private PreparedStatement sql_get_property_stmt_;

    private PreparedStatement sql_get_properties_stmt_;

    private PreparedStatement sql_create_timmon_id_stmt_;

    private PreparedStatement sql_set_parent_of_task_stmt_;

    private PreparedStatement sql_set_parent_of_activity_stmt_;

    private PreparedStatement sql_get_parent_group_ids_stmt_;

    private PreparedStatement sql_get_global_property_stmt_;

    private PreparedStatement sql_get_global_properties_stmt_;

    private PreparedStatement sql_change_global_property_stmt_;

    private PreparedStatement sql_insert_global_property_stmt_;

    private PreparedStatement sql_delete_global_property_stmt_;

    private PreparedStatement sql_get_all_users_stmt_;

    private PreparedStatement sql_get_children_groups_stmt_;

    private PreparedStatement sql_get_user_id_stmt_;

    private PreparedStatement sql_get_group_params_by_name_stmt_;

    private PreparedStatement sql_get_group_parent_stmt_;

    private int user_id_;

    private static final String SQL_ADD_USER = "insert into users values(?);";

    private static final String SQL_ADD_USER_TO_GROUP = "insert into group_members (id, user_id) values(?, ?);";

    private static final String SQL_DELETE_USER_FROM_GROUP = "delete from group_members where id = ? and user_id = ?;";

    private static final String SQL_ADD_GROUP = "insert into groups (id, parent_id) values(?, ?);";

    public static final String SQL_SELECT_GROUPS = "select groups.id from groups " + "  inner join properties on properties.id=groups.id " + "  where properties.name = \"name\" and properties.value like ? ;";

    public static final String SQL_SELECT_USERS = "select distinct users.id from users " + "  inner join properties on properties.id=users.id " + "  where (properties.name = \"name\" and properties.value like ?) " + "     or (properties.name = \"firstname\" and properties.value like ?) " + "     or (properties.name = \"lastname\" and properties.value like ?);";

    public static final String SQL_SELECT_USERS_FROM_GROUP = "select distinct users.id from users" + "  inner join group_members on group_members.user_id=users.id" + "  inner join properties on properties.id=users.id " + "  where ((properties.name = \"name\" and properties.value like ?) " + "     or (properties.name = \"firstname\" and properties.value like ?) " + "     or (properties.name = \"lastname\" and properties.value like ?)) " + "    and group_members.id=?;";

    private static final String SQL_CHECK_DB = "show tables like 'tasks'";

    private static final String SQL_CREATE_TIMMON_ID = "insert into id values()";

    private static final String SQL_CHANGE_ACTIVITY_PARENT = "update activities set parent_id = ? where id = ?";

    private static final String SQL_CHANGE_TASK_PARENT = "update tasks set parent_id = ? where id = ?";

    private static final String SQL_DELETE = "delete from id where id = ?";

    private static final String SQL_GET_CHILDREN_TASK = "select id from tasks where parent_id = ?";

    private static final String SQL_GET_CHILDREN_ACTIVITY = "select id from activities where parent_id = ?";

    private static final String SQL_GET_PARENT_OF_TASK = "select parent_id from tasks where id = ?";

    private static final String SQL_SET_PARENT_OF_TASK = "insert into tasks values(?, ?, ?)";

    private static final String SQL_GET_PARENT_OF_ACTIVITY = "select parent_id from activities where id = ?";

    private static final String SQL_SET_PARENT_OF_ACTIVITY = "insert into activities values(?, ?, ?)";

    private static final String SQL_INSERT_PROPERTY = "insert into properties values(?,?,?)";

    private static final String SQL_CHANGE_PROPERTY = "update properties set name = ?, value = ? where id = ? and name = ?";

    private static final String SQL_DELETE_PROPERTY = "delete from properties where id = ? and name = ?";

    private static final String SQL_GET_PROPERTY = "select value from properties where id = ? and name = ?";

    private static final String SQL_GET_PROPERTIES = "select name,value from properties where id = ?";

    private static final String SQL_GET_PARENT_GROUP_IDS = "select timmon.groups.id " + "  from timmon.groups " + "  inner join timmon.group_members on timmon.group_members.id=timmon.groups.id " + " where timmon.group_members.user_id = ? ";

    private static final String SQL_GET_GLOBAL_PROPERTY = "select value from global_properties where name = ?";

    private static final String SQL_GET_GLOBAL_PROPERTIES = "select key,value from global_properties";

    private static final String SQL_CHANGE_GLOBAL_PROPERTY = "update global_properties set value = ? where name = ?";

    private static final String SQL_INSERT_GLOBAL_PROPERTY = "insert into global_properties values(?,?)";

    private static final String SQL_DELETE_GLOBAL_PROPERTY = "delete from global_properties where name = ?";

    private static final String SQL_GET_ALL_USERS = "select id from timmon.users;";

    private static final String SQL_GET_CHILDREN_GROUPS = "select id from timmon.groups where parent_id=?;";

    private static final String SQL_GET_USER_ID = "select users.id from users " + "  inner join properties on properties.id=users.id " + "  where properties.name = \"name\" and properties.value = ?";

    private static final String SQL_GET_GROUP_PARAMS_BY_NAME = "select groups.id,groups.parent_id from groups " + "  inner join properties on properties.id=groups.id " + "  where properties.name = \"name\" and properties.value = ?";

    private static final String SQL_GET_GROUP_PARENT = "select parent_id from groups where id = ?;";

    public static final String DEFAULT_RIGHTS = "r[a[g[{0}]u[{1}]]d[g[]u[]]]" + "w[a[g[]u[{1}]]d[g[]u[]]]" + "e[a[g[]u[{1}]]d[g[]u[]]];";

    /**
 * The default constructor
 */
    public SQLBackEnd() throws TimmonBackEndException {
        super();
        Resources resources = ResourceManager.getResources(TreeTableFrontEnd.class, TreeTableFrontEnd.RESOURCE_BOUNDLE_NAME, TreeTableFrontEnd.RESOURCE_DIR_NAME, Locale.getDefault());
        String[] supported = resources.getStringArray("property_editor.displayable_keys", (String[]) null);
        boolean found = false;
        if (supported != null) {
            for (int count = 0; count < supported.length; count++) {
                if (PROP_KEY_RIGHT.equals(supported[count])) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                String[] new_supported = new String[supported.length + 1];
                System.arraycopy(supported, 0, new_supported, 0, supported.length);
                new_supported[supported.length] = PROP_KEY_RIGHT;
                resources.setStringArray("property_editor.displayable_keys.store", supported);
                resources.setStringArray("property_editor.displayable_keys", new_supported);
            }
        }
    }

    public void doInit() throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        try {
            initDriver();
            if (!dbExists()) {
                setupDB();
            }
        } catch (SQLException exc) {
            exc.printStackTrace();
            throw new TimmonBackEndException(exc);
        } catch (ClassNotFoundException exc) {
            throw new TimmonBackEndException(exc);
        }
    }

    /**
   * 
   */
    private void setupDB() {
        StreamTokenizer st;
        try {
            st = new StreamTokenizer(new InputStreamReader(resources_.getURL("admin.create_db.filename").openStream()));
            st.resetSyntax();
            st.eolIsSignificant(false);
            st.wordChars(32, 58);
            st.wordChars(60, 127);
            st.whitespaceChars(0, 31);
            st.whitespaceChars(128, 255);
            st.ordinaryChar(59);
            st.commentChar('#');
            StringBuffer buffer = new StringBuffer();
            while (st.nextToken() != StreamTokenizer.TT_EOF) {
                switch(st.ttype) {
                    case StreamTokenizer.TT_EOL:
                        throw new IllegalStateException("no eol allowed");
                    case StreamTokenizer.TT_NUMBER:
                        throw new IllegalStateException("no numbers allowed");
                    case StreamTokenizer.TT_WORD:
                        if (buffer.length() > 0) buffer.append('\n');
                        buffer.append(st.sval);
                        break;
                    default:
                        {
                            connection_.createStatement().execute(buffer.toString());
                        }
                        buffer.setLength(0);
                }
            }
        } catch (FileNotFoundException exc) {
            if (Debug.DEBUG) Debug.println("exc", Debug.getStackTrace(exc));
            JOptionPane.showMessageDialog(null, exc.getMessage());
        } catch (IOException exc) {
            if (Debug.DEBUG) Debug.println("exc", Debug.getStackTrace(exc));
            JOptionPane.showMessageDialog(null, exc.getMessage());
        } catch (SQLException exc) {
            if (Debug.DEBUG) Debug.println("exc", Debug.getStackTrace(exc));
            JOptionPane.showMessageDialog(null, exc.getMessage());
        }
    }

    /**
   * @return
   */
    private boolean dbExists() {
        try {
            ResultSet rs = connection_.createStatement().executeQuery(SQL_CHECK_DB);
            if (rs == null) return (false);
            return (rs.next());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean doShowSetup() {
        ResourceEditorFrame resource_editor = new ResourceEditorFrame(resources_, RESOURCE_DIR_NAME);
        resource_editor.registerEditor(Duration.class, DurationEditor.class);
        resource_editor.registerEditor(Date.class, TimeEditor.class);
        resource_editor.setVisible(true);
        return (resource_editor.returnedOK());
    }

    public boolean doShowAdmin() throws SecurityException, RemoteException, TimmonBackEndException {
        if (!logged_in_ && !showLoginDialog()) return (false);
        String old_username = null;
        String old_password = null;
        int retry_count = 0;
        while (!doCheckRight(TimmonConstants.RIGHTS_ADMIN)) {
            old_username = getUsername();
            old_password = getPassword();
            try {
                logout();
            } catch (Exception e) {
                if (Debug.DEBUG) Debug.println("exc", Debug.getStackTrace(e));
            }
            JOptionPane.showMessageDialog(null, resources_.getString("admin.not_admin_user.text", "The user you logged on is not an admin user"), resources_.getString("admin.not_admin_user.title", "No Admin User"), JOptionPane.ERROR_MESSAGE);
            if (showLoginDialog()) break;
            if (retry_count >= 3) break;
            retry_count++;
        }
        if (retry_count < 3) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            JButton user_admin = new JButton(resources_.getString("admin.users.title", "Admin Users..."));
            user_admin.setAlignmentX(JButton.CENTER_ALIGNMENT);
            user_admin.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        JDialog dlg = new SQLUserAdmin(null, SQLBackEnd.this);
                        dlg.setVisible(true);
                    } catch (Exception exc) {
                        if (Debug.DEBUG) Debug.println("exc", Debug.getStackTrace(exc));
                        JOptionPane.showMessageDialog(null, exc.getMessage());
                    }
                }
            });
            panel.add(user_admin);
            panel.add(Box.createGlue());
            JButton group_admin = new JButton(resources_.getString("admin.groups.title", "Admin Groups..."));
            group_admin.setAlignmentX(JButton.CENTER_ALIGNMENT);
            group_admin.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        JDialog dlg = new SQLGroupAdmin(null, SQLBackEnd.this);
                        dlg.setVisible(true);
                    } catch (Exception exc) {
                        if (Debug.DEBUG) Debug.println("exc", Debug.getStackTrace(exc));
                        JOptionPane.showMessageDialog(null, exc.getMessage());
                    }
                }
            });
            panel.add(group_admin);
            final JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE);
            final JDialog dialog = new JDialog((Frame) null, resources_.getString("admin.main.title", "SQL Admin"));
            final Object lock = new Object();
            dialog.setContentPane(optionPane);
            dialog.pack();
            dialog.setVisible(true);
            optionPane.addPropertyChangeListener(new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent e) {
                    String prop = e.getPropertyName();
                    if (dialog.isVisible() && (e.getSource() == optionPane) && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                        dialog.setVisible(false);
                        synchronized (lock) {
                            lock.notifyAll();
                        }
                    }
                }
            });
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e1) {
                }
            }
        }
        try {
            if ((old_username == null) || (old_password == null)) {
                logout();
                return (true);
            }
            try {
                login(old_username, old_password);
            } catch (Exception exc) {
                logout();
                JOptionPane.showMessageDialog(null, "Could not logout as system user, user is now anonymous!", "Login Warning", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception exc) {
            if (Debug.DEBUG) Debug.println("Admin Error", exc + Debug.getStackTrace(exc));
            JOptionPane.showMessageDialog(null, exc.getMessage(), "Admin Error", JOptionPane.ERROR_MESSAGE);
        }
        return (true);
    }

    /**
   * 
   */
    private boolean showLoginDialog() {
        JTextField uname = new JTextField();
        JPasswordField password = new JPasswordField();
        int option = JOptionPane.showOptionDialog(null, new Object[] { resources_.getString("login.name.label", "Name"), uname, resources_.getString("login.password.label", "Password"), password }, resources_.getString("login.title", "Login"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
        if ((option == JOptionPane.CLOSED_OPTION) || (option == JOptionPane.CANCEL_OPTION)) {
            return (false);
        }
        try {
            login(uname.getText(), new String(password.getPassword()), (BACKEND_ACCESS_MODE_READ | BACKEND_ACCESS_MODE_WRITE | BACKEND_ACCESS_MODE_FORCE));
            return (true);
        } catch (Exception e) {
            if (Debug.DEBUG) Debug.println("exc", e + Debug.getStackTrace(e));
        }
        JOptionPane.showMessageDialog(null, resources_.getString("admin.invalid_username_or_password.text", "Invalid username or password"), resources_.getString("admin.invalid_username_or_password.title", "Login Error"), JOptionPane.ERROR_MESSAGE);
        return (showLoginDialog());
    }

    /**
  * Loads the resource file, or exits on a MissingResourceException.
  */
    protected Resources doLoadResources() throws MissingResourceException {
        return (ResourceManager.getResources(SQLBackEnd.class, RESOURCE_BOUNDLE_NAME, RESOURCE_DIR_NAME, Locale.getDefault()));
    }

    /** 
* Registers oracle sql driver
* 
* @return void
* @exception SQLException if an sql error occures
*/
    private void initDriver() throws SQLException, ClassNotFoundException {
        String driver_name = resources_.getString(KEY_DRIVER, null);
        if (driver_name != null) Class.forName(driver_name);
        connection_ = DriverManager.getConnection(resources_.getString(HOSTNAME_KEY), resources_.getString(USERNAME_KEY), resources_.getString(PASSWORD_KEY));
        sql_add_user_stmt_ = connection_.prepareStatement(SQL_ADD_USER);
        sql_add_user_to_group_stmt_ = connection_.prepareStatement(SQL_ADD_USER_TO_GROUP);
        sql_delete_user_from_group_stmt_ = connection_.prepareStatement(SQL_DELETE_USER_FROM_GROUP);
        sql_add_group_stmt_ = connection_.prepareStatement(SQL_ADD_GROUP);
        sql_select_groups_stmt_ = connection_.prepareStatement(SQL_SELECT_GROUPS);
        sql_select_users_stmt_ = connection_.prepareStatement(SQL_SELECT_USERS);
        sql_select_users_from_group_stmt_ = connection_.prepareStatement(SQL_SELECT_USERS_FROM_GROUP);
        sql_create_timmon_id_stmt_ = connection_.prepareStatement(SQL_CREATE_TIMMON_ID, Statement.RETURN_GENERATED_KEYS);
        sql_change_activity_parent_stmt_ = connection_.prepareStatement(SQL_CHANGE_ACTIVITY_PARENT);
        sql_change_task_parent_stmt_ = connection_.prepareStatement(SQL_CHANGE_TASK_PARENT);
        sql_delete_stmt_ = connection_.prepareStatement(SQL_DELETE);
        sql_get_children_tasks_stmt_ = connection_.prepareStatement(SQL_GET_CHILDREN_TASK);
        sql_get_children_activities_stmt_ = connection_.prepareStatement(SQL_GET_CHILDREN_ACTIVITY);
        sql_get_parent_of_task_stmt_ = connection_.prepareStatement(SQL_GET_PARENT_OF_TASK);
        sql_get_parent_of_activity_stmt_ = connection_.prepareStatement(SQL_GET_PARENT_OF_ACTIVITY);
        sql_set_parent_of_task_stmt_ = connection_.prepareStatement(SQL_SET_PARENT_OF_TASK);
        sql_set_parent_of_activity_stmt_ = connection_.prepareStatement(SQL_SET_PARENT_OF_ACTIVITY);
        sql_insert_property_stmt_ = connection_.prepareStatement(SQL_INSERT_PROPERTY);
        sql_change_property_stmt_ = connection_.prepareStatement(SQL_CHANGE_PROPERTY);
        sql_delete_property_stmt_ = connection_.prepareStatement(SQL_DELETE_PROPERTY);
        sql_get_property_stmt_ = connection_.prepareStatement(SQL_GET_PROPERTY);
        sql_get_properties_stmt_ = connection_.prepareStatement(SQL_GET_PROPERTIES);
        sql_get_parent_group_ids_stmt_ = connection_.prepareStatement(SQL_GET_PARENT_GROUP_IDS);
        sql_get_global_property_stmt_ = connection_.prepareStatement(SQL_GET_GLOBAL_PROPERTY);
        sql_get_global_properties_stmt_ = connection_.prepareStatement(SQL_GET_GLOBAL_PROPERTIES);
        sql_change_global_property_stmt_ = connection_.prepareStatement(SQL_CHANGE_GLOBAL_PROPERTY);
        sql_insert_global_property_stmt_ = connection_.prepareStatement(SQL_INSERT_GLOBAL_PROPERTY);
        sql_delete_global_property_stmt_ = connection_.prepareStatement(SQL_DELETE_GLOBAL_PROPERTY);
        sql_get_user_id_stmt_ = connection_.prepareStatement(SQL_GET_USER_ID);
        sql_get_group_params_by_name_stmt_ = connection_.prepareStatement(SQL_GET_GROUP_PARAMS_BY_NAME);
        sql_get_group_parent_stmt_ = connection_.prepareStatement(SQL_GET_GROUP_PARENT);
        sql_get_all_users_stmt_ = connection_.prepareStatement(SQL_GET_ALL_USERS);
        sql_get_children_groups_stmt_ = connection_.prepareStatement(SQL_GET_CHILDREN_GROUPS);
    }

    /**
 * Login the backend for the given username/password.
 *
 * @param username the user's name. 
 * @param password the user's password.
 * @param mode the access mode
 *
 * the mode's values can be a combination of 
 * TimmonConstants.BACKEND_ACCESS_MODE_READ, 
 * TimmonConstants.BACKEND_ACCESS_MODE_WRITE and
 * TimmonConstants.BACKEND_ACCESS_MODE_CREATE_NEW
 *
 * @exception SecurityException in case of unknown user or wrong
 * password.
 * @exception RemoteException if a network error occurres.
 * @exception IllegalArgumentException if username or password is
 * <code>null</code>
 * @exception TimmonBackEndException any exception that is thrown by the BackEnd
 * (Database access, File access, ...).  
 */
    public void doLogin(String username, String password, int mode) throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        try {
            int user_id = getUserId(username, password);
            if (((mode & BACKEND_ACCESS_MODE_WRITE) > 0) && (mode & BACKEND_ACCESS_MODE_FORCE) == 0) {
                if (getLoginState(user_id)) throw (new TimmonBackEndException("no write access possible (another Timmon for user " + username + " is currently running)"));
            }
            setLoginState(user_id, true);
            user_id_ = user_id;
        } catch (SQLException exc) {
            if (Debug.DEBUG) Debug.println("exc", Debug.getStackTrace(exc));
            throw (new TimmonBackEndException(exc));
        }
    }

    /**
 * Logout the backend
 *
 * @exception SecurityException in case of user not logged in
 * @exception RemoteException if a network error occurres.
 * @exception TimmonBackEndException any exception that is thrown by the BackEnd
 * (Database access, File access, ...).  
 */
    public void doLogout() throws SecurityException, RemoteException, TimmonBackEndException {
        try {
            sql_get_property_stmt_.setInt(1, user_id_);
            sql_get_property_stmt_.setString(2, "online");
            ResultSet rs = sql_get_property_stmt_.executeQuery();
            if (rs.next()) {
                if ("true".equals(rs.getString(1))) {
                    sql_change_property_stmt_.setString(1, "online");
                    sql_change_property_stmt_.setString(2, "false");
                    sql_change_property_stmt_.setInt(3, user_id_);
                    sql_change_property_stmt_.setString(4, "online");
                }
            } else {
                sql_insert_property_stmt_.setInt(1, user_id_);
                sql_insert_property_stmt_.setString(2, "online");
                sql_insert_property_stmt_.setString(3, "true");
                sql_insert_property_stmt_.executeUpdate();
            }
            rs.close();
        } catch (SQLException exc) {
            throw (new TimmonBackEndException(exc));
        }
        user_id_ = -1;
    }

    /**
 * Creates a sub task or log entry for the parent task with the given
 * id and returns the id of the newly created object.
 *
 * @param parent_id the id of the parent task.
 * @param type the type of the object to be created, either
 * <code>TYPE_TASK</code> or <code>TYPE_LOG_ENTRY</code>.
 * @return the id of the created object.
 * @exception RemoteException if a network error occurres.
 * @exception SecurityException if the user does not have the required
 * access rights.
 * @exception IllegalArgumentException if <code>parent_id</code> does
 * not represent a task or <code>type</code> is not valid.  
 * @exception TimmonBackEndException any exception that is thrown by the BackEnd
 * (Database access, File access, ...).  
 */
    public TimmonId doCreate(TimmonId parent_id, int type) throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        boolean commit = false;
        try {
            commit = connection_.getAutoCommit();
            if (commit) connection_.setAutoCommit(false);
        } catch (SQLException exc) {
            throw (new TimmonBackEndException(exc));
        }
        try {
            sql_create_timmon_id_stmt_.executeUpdate();
            ResultSet rs = sql_create_timmon_id_stmt_.getGeneratedKeys();
            int id = -1;
            if (rs.next()) {
                id = rs.getInt(1);
            }
            if (id < 0) throw (new TimmonBackEndException("Could not create id!"));
            SQLTimmonId timmon_id = new SQLTimmonId(id, type);
            if (isTypeOf(TYPE_TASK_MASK, type)) {
                sql_set_parent_of_task_stmt_.setInt(1, id);
                sql_set_parent_of_task_stmt_.setInt(2, user_id_);
                sql_set_parent_of_task_stmt_.setInt(3, (int) ((SQLTimmonId) parent_id).getTimmonId());
                sql_set_parent_of_task_stmt_.executeUpdate();
                createDefaultTaskProperties((SQLTimmonId) parent_id, timmon_id);
            } else if (isTypeOf(TYPE_ACTIVITY_MASK, type)) {
                sql_set_parent_of_activity_stmt_.setInt(1, id);
                sql_set_parent_of_activity_stmt_.setInt(2, user_id_);
                sql_set_parent_of_activity_stmt_.setInt(3, (int) ((SQLTimmonId) parent_id).getTimmonId());
                sql_set_parent_of_activity_stmt_.executeUpdate();
                craeteDefaultActivityProperties((SQLTimmonId) parent_id, timmon_id);
            } else if (isTypeOf(TYPE_USER_MASK, type)) {
                sql_add_user_stmt_.setInt(1, id);
                sql_add_user_stmt_.executeUpdate();
                createDefaultUserProperties(timmon_id);
                SQLTimmonId sql_id = (SQLTimmonId) doGetGlobalProperty(GPROP_KEY_ROOT_GROUP_ID);
                sql_add_user_to_group_stmt_.setInt(1, (int) sql_id.getTimmonId());
                sql_add_user_to_group_stmt_.setInt(2, id);
                sql_add_user_to_group_stmt_.executeUpdate();
                if ((int) sql_id.getTimmonId() != (int) ((SQLTimmonId) parent_id).getTimmonId()) {
                    sql_add_user_to_group_stmt_.setInt(1, (int) ((SQLTimmonId) parent_id).getTimmonId());
                    sql_add_user_to_group_stmt_.setInt(2, id);
                    sql_add_user_to_group_stmt_.executeUpdate();
                }
            } else if (isTypeOf(TYPE_GROUP_MASK, type)) {
                sql_add_group_stmt_.setInt(1, id);
                sql_add_group_stmt_.setInt(2, (int) ((SQLTimmonId) parent_id).getTimmonId());
                sql_add_group_stmt_.executeUpdate();
                createDefaultGroupProperties(timmon_id);
            } else throw (new TimmonBackEndException("unknown type: " + type));
            connection_.commit();
            return (new SQLTimmonId(id, type));
        } catch (SQLException e) {
            if (Debug.DEBUG) Debug.println("exc", e + "\n" + Debug.getStackTrace(e));
            if (commit) {
                try {
                    connection_.rollback();
                } catch (SQLException e1) {
                    throw (new TimmonBackEndException(e1));
                }
            }
            throw (new TimmonBackEndException(e));
        } finally {
            try {
                if (commit) connection_.setAutoCommit(true);
            } catch (SQLException e1) {
            }
        }
    }

    /**
 * @param timmon_id
 */
    private void createDefaultGroupProperties(SQLTimmonId timmon_id) {
    }

    /**
 * @param timmon_id
 * @throws SQLException 
 */
    private void createDefaultUserProperties(SQLTimmonId timmon_id) throws SQLException {
        sql_insert_property_stmt_.setInt(1, (int) ((SQLTimmonId) timmon_id).getTimmonId());
        sql_insert_property_stmt_.setString(2, "online");
        sql_insert_property_stmt_.setString(3, "false");
        sql_insert_property_stmt_.executeUpdate();
    }

    /**
 * @param parent_id
 * @throws TimmonBackEndException 
 * @throws IllegalArgumentException 
 * @throws RemoteException 
 * @throws SecurityException 
 */
    private void craeteDefaultActivityProperties(SQLTimmonId parent_id, SQLTimmonId id) throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        Right right = new Right();
        right.addAllowUser(getUsername(), Right.READ | Right.EDIT);
        try {
            sql_insert_property_stmt_.setInt(1, (int) id.getTimmonId());
            sql_insert_property_stmt_.setString(2, TimmonConstants.PROP_KEY_RIGHT);
            sql_insert_property_stmt_.setString(3, right.format());
            sql_insert_property_stmt_.executeUpdate();
        } catch (SQLException e) {
            throw (new TimmonBackEndException(e));
        }
    }

    /**
 * @param id
 * @throws TimmonBackEndException 
 * @throws IllegalArgumentException 
 * @throws RemoteException 
 * @throws SecurityException 
 */
    private void createDefaultTaskProperties(SQLTimmonId parent_id, SQLTimmonId id) throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        Right right = new Right();
        right.addAllowUser(getUsername(), Right.READ | Right.CREATE | Right.EDIT);
        try {
            sql_insert_property_stmt_.setInt(1, (int) id.getTimmonId());
            sql_insert_property_stmt_.setString(2, TimmonConstants.PROP_KEY_RIGHT);
            sql_insert_property_stmt_.setString(3, right.format());
            sql_insert_property_stmt_.executeUpdate();
        } catch (SQLException e) {
            throw (new TimmonBackEndException(e));
        }
    }

    /**
 * Moves a task or a log entry to the given parent task and returns
 * the id of the moved object.
 *
 * @param souce_id the task or log entry to be moved.
 * @param new_parent_id the id of the new parent.
 * @return the id of the moved object (might stay the same).
 * @exception RemoteException if a network error occurres.
 * @exception SecurityException if the user does not have the required
 * access rights.
 * @exception IllegalArgumentException if <code>souce_id</code> or
 * <code>new_parent_id</code> is not valid.
 * @exception TimmonBackEndException any exception that is thrown by the BackEnd
 * (Database access, File access, ...).  
 */
    public TimmonId doMove(TimmonId source_id, TimmonId new_parent_id) throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        SQLTimmonId sql_source_id = (SQLTimmonId) source_id;
        if (isTypeOf(TYPE_TASK_MASK, sql_source_id.getTimmonIdType())) {
            long new_parent_id_value = ((SQLTimmonId) new_parent_id).getTimmonId();
            try {
                sql_change_task_parent_stmt_.setInt(1, (int) new_parent_id_value);
                sql_change_task_parent_stmt_.setInt(2, (int) sql_source_id.getTimmonId());
                sql_change_task_parent_stmt_.executeUpdate();
                return (sql_source_id);
            } catch (SQLException e) {
                throw (new TimmonBackEndException(e));
            }
        } else if (isTypeOf(TYPE_ACTIVITY_MASK, sql_source_id.getTimmonIdType())) {
            long new_parent_id_value = ((SQLTimmonId) new_parent_id).getTimmonId();
            try {
                sql_change_activity_parent_stmt_.setInt(1, (int) new_parent_id_value);
                sql_change_activity_parent_stmt_.setInt(2, (int) sql_source_id.getTimmonId());
                sql_change_activity_parent_stmt_.executeUpdate();
                return (sql_source_id);
            } catch (SQLException e) {
                throw (new TimmonBackEndException(e));
            }
        }
        throw (new TimmonBackEndException("unknown or usupported type for move: " + sql_source_id.getTimmonIdType()));
    }

    /**
 * Deletes a task or a log entry. If the task has sub tasks, they
 * are deleted recusively.
 *
 * @param id the id of the object to be deleted.
 * @exception RemoteException if a network error occurres.
 * @exception SecurityException if the user does not have the required
 * access rights.
 * @exception IllegalArgumentException if <code>id</code> is not
 * valid.
 * @exception TimmonBackEndException any exception that is thrown by the BackEnd
 * (Database access, File access, ...).  
 */
    public void doDelete(TimmonId id) throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        try {
            sql_delete_stmt_.setInt(1, (int) (((SQLTimmonId) id).getTimmonId()));
            sql_delete_stmt_.executeUpdate();
        } catch (SQLException e) {
            throw (new TimmonBackEndException(e));
        }
    }

    /**
 * Returns the sub tasks or log entries of the given parent task that match the given
 * <code>match_params</code>.
 *
 * @param parent_id the id of the parent task.
 * @param match_params a map containing the match arguments. If
 * <code>null</code> or empty, this method does exactly the same as
 * {@link #getChildrenIds(TimmonId,int)}.
 * @todo FIXXME describe match arguments exactly!
 * @param type the type of the children to request, either
 * <code>TYPE_TASK</code> or <code>TYPE_LOG_ENTRY</code>.
 * @return an array holding the ids of all sub tasks or log entries of
 * the given parent task that matches <code>match_params</code>. If
 * the parent task does not have any children of the given
 * <code>type</code> matching <code>match_params</code>, an empty
 * array is returned.
 * @exception RemoteException if a network error occurres.
 * @exception SecurityException if the user does not have the required
 * access rights.
 * @exception IllegalArgumentException if <code>parent_id</code> or
 * <code>type</code> is not valid.
 * @exception TimmonBackEndException any exception that is thrown by the BackEnd
 * (Database access, File access, ...).  
 */
    public synchronized TimmonId[] doGetChildrenIds(TimmonId parent_id, TimmonFilter filter, int type) throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        SQLTimmonId sql_parent_id = (SQLTimmonId) parent_id;
        Vector tmp = new Vector();
        try {
            if (isTypeOf(type, TYPE_TASK)) {
                ResultSet rs;
                sql_get_children_tasks_stmt_.setInt(1, (int) sql_parent_id.getTimmonId());
                rs = sql_get_children_tasks_stmt_.executeQuery();
                if (rs != null) {
                    SQLTimmonId timmon_id;
                    int id;
                    while (rs.next()) {
                        id = rs.getInt(1);
                        timmon_id = new SQLTimmonId((long) id, TimmonConstants.TYPE_TASK);
                        if (filter == null || filter.accept(this, timmon_id)) {
                            if (doCheckRight(timmon_id, TimmonConstants.RIGHTS_READ)) {
                                if (!doCheckRight(timmon_id, TimmonConstants.RIGHTS_CHANGE_PROPERTIES)) timmon_id.setTimmonIdType(TimmonConstants.TYPE_READ_ONLY_TASK);
                                tmp.addElement(timmon_id);
                            }
                        }
                    }
                    rs.close();
                }
            }
            if (isTypeOf(type, TYPE_ACTIVITY)) {
                ResultSet rs;
                sql_get_children_activities_stmt_.setInt(1, (int) sql_parent_id.getTimmonId());
                rs = sql_get_children_activities_stmt_.executeQuery();
                if (rs != null) {
                    SQLTimmonId timmon_id;
                    int id;
                    while (rs.next()) {
                        id = rs.getInt(1);
                        timmon_id = new SQLTimmonId((long) id, TimmonConstants.TYPE_ACTIVITY);
                        if (filter == null || filter.accept(this, timmon_id)) {
                            if (doCheckRight(timmon_id, TimmonConstants.RIGHTS_READ)) {
                                if (!doCheckRight(timmon_id, TimmonConstants.RIGHTS_CHANGE_PROPERTIES)) timmon_id.setTimmonIdType(TimmonConstants.TYPE_READ_ONLY_ACTIVITY);
                                tmp.addElement(timmon_id);
                            }
                        }
                    }
                    rs.close();
                }
            }
            if (isTypeOf(type, TYPE_USER_ENTRY)) {
                ResultSet rs;
                rs = sql_get_all_users_stmt_.executeQuery();
                if (rs != null) {
                    SQLTimmonId timmon_id;
                    int id;
                    while (rs.next()) {
                        id = rs.getInt(1);
                        timmon_id = new SQLTimmonId((long) id, TimmonConstants.TYPE_USER_ENTRY);
                        if (filter == null || filter.accept(this, timmon_id)) tmp.addElement(timmon_id);
                    }
                    rs.close();
                }
            }
            if (isTypeOf(type, TYPE_GROUP_ENTRY)) {
                ResultSet rs;
                sql_get_children_groups_stmt_.setInt(1, (int) sql_parent_id.getTimmonId());
                rs = sql_get_children_groups_stmt_.executeQuery();
                if (rs != null) {
                    SQLTimmonId timmon_id;
                    int id;
                    while (rs.next()) {
                        id = rs.getInt(1);
                        timmon_id = new SQLTimmonId((long) id, TimmonConstants.TYPE_GROUP_ENTRY);
                        if (filter == null || filter.accept(this, timmon_id)) tmp.addElement(timmon_id);
                    }
                    rs.close();
                }
            }
            SQLTimmonId[] ret = new SQLTimmonId[tmp.size()];
            tmp.toArray(ret);
            return (ret);
        } catch (SQLException e) {
            if (Debug.DEBUG) Debug.println("exc", e + Debug.getStackTrace(e));
            throw (new TimmonBackEndException(e));
        }
    }

    /**
 * Returns the type of the task or log entry that belongs to the given
 * id. See {@link TimmonConstants} for details.
 *
 * @param id the id of the task or log entry.
 * @return the type of the task or log entry.
 * @exception RemoteException if a network error occurres.
 * @exception SecurityException if the user does not have the required
 * access rights.
 * @exception IllegalArgumentException if <code>id</code> is not valid.
 * @exception TimmonBackEndException any exception that is thrown by
 * the BackEnd (Database access, File access, ...).
 */
    public int doGetType(TimmonId id) throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        try {
            return (((SQLTimmonId) id).getTimmonIdType());
        } catch (ClassCastException cce) {
            throw new IllegalArgumentException("id is not valid for this backend.");
        } catch (Exception e) {
            throw new TimmonBackEndException(e);
        }
    }

    /**
 * Returns the id of the parent task of the sub task or log entry or
 * <code>null</code> if the id is the top level task (root)..
 *
 * @param id the id of the task or the log entry.
 * @return the id of the parent task of the sub task or log entry or
 * <code>null</code> if the id is the top level task (root).
 * @exception RemoteException if a network error occurres.
 * @exception SecurityException if the user does not have the required
 * access rights.
 * @exception IllegalArgumentException if <code>id</code> is not valid.
 * @exception TimmonBackEndException any exception that is thrown by
 * the BackEnd (Database access, File access, ...).
 */
    public TimmonId doGetParentId(TimmonId timmon_id) throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        try {
            if (isTypeOf(doGetType(timmon_id), TimmonConstants.TYPE_ACTIVITY)) {
                sql_get_parent_of_activity_stmt_.setInt(1, (int) ((SQLTimmonId) timmon_id).getTimmonId());
                ResultSet rs = sql_get_parent_of_activity_stmt_.executeQuery();
                if ((rs != null) && (rs.next())) {
                    long id = (long) rs.getInt(1);
                    rs.close();
                    return (new SQLTimmonId(id, TimmonConstants.TYPE_TASK));
                }
            } else {
                sql_get_parent_of_task_stmt_.setInt(1, (int) ((SQLTimmonId) timmon_id).getTimmonId());
                ResultSet rs = sql_get_parent_of_task_stmt_.executeQuery();
                if ((rs != null) && (rs.next())) {
                    long id = (long) rs.getInt(1);
                    rs.close();
                    return (new SQLTimmonId(id, TimmonConstants.TYPE_TASK));
                }
            }
        } catch (SQLException e) {
            throw (new TimmonBackEndException(e));
        }
        return null;
    }

    /**
 * Changes a property for the given object (task, log entry, user,
 * ...). If there exists a property with the given key for the given
 * object, the new value replaces the old one. If the value is set to
 * <code>null</code> the property is deleted. If there did not exist
 * a property with the given key, it will be created.
 *
 * @param id the id of the object.
 * @param key the key of the property to be changed.
 * @param value the value of the property to be changed, or
 * <code>null</code> to delete the property.
 * @exception RemoteException if a network error occurres.
 * @exception SecurityException if the user does not have the required
 * access rights.
 * @exception IllegalArgumentException if <code>id</code> is not
 * valid, or the key is <code>null</code>.
 */
    public void doChangeProperty(TimmonId timmon_id, String key, Object value) throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        int id_type = ((SQLTimmonId) timmon_id).getTimmonIdType();
        if (isTypeOf(TYPE_USER_MASK, id_type)) {
            if (key.equals(PROP_KEY_NAME)) {
                if (value == null) throw (new TimmonBackEndException("cannot delete user name!"));
                try {
                    if (getUserId(value.toString()) != ((SQLTimmonId) timmon_id).getTimmonId()) throw (new TimmonBackEndException("user name not unique!"));
                } catch (SecurityException exc) {
                } catch (SQLException e) {
                    throw (new TimmonBackEndException(e));
                }
            }
        }
        if (isTypeOf(TYPE_GROUP_MASK, id_type)) {
            if (key.equals(PROP_KEY_NAME)) {
                if (value == null) throw (new TimmonBackEndException("cannot delete group name!"));
                try {
                    if (getGroupId(value.toString()) != ((SQLTimmonId) timmon_id).getTimmonId()) throw (new TimmonBackEndException("group name not unique!"));
                } catch (SecurityException exc) {
                } catch (SQLException e) {
                    throw (new TimmonBackEndException(e));
                }
            }
        }
        try {
            sqlChangeProperty(timmon_id, key, value);
            if (!doCheckRight(timmon_id, TimmonConstants.RIGHTS_CHANGE_PROPERTIES)) ((SQLTimmonId) timmon_id).setTimmonIdType(id_type | TimmonConstants.TYPE_READ_ONLY_MASK);
        } catch (SQLException exc) {
            if (Debug.DEBUG) Debug.println("exc", exc + Debug.getStackTrace(exc));
            throw (new TimmonBackEndException(exc));
        }
    }

    private void sqlChangeProperty(TimmonId timmon_id, String key, Object value) throws SQLException {
        int id = (int) ((SQLTimmonId) timmon_id).getTimmonId();
        if (value == null) {
            sql_delete_property_stmt_.setInt(1, id);
            sql_delete_property_stmt_.setString(2, key);
            sql_delete_property_stmt_.executeUpdate();
        } else {
            String value_rep;
            if (value instanceof Right) value_rep = ((Right) value).format(); else if (value instanceof Duration) value_rep = ((Duration) value).format(); else if (value instanceof Period) value_rep = ((Period) value).format(); else {
                if (!(value instanceof String)) System.err.println("WARNING unknown value: " + value + " of type: " + value.getClass().getName());
                value_rep = value.toString();
            }
            sql_get_property_stmt_.setInt(1, id);
            sql_get_property_stmt_.setString(2, key);
            ResultSet rs = sql_get_property_stmt_.executeQuery();
            if ((rs != null) && (rs.next())) {
                sql_change_property_stmt_.setString(1, key);
                sql_change_property_stmt_.setString(2, value_rep);
                sql_change_property_stmt_.setInt(3, id);
                sql_change_property_stmt_.setString(4, key);
                rs.close();
                sql_change_property_stmt_.executeUpdate();
            } else {
                sql_insert_property_stmt_.setInt(1, id);
                sql_insert_property_stmt_.setString(2, key);
                sql_insert_property_stmt_.setString(3, value_rep);
                sql_insert_property_stmt_.executeUpdate();
            }
        }
    }

    /**
 * Changes the properties for the given object (task, log entry, user,
 * ...). If there exists a property with one of the given keys, the
 * new value replaces the old one. If the value is set to
 * <code>null</code> the property is deleted. If there did not exist 
 * a property with one of the given keys, it will be created.
 *
 * @param id the id of the object.
 * @param properties the map containing the properties to be changed.
 * @exception RemoteException if a network error occurres.
 * @exception SecurityException if the user does not have the required
 * access rights.
 * @exception IllegalArgumentException if <code>id</code> is not
 * valid, or <code>properties</code> is <code>null</code>.
 */
    public void doChangeProperties(TimmonId id, Map properties) throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        Iterator iter = properties.entrySet().iterator();
        Map.Entry entry;
        while (iter.hasNext()) {
            entry = (Map.Entry) iter.next();
            doChangeProperty(id, (String) entry.getKey(), entry.getValue());
        }
    }

    private String sqlGetStringProperty(int id, String key) throws SQLException {
        sql_get_property_stmt_.setInt(1, id);
        sql_get_property_stmt_.setString(2, key);
        ResultSet rs = sql_get_property_stmt_.executeQuery();
        if ((rs != null) && (rs.next())) return (rs.getString(1));
        return (null);
    }

    /**
 * Returns the value of the property with the given key or
 * <code>null</code>, if the key does not exist within the
 * properties.
 *
 * @param id the id of the object.
 * @param key the key of the property.
 * @return the value of the property, or <code>null</code>, if the
 * key does not exist within the properties.
 * @exception RemoteException if a network error occurres.
 * @exception SecurityException if the user does not have the required
 * access rights.
 * @exception IllegalArgumentException if <code>id</code> is not
 * valid, or the <code>key</code> is <code>null</code>.
 */
    public Object doGetProperty(TimmonId timmon_id, String key) throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        SQLTimmonId sql_timmon_id = (SQLTimmonId) timmon_id;
        int id = (int) (sql_timmon_id.getTimmonId());
        try {
            return (mapToProperty(key, sqlGetStringProperty(id, key)));
        } catch (SQLException exc) {
            throw (new TimmonBackEndException(exc));
        }
    }

    /**
   * @param key
   * @param val
   * @return
   */
    private static Object mapToProperty(String key, String val) {
        if (val == null) return (null);
        if (TimmonConstants.PROP_KEY_LOG_DURATION.equals(key)) {
            try {
                return (Duration.parse(val));
            } catch (ParseException e) {
                return (null);
            }
        } else if (TimmonConstants.PROP_KEY_LOG_PERIOD.equals(key)) {
            return (Period.fromString(val));
        } else if (TimmonConstants.PROP_KEY_MAX_TIME.equals(key)) {
            try {
                return (Duration.parse(val));
            } catch (ParseException e) {
                return (null);
            }
        } else if (TimmonConstants.PROP_KEY_OVERTIME.equals(key)) {
            return (Period.fromString(val));
        } else if (TimmonConstants.PROP_KEY_START_TIME.equals(key)) {
            return (toJavaDate(val));
        } else if (TimmonConstants.PROP_KEY_STOP_TIME.equals(key)) {
            return (toJavaDate(val));
        } else if (TimmonConstants.PROP_KEY_TOTAL_DURATION.equals(key)) {
            try {
                return (Duration.parse(val));
            } catch (ParseException e) {
                return (null);
            }
        } else if (TimmonConstants.PROP_KEY_RIGHT.equals(key)) {
            try {
                return (Right.parse(val));
            } catch (ParseException e) {
                return (null);
            }
        } else {
            if ((!TimmonConstants.PROP_KEY_DESCRIPTION.equals(key)) && (!TimmonConstants.PROP_KEY_NAME.equals(key)) && (!TimmonConstants.PROP_KEY_TASK.equals(key)) && (!TimmonConstants.PROP_KEY_TITLE.equals(key)) && (!"lastname".equals(key)) && (!"online".equals(key)) && (!"password".equals(key)) && (!"firstname".equals(key)) && (!"lastname".equals(key))) System.err.println("WARNING no mapping for key: " + key + ", of val: " + val);
            return (val);
        }
    }

    /**
   * @param key
   * @param val
   * @return
   */
    private static Object mapToGlobalProperty(String key, String val) {
        if (TimmonConstants.GPROP_KEY_ROOT_TASK_ID.equals(key)) {
            try {
                return (new SQLTimmonId(Integer.parseInt(val), TimmonConstants.TYPE_TASK));
            } catch (NumberFormatException exc) {
                throw (new IllegalArgumentException(exc.getMessage()));
            }
        } else if (TimmonConstants.GPROP_KEY_ROOT_GROUP_ID.equals(key)) {
            try {
                return (new SQLTimmonId(Integer.parseInt(val), TimmonConstants.TYPE_GROUP_ENTRY));
            } catch (NumberFormatException exc) {
                throw (new IllegalArgumentException(exc.getMessage()));
            }
        } else {
            System.err.println("WARNING no mapping for global key: " + key + ", of val: " + val);
            return (val);
        }
    }

    /**
 * Returns a map holding key-value pairs of all properties. Returns an
 * empty Map if no properties are given.
 *int id = (int)((SQLTimmonId)timmon_id).getTimmonId();
    try
    {
      sql_get_property_stmt_.setInt(1, id);
      sql_get_property_stmt_.setString(2, key);
      ResultSet rs = sql_change_property_stmt_.executeQuery();
      if ((rs != null) && (rs.next()))
      {
        String val = rs.getString(1);
        rs.close();    
        return(mapToProperty(key, val));
      }
    }
    catch (SQLException exc)
    {
      throw (new TimmonBackEndException(exc));
    }
    return(null);
 * @param id the id of the object.
 * @return a map holding key-value pairs of all properties or an empty
 * Map if no properties are given.
 * @exception RemoteException if a network error occurres.
 * @exception SecurityException if the user does not have the required
 * access rights.
 * @exception IllegalArgumentException if <code>id</code> is not
 * valid.
 */
    public Map doGetProperties(TimmonId timmon_id) throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        HashMap map = new HashMap();
        int id = (int) ((SQLTimmonId) timmon_id).getTimmonId();
        try {
            sql_get_properties_stmt_.setInt(1, id);
            ResultSet rs = sql_get_properties_stmt_.executeQuery();
            if (rs != null) {
                String key;
                while (rs.next()) {
                    key = rs.getString(1);
                    map.put(key, mapToProperty(key, rs.getString(2)));
                }
                rs.close();
            }
            return (map);
        } catch (SQLException exc) {
            throw (new TimmonBackEndException(exc));
        }
    }

    /**
 * Changes a global property. If there exists a global property with
 * the given key, the new value replaces the old one. If the value is
 * set to <code>null</code> the global property is deleted. If there
 * did not exist a global property with the given key, it will be
 * created. 
 * For defined property keys and values see {@link TimmonConstants}.
 *
 * @param key the key of the global property to be changed.
 * @param value the value of the global property to be changed, or
 * <code>null</code> to delete the global property.
 * @exception RemoteException if a network error occurres.
 * @exception SecurityException if the user does not have the required
 * access rights.
 * @exception IllegalArgumentException if <code>key</code> is
 * <code>null</code>.
 */
    public void doChangeGlobalProperty(String key, Object value) throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        try {
            if (doGetGlobalProperty(key) != null) {
                if (value == null) {
                    sql_delete_global_property_stmt_.setString(1, key);
                    sql_delete_global_property_stmt_.executeUpdate();
                    return;
                }
                sql_change_global_property_stmt_.setString(2, key);
                sql_change_global_property_stmt_.setString(1, value.toString());
                sql_change_global_property_stmt_.executeUpdate();
                return;
            }
            sql_insert_global_property_stmt_.setString(1, key);
            sql_insert_global_property_stmt_.setString(2, value.toString());
            sql_insert_global_property_stmt_.executeUpdate();
        } catch (SQLException exc) {
            throw (new TimmonBackEndException(exc));
        }
    }

    /**
 * Changes the global properties. If there exists a global property
 * with one of the given keys, the new value replaces the old one. If
 * the value is set to <code>null</code> the global property is
 * deleted. If there did not exist a global property with one of the
 * given keys, it will be created.
 * For defined property keys and values see {@link TimmonConstants}.
 *
 * @param global properties the map containing the global properties
 * to be changed.
 * @exception RemoteException if a network error occurres.
 * @exception SecurityException if the user does not have the required
 * access rights.
 * @exception IllegalArgumentException if <code>properties</code> is
 * <code>null</code>.
 */
    public void doChangeGlobalProperties(Map properties) throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        Iterator iter = properties.entrySet().iterator();
        Map.Entry entry;
        while (iter.hasNext()) {
            entry = (Map.Entry) iter.next();
            doChangeGlobalProperty((String) entry.getKey(), entry.getValue());
        }
    }

    /**
 * Returns the value of the global property with the given key or
 * <code>null</code>, if the key does not exist within the
 * global properties.
 * For defined property keys and values see {@link TimmonConstants}.
 *
 * @param key the key of the global property.
 * @return the value of the global property, or <code>null</code>, if the
 * key does not exist within the global properties.
 * @exception RemoteException if a network error occurres.
 * @exception SecurityException if the user does not have the required
 * access rights.
 * @exception IllegalArgumentException if <code>key</code> is
 * <code>null</code>.
 */
    public Object doGetGlobalProperty(String key) throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        if (key.equals(TimmonConstants.GPROP_KEY_USER_FULL_NAME)) {
            return ("ma_lastname_" + " " + "ma_firstname_");
        }
        if (key == TimmonConstants.GPROP_KEY_USER_NAME) {
            return (getUsername());
        }
        try {
            sql_get_global_property_stmt_.setString(1, key);
            ResultSet rs = sql_get_global_property_stmt_.executeQuery();
            if ((rs != null) && (rs.next())) {
                String val = rs.getString(1);
                rs.close();
                return (mapToGlobalProperty(key, val));
            }
        } catch (SQLException exc) {
            exc.printStackTrace();
            throw (new TimmonBackEndException(exc));
        }
        return (null);
    }

    /**
 * Returns a map holding key-value pairs of all global properties or
 * <code>null</code> if no global properties exist.
 * For defined property keys and values see {@link TimmonConstants}.
 *
 * @return a map holding key-value pairs of all global properties or
 * <code>null</code> if no global properties exist.
 * @exception RemoteException if a network error occurres.
 * @exception SecurityException if the user does not have the required
 * access rights.
 */
    public Map doGetGlobalProperties() throws SecurityException, RemoteException, TimmonBackEndException {
        HashMap map = new HashMap();
        try {
            ResultSet rs = sql_get_global_properties_stmt_.executeQuery();
            if (rs != null) {
                String key;
                while (rs.next()) {
                    key = rs.getString(1);
                    map.put(key, mapToGlobalProperty(key, rs.getString(2)));
                }
                rs.close();
            }
            return (map);
        } catch (SQLException exc) {
            throw (new TimmonBackEndException(exc));
        }
    }

    private static SimpleDateFormat dbformatter_;

    public static Date toJavaDate(String date) {
        if (date == null) return null;
        if (dbformatter_ == null) dbformatter_ = new SimpleDateFormat("yyyyMMddHHmmss");
        try {
            return dbformatter_.parse(date);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected int getUserId(String username) throws SecurityException, SQLException {
        sql_get_user_id_stmt_.setString(1, username);
        ResultSet rs = sql_get_user_id_stmt_.executeQuery();
        if (!rs.next()) {
            throw (new SecurityException("user or password unknown or wrong"));
        }
        return (rs.getInt(1));
    }

    protected int getUserId(String username, String password) throws SecurityException, SQLException {
        int user_id = getUserId(username);
        sql_get_property_stmt_.setInt(1, user_id);
        sql_get_property_stmt_.setString(2, "password");
        ResultSet rs = sql_get_property_stmt_.executeQuery();
        if ((!rs.next()) || (!password.equals(rs.getString(1)))) {
            throw (new SecurityException("user or password unknown or wrong"));
        }
        rs.close();
        return (user_id);
    }

    protected int getGroupId(String groupname) throws SecurityException, SQLException {
        sql_get_group_params_by_name_stmt_.setString(1, groupname);
        ResultSet rs = sql_get_group_params_by_name_stmt_.executeQuery();
        if (!rs.next()) {
            throw (new SecurityException("user or password unknown or wrong"));
        }
        return (rs.getInt(1));
    }

    public SQLTimmonId[] searchUsers(String name) throws SQLException {
        sql_select_users_stmt_.setString(1, name);
        sql_select_users_stmt_.setString(2, name);
        sql_select_users_stmt_.setString(3, name);
        ResultSet rs = sql_select_users_stmt_.executeQuery();
        Vector ret_vector = new Vector();
        while (rs.next()) {
            SQLTimmonId user_id = new SQLTimmonId(rs.getInt(1), TimmonConstants.TYPE_USER_ENTRY);
            ret_vector.add(user_id);
        }
        SQLTimmonId[] ret = new SQLTimmonId[ret_vector.size()];
        ret_vector.toArray(ret);
        return (ret);
    }

    public SQLTimmonId[] searchGroups(String name) throws SQLException {
        sql_select_groups_stmt_.setString(1, name);
        ResultSet rs = sql_select_groups_stmt_.executeQuery();
        Vector ret_vector = new Vector();
        while (rs.next()) {
            SQLTimmonId group_id = new SQLTimmonId(rs.getInt(1), TimmonConstants.TYPE_GROUP_ENTRY);
            ret_vector.add(group_id);
        }
        SQLTimmonId[] ret = new SQLTimmonId[ret_vector.size()];
        ret_vector.toArray(ret);
        return (ret);
    }

    public SQLTimmonId[] searchUsersInGroup(String name, TimmonId timmon_id) throws SQLException {
        SQLTimmonId sql_timmon_id = (SQLTimmonId) timmon_id;
        if (!isTypeOf(TYPE_GROUP_MASK, sql_timmon_id.getTimmonIdType())) throw (new IllegalArgumentException("timmon_id must be of type 'group'"));
        int id = (int) sql_timmon_id.getTimmonId();
        sql_select_users_from_group_stmt_.setString(1, name);
        sql_select_users_from_group_stmt_.setString(2, name);
        sql_select_users_from_group_stmt_.setString(3, name);
        sql_select_users_from_group_stmt_.setInt(4, id);
        ResultSet rs = sql_select_users_from_group_stmt_.executeQuery();
        Vector ret_vector = new Vector();
        while (rs.next()) {
            SQLTimmonId user_id = new SQLTimmonId(rs.getInt(1), TimmonConstants.TYPE_USER_ENTRY);
            ret_vector.add(user_id);
        }
        SQLTimmonId[] ret = new SQLTimmonId[ret_vector.size()];
        ret_vector.toArray(ret);
        return (ret);
    }

    public void deleteLock(String username, String password) throws SecurityException, RemoteException, TimmonBackEndException {
        try {
            int user_id = getUserId(username, password);
            setLoginState(user_id, false);
        } catch (SQLException exc) {
            throw (new TimmonBackEndException(exc));
        }
        user_id_ = -1;
    }

    /**
   * @param user_id
   * @return
   */
    private boolean getLoginState(int user_id) throws SQLException {
        sql_get_property_stmt_.setInt(1, user_id);
        sql_get_property_stmt_.setString(2, "online");
        ResultSet rs = sql_get_property_stmt_.executeQuery();
        if (rs.next()) {
            if ("true".equals(rs.getString(1))) return (true);
        }
        rs.close();
        return false;
    }

    /**
   * @param user_id
   * @param b
   * @throws SQLException 
   */
    private void setLoginState(int user_id, boolean logged_in) throws SQLException {
        sql_get_property_stmt_.setInt(1, user_id);
        sql_get_property_stmt_.setString(2, "online");
        ResultSet rs = sql_get_property_stmt_.executeQuery();
        if (rs.next()) {
            if (!"false".equals(rs.getString(1))) {
                if (logged_in) return;
                sql_change_property_stmt_.setString(1, "online");
                sql_change_property_stmt_.setString(2, "false");
                sql_change_property_stmt_.setInt(3, user_id);
                sql_change_property_stmt_.setString(4, "online");
                sql_change_property_stmt_.executeUpdate();
            }
        } else {
            sql_insert_property_stmt_.setInt(1, user_id);
            sql_insert_property_stmt_.setString(2, "online");
            if (logged_in) sql_insert_property_stmt_.setString(3, "true"); else sql_insert_property_stmt_.setString(3, "false");
            sql_insert_property_stmt_.executeUpdate();
        }
        rs.close();
    }

    public void unlink(TimmonId id, TimmonId parent_id) throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        SQLTimmonId sql_id = (SQLTimmonId) id;
        SQLTimmonId sql_parent_id = (SQLTimmonId) parent_id;
        int type = sql_id.getTimmonIdType();
        if (isTypeOf(TYPE_USER_MASK, type)) {
            if (!isTypeOf(TYPE_GROUP_MASK, sql_parent_id.getTimmonIdType())) throw (new TimmonBackEndException("cannot unlink user from non 'group' parent (" + sql_parent_id.getTimmonIdType() + ")"));
            SQLTimmonId root_group_id = (SQLTimmonId) doGetGlobalProperty(GPROP_KEY_ROOT_GROUP_ID);
            if (root_group_id.equals(parent_id)) throw (new TimmonBackEndException("cannot unlink user from 'all' group"));
            try {
                sql_delete_user_from_group_stmt_.setInt(1, (int) sql_parent_id.getTimmonId());
                sql_delete_user_from_group_stmt_.setInt(2, (int) sql_id.getTimmonId());
                sql_delete_user_from_group_stmt_.executeUpdate();
            } catch (SQLException exc) {
                if (Debug.DEBUG) Debug.println("exc", exc + Debug.getStackTrace(exc));
                throw (new TimmonBackEndException(exc));
            }
        } else throw (new TimmonBackEndException("can only unlink users from groups"));
    }

    public void link(TimmonId id, TimmonId parent_id) throws SecurityException, RemoteException, IllegalArgumentException, TimmonBackEndException {
        SQLTimmonId sql_id = (SQLTimmonId) id;
        SQLTimmonId sql_parent_id = (SQLTimmonId) parent_id;
        int type = sql_id.getTimmonIdType();
        if (isTypeOf(TYPE_USER_MASK, type)) {
            if (!isTypeOf(TYPE_GROUP_MASK, sql_parent_id.getTimmonIdType())) throw (new TimmonBackEndException("cannot link user to non 'group' parent (" + sql_parent_id.getTimmonIdType() + ")"));
            SQLTimmonId root_group_id = (SQLTimmonId) doGetGlobalProperty(GPROP_KEY_ROOT_GROUP_ID);
            if (root_group_id.equals(parent_id)) throw (new TimmonBackEndException("cannot link user to 'all' group"));
            try {
                sql_add_user_to_group_stmt_.setInt(1, (int) sql_parent_id.getTimmonId());
                sql_add_user_to_group_stmt_.setInt(2, (int) sql_id.getTimmonId());
                sql_add_user_to_group_stmt_.executeUpdate();
            } catch (SQLException exc) {
                if (Debug.DEBUG) Debug.println("exc", exc + Debug.getStackTrace(exc));
                throw (new TimmonBackEndException(exc));
            }
        } else throw (new TimmonBackEndException("can only link users to groups"));
    }

    /**
   * 
   */
    protected Connection getConnection() {
        return (connection_);
    }

    private void addAllParentGroups(Vector groups, Integer group_id, Set filter_out) throws SQLException {
        Stack to_do_ids = new Stack();
        HashSet added = new HashSet();
        if (group_id.intValue() != 0) to_do_ids.push(group_id);
        int id;
        while (!to_do_ids.isEmpty()) {
            group_id = (Integer) to_do_ids.pop();
            groups.add(group_id);
            sql_get_group_parent_stmt_.setInt(1, group_id.intValue());
            ResultSet rs = sql_get_group_parent_stmt_.executeQuery();
            if ((rs != null) && (rs.next())) {
                id = rs.getInt(1);
                if (id != 0) {
                    Integer to_add = new Integer(id);
                    if ((!added.contains(to_add)) && (!filter_out.contains(to_add))) {
                        to_do_ids.push(to_add);
                        added.add(to_add);
                    }
                }
            }
        }
    }

    private void addAllParentTasks(Vector groups, Integer task_id) throws SQLException {
        Stack to_do_ids = new Stack();
        if (task_id.intValue() != 0) to_do_ids.push(task_id);
        int id;
        while (!to_do_ids.isEmpty()) {
            task_id = (Integer) to_do_ids.pop();
            groups.add(task_id);
            sql_get_parent_of_task_stmt_.setInt(1, task_id.intValue());
            ResultSet rs = sql_get_parent_of_task_stmt_.executeQuery();
            if ((rs != null) && (rs.next())) {
                id = rs.getInt(1);
                if (id != 0) to_do_ids.push(new Integer(id));
            }
        }
    }

    private Vector getAllParentGroupIds() throws SQLException {
        Vector groups = new Vector();
        HashSet filter_out = new HashSet();
        sql_get_parent_group_ids_stmt_.setInt(1, user_id_);
        ResultSet rs = sql_get_parent_group_ids_stmt_.executeQuery();
        if (rs != null) {
            while (rs.next()) {
                int pos = groups.size();
                addAllParentGroups(groups, new Integer(rs.getInt(1)), filter_out);
                for (; pos < groups.size(); pos++) filter_out.add(groups.elementAt(pos));
            }
            rs.close();
        }
        return (groups);
    }

    /**
   * @param object_id
   * @return
   * @throws SQLException 
   */
    private Vector getPathToRootIds(int object_id) throws SQLException {
        Vector parent_tasks = new Vector();
        if (isTypeOf(object_id, TimmonConstants.TYPE_ACTIVITY)) {
            parent_tasks.add(new Integer(object_id));
            sql_get_parent_of_activity_stmt_.setInt(1, object_id);
            ResultSet rs = sql_get_parent_of_activity_stmt_.executeQuery();
            if ((rs != null) && (rs.next())) {
                addAllParentTasks(parent_tasks, new Integer(rs.getInt(1)));
            }
        } else {
            addAllParentTasks(parent_tasks, new Integer(object_id));
        }
        return (parent_tasks);
    }

    protected boolean doCheckRight(TimmonId timmon_id, int required_rights) throws SecurityException, RemoteException, TimmonBackEndException {
        if (isAdminUser()) return (true);
        int right_type = 0;
        if ((required_rights & TimmonConstants.RIGHTS_CREATE_TASKS) != 0) right_type |= Right.CREATE_TASKS;
        if ((required_rights & TimmonConstants.RIGHTS_CREATE_ACTIVITIES) != 0) right_type |= Right.CREATE_ACTIVITIES;
        if ((required_rights & TimmonConstants.RIGHTS_DELETE_TASK) != 0) right_type |= Right.CREATE_TASKS;
        if ((required_rights & TimmonConstants.RIGHTS_DELETE_ACTIVITY) != 0) right_type |= Right.CREATE_ACTIVITIES;
        if ((required_rights & TimmonConstants.RIGHTS_DELETE) != 0) right_type |= Right.EDIT;
        if ((required_rights & TimmonConstants.RIGHTS_EDIT) != 0) right_type |= Right.EDIT;
        if ((required_rights & TimmonConstants.RIGHTS_MOVE) != 0) right_type |= Right.EDIT;
        if ((required_rights & TimmonConstants.RIGHTS_CHANGE_PROPERTIES) != 0) right_type |= Right.EDIT;
        if ((required_rights & TimmonConstants.RIGHTS_READ) != 0) right_type |= Right.READ;
        int object_id;
        int group_id;
        try {
            Vector parent_group_ids = getAllParentGroupIds();
            Vector parent_group_names = new Vector();
            parent_group_names.ensureCapacity(parent_group_ids.size());
            Vector parent_task_ids = getPathToRootIds((int) ((SQLTimmonId) timmon_id).getTimmonId());
            Vector parent_task_rights = new Vector();
            parent_task_rights.ensureCapacity(parent_task_ids.size());
            String group_name;
            Right rights;
            for (int task_pos = 0; task_pos < parent_task_ids.size(); task_pos++) {
                object_id = ((Integer) parent_task_ids.elementAt(task_pos)).intValue();
                if (parent_task_rights.size() <= task_pos) {
                    rights = Right.parse(sqlGetStringProperty(object_id, TimmonConstants.PROP_KEY_RIGHT));
                    parent_task_rights.add(rights);
                } else {
                    rights = (Right) parent_task_rights.elementAt(task_pos);
                }
                if (rights == null) continue;
                if (rights.isUserDenied(getUsername(), right_type)) return (false);
                if (rights.isUserAllowed(getUsername(), right_type)) return (true);
                for (int group_pos = 0; group_pos < parent_group_ids.size(); group_pos++) {
                    group_id = ((Integer) parent_group_ids.elementAt(group_pos)).intValue();
                    if (parent_group_names.size() <= group_pos) {
                        group_name = sqlGetStringProperty(group_id, TimmonConstants.PROP_KEY_NAME);
                        parent_group_names.add(group_name);
                    } else {
                        group_name = (String) parent_group_names.elementAt(group_pos);
                    }
                    if (rights.isGroupDenied(group_name, right_type)) return (false);
                    if (rights.isGroupAllowed(group_name, right_type)) return (true);
                }
            }
        } catch (ParseException exc) {
            return ((right_type & Right.EDIT) == 0);
        } catch (SQLException exc) {
            if (Debug.DEBUG) Debug.println("exc", exc + Debug.getStackTrace(exc));
            throw (new TimmonBackEndException(exc));
        }
        return (false);
    }

    protected boolean doCheckRight(int required_rights) throws SecurityException, RemoteException, TimmonBackEndException {
        if (ADMIN_USER_NAME.equals(getUsername())) return (true);
        int right_type = 0;
        if ((required_rights & TimmonConstants.RIGHTS_CREATE_TASKS) != 0) right_type |= Right.CREATE_TASKS;
        if ((required_rights & TimmonConstants.RIGHTS_CREATE_ACTIVITIES) != 0) right_type |= Right.CREATE_ACTIVITIES;
        if ((required_rights & TimmonConstants.RIGHTS_DELETE_TASK) != 0) right_type |= Right.CREATE_TASKS;
        if ((required_rights & TimmonConstants.RIGHTS_DELETE_ACTIVITY) != 0) right_type |= Right.CREATE_ACTIVITIES;
        if ((required_rights & TimmonConstants.RIGHTS_EDIT) != 0) right_type |= Right.EDIT;
        if ((required_rights & TimmonConstants.RIGHTS_DELETE) != 0) right_type |= Right.EDIT;
        if ((required_rights & TimmonConstants.RIGHTS_MOVE) != 0) right_type |= Right.EDIT;
        if ((required_rights & TimmonConstants.RIGHTS_CHANGE_PROPERTIES) != 0) right_type |= Right.EDIT;
        if ((required_rights & TimmonConstants.RIGHTS_READ) != 0) right_type |= Right.READ;
        int group_id;
        Vector parent_group_ids;
        try {
            parent_group_ids = getAllParentGroupIds();
            for (int group_pos = 0; group_pos < parent_group_ids.size(); group_pos++) {
                group_id = ((Integer) parent_group_ids.elementAt(group_pos)).intValue();
                if (isAdminGroup(sqlGetStringProperty(group_id, TimmonConstants.PROP_KEY_NAME))) return (true);
            }
        } catch (SQLException exc) {
            if (Debug.DEBUG) Debug.println("exc", exc + Debug.getStackTrace(exc));
            throw (new TimmonBackEndException(exc));
        }
        return (false);
    }
}
