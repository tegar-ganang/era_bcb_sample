package xregistry.group;

import static xregistry.utils.Utils.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import xregistry.SQLConstants;
import xregistry.XregistryConstants;
import xregistry.XregistryException;
import xregistry.context.GlobalContext;
import xregistry.generated.ListSubActorsGivenAGroupResponseDocument.ListSubActorsGivenAGroupResponse.Actor;
import xregistry.utils.Utils;
import xsul.MLogger;

public class GroupManagerImpl implements SQLConstants, GroupManager {

    protected static MLogger log = MLogger.getLogger(XregistryConstants.LOGGER_NAME);

    private final Hashtable<String, Group> groups = new Hashtable<String, Group>();

    private final GlobalContext context;

    private boolean cascadingDeletes = true;

    private Hashtable<String, User> users = new Hashtable<String, User>();

    private Hashtable<String, String> adminUsers = new Hashtable<String, String>();

    public GroupManagerImpl(GlobalContext context) throws XregistryException {
        Connection connection = context.createConnection();
        this.context = context;
        try {
            Statement statement = connection.createStatement();
            ResultSet results = statement.executeQuery(SQLConstants.GET_ALL_GROUPS_SQL);
            while (results.next()) {
                String groupId = results.getString(GROUPID);
                addGroup(new Group(groupId));
            }
            results.close();
            results = statement.executeQuery(SQLConstants.GET_ALL_GROUP2GROUP_SQL);
            while (results.next()) {
                String masterGroupId = results.getString(GROUPID);
                String containedGroupID = results.getString(CONTANTED_GROUP_ID);
                getGroup(masterGroupId).addGroup(getGroup(containedGroupID));
            }
            results.close();
            results = statement.executeQuery(SQLConstants.GET_ALL_USER2GROUP_SQL);
            while (results.next()) {
                String userID = results.getString(USERID);
                String groupId = results.getString(GROUPID);
                Group group = getGroup(groupId);
                if (group != null) {
                    group.addUser(userID);
                } else {
                    log.warning("Group " + groupId + " find in user to group table, but not found in Group table. Database may be inconsistant");
                }
            }
            results.close();
            results = statement.executeQuery(SQLConstants.GET_ADMIN_USERS_SQL);
            while (results.next()) {
                String adminUSer = results.getString(USERID);
                adminUsers.put(adminUSer, adminUSer);
            }
            results.close();
            String[] userList = listUsers();
            if (userList != null) {
                for (String user : userList) {
                    users.put(user, new User(user));
                }
            }
            results = statement.executeQuery(SQLConstants.GET_CAPABILITIES);
            while (results.next()) {
                boolean isUser = results.getBoolean(IS_USER);
                String resourceID = results.getString(RESOURCE_ID);
                String action = results.getString(ACTION_TYPE);
                String actorName = results.getString(ALLOWED_ACTOR);
                if (!isUser) {
                    Group group = getGroup(actorName);
                    if (group != null) {
                        group.addAuthorizedResource(resourceID, action);
                    }
                } else {
                    User user = getUser(actorName);
                    if (user != null) {
                        user.addAuthorizedResource(resourceID, action);
                    }
                }
            }
            results.close();
            Group publicGroup = getGroup(XregistryConstants.PUBLIC_GROUP);
            if (publicGroup == null) {
                createGroup(XregistryConstants.PUBLIC_GROUP, "Public Group");
                publicGroup = getGroup(XregistryConstants.PUBLIC_GROUP);
            }
            for (String user : users.keySet()) {
                if (!publicGroup.hasUser(user)) {
                    addUsertoGroup(publicGroup.getName(), user);
                }
            }
            String anonymousUser = Utils.canonicalizeDN(XregistryConstants.ANONYMOUS_USER);
            if (!hasUser(anonymousUser)) {
                createUser(anonymousUser, anonymousUser, false);
            }
            if (!publicGroup.hasUser(anonymousUser)) {
                addUsertoGroup(publicGroup.getName(), anonymousUser);
            }
        } catch (SQLException e) {
            throw new XregistryException(e);
        } finally {
            context.closeConnection(connection);
        }
    }

    public boolean isAdminUser(String user) {
        return adminUsers.containsKey(user);
    }

    public boolean hasUser(String userName) {
        return users.containsKey(userName);
    }

    public Group getGroup(String name) {
        return groups.get(name);
    }

    protected void addGroup(Group group) {
        groups.put(group.getName(), group);
    }

