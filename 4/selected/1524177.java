package org.damour.base.client.objects;

import java.io.Serializable;

public class PendingGroupMembership implements Serializable, IHibernateFriendly {

    public User user;

    public UserGroup userGroup;

    public PendingGroupMembership() {
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        result = prime * result + ((userGroup == null) ? 0 : userGroup.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PendingGroupMembership other = (PendingGroupMembership) obj;
        if (user == null) {
            if (other.user != null) return false;
        } else if (!user.equals(other.user)) return false;
        if (userGroup == null) {
            if (other.userGroup != null) return false;
        } else if (!userGroup.equals(other.userGroup)) return false;
        return true;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UserGroup getUserGroup() {
        return userGroup;
    }

    public void setUserGroup(UserGroup userGroup) {
        this.userGroup = userGroup;
    }

    public boolean isFieldUnique(String fieldName) {
        return false;
    }

    public boolean isFieldKey(String fieldName) {
        if (fieldName.equals("user")) {
            return true;
        } else if (fieldName.equals("userGroup")) {
            return true;
        }
        return false;
    }

    public String getSqlUpdate() {
        return null;
    }

    public String getCachePolicy() {
        return "nonstrict-read-write";
    }

    public boolean isLazy() {
        return false;
    }

    public boolean isFieldMapped(String fieldName) {
        return true;
    }

    public String getFieldType(String fieldName) {
        return null;
    }

    public int getFieldLength(String fieldName) {
        return -1;
    }
}
