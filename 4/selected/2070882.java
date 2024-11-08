package de.annotatio.client;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * @author alex
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class PermissionManager {

    private Connection database;

    private String principal;

    /**
	 * @param con Working database connection to the user database
	 */
    PermissionManager(Connection con, String Principal) {
        this.database = con;
        this.principal = Principal;
    }

    public int addUser(String uid) {
        String query = "";
        int entity_id = -1;
        try {
            query = "SELECT NEXT VALUE FOR ACL_ENT_ID FROM ACL_ENTITY";
            ResultSet rs;
            rs = this.getDatabase().createStatement().executeQuery(query);
            if (rs.next()) {
                entity_id = rs.getInt(1);
            }
            query = "INSERT INTO ACL_ENTITY (ID) VALUES (" + entity_id + ")";
            if (this.getDatabase().createStatement().executeUpdate(query) < 1) {
                System.out.println("Error executing SQL statement:" + query);
                return -1;
            }
            query = "INSERT INTO ACL_ENTITY_USER (ID, UID) VALUES(" + entity_id + ",'" + uid + "')";
            if (this.getDatabase().createStatement().executeUpdate(query) < 1) {
                System.out.println("Error executing SQL statement:" + query);
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity_id;
    }

    /**
	 * Set the user permission to an annotation
	 * @param annohash Uniform identifier of annotation, usual as MD5 hash
	 * @param uid The user name or name of authenticated Principal
	 * @param readPerm Whether the user is allowed to read the annotation
	 * @param writePerm Whether the user is allowed to change the annotation
	 * @param delegatePerm Whether the user is allowed to change the permissions
	 * @return True if the permission was set sucessfully
	 */
    public boolean setUserPermission(String annohash, String uid, boolean readPerm, boolean writePerm, boolean delegatePerm) {
        int entity_id = 0;
        String query = "";
        try {
            entity_id = getUserID(uid);
            if (entity_id == -1) entity_id = addUser(uid);
            query = "INSERT INTO ACL (ANNOHASH, ACL_ENTITY_ID, READPERM, WRITEPERM, DELEGPERM, SEARCHPERM, PRIORITY) VALUES ('" + annohash + "'," + entity_id + "," + readPerm + "," + writePerm + "," + delegatePerm + ",true,0)";
            if (this.getDatabase().createStatement().executeUpdate(query) < 1) {
                System.out.println("Error executing SQL statement:" + query);
                return false;
            }
        } catch (SQLException e) {
            System.out.println(query);
            e.printStackTrace();
        }
        return true;
    }

    public boolean setGroupPermission(String annohash, String gid, boolean readPerm, boolean writePerm, boolean delegatePerm, boolean searchPerm) {
        int entity_id = 0;
        String query = "";
        try {
            entity_id = getGroupID(gid);
            if (entity_id == -1) return false;
            query = "INSERT INTO ACL (ANNOHASH, ACL_ENTITY_ID, READPERM, WRITEPERM, DELEGPERM, SEARCHPERM, PRIORITY) VALUES ('" + annohash + "'," + entity_id + "," + readPerm + "," + writePerm + "," + delegatePerm + "," + searchPerm + ",0)";
            if (this.getDatabase().createStatement().executeUpdate(query) < 1) {
                System.out.println("Error executing SQL statement:" + query);
                return false;
            }
        } catch (SQLException e) {
            System.out.println(query);
            e.printStackTrace();
        }
        return true;
    }

    /**
	 * Check whether user has read permissions to annotation, either directly or through group membership
	 * @param annohash Hash of annotation
	 * @param uid username of user
	 * @return True if read permission, false if not
	 */
    public boolean checkReadPermission(String annohash) {
        String uid = this.principal;
        if (uid.equals("*")) return true;
        String query = "SELECT ACL.READPERM FROM acl, ( " + "SELECT ACL_ENTITY_USER.ID AS ENTITY_ID " + "FROM ACL_ENTITY_USER " + "WHERE ACL_ENTITY_USER.UID='" + uid + "' " + "UNION DISTINCT " + "SELECT ACL_ENTITY_GROUP.ID AS ENTITY_ID " + "FROM ACL_ENTITY_USER, ACL_ENTITY_GROUP, USER_GROUP " + "WHERE ACL_ENTITY_USER.UID='" + uid + "' AND ACL_ENTITY_USER.ID = USER_GROUP.USER_ID AND USER_GROUP.GROUP_ID = ACL_ENTITY_GROUP.ID) PERM " + "WHERE PERM.ENTITY_ID = ACL.ACL_ENTITY_ID AND ACL.ANNOHASH='" + annohash + "'";
        try {
            ResultSet rs = getDatabase().createStatement().executeQuery(query);
            if (rs.next()) {
                boolean perm = rs.getBoolean(1);
                return perm;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean checkUserDelegPermission(String annohash, String uid) {
        if (uid.equals("*")) return true;
        String query = "SELECT ACL.DELEGPERM FROM acl, ( " + "SELECT ACL_ENTITY_USER.ID AS ENTITY_ID " + "FROM ACL_ENTITY_USER " + "WHERE ACL_ENTITY_USER.UID='" + uid + "' " + "UNION DISTINCT " + "SELECT ACL_ENTITY_GROUP.ID AS ENTITY_ID " + "FROM ACL_ENTITY_USER, ACL_ENTITY_GROUP, USER_GROUP " + "WHERE ACL_ENTITY_USER.UID='" + uid + "' AND ACL_ENTITY_USER.ID = USER_GROUP.USER_ID AND USER_GROUP.GROUP_ID = ACL_ENTITY_GROUP.ID) PERM " + "WHERE PERM.ENTITY_ID = ACL.ACL_ENTITY_ID AND ACL.ANNOHASH='" + annohash + "'";
        try {
            ResultSet rs = getDatabase().createStatement().executeQuery(query);
            if (rs.next()) {
                boolean perm = rs.getBoolean(1);
                return perm;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
	 * Add a group to the system. The owner will get delegate permission which will enable him to delete the group
	 * later or add other persons to it
	 * @param gid
	 * @param owner_uid
	 * @return
	 */
    public int addGroup(String gid, String owner_uid) {
        if (getGroupID(gid) != -1) {
            System.out.println("Group " + gid + " already exists.");
            return -1;
        }
        String query = "";
        int entity_id = 0;
        int user_entity = getUserID(owner_uid);
        if (user_entity == -1) return -1;
        try {
            query = "SELECT NEXT VALUE FOR ACL_ENT_ID FROM ACL_ENTITY";
            ResultSet rs;
            rs = this.getDatabase().createStatement().executeQuery(query);
            if (rs.next()) {
                entity_id = rs.getInt(1);
            }
            query = "INSERT INTO ACL_ENTITY (ID) VALUES (" + entity_id + ")";
            if (this.getDatabase().createStatement().executeUpdate(query) < 1) {
                System.out.println("Error executing SQL statement:" + query);
                return -1;
            }
            query = "INSERT INTO ACL_ENTITY_GROUP (ID, UID, OWNER) VALUES(" + entity_id + ",'" + gid + "','" + user_entity + "')";
            if (this.getDatabase().createStatement().executeUpdate(query) < 1) {
                System.out.println("Error executing SQL statement:" + query);
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        addUserToGroup(owner_uid, gid, true);
        return entity_id;
    }

    public boolean addUserToGroup(String uid, String gid) {
        return addUserToGroup(uid, gid, false);
    }

    public boolean addUserToGroup(String uid, String gid, boolean delegperm) {
        int g_id = getGroupID(gid);
        int u_id = getUserID(uid);
        String query = "INSERT INTO USER_GROUP VALUES(" + u_id + "," + g_id + "," + delegperm + ")";
        try {
            if (this.getDatabase().createStatement().executeUpdate(query) < 1) {
                System.out.println("Error executing SQL statement:" + query);
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
	 * @param uid
	 * @return -1 if user does not exist, else the ID
	 */
    private int getUserID(String uid) {
        String query = "SELECT ID FROM ACL_ENTITY_USER  WHERE UID='" + uid + "'";
        try {
            ResultSet rs = getDatabase().createStatement().executeQuery(query);
            if (rs.next()) {
                int ID = rs.getInt("ID");
                return rs.getInt("ID");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private int getGroupID(String gid) {
        String query = "SELECT ID FROM ACL_ENTITY_GROUP  WHERE GID='" + gid + "'";
        try {
            ResultSet rs = getDatabase().createStatement().executeQuery(query);
            if (rs.next()) {
                int ID = rs.getInt("ID");
                return rs.getInt("ID");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
	 * Returns all Hash-IDs of Annotations that a given user has search-permissions on, either directly or through
	 * group membership
	 * @param uid
	 * @return
	 */
    public ArrayList getUserSearchableAnnotations(String uid) {
        ArrayList rlist = new ArrayList();
        String query = "SELECT ACL.ANNOHASH" + " FROM ACL, (SELECT ACL_ENTITY_USER.ID AS ENTITY_ID" + " FROM ACL_ENTITY_USER" + " WHERE ACL_ENTITY_USER.UID='" + uid + "'" + " UNION DISTINCT" + " SELECT ACL_ENTITY_GROUP.ID AS ENTITY_ID" + " FROM ACL_ENTITY_USER, ACL_ENTITY_GROUP, USER_GROUP" + " WHERE ACL_ENTITY_USER.UID='" + uid + "' AND ACL_ENTITY_USER.ID = USER_GROUP.USER_ID AND USER_GROUP.GROUP_ID = ACL_ENTITY_GROUP.ID) PERM" + " WHERE PERM.ENTITY_ID = ACL.ACL_ENTITY_ID AND ACL.SEARCHPERM=true";
        try {
            ResultSet rs = getDatabase().createStatement().executeQuery(query);
            while (rs.next()) {
                String hash = rs.getString("ANNOHASH");
                rlist.add(hash);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rlist;
    }

    /**
	 * @return Returns a list of available Groups within the permission manager
	 */
    public ArrayList getAvailableGroups() {
        ArrayList rlist = new ArrayList();
        String query = "SELECT GID FROM ACL_ENTITY_GROUP";
        try {
            ResultSet rs = getDatabase().createStatement().executeQuery(query);
            while (rs.next()) {
                String hash = rs.getString("GID");
                rlist.add(hash);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rlist;
    }

    /**
	 * @return Returns the database.
	 */
    private Connection getDatabase() {
        return database;
    }

    /**
	 * @return Returns the principal.
	 */
    public String getPrincipal() {
        return principal;
    }
}
