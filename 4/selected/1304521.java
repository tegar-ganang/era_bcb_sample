package org.jmonks.dms.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import org.jmonks.dms.versioncontrol.api.RepositoryEntry;

/**
 *
 * @author Suresh Pragada
 */
public class UserManager {

    private static final Logger logger = Logger.getLogger(UserManager.class);

    private static final UserManager userManager = new UserManager();

    private File userConfigFile = null;

    private Map userMap = null;

    public static final String DMS_SESSION = "DMSSESSION";

    private static final String SUPER_ADMIN_USER_ID = "admin";

    /**
     * Creates a new instance of UserManager
     */
    private UserManager() {
        Locale locale = Locale.getDefault();
        ResourceBundle bundle = ResourceBundle.getBundle("org.jmonks.dms.application", locale);
        File directory = new File((String) bundle.getString("dms.repository.path"));
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                directory = new File(System.getProperty("user.home") + File.separator + "repository");
                directory.mkdirs();
            }
        } else if (!directory.isDirectory()) {
            directory = new File(System.getProperty("user.home") + File.separator + "repository");
            directory.mkdirs();
        }
        this.userConfigFile = new File(directory.getAbsolutePath() + File.separator + "users.dat");
        this.userMap = new HashMap();
        if (userConfigFile.exists()) this.readMap(); else {
            User user = new User(UserManager.SUPER_ADMIN_USER_ID, "admin", User.USER_TYPE_ADMIN, new HashMap());
            this.userMap.put("admin", user);
            this.writeMap();
        }
    }

    public static UserManager getInstance() {
        return userManager;
    }

    public boolean isUserExists(String userName) {
        return this.userMap.containsKey(userName);
    }

    public User getUser(String userName) {
        return (User) this.userMap.get(userName);
    }

    public List getAllUsers() {
        Collection userCollection = this.userMap.values();
        return new ArrayList(userCollection);
    }

    public boolean createUser(String userName, String password, String userType, Map entryMap) {
        boolean created = true;
        if (this.isUserExists(userName)) created = false; else {
            synchronized (this.userMap) {
                this.userMap.put(userName, new User(userName, password, userType, entryMap));
                this.writeMap();
            }
        }
        return created;
    }

    public boolean modifyUser(String userName, String password, String userType, Map entryMap) {
        boolean modified = true;
        if (!this.isUserExists(userName)) modified = false; else {
            synchronized (this.userMap) {
                User pastUser = (User) this.userMap.remove(userName);
                this.userMap.put(userName, new User(userName, (password.equals(User.DUMMY_PASSWORD) ? pastUser.getPassword() : password), userType, entryMap));
                this.writeMap();
            }
        }
        return modified;
    }

    public boolean deleteUser(String userName) {
        boolean deleted = true;
        if (this.isUserExists(userName)) {
            synchronized (this.userMap) {
                this.userMap.remove(userName);
                this.writeMap();
            }
        } else {
            deleted = false;
        }
        return deleted;
    }

    private void readMap() {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(this.userConfigFile));
            this.userMap = (HashMap) ois.readObject();
            ois.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            logger.error("UserAuthorizationManager::readMap: Could not find the file : " + userConfigFile.toString() + " Message : " + ex.getMessage(), ex);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            logger.error("UserAuthorizationManager::readMap: Could not load the map object from file : " + userConfigFile.toString() + " Message : " + ex.getMessage(), ex);
        } catch (IOException ex) {
            ex.printStackTrace();
            logger.error("UserAuthorizationManager::readMap: Could not read content from the file : " + userConfigFile.toString() + " Message : " + ex.getMessage(), ex);
        }
    }

    private void writeMap() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(userConfigFile));
            oos.writeObject(this.userMap);
            oos.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            logger.error("UserAuthorizationManager::writeMap: Could not find the file : " + userConfigFile.toString() + " Message : " + ex.getMessage(), ex);
        } catch (IOException ex) {
            ex.printStackTrace();
            logger.error("UserAuthorizationManager::writeMap: Could not read content from the file : " + userConfigFile.toString() + " Message : " + ex.getMessage(), ex);
        }
    }

    public String toString() {
        StringBuffer retValue = new StringBuffer("{ UserAuthorizationManager ");
        retValue.append("[configFile : " + this.userConfigFile + "] ");
        retValue.append("[userMap : " + this.userMap + "] ");
        retValue.append(" }");
        return retValue.toString();
    }

    public class User implements Serializable {

        public static final String USER_TYPE_ADMIN = "ADMIN";

        public static final String USER_TYPE_USER = "USER";

        public static final String DUMMY_PASSWORD = "DUMMY";

        private String userName = null;

        private String password = null;

        private String userType = null;

        private Map entryMap = null;

        private static final long serialVersionUID = 3373081475982611075L;

        /** Creates a new instance of User by loading the user details from the request.
	 *
	 */
        private User(String userName, String password, String userType, Map entryMap) {
            this.userName = userName;
            this.userType = userType;
            this.password = password;
            this.entryMap = entryMap;
        }

        public String getUserName() {
            return this.userName;
        }

        public String getUserType() {
            return this.userType;
        }

        private String getPassword() {
            return this.password;
        }

        public boolean isAdmin() {
            if (User.USER_TYPE_ADMIN.equals(this.userType)) return true; else return false;
        }

        public List getEntryList() {
            return new ArrayList(entryMap.keySet());
        }

        public List getEntriesToShow() {
            return new ArrayList(this.entryMap.values());
        }

        public boolean isAllowed(RepositoryEntry requestedEntry) throws Exception {
            boolean allowed = true;
            if (!this.entryMap.isEmpty()) {
                long entryID = requestedEntry.getEntryID();
                if (this.entryMap.containsKey(entryID + "")) allowed = false; else {
                    RepositoryEntry parentRepositoryEntry = requestedEntry.getParentRepositoryEntry();
                    while (allowed) {
                        if (parentRepositoryEntry != null) {
                            entryID = parentRepositoryEntry.getEntryID();
                            if (this.entryMap.containsKey(entryID + "")) {
                                allowed = false;
                                break;
                            } else {
                                parentRepositoryEntry = parentRepositoryEntry.getParentRepositoryEntry();
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
            return allowed;
        }

        public boolean authenticate(String password) {
            return this.password.equals(password);
        }

        public boolean isSuperAdmin() {
            if (this.userName.equals(UserManager.SUPER_ADMIN_USER_ID)) return true; else return false;
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            stream.writeUTF(this.userName);
            stream.writeUTF(this.password);
            stream.writeUTF(this.userType);
            stream.writeObject(this.entryMap);
        }

        private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
            this.userName = stream.readUTF();
            this.password = stream.readUTF();
            this.userType = stream.readUTF();
            this.entryMap = (Map) stream.readObject();
        }

        public String toString() {
            StringBuffer retValue = new StringBuffer("{ User ");
            retValue.append("[UserName : " + this.userName + "]");
            retValue.append("[UserType : " + this.userType + "]");
            retValue.append("[password : " + this.password + "]");
            retValue.append("[EntryMap: " + this.entryMap.toString() + "]");
            retValue.append(" }");
            return retValue.toString();
        }
    }
}