    public void createGroup(String newGroup, String description) throws XregistryException {
        Connection connection = context.createConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(ADD_GROUP_SQL);
            statement.setString(1, newGroup);
            statement.setString(2, description);
            statement.executeUpdate();
            addGroup(new Group(newGroup));
            log.info("Group " + newGroup + " Created");
        } catch (SQLException e) {
            throw new XregistryException(e);
        } finally {
            context.closeConnection(connection);
        }
    }

    public void createUser(String newUser, String description, boolean isAdmin) throws XregistryException {
        Connection connection = context.createConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(ADD_USER_SQL);
            statement.setString(1, Utils.canonicalizeDN(newUser));
            statement.setString(2, description);
            statement.setBoolean(3, isAdmin);
            statement.executeUpdate();
            log.info("User " + newUser + " created");
            users.put(newUser, new User(newUser));
        } catch (SQLException e) {
            throw new XregistryException(e);
        } finally {
            context.closeConnection(connection);
        }
    }

    public void addGrouptoGroup(String groupName, String grouptoAddedName) throws XregistryException {
        Group group = getGroup(groupName);
        if (group == null) {
            throw new XregistryException("No such Group " + groupName);
        }
        Group grouptoAdd = getGroup(groupName);
        if (grouptoAdd == null) {
            throw new XregistryException("No such Group " + groupName);
        }
        if (group.hasGroup(grouptoAddedName)) {
            throw new XregistryException("Group" + grouptoAddedName + " already exisits in group " + groupName);
        }
        Connection connection = context.createConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(ADD_GROUP_TO_GROUP_SQL);
            statement.setString(1, groupName);
            statement.setString(2, grouptoAddedName);
            statement.executeUpdate();
            group.addGroup(grouptoAdd);
            log.info("Add Group " + groupName + " to " + grouptoAddedName);
        } catch (SQLException e) {
            throw new XregistryException(e);
        } finally {
            context.closeConnection(connection);
        }
    }

    public void addUsertoGroup(String groupName, String usertoAdded) throws XregistryException {
        usertoAdded = Utils.canonicalizeDN(usertoAdded);
        Group group = getGroup(groupName);
        if (group == null) {
            throw new XregistryException("No such Group " + groupName);
        }
        if (group.hasUser(usertoAdded)) {
            throw new XregistryException("user " + usertoAdded + " already exisits in group " + groupName);
        }
        Connection connection = context.createConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(ADD_USER_TO_GROUP);
            statement.setString(1, usertoAdded);
            statement.setString(2, groupName);
            statement.executeUpdate();
            group.addUser(usertoAdded);
            log.info("Add User " + usertoAdded + " to " + groupName);
        } catch (SQLException e) {
            throw new XregistryException(e);
        } finally {
            context.closeConnection(connection);
        }
    }

    public void deleteGroup(String groupID) throws XregistryException {
        try {
            Connection connection = context.createConnection();
            connection.setAutoCommit(false);
            try {
                PreparedStatement statement1 = connection.prepareStatement(DELETE_GROUP_SQL_MAIN);
                statement1.setString(1, groupID);
                int updateCount = statement1.executeUpdate();
                if (updateCount == 0) {
                    throw new XregistryException("Database is not updated, Can not find such Group " + groupID);
                }
                if (cascadingDeletes) {
                    PreparedStatement statement2 = connection.prepareStatement(DELETE_GROUP_SQL_DEPEND);
                    statement2.setString(1, groupID);
                    statement2.setString(2, groupID);
                    statement2.executeUpdate();
                }
                connection.commit();
                groups.remove(groupID);
                log.info("Delete Group " + groupID + (cascadingDeletes ? " with cascading deletes " : ""));
            } catch (SQLException e) {
                connection.rollback();
                throw new XregistryException(e);
            } finally {
                context.closeConnection(connection);
            }
        } catch (SQLException e) {
            throw new XregistryException(e);
        }
    }

    public void deleteUser(String userID) throws XregistryException {
        try {
            userID = Utils.canonicalizeDN(userID);
            Connection connection = context.createConnection();
            connection.setAutoCommit(false);
            try {
                PreparedStatement statement1 = connection.prepareStatement(DELETE_USER_SQL_MAIN);
                statement1.setString(1, userID);
                statement1.executeUpdate();
                PreparedStatement statement2 = connection.prepareStatement(DELETE_USER_SQL_DEPEND);
                statement2.setString(1, userID);
                statement2.executeUpdate();
                connection.commit();
                Collection<Group> groupList = groups.values();
                for (Group group : groupList) {
                    group.removeUser(userID);
                }
                log.info("Delete User " + userID);
            } catch (SQLException e) {
                connection.rollback();
                throw new XregistryException(e);
            } finally {
                context.closeConnection(connection);
            }
        } catch (SQLException e) {
            throw new XregistryException(e);
        }
    }

    public void genericUpdate(String sql, String[] keys) throws XregistryException {
        Connection connection = context.createConnection();
        try {
            PreparedStatement statement1 = connection.prepareStatement(sql);
            for (int i = 0; i < keys.length; i++) {
                statement1.setString(i + 1, keys[i]);
            }
            statement1.executeUpdate();
        } catch (SQLException e) {
            throw new XregistryException(e);
        } finally {
            context.closeConnection(connection);
        }
    }

    public void removeUserFromGroup(String groupName, String usertoRemoved) throws XregistryException {
        usertoRemoved = canonicalizeDN(usertoRemoved);
        genericUpdate(REMOVE_USER_FROM_GROUP, new String[] { usertoRemoved, groupName });
        Group group = getGroup(groupName);
        if (group == null) {
            throw new XregistryException("No such group " + groupName);
        }
        if (group.hasUser(usertoRemoved)) {
            group.removeUser(usertoRemoved);
        } else {
            throw new XregistryException("No such User " + usertoRemoved);
        }
    }

    public void removeGroupFromGroup(String groupName, String grouptoRemovedName) throws XregistryException {
        Group group = getGroup(groupName);
        Group groupToRemove = getGroup(groupName);
        if (group == null) {
            throw new XregistryException("No such group " + groupName);
        }
        if (grouptoRemovedName == null) {
            throw new XregistryException("No such group " + grouptoRemovedName);
        }
        group.removeGroup(groupToRemove);
        genericUpdate(REMOVE_GROUP_FROM_GROUP, new String[] { grouptoRemovedName, groupName });
    }

    public String[] listUsers() throws XregistryException {
        Connection connection = context.createConnection();
        ArrayList<String> users = new ArrayList<String>();
        try {
            Statement statement = connection.createStatement();
            ResultSet results = statement.executeQuery(SQLConstants.GET_ALL_USERS_SQL);
            while (results.next()) {
                String userID = results.getString(USERID);
                users.add(userID);
            }
        } catch (SQLException e) {
            throw new XregistryException(e);
        } finally {
            context.closeConnection(connection);
        }
        return Utils.toStrListToArray(users);
    }

    public String[] listGroups() throws XregistryException {
        Connection connection = context.createConnection();
        ArrayList<String> groups = new ArrayList<String>();
        try {
            Statement statement = connection.createStatement();
            ResultSet results = statement.executeQuery(SQLConstants.GET_ALL_GROUPS_SQL);
            while (results.next()) {
                String groupId = results.getString(GROUPID);
                groups.add(groupId);
            }
        } catch (SQLException e) {
            throw new XregistryException(e);
        } finally {
            context.closeConnection(connection);
        }
        return Utils.toStrListToArray(groups);
    }

    public String[] listGroupsGivenAUser(String user) throws XregistryException {
        user = canonicalizeDN(user);
        Connection connection = context.createConnection();
        ArrayList<String> groups = new ArrayList<String>();
        try {
            PreparedStatement statement = connection.prepareStatement(GET_GROUPS_GIVEN_USER);
            statement.setString(1, user);
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                String groupId = results.getString(GROUPID);
                groups.add(groupId);
            }
        } catch (SQLException e) {
            throw new XregistryException(e);
        } finally {
            context.closeConnection(connection);
        }
        return Utils.toStrListToArray(groups);
    }

    public Actor[] listSubActorsGivenAGroup(String group) throws XregistryException {
        List<Actor> actorList = new ArrayList<Actor>();
        Connection connection = context.createConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(GET_USERS_GIVEN_GROUP);
            statement.setString(1, group);
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                String groupId = results.getString(USERID);
                Actor actor = Actor.Factory.newInstance();
                actor.setActor(groupId);
                actor.setIsUser(true);
                actorList.add(actor);
            }
            results.close();
            statement.close();
            statement = connection.prepareStatement(GET_SUBGROUPS_GIVEN_GROUP);
            statement.setString(1, group);
            results = statement.executeQuery();
            while (results.next()) {
                String groupId = results.getString(CONTANTED_GROUP_ID);
                Actor actor = Actor.Factory.newInstance();
                actor.setActor(groupId);
                actor.setIsUser(false);
                actorList.add(actor);
            }
        } catch (SQLException e) {
            throw new XregistryException(e);
        } finally {
            context.closeConnection(connection);
        }
        return actorList.toArray(new Actor[0]);
    }

    public Collection<Group> getGroups() {
        return groups.values();
    }

    public User getUser(String user) {
        return users.get(user);
    }
}
