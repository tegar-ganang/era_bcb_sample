package org.xiaoniu.suafe.beans;

import org.xiaoniu.suafe.SubversionConstants;
import org.xiaoniu.suafe.exceptions.AppException;
import org.xiaoniu.suafe.resources.ResourceUtil;
import org.xiaoniu.suafe.validators.Validator;

/**
 * Represents a single Subversion AccessRule.
 * An AccessRule consists of a Path (repository and relative path),
 * a User or Group, and the level of access. AccessRules are always
 * persisted.
 * 
 * @author Shaun Johsnon
 */
public final class AccessRule implements Comparable<AccessRule> {

    /**
	 * Path referenced by the AccessRule.
	 */
    protected Path path;

    /**
	 * Group that has the specified level of access in the Path.
	 * Group is null if User is set.
	 */
    protected Group group;

    /**
	 * User that has the specified level of access in the Path.
	 * User is null if Group is set.
	 */
    protected User user;

    /**
	 * Level of access that the User or Group has within the Path.
	 */
    protected String level;

    /**
	 * Default Constructor.
	 */
    public AccessRule() {
        super();
        this.path = null;
        this.group = null;
        this.user = null;
        this.level = null;
    }

    /**
	 * Constructor that accepts the Path and level of access.
	 * 
	 * @param path The Path to be set.
	 * @param level The level of access to be set.
	 */
    public AccessRule(Path path, String level) {
        super();
        this.path = path;
        this.group = null;
        this.user = null;
        this.level = level;
    }

    /**
	 * Constructor that accepts the Path, Group and level of access.
	 * 
	 * @param path The Path to be set.
	 * @param group The Group to be given access.
	 * @param level The level of access to be set.
	 */
    public AccessRule(Path path, Group group, String level) {
        super();
        this.path = path;
        this.group = group;
        this.user = null;
        this.level = level;
    }

    /**
	 * Constructor that accepts the Path, User and level of access.
	 * 
	 * @param path The Path to be set.
	 * @param user The user to be given access.
	 * @param level The level of access to be set.
	 */
    public AccessRule(Path path, User user, String level) {
        super();
        this.path = path;
        this.group = null;
        this.user = user;
        this.level = level;
    }

    /**
	 * Returns the AccessRule object as a String.
	 */
    public String toString() {
        return ((path == null || path.getRepository() == null) ? "" : path.getRepository().toString()) + ":" + ((path == null) ? "" : path.toString()) + ":" + ((group == null) ? "" : group.toString()) + ":" + ((user == null) ? "" : user.toString());
    }

    /**
	 * Gets the Group referenced by the AccessRule.
	 * 
	 * @return Returns the Group.	 
	 */
    public Group getGroup() {
        return group;
    }

    /**
	 * Sets the Group referenced by the AccessRule.
	 * 
	 * @param group The Group to set.
	 */
    public void setGroup(Group group) {
        this.group = group;
    }

    /**
	 * Gets the User referenced by the AccessRule.
	 * 
	 * @return Returns the User.
	 */
    public User getUser() {
        return user;
    }

    /**
	 * Sets the User referenced by the AccessRule.
	 * 
	 * @param user The User to set.
	 */
    public void setUser(User user) {
        this.user = user;
    }

    /**
	 * Gets the Path referenced by the AccessRule.
	 * 
	 * @return Returns the Path.
	 */
    public Path getPath() {
        return path;
    }

    /**
	 * Sets the Path referenced by the AccessRule.
	 * 
	 * @param path The Path to set.
	 */
    public void setPath(Path path) {
        this.path = path;
    }

    /**
	 * Gets the level of access.
	 * 
	 * @return Returns the level of access.
	 */
    public String getLevel() {
        return level;
    }

    /**
	 * Gets the full English form of the level of access.
	 * 
	 * @return Returns the level of access in English.
	 */
    public String getLevelFullName() {
        String name = null;
        if (level == null) {
            name = ResourceUtil.getString("application.unknown");
        } else if (level.equals(SubversionConstants.SVN_ACCESS_LEVEL_DENY_ACCESS)) {
            name = ResourceUtil.getString("accesslevel.denyaccess");
        } else if (level.equalsIgnoreCase(SubversionConstants.SVN_ACCESS_LEVEL_READONLY)) {
            name = ResourceUtil.getString("accesslevel.readonly");
        } else if (level.equalsIgnoreCase(SubversionConstants.SVN_ACCESS_LEVEL_READWRITE)) {
            name = ResourceUtil.getString("accesslevel.readwrite");
        }
        return name;
    }

    /**
	 * Sets the level of access.
	 * 
	 * @param level The level of access to set.
	 * @throws AppException
	 */
    public void setLevel(String level) throws AppException {
        Validator.validateLevelOfAccess(level);
        this.level = level;
    }

    /**
	 * Compares this object to another. 
	 * 
	 * @param otherAccessRule The other AccessRule to use for comparison.
	 */
    public int compareTo(AccessRule otherAccessRule) {
        return this.toString().compareTo(otherAccessRule.toString());
    }
}
